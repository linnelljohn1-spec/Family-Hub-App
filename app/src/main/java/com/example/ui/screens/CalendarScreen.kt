package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: SavingsViewModel,
    onNavigateBack: () -> Unit
) {
    val membersWithPerformance by viewModel.familyPerformance.collectAsStateWithLifecycle()
    val activeMemberId by viewModel.activeMemberId.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()

    val activeMember = membersWithPerformance.find { it.member.id == activeMemberId }?.member
    val isActiveAdmin = activeMember?.isAdmin == true

    val todayCalendar = remember { Calendar.getInstance() }
    val todayDateStr = remember(todayCalendar) {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(todayCalendar.time)
    }

    // Calendar state
    var selectedDate by remember { mutableStateOf(todayDateStr) }
    var selectedCategoryFilter by remember { mutableStateOf("All") } // "All", "Family Outing", "Chore", "Birthday", "Reminder", "Other"
    var showOnlyMyEvents by remember { mutableStateOf(false) }
    var isCalendarExpanded by remember { mutableStateOf(false) }

    // Dynamic Month and Year navigation
    var currentYear by remember { mutableStateOf(todayCalendar.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(todayCalendar.get(Calendar.MONTH)) }

    // Add event dialog state
    var showAddEventDialog by remember { mutableStateOf(false) }

    // Edit event dialog state
    var showEditEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }

    // Delete scope confirmation state (for recurring event occurrences)
    var showDeleteScopeDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<CalendarEvent?>(null) }

    // Helper to generate list of days dynamically for currentYear and currentMonth
    val daysInMonth = remember(currentYear, currentMonth) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        (1..maxDay).map { day ->
            val dateStr = "$currentYear-${(currentMonth + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dayName = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Sun"
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                else -> "Day"
            }
            dayName to dateStr
        }
    }

    // Dynamic month year text display
    val monthYearText = remember(currentYear, currentMonth) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    // Function to handle month changes elegantly and shift selection to the new month
    fun changeMonth(increment: Int) {
        var newMonth = currentMonth + increment
        var newYear = currentYear
        if (newMonth > Calendar.DECEMBER) {
            newMonth = Calendar.JANUARY
            newYear += 1
        } else if (newMonth < Calendar.JANUARY) {
            newMonth = Calendar.DECEMBER
            newYear -= 1
        }
        currentMonth = newMonth
        currentYear = newYear
        
        // Update selectedDate to keep it valid and within the new month
        val oldDay = selectedDate.split("-").lastOrNull()?.toIntOrNull() ?: 4
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, newYear)
            set(Calendar.MONTH, newMonth)
        }
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val newDay = oldDay.coerceAtMost(maxDay)
        selectedDate = "$newYear-${(newMonth + 1).toString().padStart(2, '0')}-${newDay.toString().padStart(2, '0')}"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Family Calendar",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Family Hub"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddEventDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Calendar Event",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEventDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_event")
            ) {
                Icon(imageVector = Icons.Default.Event, contentDescription = "Schedule Event")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Calendar Header & Navigator (July 2026)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { changeMonth(-1) },
                            modifier = Modifier.size(36.dp).testTag("btn_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = monthYearText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { changeMonth(1) },
                            modifier = Modifier.size(36.dp).testTag("btn_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Expand/Collapse Mode Toggle Button
                        TextButton(
                            onClick = { isCalendarExpanded = !isCalendarExpanded },
                            modifier = Modifier.testTag("btn_toggle_calendar_expand")
                        ) {
                            Icon(
                                imageVector = if (isCalendarExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                contentDescription = if (isCalendarExpanded) "Collapse to Strip" else "Expand to Month Grid",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCalendarExpanded) "Strip" else "Month Grid",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (isCalendarExpanded) {
                    // Month Grid View
                    val firstDayOfWeekNum = remember(currentYear, currentMonth) {
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, currentYear)
                            set(Calendar.MONTH, currentMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.
                    }
                    val leadingEmptySlots = firstDayOfWeekNum - 1

                    val gridCells = remember(daysInMonth, leadingEmptySlots) {
                        val list = mutableListOf<Pair<String, String>?>()
                        repeat(leadingEmptySlots) {
                            list.add(null)
                        }
                        daysInMonth.forEach { dayPair ->
                            list.add(dayPair)
                        }
                        list
                    }

                    val weeks = remember(gridCells) {
                        gridCells.chunked(7)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                    ) {
                        // 1. Weekday labels row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val weekdayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            weekdayHeaders.forEach { header ->
                                Text(
                                    text = header,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // 2. Weeks
                        weeks.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                week.forEach { dayPair ->
                                    if (dayPair == null) {
                                        // Empty placeholder
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val (dayName, dateStr) = dayPair
                                        val isSelected = selectedDate == dateStr
                                        val dayNum = dateStr.split("-").last().toInt()

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedDate = dateStr }
                                                .testTag("day_cell_$dayNum"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )

                                                // Event indicator
                                                val eventsOnDay = calendarEvents.any { it.date == dateStr }
                                                if (eventsOnDay) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                                CircleShape
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                // Fill remaining cells in the last week if it is shorter than 7
                                if (week.size < 7) {
                                    repeat(7 - week.size) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Horizontal Days list (Strip View)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        daysInMonth.forEach { (dayName, dateStr) ->
                            val isSelected = selectedDate == dateStr
                            val dayNum = dateStr.split("-").last().toInt()

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedDate = dateStr }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .widthIn(min = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )

                                // Visual indicator for events on this day
                                val eventsOnDay = calendarEvents.any { it.date == dateStr }
                                if (eventsOnDay) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .size(4.dp)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Filters row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category list filters
                val categories = listOf("All", "Family Outing", "Chore", "Birthday", "Reminder", "Other")
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat) }
                    )
                }
            }

            // Secondary Filter for user's own events
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showOnlyMyEvents,
                        onCheckedChange = { showOnlyMyEvents = it }
                    )
                    Text(
                        text = "Show only my profile events",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Show simple count
                val selectedDateFormatted = remember(selectedDate) {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = sdf.parse(selectedDate)
                        if (date != null) {
                            SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(date)
                        } else {
                            selectedDate
                        }
                    } catch (e: Exception) {
                        selectedDate
                    }
                }
                Text(
                    text = "Selected Date: $selectedDateFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            // Events List Header
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Filtered events
            val filteredEvents = calendarEvents.filter { event ->
                val dateMatch = event.date == selectedDate
                val categoryMatch = selectedCategoryFilter == "All" || event.category == selectedCategoryFilter
                val assigneeMatch = if (showOnlyMyEvents) {
                    event.createdByMemberId == activeMemberId
                } else {
                    true
                }
                dateMatch && categoryMatch && assigneeMatch
            }.sortedWith(compareBy<CalendarEvent> { !it.isAllDay }.thenBy { it.time })

            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No Events Scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "There are no events matching your criteria for this day. Tap '+' or the FAB button to add a chore, outing, or reminder.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showAddEventDialog = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Schedule First Event")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEvents) { event ->
                        val creator = membersWithPerformance.find { it.member.id == event.createdByMemberId }?.member
                        val categoryColor = eventCategoryColors[event.category] ?: EventOther

                        val cardBg = if (event.isCompleted) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (event.isCompleted) {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Title, Checkbox (if Chore), and category circle
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (event.category == "Chore") {
                                            val canToggleCompletion = isActiveAdmin || (event.createdByMemberId == -1) || (activeMemberId == event.createdByMemberId)
                                            Checkbox(
                                                checked = event.isCompleted,
                                                onCheckedChange = { viewModel.toggleCalendarEventCompletion(event) },
                                                enabled = canToggleCompletion,
                                                modifier = Modifier.testTag("chore_checkbox_${event.id}")
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(categoryColor, CircleShape)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = event.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = if (event.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                                color = if (event.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (event.category == "Chore" && !(isActiveAdmin || (event.createdByMemberId == -1) || (activeMemberId == event.createdByMemberId))) {
                                                Text(
                                                    text = "ReadOnly: only assignee/admin can toggle",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }

                                    // Edit / Delete Event buttons (Admin or own event)
                                    val canModify = isActiveAdmin || (activeMemberId == event.createdByMemberId)
                                    if (canModify) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    eventToEdit = event
                                                    showEditEventDialog = true
                                                },
                                                modifier = Modifier.size(24.dp).testTag("btn_edit_event_${event.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit event",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            
                                            IconButton(
                                                onClick = {
                                                    if (event.seriesId != null) {
                                                        eventToDelete = event
                                                        showDeleteScopeDialog = true
                                                    } else {
                                                        viewModel.deleteCalendarEvent(event)
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp).testTag("btn_delete_event_${event.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete event",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Description
                                if (event.description.isNotEmpty()) {
                                    Text(
                                        text = event.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(8.dp))

                                // Footer row: Time, Category, Assigned Member
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Time
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (event.isAllDay) Icons.Default.Event else Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = formatEventTime(event.isAllDay, event.time, event.endTime),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Category chip
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = categoryColor.copy(alpha = 0.1f)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = event.category.uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = categoryColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Assignee Badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(creator?.let { parseColorHex(it.avatarColorHex) } ?: MaterialTheme.colorScheme.outline),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = creator?.let { getInitials(it.name) } ?: "ALL",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = creator?.name ?: "Family-wide",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Delete Scope Confirmation Dialog (recurring events) ---
    if (showDeleteScopeDialog && eventToDelete != null) {
        val event = eventToDelete!!
        Dialog(onDismissRequest = { showDeleteScopeDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Delete...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            viewModel.deleteCalendarEventOccurrence(event, applyToWholeSeries = false)
                            showDeleteScopeDialog = false
                            eventToDelete = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_delete_scope_just_this")
                    ) {
                        Text("Just this event")
                    }
                    Button(
                        onClick = {
                            viewModel.deleteCalendarEventOccurrence(event, applyToWholeSeries = true)
                            showDeleteScopeDialog = false
                            eventToDelete = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_delete_scope_this_and_following")
                    ) {
                        Text("This and following events")
                    }
                    TextButton(
                        onClick = {
                            showDeleteScopeDialog = false
                            eventToDelete = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // --- Add Event Dialog ---
    if (showAddEventDialog) {
        var eventTitle by remember { mutableStateOf("") }
        var eventDescription by remember { mutableStateOf("") }
        var eventDate by remember { mutableStateOf(selectedDate) }
        var selectedCategory by remember { mutableStateOf("Family Outing") }
        var selectedRepeatRule by remember { mutableStateOf("NONE") }
        var assignedMemberId by remember { mutableStateOf(-1) } // -1 for All / Family-wide
        
        var isAllDay by remember { mutableStateOf(false) }
        var hasEndTime by remember { mutableStateOf(false) }
        var startTime by remember { mutableStateOf("12:00") }
        var endTime by remember { mutableStateOf("13:00") }

        var showStartTimePickerDialog by remember { mutableStateOf(false) }
        var showEndTimePickerDialog by remember { mutableStateOf(false) }

        var errorText by remember { mutableStateOf("") }
        var showDatePickerDialog by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showAddEventDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Schedule Family Event",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Title
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Event Title (e.g. Lawn Mowing)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_event_title"),
                        singleLine = true
                    )

                    // Description
                    OutlinedTextField(
                        value = eventDescription,
                        onValueChange = { eventDescription = it },
                        label = { Text("Details (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Date Selection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerDialog = true }
                    ) {
                        OutlinedTextField(
                            value = eventDate,
                            onValueChange = { },
                            label = { Text("Event Date") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePickerDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = "Select Date"
                                    )
                                }
                            },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_event_date"),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // All Day Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "All Day Event",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "This event lasts the whole day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isAllDay,
                            onCheckedChange = { isAllDay = it },
                            modifier = Modifier.testTag("switch_all_day")
                        )
                    }

                    if (!isAllDay) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        // Time Selection Layout
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Start Time row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Start Time",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Button(
                                    onClick = { showStartTimePickerDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_select_start_time")
                                ) {
                                    Text(text = formatTo12Hour(startTime))
                                }
                            }

                            // End Time Switch row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = if (hasEndTime) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "Add End Time",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasEndTime) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Switch(
                                    checked = hasEndTime,
                                    onCheckedChange = { hasEndTime = it },
                                    modifier = Modifier.testTag("switch_has_end_time")
                                )
                            }

                            // End Time selection row (only shown if hasEndTime is true)
                            if (hasEndTime) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Spacer(modifier = Modifier.width(24.dp)) // indentation
                                        Text(
                                            text = "End Time",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Button(
                                        onClick = { showEndTimePickerDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("btn_select_end_time")
                                    ) {
                                        Text(text = formatTo12Hour(endTime))
                                    }
                                }
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    // Category Selection Chips
                    Text(
                        text = "Select Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf("Family Outing", "Chore", "Birthday", "Reminder", "Other")
                        categories.forEach { cat ->
                            val isSel = selectedCategory == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    // Repeat Selection Chips
                    Text(
                        text = "Repeat",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val repeatOptions = listOf("NONE" to "None", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly", "YEARLY" to "Yearly")
                        repeatOptions.forEach { (value, label) ->
                            FilterChip(
                                selected = selectedRepeatRule == value,
                                onClick = { selectedRepeatRule = value },
                                label = { Text(label) },
                                modifier = Modifier.testTag("chip_repeat_$value")
                            )
                        }
                    }

                    // Assignee Selection List
                    Text(
                        text = "Assign to Member",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All / Family option
                        val isAllSelected = assignedMemberId == -1
                        Card(
                            onClick = { assignedMemberId = -1 },
                            shape = CircleShape,
                            border = BorderStroke(
                                width = if (isAllSelected) 2.dp else 1.dp,
                                color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Text(
                                text = "General Family",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Specific family members
                        membersWithPerformance.forEach { memberItem ->
                            val m = memberItem.member
                            val isSel = assignedMemberId == m.id
                            val color = parseColorHex(m.avatarColorHex)
                            
                            Card(
                                onClick = { assignedMemberId = m.id },
                                shape = CircleShape,
                                border = BorderStroke(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) color else MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) color.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(color, CircleShape)
                                    )
                                    Text(
                                        text = m.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (errorText.isNotEmpty()) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddEventDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmedTitle = eventTitle.trim()
                                val trimmedDate = eventDate.trim()
                                val dateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
                                
                                if (trimmedTitle.isEmpty()) {
                                    errorText = "Please specify an event title"
                                } else if (!dateRegex.matches(trimmedDate)) {
                                    errorText = "Please enter date in YYYY-MM-DD format (e.g. 2026-07-04)"
                                } else if (!isAllDay && hasEndTime && isEndTimeBeforeStartTime(startTime, endTime)) {
                                    errorText = "End time must be after start time"
                                } else {
                                    val parts = trimmedDate.split("-")
                                    val yr = parts[0].toIntOrNull()
                                    val mo = parts[1].toIntOrNull()
                                    val dy = parts[2].toIntOrNull()
                                    
                                    if (yr == null || mo == null || mo !in 1..12 || dy == null || dy !in 1..31) {
                                        errorText = "Invalid date values. Please check year, month and day values."
                                    } else {
                                        viewModel.addCalendarEvent(
                                            title = trimmedTitle,
                                            description = eventDescription.trim(),
                                            date = trimmedDate,
                                            time = if (isAllDay) "All Day" else startTime.trim(),
                                            category = selectedCategory,
                                            createdByMemberId = assignedMemberId,
                                            isAllDay = isAllDay,
                                            endTime = if (isAllDay || !hasEndTime) null else endTime.trim(),
                                            repeatRule = selectedRepeatRule
                                        )
                                        // Update parent calendar state to focus on newly created event date
                                        selectedDate = trimmedDate
                                        currentYear = yr
                                        currentMonth = mo - 1
                                        showAddEventDialog = false
                                    }
                                }
                            },
                            modifier = Modifier.testTag("btn_save_event")
                        ) {
                            Text("Schedule")
                        }
                    }
                }
            }
        }

        // Overlay Time Pickers
        if (showStartTimePickerDialog) {
            TimePickerDialog(
                title = "Select Start Time",
                initialTime = startTime,
                onDismissRequest = { showStartTimePickerDialog = false },
                onTimeSelected = { selectedTime ->
                    startTime = selectedTime
                    showStartTimePickerDialog = false
                }
            )
        }

        if (showEndTimePickerDialog) {
            TimePickerDialog(
                title = "Select End Time",
                initialTime = endTime,
                onDismissRequest = { showEndTimePickerDialog = false },
                onTimeSelected = { selectedTime ->
                    endTime = selectedTime
                    showEndTimePickerDialog = false
                }
            )
        }

        if (showDatePickerDialog) {
            // Parse current year & month from eventDate if possible, otherwise use calendar defaults
            var pickerYear by remember {
                val yr = eventDate.split("-").firstOrNull()?.toIntOrNull()
                mutableStateOf(yr ?: currentYear)
            }
            var pickerMonth by remember {
                val mo = eventDate.split("-").getOrNull(1)?.toIntOrNull()
                mutableStateOf(if (mo != null) mo - 1 else currentMonth)
            }
            var pickerSelectedDate by remember { mutableStateOf(eventDate) }

            val pickerDays = remember(pickerYear, pickerMonth) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, pickerYear)
                cal.set(Calendar.MONTH, pickerMonth)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                (1..maxDay).map { day ->
                    "$pickerYear-${(pickerMonth + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                }
            }

            val pickerMonthYearText = remember(pickerYear, pickerMonth) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, pickerYear)
                    set(Calendar.MONTH, pickerMonth)
                }
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }

            Dialog(onDismissRequest = { showDatePickerDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select Event Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Navigator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (pickerMonth == Calendar.JANUARY) {
                                        pickerMonth = Calendar.DECEMBER
                                        pickerYear -= 1
                                    } else {
                                        pickerMonth -= 1
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ChevronLeft, "Prev Month")
                            }

                            Text(
                                text = pickerMonthYearText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = {
                                    if (pickerMonth == Calendar.DECEMBER) {
                                        pickerMonth = Calendar.JANUARY
                                        pickerYear += 1
                                    } else {
                                        pickerMonth += 1
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ChevronRight, "Next Month")
                            }
                        }

                        // Weekdays Row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val wkDays = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                            wkDays.forEach { wd ->
                                Text(
                                    text = wd,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Grid
                        val firstDayOfWeek = remember(pickerYear, pickerMonth) {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, pickerYear)
                                set(Calendar.MONTH, pickerMonth)
                                set(Calendar.DAY_OF_MONTH, 1)
                            }
                            cal.get(Calendar.DAY_OF_WEEK)
                        }
                        val emptyLeading = firstDayOfWeek - 1
                        val gridData = remember(pickerDays, emptyLeading) {
                            val list = mutableListOf<String?>()
                            repeat(emptyLeading) { list.add(null) }
                            pickerDays.forEach { list.add(it) }
                            list.chunked(7)
                        }

                        gridData.forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                week.forEach { dayStr ->
                                    if (dayStr == null) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val dayNum = dayStr.split("-").last().toInt()
                                        val isSel = pickerSelectedDate == dayStr
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1.2f)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                                                )
                                                .clickable { pickerSelectedDate = dayStr }
                                                .testTag("picker_day_cell_$dayNum"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                if (week.size < 7) {
                                    repeat(7 - week.size) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showDatePickerDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    eventDate = pickerSelectedDate
                                    showDatePickerDialog = false
                                },
                                modifier = Modifier.testTag("btn_picker_confirm")
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Edit Event Dialog ---
    if (showEditEventDialog && eventToEdit != null) {
        val event = eventToEdit!!
        var eventTitle by remember(event.id) { mutableStateOf(event.title) }
        var eventDescription by remember(event.id) { mutableStateOf(event.description) }
        var eventDate by remember(event.id) { mutableStateOf(event.date) }
        var selectedCategory by remember(event.id) { mutableStateOf(event.category) }
        var selectedRepeatRule by remember(event.id) { mutableStateOf(event.repeatRule) }
        var assignedMemberId by remember(event.id) { mutableStateOf(event.createdByMemberId) }
        var showRecurrenceScopeDialog by remember(event.id) { mutableStateOf(false) }
        var pendingUpdatedEvent by remember(event.id) { mutableStateOf<CalendarEvent?>(null) }
        
        var isAllDay by remember(event.id) { mutableStateOf(event.isAllDay) }
        var hasEndTime by remember(event.id) { mutableStateOf(event.endTime != null) }
        var startTime by remember(event.id) { mutableStateOf(if (event.isAllDay) "12:00" else event.time) }
        var endTime by remember(event.id) { mutableStateOf(event.endTime ?: "13:00") }

        var showEditStartTimePickerDialog by remember { mutableStateOf(false) }
        var showEditEndTimePickerDialog by remember { mutableStateOf(false) }

        var errorText by remember { mutableStateOf("") }
        var showEditDatePickerDialog by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showEditEventDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Edit Scheduled Event",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Title
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Event Title (e.g. Lawn Mowing)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_input_event_title"),
                        singleLine = true
                    )

                    // Description
                    OutlinedTextField(
                        value = eventDescription,
                        onValueChange = { eventDescription = it },
                        label = { Text("Details (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Date Selection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEditDatePickerDialog = true }
                    ) {
                        OutlinedTextField(
                            value = eventDate,
                            onValueChange = { },
                            label = { Text("Event Date") },
                            trailingIcon = {
                                IconButton(onClick = { showEditDatePickerDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = "Select Date"
                                    )
                                }
                            },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_input_event_date"),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // All Day Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "All Day Event",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "This event lasts the whole day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isAllDay,
                            onCheckedChange = { isAllDay = it },
                            modifier = Modifier.testTag("edit_switch_all_day")
                        )
                    }

                    if (!isAllDay) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        // Time Selection Layout
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Start Time row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Start Time",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Button(
                                    onClick = { showEditStartTimePickerDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("edit_btn_select_start_time")
                                ) {
                                    Text(text = formatTo12Hour(startTime))
                                }
                            }

                            // End Time Switch row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = if (hasEndTime) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "Add End Time",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasEndTime) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Switch(
                                    checked = hasEndTime,
                                    onCheckedChange = { hasEndTime = it },
                                    modifier = Modifier.testTag("edit_switch_has_end_time")
                                )
                            }

                            // End Time selection row (only shown if hasEndTime is true)
                            if (hasEndTime) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Spacer(modifier = Modifier.width(24.dp)) // indentation
                                        Text(
                                            text = "End Time",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Button(
                                        onClick = { showEditEndTimePickerDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("edit_btn_select_end_time")
                                    ) {
                                        Text(text = formatTo12Hour(endTime))
                                    }
                                }
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    // Category Selection Chips
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val availableCategories = listOf("Family Outing", "Chore", "Birthday", "Reminder", "Other")
                        availableCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                modifier = Modifier.testTag("edit_chip_$cat")
                            )
                        }
                    }

                    // Repeat Selection Chips - only editable for standalone (non-series) events.
                    // Changing an existing series' repeat rule isn't supported; use "delete
                    // this and following" to end a series instead.
                    Text(
                        text = "Repeat",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (event.seriesId == null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val repeatOptions = listOf("NONE" to "None", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly", "YEARLY" to "Yearly")
                            repeatOptions.forEach { (value, label) ->
                                FilterChip(
                                    selected = selectedRepeatRule == value,
                                    onClick = { selectedRepeatRule = value },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("edit_chip_repeat_$value")
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Part of a recurring series (${event.repeatRule.lowercase()}). Delete \"this and following\" to end it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Assignee Selection List
                    Text(
                        text = "Assign to Member",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All / Family option
                        val isAllSelected = assignedMemberId == -1
                        Card(
                            onClick = { assignedMemberId = -1 },
                            shape = CircleShape,
                            border = BorderStroke(
                                width = if (isAllSelected) 2.dp else 1.dp,
                                color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Text(
                                text = "General Family",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Specific family members
                        membersWithPerformance.forEach { memberItem ->
                            val m = memberItem.member
                            val isSel = assignedMemberId == m.id
                            val color = parseColorHex(m.avatarColorHex)
                            
                            Card(
                                onClick = { assignedMemberId = m.id },
                                shape = CircleShape,
                                border = BorderStroke(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) color else MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) color.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(color, CircleShape)
                                    )
                                    Text(
                                        text = m.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (errorText.isNotEmpty()) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditEventDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmedTitle = eventTitle.trim()
                                val trimmedDate = eventDate.trim()
                                val dateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
                                
                                if (trimmedTitle.isEmpty()) {
                                    errorText = "Please specify an event title"
                                } else if (!dateRegex.matches(trimmedDate)) {
                                    errorText = "Please enter date in YYYY-MM-DD format (e.g. 2026-07-04)"
                                } else if (!isAllDay && hasEndTime && isEndTimeBeforeStartTime(startTime, endTime)) {
                                    errorText = "End time must be after start time"
                                } else {
                                    val parts = trimmedDate.split("-")
                                    val yr = parts[0].toIntOrNull()
                                    val mo = parts[1].toIntOrNull()
                                    val dy = parts[2].toIntOrNull()
                                    
                                    if (yr == null || mo == null || mo !in 1..12 || dy == null || dy !in 1..31) {
                                        errorText = "Invalid date values. Please check year, month and day values."
                                    } else {
                                        val updatedEvent = event.copy(
                                            title = trimmedTitle,
                                            description = eventDescription.trim(),
                                            date = trimmedDate,
                                            time = if (isAllDay) "All Day" else startTime.trim(),
                                            category = selectedCategory,
                                            createdByMemberId = assignedMemberId,
                                            isAllDay = isAllDay,
                                            endTime = if (isAllDay || !hasEndTime) null else endTime.trim(),
                                            repeatRule = selectedRepeatRule,
                                            seriesId = if (selectedRepeatRule == "NONE") null else event.seriesId
                                        )
                                        // Update parent calendar state to focus on newly updated event date
                                        selectedDate = trimmedDate
                                        currentYear = yr
                                        currentMonth = mo - 1

                                        if (event.seriesId != null) {
                                            // Editing a row that's part of an existing series - ask scope first.
                                            pendingUpdatedEvent = updatedEvent
                                            showRecurrenceScopeDialog = true
                                        } else if (selectedRepeatRule != "NONE") {
                                            // Starting a new recurrence from a previously standalone event.
                                            viewModel.addRecurrenceToExistingEvent(updatedEvent, selectedRepeatRule)
                                            showEditEventDialog = false
                                        } else {
                                            viewModel.updateCalendarEvent(updatedEvent)
                                            showEditEventDialog = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.testTag("btn_save_edit_event")
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }

        // Recurrence scope confirmation - shown when saving edits to an event that's part of a series.
        if (showRecurrenceScopeDialog) {
            val updatedEvent = pendingUpdatedEvent
            Dialog(onDismissRequest = { showRecurrenceScopeDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Apply changes to...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                updatedEvent?.let {
                                    viewModel.updateCalendarEventOccurrence(
                                        it.copy(seriesId = null, repeatRule = "NONE"),
                                        applyToWholeSeries = false
                                    )
                                }
                                showRecurrenceScopeDialog = false
                                showEditEventDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_scope_just_this")
                        ) {
                            Text("Just this event")
                        }
                        Button(
                            onClick = {
                                updatedEvent?.let {
                                    viewModel.updateCalendarEventOccurrence(it, applyToWholeSeries = true)
                                }
                                showRecurrenceScopeDialog = false
                                showEditEventDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_scope_this_and_following")
                        ) {
                            Text("This and following events")
                        }
                        TextButton(
                            onClick = { showRecurrenceScopeDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        // Overlay Time Pickers for Edit Dialog
        if (showEditStartTimePickerDialog) {
            TimePickerDialog(
                title = "Select Start Time",
                initialTime = startTime,
                onDismissRequest = { showEditStartTimePickerDialog = false },
                onTimeSelected = { selectedTime ->
                    startTime = selectedTime
                    showEditStartTimePickerDialog = false
                }
            )
        }

        if (showEditEndTimePickerDialog) {
            TimePickerDialog(
                title = "Select End Time",
                initialTime = endTime,
                onDismissRequest = { showEditEndTimePickerDialog = false },
                onTimeSelected = { selectedTime ->
                    endTime = selectedTime
                    showEditEndTimePickerDialog = false
                }
            )
        }

        if (showEditDatePickerDialog) {
            var pickerYear by remember {
                val yr = eventDate.split("-").firstOrNull()?.toIntOrNull()
                mutableStateOf(yr ?: currentYear)
            }
            var pickerMonth by remember {
                val mo = eventDate.split("-").getOrNull(1)?.toIntOrNull()
                mutableStateOf(if (mo != null) mo - 1 else currentMonth)
            }
            var pickerSelectedDate by remember { mutableStateOf(eventDate) }

            val pickerDays = remember(pickerYear, pickerMonth) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, pickerYear)
                cal.set(Calendar.MONTH, pickerMonth)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                (1..maxDay).map { day ->
                    "$pickerYear-${(pickerMonth + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                }
            }

            val pickerMonthYearText = remember(pickerYear, pickerMonth) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, pickerYear)
                    set(Calendar.MONTH, pickerMonth)
                }
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }

            Dialog(onDismissRequest = { showEditDatePickerDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select Event Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Navigator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (pickerMonth == Calendar.JANUARY) {
                                        pickerMonth = Calendar.DECEMBER
                                        pickerYear -= 1
                                    } else {
                                        pickerMonth -= 1
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ChevronLeft, "Prev Month")
                            }

                            Text(
                                text = pickerMonthYearText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = {
                                    if (pickerMonth == Calendar.DECEMBER) {
                                        pickerMonth = Calendar.JANUARY
                                        pickerYear += 1
                                    } else {
                                        pickerMonth += 1
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ChevronRight, "Next Month")
                            }
                        }

                        // Weekdays Row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val wkDays = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                            wkDays.forEach { wd ->
                                Text(
                                    text = wd,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Grid
                        val firstDayOfWeek = remember(pickerYear, pickerMonth) {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, pickerYear)
                                set(Calendar.MONTH, pickerMonth)
                                set(Calendar.DAY_OF_MONTH, 1)
                            }
                            cal.get(Calendar.DAY_OF_WEEK)
                        }
                        val emptyLeading = firstDayOfWeek - 1
                        val gridData = remember(pickerDays, emptyLeading) {
                            val list = mutableListOf<String?>()
                            repeat(emptyLeading) { list.add(null) }
                            pickerDays.forEach { list.add(it) }
                            list.chunked(7)
                        }

                        gridData.forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                week.forEach { dayStr ->
                                    if (dayStr == null) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val dayNum = dayStr.split("-").last().toInt()
                                        val isSel = pickerSelectedDate == dayStr
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1.2f)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                                                )
                                                .clickable { pickerSelectedDate = dayStr }
                                                .testTag("edit_picker_day_cell_$dayNum"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                if (week.size < 7) {
                                    repeat(7 - week.size) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showEditDatePickerDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    eventDate = pickerSelectedDate
                                    showEditDatePickerDialog = false
                                },
                                modifier = Modifier.testTag("edit_btn_picker_confirm")
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers and Custom Dialogs
fun formatTo12Hour(timeStr: String): String {
    return try {
        val parts = timeStr.split(":")
        if (parts.size != 2) return timeStr
        val hour = parts[0].toIntOrNull() ?: return timeStr
        val minute = parts[1].toIntOrNull() ?: return timeStr
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val displayMinute = minute.toString().padStart(2, '0')
        "$displayHour:$displayMinute $amPm"
    } catch (e: Exception) {
        timeStr
    }
}

fun formatEventTime(isAllDay: Boolean, startTime: String, endTime: String?): String {
    if (isAllDay) return "All Day"
    val start12 = formatTo12Hour(startTime)
    return if (!endTime.isNullOrEmpty()) {
        "$start12 - ${formatTo12Hour(endTime)}"
    } else {
        start12
    }
}

fun isEndTimeBeforeStartTime(start: String, end: String): Boolean {
    return try {
        val startParts = start.split(":")
        val endParts = end.split(":")
        val startHour = startParts[0].toInt()
        val startMin = startParts[1].toInt()
        val endHour = endParts[0].toInt()
        val endMin = endParts[1].toInt()
        
        if (endHour < startHour) true
        else if (endHour == startHour && endMin <= startMin) true
        else false
    } catch (e: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    initialTime: String,
    onDismissRequest: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = state)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val formattedTime = "${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}"
                            onTimeSelected(formattedTime)
                        }
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
