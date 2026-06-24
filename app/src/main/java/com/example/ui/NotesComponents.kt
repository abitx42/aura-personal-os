package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.data.NoteVersion
import com.example.audio.PlaybackState
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(
    viewModel: AppViewModel,
    onOpenNoteEditor: (Note?) -> Unit,
    onOpenDrawingWorkspace: (String?) -> Unit
) {
    val searchVal by viewModel.notesSearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedNoteCategory.collectAsState()
    val sortVal by viewModel.notesSortOrder.collectAsState()
    val isGridView by viewModel.isNotesGridView.collectAsState()
    val notesList by viewModel.filteredNotes.collectAsState()

    val availableCategories by viewModel.allUniqueCategories.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "AURA NOTEBOOK",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${notesList.size} notes stored locally",
                    fontSize = 11.sp,
                    color = AuraWhiteMuted
                )
            }

            Row {
                IconButton(onClick = { viewModel.toggleNotesLayout() }) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle Layout",
                        tint = Color.White
                    )
                }

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Notes", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(AuraSlateCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Recent Modified", color = Color.White) },
                            onClick = {
                                viewModel.setNotesSortOrder(SortOrder.ModifiedRecent)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest Modified", color = Color.White) },
                            onClick = {
                                viewModel.setNotesSortOrder(SortOrder.ModifiedOldest)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Recent Created", color = Color.White) },
                            onClick = {
                                viewModel.setNotesSortOrder(SortOrder.CreatedRecent)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Title Alphabetical", color = Color.White) },
                            onClick = {
                                viewModel.setNotesSortOrder(SortOrder.TitleAscending)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Live Clock persistent widget at the top of note section
        LiveClockWidget(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            initialIsAnalog = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchVal,
            onValueChange = { viewModel.setNotesSearchQuery(it) },
            placeholder = { Text("Search by title, tags, contents...", color = AuraWhiteMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AuraWhiteMuted) },
            trailingIcon = {
                if (searchVal.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setNotesSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AuraSlateCard,
                unfocusedContainerColor = AuraSlateCard.copy(alpha = 0.5f),
                focusedBorderColor = AuraCyanNeon,
                unfocusedBorderColor = AuraSlateLight,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val fullList = listOf("All") + availableCategories
                    fullList.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedCategory(cat) },
                            label = { Text(cat, fontSize = 11.sp, color = if (isSelected) Color.Black else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AuraCyanNeon,
                                containerColor = AuraSlateCard
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Document List
        if (notesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DriveFileRenameOutline,
                        contentDescription = "Empty Notes",
                        tint = AuraWhiteMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No local notes found", color = Color.White)
                    Text("Create a new digital tab using the floating button", fontSize = 11.sp, color = AuraWhiteMuted)
                }
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notesList, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            onClicked = { onOpenNoteEditor(note) },
                            onTogglePinned = { viewModel.toggleNotePinned(note) },
                            onToggleFavorite = { viewModel.toggleNoteFavorite(note) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notesList, key = { it.id }) { note ->
                        NoteListItem(
                            note = note,
                            onClicked = { onOpenNoteEditor(note) },
                            onTogglePinned = { viewModel.toggleNotePinned(note) },
                            onToggleFavorite = { viewModel.toggleNoteFavorite(note) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCardItem(
    note: Note,
    onClicked: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .border(
                1.dp,
                if (note.isPinned) AuraCyanNeon.copy(alpha = 0.5f) else AuraSlateLight,
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClicked,
                onLongClick = onTogglePinned
            )
            .testTag("note_item_card_${note.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned) AuraSlateCard else AuraCharcoalBase
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.category.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraCyanNeon,
                        letterSpacing = 1.sp
                    )
                    Row {
                        if (note.isPinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = AuraCyanNeon,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (note.isBookmarked) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = "Bookmarked",
                                tint = AuraPurpleAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = note.title.ifBlank { "Untitled" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = note.content.ifBlank { "Empty details..." },
                            fontSize = 11.sp,
                            color = AuraWhiteMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (note.photoPath != null) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(note.photoPath),
                            contentDescription = "Attachment preview",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, AuraSlateLight, RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sdf.format(Date(note.lastModified)),
                    fontSize = 9.sp,
                    color = AuraWhiteMuted
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.photoPath != null) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Media attached", tint = AuraCyanNeon, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (note.voicePath != null) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Audio track", tint = AuraCyanNeon, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (note.drawingData != null) {
                        Icon(Icons.Default.Edit, contentDescription = "Sketch included", tint = AuraPurpleAccent, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (note.isFavorite) Color.Red else AuraWhiteMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onToggleFavorite() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItem(
    note: Note,
    onClicked: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (note.isPinned) AuraCyanNeon.copy(alpha = 0.4f) else AuraSlateLight,
                RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClicked,
                onLongClick = onTogglePinned
            ),
        colors = CardDefaults.cardColors(containerColor = AuraCharcoalBase),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = AuraCyanNeon, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = note.title.ifBlank { "Untitled" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    fontSize = 11.sp,
                    color = AuraWhiteMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.voicePath != null) {
                    Icon(Icons.Default.Mic, contentDescription = "Audio track", tint = AuraCyanNeon, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                if (note.drawingData != null) {
                    Icon(Icons.Default.Gesture, contentDescription = "Sketch included", tint = AuraPurpleAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (note.isFavorite) Color.Red else AuraWhiteMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// RICH MARKDOWN EDITOR SCREEN
// ==========================================
@Composable
fun NoteEditorScreen(
    note: Note?,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenDrawingBoard: (String?) -> Unit
) {
    val isGlobalRecording by viewModel.isRecording.collectAsState()
    val recordedMicSecs by viewModel.recordedDuration.collectAsState()

    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var category by remember { mutableStateOf(note?.category ?: "Personal") }
    var tags by remember { mutableStateOf(note?.tags ?: "") }
    var isBookmarked by remember { mutableStateOf(note?.isBookmarked ?: false) }

    var isPreviewMode by remember { mutableStateOf(false) }
    var showVersionsSheet by remember { mutableStateOf(false) }

    // Recover drawing and voice attachments
    var currentVoicePath by remember { mutableStateOf(note?.voicePath) }
    var currentDrawingData by remember { mutableStateOf(note?.drawingData) }
    var currentPhotoPath by remember { mutableStateOf(note?.photoPath) }

    val noteVersions by viewModel.noteVersionsFlow.collectAsState()

    // Calculate dynamic counts
    val charCount = content.length
    val wordCount = if (content.trim().isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    val readTimeMinutes = (wordCount / 200).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
    ) {
        // Toolbar
        Surface(color = AuraCharcoalBase, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        // Auto save on back click
                        viewModel.saveDraftNote(title, content, category, tags, currentVoicePath, currentDrawingData, isBookmarked, currentPhotoPath)
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (note == null) "NEW NOTE" else "EDIT NOTE",
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Preview Switch Button
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Default.EditNote else Icons.Default.Visibility,
                            contentDescription = "Toggle Preview",
                            tint = if (isPreviewMode) AuraCyanNeon else Color.White
                        )
                    }

                    // Smart Bookmark Highlight Toggle
                    IconButton(onClick = { isBookmarked = !isBookmarked }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Smart Bookmarking Highlight",
                            tint = if (isBookmarked) AuraPurpleAccent else Color.White
                        )
                    }

                    // Version history trigger
                    if (note != null) {
                        IconButton(onClick = { showVersionsSheet = true }) {
                            Icon(Icons.Default.History, contentDescription = "Version Snapshots", tint = Color.White)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.saveDraftNote(title, content, category, tags, currentVoicePath, currentDrawingData, isBookmarked, currentPhotoPath)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sub Bar: Configuration tags
        Surface(color = AuraSlateCard, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Tag Configuration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Label:", fontSize = 11.sp, color = AuraWhiteMuted)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        var expandedCat by remember { mutableStateOf(false) }
                        AssistChip(
                            onClick = { expandedCat = true },
                            label = { Text(category, fontSize = 11.sp, color = Color.White) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = AuraCharcoalBase),
                            border = null
                        )
                        DropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false },
                            modifier = Modifier.background(AuraSlateCard)
                        ) {
                            listOf("Personal", "Work", "Study", "Ideas", "Journal").forEach { itemCat ->
                                DropdownMenuItem(
                                    text = { Text(itemCat, color = Color.White) },
                                    onClick = {
                                        category = itemCat
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Tags CSV
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    placeholder = { Text("tags separated by comma", fontSize = 11.sp, color = AuraWhiteMuted) },
                    modifier = Modifier
                        .width(180.dp)
                        .height(46.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AuraCharcoalBase,
                        unfocusedContainerColor = AuraCharcoalBase,
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Media attachments layout row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Drawing attachment pill
            AssistChip(
                onClick = { onOpenDrawingBoard(currentDrawingData) },
                label = {
                    Text(
                        if (currentDrawingData == null) "Attach Drawing Workspace" else "Modify Sketchpad drawing",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Gesture,
                        contentDescription = "Drawing",
                        tint = if (currentDrawingData == null) AuraWhiteMuted else AuraCyanNeon,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (currentDrawingData == null) AuraSlateCard else AuraPurpleAccent.copy(alpha = 0.2f)
                ),
                border = null
            )

            // Voice recorder controller pill
            AssistChip(
                onClick = {
                    if (isGlobalRecording) {
                        viewModel.stopAudioNoteRecording()
                        currentVoicePath = viewModel.getVoiceNoteFile()
                    } else {
                        val path = viewModel.startAudioNoteRecording()
                        if (path != null) {
                            currentVoicePath = path
                        }
                    }
                },
                label = {
                    Text(
                        if (isGlobalRecording) "Recording... (${recordedMicSecs / 1000}s)"
                        else if (currentVoicePath != null) "Playback attachment"
                        else "Record Voice Note",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isGlobalRecording) Icons.Default.FiberManualRecord else Icons.Default.Mic,
                        contentDescription = "Voice Recorder",
                        tint = if (isGlobalRecording) Color.Red else if (currentVoicePath != null) AuraCyanNeon else AuraWhiteMuted,
                        modifier = Modifier.size(14.dp).animateContentSize()
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isGlobalRecording) Color.Red.copy(alpha = 0.2f) else AuraSlateCard
                ),
                border = null
            )
        }

        // Play attached voice button if file registered
        if (!isGlobalRecording && currentVoicePath != null) {
            val audioState by viewModel.playbackState.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (audioState is PlaybackState.Playing) {
                        viewModel.pauseVoiceNote()
                    } else if (audioState is PlaybackState.Paused) {
                        viewModel.resumeVoiceNote()
                    } else {
                        viewModel.startPlayingVoiceNote(currentVoicePath!!, title.ifBlank { "Voice Note attachment" })
                    }
                }) {
                    Icon(
                        imageVector = if (audioState is PlaybackState.Playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "Play/Pause attachment",
                        tint = AuraCyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text("Voice attachment loaded", fontSize = 11.sp, color = AuraCyanNeon)

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { currentVoicePath = null }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete voice file", tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Image/Video Attachment Card (Render with Coil)
        if (currentPhotoPath != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AuraSlateCard)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(currentPhotoPath),
                        contentDescription = "Note visual attachment",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    
                    // Small overlay play icon if it's a video
                    if (currentPhotoPath!!.endsWith(".mp4") || currentPhotoPath!!.contains("video")) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Video file", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    // Remove icon in premium glass circle button
                    IconButton(
                        onClick = { currentPhotoPath = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete photo asset", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Main Markdown Text Editor Area
        if (isPreviewMode) {
            // Text rendering block with simulated markdown structures
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, AuraSlateLight, RoundedCornerShape(12.dp))
                    .background(AuraCharcoalBase, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                item {
                    Text(
                        text = title.ifBlank { "Untitled Document" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = AuraSlateLight)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val paragraphs = content.split("\n")
                items(paragraphs) { pText ->
                    MarkdownParagraphRenderer(text = pText)
                }
            }
        } else {
            // Standard Text Fields
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, AuraSlateLight, RoundedCornerShape(12.dp))
                    .background(AuraCharcoalBase, RoundedCornerShape(12.dp))
            ) {
                // Title Field
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AuraWhiteMuted) },
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Divider(color = AuraSlateLight, thickness = 1.dp)

                // Description Contents
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Write your thoughts or markdown headings...", fontSize = 14.sp, color = AuraWhiteMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f).testTag("note_content_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 14.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }

        // Sub footer showing live statistics
        Surface(
            color = AuraCharcoalBase,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Char: $charCount   |   Words: $wordCount",
                    fontSize = 11.sp,
                    color = AuraWhiteMuted
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = "Read Time", tint = AuraWhiteMuted, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Read: ~$readTimeMinutes min",
                        fontSize = 11.sp,
                        color = AuraWhiteMuted
                    )
                }
            }
        }
    }

    // Version history restoration bottom sheet details
    if (showVersionsSheet) {
        AlertDialog(
            onDismissRequest = { showVersionsSheet = false },
            title = { Text("VERSION HISTORY LOGS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    if (noteVersions.isEmpty()) {
                        Text("No snapshot history saved yet.", color = AuraWhiteMuted)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(noteVersions) { version ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.restoreNoteVersion(version)
                                            title = version.title
                                            content = version.content
                                            showVersionsSheet = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = AuraSlateCard)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(version.changeDescription, fontSize = 12.sp, color = AuraCyanNeon, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(version.title, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(version.modifiedAt)),
                                            fontSize = 9.sp,
                                            color = AuraWhiteMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionsSheet = false }) {
                    Text("Close", color = AuraCyanNeon)
                }
            },
            containerColor = AuraCharcoalBase
        )
    }
}

@Composable
fun MarkdownParagraphRenderer(text: String, modifier: Modifier = Modifier) {
    val trimmed = text.trim()
    when {
        // H1 Heading
        trimmed.startsWith("# ") -> {
            Text(
                text = trimmed.substring(2),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = AuraCyanNeon,
                modifier = modifier.padding(vertical = 8.dp)
            )
        }
        // H2 Heading
        trimmed.startsWith("## ") -> {
            Text(
                text = trimmed.substring(3),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = AuraPurpleAccent,
                modifier = modifier.padding(vertical = 6.dp)
            )
        }
        // Bold Block
        trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length > 4 -> {
            Text(
                text = trimmed.substring(2, trimmed.length - 2),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = modifier.padding(vertical = 2.dp)
            )
        }
        // Horizontal divider
        trimmed == "---" -> {
            Divider(color = AuraSlateLight, thickness = 1.dp, modifier = modifier.padding(vertical = 12.dp))
        }
        // Bullet points
        trimmed.startsWith("- ") -> {
            Row(modifier = modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(4.dp).background(AuraCyanNeon, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = trimmed.substring(2), fontSize = 13.sp, color = AuraWhiteMedium)
            }
        }
        // Standard bullet lists / checklists
        trimmed.startsWith("[ ] ") -> {
            Row(modifier = modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = "Todo blank", tint = AuraWhiteMuted, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = trimmed.substring(4), fontSize = 13.sp, color = AuraWhiteMedium)
            }
        }
        trimmed.startsWith("[x] ") -> {
            Row(modifier = modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckBox, contentDescription = "Todo completed", tint = AuraCyanNeon, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trimmed.substring(4),
                    fontSize = 13.sp,
                    color = AuraWhiteMuted,
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }
        else -> {
            Text(
                text = text,
                fontSize = 13.sp,
                color = AuraWhiteMedium,
                lineHeight = 20.sp,
                modifier = modifier.padding(vertical = 2.dp)
            )
        }
    }
}
