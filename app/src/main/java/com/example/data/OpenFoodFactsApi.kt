package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// Bezpieczne adaptery dla Moshi do obsługi pól tekstowych i liczbowych naprzemiennie
class SafeDoubleAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Double? {
        return when (reader.peek()) {
            JsonReader.Token.NUMBER -> reader.nextDouble()
            JsonReader.Token.STRING -> reader.nextString().toDoubleOrNull()
            JsonReader.Token.NULL -> reader.nextNull()
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(value: Double?): Double? {
        return value
    }
}

class SafeIntAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Int? {
        return when (reader.peek()) {
            JsonReader.Token.NUMBER -> reader.nextInt()
            JsonReader.Token.STRING -> reader.nextString().toIntOrNull()
            JsonReader.Token.NULL -> reader.nextNull()
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(value: Int?): Int? {
        return value
    }
}

// Klasy mapujące odpowiedź z Open Food Facts API v2 z bezpiecznymi zdefiniowanymi typami
@JsonClass(generateAdapter = true)
data class OFFResponse(
    @Json(name = "status") val status: Int?, // Używa SafeIntAdapter w Moshi
    @Json(name = "product") val product: OFFProduct?
)

@JsonClass(generateAdapter = true)
data class OFFProduct(
    @Json(name = "product_name") val name: String?,
    @Json(name = "brands") val brands: String?,
    @Json(name = "image_front_url") val imageUrl: String?,
    @Json(name = "nutriments") val nutriments: OFFNutriments?
)

@JsonClass(generateAdapter = true)
data class OFFNutriments(
    @Json(name = "energy-kcal_100g") val calories100g: Double?, // Używa SafeDoubleAdapter
    @Json(name = "proteins_100g") val proteins100g: Double?,
    @Json(name = "fat_100g") val fat100g: Double?,
    @Json(name = "carbohydrates_100g") val carbs100g: Double?
)

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductInfo(@Path("barcode") barcode: String): OFFResponse

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/"

        fun create(): OpenFoodFactsApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .add(SafeDoubleAdapter())
                .add(SafeIntAdapter())
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(OpenFoodFactsApi::class.java)
        }
    }
}
