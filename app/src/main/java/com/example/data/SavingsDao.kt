package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {
    @Query("SELECT * FROM family_members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<FamilyMember>>

    @Query("SELECT * FROM family_members WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getMemberByFirestoreId(firestoreId: String): FamilyMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMember): Long

    @Update
    suspend fun updateMember(member: FamilyMember)

    @Delete
    suspend fun deleteMember(member: FamilyMember)

    @Query("SELECT * FROM savings_goals WHERE memberId = :memberId ORDER BY createdAt DESC")
    fun getGoalsForMember(memberId: Int): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals")
    fun getAllGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getGoalByFirestoreId(firestoreId: String): SavingsGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)

    @Query("SELECT * FROM contributions WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun getContributionsForGoal(goalId: Int): Flow<List<Contribution>>

    @Query("SELECT * FROM contributions ORDER BY timestamp DESC")
    fun getAllContributions(): Flow<List<Contribution>>

    @Query("SELECT * FROM contributions WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getContributionByFirestoreId(firestoreId: String): Contribution?

    @Query("DELETE FROM contributions")
    suspend fun clearContributions()

    @Query("DELETE FROM savings_goals")
    suspend fun clearGoals()

    @Query("DELETE FROM family_members")
    suspend fun clearMembers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<FamilyMember>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<SavingsGoal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributions(contributions: List<Contribution>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: Contribution): Long

    @Delete
    suspend fun deleteContribution(contribution: Contribution)
}
