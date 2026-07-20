package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val NO_MEMBER = -1

enum class MigrationStatus {
    IDLE,
    CHECKING,
    MIGRATING,
    SUCCESS,
    REFUSED_NOT_EMPTY,
    ERROR
}

data class GoalWithProgress(
    val goal: SavingsGoal,
    val currentAmount: Double,
    val progress: Float,
    val contributions: List<Contribution>
)

data class MemberWithPerformance(
    val member: FamilyMember,
    val goalsWithProgress: List<GoalWithProgress>,
    val totalSaved: Double,
    val totalTarget: Double,
    val performanceProgress: Float
)

data class MemberContributionSummary(
    val member: FamilyMember,
    val amountSavedThisMonth: Double,
    val percentOfTotalMonthSavings: Float
)

data class MonthlyReport(
    val monthYearString: String,
    val totalSavedThisMonth: Double,
    val contributionsCount: Int,
    val memberContributions: List<MemberContributionSummary>
)

class SavingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SavingsRepository = SavingsRepository(AppDatabase.getDatabase(application).savingsDao())
    private val calendarRepository: CalendarRepository = CalendarRepository(AppDatabase.getDatabase(application).calendarDao())
    private val taskRepository: TaskRepository = TaskRepository(AppDatabase.getDatabase(application).taskDao())

    private val _activeMemberId = MutableStateFlow<Int>(-1)
    val activeMemberId: StateFlow<Int> = _activeMemberId.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("savings_prefs", Context.MODE_PRIVATE)

    private fun updateActiveMemberWithFallback(membersList: List<FamilyMember>) {
        if (membersList.isEmpty()) {
            _activeMemberId.value = -1
            return
        }
        val savedId = sharedPrefs.getInt("active_member_id", -1)
        val matchedMember = membersList.find { it.id == savedId }
        if (matchedMember != null) {
            _activeMemberId.value = savedId
        } else {
            val admin = membersList.find { it.isAdmin }
            val defaultId = admin?.id ?: membersList.first().id
            _activeMemberId.value = defaultId
            sharedPrefs.edit().putInt("active_member_id", defaultId).apply()
        }
    }

    // Sync group state - Firestore-backed, always-live once connected
    private val _syncGroupCode = MutableStateFlow<String>("")
    val syncGroupCode: StateFlow<String> = _syncGroupCode.asStateFlow()

    val isConnected: StateFlow<Boolean> = syncGroupCode.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _migrationStatus = MutableStateFlow(MigrationStatus.IDLE)
    val migrationStatus: StateFlow<MigrationStatus> = _migrationStatus.asStateFlow()

    private val prefs = application.getSharedPreferences("family_organizer_prefs", Context.MODE_PRIVATE)

    private fun loadSyncSettings() {
        _syncGroupCode.value = prefs.getString("sync_group_code", "") ?: ""
    }

    private fun saveSyncSettings() {
        prefs.edit().putString("sync_group_code", _syncGroupCode.value).apply()
    }

    // Raw data streams
    val members: StateFlow<List<FamilyMember>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<SavingsGoal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contributions: StateFlow<List<Contribution>> = repository.allContributions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarEvents: StateFlow<List<CalendarEvent>> = calendarRepository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<FamilyTask>> = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSyncSettings()
        val hasSyncGroup = _syncGroupCode.value.isNotEmpty()

        // Prepopulate examples on first launch, but never for a device that's already
        // part of a family sync group - it should wait for Firestore to populate instead.
        viewModelScope.launch {
            repository.allMembers.first().let { currentMembers ->
                if (currentMembers.isEmpty() && !hasSyncGroup) {
                    val adminId = repository.insertMember(FamilyMember(name = "John", avatarColorHex = "#6750A4", isAdmin = true, unallocatedBalance = 250.0))
                    val member1Id = repository.insertMember(FamilyMember(name = "Alex", avatarColorHex = "#4CAF50", unallocatedBalance = 50.0))
                    val member2Id = repository.insertMember(FamilyMember(name = "Jordan", avatarColorHex = "#FF4081", unallocatedBalance = 30.0))
                    val member3Id = repository.insertMember(FamilyMember(name = "Taylor", avatarColorHex = "#2196F3", unallocatedBalance = 15.0))

                    val goal1Id = repository.insertGoal(SavingsGoal(memberId = member1Id.toInt(), title = "New Laptop", targetAmount = 1200.0))
                    val goal2Id = repository.insertGoal(SavingsGoal(memberId = member1Id.toInt(), title = "Bicycle", targetAmount = 350.0))
                    val goal3Id = repository.insertGoal(SavingsGoal(memberId = member2Id.toInt(), title = "Summer Camp Fund", targetAmount = 1500.0))
                    val goal4Id = repository.insertGoal(SavingsGoal(memberId = member3Id.toInt(), title = "Concert Ticket", targetAmount = 150.0))
                    val goal5Id = repository.insertGoal(SavingsGoal(memberId = adminId.toInt(), title = "Family Vacation", targetAmount = 3000.0))

                    // Some contributions in current month
                    val now = System.currentTimeMillis()
                    val oneDayMs = 24 * 60 * 60 * 1000L

                    repository.insertContribution(Contribution(goalId = goal1Id.toInt(), amount = 450.0, timestamp = now - 5 * oneDayMs, note = "Allocated from personal fund"))
                    repository.insertContribution(Contribution(goalId = goal1Id.toInt(), amount = 150.0, timestamp = now - 1 * oneDayMs, note = "Allowance allocated"))
                    repository.insertContribution(Contribution(goalId = goal2Id.toInt(), amount = 120.0, timestamp = now - 3 * oneDayMs, note = "Birthday gift allocation"))
                    repository.insertContribution(Contribution(goalId = goal3Id.toInt(), amount = 600.0, timestamp = now - 2 * oneDayMs, note = "Chores payout allocation"))
                    repository.insertContribution(Contribution(goalId = goal4Id.toInt(), amount = 150.0, timestamp = now, note = "Goal achieved! 🎉"))
                    repository.insertContribution(Contribution(goalId = goal5Id.toInt(), amount = 1500.0, timestamp = now - 4 * oneDayMs, note = "Family vacation budget"))
                }
            }
        }

        // Prepopulate calendar events if calendar_events is empty and not synced
        viewModelScope.launch {
            calendarRepository.allEvents.first().let { currentEvents ->
                if (currentEvents.isEmpty() && !hasSyncGroup) {
                    val memberList = repository.allMembers.first()
                    val adminId = memberList.find { it.isAdmin }?.id ?: -1
                    val alexId = memberList.find { name -> name.name == "Alex" }?.id ?: -1
                    val jordanId = memberList.find { name -> name.name == "Jordan" }?.id ?: -1
                    val taylorId = memberList.find { name -> name.name == "Taylor" }?.id ?: -1

                    calendarRepository.insertEvent(CalendarEvent(title = "4th of July BBQ! 🍔", description = "Backyard family gathering and fireworks viewing.", date = "2026-07-04", time = "17:00", category = "Family Outing", createdByMemberId = adminId))
                    calendarRepository.insertEvent(CalendarEvent(title = "Mow the lawn", description = "Alex - please mow the front and back lawns.", date = "2026-07-05", time = "10:00", category = "Chore", createdByMemberId = alexId))
                    calendarRepository.insertEvent(CalendarEvent(title = "Summer Camp Orientation", description = "Jordan's camp instructions and packing list check.", date = "2026-07-10", time = "09:30", category = "Reminder", createdByMemberId = jordanId))
                    calendarRepository.insertEvent(CalendarEvent(title = "Concert Night! 🎸", description = "Taylor's concert at the Arena.", date = "2026-07-15", time = "19:00", category = "Family Outing", createdByMemberId = taylorId))
                    calendarRepository.insertEvent(CalendarEvent(title = "Family Vacation Starts! ✈️", description = "Packing bags and leaving for the beach resort.", date = "2026-07-20", time = "06:00", category = "Family Outing", createdByMemberId = -1))
                }
            }
        }

        // Prepopulate tasks if empty and not synced
        viewModelScope.launch {
            taskRepository.allTasks.first().let { currentTasks ->
                if (currentTasks.isEmpty() && !hasSyncGroup) {
                    val memberList = repository.allMembers.first()
                    val alexId = memberList.find { it.name == "Alex" }?.id ?: -1
                    val jordanId = memberList.find { it.name == "Jordan" }?.id ?: -1
                    val taylorId = memberList.find { it.name == "Taylor" }?.id ?: -1

                    taskRepository.insertTask(FamilyTask(title = "Wash the dishes 🍽️", description = "Load the dishwasher and clean the pans after dinner.", assignedMemberId = alexId))
                    taskRepository.insertTask(FamilyTask(title = "Vacuum the living room 🧹", description = "Make sure to clean under the couch and empty the bin afterwards.", assignedMemberId = jordanId))
                    taskRepository.insertTask(FamilyTask(title = "Take out the trash ♻️", description = "Empty the kitchen trash and recycling bins into the main wheelie bins.", assignedMemberId = taylorId))
                    taskRepository.insertTask(FamilyTask(title = "Walk the dog 🐕", description = "Take Buster for a 20 minute walk around the park.", assignedMemberId = -1))
                }
            }
        }

        // Set the active member ID automatically to the Admin when loaded
        viewModelScope.launch {
            members.collect { memberList ->
                if (_activeMemberId.value == -1 && memberList.isNotEmpty()) {
                    updateActiveMemberWithFallback(memberList)
                }
            }
        }
    }

    fun setActiveMember(id: Int) {
        _activeMemberId.value = id
        sharedPrefs.edit().putInt("active_member_id", id).apply()
    }

    // ---- Firestore push helpers ----
    // Every entity gets a firestoreId assigned at creation time (whether or not this
    // device is currently connected), so it's always ready to be migrated/synced later.
    // Pushing to Firestore is a no-op whenever syncGroupCode is empty.

    private suspend fun ensureMemberFirestoreId(member: FamilyMember): FamilyMember {
        if (member.firestoreId != null) return member
        val updated = member.copy(firestoreId = UUID.randomUUID().toString())
        repository.updateMember(updated)
        return updated
    }

    private suspend fun ensureGoalFirestoreId(goal: SavingsGoal): SavingsGoal {
        if (goal.firestoreId != null) return goal
        val updated = goal.copy(firestoreId = UUID.randomUUID().toString())
        repository.updateGoal(updated)
        return updated
    }

    private suspend fun ensureContributionFirestoreId(contribution: Contribution): Contribution {
        if (contribution.firestoreId != null) return contribution
        val updated = contribution.copy(firestoreId = UUID.randomUUID().toString())
        repository.insertContribution(updated)
        return updated
    }

    private suspend fun ensureCalendarEventFirestoreId(event: CalendarEvent): CalendarEvent {
        if (event.firestoreId != null) return event
        val updated = event.copy(firestoreId = UUID.randomUUID().toString())
        calendarRepository.insertEvent(updated)
        return updated
    }

    private suspend fun ensureTaskFirestoreId(task: FamilyTask): FamilyTask {
        if (task.firestoreId != null) return task
        val updated = task.copy(firestoreId = UUID.randomUUID().toString())
        taskRepository.insertTask(updated)
        return updated
    }

    private suspend fun pushMember(member: FamilyMember) {
        val code = _syncGroupCode.value
        if (code.isEmpty()) return
        FamilyDataSyncService.upsertMember(code, ensureMemberFirestoreId(member))
    }

    private suspend fun pushGoal(goal: SavingsGoal) {
        val code = _syncGroupCode.value
        if (code.isEmpty()) return
        val member = repository.allMembers.first().find { it.id == goal.memberId } ?: return
        val ensuredMember = ensureMemberFirestoreId(member)
        val ensuredGoal = ensureGoalFirestoreId(goal)
        FamilyDataSyncService.upsertGoal(code, ensuredGoal, ensuredMember.firestoreId!!)
    }

    private suspend fun pushContribution(contribution: Contribution) {
        val code = _syncGroupCode.value
        if (code.isEmpty()) return
        val goal = repository.allGoals.first().find { it.id == contribution.goalId } ?: return
        val ensuredGoal = ensureGoalFirestoreId(goal)
        FamilyDataSyncService.upsertContribution(code, ensureContributionFirestoreId(contribution), ensuredGoal.firestoreId!!)
    }

    private suspend fun pushCalendarEvent(event: CalendarEvent) {
        val code = _syncGroupCode.value
        if (code.isEmpty()) return
        val ensuredEvent = ensureCalendarEventFirestoreId(event)
        val createdByFirestoreId = if (ensuredEvent.createdByMemberId == NO_MEMBER) {
            null
        } else {
            repository.allMembers.first().find { it.id == ensuredEvent.createdByMemberId }?.let { ensureMemberFirestoreId(it).firestoreId }
        }
        FamilyDataSyncService.upsertCalendarEvent(code, ensuredEvent, createdByFirestoreId)
    }

    private suspend fun pushTask(task: FamilyTask) {
        val code = _syncGroupCode.value
        if (code.isEmpty()) return
        val ensuredTask = ensureTaskFirestoreId(task)
        val assignedFirestoreId = if (ensuredTask.assignedMemberId == NO_MEMBER) {
            null
        } else {
            repository.allMembers.first().find { it.id == ensuredTask.assignedMemberId }?.let { ensureMemberFirestoreId(it).firestoreId }
        }
        FamilyDataSyncService.upsertTask(code, ensuredTask, assignedFirestoreId)
    }

    private suspend fun pushDeleteMember(member: FamilyMember) {
        val code = _syncGroupCode.value
        val firestoreId = member.firestoreId
        if (code.isEmpty() || firestoreId == null) return
        FamilyDataSyncService.deleteMember(code, firestoreId)
    }

    private suspend fun pushDeleteGoal(goal: SavingsGoal) {
        val code = _syncGroupCode.value
        val firestoreId = goal.firestoreId
        if (code.isEmpty() || firestoreId == null) return
        FamilyDataSyncService.deleteGoal(code, firestoreId)
    }

    private fun pushDeleteContribution(contribution: Contribution) {
        val code = _syncGroupCode.value
        val firestoreId = contribution.firestoreId
        if (code.isEmpty() || firestoreId == null) return
        FamilyDataSyncService.deleteContribution(code, firestoreId)
    }

    private fun pushDeleteCalendarEvent(event: CalendarEvent) {
        val code = _syncGroupCode.value
        val firestoreId = event.firestoreId
        if (code.isEmpty() || firestoreId == null) return
        FamilyDataSyncService.deleteCalendarEvent(code, firestoreId)
    }

    private fun pushDeleteTask(task: FamilyTask) {
        val code = _syncGroupCode.value
        val firestoreId = task.firestoreId
        if (code.isEmpty() || firestoreId == null) return
        FamilyDataSyncService.deleteTask(code, firestoreId)
    }

    fun addCalendarEvent(
        title: String,
        description: String,
        date: String,
        time: String,
        category: String,
        createdByMemberId: Int,
        isAllDay: Boolean = false,
        endTime: String? = null
    ) {
        viewModelScope.launch {
            val event = CalendarEvent(
                title = title,
                description = description,
                date = date,
                time = time,
                category = category,
                createdByMemberId = createdByMemberId,
                isAllDay = isAllDay,
                endTime = endTime,
                firestoreId = UUID.randomUUID().toString()
            )
            calendarRepository.insertEvent(event)
            pushCalendarEvent(event)
        }
    }

    fun deleteCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch {
            calendarRepository.deleteEvent(event)
            pushDeleteCalendarEvent(event)
        }
    }

    fun toggleCalendarEventCompletion(event: CalendarEvent) {
        viewModelScope.launch {
            val updated = event.copy(isCompleted = !event.isCompleted)
            calendarRepository.insertEvent(updated)
            pushCalendarEvent(updated)
        }
    }

    fun updateCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch {
            calendarRepository.insertEvent(event)
            pushCalendarEvent(event)
        }
    }

    // Combined stream representing family performance
    val familyPerformance: StateFlow<List<MemberWithPerformance>> = combine(
        members,
        goals,
        contributions
    ) { memberList, goalList, contributionList ->
        memberList.map { member ->
            val memberGoals = goalList.filter { it.memberId == member.id }
            val goalsWithProgress = memberGoals.map { goal ->
                val goalContributions = contributionList.filter { it.goalId == goal.id }
                val currentAmount = goalContributions.sumOf { it.amount }
                val progress = if (goal.targetAmount > 0) {
                    (currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                } else {
                    1f
                }
                GoalWithProgress(
                    goal = goal,
                    currentAmount = currentAmount,
                    progress = progress,
                    contributions = goalContributions
                )
            }
            val totalSaved = goalsWithProgress.sumOf { it.currentAmount }
            val totalTarget = goalsWithProgress.sumOf { it.goal.targetAmount }
            val performanceProgress = if (totalTarget > 0) {
                (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }

            MemberWithPerformance(
                member = member,
                goalsWithProgress = goalsWithProgress,
                totalSaved = totalSaved,
                totalTarget = totalTarget,
                performanceProgress = performanceProgress
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined monthly summary reports stream
    val monthlyReports: StateFlow<List<MonthlyReport>> = combine(
        members,
        goals,
        contributions
    ) { memberList, goalList, contributionList ->
        if (contributionList.isEmpty()) return@combine emptyList<MonthlyReport>()

        val goalToMemberId = goalList.associate { it.id to it.memberId }
        val memberMap = memberList.associateBy { it.id }

        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val groupedByMonth = contributionList.groupBy { contribution ->
            sdf.format(Date(contribution.timestamp))
        }

        groupedByMonth.map { (monthYear, monthContributions) ->
            val totalSavedThisMonth = monthContributions.sumOf { it.amount }
            val contributionsCount = monthContributions.size

            // Group by member
            val memberSums = monthContributions.groupBy { contribution ->
                goalToMemberId[contribution.goalId]
            }.mapNotNull { (memberId, contribs) ->
                if (memberId == null) return@mapNotNull null
                val member = memberMap[memberId] ?: return@mapNotNull null
                val amountSaved = contribs.sumOf { it.amount }
                MemberContributionSummary(
                    member = member,
                    amountSavedThisMonth = amountSaved,
                    percentOfTotalMonthSavings = if (totalSavedThisMonth > 0) {
                        (amountSaved / totalSavedThisMonth).toFloat()
                    } else {
                        0f
                    }
                )
            }.sortedByDescending { it.amountSavedThisMonth }

            MonthlyReport(
                monthYearString = monthYear,
                totalSavedThisMonth = totalSavedThisMonth,
                contributionsCount = contributionsCount,
                memberContributions = memberSums
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // CRUD actions
    fun addMember(name: String, colorHex: String, isAdmin: Boolean = false, password: String = "1234") {
        viewModelScope.launch {
            val member = FamilyMember(name = name, avatarColorHex = colorHex, unallocatedBalance = 0.0, isAdmin = isAdmin, password = password, firestoreId = UUID.randomUUID().toString())
            repository.insertMember(member)
            pushMember(member)
        }
    }

    fun updateMemberPassword(memberId: Int, newPassword: String) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                val updatedMember = currentMember.copy(password = newPassword)
                repository.updateMember(updatedMember)
                pushMember(updatedMember)
            }
        }
    }

    fun updateMemberProfile(memberId: Int, newName: String, newColorHex: String) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                val updatedMember = currentMember.copy(name = newName, avatarColorHex = newColorHex)
                repository.updateMember(updatedMember)
                pushMember(updatedMember)
            }
        }
    }

    fun depositToUnallocatedFund(memberId: Int, amount: Double) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                val updatedMember = currentMember.copy(unallocatedBalance = currentMember.unallocatedBalance + amount)
                repository.updateMember(updatedMember)
                pushMember(updatedMember)
            }
        }
    }

    fun deductFromUnallocatedFund(memberId: Int, amount: Double) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                if (currentMember.unallocatedBalance >= amount) {
                    val updatedMember = currentMember.copy(unallocatedBalance = (currentMember.unallocatedBalance - amount).coerceAtLeast(0.0))
                    repository.updateMember(updatedMember)
                    pushMember(updatedMember)
                }
            }
        }
    }

    fun allocateUnallocatedFundToGoal(memberId: Int, goalId: Int, amount: Double, note: String?) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                if (currentMember.unallocatedBalance >= amount) {
                    val previousTotal = repository.getContributionsForGoal(goalId).first().sumOf { it.amount }
                    val updatedMember = currentMember.copy(unallocatedBalance = (currentMember.unallocatedBalance - amount).coerceAtLeast(0.0))
                    repository.updateMember(updatedMember)
                    pushMember(updatedMember)
                    val contribution = Contribution(goalId = goalId, amount = amount, note = note ?: "Allocated from personal fund", firestoreId = UUID.randomUUID().toString())
                    repository.insertContribution(contribution)
                    pushContribution(contribution)
                    checkAndNotifyGoalReached(goalId, memberId, previousTotal, previousTotal + amount)
                }
            }
        }
    }

    fun withdrawFundFromGoal(memberId: Int, goalId: Int, amount: Double, note: String?) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                val updatedMember = currentMember.copy(unallocatedBalance = currentMember.unallocatedBalance + amount)
                repository.updateMember(updatedMember)
                pushMember(updatedMember)
                val contribution = Contribution(goalId = goalId, amount = -amount, note = note ?: "Withdrawn to personal wallet", firestoreId = UUID.randomUUID().toString())
                repository.insertContribution(contribution)
                pushContribution(contribution)
            }
        }
    }

    fun deleteMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteMember(member)
            pushDeleteMember(member)
        }
    }

    fun addGoal(memberId: Int, title: String, targetAmount: Double, purchaseUrl: String? = null) {
        viewModelScope.launch {
            val goal = SavingsGoal(memberId = memberId, title = title, targetAmount = targetAmount, purchaseUrl = purchaseUrl, firestoreId = UUID.randomUUID().toString())
            repository.insertGoal(goal)
            pushGoal(goal)
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            val contributionsForGoal = repository.getContributionsForGoal(goal.id).first()
            val totalSaved = contributionsForGoal.sumOf { it.amount }
            if (!goal.isCompleted && totalSaved > 0.0) {
                val memberList = repository.allMembers.first()
                memberList.find { it.id == goal.memberId }?.let { member ->
                    val updatedMember = member.copy(unallocatedBalance = member.unallocatedBalance + totalSaved)
                    repository.updateMember(updatedMember)
                    pushMember(updatedMember)
                }
            }
            repository.deleteGoal(goal)
            pushDeleteGoal(goal)
        }
    }

    fun toggleGoalCompletion(goal: SavingsGoal) {
        viewModelScope.launch {
            val updatedGoal = goal.copy(isCompleted = !goal.isCompleted)
            repository.updateGoal(updatedGoal)
            pushGoal(updatedGoal)
        }
    }

    fun addContribution(goalId: Int, amount: Double, note: String?) {
        viewModelScope.launch {
            val goal = repository.allGoals.first().find { it.id == goalId }
            val previousTotal = repository.getContributionsForGoal(goalId).first().sumOf { it.amount }
            val contribution = Contribution(goalId = goalId, amount = amount, note = note, firestoreId = UUID.randomUUID().toString())
            repository.insertContribution(contribution)
            pushContribution(contribution)
            if (goal != null) {
                checkAndNotifyGoalReached(goalId, goal.memberId, previousTotal, previousTotal + amount)
            }
        }
    }

    private suspend fun checkAndNotifyGoalReached(goalId: Int, memberId: Int, previousTotal: Double, newTotal: Double) {
        val goal = repository.allGoals.first().find { it.id == goalId } ?: return
        if (previousTotal >= goal.targetAmount || newTotal < goal.targetAmount) return

        val memberName = repository.allMembers.first().find { it.id == memberId }?.name ?: "A family member"
        NotificationHelper.showGoalReachedNotification(getApplication(), memberName, goal.title)
        GoalEventService.recordGoalReached(
            syncGroupCode = _syncGroupCode.value,
            goalId = goal.id,
            memberId = memberId,
            memberName = memberName,
            goalTitle = goal.title
        )
    }

    fun deleteContribution(contribution: Contribution) {
        viewModelScope.launch {
            val allGoalsList = repository.allGoals.first()
            val goal = allGoalsList.find { it.id == contribution.goalId }
            if (goal != null) {
                val memberList = repository.allMembers.first()
                memberList.find { it.id == goal.memberId }?.let { member ->
                    val updatedBalance = (member.unallocatedBalance + contribution.amount).coerceAtLeast(0.0)
                    val updatedMember = member.copy(unallocatedBalance = updatedBalance)
                    repository.updateMember(updatedMember)
                    pushMember(updatedMember)
                }
            }
            repository.deleteContribution(contribution)
            pushDeleteContribution(contribution)
        }
    }

    // ---- Sync group lifecycle ----
    // Firestore collections/documents are created implicitly on first write, so
    // "creating" a group is just adopting a fresh local code - no network call needed.

    fun createSyncGroup() {
        val newCode = UUID.randomUUID().toString().take(8).uppercase()
        _syncGroupCode.value = newCode
        saveSyncSettings()
    }

    fun joinSyncGroup(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            // Clear local data so this device's own pre-join data doesn't mix with
            // the family's - FamilyDataSyncViewModel's listeners repopulate it live.
            repository.clearAll()
            calendarRepository.clearAll()
            taskRepository.clearAll()
            _syncGroupCode.value = trimmed
            saveSyncSettings()
        }
    }

    fun disconnectSyncGroup() {
        _syncGroupCode.value = ""
        saveSyncSettings()
    }

    fun migrateLocalDataToCloud() {
        val code = _syncGroupCode.value
        if (code.isBlank()) return
        viewModelScope.launch {
            _migrationStatus.value = MigrationStatus.CHECKING
            try {
                val alreadyHasData = FamilyDataSyncService.hasAnyRemoteData(code)
                if (alreadyHasData) {
                    _migrationStatus.value = MigrationStatus.REFUSED_NOT_EMPTY
                    return@launch
                }

                _migrationStatus.value = MigrationStatus.MIGRATING

                val migratedMembers = repository.allMembers.first().map { ensureMemberFirestoreId(it) }
                val migratedGoals = repository.allGoals.first().map { ensureGoalFirestoreId(it) }
                val migratedContributions = repository.allContributions.first().map { ensureContributionFirestoreId(it) }
                val migratedEvents = calendarRepository.allEvents.first().map { ensureCalendarEventFirestoreId(it) }
                val migratedTasks = taskRepository.allTasks.first().map { ensureTaskFirestoreId(it) }

                FamilyDataSyncService.backfill(
                    code,
                    migratedMembers,
                    migratedGoals,
                    migratedContributions,
                    migratedEvents,
                    migratedTasks
                )
                _migrationStatus.value = MigrationStatus.SUCCESS
            } catch (e: Exception) {
                _migrationStatus.value = MigrationStatus.ERROR
            }
        }
    }

    fun exportDatabase(context: Context, outputStream: java.io.OutputStream): Boolean {
        return try {
            val dbFile = context.getDatabasePath("family_savings_db")
            if (dbFile.exists()) {
                // Ensure any pending writes are flushed
                AppDatabase.getDatabase(context).runInTransaction {
                    // Simple empty transaction to ensure checkpoint
                }
                dbFile.inputStream().use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importDatabase(context: Context, inputStream: java.io.InputStream): Boolean {
        return try {
            AppDatabase.closeDatabase()
            val dbFile = context.getDatabasePath("family_savings_db")
            dbFile.outputStream().use { output ->
                inputStream.use { input ->
                    input.copyTo(output)
                }
            }
            val walFile = context.getDatabasePath("family_savings_db-wal")
            val shmFile = context.getDatabasePath("family_savings_db-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun addTask(title: String, description: String, assignedMemberId: Int, dueDate: String = "") {
        viewModelScope.launch {
            val task = FamilyTask(title = title, description = description, assignedMemberId = assignedMemberId, dueDate = dueDate, firestoreId = UUID.randomUUID().toString())
            taskRepository.insertTask(task)
            pushTask(task)
        }
    }

    fun completeTask(task: FamilyTask) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = true)
            taskRepository.updateTask(updated)
            pushTask(updated)
        }
    }

    fun deleteTask(task: FamilyTask) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            pushDeleteTask(task)
        }
    }
}
