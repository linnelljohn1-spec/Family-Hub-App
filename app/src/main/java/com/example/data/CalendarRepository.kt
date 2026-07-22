package com.example.data

import kotlinx.coroutines.flow.Flow

class CalendarRepository(private val calendarDao: CalendarDao) {
    val allEvents: Flow<List<CalendarEvent>> = calendarDao.getAllEvents()

    fun getEventsByDate(date: String): Flow<List<CalendarEvent>> = calendarDao.getEventsByDate(date)

    suspend fun getEventByFirestoreId(firestoreId: String): CalendarEvent? = calendarDao.getEventByFirestoreId(firestoreId)

    suspend fun getEventById(id: Int): CalendarEvent? = calendarDao.getEventById(id)

    suspend fun getEventsBySeriesId(seriesId: String): List<CalendarEvent> = calendarDao.getEventsBySeriesId(seriesId)

    suspend fun insertEvent(event: CalendarEvent): Long = calendarDao.insertEvent(event)

    suspend fun deleteEvent(event: CalendarEvent) = calendarDao.deleteEvent(event)

    suspend fun clearAll() {
        calendarDao.clearEvents()
    }

    suspend fun insertAll(events: List<CalendarEvent>) {
        calendarDao.insertEvents(events)
    }
}
