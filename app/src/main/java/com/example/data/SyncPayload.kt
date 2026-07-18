package com.example.data

data class SyncPayload(
    val members: List<FamilyMember>,
    val goals: List<SavingsGoal>,
    val contributions: List<Contribution>,
    val events: List<CalendarEvent>,
    val tasks: List<FamilyTask> = emptyList(),
    val lastUpdated: Long
)
