package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MetabolicViewModel(application: Application) : AndroidViewModel(application) {

    // Dane z bazy Room oraz API
    private val database = MetabolicDatabase.getDatabase(application)
    private val api = OpenFoodFactsApi.create()
    private val repository = MetabolicRepository(database, api)

    // Reaktywna lista posiłków na dziś
    val todayMeals: StateFlow<List<MealEntryEntity>> = repository.getMealEntriesForToday()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reaktywna lista produktów w lokalnej bazie
    val localProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reaktywna lista wypitej wody na dziś
    val todayWater: StateFlow<List<WaterEntryEntity>> = repository.getWaterEntriesForToday()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dane użytkownika do obliczeń metabolicznych (pamiętane w sesji lub domyślne)
    var gender by mutableStateOf(Gender.MALE)
    var age by mutableStateOf(28)
    var height by mutableStateOf(175f)
    var weight by mutableStateOf(75f)
    var activityLevel by mutableStateOf(ActivityLevel.MODERATELY_ACTIVE)
    var fitnessGoal by mutableStateOf(FitnessGoal.MAINTAIN)
    var bmrFormula by mutableStateOf(BmrFormula.MIFFLIN_ST_JEOR)
    var dietType by mutableStateOf(DietType.BALANCED)
    var bodyFatPercent by mutableStateOf(20f)
    
    // Spersonalizowane cele wagowe
    var isCustomGoalEnabled by mutableStateOf(false)
    var customGoalType by mutableStateOf(CustomGoalType.LOSE)
    var customTargetWeight by mutableStateOf(70f)
    var customWeeklyRate by mutableStateOf(0.5f) // w kg na tydzień

    // Stan UI dotyczący pobierania kodów kreskowych
    var scannedProduct by mutableStateOf<ProductEntity?>(null)
        private set
    var isSearchingProduct by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        // Przygotowujemy bazę domyślnych produktów spożywczych (seed) na starcie
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Wyliczenia BMR (PPM)
    val bmr: Double
        get() = when (bmrFormula) {
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
        }.toDouble()

    // Wyliczenia TDEE (CPM)
    val tdee: Double
        get() = bmr * activityLevel.multiplier

    // Docelowe dziennie kalorie (uwzględniające cel i poziom bezpieczeństwa)
    val targetCalories: Int
        get() {
            val delta = if (isCustomGoalEnabled) {
                when (customGoalType) {
                    CustomGoalType.LOSE -> -customWeeklyRate * 1100f
                    CustomGoalType.GAIN -> customWeeklyRate * 1100f
                    CustomGoalType.MAINTAIN -> 0f
                }
            } else {
                fitnessGoal.deltaCalories.toFloat()
            }
            return maxOf(1200.0, tdee + delta).roundToInt()
        }

    // Prognoza czasu osiągnięcia celu (w tygodniach)
    val customGoalWeeksToTarget: Double?
        get() {
            if (!isCustomGoalEnabled || customGoalType == CustomGoalType.MAINTAIN) return null
            if (customWeeklyRate <= 0.01f) return null
            val diff = kotlin.math.abs(weight - customTargetWeight)
            return diff.toDouble() / customWeeklyRate
        }

    // Wyznaczenie docelowych limitów makroskładników na podstawie wybranego typu diety
    val targetProteinGrams: Int
        get() = when (dietType) {
            DietType.BALANCED -> (weight * 2.0f).roundToInt()
            DietType.KETO -> (weight * 2.0f).roundToInt()
            DietType.HIGH_PROTEIN -> (weight * 2.5f).roundToInt()
            DietType.LOW_FAT -> (weight * 2.0f).roundToInt()
        }

    val targetFatGrams: Int
        get() = when (dietType) {
            DietType.BALANCED -> ((targetCalories * 0.25f) / 9f).roundToInt()
            DietType.KETO -> {
                val proteinKcal = targetProteinGrams * 4
                val carbsKcal = 40 * 4
                val remainingKcal = maxOf(0, targetCalories - (proteinKcal + carbsKcal))
                (remainingKcal / 9f).roundToInt()
            }
            DietType.HIGH_PROTEIN -> ((targetCalories * 0.20f) / 9f).roundToInt()
            DietType.LOW_FAT -> ((targetCalories * 0.15f) / 9f).roundToInt()
        }

    val targetCarbsGrams: Int
        get() = when (dietType) {
            DietType.BALANCED -> {
                val proteinKcal = targetProteinGrams * 4
                val fatKcal = targetFatGrams * 9
                val remainingKcal = maxOf(0, targetCalories - (proteinKcal + fatKcal))
                (remainingKcal / 4f).roundToInt()
            }
            DietType.KETO -> 40
            DietType.HIGH_PROTEIN -> {
                val proteinKcal = targetProteinGrams * 4
                val fatKcal = targetFatGrams * 9
                val remainingKcal = maxOf(0, targetCalories - (proteinKcal + fatKcal))
                (remainingKcal / 4f).roundToInt()
            }
            DietType.LOW_FAT -> {
                val proteinKcal = targetProteinGrams * 4
                val fatKcal = targetFatGrams * 9
                val remainingKcal = maxOf(0, targetCalories - (proteinKcal + fatKcal))
                (remainingKcal / 4f).roundToInt()
            }
        }

    // Wyszukiwanie lub skanowanie kodu kreskowego
    fun queryBarcode(barcode: String, onCompleted: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            scannedProduct = null
            errorMessage = null
            isSearchingProduct = true
            
            val product = repository.getProductOrCreate(barcode)
            if (product != null) {
                scannedProduct = product
                onCompleted(true)
            } else {
                errorMessage = "Nie znaleziono produktu o kodzie $barcode w bazie danych ani w sieci Open Food Facts."
                onCompleted(false)
            }
            isSearchingProduct = false
        }
    }

    // Dodawanie produktu do dzisiejszego bilansu
    fun logMeal(product: ProductEntity, weightGrams: Double) {
        viewModelScope.launch {
            repository.logMeal(product, weightGrams)
            clearActiveScan()
        }
    }

    // Dodanie własnego (ręcznego) produktu do bazy i natychmiastowe zjedzenie
    fun addNewCustomProductAndLog(
        name: String,
        brand: String,
        calories: Double,
        protein: Double,
        fat: Double,
        carbs: Double,
        portionGrams: Double
    ) {
        viewModelScope.launch {
            val generatedBarcode = "user_" + System.currentTimeMillis()
            val product = ProductEntity(
                barcode = generatedBarcode,
                name = name,
                brand = brand.ifEmpty { "Własny produkt" },
                caloriesPer100g = calories,
                proteinPer100g = protein,
                fatPer100g = fat,
                carbsPer100g = carbs,
                imageUrl = null,
                isCustom = true
            )
            repository.insertCustomProduct(product)
            repository.logMeal(product, portionGrams)
        }
    }

    // Usunięcie wpisu posiłku
    fun removeMealEntry(id: Int) {
        viewModelScope.launch {
            repository.removeMealEntry(id)
        }
    }

    // Wyczyszczenie dzisiejszego logu
    fun clearToday() {
        viewModelScope.launch {
            repository.clearTodayLog()
        }
    }

    fun clearActiveScan() {
        scannedProduct = null
        errorMessage = null
    }

    // Podsumowanie faktycznego spożycia z dzisiejszych wpisów posiłków
    val totalConsumedCalories: Double
        get() = todayMeals.value.sumOf { it.calories }

    val totalConsumedProtein: Double
        get() = todayMeals.value.sumOf { it.protein }

    val totalConsumedFat: Double
        get() = todayMeals.value.sumOf { it.fat }

    val totalConsumedCarbs: Double
        get() = todayMeals.value.sumOf { it.carbs }

    // Logowanie wypitej wody
    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            repository.logWater(amountMl)
        }
    }

    // Usunięcie wpisu wody
    fun removeWaterEntry(id: Int) {
        viewModelScope.launch {
            repository.removeWaterEntry(id)
        }
    }

    // Wyczyszczenie wpisów wody z dziś
    fun clearTodayWater() {
        viewModelScope.launch {
            repository.clearTodayWaterLog()
        }
    }

    val targetWaterMl: Int
        get() = (weight * 35f).roundToInt()

    val totalConsumedWaterMl: Int
        get() = todayWater.value.sumOf { it.amountMl }

    // Obliczenia BMI i pomiary zdrowotne
    val bmi: Double
        get() {
            val hMeter = height / 100.0
            if (hMeter <= 0.0) return 0.0
            return weight / (hMeter * hMeter)
        }

    val bmiCategory: Pair<String, String> // Sformatowany komunikat i kod koloru HEX
        get() {
            val v = bmi
            return when {
                v < 18.5 -> Pair("Niedowaga", "#FFEB3B")
                v < 25.0 -> Pair("Waga prawidłowa", "#4CAF50")
                v < 30.0 -> Pair("Nadwaga", "#FF9800")
                else -> Pair("Otyłość", "#F44336")
            }
        }

    val idealBodyWeight: Double
        get() {
            // Wzór Devine dla Idealnej Masy Ciała
            val inchesOver5Feet = (height / 2.54) - 60.0
            val base = if (gender == Gender.MALE) 50.0 else 45.5
            val calculated = base + (2.3 * maxOf(0.0, inchesOver5Feet))
            return calculated
        }

    // Kalkulator procentu tkanki tłuszczowej metodą US Navy (dla dopasowania wzoru Katch-McArdle)
    fun calculateNavyBodyFat(waistCm: Float, neckCm: Float, hipCm: Float?): Float {
        return if (gender == Gender.MALE) {
            val valSub = waistCm - neckCm
            if (valSub <= 1f) return 15f
            val density = 1.0324f - (0.19106f * kotlin.math.log10(valSub.toDouble())) + (0.15456f * kotlin.math.log10(height.toDouble()))
            if (density <= 0.0) return 15f
            val bf = (495f / density) - 450f
            bf.toFloat().coerceIn(3f, 55f)
        } else {
            val hipValue = hipCm ?: 95f
            val valSub = waistCm + hipValue - neckCm
            if (valSub <= 1f) return 22f
            val density = 1.29579f - (0.35004f * kotlin.math.log10(valSub.toDouble())) + (0.22100f * kotlin.math.log10(height.toDouble()))
            if (density <= 0.0) return 22f
            val bf = (495f / density) - 450f
            bf.toFloat().coerceIn(5f, 60f)
        }
    }
}
