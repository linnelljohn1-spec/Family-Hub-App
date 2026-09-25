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
import androidx.compose.material.icons.automirrored.filled.Undo
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
import com.example.ui.theme.AppTheme
import com.example.ui.theme.FeatureColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: SavingsViewModel,
    onNavigateBack: () -> Unit
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val activeMemberId by viewModel.activeMemberId.collectAsStateWithLifecycle()

    val activeMember = members.find { it.id == activeMemberId }
    val isActiveAdmin = activeMember?.isAdmin == true

    var selectedAssigneeFilter by remember { mutableStateOf<Int>(-2) } // -2 for "All", -1 for "Anyone/Unassigned", or specific member ID
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var statsSectionExpanded by remember { mutableStateOf(false) }

    // Sort and filter active tasks: display only uncompleted tasks
    val activeTasks = remember(tasks, selectedAssigneeFilter) {
        tasks.filter { !it.isCompleted }
            .filter { selectedAssigneeFilter == -2 || it.assignedMemberId == selectedAssigneeFilter }
            .sortedBy { it.createdAt }
    }

    // Tasks marked done by a member but not yet approved/rejected by an admin.
    val awaitingApprovalTasks = remember(tasks, selectedAssigneeFilter) {
        tasks.filter { it.isCompleted && !it.isApproved }
            .filter { selectedAssigneeFilter == -2 || it.assignedMemberId == selectedAssigneeFilter }
            .sortedBy { it.createdAt }
    }

    // Per-month, per-member assigned/completed breakdown (uses the full task list, not activeTasks,
    // since completed tasks are required for the completion percentage calculation)
    val monthlyTaskReports = remember(tasks, members) { computeMonthlyTaskReports(tasks, members) }
    val oldCompletedTaskCount = remember(tasks) { completedTasksOlderThanStatsWindow(tasks).size }
    var showDeleteOldTasksDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Family Tasks & Chores",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("tasks_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Family Hub"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.testTag("tasks_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Task",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddTaskDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Task") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .testTag("tasks_fab")
                    .padding(bottom = 16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Member/Assignee Filter Selector Chips
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "All" filter chip
                    FilterChip(
                        selected = selectedAssigneeFilter == -2,
                        onClick = { selectedAssigneeFilter = -2 },
                        label = { Text("All Tasks") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    // "Unassigned/Anyone" filter chip
                    FilterChip(
                        selected = selectedAssigneeFilter == -1,
                        onClick = { selectedAssigneeFilter = -1 },
                        label = { Text("Anyone") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    // Individual member filter chips
                    members.forEach { member ->
                        val isSelected = selectedAssigneeFilter == member.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAssigneeFilter = member.id },
                            label = { Text(member.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(member.avatarColorHex))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Task List Area (single scroll container: stats section, then empty-state or task list)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                item(key = "completion_stats_section") {
                    CompletionStatsExpandableSection(
                        reports = monthlyTaskReports,
                        isExpanded = statsSectionExpanded,
                        onToggleExpanded = { statsSectionExpanded = !statsSectionExpanded },
                        isActiveAdmin = isActiveAdmin,
                        oldCompletedTaskCount = oldCompletedTaskCount,
                        onDeleteOldCompletedTasks = { showDeleteOldTasksDialog = true }
                    )
                }

                if (awaitingApprovalTasks.isNotEmpty()) {
                    item(key = "awaiting_approval_header") {
                        AwaitingApprovalHeader(count = awaitingApprovalTasks.size)
                    }
                    items(
                        items = awaitingApprovalTasks,
                        key = { it.id }
                    ) { task ->
                        var isItemVisible by remember { mutableStateOf(true) }

                        LaunchedEffect(task.isCompleted, task.isApproved) {
                            if (task.isApproved || !task.isCompleted) {
                                // Left the "awaiting approval" cohort: approved -> Done, rejected -> back to Active.
                                isItemVisible = false
                            }
                        }

                        AnimatedVisibility(
                            visible = isItemVisible,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            TaskCard(
                                task = task,
                                members = members,
                                isAwaitingApproval = true,
                                isActiveAdmin = isActiveAdmin,
                                onApprove = {
                                    isItemVisible = false
                                    viewModel.approveTask(task)
                                },
                                onReject = {
                                    isItemVisible = false
                                    viewModel.rejectTaskApproval(task)
                                }
                            )
                        }
                    }
                }

                if (activeTasks.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillParentMaxHeight(0.7f)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Text(
                                    text = "All Chores Done! 🎉",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (selectedAssigneeFilter == -2) {
                                        "There are no pending chores for the family. Enjoy the free time!"
                                    } else {
                                        "No chores found matching this category. Feel free to add some!"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = activeTasks,
                        key = { it.id }
                    ) { task ->
                        // Wrap task item in AnimatedVisibility for seamless slide/fade exit on completion
                        var isItemVisible by remember { mutableStateOf(true) }

                        LaunchedEffect(task.isCompleted) {
                            if (task.isCompleted) {
                                isItemVisible = false
                            }
                        }

                        AnimatedVisibility(
                            visible = isItemVisible,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            TaskCard(
                                task = task,
                                members = members,
                                onMarkDone = {
                                    // Trigger animations, then perform update
                                    isItemVisible = false
                                    viewModel.completeTask(task)
                                },
                                onDelete = {
                                    viewModel.deleteTask(task)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteOldTasksDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteOldTasksDialog = false },
            title = { Text("Delete old completed tasks?") },
            text = {
                Text(
                    "This permanently deletes $oldCompletedTaskCount completed " +
                        (if (oldCompletedTaskCount == 1) "task" else "tasks") +
                        " created more than $STATS_MONTHS_SHOWN months ago, for the whole family. " +
                        "Tasks still awaiting approval are kept."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteOldCompletedTasks()
                        showDeleteOldTasksDialog = false
                    },
                    modifier = Modifier.testTag("confirm_delete_old_tasks")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteOldTasksDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            members = members,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, description, assignedId, dueDate ->
                viewModel.addTask(title, description, assignedId, dueDate)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun TaskCard(
    task: FamilyTask,
    members: List<FamilyMember>,
    isAwaitingApproval: Boolean = false,
    isActiveAdmin: Boolean = false,
    onMarkDone: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onApprove: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null
) {
    val assignedMember = members.find { it.id == task.assignedMemberId }
    val unassignedColor = MaterialTheme.colorScheme.outline
    val avatarColor = remember(assignedMember, unassignedColor) {
        assignedMember?.avatarColorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: unassignedColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular Done Checkbox button (or a static "awaiting approval" indicator)
            if (isAwaitingApproval) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = "Awaiting approval",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                )
            } else {
                IconButton(
                    onClick = { onMarkDone?.invoke() },
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                        .testTag("complete_task_btn_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Mark Task Done",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Assignee and Due Date Indicators row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Assignee badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = assignedMember?.name?.take(1)?.uppercase() ?: "?",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = assignedMember?.name ?: "Anyone",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Due date badge if specified
                    if (task.dueDate.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = task.dueDate,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (isAwaitingApproval) {
                if (isActiveAdmin) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onApprove?.invoke() },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("approve_task_btn_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Approve Task",
                                tint = AppTheme.extendedColors.success,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(
                            onClick = { onReject?.invoke() },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("reject_task_btn_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Reject Task",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Awaiting approval") },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .testTag("awaiting_approval_chip_${task.id}")
                    )
                }
            } else {
                // Trash delete button
                IconButton(
                    onClick = { onDelete?.invoke() },
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                        .testTag("delete_task_btn_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AwaitingApprovalHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("awaiting_approval_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.HourglassTop,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = "Awaiting Approval ($count)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun CompletionStatsExpandableSection(
    reports: List<MonthlyTaskReport>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    isActiveAdmin: Boolean,
    oldCompletedTaskCount: Int,
    onDeleteOldCompletedTasks: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_completion_stats_section"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FeatureColors.tasksContainer.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(16.dp)
                    .testTag("task_completion_stats_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = FeatureColors.tasks
                    )
                    Text(
                        text = "Completion Stats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Last $STATS_MONTHS_SHOWN months",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (reports.isEmpty()) {
                        Text(
                            text = "No assigned chores in the last $STATS_MONTHS_SHOWN months — stats appear once tasks are assigned to a family member.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        reports.forEach { report ->
                            MonthlyTaskStatsCard(report)
                        }
                    }
                    if (isActiveAdmin) {
                        OutlinedButton(
                            onClick = onDeleteOldCompletedTasks,
                            enabled = oldCompletedTaskCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_old_completed_tasks_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (oldCompletedTaskCount > 0) {
                                    "Delete $oldCompletedTaskCount completed older than $STATS_MONTHS_SHOWN months"
                                } else {
                                    "No completed tasks older than $STATS_MONTHS_SHOWN months"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyTaskStatsCard(report: MonthlyTaskReport) {
    val slug = remember(report.monthYearString) {
        report.monthYearString.lowercase(Locale.getDefault()).replace(" ", "_")
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_stats_month_$slug"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.monthYearString,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (report.totalAwaitingApprovalCount > 0) {
                        "${report.totalCompletedCount}/${report.totalAssignedCount} done · ${report.totalAwaitingApprovalCount} awaiting"
                    } else {
                        "${report.totalCompletedCount}/${report.totalAssignedCount} done"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            report.memberSummaries.forEach { summary ->
                MemberTaskCompletionRow(summary)
            }
        }
    }
}

@Composable
private fun MemberTaskCompletionRow(summary: MemberTaskCompletionSummary) {
    val avatarColor = remember(summary.member.avatarColorHex) {
        Color(android.graphics.Color.parseColor(summary.member.avatarColorHex))
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = summary.member.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = summary.member.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = if (summary.awaitingApprovalCount > 0) {
                    "${summary.completedCount}/${summary.assignedCount} (${(summary.completionPercent * 100).toInt()}%) · ${summary.awaitingApprovalCount} awaiting"
                } else {
                    "${summary.completedCount}/${summary.assignedCount} (${(summary.completionPercent * 100).toInt()}%)"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { summary.completionPercent },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = FeatureColors.tasks,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    members: List<FamilyMember>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, assignedId: Int, dueDate: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignedId by remember { mutableStateOf(-1) } // Default: Anyone (-1)
    var dueDate by remember { mutableStateOf("") }

    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("add_task_dialog"),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Family Chore / Task",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Clean the kitchen counter") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_input_title")
                )

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Add instructions, rewards, or notes") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_input_description")
                )

                // Assignee Dropdown Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    val assignedName = if (assignedId == -1) "Anyone" else members.find { it.id == assignedId }?.name ?: "Anyone"
                    OutlinedTextField(
                        value = assignedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign To") },
                        trailingIcon = {
                            IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown Trigger")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDropdown = true }
                            .testTag("task_input_assignee")
                    )
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Anyone") },
                            onClick = {
                                assignedId = -1
                                expandedDropdown = false
                            }
                        )
                        members.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.name) },
                                onClick = {
                                    assignedId = member.id
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // Due Date Input
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (Optional)") },
                    placeholder = { Text("e.g. Tomorrow or YYYY-MM-DD") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_input_due_date")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("task_dialog_cancel")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title.trim(), description.trim(), assignedId, dueDate.trim())
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("task_dialog_confirm"),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
