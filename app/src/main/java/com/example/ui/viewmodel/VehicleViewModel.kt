package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.VehicleDatabase
import com.example.data.model.SearchHistory
import com.example.data.model.VehicleReport
import com.example.data.repository.VehicleRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val report: VehicleReport) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class VehicleViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VehicleDatabase.getDatabase(application)
    private val repository = VehicleRepository(db.searchHistoryDao())

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val reportAdapter = moshi.adapter(VehicleReport::class.java)

    // Текущий поисковый стейт
    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    // Вводимые пользователем значения
    val queryInput = MutableStateFlow("")
    val searchType = MutableStateFlow("VIN") // "VIN" or "PLATE"
    val selectedCountry = MutableStateFlow("Россия") // "Россия" or "Таджикистан"

    // История поиска из локальной БД Room
    val historyList: StateFlow<List<SearchHistory>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onQueryChanged(newQuery: String) {
        queryInput.value = newQuery
    }

    fun onSearchTypeChanged(newType: String) {
        searchType.value = newType
        queryInput.value = ""
        _searchState.value = SearchUiState.Idle
    }

    fun onCountryChanged(newCountry: String) {
        selectedCountry.value = newCountry
        queryInput.value = ""
        _searchState.value = SearchUiState.Idle
    }

    /**
     * Вызов валидации поля ввода
     */
    fun validateCurrentInput(): String? {
        val raw = queryInput.value.trim()
        if (raw.isBlank()) {
            return "Пожалуйста, введите данные для поиска"
        }
        if (searchType.value == "VIN") {
            if (!repository.validateVin(raw)) {
                return "Неверный формат VIN. Должно быть ровно 17 символов. Буквы I, O, Q запрещены."
            }
        } else {
            // PLATE
            if (selectedCountry.value == "Россия") {
                if (!repository.validateRussianPlate(raw)) {
                    return "Неверный формат госномера РФ. Пример: А123БВ77"
                }
            } else {
                // Таджикистан
                if (!repository.validateTajikPlate(raw)) {
                    return "Неверный формат госномера РТ. Пример: 1234АВ01"
                }
            }
        }
        return null
    }

    /**
     * Основная функция поиска
     */
    fun performSearch() {
        val validationError = validateCurrentInput()
        if (validationError != null) {
            _searchState.value = SearchUiState.Error(validationError)
            return
        }

        viewModelScope.launch {
            _searchState.value = SearchUiState.Loading
            val query = queryInput.value.trim().uppercase()
            val country = selectedCountry.value
            val sType = searchType.value

            try {
                // Если Таджикистан и поиск по госномеру - симулируем сбор почты (как в ТЗ)
                if (country == "Таджикистан" && sType == "PLATE") {
                    // Генерируем "недоступно", но позволяем отфильтровать далее в UI
                    val report = repository.getVehicleReport(query, sType, country)
                    _searchState.value = SearchUiState.Success(report)
                } else {
                    val report = repository.getVehicleReport(query, sType, country)
                    _searchState.value = SearchUiState.Success(report)

                    // Сохраняем в кэш поиска Room
                    val jsonReport = reportAdapter.toJson(report) ?: ""
                    val historyItem = SearchHistory(
                        queryField = query,
                        queryType = sType,
                        country = country,
                        make = report.make,
                        model = report.model,
                        year = report.year,
                        jsonReport = jsonReport
                    )
                    repository.saveToHistory(historyItem)
                }
            } catch (e: Exception) {
                _searchState.value = SearchUiState.Error("Ошибка получения данных: ${e.localizedMessage ?: "проверьте интернет"}")
            }
        }
    }

    /**
     * Загрузить отчет из истории
     */
    fun selectHistoryItem(history: SearchHistory) {
        viewModelScope.launch {
            _searchState.value = SearchUiState.Loading
            try {
                val report = reportAdapter.fromJson(history.jsonReport)
                if (report != null) {
                    _searchState.value = SearchUiState.Success(report)
                    queryInput.value = history.queryField
                    searchType.value = history.queryType
                    selectedCountry.value = history.country
                } else {
                    _searchState.value = SearchUiState.Error("Не удалось загрузить отчет из истории")
                }
            } catch (e: Exception) {
                _searchState.value = SearchUiState.Error("Ошибка открытия: ${e.message}")
            }
        }
    }

    /**
     * Разблокировать подробный отчет (Freemium оплата)
     */
    fun unlockFullReport(report: VehicleReport) {
        viewModelScope.launch {
            val updated = report.copy(isFullReportPaid = true)
            _searchState.value = SearchUiState.Success(updated)

            // Также обновим в истории, если этот элемент там есть
            try {
                // Ищем по нашему сохраненному VIN/номеру
                val historyListCurrent = historyList.value
                val matchingHistory = historyListCurrent.firstOrNull { 
                    it.queryField.uppercase() == report.vin.uppercase() || 
                    (report.plate != null && it.queryField.uppercase() == report.plate.uppercase()) 
                }
                if (matchingHistory != null) {
                    val updatedJson = reportAdapter.toJson(updated) ?: ""
                    repository.saveToHistory(matchingHistory.copy(jsonReport = updatedJson))
                }
            } catch (e: Exception) {
                // Не критично для UI
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearAllSearchHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun resetState() {
        _searchState.value = SearchUiState.Idle
        queryInput.value = ""
    }
}
