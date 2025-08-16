package com.chui.pos.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chui.pos.dtos.HeldOrderResponse

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeldOrdersDialog(
    heldOrders: List<HeldOrderResponse>,
    onDismiss: () -> Unit,
    onResume: (HeldOrderResponse) -> Unit,
    onDelete: (Long) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.width(600.dp).heightIn(max = 500.dp)) {
            Column {
                Text(
                    "Held Orders",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                if (heldOrders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No orders are currently on hold.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(heldOrders, key = { it.id }) { order ->
                            // 1. Calculate the total for the order
                            val total = order.items.sumOf { it.price * it.quantity }

                            ListItem(
                                // 2. Add the customer name to the overline
                                overlineContent = { Text(order.customerName) },
                                headlineContent = { Text(order.ref, fontWeight = FontWeight.Bold) },
                                supportingContent = {
                                    // 3. Display the calculated total and other info
                                    Text("Total: %.2f | Items: %d | Held on: %s".format(total, order.items.size, order.createdAt ?: "N/A"))
                                },
                                trailingContent = {
                                    TooltipArea(tooltip = { Text("Delete Order") }) {
                                        IconButton(onClick = { onDelete(order.id) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Order",
                                                tint = MaterialTheme.colorScheme.error // Use error color for destructive actions
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.clickable { onResume(order) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}