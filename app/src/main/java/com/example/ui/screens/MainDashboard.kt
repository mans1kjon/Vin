package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SearchHistory
import com.example.data.model.VehicleReport
import com.example.ui.viewmodel.SearchUiState
import com.example.ui.viewmodel.VehicleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: VehicleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val queryInput by viewModel.queryInput.collectAsStateWithLifecycle()
    val searchType by viewModel.searchType.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Поиск, 1 = История, 2 = Информация
    var showScanDialog by remember { mutableStateOf(false) }
    var subscriptionEmail by remember { mutableStateOf("") }
    var subSuccess by remember { mutableStateOf(false) }

    // Градиент фона приложения для создания атмосферности
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Car",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AutoCheck",
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.resetState()
                            subSuccess = false
                            subscriptionEmail = ""
                        },
                        modifier = Modifier.testTag("reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сброс")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                    label = { Text("Поиск") },
                    modifier = Modifier.testTag("nav_search")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "История") },
                    label = { Text("История") },
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Инфо") },
                    label = { Text("Инфо") },
                    modifier = Modifier.testTag("nav_info")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(bgGradient)
        ) {
            when (activeTab) {
                0 -> {
                    // Вкладка Поиска
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Баннер бренда автомобиля
                        AutoIntroBanner()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Панель выбора страны
                        CountryPicker(
                            selectedCountry = selectedCountry,
                            onCountrySelected = { viewModel.onCountryChanged(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Переключатель VIN / Госномер
                        SearchTypeSelector(
                            currentType = searchType,
                            onTypeSelected = { viewModel.onSearchTypeChanged(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Поле ввода + кнопка сканера
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = queryInput,
                                onValueChange = { viewModel.onQueryChanged(it) },
                                label = {
                                    Text(
                                        if (searchType == "VIN") "Введите 17-значный VIN"
                                        else if (selectedCountry == "Россия") "Госномер (например: А123БВ77)"
                                        else "Госномер (например: 1234AB01)"
                                    )
                                },
                                placeholder = {
                                    Text(
                                        if (searchType == "VIN") "WBA123..."
                                        else if (selectedCountry == "Россия") "А777АА99"
                                        else "8888AA01"
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (searchType == "VIN") Icons.Default.QrCode
                                        else Icons.Default.Pin,
                                        contentDescription = "TypeIcon"
                                    )
                                },
                                trailingIcon = {
                                    if (queryInput.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_input_field"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    keyboardType = KeyboardType.Ascii
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Кнопка Камера-сканера
                            FilledIconButton(
                                onClick = { showScanDialog = true },
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("scan_camera_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Сканировать номер",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Текст подсказки формата
                        InputHintText(searchType, selectedCountry)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Главная кнопка Проверить
                        Button(
                            onClick = { viewModel.performSearch() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("search_action_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = "Проверить")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Проверить автомобиль", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Состояние поиска (Лоадер, успех, ошибка, либо сбор емейла для Таджикистана по номерам)
                        AnimatedContent(
                            targetState = searchState,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "search_state_content"
                        ) { state ->
                            when (state) {
                                is SearchUiState.Idle -> {
                                    InfoCardsSection()
                                }
                                is SearchUiState.Loading -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "Опрашиваем базы данных и расшифровываем VIN...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                is SearchUiState.Success -> {
                                    val r = state.report
                                    // Если Таджикистан и поиск по госномеру - выводим форму подписки (ТЗ)
                                    if (r.country == "Таджикистан" && searchType == "PLATE") {
                                        TajikistanUnavailableSection(
                                            query = r.vin,
                                            email = subscriptionEmail,
                                            onEmailChanged = { subscriptionEmail = it },
                                            successSub = subSuccess,
                                            onSubscribe = {
                                                if (subscriptionEmail.contains("@") && subscriptionEmail.contains(".")) {
                                                    subSuccess = true
                                                    Toast.makeText(context, "Успешно подписано!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Введите корректный Email", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    } else {
                                        ReportDetailCard(
                                            report = r,
                                            onUnlockFullReport = { viewModel.unlockFullReport(r) }
                                        )
                                    }
                                }
                                is SearchUiState.Error -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.BugReport,
                                                contentDescription = "Ошибка",
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = state.message,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Вкладка Истории
                    SearchHistoryTab(
                        history = historyList,
                        onHistorySelected = {
                            viewModel.selectHistoryItem(it)
                            activeTab = 0 // Переключаемся на вкладку поиска
                        },
                        onDeleteHistory = { viewModel.deleteHistoryItem(it) },
                        onClearAll = { viewModel.clearAllSearchHistory() }
                    )
                }
                2 -> {
                    // Вкладка Информации
                    AppInformationTab()
                }
            }
        }
    }

    // Диалог симулятора сканирования (OCR)
    if (showScanDialog) {
        ScanSimulatorDialog(
            country = selectedCountry,
            searchType = searchType,
            onDismiss = { showScanDialog = false },
            onScanResult = { scannedText ->
                viewModel.onQueryChanged(scannedText)
                showScanDialog = false
                Toast.makeText(context, "Успешно распознано: $scannedText", Toast.LENGTH_LONG).show()
            }
        )
    }
}

// Вспомогательные компоненты UI

@Composable
fun AutoIntroBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Мгновенная проверка",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "История регистрации, ДТП, залоги, ограничения и штрафы транспортных средств в СНГ в одном месте.",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun CountryPicker(
    selectedCountry: String,
    onCountrySelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Регион проверки:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Россия Button
                CountryButton(
                    countryName = "Россия",
                    flagChar = "🇷🇺",
                    isSelected = selectedCountry == "Россия",
                    onClick = { onCountrySelected("Россия") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("country_picker_ru")
                )

                // Таджикистан Button
                CountryButton(
                    countryName = "Таджикистан",
                    flagChar = "🇹🇯",
                    isSelected = selectedCountry == "Таджикистан",
                    onClick = { onCountrySelected("Таджикистан") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("country_picker_tj")
                )
            }
        }
    }
}

@Composable
fun CountryButton(
    countryName: String,
    flagChar: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text(flagChar, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(countryName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SearchTypeSelector(
    currentType: String,
    onTypeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        val options = listOf("VIN" to "Проверка по VIN", "PLATE" to "По госномеру")
        options.forEach { (type, label) ->
            val isSelected = currentType == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = 10.dp)
                    .testTag("search_type_${type.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InputHintText(searchType: String, country: String) {
    val text = if (searchType == "VIN") {
        "VIN содержит 17 латинских символов и цифр. Без букв I, O, Q."
    } else {
        if (country == "Россия") {
            "Формат: С Кириллической буквой А,В,Е,К,М,Н,О,Р,С,Т,У,Х. Например: А777АА77 или К182КК199"
        } else {
            "Формат Таджикистана: 4 цифры, затем 2 латинские/русские буквы, 2 цифры региона. Например: 1234AB01"
        }
    }
    Text(
        text = text,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        textAlign = TextAlign.Start
    )
}

@Composable
fun InfoCardsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Наши возможности:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoFeatureCard(
                icon = Icons.Outlined.HistoryEdu,
                title = "Регистрации",
                desc = "История владельцев по ПТС",
                modifier = Modifier.weight(1f)
            )
            InfoFeatureCard(
                icon = Icons.Outlined.Gavel,
                title = "Ограничения",
                desc = "Залоги, розыски, запреты пристава",
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoFeatureCard(
                icon = Icons.Outlined.ReportGmailerrorred,
                title = "ДТП",
                desc = "Записи происшествий с повреждениями",
                modifier = Modifier.weight(1f)
            )
            InfoFeatureCard(
                icon = Icons.Outlined.Payments,
                title = "Штрафы",
                desc = "С камер фиксации нарушений",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InfoFeatureCard(
    icon: ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, lineHeight = 14.sp)
        }
    }
}
