package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*

@Composable
fun ReportDetailCard(
    report: VehicleReport,
    onUnlockFullReport: () -> Unit
) {
    var expandedGeneral by remember { mutableStateOf(true) }
    var expandedHistory by remember { mutableStateOf(false) }
    var expandedAccidents by remember { mutableStateOf(false) }
    var expandedFines by remember { mutableStateOf(false) }
    var expandedMileage by remember { mutableStateOf(false) }

    val warningColor = MaterialTheme.colorScheme.error

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("vehicle_report_detail_layout"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Заголовок - Марка, модель, год
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = report.make.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = report.model,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Флаг страны
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (report.country == "Россия") "🇷🇺 РФ" else "🇹🇯 РТ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(12.dp))

                // Год и WMI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoLine(label = "Год выпуска", value = report.year.toString(), modifier = Modifier.weight(1f))
                    InfoLine(label = "Сборка (WMI)", value = report.wmiCountry, modifier = Modifier.weight(1f))
                }
            }
        }

        // Блок критических проверок (Розыск, Залог, Ограничения)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusIndicatorCard(
                title = "Розыск",
                isAlert = report.isWanted,
                alertText = "В розыске!",
                okText = "Чист",
                icon = Icons.Default.AirportShuttle,
                modifier = Modifier.weight(1f).testTag("status_wanted")
            )

            StatusIndicatorCard(
                title = "Залоги",
                isAlert = report.pledges.any { it.status == "Действует" },
                alertText = "В залоге!",
                okText = "Нет залогов",
                icon = Icons.Default.AccountBalance,
                modifier = Modifier.weight(1f).testTag("status_pledges")
            )

            StatusIndicatorCard(
                title = "Запреты",
                isAlert = report.restrictions.isNotEmpty(),
                alertText = "Запрещен ГАИ",
                okText = "Ограничений нет",
                icon = Icons.Default.Gavel,
                modifier = Modifier.weight(1f).testTag("status_restrictions")
            )
        }

        // КНОПКА КУПИТЬ / ПЛАТНЫЙ ДЕТАЛЬНЫЙ ОТЧЕТ (Freemium)
        if (!report.isFullReportPaid) {
            PremiumUnlockCard(
                onUnlock = onUnlockFullReport
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = "Premium Unlocked", tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Все детальные платные реестры ФССП, Такси и Дилеров успешно разблокированы!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }

        // 1. Общие характеристики
        ExpandableSection(
            title = "Технические характеристики",
            icon = Icons.Default.Settings,
            isExpanded = expandedGeneral,
            onToggle = { expandedGeneral = !expandedGeneral }
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow(label = "VIN-номер", value = report.vin)
                DetailRow(label = "Госномер", value = report.plate ?: "Не указан")
                DetailRow(label = "Тип кузова", value = report.bodyType)
                DetailRow(label = "Двигатель", value = report.engineInfo)
                DetailRow(label = "Цвет", value = report.color)
            }
        }

        // 2. История владения
        ExpandableSection(
            title = "История регистрации (${report.registrations.size})",
            icon = Icons.Default.HistoryEdu,
            isExpanded = expandedHistory,
            onToggle = { expandedHistory = !expandedHistory }
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (report.registrations.isEmpty()) {
                    Text("Данные отсутствуют", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                } else {
                    report.registrations.forEach { reg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(reg.ownerType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (reg.isActive) {
                                        Text(
                                            "Действующая",
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                DetailRow(label = "Период", value = reg.period)
                                DetailRow(label = "Регион", value = reg.region)
                            }
                        }
                    }
                }
            }
        }

        // 3. Участие в ДТП
        ExpandableSection(
            title = "Участие в ДТП (${report.accidents.size})",
            icon = Icons.Default.ReportGmailerrorred,
            isExpanded = expandedAccidents,
            onToggle = { expandedAccidents = !expandedAccidents }
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (report.accidents.isEmpty()) {
                    Text("Официальных записей о страховых ДТП не обнаружено ✅", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                } else {
                    report.accidents.forEach { acc ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(acc.type, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                                    Text(acc.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Spacer(modifier = Modifier.height(4.dp))

                                // Скрытие деталей для фримиума
                                val damage = if (report.isFullReportPaid) acc.damagedParts else "🔒 Доступно в полном отчете"
                                DetailRow(
                                    label = "Повреждения",
                                    value = damage,
                                    valColor = if (report.isFullReportPaid) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
                                )
                                DetailRow(label = "Место ДТП", value = acc.region)
                            }
                        }
                    }
                }
            }
        }

        // 4. Штрафы ГИБДД/ГАИ
        ExpandableSection(
            title = "Неоплаченные штрафы (${report.fines.count { !it.isPaid }})",
            icon = Icons.Default.Payments,
            isExpanded = expandedFines,
            onToggle = { expandedFines = !expandedFines }
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val unpaidFines = report.fines.filter { !it.isPaid }
                if (unpaidFines.isEmpty()) {
                    Text("Все штрафы оплачены. Задолженностей нет ✅", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                } else {
                    unpaidFines.forEach { fine ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val fineDesc = if (report.isFullReportPaid) fine.description else "🔒 Статья и место нарушения скрыты"
                                    Text(
                                        text = fineDesc,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${fine.amount.toInt()} ${if (report.country == "Россия") "₽" else "TJS"}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                DetailRow(label = "Дата постановления", value = fine.date)
                            }
                        }
                    }
                }
            }
        }

        // 5. Пробеги / Техосмотр
        ExpandableSection(
            title = "Показания одометра (История пробега)",
            icon = Icons.Default.Timeline,
            isExpanded = expandedMileage,
            onToggle = { expandedMileage = !expandedMileage }
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (report.mileages.isEmpty()) {
                    Text("Исторические данные пробега отсутствуют", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                } else {
                    val visibleMileages = if (report.isFullReportPaid) report.mileages else listOf(report.mileages.first())

                    visibleMileages.forEachIndexed { idx, mil ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("${mil.valueKm} км", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                    Text(mil.source, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Text(mil.date, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    if (!report.isFullReportPaid && report.mileages.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.15f)),
                            modifier = Modifier.fillMaxWidth().clickable { onUnlockFullReport() }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Скрыто еще ${report.mileages.size - 1} записи пробега. Разблокировать",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicatorCard(
    title: String,
    isAlert: Boolean,
    alertText: String,
    okText: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isAlert) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.surface

    val contentColor = if (isAlert) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(0.5.dp, if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = if (isAlert) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = if (isAlert) alertText else okText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = if (isAlert) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun PremiumUnlockCard(
    onUnlock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("premium_unlock_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Premium Quality",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Доступен подробный отчет",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Раскройте всю подноготную авто! В подробный отчет входит: проверка участия в коммерческих таксопарках/каршеринге, утилизационные залоги банков РФ, судебные притязания приставов ФССП, залоги и штрафы, карты кузовных повреждений со всех СТО.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onUnlock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_premium_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CreditCard, contentDescription = "Pay")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Разблокировать отчет за 299 ₽", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand toggle"
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    content()
                }
            }
        }
    }
}

@Composable
fun InfoLine(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = valColor,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
