package com.example.habittracker.data

import kotlinx.coroutines.CancellationException

enum class CityResult { OK, NOT_FOUND, NETWORK_ERROR }

class HabitRepository(
    private val dao: HabitDao,
    private val geocoding: GeocodingApi,
    private val forecast: ForecastApi
) {
    val habits = dao.observeHabits()
    val completions = dao.observeCompletions()
    val weather = dao.observeWeather()

    suspend fun addHabit(name: String, colour: Int, outdoor: Boolean) {
        dao.insertHabit(Habit(name = name.trim(), colour = colour, outdoor = outdoor))
    }

    suspend fun deleteHabit(habit: Habit) = dao.deleteHabit(habit)

    suspend fun setCompleted(habitId: Long, date: Long, completed: Boolean) {
        if (completed) dao.addCompletion(HabitCompletion(habitId, date))
        else dao.removeCompletion(habitId, date)
    }

    /** Looks up a city, fetches its current weather and caches both. */
    suspend fun setCity(query: String, now: Long): CityResult {
        return try {
            val place = geocoding.search(query.trim(), 1).results?.firstOrNull()
                ?: return CityResult.NOT_FOUND
            val current = forecast.forecast(place.latitude, place.longitude, "temperature_2m,weather_code").current
            dao.saveWeather(
                CachedWeather(
                    city = place.name,
                    latitude = place.latitude,
                    longitude = place.longitude,
                    temperatureC = current.temperature,
                    weatherCode = current.weatherCode,
                    fetchedAt = now
                )
            )
            CityResult.OK
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CityResult.NETWORK_ERROR
        }
    }

    /** Refreshes weather for the saved city if the cache is over an hour old. False = network failed. */
    suspend fun refreshWeather(now: Long): Boolean {
        val cached = dao.getWeather() ?: return true // no city chosen yet
        if (now - cached.fetchedAt < 60 * 60 * 1000L) return true
        return try {
            val current = forecast.forecast(cached.latitude, cached.longitude, "temperature_2m,weather_code").current
            dao.saveWeather(
                cached.copy(temperatureC = current.temperature, weatherCode = current.weatherCode, fetchedAt = now)
            )
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }
}
