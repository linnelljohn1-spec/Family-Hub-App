package com.example.ui

import com.example.data.FamilyMember
import com.example.data.FamilyTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Per-member task completion figures for a single month.
 * "assignedCount" / "completedCount" refer to the cohort of tasks assigned to this member
 * and created in that month (see [computeMonthlyTaskReports]).
 */
data class MemberTaskCompletionSummary(
    val member: FamilyMember,
    val assignedCount: Int,
    val completedCount: Int,
    val awaitingApprovalCount: Int,
    val completionPercent: Float // 0f..1f
)

/** Aggregated task completion figures for a single calendar month, across all members. */
data class MonthlyTaskReport(
    val monthYearString: String,
    val totalAssignedCount: Int,
    val totalCompletedCount: Int,
    val totalAwaitingApprovalCount: Int,
    val memberSummaries: List<MemberTaskCompletionSummary>
)

/** How many calendar months of stats to show: the current month plus the previous (N - 1). */
const val STATS_MONTHS_SHOWN = 6

/**
 * Start of the stats window: midnight on the 1st of the month (STATS_MONTHS_SHOWN - 1) months
 * before [nowMillis]'s month. Tasks created before this are neither shown in the stats nor kept
 * by the admin "delete old completed tasks" cleanup.
 */
fun statsWindowStartMillis(nowMillis: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, -(STATS_MONTHS_SHOWN - 1))
    }.timeInMillis

/** Approved, completed tasks created before the stats window - what the admin cleanup deletes. */
fun completedTasksOlderThanStatsWindow(
    tasks: List<FamilyTask>,
    nowMillis: Long = System.currentTimeMillis()
): List<FamilyTask> {
    val windowStart = statsWindowStartMillis(nowMillis)
    return tasks.filter { it.isCompleted && it.isApproved && it.createdAt < windowStart }
}

/**
 * Groups [tasks] by the calendar month they were created in, then by assignee, producing a
 * completion percentage per member per month. Only the last [STATS_MONTHS_SHOWN] calendar months
 * are included (see [statsWindowStartMillis]).
 *
 * Metric definition: a task counts toward month M for a member if it was assigned to that member
 * and created in month M. Its completion status is read as of now, regardless of when it was
 * actually completed (there is no separate completedAt timestamp) - this is an intentional cohort
 * semantic ("of what was assigned in month M, how much is done now"), not a time-windowed count of
 * completions. Tasks assigned to "Anyone" (assignedMemberId == -1) are excluded, since they aren't
 * attributable to a single person's completion rate.
 *
 * "Completed" requires both isCompleted and isApproved - a task a member marked done still needs
 * an admin's approval before it counts, so awaitingApprovalCount surfaces that in-between state
 * rather than letting it silently vanish from both the assigned and completed tallies.
 */
fun computeMonthlyTaskReports(
    tasks: List<FamilyTask>,
    members: List<FamilyMember>
): List<MonthlyTaskReport> {
    if (tasks.isEmpty() || members.isEmpty()) return emptyList()

    val memberMap = members.associateBy { it.id }
    val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    // Exclude "Anyone" (-1) and any task pointing at a deleted/unknown member.
    val windowStart = statsWindowStartMillis()
    val assignedTasks = tasks.filter {
        it.assignedMemberId != -1 && memberMap.containsKey(it.assignedMemberId) && it.createdAt >= windowStart
    }
    if (assignedTasks.isEmpty()) return emptyList()

    val groupedByMonth = assignedTasks.groupBy { sdf.format(Date(it.createdAt)) }

    return groupedByMonth.map { (monthYear, monthTasks) ->
        val memberSummaries = monthTasks.groupBy { it.assignedMemberId }
            .map { (memberId, memberTasks) ->
                val assignedCount = memberTasks.size
                val completedCount = memberTasks.count { it.isCompleted && it.isApproved }
                val awaitingApprovalCount = memberTasks.count { it.isCompleted && !it.isApproved }
                MemberTaskCompletionSummary(
                    member = memberMap.getValue(memberId),
                    assignedCount = assignedCount,
                    completedCount = completedCount,
                    awaitingApprovalCount = awaitingApprovalCount,
                    completionPercent = if (assignedCount > 0) completedCount.toFloat() / assignedCount else 0f
                )
            }
            .sortedByDescending { it.assignedCount }

        MonthlyTaskReport(
            monthYearString = monthYear,
            totalAssignedCount = monthTasks.size,
            totalCompletedCount = monthTasks.count { it.isCompleted && it.isApproved },
            totalAwaitingApprovalCount = monthTasks.count { it.isCompleted && !it.isApproved },
            memberSummaries = memberSummaries
        )
    }.sortedWith { r1, r2 ->
        try {
            val d1 = sdf.parse(r1.monthYearString)
            val d2 = sdf.parse(r2.monthYearString)
            d2?.compareTo(d1) ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
