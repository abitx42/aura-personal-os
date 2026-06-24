package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.example.ui.theme.AuraSlateCard

@Composable
fun AuraSectionInfoButton(
    viewModel: AppViewModel,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    IconButton(
        onClick = {
            AuraHaptics.triggerSubtleTick(view)
            viewModel.showInfoSheet(title, description)
        },
        modifier = modifier.background(AuraSlateCard.copy(alpha = 0.8f), CircleShape)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "About $title Section",
            tint = Color.White
        )
    }
}
