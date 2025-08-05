package com.chui.pos.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SessionExpiredDialog(onConfirm: () -> Unit) {
    AlertDialog(
        // Prevent dismissing by clicking outside or pressing the back button
        onDismissRequest = { },
        title = { Text("Session Expired") },
        text = { Text("Your session has expired. You will be logged out for security reasons. Please login again") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("OK")
            }
        }
    )
}