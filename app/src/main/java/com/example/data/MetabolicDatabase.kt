package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entity: Produkt w naszej bazie (wbudowany, pobrany z API lub dodany przez użytkownika)
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val barcode: String, // Dla produktów bez kodu możemy wygenerować np. "user_12345"
    val name: String,
    val brand: String? = null,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val imageUrl: String? = null,
    val isCustom: Boolean = false // Czy wpisane ręcznie przez użytkownika
)

// 2. Entity: Wpis posiłku (Log spożycia)
@Entity(tableName = "meal_entries")
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val calories: Double, // Wyliczone na podstawie gramatury
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val weightGrams: Double,
    val timestamp: Long = System.currentTimeMillis()
)

// 2b. Entity: Wpis wody (Log wypitej wody)
@Entity(tableName = "water_entries")
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

// 3. DAO: Dostęp do danych
@Dao
interface MetabolicDao {
    // Produkty
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE barcode = :barcode")
    suspend fun deleteProduct(barcode: String)

    // Logi posiłków
    @Query("SELECT * FROM meal_entries WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    fun getMealEntriesForDayFlow(startOfDay: Long, endOfDay: Long): Flow<List<MealEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealEntry(entry: MealEntryEntity)

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun deleteMealEntry(id: Int)

    @Query("DELETE FROM meal_entries")
    suspend fun clearAllMealEntries()

    // Logi wody
    @Query("SELECT * FROM water_entries WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    fun getWaterEntriesForDayFlow(startOfDay: Long, endOfDay: Long): Flow<List<WaterEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterEntry(entry: WaterEntryEntity)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteWaterEntry(id: Int)

    @Query("DELETE FROM water_entries")
    suspend fun clearAllWaterEntries()
}

// 4. Database definition
@Database(entities = [ProductEntity::class, MealEntryEntity::class, WaterEntryEntity::class], version = 2, exportSchema = false)
abstract class MetabolicDatabase : RoomDatabase() {
    abstract fun dao(): MetabolicDao

    companion object {
        @Volatile
        private var INSTANCE: MetabolicDatabase? = null

        fun getDatabase(context: Context): MetabolicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MetabolicDatabase::class.java,
                    "metabolic_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
