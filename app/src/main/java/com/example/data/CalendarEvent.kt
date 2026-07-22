package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_events",
    indices = [Index(value = ["firestoreId"], unique = true)]
)
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val date: String, // "yyyy-MM-dd"
    val time: String, // "HH:mm"
    val category: String, // "Family Outing", "Chore", "Birthday", "Reminder", "Other"
    val createdByMemberId: Int, // -1 for All / General Family, or specific member ID
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val isAllDay: Boolean = false,
    val endTime: String? = null,
    val firestoreId: String? = null,
    val repeatRule: String = "NONE", // "NONE", "WEEKLY", "MONTHLY", "YEARLY"
    val seriesId: String? = null // shared across all occurrences of one recurring series; null if standalone
)
