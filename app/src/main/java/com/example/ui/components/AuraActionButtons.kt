package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.anim.AuraCornerRadius
import com.example.ui.anim.auraSpringPress

/**
 * Primary filled pill action button with spring feedback and haptics.
 */
@Composable
fun AuraPrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val shape = RoundedCornerShape(50.dp) // Pill shape

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (enabled) {
                    Modifier.auraSpringPress(cornerRadius = 50.dp, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .background(
                if (enabled) containerColor else containerColor.copy(alpha = 0.4f)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Secondary dashed-outline pill action button with spring feedback and haptics.
 * Used side-by-side with AuraPrimaryAction.
 */
@Composable
fun AuraSecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    outlineColor: Color = MaterialTheme.colorScheme.outline,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val shape = RoundedCornerShape(50.dp) // Pill shape

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (enabled) {
                    Modifier.auraSpringPress(cornerRadius = 50.dp, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 1.5.dp,
                color = if (enabled) outlineColor else outlineColor.copy(alpha = 0.3f),
                shape = shape
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Helper composable for standard paired action buttons (Primary + Secondary side by side).
 */
@Composable
fun AuraActionPair(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryIcon: ImageVector? = null,
    secondaryIcon: ImageVector? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuraSecondaryAction(
            text = secondaryText,
            onClick = onSecondaryClick,
            icon = secondaryIcon,
            modifier = Modifier.weight(1f)
        )
        AuraPrimaryAction(
            text = primaryText,
            onClick = onPrimaryClick,
            icon = primaryIcon,
            modifier = Modifier.weight(1f)
        )
    }
}
