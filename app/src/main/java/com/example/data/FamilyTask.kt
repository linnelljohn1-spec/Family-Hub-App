package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "family_tasks",
    indices = [Index(value = ["firestoreId"], unique = true)]
)
data class FamilyTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val assignedMemberId: Int = -1, // -1 for anyone
    val dueDate: String = "", // "yyyy-MM-dd"
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val firestoreId: String? = null
)
