package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.ProteinColor
import com.example.ui.theme.FatsColor
import kotlin.math.roundToInt

// Enums for precise metabolic computations
enum class Gender(val displayName: String, val icon: String) {
    MALE("Mężczyzna", "♂"),
    FEMALE("Kobieta", "♀")
}

enum class BmrFormula(val displayName: String, val detailedDesc: String) {
    MIFFLIN_ST_JEOR("Mifflin-St Jeor", "Współczesny standard naukowy, zalecany dla większości osób."),
    KATCH_MCARDLE("Katch-McArdle", "Najdokładniejszy dla osób o znanej zawartości tkanki tłuszczowej (oparty na LBM)."),
    HARRIS_BENEDICT("Harris-Benedict (Klasyczny)", "Tradycyjny wzór, niezawodny dla osób średnio aktywnych.")
}

enum class DietType(val displayName: String, val detailedDesc: String) {
    BALANCED("Zrównoważona (Standard)", "Białko: 2.0g/kg, Tłuszcze: 25%, Węglowodany: reszta energii. Ogólnorozwojowa."),
    KETO("Ketogeniczna / Niskowęglowodanowa", "Białko: 2.0g/kg, Węglowodany: do 40g, Tłuszcze: reszta energii (ok. 70%+)."),
    HIGH_PROTEIN("Wysokobiałkowa (Sportowa)", "Białko: 2.5g/kg, Tłuszcze: 20%, Węglowodany: reszta (Dla budujących mięśnie)."),
    LOW_FAT("Niskotłuszczowa", "Białko: 2.0g/kg, Tłuszcze: 15%, Węglowodany: reszta (Wspomaga wydolność tlenową).")
}

enum class ActivityLevel(val multiplier: Double, val displayName: String, val detailedDesc: String) {
    SEDENTARY(1.2, "Brak aktywności (Siedzący)", "Praca przy biurku, brak planowanych treningów"),
    LIGHTLY_ACTIVE(1.375, "Niska aktywność (1-2 tren/tyg)", "Spacery, lekki sport raz lub dwa razy w tygodniu"),
    MODERATELY_ACTIVE(1.55, "Umiarkowana (3-4 tren/tyg)", "Regularne treningi, umiarkowana aktywność codzienna"),
    VERY_ACTIVE(1.725, "Wysoka aktywność (5-6 tren/tyg)", "Intensywne treningi prawie codziennie, aktywny tryb życia"),
    EXTRA_ACTIVE(1.9, "Ekstremalna (Codzienny sport/fizyczna)", "Ciężka praca fizyczna lub codzienne wyczynowe treningi")
}

enum class FitnessGoal(val deltaCalories: Int, val displayName: String, val detailedDesc: String) {
    EXTREME_LOSE(-600, "Szybka redukcja (Duży deficyt)", "Szybka utrata masy ciała. Krótkoterminowa, wymagająca skupienia."),
    LOSE_WEIGHT(-350, "Zdrowa redukcja (Zalecana)", "Bezpieczna i stabilna utrata tkanki tłuszczowej bez utraty mięśni."),
    MAINTAIN(0, "Utrzymanie wagi (Zero)", "Utrzymanie obecnej masy ciała, stabilizacja i regeneracja."),
    LEAN_GAIN(200, "Powolna masa (Lean Bulk)", "Konstruktywna budowa mięśni przy minimalnym przyroście tłuszczu."),
    GAIN_WEIGHT(450, "Szybka masa (Nadwyżka)", "Maksymalny rozwój siły i masy mięśniowej z nadwyżką energii.")
}

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
    // Reactive State
    var gender by rememberSaveable { mutableStateOf(Gender.MALE) }
    var age by rememberSaveable { mutableStateOf(28) }
    var height by rememberSaveable { mutableStateOf(175f) }
    var weight by rememberSaveable { mutableStateOf(75f) }
    var activityLevel by rememberSaveable { mutableStateOf(ActivityLevel.MODERATELY_ACTIVE) }
    var fitnessGoal by rememberSaveable { mutableStateOf(FitnessGoal.MAINTAIN) }
    var bmrFormula by rememberSaveable { mutableStateOf(BmrFormula.MIFFLIN_ST_JEOR) }
    var dietType by rememberSaveable { mutableStateOf(DietType.BALANCED) }
    var bodyFatPercent by rememberSaveable { mutableStateOf(20f) }
    
    // Explanation card collapse status
    var showExplanation by remember { mutableStateOf(false) }

    // Computations using selected Formula
    val bmr = when (bmrFormula) {
        BmrFormula.MIFFLIN_ST_JEOR -> {
            if (gender == Gender.MALE) {
                (10f * weight) + (6.25f * height) - (5f * age) + 5f
            } else {
                (10f * weight) + (6.25f * height) - (5f * age) - 161f
            }
        }
        BmrFormula.KATCH_MCARDLE -> {
            val lbm = weight * (1f - (bodyFatPercent / 100f))
            370f + (21.6f * lbm)
        }
        BmrFormula.HARRIS_BENEDICT -> {
            if (gender == Gender.MALE) {
                66.473f + (13.7516f * weight) + (5.0033f * height) - (6.755f * age)
            } else {
                655.0955f + (9.5634f * weight) + (1.8496f * height) - (4.6756f * age)
            }
        }
    }

    val tdee = bmr * activityLevel.multiplier.toFloat()
    val targetCalories = maxOf(1200f, tdee + fitnessGoal.deltaCalories) // Safe calorie floor

    // Macro breakdowns:
    val proteinGrams: Int
    val proteinKcal: Int
    val fatGrams: Int
    val fatKcal: Int
    val carbsGrams: Int
    val carbsKcal: Int

    when (dietType) {
        DietType.BALANCED -> {
            proteinGrams = (weight * 2.0f).roundToInt()
            proteinKcal = proteinGrams * 4
            fatKcal = (targetCalories * 0.25f).roundToInt()
            fatGrams = (fatKcal / 9f).roundToInt()
            val remainingKcal = maxOf(0f, targetCalories - (proteinKcal + fatKcal))
            carbsGrams = (remainingKcal / 4f).roundToInt()
            carbsKcal = carbsGrams * 4
        }
        DietType.KETO -> {
            proteinGrams = (weight * 2.0f).roundToInt()
            proteinKcal = proteinGrams * 4
            carbsGrams = 40
            carbsKcal = carbsGrams * 4
            val remainingKcal = maxOf(0f, targetCalories - (proteinKcal + carbsKcal))
            fatKcal = remainingKcal.roundToInt()
            fatGrams = (fatKcal / 9f).roundToInt()
        }
        DietType.HIGH_PROTEIN -> {
            proteinGrams = (weight * 2.5f).roundToInt()
            proteinKcal = proteinGrams * 4
            fatKcal = (targetCalories * 0.20f).roundToInt()
            fatGrams = (fatKcal / 9f).roundToInt()
            val remainingKcal = maxOf(0f, targetCalories - (proteinKcal + fatKcal))
            carbsGrams = (remainingKcal / 4f).roundToInt()
            carbsKcal = carbsGrams * 4
        }
        DietType.LOW_FAT -> {
            proteinGrams = (weight * 2.0f).roundToInt()
            proteinKcal = proteinGrams * 4
            fatKcal = (targetCalories * 0.15f).roundToInt()
            fatGrams = (fatKcal / 9f).roundToInt()
            val remainingKcal = maxOf(0f, targetCalories - (proteinKcal + fatKcal))
            carbsGrams = (remainingKcal / 4f).roundToInt()
            carbsKcal = carbsGrams * 4
        }
    }

    val totalCalculatedKcal = proteinKcal + fatKcal + carbsKcal

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        AppHeader()

        // Primary Dynamic Result Dial Card
        ResultCard(
            targetCalories = targetCalories.roundToInt(),
            bmr = bmr.roundToInt(),
            tdee = tdee.roundToInt(),
            goal = fitnessGoal
        )

        // Macronutrients Visual Card
        MacrosCard(
            proteinGrams = proteinGrams,
            proteinKcal = proteinKcal,
            fatGrams = fatGrams,
            fatKcal = fatKcal,
            carbsGrams = carbsGrams,
            carbsKcal = carbsKcal,
            totalKcal = totalCalculatedKcal
        )

        // Form Title
        Text(
            text = "Twoja Metryka i Cel",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        // Gender Selection
        GenderSelector(
            selectedGender = gender,
            onGenderSelected = { gender = it }
        )

        // Formuła BMR
        DropdownSelectorCard(
            title = "Metoda obliczania BMR",
            value = bmrFormula.displayName,
            description = bmrFormula.detailedDesc,
            icon = Icons.Default.Settings,
            options = BmrFormula.values().map { it.displayName },
            optionDescriptions = BmrFormula.values().map { it.detailedDesc },
            onOptionSelected = { selectedName ->
                BmrFormula.values().find { it.displayName == selectedName }?.let {
                    bmrFormula = it
                }
            }
        )

        // Conditional Body Fat slider for Katch-McArdle Formula
        AnimatedVisibility(
            visible = bmrFormula == BmrFormula.KATCH_MCARDLE,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            MetricSliderCard(
                title = "Poziom tkanki tłuszczowej",
                value = bodyFatPercent,
                valueRange = 5f..50f,
                suffix = "%",
                icon = Icons.Default.FavoriteBorder,
                onValueChange = { bodyFatPercent = it }
            )
        }

        // Metrics Input Cards
        MetricSliderCard(
            title = "Wiek",
            value = age.toFloat(),
            valueRange = 15f..90f,
            suffix = "lat",
            icon = Icons.Default.DateRange,
            onValueChange = { age = it.roundToInt() }
        )

        MetricSliderCard(
            title = "Wzrost",
            value = height,
            valueRange = 130f..220f,
            suffix = "cm",
            icon = Icons.Default.Info,
            onValueChange = { height = it }
        )

        MetricSliderCard(
            title = "Masa ciała",
            value = weight,
            valueRange = 40f..150f,
            suffix = "kg",
            icon = Icons.Default.Star,
            onValueChange = { weight = it }
        )

        // Dropdown Parameter Selectors
        DropdownSelectorCard(
            title = "Poziom aktywności dobowej",
            value = activityLevel.displayName,
            description = activityLevel.detailedDesc,
            icon = Icons.Default.PlayArrow,
            options = ActivityLevel.values().map { it.displayName },
            optionDescriptions = ActivityLevel.values().map { it.detailedDesc },
            onOptionSelected = { selectedName ->
                ActivityLevel.values().find { it.displayName == selectedName }?.let {
                    activityLevel = it
                }
            }
        )

        DropdownSelectorCard(
            title = "Twój cel dietetyczny",
            value = fitnessGoal.displayName,
            description = fitnessGoal.detailedDesc,
            icon = Icons.Default.ThumbUp,
            options = FitnessGoal.values().map { it.displayName },
            optionDescriptions = FitnessGoal.values().map { it.detailedDesc },
            onOptionSelected = { selectedName ->
                FitnessGoal.values().find { it.displayName == selectedName }?.let {
                    fitnessGoal = it
                }
            }
        )

        DropdownSelectorCard(
            title = "Model diety (rozmieszczenie makroskładników)",
            value = dietType.displayName,
            description = dietType.detailedDesc,
            icon = Icons.Default.CheckCircle,
            options = DietType.values().map { it.displayName },
            optionDescriptions = DietType.values().map { it.detailedDesc },
            onOptionSelected = { selectedName ->
                DietType.values().find { it.displayName == selectedName }?.let {
                    dietType = it
                }
            }
        )

        // Knowledge Expander
        EducationalExpander(
            isExpanded = showExplanation,
            onToggle = { showExplanation = !showExplanation }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Fit Flame Logo",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Metabolix",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Kalkulator zapotrzebowania kalorycznego",
                fontSize = 11.sp,
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
                fontSize = 12.sp,
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
                    subtitle = "Budulec mięśni, regeneracja i sytość (2.0g/kg)",
                    grams = proteinGrams,
                    kcal = proteinKcal,
                    barColor = ProteinColor,
                    percent = if (totalKcal > 0) ((proteinKcal.toFloat() / totalKcal) * 100).roundToInt() else 25
                )

                MacroRowItem(
                    name = "Tłuszcze",
                    subtitle = "Gospodarka hormonalna i energia (25%)",
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
