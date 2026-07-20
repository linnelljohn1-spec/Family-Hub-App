package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PollDao {
    @Query("SELECT * FROM polls ORDER BY createdAt DESC")
    fun getAllPolls(): Flow<List<Poll>>

    @Query("SELECT * FROM poll_votes")
    fun getAllVotes(): Flow<List<PollVote>>

    @Query("SELECT * FROM polls WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getPollByFirestoreId(firestoreId: String): Poll?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoll(poll: Poll): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: PollVote): Long

    @Query("DELETE FROM polls WHERE id = :pollId")
    suspend fun deletePollById(pollId: Int)

    @Query("DELETE FROM poll_votes WHERE pollId = :pollId AND memberId = :memberId")
    suspend fun deleteVote(pollId: Int, memberId: Int)

    @Query("DELETE FROM polls")
    suspend fun clearPolls()

    @Query("DELETE FROM poll_votes")
    suspend fun clearVotes()
}
