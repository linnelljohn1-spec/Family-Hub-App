package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "poll_options",
    foreignKeys = [
        ForeignKey(
            entity = Poll::class,
            parentColumns = ["id"],
            childColumns = ["pollId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["pollId"]),
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class PollOption(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pollId: Int,
    val text: String,
    val createdByMemberId: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val firestoreId: String? = null
)
