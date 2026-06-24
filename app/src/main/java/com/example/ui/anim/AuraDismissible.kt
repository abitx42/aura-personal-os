package com.example.ui.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.AuraHaptics
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun AuraDismissibleOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!visible) return

    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    
    // Vertical offset tracking
    val offsetY = remember { Animatable(0f) }
    
    // Dismiss threshold (in pixels or dp) - 140dp translated roughly
    val dismissThreshold = 350f

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetY.value > dismissThreshold) {
                                // Trigger dismissal haptics and trigger callback
                                AuraHaptics.triggerConfirm(view)
                                onDismiss()
                                offsetY.snapTo(0f)
                            } else {
                                // Dynamic elastic bounce-back
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetY.animateTo(0f)
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        // Dragging up is heavily restricted to prevent negative offsets
                        val newOffset = max(0f, offsetY.value + dragAmount)
                        coroutineScope.launch {
                            offsetY.snapTo(newOffset)
                        }
                    }
                )
            }
    ) {
        // Dynamic calculated properties based on drag distance
        val currentOffset = offsetY.value
        val dragFraction = min(1f, currentOffset / 600f)
        
        // Dim the black scrim overlay dynamically
        val scrimAlpha = max(0f, min(0.6f, 0.6f * (1f - dragFraction)))
        
        // Map drag fraction to scale (1.0f down to 0.92f)
        val currentScale = 1f - (dragFraction * 0.08f)
        
        // Map drag fraction to corner radius (0.dp up to 24.dp)
        val currentRadius = (dragFraction * 24f).dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, currentOffset.roundToInt()) }
                    .graphicsLayer {
                        scaleX = currentScale
                        scaleY = currentScale
                    }
                    .clip(RoundedCornerShape(currentRadius))
            ) {
                content()
            }
        }
    }
}
