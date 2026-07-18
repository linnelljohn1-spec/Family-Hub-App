package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
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

    // Sync States
    private val _syncGroupCode = MutableStateFlow<String>("")
    val syncGroupCode: StateFlow<String> = _syncGroupCode.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow<Boolean>(true)
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow<Long>(0L)
    val lastSyncedTime: StateFlow<Long> = _lastSyncedTime.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String>("")
    val syncErrorMessage: StateFlow<String> = _syncErrorMessage.asStateFlow()

    private val prefs = application.getSharedPreferences("family_organizer_prefs", Context.MODE_PRIVATE)

    private fun loadSyncSettings() {
        _syncGroupCode.value = prefs.getString("sync_group_code", "") ?: ""
        _isAutoSyncEnabled.value = prefs.getBoolean("is_auto_sync_enabled", true)
        _lastSyncedTime.value = prefs.getLong("last_sync_time", 0L)
    }

    private fun saveSyncSettings() {
        prefs.edit()
            .putString("sync_group_code", _syncGroupCode.value)
            .putBoolean("is_auto_sync_enabled", _isAutoSyncEnabled.value)
            .putLong("last_sync_time", _lastSyncedTime.value)
            .apply()
    }

    fun triggerAutoSync() {
        val code = _syncGroupCode.value
        if (code.isNotEmpty() && _isAutoSyncEnabled.value) {
            viewModelScope.launch {
                try {
                    _syncStatus.value = SyncStatus.SYNCING
                    val payload = SyncPayload(
                        members = repository.allMembers.first(),
                        goals = repository.allGoals.first(),
                        contributions = repository.allContributions.first(),
                        events = calendarRepository.allEvents.first(),
                        lastUpdated = System.currentTimeMillis()
                    )
                    SyncService.updateSyncPayload(code, payload)
                    _lastSyncedTime.value = payload.lastUpdated
                    _syncStatus.value = SyncStatus.SUCCESS
                    saveSyncSettings()
                } catch (e: Exception) {
                    _syncStatus.value = SyncStatus.ERROR
                    _syncErrorMessage.value = e.message ?: "Failed to upload update"
                }
            }
        }
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

    init {
        // Prepopulate examples on first launch if database is empty
        viewModelScope.launch {
            repository.allMembers.first().let { currentMembers ->
                if (currentMembers.isEmpty()) {
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

        // Prepopulate calendar events if calendar_events is empty
        viewModelScope.launch {
            calendarRepository.allEvents.first().let { currentEvents ->
                if (currentEvents.isEmpty()) {
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

        // Load sync settings and trigger on start sync if connected
        loadSyncSettings()
        if (_syncGroupCode.value.isNotEmpty()) {
            syncNow()
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
            calendarRepository.insertEvent(
                CalendarEvent(
                    title = title,
                    description = description,
                    date = date,
                    time = time,
                    category = category,
                    createdByMemberId = createdByMemberId,
                    isAllDay = isAllDay,
                    endTime = endTime
                )
            )
            triggerAutoSync()
        }
    }

    fun deleteCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch {
            calendarRepository.deleteEvent(event)
            triggerAutoSync()
        }
    }

    fun toggleCalendarEventCompletion(event: CalendarEvent) {
        viewModelScope.launch {
            calendarRepository.insertEvent(event.copy(isCompleted = !event.isCompleted))
            triggerAutoSync()
        }
    }

    fun updateCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch {
            calendarRepository.insertEvent(event)
            triggerAutoSync()
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
            repository.insertMember(FamilyMember(name = name, avatarColorHex = colorHex, unallocatedBalance = 0.0, isAdmin = isAdmin, password = password))
            triggerAutoSync()
        }
    }

    fun updateMemberPassword(memberId: Int, newPassword: String) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                val updatedMember = currentMember.copy(password = newPassword)
                repository.updateMember(updatedMember)
                triggerAutoSync()
            }
        }
    }

    fun updateMemberProfile(memberId: Int, newName: String, newColorHex: String) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                val updatedMember = currentMember.copy(name = newName, avatarColorHex = newColorHex)
                repository.updateMember(updatedMember)
                triggerAutoSync()
            }
        }
    }

    fun depositToUnallocatedFund(memberId: Int, amount: Double) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                val updatedMember = currentMember.copy(unallocatedBalance = currentMember.unallocatedBalance + amount)
                repository.updateMember(updatedMember)
                triggerAutoSync()
            }
        }
    }

    fun deductFromUnallocatedFund(memberId: Int, amount: Double) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                if (currentMember.unallocatedBalance >= amount) {
                    val updatedMember = currentMember.copy(unallocatedBalance = (currentMember.unallocatedBalance - amount).coerceAtLeast(0.0))
                    repository.updateMember(updatedMember)
                    triggerAutoSync()
                }
            }
        }
    }

    fun allocateUnallocatedFundToGoal(memberId: Int, goalId: Int, amount: Double, note: String?) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                if (currentMember.unallocatedBalance >= amount) {
                    val updatedMember = currentMember.copy(unallocatedBalance = (currentMember.unallocatedBalance - amount).coerceAtLeast(0.0))
                    repository.updateMember(updatedMember)
                    repository.insertContribution(Contribution(goalId = goalId, amount = amount, note = note ?: "Allocated from personal fund"))
                    triggerAutoSync()
                }
            }
        }
    }

    fun withdrawFundFromGoal(memberId: Int, goalId: Int, amount: Double, note: String?) {
        viewModelScope.launch {
            members.value.find { it.id == memberId }?.let { currentMember ->
                val updatedMember = currentMember.copy(unallocatedBalance = currentMember.unallocatedBalance + amount)
                repository.updateMember(updatedMember)
                repository.insertContribution(Contribution(goalId = goalId, amount = -amount, note = note ?: "Withdrawn to personal wallet"))
                triggerAutoSync()
            }
        }
    }

    fun deleteMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteMember(member)
            triggerAutoSync()
        }
    }

    fun addGoal(memberId: Int, title: String, targetAmount: Double, purchaseUrl: String? = null) {
        viewModelScope.launch {
            repository.insertGoal(SavingsGoal(memberId = memberId, title = title, targetAmount = targetAmount, purchaseUrl = purchaseUrl))
            triggerAutoSync()
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            val contributions = repository.getContributionsForGoal(goal.id).first()
            val totalSaved = contributions.sumOf { it.amount }
            if (!goal.isCompleted && totalSaved > 0.0) {
                val memberList = repository.allMembers.first()
                memberList.find { it.id == goal.memberId }?.let { member ->
                    val updatedMember = member.copy(unallocatedBalance = member.unallocatedBalance + totalSaved)
                    repository.updateMember(updatedMember)
                }
            }
            repository.deleteGoal(goal)
            triggerAutoSync()
        }
    }

    fun toggleGoalCompletion(goal: SavingsGoal) {
        viewModelScope.launch {
            val updatedGoal = goal.copy(isCompleted = !goal.isCompleted)
            repository.updateGoal(updatedGoal)
            triggerAutoSync()
        }
    }

    fun addContribution(goalId: Int, amount: Double, note: String?) {
        viewModelScope.launch {
            repository.insertContribution(Contribution(goalId = goalId, amount = amount, note = note))
            triggerAutoSync()
        }
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
                }
            }
            repository.deleteContribution(contribution)
            triggerAutoSync()
        }
    }

    // Sync Operations
    fun createSyncGroup() {
        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus.SYNCING
                val localPayload = SyncPayload(
                    members = repository.allMembers.first(),
                    goals = repository.allGoals.first(),
                    contributions = repository.allContributions.first(),
                    events = calendarRepository.allEvents.first(),
                    lastUpdated = System.currentTimeMillis()
                )
                val newCode = SyncService.createSyncGroup(localPayload)
                _syncGroupCode.value = newCode
                _lastSyncedTime.value = localPayload.lastUpdated
                _syncStatus.value = SyncStatus.SUCCESS
                saveSyncSettings()
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                val rawMsg = e.message ?: "Failed to create sync group"
                _syncErrorMessage.value = if (rawMsg.contains("500")) {
                    "The cloud sync server is currently experiencing issues. Please try again later."
                } else {
                    rawMsg
                }
            }
        }
    }

    fun joinSyncGroup(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus.SYNCING
                val remotePayload = SyncService.fetchSyncPayload(trimmed)

                repository.clearAll()
                calendarRepository.clearAll()

                repository.insertAll(
                    remotePayload.members,
                    remotePayload.goals,
                    remotePayload.contributions
                )
                calendarRepository.insertAll(remotePayload.events)

                _syncGroupCode.value = trimmed
                _lastSyncedTime.value = remotePayload.lastUpdated
                _syncStatus.value = SyncStatus.SUCCESS
                saveSyncSettings()
                
                // Refresh active member
                updateActiveMemberWithFallback(remotePayload.members)
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                val rawMsg = e.message ?: "Failed to join sync group"
                _syncErrorMessage.value = if (rawMsg.contains("404")) {
                    "The sync code is invalid or has expired. Please create a new sync group on one device and share the code."
                } else if (rawMsg.contains("500")) {
                    "The cloud sync server is currently experiencing issues. Please try again later."
                } else {
                    rawMsg
                }
            }
        }
    }

    fun disconnectSyncGroup() {
        _syncGroupCode.value = ""
        _lastSyncedTime.value = 0L
        _syncStatus.value = SyncStatus.IDLE
        saveSyncSettings()
    }

    fun toggleAutoSync(enabled: Boolean) {
        _isAutoSyncEnabled.value = enabled
        saveSyncSettings()
    }

    fun syncNow() {
        val code = _syncGroupCode.value
        if (code.isEmpty()) return

        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus.SYNCING
                val remotePayload = SyncService.fetchSyncPayload(code)
                val localLastSynced = _lastSyncedTime.value

                if (remotePayload.lastUpdated > localLastSynced) {
                    repository.clearAll()
                    calendarRepository.clearAll()

                    repository.insertAll(
                        remotePayload.members,
                        remotePayload.goals,
                        remotePayload.contributions
                    )
                    calendarRepository.insertAll(remotePayload.events)

                    _lastSyncedTime.value = remotePayload.lastUpdated
                    _syncStatus.value = SyncStatus.SUCCESS
                    saveSyncSettings()

                    // Refresh active member
                    updateActiveMemberWithFallback(remotePayload.members)
                } else if (remotePayload.lastUpdated < localLastSynced || (localLastSynced == 0L)) {
                    val localPayload = SyncPayload(
                        members = repository.allMembers.first(),
                        goals = repository.allGoals.first(),
                        contributions = repository.allContributions.first(),
                        events = calendarRepository.allEvents.first(),
                        lastUpdated = System.currentTimeMillis()
                    )
                    SyncService.updateSyncPayload(code, localPayload)
                    _lastSyncedTime.value = localPayload.lastUpdated
                    _syncStatus.value = SyncStatus.SUCCESS
                    saveSyncSettings()
                } else {
                    _syncStatus.value = SyncStatus.SUCCESS
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                val rawMsg = e.message ?: "Failed to synchronize"
                _syncErrorMessage.value = if (rawMsg.contains("404")) {
                    "Your cloud sync group has expired or been removed. Please disconnect and create a new group."
                } else if (rawMsg.contains("500")) {
                    "The cloud sync server is currently experiencing issues. Please try again later."
                } else {
                    rawMsg
                }
            }
        }
    }

    fun forceUpload() {
        val code = _syncGroupCode.value
        if (code.isEmpty()) return

        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus.SYNCING
                val localPayload = SyncPayload(
                    members = repository.allMembers.first(),
                    goals = repository.allGoals.first(),
                    contributions = repository.allContributions.first(),
                    events = calendarRepository.allEvents.first(),
                    lastUpdated = System.currentTimeMillis()
                )
                SyncService.updateSyncPayload(code, localPayload)
                _lastSyncedTime.value = localPayload.lastUpdated
                _syncStatus.value = SyncStatus.SUCCESS
                saveSyncSettings()
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                val rawMsg = e.message ?: "Force upload failed"
                _syncErrorMessage.value = if (rawMsg.contains("404")) {
                    "Your cloud sync group has expired or been removed. Please disconnect and create a new group."
                } else if (rawMsg.contains("500")) {
                    "The cloud sync server is currently experiencing issues. Please try again later."
                } else {
                    rawMsg
                }
            }
        }
    }

    fun forceDownload() {
        val code = _syncGroupCode.value
        if (code.isEmpty()) return

        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus.SYNCING
                val remotePayload = SyncService.fetchSyncPayload(code)

                repository.clearAll()
                calendarRepository.clearAll()

                repository.insertAll(
                    remotePayload.members,
                    remotePayload.goals,
                    remotePayload.contributions
                )
                calendarRepository.insertAll(remotePayload.events)

                _lastSyncedTime.value = remotePayload.lastUpdated
                _syncStatus.value = SyncStatus.SUCCESS
                saveSyncSettings()

                // Refresh active member
                updateActiveMemberWithFallback(remotePayload.members)
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                val rawMsg = e.message ?: "Force download failed"
                _syncErrorMessage.value = if (rawMsg.contains("404")) {
                    "Your cloud sync group has expired or been removed. Please disconnect and create a new group."
                } else if (rawMsg.contains("500")) {
                    "The cloud sync server is currently experiencing issues. Please try again later."
                } else {
                    rawMsg
                }
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
}
