package com.example.data

import kotlinx.coroutines.flow.Flow

class PollRepository(private val pollDao: PollDao) {
    val allPolls: Flow<List<Poll>> = pollDao.getAllPolls()
    val allVotes: Flow<List<PollVote>> = pollDao.getAllVotes()
    val allOptions: Flow<List<PollOption>> = pollDao.getAllOptions()

    suspend fun getPollByFirestoreId(firestoreId: String): Poll? = pollDao.getPollByFirestoreId(firestoreId)
    suspend fun getPollOptionByFirestoreId(firestoreId: String): PollOption? = pollDao.getPollOptionByFirestoreId(firestoreId)

    suspend fun insertPoll(poll: Poll): Long = pollDao.insertPoll(poll)
    suspend fun insertVote(vote: PollVote): Long = pollDao.insertVote(vote)
    suspend fun insertOption(option: PollOption): Long = pollDao.insertOption(option)

    suspend fun deletePollById(pollId: Int) = pollDao.deletePollById(pollId)
    suspend fun deleteVote(pollId: Int, memberId: Int) = pollDao.deleteVote(pollId, memberId)
    suspend fun deleteOptionById(optionId: Int) = pollDao.deleteOptionById(optionId)
}
