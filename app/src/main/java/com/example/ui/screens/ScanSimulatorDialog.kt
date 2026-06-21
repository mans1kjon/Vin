package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ScanSimulatorDialog(
    country: String,
    searchType: String,
    onDismiss: () -> Unit,
    onScanResult: (String) -> Unit
) {
    var isScanning by remember { mutableStateOf(false) }
    var detectedText by remember { mutableStateOf<String?>(null) }
    var flashOn by remember { mutableStateOf(false) }

    // Анимация бегущей линии лазера сканера
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Примеры готовых данных для быстрой симуляции
    val presetRuVIN = "XTA211290K0581958"
    val presetTjVIN = "WBAKB81060ED28751"
    val presetRuPlate = "А777АА99"
    val presetTjPlate = "8888AB01"

    val scanTargetLabel = if (searchType == "VIN") "Распознавание VIN-кода" else "Сканирование госномера"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("scan_simulator_dialog"),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Псевдо-камера видоискатель (темный фон с градиентами шума)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF1C1D24), Color(0xFF0C0D10))
                            )
                        )
                )

                // Кнопка закрытия
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(top = 40.dp, start = 16.dp)
                        .align(Alignment.TopStart)
                        .testTag("close_scan_dialog")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Кнопка вспышки
                IconButton(
                    onClick = { flashOn = !flashOn },
                    modifier = Modifier
                        .padding(top = 40.dp, end = 16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Вспышка",
                        tint = if (flashOn) Color.Yellow else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Сетка кадрирования сканера в центре экрана
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = scanTargetLabel,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Поместите ${if (searchType == "VIN") "код ПТС" else "номер авто"} в рамку ниже",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Рамка считывателя
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .border(2.dp, if (detectedText != null) Color.Green else Color.Cyan, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        // Рассекающий луч лазера
                        if (isScanning && detectedText == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .fillMaxHeight(laserYOffset)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Green, Color.Transparent)
                                        )
                                    )
                                    .align(Alignment.TopCenter)
                                    .offset(y = (laserYOffset * 140).dp)
                            )
                        }

                        if (detectedText != null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "ОБНАРУЖЕНО",
                                        color = Color.Green,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        detectedText!!,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (searchType == "VIN") Icons.Default.QrCode else Icons.Default.Pin,
                                    contentDescription = "Placeholder",
                                    tint = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }
                    }
                }

                // Инструкция и демонстрационные пресеты (для удобства тестирования)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .align(Alignment.BottomCenter),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2129)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Интерактивная демо-симуляция OCR",
                            color = Color.Cyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "В реальном устройстве здесь работает Google ML Kit OCR. Нажмите на быстрый шаблон ниже, чтобы сымитировать успешный захват изображения:",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (searchType == "VIN") {
                                DemoScanOption(
                                    text = "VIN (РФ)",
                                    onClick = {
                                        isScanning = true
                                        detectedText = null
                                    },
                                    onScanComplete = {
                                        detectedText = presetRuVIN
                                        onScanResult(presetRuVIN)
                                    },
                                    modifier = Modifier.weight(1f).testTag("demo_scan_vin_ru")
                                )
                                DemoScanOption(
                                    text = "VIN (РТ)",
                                    onClick = {
                                        isScanning = true
                                        detectedText = null
                                    },
                                    onScanComplete = {
                                        detectedText = presetTjVIN
                                        onScanResult(presetTjVIN)
                                    },
                                    modifier = Modifier.weight(1f).testTag("demo_scan_vin_tj")
                                )
                            } else {
                                DemoScanOption(
                                    text = "Номер РФ",
                                    onClick = {
                                        isScanning = true
                                        detectedText = null
                                    },
                                    onScanComplete = {
                                        detectedText = presetRuPlate
                                        onScanResult(presetRuPlate)
                                    },
                                    modifier = Modifier.weight(1f).testTag("demo_scan_plate_ru")
                                )
                                DemoScanOption(
                                    text = "Номер РТ",
                                    onClick = {
                                        isScanning = true
                                        detectedText = null
                                    },
                                    onScanComplete = {
                                        detectedText = presetTjPlate
                                        onScanResult(presetTjPlate)
                                    },
                                    modifier = Modifier.weight(1f).testTag("demo_scan_plate_tj")
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
fun DemoScanOption(
    text: String,
    onClick: () -> Unit,
    onScanComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    Button(
        onClick = {
            onClick()
            scope.launch {
                delay(1500L)
                onScanComplete()
            }
        },
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(Icons.Default.DocumentScanner, contentDescription = "Scan icon", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
