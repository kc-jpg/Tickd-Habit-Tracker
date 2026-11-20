package com.example.habittracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colour: Int,              // ARGB value
    val targetDays: Int = 127,    // bitmask: bit 0 = Monday ... bit 6 = Sunday
    val outdoor: Boolean = false  // used for weather-based tips
)

/** One row per habit per day completed. Dates are stored as LocalDate epoch days. */
@Entity(
    primaryKeys = ["habitId", "date"],
    foreignKeys = [ForeignKey(
        entity = Habit::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId")]
)
data class HabitCompletion(
    val habitId: Long,
    val date: Long
)

/** Single-row cache of the user's city and its latest weather, available offline. */
@Entity
data class CachedWeather(
    @PrimaryKey val id: Int = 1,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val weatherCode: Int,
    val fetchedAt: Long
)
