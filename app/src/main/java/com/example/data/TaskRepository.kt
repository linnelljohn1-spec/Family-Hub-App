package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<FamilyTask>> = taskDao.getAllTasks()

    suspend fun getTaskByFirestoreId(firestoreId: String): FamilyTask? = taskDao.getTaskByFirestoreId(firestoreId)

    suspend fun insertTask(task: FamilyTask): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: FamilyTask) = taskDao.updateTask(task)
    suspend fun deleteTask(task: FamilyTask) = taskDao.deleteTask(task)

    suspend fun clearAll() {
        taskDao.clearTasks()
    }

    suspend fun insertAll(tasks: List<FamilyTask>) {
        taskDao.insertTasks(tasks)
    }
}
