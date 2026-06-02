package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.ProteinColor
import com.example.ui.theme.FatsColor
import com.example.viewmodel.MetabolicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    MetabolicCalculatorScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetabolicCalculatorScreen(modifier: Modifier = Modifier) {
    val viewModel: MetabolicViewModel = viewModel()
    val context = LocalContext.current

    // Stan bieżącej zakładki
    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0 = Dziennik Diety, 1 = Twój Profil i Parametry

    // Stan dialogów dodawania produktów i skanera
    var showScannerDialog by remember { mutableStateOf(false) }
    var showAddCustomProductDialog by remember { mutableStateOf(false) }

    // Zbieramy dane z ViewModel
    val todayMeals by viewModel.todayMeals.collectAsState()

    // Szybkie statystyki
    val consumedCal = viewModel.totalConsumedCalories
    val consumedProt = viewModel.totalConsumedProtein
    val consumedFat = viewModel.totalConsumedFat
    val consumedCarb = viewModel.totalConsumedCarbs

    val targetCal = viewModel.targetCalories
    val targetProt = viewModel.targetProteinGrams
    val targetFat = viewModel.targetFatGrams
    val targetCarb = viewModel.targetCarbsGrams

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Górny pasek aplikacji z Logo i Nazwą
        AppHeader()

        // Przełącznik zakładek (Tabs) o nowoczesnym wyglądzie
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Zakładka Dziennik",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dziennik Diety", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Zakładka Profil i Kalkulator",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Profil i Kalkulator", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (selectedTab == 0) {
                // ---- ZAKŁADKA 0: DZIENNIK DIETY & BAZA PRODUKTÓW ----
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Karda Podsumowania Kalorii (Cel - Zjedzone = Pozostało)
                    DailyCalorieBalanceCard(
                        targetCal = targetCal,
                        consumedCal = consumedCal.roundToInt(),
                        goal = viewModel.fitnessGoal
                    )

                    // 2. Karta Postępu Makroskładników w czasie rzeczywistym
                    DailyMacrosProgressCard(
                        consumedProt = consumedProt.roundToInt(),
                        targetProt = targetProt,
                        consumedFat = consumedFat.roundToInt(),
                        targetFat = targetFat,
                        consumedCarb = consumedCarb.roundToInt(),
                        targetCarb = targetCarb
                    )

                    // Monitor nawodnienia organizmu
                    WaterTrackerCard(viewModel = viewModel)

                    // 3. Przyciski akcji: Skanowanie kodu EAN / Dodawanie własne
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showScannerDialog = true },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Skanuj ean"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Skanuj / Kod EAN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { showAddCustomProductDialog = true },
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Dodaj własny",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ręczny wpis", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // 4. Lista zjedzonych posiłków dzisiaj
                    Text(
                        text = "Dzisiejsze posiłki",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (todayMeals.isEmpty()) {
                        // Empty State z zachęcającym tekstem i ikoną
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Brak posiłków",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Dziennik posiłków jest pusty",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Dodaj produkty za pomocą skanera kodu kreskowego lub ręcznie, aby zobaczyć bilans i bilansować makroskładniki.",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        // Lista posiłków
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            todayMeals.forEach { meal ->
                                MealLogItem(
                                    meal = meal,
                                    onDeleteClick = {
                                        viewModel.removeMealEntry(meal.id)
                                        Toast.makeText(context, "Usunięto ${meal.name}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Przycisk wyczyszczenia wszystkiego
                            TextButton(
                                onClick = {
                                    viewModel.clearToday()
                                    Toast.makeText(context, "Wyczyszczono dzisiejszy dziennik", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Wyczyść dzień")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Wyczyść dzisiejszy dziennik", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                // ---- ZAKŁADKA 1: PROFIL I PARAMETRY METABOLICZNE (Dotychczasowy Kalkulator) ----
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // TDEE and BMR results visualizer card
                    ResultCard(
                        targetCalories = targetCal,
                        bmr = viewModel.bmr.roundToInt(),
                        tdee = viewModel.tdee.roundToInt(),
                        goal = viewModel.fitnessGoal
                    )

                    // Macro breakdown suggestion
                    MacrosCard(
                        proteinGrams = targetProt,
                        proteinKcal = targetProt * 4,
                        fatGrams = targetFat,
                        fatKcal = targetFat * 9,
                        carbsGrams = targetCarb,
                        carbsKcal = targetCarb * 4,
                        totalKcal = targetCal
                    )

                    // Karta pomiarów parametrów zdrowotnych (BMI, IBW, procent tłuszczu US Navy)
                    BodyMetricsCard(viewModel = viewModel)

                    Text(
                        text = "Twoja Metryka i Cel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    // Wybór płci
                    GenderSelector(
                        selectedGender = viewModel.gender,
                        onGenderSelected = { viewModel.gender = it }
                    )

                    // Wybór Formuły BMR
                    DropdownSelectorCard(
                        title = "Metoda obliczania BMR",
                        value = viewModel.bmrFormula.displayName,
                        description = viewModel.bmrFormula.detailedDesc,
                        icon = Icons.Default.Settings,
                        options = BmrFormula.values().map { it.displayName },
                        optionDescriptions = BmrFormula.values().map { it.detailedDesc },
                        onOptionSelected = { selectedName ->
                            BmrFormula.values().find { it.displayName == selectedName }?.let {
                                viewModel.bmrFormula = it
                            }
                        }
                    )

                    // Conditional Body Fat slider for Katch-McArdle Formula
                    AnimatedVisibility(
                        visible = viewModel.bmrFormula == BmrFormula.KATCH_MCARDLE,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        MetricSliderCard(
                            title = "Poziom tkanki tłuszczowej",
                            value = viewModel.bodyFatPercent,
                            valueRange = 5f..50f,
                            suffix = "%",
                            icon = Icons.Default.FavoriteBorder,
                            onValueChange = { viewModel.bodyFatPercent = it }
                        )
                    }

                    // Suwaki parametrów fizycznych
                    MetricSliderCard(
                        title = "Wiek",
                        value = viewModel.age.toFloat(),
                        valueRange = 15f..90f,
                        suffix = "lat",
                        icon = Icons.Default.DateRange,
                        onValueChange = { viewModel.age = it.roundToInt() }
                    )

                    MetricSliderCard(
                        title = "Wzrost",
                        value = viewModel.height,
                        valueRange = 130f..220f,
                        suffix = "cm",
                        icon = Icons.Default.Info,
                        onValueChange = { viewModel.height = it }
                    )

                    MetricSliderCard(
                        title = "Masa ciała",
                        value = viewModel.weight,
                        valueRange = 40f..150f,
                        suffix = "kg",
                        icon = Icons.Default.Star,
                        onValueChange = { viewModel.weight = it }
                    )

                    // Dropdowns
                    DropdownSelectorCard(
                        title = "Poziom aktywności dobowej",
                        value = viewModel.activityLevel.displayName,
                        description = viewModel.activityLevel.detailedDesc,
                        icon = Icons.Default.PlayArrow,
                        options = ActivityLevel.values().map { it.displayName },
                        optionDescriptions = ActivityLevel.values().map { it.detailedDesc },
                        onOptionSelected = { selectedName ->
                            ActivityLevel.values().find { it.displayName == selectedName }?.let {
                                viewModel.activityLevel = it
                            }
                        }
                    )

                    DropdownSelectorCard(
                        title = "Twój cel dietetyczny",
                        value = viewModel.fitnessGoal.displayName,
                        description = viewModel.fitnessGoal.detailedDesc,
                        icon = Icons.Default.ThumbUp,
                        options = FitnessGoal.values().map { it.displayName },
                        optionDescriptions = FitnessGoal.values().map { it.detailedDesc },
                        onOptionSelected = { selectedName ->
                            FitnessGoal.values().find { it.displayName == selectedName }?.let {
                                viewModel.fitnessGoal = it
                            }
                        }
                    )

                    DropdownSelectorCard(
                        title = "Model diety (rozmieszczenie makroskładników)",
                        value = viewModel.dietType.displayName,
                        description = viewModel.dietType.detailedDesc,
                        icon = Icons.Default.CheckCircle,
                        options = DietType.values().map { it.displayName },
                        optionDescriptions = DietType.values().map { it.detailedDesc },
                        onOptionSelected = { selectedName ->
                            DietType.values().find { it.displayName == selectedName }?.let {
                                viewModel.dietType = it
                            }
                        }
                    )

                    // Podręcznik objaśnień (Knowledge expander)
                    var showExplanation by remember { mutableStateOf(false) }
                    EducationalExpander(
                        isExpanded = showExplanation,
                        onToggle = { showExplanation = !showExplanation }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // ---- DIALOG 1: SKANER KODÓW KRESKOWYCH (INTERAKTYWNA SYMULACJA + POŁĄCZENIE Z OPEN FOOD FACTS API v2) ----
    if (showScannerDialog) {
        Dialog(
            onDismissRequest = {
                viewModel.clearActiveScan()
                showScannerDialog = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                var barcodeInput by remember { mutableStateOf("") }
                var selectedPortionGrams by remember { mutableStateOf(100f) }
                val activeScannedProduct = viewModel.scannedProduct

                // Animacja laserowej linii skanowania
                val infiniteTransition = rememberInfiniteTransition(label = "laser")
                val laserOffsetY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 180f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "laserOffset"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Nagłówek
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Skaner Kodów EAN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = {
                            viewModel.clearActiveScan()
                            showScannerDialog = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Zamknij skaner")
                        }
                    }

                    if (activeScannedProduct == null) {
                        // Skaner jest w trybie celownika
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.Black, RoundedCornerShape(16.dp))
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Grafika Celownika / Ramki skanera
                            Box(
                                modifier = Modifier
                                    .size(width = 240.dp, height = 120.dp)
                                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            ) {
                                // Czerwona linia lasera
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .offset(y = laserOffsetY.dp)
                                        .background(Color.Red)
                                )
                            }

                            // Loading Overlay
                            if (viewModel.isSearchingProduct) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                Text(
                                    text = "Skieruj aparat na kod lub wpisz poniżej",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Informacja o błędzie jeśli nie znaleziono kodu
                        viewModel.errorMessage?.let { error ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Ręczne wpisanie kodu EAN-13
                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = { if (it.length <= 15) barcodeInput = it },
                            label = { Text("Wpisz kod kreskowy (EAN)") },
                            trailingIcon = {
                                if (barcodeInput.isNotEmpty()) {
                                    IconButton(onClick = { barcodeInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Wyczyść tekst")
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Ikona wprowadzania")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (barcodeInput.trim().isNotEmpty()) {
                                        viewModel.queryBarcode(barcodeInput)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = barcodeInput.trim().isNotEmpty() && !viewModel.isSearchingProduct,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Szukaj w bazie Open Food Facts", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Przycisk-skróty dla użytkowników emulatora bez fizycznego aparatu
                        Text(
                            text = "Brak fizycznego kodu? Wypróbuj demonstracyjne skanowanie (klienci chętnie klikają):",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Demo 1: Nutella
                            DemoProductChip(
                                name = "Nutella Krem 350g",
                                brand = "Ferrero",
                                code = "3017620422003",
                                onClick = {
                                    barcodeInput = "3017620422003"
                                    viewModel.queryBarcode("3017620422003")
                                }
                            )
                            // Demo 2: Coca-Cola
                            DemoProductChip(
                                name = "Coca-Cola oryginalna puszka",
                                brand = "The Coca-Cola Company",
                                code = "5900511100147",
                                onClick = {
                                    barcodeInput = "5900511100147"
                                    viewModel.queryBarcode("5900511100147")
                                }
                            )
                            // Demo 3: Pringles Sour Cream
                            DemoProductChip(
                                name = "Pringles Śmietana i Cebula",
                                brand = "Pringles",
                                code = "5053990138722",
                                onClick = {
                                    barcodeInput = "5053990138722"
                                    viewModel.queryBarcode("5053990138722")
                                }
                            )
                            // Demo 4: Pierś z Kurczaka (lokalna)
                            DemoProductChip(
                                name = "Pierś z piersi kurczaka",
                                brand = "Wbudowana baza",
                                code = "1111111111111",
                                onClick = {
                                    barcodeInput = "1111111111111"
                                    viewModel.queryBarcode("1111111111111")
                                }
                            )
                        }
                    } else {
                        // Skanowanie powiodło się, prezentujemy kartę zjedzonego produktu
                        Text(
                            text = "🎉 Produkt znaleziony!",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Główna karta produktu
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Zdjęcie produktu za pomocą Coil
                                activeScannedProduct.imageUrl?.let { imgUrl ->
                                    AsyncImage(
                                        model = imgUrl,
                                        contentDescription = "Zdjęcie produktu",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                    )
                                } ?: Icon(
                                    imageVector = Icons.Default.Fastfood,
                                    contentDescription = "Domyślna ikona jedzenia",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )

                                Text(
                                    text = activeScannedProduct.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                activeScannedProduct.brand?.let { b ->
                                    Text(
                                        text = b,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Text(
                                    text = "Wartości odżywcze na 100g:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )

                                // Siatka makroskładników na 100g
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    NutrientMiniStat(name = "Kcal", value = "${activeScannedProduct.caloriesPer100g.roundToInt()}", color = MaterialTheme.colorScheme.primary)
                                    NutrientMiniStat(name = "Białko", value = "${activeScannedProduct.proteinPer100g}g", color = ProteinColor)
                                    NutrientMiniStat(name = "Tłuszcze", value = "${activeScannedProduct.fatPer100g}g", color = FatsColor)
                                    NutrientMiniStat(name = "Węglowodany", value = "${activeScannedProduct.carbsPer100g}g", color = CarbsColor)
                                }
                            }
                        }

                        // Sekcja wielkości zjedzonej porcji
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Waga zjedzonej porcji",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${selectedPortionGrams.roundToInt()} g",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Slider(
                                    value = selectedPortionGrams,
                                    valueRange = 10f..1000f,
                                    onValueChange = { selectedPortionGrams = it }
                                )

                                // Szybkie przyciski wyboru wagi porcji
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(50f, 100f, 150f, 250f, 500f).forEach { g ->
                                        OutlinedButton(
                                            onClick = { selectedPortionGrams = g },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(0.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("${g.roundToInt()}g", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }

                        // Dodanie do bilansu
                        Button(
                            onClick = {
                                viewModel.logMeal(activeScannedProduct, selectedPortionGrams.toDouble())
                                Toast.makeText(context, "Zapisano ${activeScannedProduct.name} (${selectedPortionGrams.roundToInt()}g)!", Toast.LENGTH_SHORT).show()
                                showScannerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Dodaj posiłek")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dodaj do dzisiejszego bilansu", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Przycisk wstecz
                        TextButton(
                            onClick = { viewModel.clearActiveScan() }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Wróć do skanowania")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Skanuj inny produkt", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ---- DIALOG 2: RĘCZNE DODAJ WŁASNY PRODUKT ----
    if (showAddCustomProductDialog) {
        Dialog(
            onDismissRequest = { showAddCustomProductDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                var cName by remember { mutableStateOf("") }
                var cBrand by remember { mutableStateOf("") }
                var cCalories by remember { mutableStateOf("") }
                var cProtein by remember { mutableStateOf("") }
                var cFat by remember { mutableStateOf("") }
                var cCarbs by remember { mutableStateOf("") }
                var cWeight by remember { mutableStateOf("150") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ręczne dodanie posiłku",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { showAddCustomProductDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Zamknij")
                        }
                    }

                    Text(
                        text = "Wypełnij parametry produktu spożywczego na 100g, a także wagę zjedzonej porcji.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cName,
                        onValueChange = { cName = it },
                        label = { Text("Nazwa posiłku / produktu (np. Banan, Obiad)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cBrand,
                        onValueChange = { cBrand = it },
                        label = { Text("Marka / Restauracja (opcjonalnie)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Wartosci odzywcze
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = cCalories,
                            onValueChange = { cCalories = it },
                            label = { Text("Kcal na 100g") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cWeight,
                            onValueChange = { cWeight = it },
                            label = { Text("Waga porcji (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = cProtein,
                            onValueChange = { cProtein = it },
                            label = { Text("Białko/100g") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cFat,
                            onValueChange = { cFat = it },
                            label = { Text("Tłuszcz/100g") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cCarbs,
                            onValueChange = { cCarbs = it },
                            label = { Text("Węglowodany/100g") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val caloriesVal = cCalories.toDoubleOrNull() ?: 0.0
                            val proteinVal = cProtein.toDoubleOrNull() ?: 0.0
                            val fatVal = cFat.toDoubleOrNull() ?: 0.0
                            val carbsVal = cCarbs.toDoubleOrNull() ?: 0.0
                            val portionGrams = cWeight.toDoubleOrNull() ?: 150.0

                            if (cName.trim().isEmpty()) {
                                Toast.makeText(context, "Podaj nazwę produktu!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.addNewCustomProductAndLog(
                                name = cName,
                                brand = cBrand,
                                calories = caloriesVal,
                                protein = proteinVal,
                                fat = fatVal,
                                carbs = carbsVal,
                                portionGrams = portionGrams
                            )

                            Toast.makeText(context, "Dodano własny posiłek: $cName!", Toast.LENGTH_SHORT).show()
                            showAddCustomProductDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Zapisz")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Zapisz w dzienniku posiłków", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// Komponent 1: Chip dla demo produktów
@Composable
fun DemoProductChip(
    name: String,
    brand: String,
    code: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.12f)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Fastfood,
                contentDescription = "Demo produkt",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("$brand • Kod EAN: $code", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Wybierz",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Komponent 2: Statystka odżywcza w małym kwadracie
@Composable
fun NutrientMiniStat(
    name: String,
    value: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

// Komponent 3: Podsumowanie bilansu kalorycznego
@Composable
fun DailyCalorieBalanceCard(
    targetCal: Int,
    consumedCal: Int,
    goal: FitnessGoal
) {
    val remainingCalStr = (targetCal - consumedCal).toString()
    val isOverLimit = (targetCal - consumedCal) < 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "TWÓJ BILANS DOBOWY",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cel
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$targetCal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Limity (Cel)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }

                // Operator minus
                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))

                // Zjedzone
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$consumedCal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Spożyte", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }

                // Operator równa się
                Text("=", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))

                // Pozostało
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isOverLimit) "Nadwyżka $remainingCalStr" else "$remainingCalStr",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                    )
                    Text("Pozostało", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }

            SuggestionBadge(goal)
        }
    }
}

// Komponent 4: Postęp Makroskładników
@Composable
fun DailyMacrosProgressCard(
    consumedProt: Int,
    targetProt: Int,
    consumedFat: Int,
    targetFat: Int,
    consumedCarb: Int,
    targetCarb: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Wykorzystanie Makroskładników",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Białko
            MacroProgressBarRow(
                name = "Białko",
                consumed = consumedProt,
                target = targetProt,
                color = ProteinColor
            )

            // Tłuszcze
            MacroProgressBarRow(
                name = "Tłuszcze",
                consumed = consumedFat,
                target = targetFat,
                color = FatsColor
            )

            // Węglowodany
            MacroProgressBarRow(
                name = "Węglowodany",
                consumed = consumedCarb,
                target = targetCarb,
                color = CarbsColor
            )
        }
    }
}

// Komponent pomocniczy paska postępu makro
@Composable
fun MacroProgressBarRow(
    name: String,
    consumed: Int,
    target: Int,
    color: Color
) {
    val progress = if (target > 0) consumed / target.toFloat() else 0f
    val displayProgress = progress.coerceIn(0f, 1f)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(
                text = "$consumed / $target g",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(displayProgress)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

// Komponent 5: Karta wpisu na liście obiadów/posiłków
@Composable
fun MealLogItem(
    meal: MealEntryEntity,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ikona jedzenia
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fastfood,
                    contentDescription = "Posiłek",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${meal.brand ?: "Różne marki EAN"} • ${meal.weightGrams.roundToInt()}g",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("B: ${meal.protein.roundToInt()}g", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProteinColor)
                    Text("T: ${meal.fat.roundToInt()}g", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FatsColor)
                    Text("W: ${meal.carbs.roundToInt()}g", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbsColor)
                }
            }

            // Kaloryczność i przycisk usuwania porcji
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${meal.calories.roundToInt()} kcal",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Usuń posiłek",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Metabolix Logo",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "Metabolix",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Kalkulator zapotrzebowania i dziennik diety EAN",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ResultCard(
    targetCalories: Int,
    bmr: Int,
    tdee: Int,
    goal: FitnessGoal
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "TWÓJ CEL KALORYCZNY",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.5.sp
            )

            // Massive calorie indicator ring/glow combo
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$targetCalories",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "kcal / dobę",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Quick Badge and Explanation for goal adjustment
            SuggestionBadge(goal)

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            // Sub levels row (PPM vs CPM)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$bmr kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.onBackground.copy(0.4f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BMR (PPM)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$tdee kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TDEE (CPM)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionBadge(goal: FitnessGoal) {
    val description = when (goal) {
        FitnessGoal.EXTREME_LOSE -> "Uwzględniono agresywny deficyt redukcyjny -600 kcal."
        FitnessGoal.LOSE_WEIGHT -> "Zastosowano optymalny i bezpieczny deficyt -350 kcal."
        FitnessGoal.MAINTAIN -> "Bilans zerowy. Idealny do stabilizacji lub rekompozycji."
        FitnessGoal.LEAN_GAIN -> "Delikatna nadwyżka anaboliczna +200 kcal dla budowy mięśni."
        FitnessGoal.GAIN_WEIGHT -> "Klasyczna nadwyżka na masę +450 kcal wspierająca siłę."
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Goal Badge Icon",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MacrosCard(
    proteinGrams: Int,
    proteinKcal: Int,
    fatGrams: Int,
    fatKcal: Int,
    carbsGrams: Int,
    carbsKcal: Int,
    totalKcal: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sugerowany Podział Makroskładników",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Horizontal visual segmented meter showing relative carbs/protein/fats ratios
            val totalNutrients = (proteinGrams + fatGrams + carbsGrams).toFloat()
            val ratioProtein = if (totalNutrients > 0) proteinGrams / totalNutrients else 0.33f
            val ratioFats = if (totalNutrients > 0) fatGrams / totalNutrients else 0.33f
            val ratioCarbs = if (totalNutrients > 0) carbsGrams / totalNutrients else 0.33f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(CircleShape)
            ) {
                if (ratioProtein > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(ratioProtein)
                            .background(ProteinColor)
                    )
                }
                if (ratioFats > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(ratioFats)
                            .background(FatsColor)
                    )
                }
                if (ratioCarbs > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(ratioCarbs)
                            .background(CarbsColor)
                    )
                }
            }

            // Macros Legend with detailed values
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroRowItem(
                    name = "Białko",
                    subtitle = "Budulec mięśni, regeneracja i sytość",
                    grams = proteinGrams,
                    kcal = proteinKcal,
                    barColor = ProteinColor,
                    percent = if (totalKcal > 0) ((proteinKcal.toFloat() / totalKcal) * 100).roundToInt() else 25
                )

                MacroRowItem(
                    name = "Tłuszcze",
                    subtitle = "Gospodarka hormonalna i energia",
                    grams = fatGrams,
                    kcal = fatKcal,
                    barColor = FatsColor,
                    percent = if (totalKcal > 0) ((fatKcal.toFloat() / totalKcal) * 100).roundToInt() else 25
                )

                MacroRowItem(
                    name = "Węglowodany",
                    subtitle = "Główne źródło siły, regeneracja glikogenu",
                    grams = carbsGrams,
                    kcal = carbsKcal,
                    barColor = CarbsColor,
                    percent = if (totalKcal > 0) ((carbsKcal.toFloat() / totalKcal) * 100).roundToInt() else 50
                )
            }
        }
    }
}

@Composable
fun MacroRowItem(
    name: String,
    subtitle: String,
    grams: Int,
    kcal: Int,
    barColor: Color,
    percent: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored Dot indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(barColor, CircleShape)
        )
        
        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$grams g",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Kcal Tag
        Card(
            colors = CardDefaults.cardColors(
                containerColor = barColor.copy(alpha = 0.12f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$kcal kcal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
                Text(
                    text = "$percent%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun GenderSelector(
    selectedGender: Gender,
    onGenderSelected: (Gender) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Gender.values().forEach { gender ->
            val isSelected = selectedGender == gender
            val backgroundToken = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundToken)
                    .clickable { onGenderSelected(gender) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = gender.icon,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = gender.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun MetricSliderCard(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    suffix: String,
    icon: ImageVector,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "$title Icon",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                }

                // Interactive plus and minus precise control buttons flanking the metric text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onValueChange(maxOf(valueRange.start, value - 1)) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Zmniejsz wartość",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "${value.roundToInt()} $suffix",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = { onValueChange(minOf(valueRange.endInclusive, value + 1)) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Zwiększ wartość",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = value,
                valueRange = valueRange,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    thumbColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DropdownSelectorCard(
    title: String,
    value: String,
    description: String,
    icon: ImageVector,
    options: List<String>,
    optionDescriptions: List<String> = emptyList(),
    onOptionSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$title Icon",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Rozwiń opcje",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    options.forEachIndexed { index, option ->
                        val isSelected = option == value
                        val optDesc = optionDescriptions.getOrNull(index) ?: ""

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOptionSelected(option)
                                    showDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                }
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = option,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (optDesc.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = optDesc,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Zamknij", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun EducationalExpander(
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Education Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vademecum pojęć metabolicznych",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Rozwiń",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    EducationalItem(
                        title = "PPM / BMR (Podstawowa Przemiana Materii)",
                        description = "Ilość energii w kilokaloriach niezbędna do podtrzymania podstawowych funkcji życiowych (takich jak oddychanie, praca serca, krążenie krwi) w stanie całkowitego spoczynku fizycznego i psychicznego."
                    )

                    EducationalItem(
                        title = "Wzór Mifflin-St Jeor",
                        description = "Standardowy, nowoczesny algorytm opierający się na wadze, wzroście, wieku i płci. Jest świetnym punktem wyjścia dla większości populacji."
                    )

                    EducationalItem(
                        title = "Wzór Katch-McArdle",
                        description = "Unikalny wzór oparty na beztłuszczowej masie ciała (LBM). Nie bierze pod uwagę wieku ani wzrostu bezpośrednio, ponieważ ilość suchej masy mięśniowej determinuje wydatek energetyczny. Wysoce zalecany dla sportowców i osób znających swój procent tkanki tłuszczowej."
                    )

                    EducationalItem(
                        title = "Wzór Harris-Benedict (Klasyczny)",
                        description = "Tradycyjny i jeden z pierwszych naukowych wzorów z początku XX wieku. Może nieznacznie zawyżać zapotrzebowanie u osób mniej aktywnych."
                    )

                    EducationalItem(
                        title = "CPM / TDEE (Całkowita Przemiana Materii)",
                        description = "Rzeczywiste dobowe zapotrzebowanie energetyczne organizmu, uwzględniające zarówno funkcje życiowe (PPM), jak i całą aktywność fizyczną w ciągu dnia (w tym pracę zawodową, codzienne obowiązki i zaplanowane treningi)."
                    )

                    EducationalItem(
                        title = "Białko (Budulec)",
                        description = "Kluczowy makroskładnik w diecie. Służy do regeneracji mikrouszkodzeń mięśni, syntezy białek mięśniowych oraz optymalizacji sytości."
                    )

                    EducationalItem(
                        title = "Tłuszcze (Regulacja)",
                        description = "Są nieodzowne do prawidłowej pracy układu nerwowego i hormonalnego, ułatwiają wchłanianie witamin A, D, E, K oraz wspierają odporność organizmu."
                    )

                    EducationalItem(
                        title = "Węglowodany (Paliwo)",
                        description = "Uzupełniają zapasy glikogenu w mięśniach i wątrobie, stanowiąc natychmiastowe paliwo do intensywnej pracy mięśniowej podczas ćwiczeń."
                    )
                }
            }
        }
    }
}

@Composable
fun EducationalItem(title: String, description: String) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            lineHeight = 15.sp
        )
    }
}



@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        MetabolicCalculatorScreen()
    }
}
