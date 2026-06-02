package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Gender
import com.example.viewmodel.MetabolicViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WaterTrackerCard(viewModel: MetabolicViewModel) {
    val targetWaterMl = viewModel.targetWaterMl
    val consumedWaterMl = viewModel.totalConsumedWaterMl
    val todayWaterEntries by viewModel.todayWater.collectAsState()
    val context = LocalContext.current

    val progress = if (targetWaterMl > 0) {
        (consumedWaterMl.toFloat() / targetWaterMl).coerceIn(0f, 1f)
    } else 0f

    var showHistory by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nagłówek Nawodnienia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Ikona nawodnienia",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Monitor Nawodnienia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${consumedWaterMl} / ${targetWaterMl} ml",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color(0xFF1E88E5)
                )
            }

            // Wizualny Pasek Postępu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFFE3F2FD))
            ) {
                // Niebieska woda
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF64B5F6), Color(0xFF2196F3))
                            )
                        )
                )

                // Tekst %
                Text(
                    text = "${(progress * 100).roundToInt()}% zrealizowane",
                    modifier = Modifier.align(Alignment.Center),
                    color = if (progress > 0.4f) Color.White else Color(0xFF1E88E5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Przyciski akcji (Szybkie dodawanie wody)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Szklanka 250ml
                Button(
                    onClick = {
                        viewModel.logWater(250)
                        Toast.makeText(context, "Dodano 250 ml wody \uD83E\uDD5B", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF1E88E5)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("\uD83E\uDD5B +250 ml", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Średnia szklanka / kubek 350ml
                Button(
                    onClick = {
                        viewModel.logWater(350)
                        Toast.makeText(context, "Dodano 350 ml wody \uD83E\uDD64", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1.1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBDEFB), contentColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("\uD83E\uDD64 +350 ml", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Butelka 500ml
                Button(
                    onClick = {
                        viewModel.logWater(500)
                        Toast.makeText(context, "Dodano 500 ml wody \uD83D\uDCA7", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90CAF9), contentColor = Color(0xFF0D47A1)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("\uD83D\uDCA7 +500 ml", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Opcje zaawansowane: Wyczyszczenie i Historia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showHistory = !showHistory },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1976D2))
                ) {
                    Icon(
                        imageVector = if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Pokaż historię"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showHistory) "Ukryj wpisy" else "Pokaż historię wody (${todayWaterEntries.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (todayWaterEntries.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.clearTodayWater()
                            Toast.makeText(context, "Zresetowano nawodnienie", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Zresetuj wodę", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zresetuj", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(visible = showHistory) {
                if (todayWaterEntries.isEmpty()) {
                    Text(
                        text = "Brak dzisiejszych wpisów o nawodnieniu.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        todayWaterEntries.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💧", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${entry.amountMl} ml",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.removeWaterEntry(entry.id)
                                        Toast.makeText(context, "Usunięto wpis wody", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Usuń wpis wody",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.61f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BodyMetricsCard(viewModel: MetabolicViewModel) {
    val bmr = viewModel.bmr
    val tdee = viewModel.tdee
    val bmi = viewModel.bmi
    val (bmiLabel, bmiHex) = viewModel.bmiCategory
    val idealWeightVal = viewModel.idealBodyWeight
    val context = LocalContext.current

    // US Navy calculator state
    var showNavyCalc by remember { mutableStateOf(false) }
    var waistInput by remember { mutableStateOf("") }
    var neckInput by remember { mutableStateOf("") }
    var hipInput by remember { mutableStateOf("") } // female only
    var calculatedBfResult by remember { mutableStateOf<Float?>(null) }

    val formattedBmhHex = Color(android.graphics.Color.parseColor(bmiHex))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Nagłówek panelu zdrowia
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = "Pomiary zdrowia",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Parametry Zdrowotne",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Siatka ważnych wskaźników (BMI i Idealna Waga)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Karta BMI
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Twoje BMI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.1f", bmi),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(formattedBmhHex.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = bmiLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = formattedBmhHex
                            )
                        }
                    }
                }

                // Karta Idealnej Masy Ciała
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Idealna Waga (IBW)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.1f kg", idealWeightVal),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Wzór Devine",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Sekcja estymatora tkanki tłuszczowej US Navy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Estymator Tkanki Tłuszczowej",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Metoda US Navy (szyja + pas)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = { showNavyCalc = !showNavyCalc },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = if (showNavyCalc) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Kalkulator tkanki tłuszczowej",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = showNavyCalc) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.015f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Wprowadź obwody ciała w cm (niezbędne do oszacowania tkanki tłuszczowej i obliczenia dokładnego BMR metodą Katch-McArdle):",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = waistInput,
                            onValueChange = { waistInput = it },
                            label = { Text("Obwód pasa (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = neckInput,
                            onValueChange = { neckInput = it },
                            label = { Text("Obwód szyi (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (viewModel.gender == Gender.FEMALE) {
                        OutlinedTextField(
                            value = hipInput,
                            onValueChange = { hipInput = it },
                            label = { Text("Obwód bioder (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val waistVal = waistInput.toFloatOrNull()
                                val neckVal = neckInput.toFloatOrNull()
                                val hipVal = hipInput.toFloatOrNull()
                                
                                if (waistVal != null && neckVal != null) {
                                    val bf = viewModel.calculateNavyBodyFat(waistVal, neckVal, hipVal)
                                    calculatedBfResult = bf
                                } else {
                                    Toast.makeText(context, "Proszę uzupełnić wszystkie obwody prawidłowo!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("Oszacuj BF%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (calculatedBfResult != null) {
                            OutlinedButton(
                                onClick = {
                                    waistInput = ""
                                    neckInput = ""
                                    hipInput = ""
                                    calculatedBfResult = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("Wyczyść", fontSize = 11.sp)
                            }
                        }
                    }

                    calculatedBfResult?.let { bfResult ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Oszacowany poziom tkanki tłuszczowej: " + String.format("%.1f%%", bfResult),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Button(
                                    onClick = {
                                        viewModel.bodyFatPercent = bfResult
                                        loggerApplyBfToast(context, bfResult)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Zapisz BF", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Zastosuj ten % tłuszczu w profilu", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loggerApplyBfToast(context: android.content.Context, value: Float) {
    Toast.makeText(context, String.format("Zastosowano %.1f%% tkanki tłuszczowej w Twoim profilu!", value), Toast.LENGTH_LONG).show()
}
