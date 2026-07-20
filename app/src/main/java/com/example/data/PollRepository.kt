package com.example.data

import kotlinx.coroutines.flow.Flow

class PollRepository(private val pollDao: PollDao) {
    val allPolls: Flow<List<Poll>> = pollDao.getAllPolls()
    val allVotes: Flow<List<PollVote>> = pollDao.getAllVotes()

    suspend fun getPollByFirestoreId(firestoreId: String): Poll? = pollDao.getPollByFirestoreId(firestoreId)

    suspend fun insertPoll(poll: Poll): Long = pollDao.insertPoll(poll)
    suspend fun insertVote(vote: PollVote): Long = pollDao.insertVote(vote)

    suspend fun deletePollById(pollId: Int) = pollDao.deletePollById(pollId)
    suspend fun deleteVote(pollId: Int, memberId: Int) = pollDao.deleteVote(pollId, memberId)
}
