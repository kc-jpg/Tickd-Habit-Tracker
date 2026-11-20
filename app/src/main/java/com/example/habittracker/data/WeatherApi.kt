package com.example.habittracker.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Geocoding: turns a city name into coordinates. "results" is missing when nothing matches.
data class GeoResponse(val results: List<GeoResult>?)
data class GeoResult(val name: String, val latitude: Double, val longitude: Double, val country: String?)

// Forecast: nested "current" object
data class ForecastResponse(val current: CurrentWeather)
data class CurrentWeather(
    @Json(name = "temperature_2m") val temperature: Double,
    @Json(name = "weather_code") val weatherCode: Int
)

interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(@Query("name") name: String, @Query("count") count: Int): GeoResponse
}

interface ForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String
    ): ForecastResponse
}

object WeatherApis {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private fun <T> build(baseUrl: String, service: Class<T>): T =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(service)

    val geocoding: GeocodingApi by lazy { build("https://geocoding-api.open-meteo.com/", GeocodingApi::class.java) }
    val forecast: ForecastApi by lazy { build("https://api.open-meteo.com/", ForecastApi::class.java) }
}
