package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.anim.auraSpringPress
import com.example.ui.anim.ShimmerDashboardGrid
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import android.net.Uri
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun MainAppContainer(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.currentSection.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Trigger initial PIN security lock assessment
    LaunchedEffect(Unit) {
        viewModel.checkSecurityLock()
    }

    Box(modifier = modifier.fillMaxSize().background(AuraObsidian)) {
        if (!isAppUnlocked) {
            // High-contrast Glassmorphic security lock screen
            SecurityPinKeypadGate(viewModel = viewModel)
        } else {
            // Main Scaffold with bottom bar routing
            var editNoteItem by remember { mutableStateOf<Note?>(null) }
            var editTaskItem by remember { mutableStateOf<Task?>(null) }
            
            var showAudioRecordDialog by remember { mutableStateOf(false) }
            var showImageCaptureDialog by remember { mutableStateOf(false) }
            var showCustomIconDialog by remember { mutableStateOf(false) }
            var showGlobalQuickTransactionType by remember { mutableStateOf<String?>(null) }
            
            var showOverlayMenu by remember { mutableStateOf(false) }
            val captureColorId by viewModel.captureButtonColor.collectAsState()
            val captureAnimId by viewModel.captureButtonAnimationType.collectAsState()

            val accounts by viewModel.allAccounts.collectAsState()
            val friends by viewModel.allFriends.collectAsState()
            
            // Sub workspace triggers
            var workspaceDrawingData by remember { mutableStateOf<String?>(null) }
            var isDrawingWorkspaceOpen by remember { mutableStateOf(false) }

            var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
            val context = LocalContext.current

            val iconPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri ->
                    if (uri != null) {
                        viewModel.setQuickCaptureIconUri(uri.toString())
                    }
                }
            )

            val mediaPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri ->
                    if (uri != null) {
                        selectedMediaUri = uri
                    }
                }
            )

            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview(),
                onResult = { bitmap ->
                    if (bitmap != null) {
                        try {
                            val file = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.png")
                            file.outputStream().use { outStream ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                            }
                            selectedMediaUri = Uri.fromFile(file)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )

            // Active internal navigation switches
            val contentModifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (activeTab != Section.RichNoteEditor && activeTab != Section.DrawingWorkspace && !isDrawingWorkspaceOpen) {
                        AuraBottomNavRow(viewModel = viewModel, active = activeTab)
                    }
                },
                floatingActionButton = {
                    if (activeTab != Section.RichNoteEditor && activeTab != Section.DrawingWorkspace && !isDrawingWorkspaceOpen) {
                        val quickCaptureIconUri by viewModel.quickCaptureIconUri.collectAsState()
                        val haptic = LocalHapticFeedback.current

                        val infiniteTransition = rememberInfiniteTransition()

                        // Pulse Outer Ring Animation for PULSE mode
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1400, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 0.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1400, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )

                        // Breathe Animation for BREATHE mode
                        val breatheScale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = if (captureAnimId == "BREATHE") 1.12f else 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        // Spin Rotation for SPIN mode
                        val spinRotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = if (captureAnimId == "SPIN") 360f else 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(3500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )

                        val buttonBrush = when (captureColorId) {
                            "CYAN" -> Brush.radialGradient(listOf(AuraCyanNeon, Color(0xFF00B0FF)))
                            "PURPLE" -> Brush.radialGradient(listOf(AuraPurpleAccent, Color(0xFFD500F9)))
                            "EMERALD" -> Brush.radialGradient(listOf(Color(0xFF00FF87), Color(0xFF00C853)))
                            "PINK" -> Brush.radialGradient(listOf(Color(0xFFFF2A85), Color(0xFFFF1744)))
                            "RED" -> Brush.radialGradient(listOf(Color(0xFFFF3E3E), Color(0xFFD50000)))
                            "AMBER" -> Brush.radialGradient(listOf(Color(0xFFFFC107), Color(0xFFFF6D00)))
                            else -> Brush.radialGradient(listOf(AuraCyanNeon, AuraPurpleAccent))
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            // Rich & Clean Frosted Card instead of messy floating text blocks
                            AnimatedVisibility(
                                visible = showOverlayMenu,
                                enter = scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f), initialScale = 0.7f) + fadeIn(),
                                exit = scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f), targetScale = 0.7f) + fadeOut()
                            ) {
                                Card(
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase.copy(alpha = 0.96f)),
                                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(AuraCyanNeon.copy(alpha = 0.4f), AuraPurpleAccent.copy(alpha = 0.4f)))),
                                    modifier = Modifier
                                        .width(180.dp)
                                        .padding(bottom = 8.dp)
                                        .shadow(8.dp, RoundedCornerShape(22.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // 1. Write Note
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    showOverlayMenu = false
                                                    viewModel.selectNote(null)
                                                    viewModel.navigateTo(Section.RichNoteEditor)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .background(AuraSlateCard, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = AuraCyanNeon, modifier = Modifier.size(16.dp))
                                            }
                                            Text("Quick Note", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // 2. AudioMemo
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    showOverlayMenu = false
                                                    showAudioRecordDialog = true
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .background(AuraSlateCard, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Mic, contentDescription = null, tint = AuraPurpleAccent, modifier = Modifier.size(16.dp))
                                            }
                                            Text("Voice Memo", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // 3. Take Image
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    showOverlayMenu = false
                                                    showImageCaptureDialog = true
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .background(AuraSlateCard, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF00FF87), modifier = Modifier.size(16.dp))
                                            }
                                            Text("Snap Image", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // 4. Add Payment
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    showOverlayMenu = false
                                                    showGlobalQuickTransactionType = "RECEIVED"
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .background(AuraSlateCard, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.CallReceived, contentDescription = null, tint = MoodHappy, modifier = Modifier.size(16.dp))
                                            }
                                            Text("Received ₹", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // 5. Deduct Payment
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    showOverlayMenu = false
                                                    showGlobalQuickTransactionType = "SENT"
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .background(AuraSlateCard, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = Color(0xFFFF3E3E), modifier = Modifier.size(16.dp))
                                            }
                                            Text("Deducted ₹", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // 6. Customize Icon
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    showOverlayMenu = false
                                                    showCustomIconDialog = true
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .background(AuraSlateCard, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AuraCopperWarm, modifier = Modifier.size(16.dp))
                                            }
                                            Text("Aesthetics", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Dynamic interactive button
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .scale(breatheScale),
                                contentAlignment = Alignment.Center
                            ) {
                                // Pulsing halo ring animation behind button if enabled
                                if (captureAnimId == "PULSE") {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .scale(pulseScale)
                                            .background(buttonBrush, CircleShape)
                                            .graphicsLayer { alpha = pulseAlpha }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(buttonBrush)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showOverlayMenu = !showOverlayMenu
                                        }
                                        .border(2.dp, Color.White, CircleShape)
                                        .testTag("global_fab"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (quickCaptureIconUri.isNotEmpty()) {
                                        androidx.compose.foundation.Image(
                                            painter = coil.compose.rememberAsyncImagePainter(
                                                 model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                     .data(quickCaptureIconUri)
                                                     .size(coil.size.Size(120, 120))
                                                     .crossfade(true)
                                                     .memoryCacheKey("quick_capture_fab")
                                                     .build()
                                             ),
                                            contentDescription = "Custom Quick Capture Graphic",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = "Default Quick Capture Mode",
                                            tint = Color.Black,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .graphicsLayer(rotationZ = spinRotation)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                containerColor = AuraObsidian
            ) { innerPadding ->
                val isOnline by viewModel.isOnline.collectAsState()
                val pendingOpsCount by viewModel.pendingOpsCount.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(bottom = if (activeTab != Section.RichNoteEditor && activeTab != Section.DrawingWorkspace) 76.dp else 0.dp)
                ) {
                    // Non-intrusive offline status banner
                    AnimatedVisibility(
                        visible = !isOnline,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter).zIndex(10f)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Offline",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Offline mode active",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                if (pendingOpsCount > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) { Text("$pendingOpsCount pending sync") }
                                }
                            }
                        }
                    }

                    // Elegant semi-transparent backdrop overlay when Quick Capture menu is active
                    AnimatedVisibility(
                        visible = showOverlayMenu,
                        enter = fadeIn(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(250))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable(
                                    onClick = { showOverlayMenu = false },
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                )
                        )
                    }

                    if (isDrawingWorkspaceOpen) {
                        DrawingCanvas(
                            initialData = workspaceDrawingData,
                            onSaveDrawing = { savedStr ->
                                workspaceDrawingData = savedStr
                                coroutineScope.launch {
                                    // Save drawing directly back to the active note if currently editing
                                    val currentEditingNote = viewModel.selectedNote.value
                                    if (currentEditingNote != null) {
                                        viewModel.saveDraftNote(
                                            title = currentEditingNote.title,
                                            content = currentEditingNote.content,
                                            category = currentEditingNote.category,
                                            tags = currentEditingNote.tags,
                                            voicePath = currentEditingNote.voicePath,
                                            drawingData = savedStr,
                                            isBookmarked = currentEditingNote.isBookmarked
                                        )
                                    }
                                }
                                isDrawingWorkspaceOpen = false
                            },
                            onBack = { isDrawingWorkspaceOpen = false },
                            viewModel = viewModel
                        )
                    } else {
                        val mainTabs = remember {
                            listOf(
                                Section.Dashboard,
                                Section.Notes,
                                Section.Tasks,
                                Section.Habits,
                                Section.Day,
                                Section.Money
                            )
                        }
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                            initialPage = 0,
                            pageCount = { mainTabs.size }
                        )

                        // Keep pager in sync with programmatic state updates
                        val targetPage = mainTabs.indexOf(activeTab)
                        LaunchedEffect(targetPage) {
                            if (targetPage != -1 && pagerState.currentPage != targetPage) {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }

                        // Sync swiping back to ViewModel state
                        LaunchedEffect(pagerState.currentPage) {
                            val currentSwipeSection = mainTabs[pagerState.currentPage]
                            if (viewModel.currentSection.value != currentSwipeSection && targetPage != -1) {
                                viewModel.navigateTo(currentSwipeSection)
                            }
                        }

                        // Trigger tactile tick when a page change successfully commits/snaps
                        val view = androidx.compose.ui.platform.LocalView.current
                        LaunchedEffect(pagerState.settledPage) {
                            com.example.ui.AuraHaptics.triggerSubtleTick(view)
                        }

                        if (activeTab !in mainTabs) {
                            when (activeTab) {
                                Section.RichNoteEditor -> {
                                    val selectedNoteVal by viewModel.selectedNote.collectAsState()
                                    NoteEditorScreen(
                                        note = selectedNoteVal,
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateTo(Section.Notes) },
                                        onOpenDrawingBoard = { data ->
                                            workspaceDrawingData = data
                                            isDrawingWorkspaceOpen = true
                                        }
                                    )
                                }
                                Section.DrawingWorkspace -> {
                                    val selectedTaskVal by viewModel.selectedEditTask.collectAsState()
                                    TaskComposerScreen(
                                        task = selectedTaskVal,
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateTo(Section.Tasks) }
                                    )
                                }
                                Section.SecuritySettings -> {
                                    AppSecuritySettingsScreen(viewModel = viewModel)
                                }
                                else -> { /* Safety fallback */ }
                            }
                        } else {
                            androidx.compose.foundation.pager.HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = true
                            ) { pageIndex ->
                                when (mainTabs[pageIndex]) {
                                    Section.Dashboard -> {
                                        DashboardScreen(
                                            viewModel = viewModel,
                                            onOpenNote = { note ->
                                                viewModel.selectNote(note)
                                                viewModel.navigateTo(Section.RichNoteEditor)
                                            },
                                            onOpenTask = { task ->
                                                viewModel.selectEditTask(task)
                                                viewModel.navigateTo(Section.DrawingWorkspace)
                                            }
                                        )
                                    }
                                    Section.Notes -> {
                                        NotesScreen(
                                            viewModel = viewModel,
                                            onOpenNoteEditor = { note ->
                                                viewModel.selectNote(note)
                                                viewModel.navigateTo(Section.RichNoteEditor)
                                            },
                                            onOpenDrawingWorkspace = { data ->
                                                workspaceDrawingData = data
                                                isDrawingWorkspaceOpen = true
                                            }
                                        )
                                    }
                                    Section.Tasks -> {
                                        TasksScreen(
                                            viewModel = viewModel,
                                            onOpenTaskComposer = { task ->
                                                viewModel.selectEditTask(task)
                                                viewModel.navigateTo(Section.DrawingWorkspace)
                                            }
                                        )
                                    }
                                    Section.Habits -> {
                                        HabitsTabScreen(viewModel = viewModel)
                                    }
                                    Section.Day -> {
                                        JournalAndCalendarScreen(
                                            viewModel = viewModel,
                                            onOpenDrawingWorkspace = { data ->
                                                workspaceDrawingData = data
                                                isDrawingWorkspaceOpen = true
                                            }
                                        )
                                    }
                                    Section.Money -> {
                                        MoneyTrackerScreen(viewModel = viewModel)
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }

                    // Global floating voice playback controller player
                    val playState by viewModel.playbackState.collectAsState()
                    val playProgress by viewModel.playbackProgress.collectAsState()
                    val headerTitle by viewModel.playbackHeader.collectAsState()

                    if (playState is PlaybackState.Playing || playState is PlaybackState.Paused) {
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            GlobalMiniFloatingPlayer(
                                header = headerTitle,
                                isPlaying = playState is PlaybackState.Playing,
                                progress = playProgress,
                                onTogglePlayPause = {
                                    if (playState is PlaybackState.Playing) viewModel.pauseVoiceNote()
                                    else viewModel.resumeVoiceNote()
                                },
                                onSeek = { viewModel.seekVoiceNote(it) },
                                onClose = { viewModel.stopVoiceNote() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Slide up premium Rounded Bottom Sheet for Info Architecture
                    val infoTitle by viewModel.infoSheetTitle.collectAsState()
                    val infoContent by viewModel.infoSheetContent.collectAsState()

                    AnimatedVisibility(
                        visible = infoTitle != null,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(350, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                        ) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { viewModel.dismissInfoSheet() },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = false) {} // Disable click propagation to dismiss
                                    .padding(16.dp)
                                    .shadow(16.dp, RoundedCornerShape(28.dp))
                                    .border(1.dp, AuraSlateLight, RoundedCornerShape(28.dp)),
                                colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Handle drag indicator bar
                                    Box(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(AuraSlateLight)
                                    )

                                    Text(
                                        text = infoTitle ?: "About this Section",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = infoContent ?: "",
                                        color = AuraWhiteMedium,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = { viewModel.dismissInfoSheet() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AuraSlateLight),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Text(
                                            "UNDERSTOOD",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- AUDIO RECORDING DIALOG BEGIN ---
                if (showAudioRecordDialog) {
                    val recordedDuration by viewModel.audioController.recordedDuration.collectAsState()
                    val isRecordingGlobally by viewModel.audioController.isRecording.collectAsState()

                    // Formatted time: mm:ss
                    val secs = (recordedDuration / 1000) % 60
                    val mins = (recordedDuration / 1000) / 60
                    val timeString = String.format("%02d:%02d", mins, secs)

                    // Wave animations: pulsing neon circles using animatable scale
                    val pulseScale1 = remember { Animatable(1f) }
                    val pulseScale2 = remember { Animatable(1f) }
                    
                    LaunchedEffect(isRecordingGlobally) {
                        if (isRecordingGlobally) {
                            // Animating wave ripples
                            launch {
                                while (true) {
                                    pulseScale1.animateTo(1.5f, animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse))
                                }
                            }
                            launch {
                                while (true) {
                                    pulseScale2.animateTo(1.8f, animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse))
                                }
                            }
                        }
                    }

                    LaunchedEffect(showAudioRecordDialog) {
                        if (showAudioRecordDialog) {
                            viewModel.startAudioNoteRecording()
                        }
                    }

                    AlertDialog(
                        onDismissRequest = {
                            viewModel.stopAudioNoteRecording()
                            showAudioRecordDialog = false
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AUDIO QUICK CAPTURE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .scale(pulseScale2.value)
                                            .background(Color.Red.copy(alpha = 0.15f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(65.dp)
                                            .scale(pulseScale1.value)
                                            .background(Color.Red.copy(alpha = 0.25f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(Color.Red, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.FiberManualRecord,
                                            contentDescription = "Recording mic active",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = timeString,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    "Speak clearly. The audio is automatically saved.",
                                    color = AuraWhiteMuted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.stopAudioNoteRecording()
                                    val voicePathForNote = viewModel.getVoiceNoteFile()
                                    
                                    if (voicePathForNote != null) {
                                        viewModel.saveDraftNote(
                                            title = "Voice Capture Note",
                                            content = "Captured via instant quick recorder on " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                                            category = "Voice Log",
                                            tags = "QuickCapture, Voice",
                                            voicePath = voicePathForNote,
                                            drawingData = null,
                                            photoPath = null
                                        )
                                        viewModel.navigateTo(Section.Notes)
                                    }
                                    showAudioRecordDialog = false
                                }
                            ) {
                                Text("SAVE & STOP", color = AuraCyanNeon, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    viewModel.stopAudioNoteRecording()
                                    showAudioRecordDialog = false
                                }
                            ) {
                                Text("DISCARD & CANCEL", color = Color.White)
                            }
                        },
                        containerColor = AuraCharcoalBase
                    )
                }

                // --- IMAGE/MEDIA CAPTURE DIALOG BEGIN ---
                if (showImageCaptureDialog) {
                    var noteTitleState by remember { mutableStateOf("Captured Media Note") }
                    var noteContentState by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = {
                            showImageCaptureDialog = false
                            selectedMediaUri = null
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = AuraCyanNeon, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CAPTURE MEDIA NOTE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text("Select how you want to capture or import your image/video:", color = AuraWhiteMuted, fontSize = 11.sp)
                                }

                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                try {
                                                    cameraLauncher.launch(null)
                                                } catch (e: Exception) {
                                                    mediaPickerLauncher.launch("image/* video/*")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Shoot Photo", color = Color.Black, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                mediaPickerLauncher.launch("image/* video/*")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AuraSlateCard),
                                            border = BorderStroke(1.dp, AuraSlateLight),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Choose Gallery", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }

                                if (selectedMediaUri != null) {
                                    item {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, AuraCyanNeon, RoundedCornerShape(12.dp)),
                                            colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                androidx.compose.foundation.Image(
                                                    painter = coil.compose.rememberAsyncImagePainter(
                                                     model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                         .data(selectedMediaUri)
                                                         .size(coil.size.Size(400, 300))
                                                         .crossfade(true)
                                                         .memoryCacheKey("selected_media_preview_${selectedMediaUri.hashCode()}")
                                                         .build()
                                                 ),
                                                    contentDescription = "Preview of captured image/video",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(6.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Captured/Chosen", color = AuraCyanNeon, fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    OutlinedTextField(
                                        value = noteTitleState,
                                        onValueChange = { noteTitleState = it },
                                        placeholder = { Text("Note Title", color = AuraWhiteMuted) },
                                        label = { Text("Title", color = AuraCyanNeon) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AuraCyanNeon, unfocusedBorderColor = AuraSlateLight)
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = noteContentState,
                                        onValueChange = { noteContentState = it },
                                        placeholder = { Text("Add any details or transcription here...", color = AuraWhiteMuted) },
                                        label = { Text("Memos / Details", color = AuraCyanNeon) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AuraCyanNeon, unfocusedBorderColor = AuraSlateLight)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val mediaPath = selectedMediaUri?.toString() ?: ""
                                    viewModel.saveDraftNote(
                                        title = noteTitleState.ifBlank { "Captured Media" },
                                        content = noteContentState,
                                        category = "Photos",
                                        tags = "QuickCapture, Media",
                                        voicePath = null,
                                        drawingData = null,
                                        photoPath = mediaPath.ifBlank { null }
                                    )
                                    selectedMediaUri = null
                                    showImageCaptureDialog = false
                                    viewModel.navigateTo(Section.Notes)
                                },
                                enabled = selectedMediaUri != null
                            ) {
                                Text(
                                    "SAVE NOTE",
                                    color = if (selectedMediaUri != null) AuraCyanNeon else AuraWhiteMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    selectedMediaUri = null
                                    showImageCaptureDialog = false
                                }
                            ) {
                                Text("CANCEL", color = Color.White)
                            }
                        },
                        containerColor = AuraCharcoalBase
                    )
                }

                // --- CUSTOM QUICK CAPTURE ICON DIALOG BEGIN ---
                if (showCustomIconDialog) {
                    val quickCaptureIconUri by viewModel.quickCaptureIconUri.collectAsState()
                    var rawUrlInput by remember { mutableStateOf("") }
                    val presetIcons = listOf(
                        Pair("Neon Star 🌌", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExNG11aDdkczkza3ZodmN2MDY5bTAxMXhhbWVwZ2Z3ZHkxaHA0azBoOSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/gT9SOnHj3S6P1810iC/giphy.gif"),
                        Pair("Neon Bolt ⚡", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExM3ZpcTcyN3F3MXZncDV1MGF1eHR4cnE5N2F4cjA1MG0zampreDNzOSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/WfNfPwO6LidD86v6x9/giphy.gif"),
                        Pair("Cosmic Orbit 🌀", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExbnZnbWptbjRhOG9obmI5N2d3czgzdHYwMzF6dW4wZXk1cnRocjAycSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/Y8S6gBqg3N68N0iH3L/giphy.gif"),
                        Pair("Galaxy Vortex 🌀", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExbDVnbXQ3bTN0Y3kyNWlhNDJ5MTBicm1vMzI2YWZ3dzdtcm91djhpNCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/JsiL1g5xO76yKqO41S/giphy.gif"),
                        Pair("Party Cat 🐱", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExM3Zpc3g0N3l2cW9xczh4amx6dGZ0NDF0bnY1MXQyNHp3MG8xeWFwYyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/vFKqnCdLPNOKc/giphy.gif"),
                        Pair("Banana Cat 😸", "https://media.giphy.com/media/ND6xk9vIfV69O/giphy.gif"),
                        Pair("Wink Doggo 🐶", "https://media.giphy.com/media/12p3g23Ysrvw9W/giphy.gif")
                    )

                    AlertDialog(
                        onDismissRequest = { showCustomIconDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = AuraPurpleAccent, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CUSTOMIZE CAPTURE ICON", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Text("Transform your quick capture button with funny GIFs of cats, dogs, customized circles, animations or photographs!", color = AuraWhiteMuted, fontSize = 11.sp)
                                }

                                item {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                iconPickerLauncher.launch("image/*")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AuraPurpleAccent),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pick from Device", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }

                                item {
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AuraSlateLight))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Amusing Preset GIFs & Circles (tap to apply):", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                item {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(115.dp)
                                    ) {
                                        items(presetIcons) { preset ->
                                            val isSelected = quickCaptureIconUri == preset.second
                                            Box(
                                                modifier = Modifier
                                                    .width(105.dp)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(AuraSlateCard)
                                                    .border(2.dp, if (isSelected) AuraCyanNeon else AuraSlateLight, RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        viewModel.setQuickCaptureIconUri(preset.second)
                                                    }
                                                    .padding(6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(54.dp)
                                                            .clip(CircleShape)
                                                            .background(AuraSlateLight),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        androidx.compose.foundation.Image(
                                                            painter = coil.compose.rememberAsyncImagePainter(
                                                             model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                                 .data(preset.second)
                                                                 .size(coil.size.Size(120, 120))
                                                                 .crossfade(true)
                                                                 .memoryCacheKey("preset_fab_${preset.first}")
                                                                 .build()
                                                         ),
                                                            contentDescription = preset.first,
                                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(preset.first, color = Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    OutlinedTextField(
                                        value = rawUrlInput,
                                        onValueChange = { rawUrlInput = it },
                                        placeholder = { Text("https://example.com/item.gif", color = AuraWhiteMuted) },
                                        label = { Text("Or paste image URL", color = AuraPurpleAccent) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AuraPurpleAccent, unfocusedBorderColor = AuraSlateLight)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (rawUrlInput.isNotBlank()) {
                                        viewModel.setQuickCaptureIconUri(rawUrlInput.trim())
                                    }
                                    showCustomIconDialog = false
                                }
                            ) {
                                Text("DONE", color = AuraPurpleAccent, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    viewModel.setQuickCaptureIconUri("")
                                    showCustomIconDialog = false
                                }
                            ) {
                                Text("RESET", color = Color.Red)
                            }
                        },
                        containerColor = AuraCharcoalBase
                    )
                }

                // --- GLOBAL QUICK TRANSACTION DIALOG (ADD & DEDUCT PAYMENT) ---
                if (showGlobalQuickTransactionType != null) {
                    val txType = showGlobalQuickTransactionType!!
                    val sentOptions by viewModel.sentOptions.collectAsState()
                    QuickTransactionBottomSheet(
                        type = txType,
                        accounts = accounts,
                        friends = friends,
                        sentOptions = sentOptions,
                        onDismiss = { showGlobalQuickTransactionType = null },
                        onSubmit = { amount, recipient, category, note, loc, method, acctId, tickedFriends, includeMe ->
                            viewModel.addTransaction(
                                type = txType,
                                amount = amount,
                                recipientOrSender = recipient,
                                category = category,
                                note = note,
                                location = loc,
                                paymentMethod = method,
                                accountId = acctId
                            )
                            if (category == "Friend" && tickedFriends.isNotEmpty()) {
                                val shareCount = tickedFriends.size + (if (includeMe) 1 else 0)
                                val splitShare = amount / shareCount
                                tickedFriends.forEach { fri ->
                                    viewModel.addDebt(
                                        friendId = fri.id,
                                        friendName = fri.name,
                                        title = note.ifBlank { "Shared Quick Expense" },
                                        totalAmount = splitShare,
                                        amount = splitShare,
                                        isYouOwe = (txType == "RECEIVED")
                                    )
                                }
                            }
                            showGlobalQuickTransactionType = null
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// CENTRAL IMMERSIVE DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onOpenNote: (Note) -> Unit,
    onOpenTask: (Task) -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val notesList by viewModel.activeNotes.collectAsState()
    val tasksList by viewModel.allTasks.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()
    val investments by viewModel.allInvestments.collectAsState()
    val debts by viewModel.allDebts.collectAsState()
    val journals by viewModel.journalEntries.collectAsState()
    val isDashboardLoading by viewModel.isDashboardLoading.collectAsState()

    val todayDate = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US).format(Date())
    val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val totalAvailableBalance = accounts.sumOf { it.balance }
    val totalInvested = investments.sumOf { it.amount }
    val totalToReceive = debts.filter { !it.isYouOwe && it.status == "PENDING" }.sumOf { it.remainingAmount }
    val totalYouOwe = debts.filter { it.isYouOwe && it.status == "PENDING" }.sumOf { it.remainingAmount }
    val netWorth = totalAvailableBalance + totalInvested + totalToReceive - totalYouOwe

    val todayJournal = journals.find { it.date == todayString }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header info
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("HELLO USER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AuraCyanNeon, letterSpacing = 1.sp)
                    Text("MY WORKSPACE", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(todayDate.uppercase(), fontSize = 11.sp, color = AuraWhiteMuted)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraSectionInfoButton(
                        viewModel = viewModel,
                        title = "Zen Workspace",
                        description = "Your Aura central dashboard. View quick status summaries, cognitive daily completion metrics, recent offline notes, active timers, and instant security stats at a single glance."
                    )
                    IconButton(
                        onClick = { viewModel.navigateTo(Section.SecuritySettings) },
                        modifier = Modifier.background(AuraSlateCard.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }
        }

        if (isDashboardLoading) {
            item {
                ShimmerDashboardGrid()
            }
        } else {
            // COGNITIVE STATUS PROGRESS CARD (Clickable: Navigates to Daily Tasks)
            item {
                Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .auraSpringPress(
                        cornerRadius = 24.dp,
                        onClick = { viewModel.navigateTo(Section.Tasks) }
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(AuraCyanNeon.copy(alpha = 0.4f), AuraPurpleAccent.copy(alpha = 0.2f))),
                        RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "DAILY COMPLETION PROGRESS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraWhiteMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stats.productivityPercentage}% ACHIEVED",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = AuraCyanNeon
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stats.todayCompletedTasksCount} objectives finished of ${stats.todayTasksCount} scheduled for today",
                            fontSize = 11.sp,
                            color = AuraWhiteMedium
                        )
                    }

                    // Progress Ring Custom Canvas
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                        val progressSweep = animateFloatAsState(
                            targetValue = stats.productivityPercentage.toFloat() / 100f,
                            animationSpec = tween(1000, easing = FastOutSlowInEasing)
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = AuraSlateLight, style = Stroke(6.dp.toPx()))
                            drawArc(
                                color = AuraCyanNeon,
                                startAngle = -90f,
                                sweepAngle = progressSweep.value * 360f,
                                useCenter = false,
                                style = Stroke(6.dp.toPx())
                            )
                        }
                        Text(
                            "${stats.productivityPercentage}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // QUICK ENGAGEMENT GRID (100% Clickable navigation shortcuts to all sub-modules)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Notes count card (Clickable gateway to Section.Notes)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .auraSpringPress(
                                cornerRadius = 16.dp,
                                onClick = { viewModel.navigateTo(Section.Notes) }
                            )
                            .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Feed, contentDescription = "Notes", tint = AuraPurpleAccent)
                                Icon(Icons.Default.ArrowForward, contentDescription = "View", tint = AuraWhiteMuted, modifier = Modifier.size(12.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text("Notes Ledger", fontSize = 11.sp, color = AuraWhiteMuted)
                                Text("${stats.totalNotesCount} pgs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Habits fire streaks card (Clickable gateway to Section.Habits)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .auraSpringPress(
                                cornerRadius = 16.dp,
                                onClick = { viewModel.navigateTo(Section.Habits) }
                            )
                            .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streaks count", tint = AuraCopperWarm)
                                Icon(Icons.Default.ArrowForward, contentDescription = "View", tint = AuraWhiteMuted, modifier = Modifier.size(12.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text("Habit Stream", fontSize = 11.sp, color = AuraWhiteMuted)
                                Text("${stats.allTimeStreakValue} 🔥 days", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AuraCopperWarm)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Money Ledger Net Worth card (Clickable gateway to Section.Money)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .auraSpringPress(
                                cornerRadius = 16.dp,
                                onClick = { viewModel.navigateTo(Section.Money) }
                            )
                            .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = "Financial Net Worth", tint = AuraCyanNeon)
                                Icon(Icons.Default.ArrowForward, contentDescription = "View", tint = AuraWhiteMuted, modifier = Modifier.size(12.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text("Money Ledger", fontSize = 11.sp, color = AuraWhiteMuted)
                                Text("₹${"%,.0f".format(netWorth)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Calendar Day/Mood card (Clickable gateway to Section.Day)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .auraSpringPress(
                                cornerRadius = 16.dp,
                                onClick = { viewModel.navigateTo(Section.Day) }
                            )
                            .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Calendar/Diary", tint = MoodContent)
                                Icon(Icons.Default.ArrowForward, contentDescription = "View", tint = AuraWhiteMuted, modifier = Modifier.size(12.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text("Journal/Day", fontSize = 11.sp, color = AuraWhiteMuted)
                                Text(if (todayJournal != null) todayJournal.mood else "Not logged", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuraCyanNeon)
                            }
                        }
                    }
                }
            }
        }

        // RECENT NOTES HORIZONTAL CAROUSEL SLIDER (Header clickable to navigate)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.navigateTo(Section.Notes) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "RECENT NOTEBOOK REVISIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraWhiteMuted,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("VIEW ALL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AuraCyanNeon)
                        Icon(Icons.Default.ArrowForward, contentDescription = "Notes", tint = AuraCyanNeon, modifier = Modifier.size(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (notesList.isEmpty()) {
                    Text("Draft digital text tabs or scribble drawings in notes.", fontSize = 11.sp, color = AuraWhiteMuted)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(notesList.take(6)) { note ->
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(110.dp)
                                    .border(1.dp, AuraSlateLight, RoundedCornerShape(14.dp))
                                    .clickable { onOpenNote(note) },
                                colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(note.title.ifBlank { "Untitled" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(note.content, fontSize = 10.sp, color = AuraWhiteMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(note.category.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = AuraCyanNeon)
                                }
                            }
                        }
                    }
                }
            }
        }

        // TODAY'S TASKS LIST QUICK PREVIEW (Header clickable to navigate)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.navigateTo(Section.Tasks) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TODAY OBJECTIVES PREVIEW",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraWhiteMuted,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("MANAGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AuraCyanNeon)
                    Icon(Icons.Default.ArrowForward, contentDescription = "Tasks", tint = AuraCyanNeon, modifier = Modifier.size(10.dp))
                }
            }
        }

        val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayTasks = tasksList.filter { it.date == todayString }

        if (todayTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DoneAll, contentDescription = "All done", tint = AuraWhiteMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No objectives planned for today", fontSize = 12.sp, color = AuraWhiteMuted)
                    }
                }
            }
        } else {
            items(todayTasks, key = { it.id }) { task ->
                TaskRowItem(
                    task = task,
                    onClicked = { onOpenTask(task) },
                    onToggleCompleted = { viewModel.toggleTaskCompleted(task) },
                    onStartTimer = { viewModel.startTaskTimer(task.id, 25) }
                )
            }
        }
        }
    }
}

// ==========================================
// COMPACT GLASS NAVIGATION BAR
// ==========================================
@Composable
fun AuraBottomNavRow(
    viewModel: AppViewModel,
    active: Section
) {
    Surface(
        color = AuraCharcoalBase.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dashboard
            IconButton(onClick = { viewModel.navigateTo(Section.Dashboard) }) {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = "Workspace Dashboard",
                    tint = if (active == Section.Dashboard) AuraCyanNeon else AuraWhiteMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Notebook
            IconButton(onClick = { viewModel.navigateTo(Section.Notes) }) {
                Icon(
                    imageVector = Icons.Default.Feed,
                    contentDescription = "Notebook tabs",
                    tint = if (active == Section.Notes) AuraCyanNeon else AuraWhiteMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tasks Kanban
            IconButton(onClick = { viewModel.navigateTo(Section.Tasks) }, modifier = Modifier.testTag("nav_tasks")) {
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = "Tasks Kanban",
                    tint = if (active == Section.Tasks) AuraCyanNeon else AuraWhiteMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Habits
            IconButton(onClick = { viewModel.navigateTo(Section.Habits) }) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Habits check trackers",
                    tint = if (active == Section.Habits) AuraCyanNeon else AuraWhiteMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Calendars & diaries
            IconButton(onClick = { viewModel.navigateTo(Section.Day) }, modifier = Modifier.testTag("nav_journal")) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Calendars diary logs",
                    tint = if (active == Section.Day) AuraCyanNeon else AuraWhiteMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Money tracking engine
            IconButton(onClick = { viewModel.navigateTo(Section.Money) }, modifier = Modifier.testTag("nav_money")) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = "Ledger and splits",
                    tint = if (active == Section.Money) AuraCyanNeon else AuraWhiteMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ==========================================
// SECURITY FROSTED PASS KEYPAD LOCK SCREEN
// ==========================================
@Composable
fun SecurityPinKeypadGate(
    viewModel: AppViewModel
) {
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = "Security Required", tint = AuraPurpleAccent, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("AURA SAFEGUARD LOCK", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
        Text("Enter local verification code PIN", fontSize = 11.sp, color = AuraWhiteMuted)

        Spacer(modifier = Modifier.height(24.dp))

        // Circular dot placeholders
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 1..4) {
                val isActive = pinText.length >= i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(if (isActive) AuraCyanNeon else AuraSlateLight, CircleShape)
                        .border(1.dp, if (isActive) Color.White else Color.Transparent, CircleShape)
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(errorMessage, fontSize = 11.sp, color = Color.Red)
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Matrix keypad layout
        val buttonsList = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.width(280.dp)
        ) {
            items(buttonsList) { digit ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(AuraSlateCard, CircleShape)
                        .border(1.dp, AuraSlateLight, CircleShape)
                        .clickable {
                            if (digit == "C") {
                                pinText = ""
                                errorMessage = ""
                            } else if (digit == "OK") {
                                if (pinText.length == 4) {
                                    val success = viewModel.verifyPin(pinText)
                                    if (!success) {
                                        errorMessage = "Invalid security PIN passcode"
                                        pinText = ""
                                    }
                                }
                            } else {
                                if (pinText.length < 4) {
                                    pinText += digit
                                }
                            }
                        }
                        .testTag("pin_btn_$digit"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(digit, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// PREFERENCES / PIN CONFIGURATIONS
// ==========================================
@Composable
fun AppSecuritySettingsScreen(
    viewModel: AppViewModel
) {
    val securitySec by viewModel.securitySettings.collectAsState()
    val friends by viewModel.allFriends.collectAsState()
    val sentOptions by viewModel.sentOptions.collectAsState()

    // Collect Google Cloud Sync states
    val isSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
    val userEmail by viewModel.cloudUserEmail.collectAsState()
    val isSyncing by viewModel.isCurrentlySyncing.collectAsState()
    val lastSync by viewModel.lastSyncedTime.collectAsState()
    val backups by viewModel.mockCloudBackups.collectAsState()
    val profileName by viewModel.profileDisplayName.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()

    // Collect DB sizes for the data summary
    val notesCount = viewModel.activeNotes.collectAsState().value.size
    val tasksCount = viewModel.allTasks.collectAsState().value.size
    val habitsCount = viewModel.habits.collectAsState().value.size
    val transactionCount = viewModel.allTransactions.collectAsState().value.size

    var isEnabled by remember(securitySec) { mutableStateOf(securitySec?.isLockEnabled ?: false) }
    var pinValue by remember(securitySec) { mutableStateOf(securitySec?.pinCode ?: "") }

    var newOptionText by remember { mutableStateOf("") }
    var newFriendName by remember { mutableStateOf("") }
    var newFriendPhone by remember { mutableStateOf("") }
    var newFriendNotes by remember { mutableStateOf("") }

    // Settings Profile Sync states
    var showGoogleSignDialogInSettings by remember { mutableStateOf(false) }
    var tempProfileNameInput by remember(profileName) { mutableStateOf(profileName) }
    var isEditingProfile by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val intent = result.data
            if (intent != null) {
                try {
                    val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(intent)
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    val idToken = account?.idToken
                    if (idToken != null) {
                        viewModel.signInWithGoogleReal(idToken) { success ->
                            // real Google Sign-In succeeded
                        }
                    } else {
                        account?.email?.let { email ->
                            viewModel.signInWithGoogle(email)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    )

    if (showGoogleSignDialogInSettings) {
        var tempEmailInput by remember { mutableStateOf(userEmail ?: "moreaboutastram@gmail.com") }
        AlertDialog(
            onDismissRequest = { showGoogleSignDialogInSettings = false },
            title = { Text("CONNECT GOOGLE WORKSPACE ACCOUNT", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select your authorization pathway for secure Cloud database synchronization & multi-device backup indexing:", color = AuraWhiteMedium, fontSize = 11.sp, lineHeight = 15.sp)
                    
                    // Real Connection Button
                    Button(
                        onClick = {
                            try {
                                val intent = viewModel.authManager.getSignInIntent()
                                googleSignInLauncher.launch(intent)
                                showGoogleSignDialogInSettings = false
                            } catch (e: Exception) {
                                // fall back to standard text field input if services not loaded
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIGN IN WITH GOOGLE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Text("Or configure simulated email identification below:", color = AuraWhiteMedium, fontSize = 10.sp)

                    OutlinedTextField(
                        value = tempEmailInput,
                        onValueChange = { tempEmailInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyanNeon,
                            unfocusedBorderColor = AuraSlateLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        placeholder = { Text("example@gmail.com") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signInWithGoogle(tempEmailInput)
                        showGoogleSignDialogInSettings = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
                ) {
                    Text("IDENTIFY & CONNECT", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showGoogleSignDialogInSettings = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraSlateLight)
                ) {
                    Text("CANCEL", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = AuraSlateCard
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo(Section.Dashboard) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Dashboard", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("SETTINGS & SYSTEM CONFIG", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // ======================================================================
        // NEW CARD: GOOGLE CLOUD SYNC & PROFILE MANAGEMENT (USER REQUEST)
        // ======================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, if (isSyncEnabled) AuraCyanNeon.copy(alpha = 0.5f) else AuraSlateLight, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Cloud logo",
                            tint = if (isSyncEnabled) AuraCyanNeon else AuraPurpleAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "AURA PROFILE & GOOGLE DRIVE CLOUD SYNC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AuraCyanNeon, strokeWidth = 2.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isSyncEnabled) MoodHappy else AuraCopperWarm, CircleShape)
                        )
                    }
                }

                Divider(color = AuraSlateLight.copy(alpha = 0.3f))

                // Profile Display Name Config
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Sync Profile Label", fontSize = 9.sp, color = AuraWhiteMuted, fontWeight = FontWeight.Bold)
                        if (isEditingProfile) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = tempProfileNameInput,
                                    onValueChange = { tempProfileNameInput = it },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AuraCyanNeon,
                                        unfocusedBorderColor = AuraSlateLight,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        if (tempProfileNameInput.isNotBlank()) {
                                            viewModel.updateProfileName(tempProfileNameInput)
                                        }
                                        isEditingProfile = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("SAVE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = profileName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile Name",
                                    tint = AuraCyanNeon,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { isEditingProfile = true }
                                )
                            }
                        }
                    }
                }

                // Cloud Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AuraObsidian.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Google Cloud Connection Status", fontSize = 9.sp, color = AuraWhiteMuted, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (userEmail != null) "Connected: $userEmail" else "Offline Local Only (Unlinked)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (userEmail != null) AuraCyanNeon else AuraWhiteMedium
                        )
                        Text(
                            text = "Mirror Latency Status: $lastSync",
                            fontSize = 8.sp,
                            color = AuraWhiteMuted
                        )
                    }

                    if (userEmail == null) {
                        Button(
                            onClick = { showGoogleSignDialogInSettings = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraSlateLight),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AuraCyanNeon.copy(alpha = 0.3f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("LINK DRIVE", color = AuraCyanNeon, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Sign Out",
                            color = Color.Red.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    viewModel.signOut()
                                    android.widget.Toast.makeText(context, "Unlinked Google account successfully.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        )
                    }
                }

                // Switch option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text("Enable Google Cloud Real-Time Auto-Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Triggers background drive updates on any database frames changed.", fontSize = 9.sp, color = AuraWhiteMuted)
                    }
                    Switch(
                        checked = isSyncEnabled,
                        onCheckedChange = { viewModel.setCloudSyncEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AuraCyanNeon, checkedTrackColor = AuraCyanNeon.copy(alpha = 0.5f))
                    )
                }

                // Database Frames summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AuraSlateCard.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("COMPRESSED DATA SUMMARY FOR DRIVE:", fontSize = 8.sp, fontWeight = FontWeight.Black, color = AuraPurpleAccent, letterSpacing = 0.5.sp)
                        Text(
                            text = "⚡ Real Database Payload: $notesCount Notes | $tasksCount Tasks | $habitsCount Habits | $transactionCount Ledger Records",
                            fontSize = 10.sp,
                            color = AuraWhiteMedium
                        )
                    }
                }

                // Export/Import buttons simulating save and other device
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            if (userEmail == null) {
                                android.widget.Toast.makeText(context, "Please connect Google Drive profile first to export payload!", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.createGoogleDriveBackup()
                                android.widget.Toast.makeText(context, "Compressed $notesCount notes, $tasksCount tasks, and $habitsCount habits exported safely to Drive!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Export icon", tint = Color.Black, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT & SAVE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { 
                            if (userEmail == null) {
                                android.widget.Toast.makeText(context, "Please connect Google Drive profile first to import payload!", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.triggerSyncNow()
                                android.widget.Toast.makeText(context, "Retrieving database payload indexes; device indices refreshed successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraSlateLight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        border = BorderStroke(1.dp, AuraSlateLight)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Import icon", tint = AuraCyanNeon, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("IMPORT / RESTORE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Simulating Cross-device
                Button(
                    onClick = {
                        if (userEmail == null) {
                            android.widget.Toast.makeText(context, "Google Account required to simulate cross-device syncing!", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            viewModel.simulateDeviceImportFromDrive()
                            android.widget.Toast.makeText(context, "Synchronizing current database with external workspace indices!", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraSlateCard),
                    border = BorderStroke(1.dp, AuraPurpleAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Simulate pull", tint = AuraPurpleAccent, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SIMULATE EXTERNAL DEVICE SYNC & PULL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.2.sp)
                }

                // Connected Devices List
                Spacer(modifier = Modifier.height(4.dp))
                Text("WORKSPACE CLOUD PARTICIPANT DEVICES:", fontSize = 8.sp, fontWeight = FontWeight.Black, color = AuraWhiteMuted, letterSpacing = 0.5.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    connectedDevices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AuraObsidian.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (device.contains("This Device")) Icons.Default.Smartphone else Icons.Default.Tv,
                                    contentDescription = "Device icon",
                                    tint = if (device.contains("This Device")) AuraCyanNeon else AuraWhiteMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(device, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = if (device.contains("This Device")) "Active Now" else "Idle State Sync",
                                fontSize = 8.sp,
                                color = if (device.contains("This Device")) MoodHappy else AuraWhiteMuted
                            )
                        }
                    }
                }
            }
        }

        // ======================================================================
        // NEW CARD: CAPTURE BUTTON AESTHETICS & FEEL (USER REQUEST)
        // ======================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, AuraSlateLight, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase),
            shape = RoundedCornerShape(20.dp)
        ) {
            val currentCaptureColor by viewModel.captureButtonColor.collectAsState()
            val currentCaptureAnim by viewModel.captureButtonAnimationType.collectAsState()

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Aesthetics logo",
                        tint = AuraCyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "CAPTURE BUTTON AESTHETICS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    "Personalize the interactive glowing aura, haptic trigger states, and physics dynamics of the floating Quick Capture overlay.",
                    color = AuraWhiteMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AuraSlateLight))

                // 1. Color Selector
                Text("INTELLIGENT TINT GLOW COLOR:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraCyanNeon)

                class LocalCaptureColorSetting(val id: String, val label: String, val brush: Brush)

                val colorOptions = listOf(
                    LocalCaptureColorSetting("DEFAULT", "Gradient", Brush.linearGradient(listOf(AuraCyanNeon, AuraPurpleAccent))),
                    LocalCaptureColorSetting("CYAN", "Cyber Cyan", Brush.linearGradient(listOf(AuraCyanNeon, AuraCyanNeon))),
                    LocalCaptureColorSetting("PURPLE", "Aura Purple", Brush.linearGradient(listOf(AuraPurpleAccent, AuraPurpleAccent))),
                    LocalCaptureColorSetting("EMERALD", "Emerald Mint", Brush.linearGradient(listOf(Color(0xFF00FF87), Color(0xFF00FF87)))),
                    LocalCaptureColorSetting("PINK", "Hot Pink", Brush.linearGradient(listOf(Color(0xFFFF2A85), Color(0xFFFF2A85)))),
                    LocalCaptureColorSetting("RED", "Volcano", Brush.linearGradient(listOf(Color(0xFFFF3E3E), Color(0xFFFF3E3E)))),
                    LocalCaptureColorSetting("AMBER", "Amber Gold", Brush.linearGradient(listOf(Color(0xFFFFC107), Color(0xFFFFC107))))
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colorOptions) { option ->
                        val isSelected = currentCaptureColor == option.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AuraSlateCard)
                                .border(1.5.dp, if (isSelected) AuraCyanNeon else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setCaptureButtonColor(option.id)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(option.brush)
                                )
                                Text(option.label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AuraSlateLight))

                // 2. Animation Selector
                Text("PULSE & ANIMATION DYNAMICS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraPurpleAccent)

                val animOptions = listOf(
                    Pair("SPRING", "Classic Spring"),
                    Pair("BREATHE", "Aura Breathing"),
                    Pair("PULSE", "Holographic Pulse"),
                    Pair("SPIN", "Subtle Spin")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    animOptions.forEach { anim ->
                        val isSelected = currentCaptureAnim == anim.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AuraPurpleAccent.copy(alpha = 0.2f) else AuraSlateCard)
                                .border(1.5.dp, if (isSelected) AuraPurpleAccent else AuraSlateLight, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setCaptureButtonAnimationType(anim.first)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = anim.second,
                                color = if (isSelected) Color.White else AuraWhiteMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 1. PIN LOCK CARD
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "SECURITY CONFIRMATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraCyanNeon,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable PIN Lock Passcode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Prompts keypad gate upon launching Aura safeguard", fontSize = 11.sp, color = AuraWhiteMuted)
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AuraCyanNeon, checkedTrackColor = AuraCyanNeon.copy(alpha = 0.5f))
                    )
                }

                if (isEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 4) pinValue = it },
                        placeholder = { Text("Enter 4-character PIN", color = AuraWhiteMuted) },
                        label = { Text("Security passcode", color = AuraCyanNeon) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AuraCyanNeon, unfocusedBorderColor = AuraSlateLight),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.configurePin(
                            pin = if (isEnabled) pinValue else null,
                            enabled = isEnabled
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Security Policies", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1.5 VISUAL SYSTEM THEMING CARD
        val activeMode by viewModel.themeMode.collectAsState()
        val activePalette by viewModel.themePalette.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "VISUAL COLOR WORKSPACE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraCyanNeon,
                    letterSpacing = 1.sp
                )
                Text(
                    "Customize workspace environments, background theme, and overall color accent palettes.",
                    fontSize = 11.sp,
                    color = AuraWhiteMuted
                )

                // Theme Mode Selector
                Text("Background Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf("DARK" to "Deep Slate", "AMOLED" to "Black AMOLED", "LIGHT" to "Premium Bone")
                    modes.forEach { (m, label) ->
                        val isSelected = activeMode == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (isSelected) AuraCyanNeon else AuraSlateCard,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(1.dp, if (isSelected) AuraCyanNeon else AuraSlateLight, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setThemeMode(m) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Color accent palettes
                Text("Color Accent Palette", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val palettes = listOf(
                        "CYAN_GLOW" to Pair("Aura Cyan Glow", AuraCyanNeon),
                        "EMERALD_GARDEN" to Pair("Mint Emerald", Color(0xFF2ECD71)),
                        "RADIANT_SUNSET" to Pair("Radiant Sunset", Color(0xFFFF5722)),
                        "ROYAL_AMETHYST" to Pair("Royal Amethyst", Color(0xFFBB86FC)),
                        "OCEAN_BREEZE" to Pair("Ocean Breeze", Color(0xFF00B0FF))
                    )
                    palettes.forEach { (p, pair) ->
                        val (pLabel, pColor) = pair
                        val isSelected = activePalette == p
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) AuraSlateLight else AuraSlateCard, RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) AuraCyanNeon else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setThemePalette(p) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(pColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(pLabel, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = AuraCyanNeon, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // 2. QUICK SENT TARGET CARD
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "QUICK TRANSACTION TARGETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraCyanNeon,
                    letterSpacing = 1.sp
                )
                Text(
                    "Customize destination options available under Quick Transaction 'Money Sent'. Default categories include Food, Friend, Merchant.",
                    fontSize = 11.sp,
                    color = AuraWhiteMuted
                )

                // Grid layout of target options
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sentOptions.chunked(3).forEach { rowOpts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowOpts.forEach { opt ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(AuraSlateCard, RoundedCornerShape(8.dp))
                                        .border(1.dp, AuraSlateLight, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(opt, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        IconButton(
                                            onClick = { viewModel.removeSentOption(opt) },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove option",
                                                tint = Color.Red,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Fill remaining space if chunks < 3
                            repeat(3 - rowOpts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Divider(color = AuraSlateLight.copy(alpha = 0.5f))

                // Inline form to add option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newOptionText,
                        onValueChange = { newOptionText = it },
                        placeholder = { Text("e.g. Rent, Gift, Travel", color = AuraWhiteMuted) },
                        label = { Text("Add Custom Option", color = AuraCyanNeon) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AuraCyanNeon, unfocusedBorderColor = AuraSlateLight),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    
                    Button(
                        onClick = {
                            if (newOptionText.isNotBlank()) {
                                viewModel.addSentOption(newOptionText)
                                newOptionText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 3. FRIENDS REGISTER CARD
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "FRIENDS DIRECTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraCyanNeon,
                    letterSpacing = 1.sp
                )
                Text(
                    "Manage your friends registry used for splitting bills. Your list is stored locally on this device.",
                    fontSize = 11.sp,
                    color = AuraWhiteMuted
                )

                // Scrollable list of friends directly inside the card in Settings with delete triggers
                if (friends.isEmpty()) {
                    Text(
                        "No friends registered in directory yet.",
                        color = AuraWhiteMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        friends.forEach { frnd ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AuraSlateCard, RoundedCornerShape(10.dp))
                                    .border(1.dp, AuraSlateLight, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(frnd.name, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    if (frnd.notes.isNotBlank() || frnd.phone.isNotBlank()) {
                                        Text(
                                            listOfNotNull(
                                                frnd.phone.takeIf { it.isNotBlank() },
                                                frnd.notes.takeIf { it.isNotBlank() }
                                            ).joinToString(" | "),
                                            fontSize = 9.sp,
                                            color = AuraWhiteMuted
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteFriend(frnd) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove friend",
                                        tint = Color.Red.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = AuraSlateLight.copy(alpha = 0.5f))

                // Inline form to add friend
                Text("Register New Friend Entry", fontSize = 11.sp, color = AuraWhiteMedium, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = newFriendName,
                    onValueChange = { newFriendName = it },
                    placeholder = { Text("Friend Name", color = AuraWhiteMuted) },
                    label = { Text("Full Name", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AuraCyanNeon, unfocusedBorderColor = AuraSlateLight),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newFriendPhone,
                        onValueChange = { newFriendPhone = it },
                        placeholder = { Text("Phone (optional)", color = AuraWhiteMuted) },
                        label = { Text("Phone", color = AuraCyanNeon) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AuraCyanNeon, unfocusedBorderColor = AuraSlateLight),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newFriendNotes,
                        onValueChange = { newFriendNotes = it },
                        placeholder = { Text("e.g. Roommate", color = AuraWhiteMuted) },
                        label = { Text("Notes", color = AuraCyanNeon) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AuraCyanNeon, unfocusedBorderColor = AuraSlateLight),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Button(
                    onClick = {
                        if (newFriendName.isNotBlank()) {
                            viewModel.addFriend(newFriendName, newFriendPhone, newFriendNotes)
                            newFriendName = ""
                            newFriendPhone = ""
                            newFriendNotes = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Add Friend to Ledger", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // ======================================================================
        // ABOUT US CARD (USER REQUEST)
        // ======================================================================
        val currentAboutUsText by viewModel.aboutUsText.collectAsState()
        val isOwner = (userEmail?.trim()?.lowercase() == "moreaboutastram@gmail.com")
        var isEditingAboutUs by remember { mutableStateOf(false) }
        var tempAboutUsInput by remember { mutableStateOf("") }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About us logo",
                            tint = AuraCyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "ABOUT US & CREATOR PANEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }

                    // Only show "EDIT" button if logged in as moreaboutastram@gmail.com
                    if (isOwner) {
                        if (isEditingAboutUs) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = { isEditingAboutUs = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("CANCEL", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateAboutUsText(tempAboutUsInput)
                                        isEditingAboutUs = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("SAVE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    tempAboutUsInput = currentAboutUsText
                                    isEditingAboutUs = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AuraPurpleAccent),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("EDIT (OWNER)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Helpful reminder for other emails
                        Box(
                            modifier = Modifier
                                .background(AuraSlateLight, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("READ ONLY", color = AuraWhiteMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isEditingAboutUs && isOwner) {
                    OutlinedTextField(
                        value = tempAboutUsInput,
                        onValueChange = { tempAboutUsInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyanNeon,
                            unfocusedBorderColor = AuraSlateLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraSlateCard, RoundedCornerShape(10.dp))
                            .border(1.dp, AuraSlateLight, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = currentAboutUsText,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                if (!isOwner) {
                    Text(
                        "* Log in with Gmail ‘moreaboutastram@gmail.com’ via Sync to authenticate as owner and edit this content.",
                        color = AuraWhiteMuted,
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // Elegant minimal footer section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "your support matters".uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AuraCyanNeon,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "created by aadii_xy",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AuraWhiteMuted,
                letterSpacing = 1.sp
            )
        }
    }
}

// ==========================================
// FLOATING PLAYER POPUP CARD
// ==========================================
@Composable
fun GlobalMiniFloatingPlayer(
    header: String,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.linearGradient(listOf(AuraCyanNeon.copy(alpha = 0.6f), AuraPurpleAccent.copy(alpha = 0.3f))),
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Voice playing note",
                        tint = AuraCyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = header,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Player", tint = Color.Red, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Wave progress slider bar
            Slider(
                value = progress,
                onValueChange = onSeek,
                colors = SliderDefaults.colors(
                    thumbColor = AuraCyanNeon,
                    activeTrackColor = AuraCyanNeon,
                    inactiveTrackColor = AuraSlateLight
                ),
                modifier = Modifier.height(18.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = "Playback toggle button",
                        tint = AuraCyanNeon,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// HABITS SCREEN TAB DETAILS
// ==========================================
@Composable
fun HabitsTabScreen(
    viewModel: AppViewModel
) {
    val habitsList by viewModel.habits.collectAsState()
    val rawLogs by viewModel.habitLogs.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTabFreq by remember { mutableStateOf("Daily") } // Daily vs Weekly

    // Habit Timer & Configuration Customization States
    var habitToConfigure by remember { mutableStateOf<Habit?>(null) }
    var configReminderTime by remember { mutableStateOf("") }
    var configTargetMins by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("HABIT MATRIX", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
                Text("Repetitive neurological feedback structures", fontSize = 11.sp, color = AuraWhiteMuted)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraSectionInfoButton(
                    viewModel = viewModel,
                    title = "Habit Matrix",
                    description = "Form atomic habits through repetitive feedback loops. Log completions, set dynamic goals with timed focus logs, and visualize completion metrics."
                )
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraPurpleAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("INSERT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ACTIVE FLOATING HABIT Practice Timer BANNER
        val activeHabitId by viewModel.activeTimerHabitId.collectAsState()
        val habitSecondsLeft by viewModel.habitSecondsLeft.collectAsState()
        val isHabitTimerRunning by viewModel.isHabitTimerRunning.collectAsState()

        if (activeHabitId != null) {
            val timedHabit = habitsList.find { it.id == activeHabitId }
            if (timedHabit != null) {
                Card(
                     modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, AuraPurpleAccent, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = "Active Habit Timer",
                                    tint = AuraPurpleAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACTIVE HABIT FOCUS PRACTICE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraPurpleAccent,
                                    letterSpacing = 1.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.resetHabitTimer() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Timer", tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = timedHabit.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val mins = habitSecondsLeft / 60
                            val secs = habitSecondsLeft % 60
                            val formattedTime = "%02d:%02d".format(mins, secs)

                            Text(
                                text = formattedTime,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = AuraPurpleAccent,
                                style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (isHabitTimerRunning) {
                                            viewModel.pauseHabitTimer()
                                        } else {
                                            viewModel.resumeHabitTimer()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isHabitTimerRunning) AuraCopperWarm else AuraPurpleAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isHabitTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/pause button",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHabitTimerRunning) "PAUSE" else "RESUME",
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.resetHabitTimer() },
                                    border = BorderStroke(1.dp, AuraSlateLight),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("RESET", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        TabRow(
            selectedTabIndex = if (selectedTabFreq == "Daily") 0 else 1,
            containerColor = AuraCharcoalBase,
            contentColor = AuraCyanNeon
        ) {
            Tab(selected = selectedTabFreq == "Daily", onClick = { selectedTabFreq = "Daily" }, text = { Text("DAILY LOOPS", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTabFreq == "Weekly", onClick = { selectedTabFreq = "Weekly" }, text = { Text("WEEKLY LOOPS", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
        }

        Spacer(modifier = Modifier.height(12.dp))

        val filteredHabits = habitsList.filter { it.frequency == selectedTabFreq }

        if (filteredHabits.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = "Empty habits", tint = AuraWhiteMuted, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No local habits configured inside Aura matrix", color = Color.White, fontSize = 13.sp)
                }
            }
        } else {
            val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredHabits, key = { it.id }) { habit ->
                    // Gather completion state for today
                    val isDoneToday = rawLogs.any { it.habitId == habit.id && it.completionDate == todayString }
                    
                    val habitsStreakFlow = remember(habit.id) { viewModel.getStreakForHabit(habit.id) }.collectAsState(initial = HabitStreak(0,0))
                    val completePctFlow = remember(habit.id) { viewModel.getCompletionPercentageForHabit(habit.id) }.collectAsState(initial = 0f)

                    val alarmTime = viewModel.getHabitReminderTime(habit.id)
                    val limitMins = viewModel.getHabitTargetMinutes(habit.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AuraSlateLight, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                IconButton(
                                    onClick = { viewModel.toggleHabitToday(habit.id, !isDoneToday) },
                                    modifier = Modifier.size(28.dp).testTag("habit_toggle_${habit.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isDoneToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Complete Habit for today",
                                        tint = if (isDoneToday) MoodHappy else AuraWhiteMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = habit.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDoneToday) AuraWhiteMuted else Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ChevronRight, contentDescription = "Active streak", tint = AuraCopperWarm, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                "STREAK: ${habitsStreakFlow.value.currentStreak} d",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AuraWhiteDescription
                                            )
                                        }

                                        if (!alarmTime.isNullOrBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.NotificationsActive, contentDescription = "Alarm active", tint = AuraCyanNeon, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(alarmTime, fontSize = 9.sp, color = AuraCyanNeon, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (limitMins > 0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Timer, contentDescription = "Target limit", tint = AuraPurpleAccent, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("${limitMins}m Goal", fontSize = 9.sp, color = AuraPurpleAccent, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Streaks indicator & play controls
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${(completePctFlow.value * 100).toInt()}% COMPLETED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraCyanNeon
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Start habit timer loop
                                    IconButton(
                                        onClick = {
                                            val minutesToRun = if (limitMins > 0) limitMins else 15
                                            viewModel.startHabitTimer(habit.id, minutesToRun)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = "Start Habit Timer", tint = AuraPurpleAccent, modifier = Modifier.size(16.dp))
                                    }

                                    // Config details
                                    IconButton(
                                        onClick = { habitToConfigure = habit },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Habit Alarm Settings", tint = AuraWhiteMuted, modifier = Modifier.size(14.dp))
                                    }

                                    IconButton(onClick = { viewModel.deleteHabit(habit) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Habit", tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var inlineHabitName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("CREATE HABIT", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = inlineHabitName,
                        onValueChange = { inlineHabitName = it },
                        placeholder = { Text("Meditate, water intake...", color = AuraWhiteMuted) },
                        label = { Text("Habit name loop", color = AuraCyanNeon) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AuraCyanNeon, unfocusedBorderColor = AuraSlateLight)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select tracking frequency", fontSize = 11.sp, color = AuraWhiteMuted)
                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Daily", "Weekly").forEach { freq ->
                            val isSel = selectedTabFreq == freq
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedTabFreq = freq },
                                label = { Text(freq) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AuraPurpleAccent, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inlineHabitName.isNotBlank()) {
                            viewModel.createNewHabit(inlineHabitName, selectedTabFreq)
                            showCreateDialog = false
                            inlineHabitName = ""
                        }
                    }
                ) {
                    Text("CREATE", color = AuraCyanNeon)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("CANCEL", color = Color.White)
                }
            },
            containerColor = AuraCharcoalBase
        )
    }

    if (habitToConfigure != null) {
        val editingHabit = habitToConfigure!!
        
        LaunchedEffect(editingHabit.id) {
            configReminderTime = viewModel.getHabitReminderTime(editingHabit.id) ?: ""
            configTargetMins = viewModel.getHabitTargetMinutes(editingHabit.id).let { if (it > 0) it.toString() else "" }
        }
        
        AlertDialog(
            onDismissRequest = { habitToConfigure = null },
            title = { Text("HABIT SETTINGS", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Structure your habit loop with precise target guidelines and daily reminder alarm triggers.", color = AuraWhiteMuted, fontSize = 11.sp)
                    
                    OutlinedTextField(
                        value = configReminderTime,
                        onValueChange = { configReminderTime = it },
                        placeholder = { Text("e.g. 08:30 or 21:00", color = AuraWhiteMuted) },
                        label = { Text("Daily Reminder (HH:MM)", color = AuraWhiteMedium) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AuraCyanNeon, unfocusedBorderColor = AuraSlateLight, focusedTextColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = configTargetMins,
                        onValueChange = { configTargetMins = it.filter { c -> c.isDigit() } },
                        placeholder = { Text("e.g. 15 or 30 mins", color = AuraWhiteMuted) },
                        label = { Text("Practice Goals (Minutes)", color = AuraWhiteMedium) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AuraCyanNeon, unfocusedBorderColor = AuraSlateLight, focusedTextColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = configTargetMins.toIntOrNull() ?: 0
                        viewModel.setHabitReminderTime(editingHabit.id, configReminderTime.ifBlank { null })
                        viewModel.setHabitTargetMinutes(editingHabit.id, mins)
                        habitToConfigure = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
                ) {
                    Text("SAVE CONFIG", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToConfigure = null }) {
                    Text("CANCEL", color = Color.White)
                }
            },
            containerColor = AuraCharcoalBase
        )
    }
}

val AuraWhiteDescription = Color(0xFFC5D1E6)
