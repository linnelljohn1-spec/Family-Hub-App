package com.aistudio.familysavings.cleaninstall.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.*
import com.example.ui.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Helper function to safely parse a Hex color string to a Compose Color
fun parseColorHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF6750A4) // Default primary purple
    }
}

// Helper to safely get the first letter of first and last name or first two letters of name
fun getInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex())
    return if (parts.size >= 2) {
        (parts[0].take(1) + parts[1].take(1)).uppercase()
    } else {
        name.take(2).uppercase()
    }
}

// Predefined colors for family members
val familyColors = listOf(
    "#2E7D32" to "Emerald Green",
    "#D81B60" to "Vibrant Pink",
    "#1565C0" to "Ocean Blue",
    "#EF6C00" to "Sunset Orange",
    "#6A1B9A" to "Royal Purple",
    "#00838F" to "Teal Sea",
    "#FF8F00" to "Golden Amber"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilySavingsApp(viewModel: SavingsViewModel) {
    val membersWithPerformance by viewModel.familyPerformance.collectAsStateWithLifecycle()
    val monthlyReports by viewModel.monthlyReports.collectAsStateWithLifecycle()
    val activeMemberId by viewModel.activeMemberId.collectAsStateWithLifecycle()

    val activeMember = membersWithPerformance.find { it.member.id == activeMemberId }?.member
    val isActiveAdmin = activeMember?.isAdmin == true

    var currentBottomTab by remember { mutableStateOf(0) } // 0: Family, 1: Monthly Summary
    var selectedFamilyTabId by remember { mutableStateOf(-1) } // -1 for All Members
    var activeScreen by remember { mutableStateOf("hub") } // "hub", "savings", "calendar"

    // Default onto the user who is logged on when activeMemberId loads
    LaunchedEffect(activeMemberId) {
        if (activeMemberId != -1) {
            selectedFamilyTabId = activeMemberId
        }
    }

    // Dialog trigger states
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showAddGoalDialogForMemberId by remember { mutableStateOf<Int?>(null) }
    var showAddContributionDialogForGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showWithdrawDialogForGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showAddWalletDialogForMemberId by remember { mutableStateOf<Int?>(null) }
    var walletDialogInitialIsDeposit by remember { mutableStateOf(true) }
    var showConfirmDeleteMember by remember { mutableStateOf<FamilyMember?>(null) }
    var showSwitchProfileDialog by remember { mutableStateOf(false) }
    var memberPendingSwitch by remember { mutableStateOf<FamilyMember?>(null) }
    var switchPasswordInput by remember { mutableStateOf("") }
    var switchPasswordError by remember { mutableStateOf("") }
    var goalToToggleComplete by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoal?>(null) }

    AnimatedContent(
        targetState = activeScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "app_screens"
    ) { screen ->
        when (screen) {
            "hub" -> {
                HubScreen(
                    viewModel = viewModel,
                    onNavigateToSavings = { 
                        activeScreen = "savings" 
                        if (activeMemberId != -1) {
                            selectedFamilyTabId = activeMemberId
                        }
                    },
                    onNavigateToCalendar = { activeScreen = "calendar" },
                    onNavigateToTasks = { activeScreen = "tasks" },
                    onSwitchProfileClick = { showSwitchProfileDialog = true }
                )
            }
            "calendar" -> {
                CalendarScreen(
                    viewModel = viewModel,
                    onNavigateBack = { activeScreen = "hub" }
                )
            }
            "tasks" -> {
                TasksScreen(
                    viewModel = viewModel,
                    onNavigateBack = { activeScreen = "hub" }
                )
            }
            "savings" -> {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("app_scaffold"),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Family Savings",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    activeMember?.let { member ->
                                        Text(
                                            text = if (member.isAdmin) "Admin: ${member.name}" else "User: ${member.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (member.isAdmin) Color(0xFF6750A4) else Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { activeScreen = "hub" }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Hub"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            actions = {
                                if (isActiveAdmin) {
                                    IconButton(
                                        onClick = { showAddMemberDialog = true },
                                        modifier = Modifier.testTag("action_add_member")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = "Add Family Member",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(activeMember?.let { parseColorHex(it.avatarColorHex) } ?: Color(0xFF6750A4))
                                        .clickable { showSwitchProfileDialog = true },
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
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentBottomTab == 0,
                                onClick = { currentBottomTab = 0 },
                                icon = { Icon(Icons.Default.People, contentDescription = "Family View") },
                                label = { Text("Family") },
                                modifier = Modifier.testTag("nav_family")
                            )
                            NavigationBarItem(
                                selected = currentBottomTab == 1,
                                onClick = { currentBottomTab = 1 },
                                icon = { Icon(Icons.Default.DateRange, contentDescription = "Summary Reports View") },
                                label = { Text("Reports") },
                                modifier = Modifier.testTag("nav_reports")
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentBottomTab) {
                            0 -> {
                                // Family performance view with individual tabs
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Sleek capsule tabs layout
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .testTag("family_member_tabs"),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // "All Members" tab button
                                        val isAllSelected = selectedFamilyTabId == -1
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(
                                                    if (isAllSelected) Color(0xFFEADDFF) else Color(0xFFF3EDF7)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isAllSelected) Color.Transparent else Color(0xFFCAC4D0),
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedFamilyTabId = -1 }
                                                .padding(horizontal = 20.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "All Members",
                                                color = if (isAllSelected) Color(0xFF21005D) else Color(0xFF49454F),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // "Purchased" tab button
                                        val isPurchasedSelected = selectedFamilyTabId == -2
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(
                                                    if (isPurchasedSelected) Color(0xFFEADDFF) else Color(0xFFF3EDF7)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isPurchasedSelected) Color.Transparent else Color(0xFFCAC4D0),
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedFamilyTabId = -2 }
                                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                                .testTag("tab_purchased_items"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = if (isPurchasedSelected) Color(0xFF21005D) else Color(0xFF49454F),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Purchased",
                                                    color = if (isPurchasedSelected) Color(0xFF21005D) else Color(0xFF49454F),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Individual members
                                        membersWithPerformance.forEach { item ->
                                            val isSelected = selectedFamilyTabId == item.member.id
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) Color(0xFFEADDFF) else Color(0xFFF3EDF7)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) Color.Transparent else Color(0xFFCAC4D0),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { selectedFamilyTabId = item.member.id }
                                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .background(
                                                                parseColorHex(item.member.avatarColorHex),
                                                                CircleShape
                                                            )
                                                    )
                                                    Text(
                                                        text = item.member.name,
                                                        color = if (isSelected) Color(0xFF21005D) else Color(0xFF49454F),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }

                                        // Plus button tab: dashed border to add member
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF3EDF7))
                                                .border(
                                                    width = 1.dp,
                                                    color = Color(0xFF6750A4),
                                                    shape = CircleShape
                                                )
                                                .clickable { showAddMemberDialog = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Member Tab Shortcut",
                                                tint = Color(0xFF6750A4),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Content area based on selected tab
                                    AnimatedContent(
                                        targetState = selectedFamilyTabId,
                                        transitionSpec = {
                                            fadeIn() togetherWith fadeOut()
                                        },
                                        label = "family_content_animation",
                                        modifier = Modifier.weight(1f)
                                    ) { targetMemberId ->
                                        if (targetMemberId == -1) {
                                            // "All Members" Overview Screen
                                            AllMembersOverview(
                                                membersWithPerformance = membersWithPerformance,
                                                isActiveAdmin = isActiveAdmin,
                                                onSelectMember = { memberId -> selectedFamilyTabId = memberId },
                                                onAddMemberClick = { showAddMemberDialog = true }
                                            )
                                        } else if (targetMemberId == -2) {
                                            // "Purchased Items" Screen broken down per person
                                            PurchasedItemsOverview(
                                                membersWithPerformance = membersWithPerformance,
                                                isActiveAdmin = isActiveAdmin,
                                                onToggleGoalCompletionClick = { goal -> goalToToggleComplete = goal },
                                                onDeleteGoalClick = { goal -> goalToDelete = goal }
                                            )
                                        } else {
                                            // Specific Member Detail Screen
                                            val selectedItem = membersWithPerformance.find { it.member.id == targetMemberId }
                                            if (selectedItem != null) {
                                                MemberDetailView(
                                                    memberPerformance = selectedItem,
                                                    activeMemberId = activeMemberId,
                                                    isActiveAdmin = isActiveAdmin,
                                                    onAddGoalClick = { showAddGoalDialogForMemberId = targetMemberId },
                                                    onDepositClick = { goal -> showAddContributionDialogForGoal = goal },
                                                    onWithdrawClick = { goal -> showWithdrawDialogForGoal = goal },
                                                    onDepositToWalletClick = {
                                                        showAddWalletDialogForMemberId = targetMemberId
                                                        walletDialogInitialIsDeposit = true
                                                    },
                                                    onDeductFromWalletClick = {
                                                        showAddWalletDialogForMemberId = targetMemberId
                                                        walletDialogInitialIsDeposit = false
                                                    },
                                                    onDeleteGoalClick = { goal -> goalToDelete = goal },
                                                    onToggleGoalCompletionClick = { goal -> goalToToggleComplete = goal },
                                                    onDeleteMemberClick = { showConfirmDeleteMember = selectedItem.member },
                                                    onDeleteContribution = { contribution -> viewModel.deleteContribution(contribution) },
                                                    onUpdatePassword = { newPass -> viewModel.updateMemberPassword(selectedItem.member.id, newPass) },
                                                    onUpdateProfile = { newName, newColor -> viewModel.updateMemberProfile(selectedItem.member.id, newName, newColor) }
                                                )
                                            } else {
                                                // Fallback if deleted
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Family member not found.")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Monthly summary reports screen
                                MonthlySummaryReports(
                                    reports = monthlyReports,
                                    onDeleteContribution = { contribution -> viewModel.deleteContribution(contribution) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Add Family Member Dialog
    if (showAddMemberDialog) {
        var memberName by remember { mutableStateOf("") }
        var memberPassword by remember { mutableStateOf("1234") }
        var selectedColorHex by remember { mutableStateOf(familyColors.first().first) }
        var isMemberAdmin by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showAddMemberDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("add_member_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Add Family Member",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_member_name")
                    )

                    OutlinedTextField(
                        value = memberPassword,
                        onValueChange = { memberPassword = it },
                        label = { Text("Password (default: 1234)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_member_password")
                    )

                    Text(
                        text = "Choose Profile Color:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Color picker grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        familyColors.forEach { (hex, _) ->
                            val color = parseColorHex(hex)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (selectedColorHex == hex) 3.dp else 0.dp,
                                        color = if (selectedColorHex == hex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }

                    // Admin Role Checkbox Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isMemberAdmin,
                            onCheckedChange = { isMemberAdmin = it }
                        )
                        Column {
                            Text(
                                text = "Make Admin / Head of Family",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Can give pocket money and manage files.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddMemberDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (memberName.isNotBlank()) {
                                    viewModel.addMember(
                                        name = memberName.trim(),
                                        colorHex = selectedColorHex,
                                        isAdmin = isMemberAdmin,
                                        password = if (memberPassword.isBlank()) "1234" else memberPassword.trim()
                                    )
                                    showAddMemberDialog = false
                                }
                            },
                            enabled = memberName.isNotBlank(),
                            modifier = Modifier.testTag("btn_save_member")
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }

    // 2. Add Savings Goal Dialog
    showAddGoalDialogForMemberId?.let { memberId ->
        var goalTitle by remember { mutableStateOf("") }
        var targetAmountText by remember { mutableStateOf("") }
        var purchaseUrl by remember { mutableStateOf("") }
        var hasError by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showAddGoalDialogForMemberId = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("add_goal_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create Savings Goal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("What are you saving for?") },
                        placeholder = { Text("e.g., Bike, Vacation, College") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_goal_title")
                    )

                    OutlinedTextField(
                        value = targetAmountText,
                        onValueChange = {
                            targetAmountText = it
                            hasError = it.toDoubleOrNull() == null || it.toDouble() <= 0
                        },
                        label = { Text("Target Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = hasError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_goal_target")
                    )

                    OutlinedTextField(
                        value = purchaseUrl,
                        onValueChange = { purchaseUrl = it },
                        label = { Text("Purchase / Shop Link (Optional)") },
                        placeholder = { Text("e.g., https://amazon.com/...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_goal_purchase_url")
                    )

                    if (hasError) {
                        Text(
                            text = "Please enter a valid positive number",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddGoalDialogForMemberId = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = targetAmountText.toDoubleOrNull()
                                if (goalTitle.isNotBlank() && amount != null && amount > 0) {
                                    viewModel.addGoal(
                                        memberId, 
                                        goalTitle.trim(), 
                                        amount, 
                                        purchaseUrl.trim().ifEmpty { null }
                                    )
                                    showAddGoalDialogForMemberId = null
                                }
                            },
                            enabled = goalTitle.isNotBlank() && targetAmountText.toDoubleOrNull() != null && targetAmountText.toDouble() > 0,
                            modifier = Modifier.testTag("btn_save_goal")
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    // 3. Add Contribution Dialog
    showAddContributionDialogForGoal?.let { goal ->
        var depositAmountText by remember { mutableStateOf("") }
        var depositNote by remember { mutableStateOf("") }
        var hasError by remember { mutableStateOf(false) }

        val memberPerformance = membersWithPerformance.find { it.member.id == goal.memberId }
        val walletBalance = memberPerformance?.member?.unallocatedBalance ?: 0.0
        val memberColor = memberPerformance?.let { parseColorHex(it.member.avatarColorHex) } ?: Color(0xFF6750A4)

        Dialog(onDismissRequest = { showAddContributionDialogForGoal = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("add_contribution_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Allocate Wallet Funds",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Adding money to: ${goal.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Wallet Balance Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(memberColor.copy(alpha = 0.08f))
                            .border(1.dp, memberColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet Balance",
                                tint = memberColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Available Wallet Balance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = NumberFormat.getCurrencyInstance().format(walletBalance),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = memberColor
                                )
                            }
                        }
                    }

                    if (walletBalance <= 0.0) {
                        Text(
                            text = "No available funds in personal wallet. An Admin/Head of Family must add pocket money to your wallet before you can allocate to goals.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = depositAmountText,
                            onValueChange = {
                                depositAmountText = it
                                hasError = it.toDoubleOrNull() == null || it.toDouble() <= 0
                            },
                            label = { Text("Allocation Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = hasError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_contribution_amount")
                        )

                        OutlinedTextField(
                            value = depositNote,
                            onValueChange = { depositNote = it },
                            label = { Text("Note (Optional)") },
                            placeholder = { Text("e.g., Transfer from my personal fund") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_contribution_note")
                        )
                    }

                    val enteredAmount = depositAmountText.toDoubleOrNull()
                    val walletBalanceExceeded = enteredAmount != null && enteredAmount > walletBalance

                    if (hasError && walletBalance > 0.0) {
                        Text(
                            text = "Please enter a valid positive number",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (walletBalanceExceeded) {
                        Text(
                            text = "Amount exceeds available wallet balance of ${NumberFormat.getCurrencyInstance().format(walletBalance)}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddContributionDialogForGoal = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = depositAmountText.toDoubleOrNull()
                                if (amount != null && amount > 0) {
                                    viewModel.allocateUnallocatedFundToGoal(
                                        memberId = goal.memberId,
                                        goalId = goal.id,
                                        amount = amount,
                                        note = depositNote.trim().ifEmpty { "Allocated from personal fund" }
                                    )
                                    showAddContributionDialogForGoal = null
                                }
                            },
                            enabled = walletBalance > 0.0 && depositAmountText.toDoubleOrNull() != null && depositAmountText.toDouble() > 0 && !walletBalanceExceeded,
                            modifier = Modifier.testTag("btn_save_contribution")
                        ) {
                            Text("Allocate")
                        }
                    }
                }
            }
        }
    }

    // 5. Add Wallet Funds Dialog
    showAddWalletDialogForMemberId?.let { memberId ->
        val currentMemberItem = membersWithPerformance.find { it.member.id == memberId }
        val memberName = currentMemberItem?.member?.name ?: "Member"
        val memberColor = currentMemberItem?.let { parseColorHex(it.member.avatarColorHex) } ?: Color(0xFF6750A4)
        var walletAmountText by remember { mutableStateOf("") }
        var hasError by remember { mutableStateOf(false) }
        var isDepositMode by remember(walletDialogInitialIsDeposit) { mutableStateOf(walletDialogInitialIsDeposit) }

        Dialog(onDismissRequest = { showAddWalletDialogForMemberId = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("add_to_wallet_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Manage Wallet Funds",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Segmented Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Deposit Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDepositMode) memberColor else Color.Transparent)
                                .clickable { isDepositMode = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Deposit",
                                color = if (isDepositMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Deduct Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isDepositMode) memberColor else Color.Transparent)
                                .clickable { isDepositMode = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Remove",
                                color = if (!isDepositMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    val walletBalance = currentMemberItem?.member?.unallocatedBalance ?: 0.0
                    
                    // Wallet Balance Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(memberColor.copy(alpha = 0.08f))
                            .border(1.dp, memberColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet Balance",
                                tint = memberColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Current Wallet Balance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = NumberFormat.getCurrencyInstance().format(walletBalance),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = memberColor
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isDepositMode) {
                            "Deposit pocket money or other allowances to $memberName's personal fund. They can assign this money later to active savings goals."
                        } else {
                            "Remove or deduct funds from $memberName's personal fund (e.g., if they spent it, or for correction)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    val enteredAmount = walletAmountText.toDoubleOrNull()
                    val isValidAmount = enteredAmount != null && enteredAmount > 0
                    val isLimitExceeded = !isDepositMode && enteredAmount != null && enteredAmount > walletBalance

                    OutlinedTextField(
                        value = walletAmountText,
                        onValueChange = {
                            walletAmountText = it
                            hasError = it.toDoubleOrNull() == null || it.toDouble() <= 0
                        },
                        label = { Text(if (isDepositMode) "Deposit Amount ($)" else "Removal Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = hasError || isLimitExceeded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_wallet_amount")
                    )

                    if (hasError) {
                        Text(
                            text = "Please enter a valid positive number",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (isLimitExceeded) {
                        Text(
                            text = "Amount exceeds available wallet balance of ${NumberFormat.getCurrencyInstance().format(walletBalance)}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddWalletDialogForMemberId = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = walletAmountText.toDoubleOrNull()
                                if (amount != null && amount > 0) {
                                    if (isDepositMode) {
                                        viewModel.depositToUnallocatedFund(memberId, amount)
                                    } else {
                                        viewModel.deductFromUnallocatedFund(memberId, amount)
                                    }
                                    showAddWalletDialogForMemberId = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = memberColor),
                            enabled = isValidAmount && !isLimitExceeded,
                            modifier = Modifier.testTag("btn_save_wallet_funds")
                        ) {
                            Text(if (isDepositMode) "Deposit" else "Deduct")
                        }
                    }
                }
            }
        }
    }

    // 6. Withdraw Funds from Goal Dialog
    showWithdrawDialogForGoal?.let { goal ->
        var withdrawAmountText by remember { mutableStateOf("") }
        var withdrawNote by remember { mutableStateOf("") }
        var hasError by remember { mutableStateOf(false) }

        val memberPerformance = membersWithPerformance.find { it.member.id == goal.memberId }
        val goalPerformance = memberPerformance?.goalsWithProgress?.find { it.goal.id == goal.id }
        val currentAmountSaved = goalPerformance?.currentAmount ?: 0.0
        val memberColor = memberPerformance?.let { parseColorHex(it.member.avatarColorHex) } ?: Color(0xFF6750A4)

        Dialog(onDismissRequest = { showWithdrawDialogForGoal = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("withdraw_from_goal_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Withdraw Goal Funds",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Return money from goal \"${goal.title}\" back to your personal wallet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Current Goal Saved Amount Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(memberColor.copy(alpha = 0.08f))
                            .border(1.dp, memberColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = "Savings Amount",
                                tint = memberColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Current Saved Amount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = NumberFormat.getCurrencyInstance().format(currentAmountSaved),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = memberColor
                                )
                            }
                        }
                    }

                    if (currentAmountSaved <= 0.0) {
                        Text(
                            text = "There are no funds saved towards this goal to withdraw.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = withdrawAmountText,
                            onValueChange = {
                                withdrawAmountText = it
                                hasError = it.toDoubleOrNull() == null || it.toDouble() <= 0
                            },
                            label = { Text("Withdrawal Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = hasError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_withdraw_amount")
                        )

                        OutlinedTextField(
                            value = withdrawNote,
                            onValueChange = { withdrawNote = it },
                            label = { Text("Note (Optional)") },
                            placeholder = { Text("e.g., Transferring back to wallet") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_withdraw_note")
                        )
                    }

                    val enteredAmount = withdrawAmountText.toDoubleOrNull()
                    val limitExceeded = enteredAmount != null && enteredAmount > currentAmountSaved

                    if (hasError && currentAmountSaved > 0.0) {
                        Text(
                            text = "Please enter a valid positive number",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (limitExceeded) {
                        Text(
                            text = "Amount exceeds current saved balance of ${NumberFormat.getCurrencyInstance().format(currentAmountSaved)}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showWithdrawDialogForGoal = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = withdrawAmountText.toDoubleOrNull()
                                if (amount != null && amount > 0) {
                                    viewModel.withdrawFundFromGoal(
                                        memberId = goal.memberId,
                                        goalId = goal.id,
                                        amount = amount,
                                        note = withdrawNote.trim().ifEmpty { "Withdrawn from goal" }
                                    )
                                    showWithdrawDialogForGoal = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = memberColor),
                            enabled = currentAmountSaved > 0.0 && withdrawAmountText.toDoubleOrNull() != null && withdrawAmountText.toDouble() > 0 && !limitExceeded,
                            modifier = Modifier.testTag("btn_save_withdrawal")
                        ) {
                            Text("Withdraw")
                        }
                    }
                }
            }
        }
    }

    // 4. Confirm Delete Member Dialog
    showConfirmDeleteMember?.let { member ->
        AlertDialog(
            onDismissRequest = { showConfirmDeleteMember = null },
            title = { Text("Delete Family Member?") },
            text = { Text("This will permanently delete ${member.name} and all their savings goals and contributions. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMember(member)
                        selectedFamilyTabId = -1 // Reset tab back to All
                        showConfirmDeleteMember = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_member")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteMember = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Toggle Goal Completion Dialog
    goalToToggleComplete?.let { goal ->
        val dialogTitle = if (goal.isCompleted) "Reopen Savings Goal?" else "Mark Goal as Completed?"
        val dialogText = if (goal.isCompleted) {
            "Are you sure you want to reopen \"${goal.title}\"? It will be moved back to the active savings list."
        } else {
            "Are you sure you want to mark \"${goal.title}\" as completed (purchased)? It will be moved to the Purchased items list."
        }
        val confirmButtonText = if (goal.isCompleted) "Reopen" else "Complete"
        
        AlertDialog(
            onDismissRequest = { goalToToggleComplete = null },
            title = { Text(dialogTitle) },
            text = { Text(dialogText) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleGoalCompletion(goal)
                        goalToToggleComplete = null
                    },
                    modifier = Modifier.testTag("btn_confirm_toggle_complete")
                ) {
                    Text(confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToToggleComplete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Delete Goal Dialog
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Delete Savings Goal?") },
            text = { Text("Are you sure you want to permanently delete \"${goal.title}\" and all of its contribution history? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goal)
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_goal")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Switch Profile Dialog
    if (showSwitchProfileDialog) {
        Dialog(onDismissRequest = { showSwitchProfileDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("switch_profile_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Switch Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select a family member's profile to view and allocate savings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                    ) {
                        items(membersWithPerformance) { item ->
                            val isSelected = item.member.id == activeMemberId
                            val memberColor = parseColorHex(item.member.avatarColorHex)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) memberColor.copy(alpha = 0.12f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) memberColor else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        memberPendingSwitch = item.member
                                        switchPasswordInput = ""
                                        switchPasswordError = ""
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(memberColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = getInitials(item.member.name),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.member.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1B20)
                                    )
                                    Text(
                                        text = if (item.member.isAdmin) "Admin • Head of Family" else "Family Member",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (item.member.isAdmin) Color(0xFF6750A4) else Color.Gray,
                                        fontWeight = if (item.member.isAdmin) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (item.member.unallocatedBalance > 0.0) {
                                    Text(
                                        text = NumberFormat.getCurrencyInstance().format(item.member.unallocatedBalance),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSwitchProfileDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    if (memberPendingSwitch != null) {
        val targetMember = memberPendingSwitch!!
        Dialog(onDismissRequest = { memberPendingSwitch = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("switch_password_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Enter Password",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "A password is required to switch to ${targetMember.name}'s profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = switchPasswordInput,
                        onValueChange = { 
                            switchPasswordInput = it
                            switchPasswordError = ""
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        isError = switchPasswordError.isNotEmpty(),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_switch_password")
                    )

                    if (switchPasswordError.isNotEmpty()) {
                        Text(
                            text = switchPasswordError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { memberPendingSwitch = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (switchPasswordInput == targetMember.password) {
                                    viewModel.setActiveMember(targetMember.id)
                                    memberPendingSwitch = null
                                    showSwitchProfileDialog = false
                                } else {
                                    switchPasswordError = "Incorrect password. Please try again."
                                }
                            },
                            modifier = Modifier.testTag("btn_confirm_switch")
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}

// --- Content Sections ---

@Composable
fun AllMembersOverview(
    membersWithPerformance: List<MemberWithPerformance>,
    isActiveAdmin: Boolean,
    onSelectMember: (Int) -> Unit,
    onAddMemberClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("all_members_overview"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Combined Family Goal Performance Card styled to match Sleek Interface Monthly Summary Card
        if (membersWithPerformance.isNotEmpty()) {
            val totalSavedCombined = membersWithPerformance.sumOf { it.totalSaved }
            val totalTargetCombined = membersWithPerformance.sumOf { it.totalTarget }
            val combinedProgress = if (totalTargetCombined > 0) {
                (totalSavedCombined / totalTargetCombined).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3EDF7)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "SAVINGS SUMMARY",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF6750A4),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = NumberFormat.getCurrencyInstance().format(totalSavedCombined),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1D1B20)
                                )
                            }
                            
                            // Sleek white progress badge
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "SAVED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = String.format("%.0f%%", combinedProgress * 100),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "You are ${String.format("%.0f%%", combinedProgress * 100)} towards your monthly family contribution target.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF49454F)
                        )

                        LinearProgressIndicator(
                            progress = { combinedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = Color(0xFF6750A4),
                            trackColor = Color.White
                        )
                        
                        Text(
                            text = "Combined target: ${NumberFormat.getCurrencyInstance().format(totalTargetCombined)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF49454F).copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Section Title: Member Rankings / Performances
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Family Members",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isActiveAdmin) {
                    TextButton(
                        onClick = onAddMemberClick,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Person")
                    }
                }
            }
        }

        if (membersWithPerformance.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No family members added yet!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isActiveAdmin) "Click 'Add Person' at the top right to start tracking together." else "Please ask an Admin to add family members.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isActiveAdmin) {
                        Button(onClick = onAddMemberClick, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Add First Member")
                        }
                    }
                }
            }
        } else {
            items(membersWithPerformance) { item ->
                val memberColor = parseColorHex(item.member.avatarColorHex)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMember(item.member.id) }
                        .testTag("member_card_${item.member.name.lowercase()}"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Avatar bubble with color representing member
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(memberColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.member.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }

                        // Text performance column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.member.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D1B20)
                                        )
                                        if (item.member.isAdmin) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "ADMIN",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF6750A4),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (item.member.unallocatedBalance > 0.0) {
                                        Text(
                                            text = "Wallet: ${NumberFormat.getCurrencyInstance().format(item.member.unallocatedBalance)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6750A4)
                                        )
                                    }
                                }
                                Text(
                                    text = String.format("%.0f%%", item.performanceProgress * 100),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = memberColor
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Total progress text
                            Text(
                                text = "${NumberFormat.getCurrencyInstance().format(item.totalSaved)} saved of ${NumberFormat.getCurrencyInstance().format(item.totalTarget)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454F)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Colored individual progress bar
                            LinearProgressIndicator(
                                progress = { item.performanceProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = memberColor,
                                trackColor = Color(0xFFF3EDF7)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open profile",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberDetailView(
    memberPerformance: MemberWithPerformance,
    activeMemberId: Int,
    isActiveAdmin: Boolean,
    onAddGoalClick: () -> Unit,
    onDepositClick: (SavingsGoal) -> Unit,
    onWithdrawClick: (SavingsGoal) -> Unit,
    onDepositToWalletClick: () -> Unit,
    onDeductFromWalletClick: () -> Unit,
    onDeleteGoalClick: (SavingsGoal) -> Unit,
    onToggleGoalCompletionClick: (SavingsGoal) -> Unit,
    onDeleteMemberClick: () -> Unit,
    onDeleteContribution: (Contribution) -> Unit,
    onUpdatePassword: (String) -> Unit,
    onUpdateProfile: (String, String) -> Unit
) {
    val memberColor = parseColorHex(memberPerformance.member.avatarColorHex)
    val isOwnProfile = activeMemberId == memberPerformance.member.id
    val canAddGoal = isActiveAdmin || isOwnProfile
    val canDeleteGoal = isActiveAdmin
    val canDepositToGoal = isActiveAdmin || isOwnProfile
    val canAddWalletFunds = isActiveAdmin

    val activeGoals = memberPerformance.goalsWithProgress.filter { !it.goal.isCompleted }
    val canDeleteMember = isActiveAdmin && !memberPerformance.member.isAdmin

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("member_detail_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Individual Profile Header Card with rich custom gradient
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = memberColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "${memberPerformance.member.name}'s Summary",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Delete member option
                        if (canDeleteMember) {
                            IconButton(onClick = onDeleteMemberClick) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete family member",
                                    tint = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = NumberFormat.getCurrencyInstance().format(memberPerformance.totalSaved),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "of ${NumberFormat.getCurrencyInstance().format(memberPerformance.totalTarget)} target",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Text(
                            text = String.format("%.0f%%", memberPerformance.performanceProgress * 100),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    LinearProgressIndicator(
                        progress = { memberPerformance.performanceProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }

        // Sleek Personal Wallet Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(memberColor.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "Personal Wallet",
                                tint = memberColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Personal Fund Wallet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                            Text(
                                text = "Money ready to allocate to goals",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = NumberFormat.getCurrencyInstance().format(memberPerformance.member.unallocatedBalance),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = memberColor
                        )
                        if (canAddWalletFunds) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onDeductFromWalletClick,
                                    border = BorderStroke(1.dp, memberColor),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = memberColor),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("btn_deduct_wallet_${memberPerformance.member.name.lowercase()}")
                                ) {
                                    Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = onDepositToWalletClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = memberColor),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("btn_deposit_wallet_${memberPerformance.member.name.lowercase()}")
                                ) {
                                    Text("Add Money", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Password section if it is their own profile
        if (isOwnProfile) {
            item {
                var showEditProfileDialog by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable { showEditProfileDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Edit Profile Info",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Update your profile name and theme color",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (showEditProfileDialog) {
                    var editNameInput by remember { mutableStateOf(memberPerformance.member.name) }
                    var editColorHex by remember { mutableStateOf(memberPerformance.member.avatarColorHex) }
                    var editError by remember { mutableStateOf("") }

                    Dialog(onDismissRequest = { showEditProfileDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Edit Profile Info",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = editNameInput,
                                    onValueChange = { 
                                        editNameInput = it
                                        editError = ""
                                    },
                                    label = { Text("Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_edit_member_name")
                                )

                                Text(
                                    text = "Choose Profile Color:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                // Color picker grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    familyColors.forEach { (hex, _) ->
                                        val color = parseColorHex(hex)
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(color, CircleShape)
                                                .border(
                                                    width = if (editColorHex == hex) 3.dp else 0.dp,
                                                    color = if (editColorHex == hex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { editColorHex = hex }
                                        )
                                    }
                                }

                                if (editError.isNotEmpty()) {
                                    Text(
                                        text = editError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { showEditProfileDialog = false }) {
                                        Text("Cancel")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (editNameInput.isBlank()) {
                                                editError = "Name cannot be empty"
                                            } else {
                                                onUpdateProfile(editNameInput.trim(), editColorHex)
                                                showEditProfileDialog = false
                                            }
                                        },
                                        modifier = Modifier.testTag("btn_save_profile_info")
                                    ) {
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                var showChangePasswordDialog by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable { showChangePasswordDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Change Password",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Profile Password",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap to update your account password",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (showChangePasswordDialog) {
                    var newPassword by remember { mutableStateOf("") }
                    var confirmPassword by remember { mutableStateOf("") }
                    var passwordError by remember { mutableStateOf("") }

                    Dialog(onDismissRequest = { showChangePasswordDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Change Password",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { 
                                        newPassword = it
                                        passwordError = ""
                                    },
                                    label = { Text("New Password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("input_new_password")
                                )

                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { 
                                        confirmPassword = it
                                        passwordError = ""
                                    },
                                    label = { Text("Confirm New Password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("input_confirm_password")
                                )

                                if (passwordError.isNotEmpty()) {
                                    Text(
                                        text = passwordError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { showChangePasswordDialog = false }) {
                                        Text("Cancel")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (newPassword.isBlank()) {
                                                passwordError = "Password cannot be empty"
                                            } else if (newPassword != confirmPassword) {
                                                passwordError = "Passwords do not match"
                                            } else {
                                                onUpdatePassword(newPassword.trim())
                                                showChangePasswordDialog = false
                                            }
                                        },
                                        modifier = Modifier.testTag("btn_save_new_password")
                                    ) {
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Goals List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Savings Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (canAddGoal) {
                    Button(
                        onClick = onAddGoalClick,
                        colors = ButtonDefaults.buttonColors(containerColor = memberColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_add_goal")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Goal")
                    }
                }
            }
        }

        // Empty State or Goals List
        if (activeGoals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = memberColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No active savings goals!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Create a custom savings target (e.g. computer, trip, pet) and start recording payments.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(activeGoals) { goalItem ->
                SavingsGoalCard(
                    goalItem = goalItem,
                    memberColor = memberColor,
                    canDepositToGoal = canDepositToGoal,
                    canDeleteGoal = canDeleteGoal,
                    onDepositClick = { onDepositClick(goalItem.goal) },
                    onWithdrawClick = { onWithdrawClick(goalItem.goal) },
                    onDeleteGoalClick = { onDeleteGoalClick(goalItem.goal) },
                    onToggleGoalCompletionClick = { onToggleGoalCompletionClick(goalItem.goal) },
                    onDeleteContribution = onDeleteContribution
                )
            }
        }
    }
}

// Helper to return a fun emoji based on the savings goal title
fun getEmojiForGoal(title: String): String {
    val t = title.lowercase()
    return when {
        t.contains("bike") || t.contains("cycle") || t.contains("wheel") -> "🚲"
        t.contains("trip") || t.contains("vacation") || t.contains("travel") || t.contains("flight") || t.contains("summer") || t.contains("beach") -> "✈️"
        t.contains("car") || t.contains("drive") || t.contains("vehicle") || t.contains("auto") -> "🚗"
        t.contains("computer") || t.contains("laptop") || t.contains("phone") || t.contains("device") || t.contains("game") || t.contains("tech") || t.contains("pc") || t.contains("ipad") -> "💻"
        t.contains("pet") || t.contains("dog") || t.contains("cat") || t.contains("animal") || t.contains("kitten") || t.contains("puppy") -> "🐾"
        t.contains("college") || t.contains("school") || t.contains("study") || t.contains("book") || t.contains("course") || t.contains("uni") -> "🎓"
        t.contains("house") || t.contains("home") || t.contains("furniture") || t.contains("room") || t.contains("flat") || t.contains("bed") -> "🏠"
        t.contains("food") || t.contains("dinner") || t.contains("eat") || t.contains("restaurant") || t.contains("cooking") || t.contains("lunch") -> "🍕"
        t.contains("gift") || t.contains("present") || t.contains("birthday") || t.contains("christmas") || t.contains("anniversary") -> "🎁"
        t.contains("toy") || t.contains("lego") || t.contains("doll") -> "🧸"
        t.contains("shoe") || t.contains("clothes") || t.contains("dress") || t.contains("jacket") || t.contains("boot") -> "👟"
        t.contains("music") || t.contains("song") || t.contains("guitar") || t.contains("piano") || t.contains("concert") || t.contains("band") -> "🎵"
        else -> "💰"
    }
}

@Composable
fun SavingsGoalCard(
    goalItem: GoalWithProgress,
    memberColor: Color,
    canDepositToGoal: Boolean,
    canDeleteGoal: Boolean,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onDeleteGoalClick: () -> Unit,
    onToggleGoalCompletionClick: () -> Unit,
    onDeleteContribution: (Contribution) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val goalSdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goalItem.goal.title.lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Emoji, Title & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Emoji container styled like HTML box
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(memberColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getEmojiForGoal(goalItem.goal.title),
                            fontSize = 20.sp
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = goalItem.goal.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (goalItem.goal.isCompleted) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "COMPLETED",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        val uriHandler = LocalUriHandler.current
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Added ${goalSdf.format(Date(goalItem.goal.createdAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Normal
                            )
                            goalItem.goal.purchaseUrl?.let { url ->
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Buy Link ↗",
                                    color = memberColor,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier
                                        .clickable {
                                            try {
                                                uriHandler.openUri(url)
                                            } catch (e: Exception) {
                                                // ignore
                                            }
                                        }
                                        .testTag("btn_buy_${goalItem.goal.title.lowercase()}")
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (canDepositToGoal) {
                        IconButton(
                            onClick = onToggleGoalCompletionClick,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_toggle_complete_${goalItem.goal.title.lowercase()}")
                        ) {
                            Icon(
                                imageVector = if (goalItem.goal.isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                contentDescription = "Toggle Complete",
                                tint = if (goalItem.goal.isCompleted) Color(0xFF2E7D32) else Color.Gray.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else if (goalItem.goal.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 4.dp)
                        )
                    }

                    Text(
                        text = String.format("%.0f%%", goalItem.progress * 100),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = memberColor
                    )
                    
                    if (canDeleteGoal) {
                        IconButton(
                            onClick = onDeleteGoalClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete savings goal",
                                tint = Color.Gray.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Visual Progress Bar - 12dp height and fully rounded to match h-3 in HTML
            LinearProgressIndicator(
                progress = { goalItem.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = memberColor,
                trackColor = Color(0xFFF1F5F9)
            )

            // Amounts Row with styled bold labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Saved",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = NumberFormat.getCurrencyInstance().format(goalItem.currentAmount),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = memberColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = NumberFormat.getCurrencyInstance().format(goalItem.goal.targetAmount),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                }
            }

            // Bottom Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand Contributions Button
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Hide Deposits" else "View Deposits (${goalItem.contributions.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6750A4),
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF6750A4)
                    )
                }

                // Withdraw and Deposit buttons (Capsule shaped buttons matching member profile color)
                if (canDepositToGoal) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onWithdrawClick,
                            border = BorderStroke(1.dp, memberColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = memberColor),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_withdraw_${goalItem.goal.title.lowercase()}")
                        ) {
                            Text("Withdraw", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDepositClick,
                            colors = ButtonDefaults.buttonColors(containerColor = memberColor),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_deposit_${goalItem.goal.title.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Deposit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Expanded deposits detail section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (goalItem.contributions.isEmpty()) {
                        Text(
                            text = "No deposits made towards this goal yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        goalItem.contributions.forEach { contribution ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val isNegative = contribution.amount < 0
                                        Text(
                                            text = NumberFormat.getCurrencyInstance().format(contribution.amount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNegative) MaterialTheme.colorScheme.error else memberColor
                                        )
                                        Text(
                                            text = sdf.format(Date(contribution.timestamp)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    contribution.note?.let { note ->
                                        Text(
                                            text = "\"$note\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (canDeleteGoal) {
                                    IconButton(
                                        onClick = { onDeleteContribution(contribution) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete deposit record",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

// --- Monthly Summary Reports Section ---

@Composable
fun MonthlySummaryReports(
    reports: List<MonthlyReport>,
    onDeleteContribution: (Contribution) -> Unit
) {
    var expandedReportMonth by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("monthly_reports_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Monthly Performance Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Compare contributions and see who is hitting their goals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (reports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No reports generated yet!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monthly comparative summaries appear automatically once contributions are added.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(reports) { report ->
                val isExpanded = expandedReportMonth == report.monthYearString

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_card_${report.monthYearString.lowercase().replace(" ", "_")}"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Month & total row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = report.monthYearString,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${report.contributionsCount} deposits this month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = NumberFormat.getCurrencyInstance().format(report.totalSavedThisMonth),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Comparative visual progress bar breakdown
                        if (report.memberContributions.isNotEmpty()) {
                            Text(
                                text = "Savings Distribution By Member",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Stacked horizontal bar chart
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                report.memberContributions.forEach { summary ->
                                    val memberColor = parseColorHex(summary.member.avatarColorHex)
                                    if (summary.percentOfTotalMonthSavings > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(summary.percentOfTotalMonthSavings)
                                                .background(memberColor)
                                                .border(
                                                    width = 1.dp,
                                                    color = Color.White.copy(alpha = 0.3f)
                                                )
                                        )
                                    }
                                }
                            }

                            // Member contribution descriptions grid
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                report.memberContributions.forEach { summary ->
                                    val memberColor = parseColorHex(summary.member.avatarColorHex)
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
                                                    .size(12.dp)
                                                    .background(memberColor, CircleShape)
                                            )
                                            Text(
                                                text = summary.member.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = NumberFormat.getCurrencyInstance().format(summary.amountSavedThisMonth),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = String.format("(%.0f%%)", summary.percentOfTotalMonthSavings * 100),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Expand Details Trigger
                        TextButton(
                            onClick = {
                                expandedReportMonth = if (isExpanded) null else report.monthYearString
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(if (isExpanded) "Hide Full Logs" else "View Individual Transactions")
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }

                        // Full Logs table inside month
                        AnimatedVisibility(visible = isExpanded) {
                            // Fetch raw transactions in current month
                            // To display them cleanly, we can find them from our report
                            // Since report only has aggregated numbers, let's create a custom list layout
                            // or just list them here. Wait, since we are doing offline calculations in compile,
                            // having an expandable list is incredibly useful and complete!
                            // We will display a text summary or message
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Deposit Activity logs:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "All deposits made in ${report.monthYearString} are shown on each member's individual tabs.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchasedItemsOverview(
    membersWithPerformance: List<MemberWithPerformance>,
    isActiveAdmin: Boolean,
    onToggleGoalCompletionClick: (SavingsGoal) -> Unit,
    onDeleteGoalClick: (SavingsGoal) -> Unit
) {
    // Collect all completed goals per member
    val membersWithCompletedGoals = membersWithPerformance.map { item ->
        val completed = item.goalsWithProgress.filter { it.goal.isCompleted }
        Pair(item, completed)
    }.filter { it.second.isNotEmpty() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("purchased_items_overview"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Purchased Items",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Bold
                        )
                        val totalPurchased = membersWithCompletedGoals.sumOf { it.second.size }
                        val totalSaved = membersWithCompletedGoals.sumOf { it.second.sumOf { it.currentAmount } }
                        Text(
                            text = "$totalPurchased items purchased across the family (" + NumberFormat.getCurrencyInstance().format(totalSaved) + " saved!)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        if (membersWithCompletedGoals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No purchased items yet!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Keep saving! Once a goal reaches its target, mark it complete to see it here.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Grouped by Person
            membersWithCompletedGoals.forEach { (memberPerformance, completedList) ->
                val memberColor = parseColorHex(memberPerformance.member.avatarColorHex)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(memberColor, CircleShape)
                        )
                        Text(
                            text = "${memberPerformance.member.name}'s Purchases",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .background(memberColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${completedList.size} items",
                                color = memberColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                items(completedList) { goalItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("purchased_card_${goalItem.goal.title.lowercase()}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(memberColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = getEmojiForGoal(goalItem.goal.title),
                                            fontSize = 20.sp
                                        )
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = goalItem.goal.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D1B20),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        val uriHandler = LocalUriHandler.current
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Completed!",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                            goalItem.goal.purchaseUrl?.let { url ->
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "Buy Link ↗",
                                                    color = memberColor,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    textDecoration = TextDecoration.Underline,
                                                    modifier = Modifier
                                                        .clickable {
                                                            try {
                                                                uriHandler.openUri(url)
                                                            } catch (e: Exception) {
                                                                // ignore
                                                            }
                                                        }
                                                        .testTag("btn_buy_purchased_${goalItem.goal.title.lowercase()}")
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onToggleGoalCompletionClick(goalItem.goal) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("btn_reopen_purchased_${goalItem.goal.title.lowercase()}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Mark Incomplete",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (isActiveAdmin) {
                                        IconButton(
                                            onClick = { onDeleteGoalClick(goalItem.goal) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete purchased goal",
                                                tint = Color.Gray.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Saved/Spent:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                Text(
                                    text = NumberFormat.getCurrencyInstance().format(goalItem.currentAmount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1B20)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
