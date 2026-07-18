package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM family_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<FamilyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: FamilyTask): Long

    @Update
    suspend fun updateTask(task: FamilyTask)

    @Delete
    suspend fun deleteTask(task: FamilyTask)

    @Query("DELETE FROM family_tasks")
    suspend fun clearTasks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<FamilyTask>)
}
