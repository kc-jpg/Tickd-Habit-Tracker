package com.example.habittracker.util

/** Turns Open-Meteo (WMO) weather codes into text, emoji and habit advice. */
object WeatherAdvice {

    fun description(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Mostly clear"
        3 -> "Cloudy"
        45, 48 -> "Foggy"
        in 51..57 -> "Drizzly"
        in 61..67 -> "Rainy"
        in 71..77, 85, 86 -> "Snowy"
        in 80..82 -> "Showery"
        in 95..99 -> "Stormy"
        else -> "Mixed weather"
    }

    fun emoji(code: Int): String = when (code) {
        0, 1 -> "☀️"
        2, 3 -> "⛅"
        45, 48 -> "🌫️"
        in 51..67, in 80..82 -> "🌧️"
        in 71..77, 85, 86 -> "❄️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }

    fun isBadForOutdoors(code: Int, temperatureC: Double): Boolean =
        code >= 51 || temperatureC < 2.0

    fun message(code: Int, temperatureC: Double, outdoorPending: Int): String {
        val desc = description(code)
        val temp = Math.round(temperatureC)
        return when {
            isBadForOutdoors(code, temperatureC) && outdoorPending > 0 ->
                "$desc today. Try your indoor habits first and save outdoor ones for later."
            isBadForOutdoors(code, temperatureC) ->
                "$desc today ($temp°C). A good day to stay in."
            outdoorPending > 0 ->
                "$desc and $temp°C. A good day for your outdoor habits!"
            else -> "$desc and $temp°C."
        }
    }
}
