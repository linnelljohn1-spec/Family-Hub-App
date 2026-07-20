package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_events ORDER BY date ASC, time ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE date = :date ORDER BY time ASC")
    fun getEventsByDate(date: String): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getEventByFirestoreId(firestoreId: String): CalendarEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent): Long

    @Query("DELETE FROM calendar_events")
    suspend fun clearEvents()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEvent>)

    @Delete
    suspend fun deleteEvent(event: CalendarEvent)
}
