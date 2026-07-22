package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object RecurrenceGenerator {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun parseDate(date: String): Calendar {
        val cal = Calendar.getInstance()
        cal.time = dateFormat.parse(date) ?: throw IllegalArgumentException("Invalid date: $date")
        return cal
    }

    private fun formatDate(cal: Calendar): String = dateFormat.format(cal.time)

    /**
     * Computes the n-th occurrence (n >= 1) from the original anchor date, not by
     * chaining off the previous occurrence, to avoid drift. Monthly/yearly occurrences
     * clamp the day to the target month's actual max day (e.g. Jan 31 -> Feb 28,
     * Feb 29 birthday -> Feb 28 in non-leap years).
     */
    fun occurrenceDate(anchorDate: String, repeatRule: String, n: Int): String {
        val anchor = parseDate(anchorDate)
        val anchorDay = anchor.get(Calendar.DAY_OF_MONTH)
        return when (repeatRule) {
            "WEEKLY" -> {
                val cal = anchor.clone() as Calendar
                cal.add(Calendar.WEEK_OF_YEAR, n)
                formatDate(cal)
            }
            "MONTHLY" -> {
                val cal = anchor.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, n)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, anchorDay.coerceAtMost(maxDay))
                formatDate(cal)
            }
            "YEARLY" -> {
                val cal = anchor.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.YEAR, n)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, anchorDay.coerceAtMost(maxDay))
                formatDate(cal)
            }
            else -> anchorDate
        }
    }

    /** Rolling generation window end date, measured from the given date. */
    fun windowEndDate(repeatRule: String, from: Calendar = Calendar.getInstance()): String {
        val cal = from.clone() as Calendar
        when (repeatRule) {
            "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 8)
            "MONTHLY" -> cal.add(Calendar.MONTH, 12)
            "YEARLY" -> cal.add(Calendar.YEAR, 3)
        }
        return formatDate(cal)
    }
}
