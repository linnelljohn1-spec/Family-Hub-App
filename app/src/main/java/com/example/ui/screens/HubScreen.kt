package com.example.ui.screens
 
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private data class HubModule(
    val testTag: String,
    val icon: ImageVector,
    val accent: Color,
    val container: Color,
    val label: String,
    val badgeCount: Int,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    viewModel: SavingsViewModel,
    onNavigateToSavings: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToPolls: () -> Unit,
    unreadMessagesCount: Int,
    openPollsCount: Int,
    onSwitchProfileClick: () -> Unit
) {
    val membersWithPerformance by viewModel.familyPerformance.collectAsStateWithLifecycle()
    val activeMemberId by viewModel.activeMemberId.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()

    val activeMember = membersWithPerformance.find { it.member.id == activeMemberId }?.member

    var showSyncDialog by remember { mutableStateOf(false) }

    // Filter upcoming events (today and future)
    val upcomingEvents = remember(calendarEvents) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        calendarEvents.filter { it.date >= todayStr }
            .sortedWith(compareBy<CalendarEvent> { it.date }.thenBy { !it.isAllDay }.thenBy { it.time })
            .take(3)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("hub_title_row")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Family Hub",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    // Cloud Sync Action
                    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
                    val syncIcon = if (isConnected) Icons.Default.Cloud else Icons.Default.CloudQueue
                    val syncTint = if (isConnected) AppTheme.extendedColors.success else MaterialTheme.colorScheme.onSurfaceVariant

                    IconButton(
                        onClick = { showSyncDialog = true },
                        modifier = Modifier.testTag("hub_sync_button")
                    ) {
                        Icon(
                            imageVector = syncIcon,
                            contentDescription = "Cloud Sync Settings",
                            tint = syncTint
                        )
                    }

                    // Profile Chip in top right
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(activeMember?.let { parseColorHex(it.avatarColorHex) } ?: MaterialTheme.colorScheme.primary)
                            .clickable { onSwitchProfileClick() }
                            .testTag("hub_profile_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeMember?.let { getInitials(it.name) } ?: "JL",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Welcoming Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Hello, ${activeMember?.name ?: "Family"}! 👋",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Welcome back to your central family organizer. Coordinated finance tracking, shared scheduling, and goal setting in one place.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Launch Modules - compact square buttons
            Text(
                text = "Launch Modules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            val hubModules = remember(unreadMessagesCount, openPollsCount) {
                listOf(
                    HubModule(
                        testTag = "launcher_savings",
                        icon = Icons.Default.TrendingUp,
                        accent = FeatureColors.savings,
                        container = FeatureColors.savingsContainer,
                        label = "Savings",
                        badgeCount = 0,
                        onClick = onNavigateToSavings
                    ),
                    HubModule(
                        testTag = "launcher_calendar",
                        icon = Icons.Default.CalendarMonth,
                        accent = FeatureColors.calendar,
                        container = FeatureColors.calendarContainer,
                        label = "Calendar",
                        badgeCount = 0,
                        onClick = onNavigateToCalendar
                    ),
                    HubModule(
                        testTag = "launcher_tasks",
                        icon = Icons.Default.CheckCircle,
                        accent = FeatureColors.tasks,
                        container = FeatureColors.tasksContainer,
                        label = "Tasks",
                        badgeCount = 0,
                        onClick = onNavigateToTasks
                    ),
                    HubModule(
                        testTag = "launcher_chat",
                        icon = Icons.Default.Chat,
                        accent = FeatureColors.chat,
                        container = FeatureColors.chatContainer,
                        label = "Chat",
                        badgeCount = unreadMessagesCount,
                        onClick = onNavigateToChat
                    ),
                    HubModule(
                        testTag = "launcher_polls",
                        icon = Icons.Default.HowToVote,
                        accent = FeatureColors.polls,
                        container = FeatureColors.pollsContainer,
                        label = "Polls",
                        badgeCount = openPollsCount,
                        onClick = onNavigateToPolls
                    )
                )
            }

            val hubModulesPerRow = 5

            hubModules.chunked(hubModulesPerRow).forEach { rowModules ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowModules.forEach { module ->
                        Card(
                            onClick = module.onClick,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .testTag(module.testTag),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (module.badgeCount > 0) {
                                            Badge { Text(if (module.badgeCount > 99) "99+" else module.badgeCount.toString()) }
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(module.container),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = module.icon,
                                            contentDescription = null,
                                            tint = module.accent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = module.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    repeat(hubModulesPerRow - rowModules.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Agenda / Upcoming Events section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Agenda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToCalendar) {
                    Text("View Full Calendar")
                }
            }

            if (upcomingEvents.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No upcoming events scheduled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap on 'Shared Family Calendar' to schedule chores, outings, or reminders.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        upcomingEvents.forEachIndexed { index, event ->
                            val creator = membersWithPerformance.find { it.member.id == event.createdByMemberId }?.member
                            val categoryColor = eventCategoryColors[event.category] ?: EventOther

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Date tag
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(categoryColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val parts = event.date.split("-")
                                        if (parts.size == 3) {
                                            val monthNum = parts[1].toIntOrNull() ?: 1
                                            val monthAbbr = when (monthNum) {
                                                1 -> "JAN"
                                                2 -> "FEB"
                                                3 -> "MAR"
                                                4 -> "APR"
                                                5 -> "MAY"
                                                6 -> "JUN"
                                                7 -> "JUL"
                                                8 -> "AUG"
                                                9 -> "SEP"
                                                10 -> "OCT"
                                                11 -> "NOV"
                                                12 -> "DEC"
                                                else -> "JUL"
                                            }
                                            Text(
                                                text = monthAbbr,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = categoryColor
                                            )
                                            Text(
                                                text = parts[2],
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = categoryColor
                                            )
                                        } else {
                                            Text("EVT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = categoryColor)
                                        }
                                    }
                                }

                                // Event details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = event.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (event.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                            color = if (event.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (event.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = AppTheme.extendedColors.success,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = event.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = categoryColor
                                        )
                                        if (creator != null) {
                                            Text(
                                                text = "• ${creator.name}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (event.description.isNotEmpty()) {
                                        Text(
                                            text = event.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                            if (index < upcomingEvents.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSyncDialog) {
        SyncDialog(
            viewModel = viewModel,
            onDismiss = { showSyncDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDialog(
    viewModel: SavingsViewModel,
    onDismiss: () -> Unit
) {
    val syncGroupCode by viewModel.syncGroupCode.collectAsStateWithLifecycle()
    val migrationStatus by viewModel.migrationStatus.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    var inputCode by remember { mutableStateOf("") }
    var showImportWarningDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val context = LocalContext.current

    // Launcher to save database file (CreateDocument)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val success = viewModel.exportDatabase(context, outputStream)
                    if (success) {
                        Toast.makeText(context, "Database exported successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to export database", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun performImport(uri: android.net.Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val success = viewModel.importDatabase(context, inputStream)
                if (success) {
                    Toast.makeText(context, "Database restored! Restarting app...", Toast.LENGTH_LONG).show()

                    // Restart Activity to re-read Room DB with new data
                    (context as? Activity)?.let { activity ->
                        val intent = activity.intent
                        activity.finish()
                        activity.startActivity(intent)
                    }
                } else {
                    Toast.makeText(context, "Failed to import database", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Import error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher to open database file (OpenDocument)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            if (syncGroupCode.isNotEmpty()) {
                // Importing while connected to live Cloud Sync would silently diverge
                // from Firestore - warn before proceeding.
                pendingImportUri = it
                showImportWarningDialog = true
            } else {
                performImport(it)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Family Cloud Sync",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Synchronize your family organizer (savings, calendar, and members) in real-time across multiple devices using a secure code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Connection status indicator - Firestore listeners are always live once
                // connected, so there's no "syncing..."/"last synced" state to show anymore.
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (syncGroupCode.isNotEmpty()) AppTheme.extendedColors.successContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (syncGroupCode.isNotEmpty()) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppTheme.extendedColors.success)
                            Text(
                                text = "Connected - changes sync live",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.extendedColors.success,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Cloud sync inactive",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (syncGroupCode.isEmpty()) {
                    // Not connected - Options to Create or Join
                    HorizontalDivider()

                    Text(
                        text = "Join Existing Sync Group",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        label = { Text("Family Sync Code") },
                        placeholder = { Text("Enter shared code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (inputCode.isNotEmpty()) {
                                IconButton(onClick = { inputCode = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = { viewModel.joinSyncGroup(inputCode) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = inputCode.trim().isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Join Family Group")
                    }

                    HorizontalDivider()

                    Text(
                        text = "Or Create New Sync Group",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Generates a new family code. Your existing local data stays local until you tap \"Migrate Existing Data to Cloud Sync\" below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        onClick = { viewModel.createSyncGroup() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Family Group")
                    }
                } else {
                    // Connected - displays code and actions
                    HorizontalDivider()

                    Text(
                        text = "Active Family Code",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = syncGroupCode,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(syncGroupCode))
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy code")
                            }
                        }
                    }

                    Text(
                        text = "Share this code with family members on their devices to view and sync the same goals and calendar events.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    // One-time cloud backfill - explicit, single-device, guarded against
                    // double-migration. Never runs automatically.
                    Text(
                        text = "Migrate Existing Data to Cloud Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "If this device has local savings/calendar/task data that hasn't been pushed to this family code yet, migrate it once here. Only run this from one device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when (migrationStatus) {
                        MigrationStatus.MIGRATING, MigrationStatus.CHECKING -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Migrating...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        MigrationStatus.SUCCESS -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppTheme.extendedColors.success)
                                Text("Migration complete!", color = AppTheme.extendedColors.success, fontWeight = FontWeight.Bold)
                            }
                        }
                        MigrationStatus.REFUSED_NOT_EMPTY -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = "Cloud data already exists for this code - migrating again could create duplicates. If this is a mistake, leave the group and create/join the correct code.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        MigrationStatus.ERROR -> {
                            Text(
                                text = "Migration failed - check your connection and try again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        MigrationStatus.IDLE -> {}
                    }

                    FilledTonalButton(
                        onClick = { viewModel.migrateLocalDataToCloud() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = migrationStatus != MigrationStatus.MIGRATING && migrationStatus != MigrationStatus.CHECKING,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Migrate Existing Data to Cloud Sync")
                    }

                    HorizontalDivider()

                    Button(
                        onClick = { viewModel.disconnectSyncGroup() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Leave Group")
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Offline Database Backup & Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Local emergency snapshot only - does not represent your family's live cloud data once Cloud Sync is connected, and importing an old snapshot will NOT be reconciled back into Firestore. Use only for local recovery on a single un-synced device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { createDocumentLauncher.launch("family_savings_backup.db") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Export Backup", fontSize = 11.sp, maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Import / Restore", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    if (showImportWarningDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportWarningDialog = false
                pendingImportUri = null
            },
            title = { Text("Import while connected to Cloud Sync?") },
            text = {
                Text(
                    "You are connected to Cloud Sync group \"$syncGroupCode\". Importing a backup will not update the cloud - your other devices won't see this restored data, and future edits from this device may conflict. Leave the sync group first if you intend this backup to become the new source of truth."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportWarningDialog = false
                        pendingImportUri?.let { performImport(it) }
                        pendingImportUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Import Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportWarningDialog = false
                    pendingImportUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

