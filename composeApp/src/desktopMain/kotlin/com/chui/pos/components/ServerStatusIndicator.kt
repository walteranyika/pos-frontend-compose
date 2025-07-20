package com.chui.pos.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ServerStatusIndicator(isOnline: Boolean) {
    val color = if (isOnline) Color(0xFF388E3C) else MaterialTheme.colorScheme.error
    val icon = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff
    val text = if (isOnline) "Online" else "Offline"

    TooltipArea(tooltip = {
        Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp) {
            Text("Server is $text", modifier = Modifier.padding(8.dp))
        }
    }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = text, tint = color)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}