package com.example.ui.anim

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.AppViewModel
import com.example.ui.AuraHaptics
import com.example.ui.Section
import com.example.ui.theme.*

data class NavItem(
    val section: Section,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val testTag: String? = null
)

@Composable
fun AuraAnimatedNavBar(
    viewModel: AppViewModel,
    active: Section,
    modifier: Modifier = Modifier
) {
    val navItems = remember {
        listOf(
            NavItem(Section.Dashboard, Icons.Default.Dashboard, "Workspace Dashboard"),
            NavItem(Section.Notes, Icons.Default.Feed, "Notebook tabs"),
            NavItem(Section.Tasks, Icons.Default.FormatListBulleted, "Tasks Kanban", "nav_tasks"),
            NavItem(Section.Habits, Icons.Default.LocalFireDepartment, "Habits check trackers"),
            NavItem(Section.Day, Icons.Default.CalendarToday, "Calendars diary logs", "nav_journal"),
            NavItem(Section.Money, Icons.Default.Payments, "Ledger and splits", "nav_money")
        )
    }

    val activeIndex = remember(active) {
        val idx = navItems.indexOfFirst { it.section == active }
        if (idx != -1) idx else 0
    }

    Surface(
        color = AuraCharcoalBase.copy(alpha = 0.95f),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 8.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / navItems.size
            val view = LocalView.current

            // Animated sliding neon capsule background indicator
            val targetX = tabWidth * activeIndex
            val animatedX by animateDpAsState(
                targetValue = targetX,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 350f
                ),
                label = "nav_capsule_x"
            )

            // Outer capsule highlighting active tab
            Box(
                modifier = Modifier
                    .offset(x = animatedX)
                    .width(tabWidth)
                    .height(44.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(AuraCornerRadius.Row),
                        ambientColor = AuraCyanNeon.copy(alpha = 0.2f),
                        spotColor = AuraCyanNeon.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(AuraCornerRadius.Row))
                    .background(AuraCyanNeon.copy(alpha = 0.12f))
            )

            // Interactive Tabs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = index == activeIndex

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = 500f
                        ),
                        label = "nav_icon_scale"
                    )

                    val tint = if (isSelected) AuraCyanNeon else AuraWhiteMuted

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(AuraCornerRadius.Row))
                            .clickable(
                                onClick = {
                                    if (!isSelected) {
                                        AuraHaptics.triggerSubtleTick(view)
                                        viewModel.navigateTo(item.section)
                                    }
                                }
                            )
                            .then(
                                if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.description,
                            tint = tint,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                    }
                }
            }
        }
    }
}
