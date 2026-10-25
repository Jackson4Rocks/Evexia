package com.jackson4rocks.evexia.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jackson4rocks.evexia.data.LibraryItemEntity
import com.jackson4rocks.evexia.data.NoteEntity
import com.jackson4rocks.evexia.ui.AppUiState
import com.jackson4rocks.evexia.ui.FlowAiConversationSummary
import com.jackson4rocks.evexia.ui.FlowAiMessage
import com.jackson4rocks.evexia.ui.FlowAiRole
import com.jackson4rocks.evexia.ui.FlowAiViewModel
import com.jackson4rocks.evexia.util.FlowAiAvailability
import com.jackson4rocks.evexia.ui.components.ConfirmDialog
import com.jackson4rocks.evexia.ui.components.DividerLine
import com.jackson4rocks.evexia.ui.components.EmptyState
import com.jackson4rocks.evexia.ui.components.FileGlyph
import com.jackson4rocks.evexia.ui.components.GlassSheet
import com.jackson4rocks.evexia.ui.components.GlassSurface
import com.jackson4rocks.evexia.ui.components.Icon
import com.jackson4rocks.evexia.ui.components.IconButton
import com.jackson4rocks.evexia.ui.components.PagePadding
import com.jackson4rocks.evexia.ui.components.PrimaryButton
import com.jackson4rocks.evexia.ui.components.ScreenTitle
import com.jackson4rocks.evexia.ui.components.SecondaryButton
import com.jackson4rocks.evexia.ui.components.Spinner
import com.jackson4rocks.evexia.ui.components.pressable
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.ui.theme.LocalReduceMotion
import com.jackson4rocks.evexia.util.FileStore
import com.jackson4rocks.evexia.util.FlowAiDocuments

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowAiScreen(state: AppUiState) {
    val colors = DF.colors
    val type = DF.type
    val vm: FlowAiViewModel = viewModel()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val aiAvailability by vm.aiAvailability.collectAsStateWithLifecycle()
    val aiSetupMessage by vm.aiSetupMessage.collectAsStateWithLifecycle()
    val aiReady = aiAvailability == FlowAiAvailability.READY
    val history by vm.history.collectAsStateWithLifecycle()
    val currentConversationId by vm.currentConversationId.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf("") }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var confirmClearHistory by rememberSaveable { mutableStateOf(false) }
    var showDocumentPicker by rememberSaveable { mutableStateOf(false) }
    var showPlusMenu by rememberSaveable { mutableStateOf(false) }
    var thinkMore by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val imeVisible = WindowInsets.isImeVisible
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val reduceMotion = LocalReduceMotion.current
    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            vm.summariseUri(uri, thinkMore = thinkMore)
        }
    }

    LaunchedEffect(currentConversationId, messages.size, busy) {
        val count = messages.size + if (busy) 1 else 0
        if (count > 0) {
            if (reduceMotion) {
                listState.scrollToItem(count - 1)
            } else {
                listState.animateScrollToItem(count - 1)
            }
        }
    }

    fun submitDraft() {
        val text = draft.trim()
        if (text.isBlank() || busy) return
        draft = ""
        showPlusMenu = false
        vm.send(text, thinkMore = thinkMore)
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(bottom = if (imeVisible) 0.dp else 80.dp + navigationBar),
    ) {
        ScreenTitle(
            title = "Evexia AI",
            eyebrow = "Ask, summarise, understand",
            subtitle = when (aiAvailability) {
                FlowAiAvailability.CHECKING -> "Connecting to Puter AI…"
                FlowAiAvailability.SIGN_IN_REQUIRED -> "Sign in to Puter to start chatting."
                FlowAiAvailability.ERROR -> "Couldn’t connect to Puter. Check your connection."
                FlowAiAvailability.READY -> "Online AI by Puter · replies run in the cloud."
            },
            trailing = {
                IconButton(
                    icon = Icons.Outlined.History,
                    contentDescription = "Chat history",
                    onClick = { showHistory = true },
                    size = 38.dp,
                    glass = false,
                )
                if (messages.isNotEmpty()) {
                    IconButton(
                        icon = Icons.Outlined.AddComment,
                        contentDescription = "New chat",
                        onClick = {
                            showPlusMenu = false
                            vm.newChat()
                        },
                        size = 38.dp,
                        glass = false,
                        enabled = !busy,
                    )
                }
                IconButton(
                    icon = Icons.Outlined.Info,
                    contentDescription = "About Evexia AI",
                    onClick = { showInfo = true },
                    size = 38.dp,
                    glass = false,
                )
            },
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (!aiReady) {
                PuterSignInCard(
                    availability = aiAvailability,
                    setupMessage = aiSetupMessage,
                    onSignIn = vm::signInToPuter,
                    onRetry = vm::retryPuterConnection,
                )
            }
            if (messages.isEmpty()) {
                EmptyConversation(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    summariseEnabled = aiReady,
                    onSummarise = {
                        showPlusMenu = false
                        showDocumentPicker = true
                    },
                    onSuggestion = { draft = it },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = PagePadding,
                        end = PagePadding,
                        top = 8.dp,
                        bottom = 14.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                    if (busy) {
                        item(key = "flow-ai-typing") {
                            TypingBubble(status = status ?: "Thinking…")
                        }
                    }
                }
            }

            if (messages.isNotEmpty()) {
                SummariseBar(
                    enabled = !busy && aiReady,
                    onClick = {
                        showPlusMenu = false
                        showDocumentPicker = true
                    },
                )
                Spacer(Modifier.height(8.dp))
            }

            if (showPlusMenu) {
                PlusMenu(
                    thinkMore = thinkMore,
                    onAddFiles = {
                        showPlusMenu = false
                        attachmentPicker.launch(
                            arrayOf("image/*", "application/pdf", "text/*"),
                        )
                    },
                    onToggleThinkMore = {
                        thinkMore = !thinkMore
                        showPlusMenu = false
                    },
                )
                Spacer(Modifier.height(8.dp))
            }

            if (thinkMore) {
                ThinkMoreBadge(onDismiss = { thinkMore = false })
                Spacer(Modifier.height(6.dp))
            }

            Composer(
                draft = draft,
                onDraftChange = { draft = it },
                busy = busy,
                enabled = aiReady,
                onSubmit = ::submitDraft,
                onPlus = {
                    if (!busy) {
                        showPlusMenu = !showPlusMenu
                    }
                },
                plusExpanded = showPlusMenu,
            )
            BasicText(
                text = "Evexia AI can make mistakes. Check important details.",
                style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PagePadding, vertical = 8.dp),
            )
        }
    }

    if (showDocumentPicker) {
        DocumentPickerSheet(
            state = state,
            onDismiss = { showDocumentPicker = false },
            onNote = { note ->
                showDocumentPicker = false
                vm.summariseNote(note, thinkMore = thinkMore)
            },
            onItem = { item ->
                showDocumentPicker = false
                vm.summariseLibraryItem(item, thinkMore = thinkMore)
            },
        )
    }

    if (showHistory) {
        ChatHistorySheet(
            history = history,
            currentConversationId = currentConversationId,
            onOpen = { id ->
                showHistory = false
                vm.openConversation(id)
            },
            onDelete = vm::deleteConversation,
            onClearAll = {
                showHistory = false
                confirmClearHistory = true
            },
            onDismiss = { showHistory = false },
        )
    }

    if (confirmClearHistory) {
        ConfirmDialog(
            title = "Clear chat history?",
            message = "This removes every saved Evexia AI conversation on this device. " +
                "It can't be undone.",
            confirmLabel = "Clear all",
            onConfirm = {
                confirmClearHistory = false
                vm.clearHistory()
            },
            onDismiss = { confirmClearHistory = false },
        )
    }

    if (showInfo) {
        GlassSheet(
            title = "About Evexia AI",
            subtitle = "An online study assistant powered by Puter AI.",
            onDismiss = { showInfo = false },
        ) {
            PrivacyPoint(
                title = "Online AI and your data",
                body = "Questions, chat context and extracted document text are sent to Puter and its AI provider to generate replies. Avoid sending confidential information. Chat history stays in this app.",
            )
            Spacer(Modifier.height(12.dp))
            val availabilityText = when (aiAvailability) {
                FlowAiAvailability.CHECKING -> "Connecting to Puter…"
                FlowAiAvailability.SIGN_IN_REQUIRED -> "Sign in with a Puter account to use online AI."
                FlowAiAvailability.ERROR -> "Puter could not be reached. Check your connection."
                FlowAiAvailability.READY -> "Puter is connected. AI requests need an internet connection."
            }
            BasicText(
                text = availabilityText,
                style = type.cardTitle.copy(color = colors.textPrimary),
                modifier = Modifier.padding(horizontal = 3.dp),
            )
            aiSetupMessage?.let { message ->
                BasicText(
                    text = message,
                    style = type.caption.copy(color = colors.textTertiary),
                    modifier = Modifier.padding(top = 8.dp, start = 3.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            PrivacyPoint(
                title = "Who made it",
                body = "Evexia AI was created by Leon Sony (github.com/Jackson4Rocks), with Puter AI as the base for mostly everything.",
            )
            Spacer(Modifier.height(14.dp))
            PrivacyPoint(
                title = "Puter sign-in and usage",
                body = "Puter.js does not require an API key. Sign in with your Puter account; its user-pays model applies your free allowance and may prompt you to upgrade when it runs out. Internet access is required.",
            )
            Spacer(Modifier.height(14.dp))
            PrivacyPoint(
                title = "Models and answer speed",
                body = "Quick answers use Puter's Gemini 3.1 Flash Lite model for faster replies. Think more uses GPT-5.6 Luna for slower, more detailed answers. Model availability and usage are controlled by Puter.",
            )
            Spacer(Modifier.height(14.dp))
            PrivacyPoint(
                title = "Your chat history",
                body = "Conversations are saved on this phone so you can reopen them. " +
                    "Photos and files are read from a temporary cache copy, then deleted, and are " +
                    "not kept in history. Clear the chat anytime from the History button.",
            )
            Spacer(Modifier.height(14.dp))
            PrivacyPoint(
                title = "Keep it sensible",
                body = "AI can make mistakes. Check important answers, and avoid sending " +
                    "confidential documents.",
            )
            Spacer(Modifier.height(14.dp))
            if (aiAvailability == FlowAiAvailability.READY) {
                PrimaryButton(
                    label = "Sign out of Puter",
                    onClick = vm::signOutOfPuter,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (aiAvailability == FlowAiAvailability.SIGN_IN_REQUIRED) {
                PrimaryButton(
                    label = "Sign in to Puter",
                    onClick = vm::signInToPuter,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (aiAvailability == FlowAiAvailability.ERROR) {
                PrimaryButton(
                    label = "Retry connection",
                    onClick = vm::retryPuterConnection,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PuterSignInCard(
    availability: FlowAiAvailability,
    setupMessage: String?,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding, vertical = 8.dp),
        strong = true,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column {
            BasicText(
                text = when (availability) {
                    FlowAiAvailability.CHECKING -> "Connecting to Puter AI"
                    FlowAiAvailability.SIGN_IN_REQUIRED -> "Sign in to use online AI"
                    FlowAiAvailability.ERROR -> "Couldn't connect to Puter AI"
                    FlowAiAvailability.READY -> "Puter AI is ready"
                },
                style = type.cardTitle.copy(color = colors.textPrimary),
            )
            BasicText(
                text = "Evexia uses Puter AI in the cloud. No model download or API key is needed; internet access and a Puter account are required. Puter may ask you to upgrade when your free usage runs out.",
                style = type.caption.copy(color = colors.textSecondary, lineHeight = 17.sp),
                modifier = Modifier.padding(top = 5.dp, bottom = 11.dp),
            )
            when (availability) {
                FlowAiAvailability.CHECKING -> Spinner(size = 18.dp)
                FlowAiAvailability.SIGN_IN_REQUIRED -> {
                    PrimaryButton(
                        label = "Sign in to Puter",
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.AutoAwesome,
                    )
                }
                FlowAiAvailability.ERROR -> {
                    PrimaryButton(
                        label = "Retry connection",
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.AutoAwesome,
                    )
                }
                FlowAiAvailability.READY -> Unit
            }
            setupMessage?.let { message ->
                BasicText(
                    text = message,
                    style = type.caption.copy(color = colors.textTertiary),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatHistorySheet(
    history: List<FlowAiConversationSummary>,
    currentConversationId: String,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type

    GlassSheet(
        title = "Chat history",
        subtitle = "Saved on this device. Tap a chat to reopen it.",
        onDismiss = onDismiss,
    ) {
        if (history.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.History,
                title = "No saved chats yet",
                message = "Send Evexia AI a message and the conversation will appear here.",
            )
        } else {
            history.forEachIndexed { index, conversation ->
                ChatHistoryRow(
                    conversation = conversation,
                    isCurrent = conversation.id == currentConversationId,
                    onOpen = { onOpen(conversation.id) },
                    onDelete = { onDelete(conversation.id) },
                )
                if (index < history.lastIndex) {
                    DividerLine(Modifier.padding(start = 45.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            SecondaryButton(
                label = "Clear all chat history",
                icon = Icons.Outlined.DeleteOutline,
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth(),
                danger = true,
            )
            BasicText(
                text = "Only the chat text is saved, not your attached files.",
                style = type.caption.copy(color = colors.textTertiary, lineHeight = 16.sp),
                modifier = Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp),
            )
        }
    }
}

@Composable
private fun ChatHistoryRow(
    conversation: FlowAiConversationSummary,
    isCurrent: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val shape = RoundedCornerShape(18.dp)
    val background = if (isCurrent) colors.accentSoft else colors.glassQuiet
    val border = if (isCurrent) {
        colors.accent.copy(alpha = 0.24f)
    } else {
        colors.border
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .pressable(onClick = onOpen)
            .padding(start = 13.dp, top = 10.dp, bottom = 10.dp, end = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    if (isCurrent) {
                        colors.accent.copy(alpha = 0.18f)
                    } else {
                        colors.glassStrong
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.History,
                contentDescription = null,
                tint = if (isCurrent) colors.accentInk else colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = conversation.title,
                    style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(colors.accent.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        BasicText(
                            text = "CURRENT",
                            style = type.label.copy(color = colors.accentInk, fontSize = 7.sp),
                        )
                    }
                    Spacer(Modifier.width(3.dp))
                }
            }
            BasicText(
                text = "${messageCountLabel(conversation.messageCount)} · " +
                    historyTimeLabel(conversation.updatedAt),
                style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        IconButton(
            icon = Icons.Outlined.DeleteOutline,
            contentDescription = "Delete chat",
            onClick = onDelete,
            size = 34.dp,
            glass = false,
            tint = colors.textTertiary,
        )
    }
}

private fun messageCountLabel(count: Int): String =
    if (count == 1) "1 message" else "$count messages"

private fun historyTimeLabel(timestamp: Long): String {
    if (timestamp <= 0L) return "Recently"
    return android.text.format.DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        android.text.format.DateUtils.MINUTE_IN_MILLIS,
        android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

@Composable
private fun PrivacyPoint(title: String, body: String) {
    val colors = DF.colors
    val type = DF.type
    Row(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(27.dp)
                .clip(CircleShape)
                .background(colors.glassQuiet),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(13.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = title,
                style = type.caption.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                ),
            )
            BasicText(
                text = body,
                style = type.caption.copy(color = colors.textTertiary, lineHeight = 17.sp),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun EmptyConversation(
    modifier: Modifier = Modifier,
    summariseEnabled: Boolean,
    onSummarise: () -> Unit,
    onSuggestion: (String) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val suggestions = listOf(
        "Explain a concept" to "Explain this concept in simple terms, with an example: ",
        "Make a revision plan" to "Make me a short revision plan for this week. ",
        "Practise with questions" to "Give me five practice questions about ",
    )

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PagePadding)
            .padding(top = 14.dp, bottom = 12.dp),
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(colors.accentSoft)
                .border(1.dp, colors.accent.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(25.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        BasicText(
            text = "Hey, I'm Evexia AI.",
            style = type.serif.copy(color = colors.textPrimary, fontSize = 30.sp, lineHeight = 33.sp),
        )
        Spacer(Modifier.height(7.dp))
        BasicText(
            text = "Ask about your studies, or hand me a note or PDF and I'll pull out what matters.",
            style = type.body.copy(color = colors.textSecondary, lineHeight = 21.sp),
        )
        Spacer(Modifier.height(18.dp))
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (summariseEnabled) 1f else 0.55f),
            shape = RoundedCornerShape(20.dp),
            accentTint = true,
            contentPadding = PaddingValues(15.dp),
            onClick = if (summariseEnabled) onSummarise else null,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = colors.accentInk,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "Summarise a note or PDF",
                        style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 14.sp),
                    )
                    BasicText(
                        text = "Notes, text PDFs, scans and images",
                        style = type.caption.copy(color = colors.textSecondary, fontSize = 10.5.sp),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        BasicText(
            text = "TRY ASKING",
            style = type.label.copy(color = colors.textTertiary),
        )
        Spacer(Modifier.height(7.dp))
        suggestions.forEach { (label, prompt) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .pressable { onSuggestion(prompt) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = label,
                    style = type.caption.copy(color = colors.textPrimary),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: FlowAiMessage) {
    val colors = DF.colors
    val type = DF.type
    val isUser = message.role == FlowAiRole.USER
    val background = when {
        isUser -> colors.accent
        message.error -> colors.dangerSoft
        else -> colors.glassStrong
    }
    val foreground = if (isUser) colors.onAccent else if (message.error) colors.danger else colors.textPrimary
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 3.dp, bottom = 5.dp),
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = if (message.error) colors.danger else colors.accentInk,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(5.dp))
                BasicText(
                    text = if (message.error) "EVEXIA AI · NOTICE" else "EVEXIA AI",
                    style = type.label.copy(
                        color = if (message.error) colors.danger else colors.textTertiary,
                        fontSize = 8.sp,
                    ),
                )
            }
        }
        Box(
            Modifier
                .widthIn(max = 520.dp)
                .clip(shape)
                .background(background)
                .border(
                    width = if (isUser) 0.dp else 1.dp,
                    color = when {
                        isUser -> Color.Transparent
                        message.error -> colors.danger.copy(alpha = 0.28f)
                        else -> colors.border
                    },
                    shape = shape,
                )
                .padding(horizontal = 15.dp, vertical = 12.dp),
        ) {
            BasicText(
                text = if (isUser) message.text else cleanAnswer(message.text),
                style = type.body.copy(
                    color = foreground,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                ),
            )
        }
    }
}

@Composable
private fun TypingBubble(status: String) {
    val colors = DF.colors
    val type = DF.type
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                .background(colors.glassStrong)
                .border(1.dp, colors.border, RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spinner(size = 15.dp, color = colors.accentInk)
            Spacer(Modifier.width(9.dp))
            BasicText(
                text = status,
                style = type.caption.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SummariseBar(enabled: Boolean, onClick: () -> Unit) {
    val colors = DF.colors
    val type = DF.type
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding)
            .alpha(if (enabled) 1f else 0.55f),
        shape = RoundedCornerShape(17.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
        onClick = if (enabled) onClick else null,
        elevated = false,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.AttachFile,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(9.dp))
            BasicText(
                text = "Summarise a note or PDF",
                style = type.caption.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.weight(1f),
            )
            BasicText(
                text = "Attach",
                style = type.label.copy(color = colors.accentInk, fontSize = 8.sp),
            )
        }
    }
}

@Composable
private fun PlusMenu(
    thinkMore: Boolean,
    onAddFiles: () -> Unit,
    onToggleThinkMore: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding),
        shape = RoundedCornerShape(20.dp),
        strong = true,
        contentPadding = PaddingValues(5.dp),
    ) {
        Column {
            PlusMenuRow(
                icon = Icons.Outlined.AttachFile,
                title = "Add Photos or Files",
                subtitle = "Summarise a photo, PDF or text file",
                onClick = onAddFiles,
            )
            DividerLine(Modifier.padding(start = 52.dp, end = 10.dp))
            PlusMenuRow(
                icon = Icons.Outlined.AutoAwesome,
                title = "Think more",
                subtitle = if (thinkMore) {
                    "On — slower, fuller answers with more context"
                } else {
                    "Slower, fuller answers with more context"
                },
                selected = thinkMore,
                onClick = onToggleThinkMore,
            )
        }
    }
}

@Composable
private fun PlusMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val colors = DF.colors
    val type = DF.type
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) colors.accentSoft else Color.Transparent)
            .pressable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (selected) colors.accent.copy(alpha = 0.18f) else colors.glassQuiet),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) colors.accentInk else colors.textSecondary,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = title,
                style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 13.5.sp),
            )
            BasicText(
                text = subtitle,
                style = type.caption.copy(color = colors.textTertiary, fontSize = 10.5.sp),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Think more is on",
                tint = colors.accentInk,
                modifier = Modifier.size(17.dp),
            )
        } else {
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ThinkMoreBadge(onDismiss: () -> Unit) {
    val colors = DF.colors
    val type = DF.type
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding),
        horizontalArrangement = Arrangement.End,
    ) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(colors.accentSoft)
                .border(1.dp, colors.accent.copy(alpha = 0.28f), CircleShape)
                .pressable(onClick = onDismiss)
                .padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(5.dp))
            BasicText(
                text = "Think more on",
                style = type.label.copy(
                    color = colors.accentInk,
                    fontSize = 8.sp,
                    letterSpacing = 0.4.sp,
                ),
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Filled.Close,
                contentDescription = "Turn off Think more",
                tint = colors.accentInk,
                modifier = Modifier.size(11.dp),
            )
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    busy: Boolean,
    enabled: Boolean,
    onSubmit: () -> Unit,
    onPlus: () -> Unit,
    plusExpanded: Boolean,
) {
    val colors = DF.colors
    val type = DF.type
    val canSend = draft.isNotBlank() && !busy && enabled
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(23.dp))
                .background(colors.glassStrong)
                .border(1.dp, colors.border, RoundedCornerShape(23.dp))
                .padding(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            IconButton(
                icon = if (plusExpanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (plusExpanded) {
                    "Close attachment options"
                } else {
                    "Add photos, files, or options"
                },
                onClick = onPlus,
                size = 38.dp,
                glass = false,
                enabled = !busy && enabled,
                tint = if (plusExpanded) colors.accentInk else null,
            )
            Box(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 9.dp),
            ) {
                if (draft.isBlank()) {
                    BasicText(
                        text = if (enabled) "Ask Evexia AI anything…" else "Sign in to continue…",
                        style = type.body.copy(color = colors.textTertiary, fontSize = 14.sp),
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = type.body.copy(color = colors.textPrimary, fontSize = 14.sp),
                    maxLines = 4,
                    cursorBrush = SolidColor(colors.accentInk),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { if (canSend) onSubmit() },
                    ),
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canSend) colors.accent else colors.glassQuiet)
                    .pressable(enabled = canSend, onClick = onSubmit),
                contentAlignment = Alignment.Center,
            ) {
                if (busy) {
                    Spinner(size = 15.dp, color = colors.textTertiary)
                } else {
                    Icon(
                        Icons.Filled.ArrowUpward,
                        contentDescription = "Send",
                        tint = if (canSend) colors.onAccent else colors.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentPickerSheet(
    state: AppUiState,
    onDismiss: () -> Unit,
    onNote: (NoteEntity) -> Unit,
    onItem: (LibraryItemEntity) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val notes = state.notes.sortedByDescending { it.updatedAt }
    val files = state.library
        .filter { it.kind == LibraryItemEntity.KIND_FILE && FlowAiDocuments.supports(it) }
        .sortedByDescending { it.addedAt }

    GlassSheet(
        title = "Summarise something",
        subtitle = "Notes, text PDFs, scans and images. Long sources are trimmed.",
        onDismiss = onDismiss,
    ) {
        if (notes.isEmpty() && files.isEmpty()) {
            Column(Modifier.padding(vertical = 18.dp)) {
                BasicText(
                    text = "Nothing to summarise yet",
                    style = type.cardTitle.copy(color = colors.textPrimary),
                )
                BasicText(
                    text = "Write a note or add a PDF to your library, then come back.",
                    style = type.bodySmall.copy(color = colors.textSecondary),
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }

        if (notes.isNotEmpty()) {
            PickerLabel("NOTES")
            notes.forEachIndexed { index, note ->
                PickerRow(
                    title = note.title.ifBlank { "Untitled note" },
                    subtitle = listOfNotNull(
                        "Note",
                        state.subjectName(note.subjectId),
                    ).joinToString(" · "),
                    icon = Icons.Outlined.NoteAlt,
                    tint = colors.accentInk,
                    onClick = { onNote(note) },
                )
                if (index < notes.lastIndex) DividerLine(Modifier.padding(start = 42.dp))
            }
        }

        if (files.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            PickerLabel("LIBRARY")
            files.forEachIndexed { index, item ->
                PickerRow(
                    title = item.title,
                    subtitle = listOfNotNull(
                        FlowAiDocuments.kindLabel(item),
                        if (item.sizeBytes > 0) FileStore.humanSize(item.sizeBytes) else null,
                        state.subjectName(item.subjectId),
                    ).joinToString(" · "),
                    fileMime = item.mimeType,
                    onClick = { onItem(item) },
                )
                if (index < files.lastIndex) DividerLine(Modifier.padding(start = 42.dp))
            }
        }
    }
}

@Composable
private fun PickerLabel(text: String) {
    val colors = DF.colors
    val type = DF.type
    BasicText(
        text = text,
        style = type.label.copy(color = colors.textTertiary),
        modifier = Modifier.padding(top = 6.dp, bottom = 5.dp),
    )
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    tint: Color? = null,
    fileMime: String? = null,
) {
    val colors = DF.colors
    val type = DF.type
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .pressable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (fileMime != null) {
            FileGlyph(
                mimeType = fileMime,
                isBookmark = false,
                modifier = Modifier.size(29.dp),
                tint = tint,
            )
        } else {
            Box(
                Modifier
                    .size(29.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background((tint ?: colors.textSecondary).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = tint ?: colors.textSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = title,
                style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = subtitle,
                style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            Icons.Filled.ArrowForward,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(14.dp),
        )
    }
}

private fun cleanAnswer(raw: String): String = raw
    .replace(Regex("```[\\w-]*\\n?"), "")
    .replace("```", "")
    .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    .replace(Regex("__(.+?)__"), "$1")
    .replace(Regex("`([^`]+)`"), "$1")
    .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
    .replace(Regex("(?m)^#{1,6}\\s*"), "")
    .replace(Regex("(?m)^\\s*[-*]\\s+"), "• ")
    .trim()
