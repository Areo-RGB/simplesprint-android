package com.paul.simplesprint.features.saved_results.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paul.simplesprint.core.models.SavedRunCheckpointResult
import com.paul.simplesprint.core.models.SavedRunResult
import com.paul.simplesprint.formatElapsedTimerDisplay
import com.paul.simplesprint.ui.components.MetricDisplay
import com.paul.simplesprint.ui.components.SectionHeader

@Composable
internal fun SaveRunDetailsDialog(
    athleteName: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Details Result") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = athleteName,
                    onValueChange = onValueChange,
                    label = { Text("Athlete Name") },
                    singleLine = true,
                )
                Text(
                    text = "Saved name format: athlete_dd_MM_yyyy",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
internal fun SaveResultDialog(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Result") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("Name") },
                    singleLine = true,
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
internal fun SavedResultsDialog(
    rows: List<SavedRunResult>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (SavedRunResult) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Results") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("Time", modifier = Modifier.width(88.dp), fontWeight = FontWeight.SemiBold)
                    Text("Open", modifier = Modifier.width(60.dp), fontWeight = FontWeight.SemiBold)
                    Text("Delete", modifier = Modifier.width(60.dp), fontWeight = FontWeight.SemiBold)
                }
                if (rows.isEmpty()) {
                    Text("No saved results yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                } else {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = { onOpen(row) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(row.name)
                            }
                            Text(
                                formatElapsedTimerDisplay((row.durationNanos / 1_000_000L).coerceAtLeast(0L)),
                                modifier = Modifier.width(88.dp),
                            )
                            TextButton(
                                onClick = { onOpen(row) },
                                modifier = Modifier.width(60.dp),
                            ) {
                                Text("Open")
                            }
                            TextButton(
                                onClick = { onDelete(row.id) },
                                modifier = Modifier.width(60.dp),
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
internal fun SavedRunResultDetailsDialog(result: SavedRunResult, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(result.name, style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                SectionHeader("Calculated Results")
                if (result.checkpointResults.isEmpty()) {
                    Text(
                        text = "No calculated checkpoint cards saved for this result.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(result.checkpointResults) { checkpoint ->
                            SavedCheckpointResultCard(checkpoint = checkpoint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedCheckpointResultCard(checkpoint: SavedRunCheckpointResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "${distanceMetersLabel(checkpoint.distanceMeters)} • ${checkpoint.checkpointLabel}",
                style = MaterialTheme.typography.titleMedium,
            )
            MetricDisplay("Total Time", "${formatSeconds(checkpoint.totalTimeSec)} s")
            MetricDisplay("Split Time", "${formatSeconds(checkpoint.splitTimeSec)} s")
            MetricDisplay("Avg Speed", "${formatNumber(checkpoint.avgSpeedKmh)} km/h")
            MetricDisplay("Acceleration", "${formatNumber(checkpoint.accelerationMs2)} m/s²")
        }
    }
}

private fun formatSeconds(value: Double): String = formatNumber(value)

private fun distanceMetersLabel(value: Double): String {
    return if (value % 1.0 == 0.0) {
        "${value.toInt()}m"
    } else {
        "${formatNumber(value)}m"
    }
}

private fun formatNumber(value: Double): String = String.format("%.2f", value)
