package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "savings_goals",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMember::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["memberId"])]
)
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memberId: Int,
    val title: String,
    val targetAmount: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val purchaseUrl: String? = null
)
