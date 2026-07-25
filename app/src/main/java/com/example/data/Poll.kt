package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object PollMode {
    const val VOTE = "VOTE"
    const val SPIN = "SPIN"
}

@Entity(
    tableName = "polls",
    indices = [Index(value = ["firestoreId"], unique = true)]
)
data class Poll(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val question: String,
    val options: List<String>,
    val createdByMemberId: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false,
    val mode: String = PollMode.VOTE,
    val spinResultIndex: Int? = null,
    val spinStartedAt: Long? = null,
    val firestoreId: String? = null
)
