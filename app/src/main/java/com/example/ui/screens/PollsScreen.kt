package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FamilyMember
import com.example.data.Poll
import com.example.data.PollVote
import com.example.ui.PollsViewModel
import com.example.ui.theme.FeatureColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollsScreen(
    pollsViewModel: PollsViewModel,
    members: List<FamilyMember>,
    activeMemberId: Int,
    isActiveAdmin: Boolean,
    onNavigateBack: () -> Unit
) {
    val polls by pollsViewModel.polls.collectAsStateWithLifecycle()
    val votes by pollsViewModel.votes.collectAsStateWithLifecycle()

    var showAddPollDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Family Polls",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("polls_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Family Hub"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddPollDialog = true },
                        modifier = Modifier.testTag("polls_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create New Poll",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPollDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Poll") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .testTag("polls_fab")
                    .padding(bottom = 16.dp)
            )
        }
    ) { paddingValues ->
        if (polls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                            .background(FeatureColors.pollsContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = null,
                            tint = FeatureColors.polls,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Text(
                        text = "No Polls Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Ask the family something and let everyone vote. Tap 'New Poll' to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                items(
                    items = polls,
                    key = { it.id }
                ) { poll ->
                    val pollVotes = remember(votes, poll.id) { votes.filter { it.pollId == poll.id } }
                    val canModify = isActiveAdmin || (activeMemberId == poll.createdByMemberId)

                    PollCard(
                        poll = poll,
                        votes = pollVotes,
                        members = members,
                        activeMemberId = activeMemberId,
                        canModify = canModify,
                        onVote = { optionIndex -> pollsViewModel.castVote(poll.id, optionIndex) },
                        onClose = { pollsViewModel.closePoll(poll) },
                        onDelete = { pollsViewModel.deletePoll(poll) }
                    )
                }
            }
        }
    }

    if (showAddPollDialog) {
        AddPollDialog(
            onDismiss = { showAddPollDialog = false },
            onConfirm = { question, options ->
                pollsViewModel.createPoll(question, options)
                showAddPollDialog = false
            }
        )
    }
}

@Composable
fun PollCard(
    poll: Poll,
    votes: List<PollVote>,
    members: List<FamilyMember>,
    activeMemberId: Int,
    canModify: Boolean,
    onVote: (Int) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val creator = members.find { it.id == poll.createdByMemberId }
    val myVote = votes.find { it.memberId == activeMemberId }
    val totalVotes = votes.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("poll_card_${poll.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = poll.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Asked by ${creator?.name ?: "Former member"} • $totalVotes vote${if (totalVotes == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (poll.isClosed) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Closed") },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            poll.options.forEachIndexed { index, optionText ->
                val optionVoteCount = votes.count { it.optionIndex == index }
                val percentage = if (totalVotes > 0) optionVoteCount.toFloat() / totalVotes else 0f
                val isMyVote = myVote?.optionIndex == index

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isMyVote) FeatureColors.pollsContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .let {
                            if (!poll.isClosed) it.clickable { onVote(index) } else it
                        }
                        .border(
                            width = if (isMyVote) 2.dp else 0.dp,
                            color = if (isMyVote) FeatureColors.polls else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                        .testTag("vote_option_${poll.id}_$index"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isMyVote) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Your vote",
                                    tint = FeatureColors.polls,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isMyVote) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        Text(
                            text = "$optionVoteCount (${(percentage * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = FeatureColors.polls,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }

            if (canModify) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!poll.isClosed) {
                        TextButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("close_poll_btn_${poll.id}")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Close Voting")
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_poll_btn_${poll.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Poll",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddPollDialog(
    onDismiss: () -> Unit,
    onConfirm: (question: String, options: List<String>) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "")) }

    val validOptionCount = options.count { it.isNotBlank() }
    val canCreate = question.isNotBlank() && validOptionCount >= 2

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("add_poll_dialog"),
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
                    text = "Create a Poll",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question *") },
                    placeholder = { Text("e.g. Where should we eat Friday?") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("poll_input_question")
                )

                Text(
                    text = "Options (2-6)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                options.forEachIndexed { index, optionValue ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = optionValue,
                            onValueChange = { newValue ->
                                options = options.toMutableList().also { it[index] = newValue }
                            },
                            label = { Text("Option ${index + 1}") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("poll_input_option_$index")
                        )
                        if (options.size > 2) {
                            IconButton(
                                onClick = {
                                    options = options.toMutableList().also { it.removeAt(index) }
                                },
                                modifier = Modifier.testTag("poll_remove_option_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Remove option",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                if (options.size < 6) {
                    TextButton(
                        onClick = { options = options + "" },
                        modifier = Modifier.testTag("poll_add_option")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add option")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("poll_dialog_cancel")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (canCreate) {
                                onConfirm(question.trim(), options)
                            }
                        },
                        enabled = canCreate,
                        modifier = Modifier.testTag("poll_dialog_confirm"),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
