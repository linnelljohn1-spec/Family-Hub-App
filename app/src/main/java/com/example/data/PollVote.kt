package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "poll_votes",
    foreignKeys = [
        ForeignKey(
            entity = Poll::class,
            parentColumns = ["id"],
            childColumns = ["pollId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["pollId", "memberId"], unique = true),
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class PollVote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pollId: Int,
    val memberId: Int,
    val optionIndex: Int,
    val votedAt: Long = System.currentTimeMillis(),
    val firestoreId: String? = null
)
