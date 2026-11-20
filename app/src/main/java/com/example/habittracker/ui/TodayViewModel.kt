package com.example.habittracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.AppDatabase
import com.example.habittracker.data.CachedWeather
import com.example.habittracker.data.CityResult
import com.example.habittracker.data.Habit
import com.example.habittracker.data.HabitRepository
import com.example.habittracker.data.WeatherApis
import com.example.habittracker.util.StreakCalculator
import com.example.habittracker.util.WeatherAdvice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitItem(
    val habit: Habit,
    val doneToday: Boolean,
    val currentStreak: Int,
    val bestStreak: Int
)

data class TodayUiState(
    val habits: List<HabitItem> = emptyList(),
    val weather: CachedWeather? = null,
    val weatherAdvice: String? = null,
    val weatherFailed: Boolean = false,
    val cityError: String? = null
)

private data class Info(
    val weather: CachedWeather?,
    val weatherFailed: Boolean,
    val cityError: String?
)

class TodayViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = HabitRepository(
        AppDatabase.get(app).habitDao(),
        WeatherApis.geocoding,
        WeatherApis.forecast
    )

    private val weatherFailed = MutableStateFlow(false)
    private val cityError = MutableStateFlow<String?>(null)

    private val info = combine(
        repo.weather, weatherFailed, cityError
    ) { w, wf, ce -> Info(w, wf, ce) }

    val uiState: StateFlow<TodayUiState> = combine(
        repo.habits, repo.completions, info
    ) { habits, completions, i ->
        val today = LocalDate.now().toEpochDay()
        val byHabit = completions.groupBy({ it.habitId }, { it.date }).mapValues { it.value.toSet() }
        val items = habits.map { h ->
            val dates = byHabit[h.id].orEmpty()
            HabitItem(
                habit = h,
                doneToday = today in dates,
                currentStreak = StreakCalculator.current(dates, today),
                bestStreak = StreakCalculator.best(dates)
            )
        }
        val pendingOutdoor = items.count { it.habit.outdoor && !it.doneToday }
        TodayUiState(
            habits = items,
            weather = i.weather,
            weatherAdvice = i.weather?.let {
                WeatherAdvice.message(it.weatherCode, it.temperatureC, pendingOutdoor)
            },
            weatherFailed = i.weatherFailed,
            cityError = i.cityError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init {
        refreshWeather()
    }

    fun refreshWeather() = viewModelScope.launch {
        weatherFailed.value = !repo.refreshWeather(System.currentTimeMillis())
    }

    fun setCity(name: String, onSuccess: () -> Unit) = viewModelScope.launch {
        when (repo.setCity(name, System.currentTimeMillis())) {
            CityResult.OK -> {
                cityError.value = null
                weatherFailed.value = false
                onSuccess()
            }
            CityResult.NOT_FOUND -> cityError.value = "City not found. Try another spelling."
            CityResult.NETWORK_ERROR -> cityError.value = "Couldn't connect. Check your internet."
        }
    }

    fun clearCityError() {
        cityError.value = null
    }

    fun addHabit(name: String, colour: Int, outdoor: Boolean) =
        viewModelScope.launch { repo.addHabit(name, colour, outdoor) }

    fun deleteHabit(habit: Habit) = viewModelScope.launch { repo.deleteHabit(habit) }

    fun toggle(item: HabitItem, completed: Boolean) = viewModelScope.launch {
        repo.setCompleted(item.habit.id, LocalDate.now().toEpochDay(), completed)
    }
}
