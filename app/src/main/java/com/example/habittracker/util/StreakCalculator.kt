package com.example.habittracker.util

object StreakCalculator {

    /** Consecutive days ending today (or yesterday, if today isn't ticked yet). */
    fun current(dates: Set<Long>, today: Long): Int {
        var day = if (today in dates) today else today - 1
        var count = 0
        while (day in dates) {
            count++
            day--
        }
        return count
    }

    /** Longest run of consecutive days ever recorded. */
    fun best(dates: Set<Long>): Int {
        if (dates.isEmpty()) return 0
        var best = 1
        var run = 1
        val sorted = dates.sorted()
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }
}
