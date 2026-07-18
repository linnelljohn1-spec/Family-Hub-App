package com.example.data

import kotlinx.coroutines.flow.Flow

class SavingsRepository(private val savingsDao: SavingsDao) {
    val allMembers: Flow<List<FamilyMember>> = savingsDao.getAllMembers()
    val allGoals: Flow<List<SavingsGoal>> = savingsDao.getAllGoals()
    val allContributions: Flow<List<Contribution>> = savingsDao.getAllContributions()

    fun getGoalsForMember(memberId: Int): Flow<List<SavingsGoal>> = savingsDao.getGoalsForMember(memberId)
    fun getContributionsForGoal(goalId: Int): Flow<List<Contribution>> = savingsDao.getContributionsForGoal(goalId)

    suspend fun insertMember(member: FamilyMember): Long = savingsDao.insertMember(member)
    suspend fun updateMember(member: FamilyMember) = savingsDao.updateMember(member)
    suspend fun deleteMember(member: FamilyMember) = savingsDao.deleteMember(member)

    suspend fun insertGoal(goal: SavingsGoal): Long = savingsDao.insertGoal(goal)
    suspend fun updateGoal(goal: SavingsGoal) = savingsDao.updateGoal(goal)
    suspend fun deleteGoal(goal: SavingsGoal) = savingsDao.deleteGoal(goal)

    suspend fun insertContribution(contribution: Contribution): Long = savingsDao.insertContribution(contribution)
    suspend fun deleteContribution(contribution: Contribution) = savingsDao.deleteContribution(contribution)

    suspend fun clearAll() {
        savingsDao.clearContributions()
        savingsDao.clearGoals()
        savingsDao.clearMembers()
    }

    suspend fun insertAll(
        members: List<FamilyMember>,
        goals: List<SavingsGoal>,
        contributions: List<Contribution>
    ) {
        savingsDao.insertMembers(members)
        savingsDao.insertGoals(goals)
        savingsDao.insertContributions(contributions)
    }
}
