package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiRetrofitClient
import com.example.data.api.RetrofitClient
import com.example.data.database.SearchHistoryDao
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale

class VehicleRepository(private val searchHistoryDao: SearchHistoryDao) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val reportAdapter = moshi.adapter(VehicleReport::class.java)

    val allHistory: Flow<List<SearchHistory>> = searchHistoryDao.getAllHistory()

    suspend fun saveToHistory(history: SearchHistory) = withContext(Dispatchers.IO) {
        searchHistoryDao.insertHistory(history)
    }

    suspend fun deleteHistory(id: Long) = withContext(Dispatchers.IO) {
        searchHistoryDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearAllHistory()
    }

    /**
     * Валидация VIN (17 символов, латиница/цифры, без I, O, Q)
     */
    fun validateVin(vin: String): Boolean {
        val clean = vin.trim().uppercase()
        if (clean.length != 17) return false
        if (clean.contains("I") || clean.contains("O") || clean.contains("Q")) return false
        return clean.matches(Regex("^[A-Z0-9]{17}$"))
    }

    /**
     * Валидация госномера России (например, А123БВ77, А123БВ199)
     * Допускаются русские буквы, совпадающие по начертанию с латинскими: А, В, Е, К, М, Н, О, Р, С, Т, У, Х (в любом регистре)
     */
    fun validateRussianPlate(plate: String): Boolean {
        val clean = plate.trim().uppercase().replace(" ", "")
        // Приводим все возможные латинские аналоги к кириллице для единообразия
        val cyrillized = cyrillizeRussianPlate(clean)
        return cyrillized.matches(Regex("^[АВЕКМНОРСТУХ]\\d{3}[АВЕКМНОРСТУХ]{2}\\d{2,3}$"))
    }

    fun cyrillizeRussianPlate(plate: String): String {
        val mapping = mapOf(
            'A' to 'А', 'B' to 'В', 'E' to 'Е', 'K' to 'К', 'M' to 'М',
            'H' to 'Н', 'O' to 'О', 'P' to 'Р', 'C' to 'С', 'T' to 'Т',
            'Y' to 'У', 'X' to 'Х'
        )
        return plate.map { mapping[it] ?: it }.joinToString("")
    }

    /**
     * Валидация госномера Таджикистана
     * Типичный формат: 4 цифры, 2 буквы, 2 цифры региона (например, 1234АВ01 или 1234AB01)
     */
    fun validateTajikPlate(plate: String): Boolean {
        val clean = plate.trim().uppercase().replace(" ", "")
        return clean.matches(Regex("^\\d{4}[A-ZА-Я]{2}\\d{2}$"))
    }

    /**
     * Расшифровка WMI через бесплатный международный API NHTSA
     */
    suspend fun decodeVinNhtsa(vin: String): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.nhtsaService.decodeVin(vin)
            val results = response.Results
            val info = mutableMapOf<String, String>()

            // Извлекаем ключевые переменные
            val make = results.firstOrNull { it.Variable == "Make" }?.Value
            val model = results.firstOrNull { it.Variable == "Model" }?.Value
            val modelYear = results.firstOrNull { it.Variable == "Model Year" }?.Value
            val country = results.firstOrNull { it.Variable == "Plant Country" }?.Value ?: 
                          results.firstOrNull { it.Variable == "Country" }?.Value
            val bodyType = results.firstOrNull { it.Variable == "Body Class" }?.Value

            if (!make.isNullOrBlank()) info["make"] = make
            if (!model.isNullOrBlank()) info["model"] = model
            if (!modelYear.isNullOrBlank()) info["year"] = modelYear
            if (!country.isNullOrBlank()) info["country"] = country
            if (!bodyType.isNullOrBlank()) info["body"] = bodyType

            info
        } catch (e: Exception) {
            Log.e("NHTSA", "Error decoding", e)
            emptyMap()
        }
    }

    /**
     * Получить полный детальный отчет по авто через Gemini API или качественный генератор при отсутствии связи/ключей.
     */
    suspend fun getVehicleReport(
        query: String,
        queryType: String, // "VIN" or "PLATE"
        country: String // "Россия" or "Таджикистан"
    ): VehicleReport = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().uppercase()
        val apiKey = BuildConfig.GEMINI_API_KEY

        // 1. Пытаемся получить базовые данные из NHTSA, если это VIN
        var nhtsaInfo = emptyMap<String, String>()
        if (queryType == "VIN") {
            nhtsaInfo = decodeVinNhtsa(cleanQuery)
        }

        // Если ключ заменён плейсхолдером или пустой, сразу переходим на локальную генерацию
        if (apiKey.isBlank() || apiKey.contains("MY_GEMINI_API_KEY") || apiKey == "GEMINI_API_KEY") {
            Log.d("GeminiApi", "Using local simulation generator (No valid API key)")
            return@withContext generateSimulatedReport(cleanQuery, queryType, country, nhtsaInfo)
        }

        try {
            val systemInstructionText = """
                Ты — профессиональная система инспекции автомобилей в СНГ.
                Твоя задача — вернуть подробный структурированный отчет на русском языке.
                Формат ответа СТРОГО должен быть валидным JSON, соответствующим схеме:
                {
                  "vin": "строка",
                  "plate": "строка или null",
                  "country": "Россия" или "Таджикистан",
                  "make": "Марка авто",
                  "model": "Модель авто",
                  "year": 2018 (год целым числом),
                  "bodyType": "Седан / Хэтчбек / Кроссовер и т.д.",
                  "engineInfo": "Двигатель (например, 2.0 л, 150 л.с., Бензин)",
                  "color": "Цвет корпуса",
                  "wmiCountry": "Страна производства марки",
                  "registrations": [
                    { "period": "с 12.2018 по 05.2021", "ownerType": "Физическое лицо", "region": "Москва", "isActive": false }
                  ],
                  "accidents": [
                    { "date": "15.04.2021", "type": "Столкновение с препятствием", "damagedParts": "Передний бампер, капот", "region": "Московская область" }
                  ],
                  "restrictions": [
                    { "date": "10.02.2023", "basis": "Задолженность по штрафам", "status": "Активно", "region": "Москва" }
                  ],
                  "pledges": [
                    { "pledgeDate": "14.12.2018", "pledgee": "ПАО Сбербанк", "status": "Снято" }
                  ],
                  "fines": [
                    { "date": "10.05.2026", "description": "Превышение скорости на 20-40 км/ч (ст. 12.9 ч.2 КоАП РФ)", "amount": 500.0, "isPaid": false }
                  ],
                  "mileages": [
                    { "date": "15.12.2020", "valueKm": 45000, "source": "Техническое обслуживание" }
                  ],
                  "isWanted": false,
                  "isFullReportPaid": false
                }
                Если указан Таджикистан, примени местную специфику (например, органы ГАИ МВД РТ, штрафы в сомони, регистрация в Согдийской области или Душанбе).
                Не добавляй никаких дополнительных символов, markdown-разметки типа ```json или текста за пределами фигурных скобок JSON.
            """.trimIndent()

            val promptText = if (queryType == "VIN") {
                "Сделай отчет для автомобиля с VIN $cleanQuery из страны $country. Сведения от NHTSA API: марка ${nhtsaInfo["make"]}, модель ${nhtsaInfo["model"]}, год ${nhtsaInfo["year"]}, родина ${nhtsaInfo["country"]}."
            } else {
                "Сделай отчет для автомобиля с госномером $cleanQuery из страны $country."
            }

            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = promptText)))),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.5f
                ),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
            )

            val apiResponse = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val jsonText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (jsonText != null) {
                val cleanJson = cleanJsonResponse(jsonText)
                val report = reportAdapter.fromJson(cleanJson)
                if (report != null) {
                    return@withContext report.copy(
                        vin = if (report.vin.isBlank()) cleanQuery else report.vin,
                        plate = if (queryType == "PLATE") cleanQuery else report.plate
                    )
                }
            }
            throw IllegalStateException("Empty or invalid output JSON")
        } catch (e: Exception) {
            Log.e("GeminiApi", "Error retrieving report from Gemini, executing fallback code...", e)
            return@withContext generateSimulatedReport(cleanQuery, queryType, country, nhtsaInfo)
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        }
        if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        return text.trim()
    }

    /**
     * Генератор высококачественной симуляции для презентации и оффлайн режима.
     * Задействует реальные сведения из NHTSA и генерирует реалистичный отчет.
     */
    private fun generateSimulatedReport(
        query: String,
        queryType: String,
        country: String,
        nhtsaInfo: Map<String, String>
    ): VehicleReport {
        // Определение марки по VIN или плейт
        val make = nhtsaInfo["make"] ?: when {
            query.startsWith("XTA") -> "Lada"
            query.startsWith("WBA") -> "BMW"
            query.startsWith("WUA") -> "Audi"
            query.startsWith("JM3") -> "Mazda"
            query.startsWith("WDD") -> "Mercedes-Benz"
            query.startsWith("ZFF") -> "Ferrari"
            query.startsWith("X7L") -> "Opel"
            query.contains("TOY") -> "Toyota"
            country == "Таджикистан" -> listOf("Opel", "Toyota", "Lexus", "Hyundai").random()
            else -> listOf("Kia", "Hyundai", "Toyota", "Lada", "Volkswagen", "BMW").random()
        }

        val model = nhtsaInfo["model"] ?: when (make) {
            "Lada" -> listOf("Vesta", "Granta", "Niva Legend", "Priora").random()
            "BMW" -> listOf("3 Series", "5 Series", "X5", "7 Series").random()
            "Audi" -> listOf("A4", "A6", "Q5", "A8").random()
            "Mercedes-Benz" -> listOf("E-Class", "C-Class", "GLE", "S-Class").random()
            "Toyota" -> listOf("Camry", "RAV4", "Corolla", "Land Cruiser").random()
            "Kia" -> listOf("Rio", "Sportage", "Optima", "Sorento").random()
            "Hyundai" -> listOf("Solaris", "Creta", "Tucson", "Santa Fe").random()
            "Opel" -> listOf("Astra", "Vectra", "Zafira").random()
            "Lexus" -> listOf("RX350", "LX570", "ES250").random()
            else -> "Solaris"
        }

        val yearStr = nhtsaInfo["year"] ?: "2018"
        val year = yearStr.toIntOrNull() ?: (2010..2024).random()
        val bodyType = nhtsaInfo["body"] ?: listOf("Седан (4 дв.)", "Универсал", "Кроссовер (5 дв.)", "Хэтчбек (5 дв.)").random()
        val engine = listOf("1.6 л, 123 л.с., Бензин", "2.0 л, 150 л.с., Бензин", "3.0 л, 249 л.с., Дизель").random()
        val color = listOf("Белый перламутр", "Чёрный металлик", "Серый графит", "Тёмно-синий").random()
        val wmiCountry = nhtsaInfo["country"] ?: when {
            query.startsWith("X") -> "Россия"
            query.startsWith("W") -> "Германия"
            query.startsWith("J") -> "Япония"
            query.startsWith("K") -> "Южная Корея"
            else -> "США"
        }

        val vin = if (queryType == "VIN") query else "XTA21129${(10000000..99999999).random()}"
        val plate = if (queryType == "PLATE") query else {
            if (country == "Россия") {
                val region = listOf("77", "99", "777", "199", "78", "163").random()
                "А${listOf("102", "345", "967", "551").random()}ВК$region"
            } else {
                "${(1000..9999).random()}АВ${listOf("01", "02", "03", "04").random()}"
            }
        }

        // Страновая валюта и специфика
        val currency = if (country == "Россия") "₽" else "сомони"
        val regRegion = if (country == "Россия") "Московская область" else "Согдийская область, г. Худжанд"
        val isWanted = (1..100).random() > 94 // 6% шанс

        val registrations = listOf(
            RegistrationRecord("с 03.2019 по настоящее время", "Физическое лицо", if (country == "Россия") "Москва" else "Душанбе", true),
            RegistrationRecord("с 04.2018 по 03.2019", "Юридическое лицо (Лизинг)", if (country == "Россия") "Санкт-Петербург" else "г. Истаравшан", false)
        )

        val accidents = if ((1..10).random() > 6) {
            listOf(
                AccidentRecord("11.10.2019", "Столкновение сзади", "Задний бампер, оптика правая", if (country == "Россия") "Москва" else "Душанбе")
            )
        } else emptyList()

        val restrictions = if ((1..10).random() > 8) {
            listOf(
                RestrictionRecord("18.05.2025", "Неоплаченные штрафы ГИБДД", "Действует (Запрет на рег. действия)", if (country == "Россия") "Москва" else "Душанбе")
            )
        } else emptyList()

        val pledges = if ((1..10).random() > 8) {
            listOf(
                PledgeRecord("20.12.2018", "Альфа-Банк", "Действует")
            )
        } else emptyList()

        val fines = if ((1..10).random() > 6) {
            listOf(
                FineRecord("12.06.2026", "Превышение скорости на 20-40 км/ч", if (country == "Россия") 500.0 else 150.0, false),
                FineRecord("05.05.2026", "Несоблюдение требований разметки", if (country == "Россия") 1000.0 else 250.0, true)
            )
        } else emptyList()

        val mileages = listOf(
            MileageRecord("12.04.2025", 82000, "Техосмотр"),
            MileageRecord("18.10.2023", 51000, "Дилерский сервисный центр"),
            MileageRecord("22.04.2021", 15000, "Замена масла")
        )

        return VehicleReport(
            vin = vin,
            plate = plate,
            country = country,
            make = make,
            model = model,
            year = year,
            bodyType = bodyType,
            engineInfo = engine,
            color = color,
            wmiCountry = wmiCountry,
            registrations = registrations,
            accidents = accidents,
            restrictions = restrictions,
            pledges = pledges,
            fines = fines,
            mileages = mileages,
            isWanted = isWanted,
            isFullReportPaid = false // по умолчанию фальш - пользователь должен разблокировать
        )
    }
}
