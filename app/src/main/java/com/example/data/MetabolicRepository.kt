package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class MetabolicRepository(
    private val database: MetabolicDatabase,
    private val api: OpenFoodFactsApi
) {
    private val dao = database.dao()

    val allProducts: Flow<List<ProductEntity>> = dao.getAllProductsFlow()

    // Pobiera wpisy z aktualnego dnia
    fun getMealEntriesForToday(): Flow<List<MealEntryEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return dao.getMealEntriesForDayFlow(startOfDay, endOfDay)
    }

    private fun toSafeDouble(value: Any?): Double {
        if (value == null) return 0.0
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    // Pobiera produkt o podanym kodzie kreskowym z bazy lokalnej, a jeśli go nie ma - z API Open Food Facts
    suspend fun getProductOrCreate(barcode: String): ProductEntity? = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isEmpty()) return@withContext null

        // 1. Sprawdź lokalną bazę
        val localProduct = dao.getProductByBarcode(cleanBarcode)
        if (localProduct != null) {
            return@withContext localProduct
        }

        // 2. Pobierz z API
        try {
            val response = api.getProductInfo(cleanBarcode)
            val isSuccess = response.status == 1
            if (isSuccess && response.product != null) {
                val p = response.product
                val nut = p.nutriments

                val calories = nut?.calories100g ?: 0.0
                val protein = nut?.proteins100g ?: 0.0
                val fat = nut?.fat100g ?: 0.0
                val carbs = nut?.carbs100g ?: 0.0

                val newProduct = ProductEntity(
                    barcode = cleanBarcode,
                    name = p.name ?: "Nieznany produkt",
                    brand = p.brands ?: "Różne marki",
                    caloriesPer100g = calories,
                    proteinPer100g = protein,
                    fatPer100g = fat,
                    carbsPer100g = carbs,
                    imageUrl = p.imageUrl,
                    isCustom = false
                )

                dao.insertProduct(newProduct)
                return@withContext newProduct
            }
        } catch (e: Throwable) {
            Log.e("MetabolicRepository", "Błąd podczas odpytywania OpenFoodFacts API dla kodu $cleanBarcode", e)
        }

        return@withContext null
    }

    // Dodanie niestandardowego produktu przez użytkownika
    suspend fun insertCustomProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        dao.insertProduct(product)
    }

    // Usuwanie produktu z bazy podręcznej
    suspend fun deleteProduct(barcode: String) = withContext(Dispatchers.IO) {
        dao.deleteProduct(barcode)
    }

    // Zapis wpisu w dzienniku (zjedzenie porcji)
    suspend fun logMeal(product: ProductEntity, weightGrams: Double) = withContext(Dispatchers.IO) {
        val multiplier = weightGrams / 100.0
        val entry = MealEntryEntity(
            barcode = product.barcode,
            name = product.name,
            brand = product.brand,
            calories = product.caloriesPer100g * multiplier,
            protein = product.proteinPer100g * multiplier,
            fat = product.fatPer100g * multiplier,
            carbs = product.carbsPer100g * multiplier,
            weightGrams = weightGrams,
            timestamp = System.currentTimeMillis()
        )
        dao.insertMealEntry(entry)
    }

    // Usuwanie wpisu z dziennika
    suspend fun removeMealEntry(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteMealEntry(id)
    }

    // Czyszczenie dziennika na dzisiaj
    suspend fun clearTodayLog() = withContext(Dispatchers.IO) {
        dao.clearAllMealEntries()
    }

    // Pobiera wpisy wody z aktualnego dnia
    fun getWaterEntriesForToday(): Flow<List<WaterEntryEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return dao.getWaterEntriesForDayFlow(startOfDay, endOfDay)
    }

    // Zapis wpisu wypitej wody
    suspend fun logWater(amountMl: Int) = withContext(Dispatchers.IO) {
        val entry = WaterEntryEntity(
            amountMl = amountMl,
            timestamp = System.currentTimeMillis()
        )
        dao.insertWaterEntry(entry)
    }

    // Usuwanie wpisu wody
    suspend fun removeWaterEntry(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteWaterEntry(id)
    }

    // Czyszczenie wody na dziś
    suspend fun clearTodayWaterLog() = withContext(Dispatchers.IO) {
        dao.clearAllWaterEntries()
    }

    // Wstrzyknięcie domyślnej mini-bazy produktów (seed) jeśli tabela jest pusta,
    // zawierającej również popularne kody kreskowe (EAN) dla ułatwienia testowania.
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        // Dodaj tylko jeśli baza produktów jest całkowicie pusta
        val existing = dao.getProductByBarcode("1111111111111") // sprawdzamy umowny kod
        if (existing == null) {
            val seedList = listOf(
                ProductEntity(
                    barcode = "5900511100147", // Coca Cola Original taste 330ml
                    name = "Coca-Cola Original Juice/Soda",
                    brand = "The Coca-Cola Company",
                    caloriesPer100g = 42.0,
                    proteinPer100g = 0.0,
                    fatPer100g = 0.0,
                    carbsPer100g = 10.6,
                    imageUrl = "https://images.openfoodfacts.org/images/products/590/051/110/0147/front_pl.62.400.jpg",
                    isCustom = false
                ),
                ProductEntity(
                    barcode = "3017620422003", // Nutella 350g lub 600g
                    name = "Krem czekoladowy Nutella",
                    brand = "Ferrero",
                    caloriesPer100g = 539.0,
                    proteinPer100g = 6.3,
                    fatPer100g = 30.9,
                    carbsPer100g = 57.5,
                    imageUrl = "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_fr.464.400.jpg",
                    isCustom = false
                ),
                ProductEntity(
                    barcode = "5053990138722", // Pringles Sour Cream & Onion
                    name = "Pringles Śmietanka z Cebulką",
                    brand = "Pringles",
                    caloriesPer100g = 506.0,
                    proteinPer100g = 4.0,
                    fatPer100g = 32.0,
                    carbsPer100g = 51.0,
                    imageUrl = "https://images.openfoodfacts.org/images/products/505/399/013/8722/front_de.383.400.jpg",
                    isCustom = false
                ),
                ProductEntity(
                    barcode = "1111111111111",
                    name = "Pierś z piersi kurczaka (surowa)",
                    brand = "Standard",
                    caloriesPer100g = 120.0,
                    proteinPer100g = 21.5,
                    fatPer100g = 2.5,
                    carbsPer100g = 0.0,
                    imageUrl = null,
                    isCustom = false
                ),
                ProductEntity(
                    barcode = "2222222222222",
                    name = "Ryż Basmati biały",
                    brand = "Kupiec / Standard",
                    caloriesPer100g = 351.0,
                    proteinPer100g = 7.5,
                    fatPer100g = 1.0,
                    carbsPer100g = 77.0,
                    imageUrl = null,
                    isCustom = false
                ),
                ProductEntity(
                    barcode = "3333333333333",
                    name = "Banan dojrzały (surowy)",
                    brand = "Świeże owoce",
                    caloriesPer100g = 89.0,
                    proteinPer100g = 1.1,
                    fatPer100g = 0.3,
                    carbsPer100g = 22.8,
                    imageUrl = null,
                    isCustom = false
                ),
                ProductEntity(
                    barcode = "4444444444444",
                    name = "Jajko kurze (rozmiar M)",
                    brand = "Wiejskie Jaja",
                    caloriesPer100g = 143.0,
                    proteinPer100g = 12.5,
                    fatPer100g = 9.5,
                    carbsPer100g = 0.7,
                    imageUrl = null,
                    isCustom = false
                ),
                ProductEntity(
                    barcode = "5555555555555",
                    name = "Oliwa z oliwek Extra Virgin",
                    brand = "Monini / Basso",
                    caloriesPer100g = 824.0,
                    proteinPer100g = 0.0,
                    fatPer100g = 91.6,
                    carbsPer100g = 0.0,
                    imageUrl = null,
                    isCustom = false
                ),
                ProductEntity(
                    barcode = "6666666666666",
                    name = "Płatki owsiane górskie",
                    brand = "Melvit / Standard",
                    caloriesPer100g = 366.0,
                    proteinPer100g = 12.0,
                    fatPer100g = 7.0,
                    carbsPer100g = 62.0,
                    imageUrl = null,
                    isCustom = false
                ),
                ProductEntity(
                    barcode = "7777777777777",
                    name = "Skyr naturalny wysokobiałkowy",
                    brand = "Piątnica",
                    caloriesPer100g = 64.0,
                    proteinPer100g = 12.0,
                    fatPer100g = 0.0,
                    carbsPer100g = 4.1,
                    imageUrl = null,
                    isCustom = false
                )
            )

            seedList.forEach { dao.insertProduct(it) }
        }
    }
}
