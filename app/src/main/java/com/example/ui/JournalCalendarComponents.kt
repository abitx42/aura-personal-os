package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.anim.auraSpringPress
import com.example.ui.anim.ShimmerTimelineRow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalAndCalendarScreen(
    viewModel: AppViewModel,
    onOpenDrawingWorkspace: (String?) -> Unit
) {
    // Collect Activities flowchart
    val activities by viewModel.todayActivitiesFlow.collectAsState(initial = emptyList())
    val selectedDate by viewModel.selectedJournalDate.collectAsState()
    val currentJournal by viewModel.currentJournalEntry.collectAsState()
    val isJournalLoading by viewModel.isHabitsLoading.collectAsState()

    var journalText by remember(currentJournal) { mutableStateOf(currentJournal?.content ?: "") }
    var selectedMood by remember(currentJournal) { mutableStateOf(currentJournal?.mood ?: "") }
    var attachedVoicePath by remember(currentJournal) { mutableStateOf(currentJournal?.voicePath) }
    var attachedDrawingData by remember(currentJournal) { mutableStateOf(currentJournal?.drawingData) }
    var isJournalExpanded by remember { mutableStateOf(false) }

    val moodCategories = listOf(
        "HAPPY" to MoodHappy,
        "CALM" to MoodCalm,
        "CONTENT" to MoodContent,
        "NEUTRAL" to MoodNeutral,
        "CREATIVE" to MoodCreative,
        "TIRED" to MoodTired,
        "SAD" to MoodSad
    )

    // Formatter for calendar
    val todayDateObj = remember { Date() }
    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val weekDayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. TITLE ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAY SECTION",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Real-time pipeline: What you did today",
                        fontSize = 11.sp,
                        color = AuraWhiteMuted
                    )
                }
                AuraSectionInfoButton(
                    viewModel = viewModel,
                    title = "Day Section & Journal",
                    description = "An interactive timeline displaying notes created, tasks completed, and transactions logged for any selected day. Log mood updates and express gratitude on the local offline journal."
                )
            }
        }

        // --- 2. COMPACT DYNAMIC CALENDAR (TODAY'S DATE & WEEK STRIP) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AuraSlateLight, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Part: Primary Today big display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(AuraCyanNeon, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayFormat.format(todayDateObj).uppercase(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                                Text(
                                    text = weekDayFormat.format(todayDateObj).take(3).uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        Column {
                            Text(
                                text = weekDayFormat.format(todayDateObj).uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = monthFormat.format(todayDateObj).uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraCyanNeon,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Right Part: Compact Calendar strip
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Display preceding 3 days and next 1 day for a compact horizontal scroll list
                        val cal = Calendar.getInstance()
                        val daysToShow = mutableListOf<Triple<String, String, Boolean>>() // DayNum, DayNameShort, isSelected
                        
                        // Let's populate 5 days centered on today
                        cal.add(Calendar.DAY_OF_YEAR, -2)
                        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        for (i in 0 until 5) {
                            val key = sdfKey.format(cal.time)
                            val dNum = cal.get(Calendar.DAY_OF_MONTH).toString()
                            val dName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.SUNDAY -> "Su"
                                Calendar.MONDAY -> "Mo"
                                Calendar.TUESDAY -> "Tu"
                                Calendar.WEDNESDAY -> "We"
                                Calendar.THURSDAY -> "Th"
                                Calendar.FRIDAY -> "Fr"
                                Calendar.SATURDAY -> "Sa"
                                else -> ""
                            }
                            daysToShow.add(Triple(dNum, dName, selectedDate == key))
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }

                        daysToShow.forEach { (num, name, chosen) ->
                            Box(
                                modifier = Modifier
                                    .size(width = 32.dp, height = 44.dp)
                                    .background(
                                        color = if (chosen) AuraCyanNeon else AuraSlateCard,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (chosen) Color.White else AuraSlateLight.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .auraSpringPress(
                                        cornerRadius = 8.dp,
                                        onClick = {
                                            // Update VM selected date
                                            val c = Calendar.getInstance()
                                            c.set(Calendar.DAY_OF_MONTH, num.toInt())
                                            val newDateStr = sdfKey.format(c.time)
                                            viewModel.selectJournalDate(newDateStr)
                                            viewModel.selectTaskDate(newDateStr)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = name,
                                        fontSize = 8.sp,
                                        color = if (chosen) Color.Black else AuraWhiteMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = num,
                                        fontSize = 11.sp,
                                        color = if (chosen) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. ACCORDION: QUICK DAILY MENTAL REFLECTIONS & JOURNALING ---
        item {
            Card(
                modifier = Modifier
                    .fillModifierCompact()
                    .auraSpringPress(
                        cornerRadius = 16.dp,
                        onClick = { isJournalExpanded = !isJournalExpanded }
                    )
                    .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = "Journal", tint = AuraCyanNeon, modifier = Modifier.size(16.dp))
                            Text(
                                "DAILY MENTAL REFLECTIONS journal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Icon(
                            imageVector = if (isJournalExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = AuraWhiteMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isJournalExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Mood selector
                        Text("Current Day Cognitive State:", fontSize = 10.sp, color = AuraWhiteMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            moodCategories.forEach { (moodName, colorsVal) ->
                                val isSelected = selectedMood == moodName
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) colorsVal else AuraSlateCard,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedMood = moodName }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = moodName,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else AuraWhiteMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Reflection input
                        OutlinedTextField(
                            value = journalText,
                            onValueChange = { journalText = it },
                            placeholder = { Text("Log cognitive notes, workouts, ideas, mental health patterns...", color = AuraWhiteMuted, fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraCyanNeon,
                                unfocusedBorderColor = AuraSlateLight,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sketches/Voice actions row triggers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Paintboard Trigger
                            AssistChip(
                                onClick = { onOpenDrawingWorkspace(attachedDrawingData) },
                                label = { Text("Canvas Paint", color = Color.White, fontSize = 10.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Gesture,
                                        contentDescription = "Sketches",
                                        tint = if (attachedDrawingData != null) AuraCyanNeon else AuraWhiteMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            )

                            // Voice Recorder
                            val isRecordingGlobally by viewModel.isRecording.collectAsState()
                            AssistChip(
                                onClick = {
                                    if (isRecordingGlobally) {
                                        viewModel.stopAudioNoteRecording()
                                        attachedVoicePath = viewModel.getVoiceNoteFile()
                                    } else {
                                        val path = viewModel.startAudioNoteRecording()
                                        if (path != null) {
                                            attachedVoicePath = path
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (isRecordingGlobally) "Rec..." else "Voice",
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isRecordingGlobally) Icons.Default.FiberManualRecord else Icons.Default.Mic,
                                        contentDescription = "Mic",
                                        tint = if (isRecordingGlobally) Color.Red else if (attachedVoicePath != null) AuraCyanNeon else AuraWhiteMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    viewModel.saveJournal(
                                        content = journalText,
                                        mood = selectedMood,
                                        voicePath = attachedVoicePath,
                                        drawingData = attachedDrawingData
                                    )
                                    isJournalExpanded = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AuraPurpleAccent),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text("SAVE LOG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. FLOWCHART TITLE ---
        item {
            Text(
                text = "DAY FLOW CHART PIPELINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AuraPurpleAccent,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // --- 5. ACTIVITY LIST TIMELINE PIPELINE ---
        if (isJournalLoading) {
            items(4) {
                ShimmerTimelineRow()
            }
        } else if (activities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingFlat,
                            contentDescription = "Timeline empty",
                            tint = AuraWhiteMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No activities logged today. Check off some tasks, write notes, or log money transactions to flow chart them here!",
                            fontSize = 12.sp,
                            color = AuraWhiteMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Flowchart timeline renderer
            items(activities) { act ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // LEFT COLUMN: FLOW CHART NODE & CONNECTOR LINE
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Vertical Flow connector
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(AuraSlateLight.copy(alpha = 0.4f))
                        )

                        // Circular Node symbol
                        val nodeColor = when (act.type) {
                            "NOTE" -> AuraCyanNeon
                            "TASK" -> AuraPurpleAccent
                            "TRANSACTION" -> MoodHappy
                            else -> Color.White
                        }
                        val nodeIcon = when (act.type) {
                            "NOTE" -> Icons.Default.Notes
                            "TASK" -> if (act.isDone) Icons.Default.TaskAlt else Icons.Default.PendingActions
                            "TRANSACTION" -> Icons.Default.Payments
                            else -> Icons.Default.Grain
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(AuraObsidian, CircleShape)
                                .border(2.dp, nodeColor, CircleShape)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = nodeIcon,
                                contentDescription = act.type,
                                tint = nodeColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // RIGHT COLUMN: DETAILED FLOW CARD WITH THEMATIC CORNERS
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 12.dp)
                            .border(1.dp, AuraSlateLight.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = act.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                    textDecoration = if (act.isDone) TextDecoration.LineThrough else null
                                )
                                Text(
                                    text = act.time.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraCyanNeon,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = act.description,
                                fontSize = 11.sp,
                                color = AuraWhiteMedium
                            )

                            if (!act.extraInfo.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(AuraSlateLight, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = act.extraInfo,
                                            fontSize = 8.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Safety Bottom spacing
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// Fluent Layout Modifier Utilities
private fun Modifier.fillModifierCompact(): Modifier = this.fillMaxWidth()
