package com.example.habittracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM Habit ORDER BY id")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM HabitCompletion")
    fun observeCompletions(): Flow<List<HabitCompletion>>

    @Insert
    suspend fun insertHabit(habit: Habit): Long

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCompletion(completion: HabitCompletion)

    @Query("DELETE FROM HabitCompletion WHERE habitId = :habitId AND date = :date")
    suspend fun removeCompletion(habitId: Long, date: Long)

    @Query("SELECT * FROM CachedWeather WHERE id = 1")
    fun observeWeather(): Flow<CachedWeather?>

    @Query("SELECT * FROM CachedWeather WHERE id = 1")
    suspend fun getWeather(): CachedWeather?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeather(weather: CachedWeather)
}
