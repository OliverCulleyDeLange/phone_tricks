package ocd.phonetricks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ocd.phonetricks.data.TrickType
import ocd.phonetricks.training.BufferStats

@Composable
fun TrainingDataScreen(viewModel: TrainingDataViewModel) {
    val bufferStats by viewModel.bufferStats.collectAsState()
    val saveDirectory by viewModel.saveDirectory.collectAsState()
    val lastSavedFile by viewModel.lastSavedFile.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveCount by viewModel.saveCount.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Training Data Collection",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Collect sensor data samples for ML training",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📝 Instructions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "1. Tap the phone on the desired surface\n" +
                        "2. Immediately press the corresponding button\n" +
                        "3. Repeat 20-50 times per surface for best results\n" +
                        "4. Files are saved to: $saveDirectory",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Buffer Statistics
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Buffer Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Sample Count",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${bufferStats.sampleCount}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${bufferStats.durationMs / 1000.0}s",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "Sample Rate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(bufferStats.sampleRate * 10).toInt() / 10.0} Hz",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Samples saved: $saveCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Save Status
        if (lastSavedFile.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Column {
                        Text(
                            text = "Last saved:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = lastSavedFile,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Tap Type Buttons
        Text(
            text = "Tap Types",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Front/Back
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TapButton(
                label = "Front (Screen)",
                trickType = TrickType.TAP_FRONT,
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                onClick = { viewModel.saveSample(TrickType.TAP_FRONT) }
            )
            TapButton(
                label = "Back",
                trickType = TrickType.TAP_BACK,
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                onClick = { viewModel.saveSample(TrickType.TAP_BACK) }
            )
        }

        // Top/Bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TapButton(
                label = "Top",
                trickType = TrickType.TAP_TOP,
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                onClick = { viewModel.saveSample(TrickType.TAP_TOP) }
            )
            TapButton(
                label = "Bottom",
                trickType = TrickType.TAP_BOTTOM,
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                onClick = { viewModel.saveSample(TrickType.TAP_BOTTOM) }
            )
        }

        // Left/Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TapButton(
                label = "Left",
                trickType = TrickType.TAP_LEFT,
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                onClick = { viewModel.saveSample(TrickType.TAP_LEFT) }
            )
            TapButton(
                label = "Right",
                trickType = TrickType.TAP_RIGHT,
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                onClick = { viewModel.saveSample(TrickType.TAP_RIGHT) }
            )
        }

        // Clear Buffer Button
        Button(
            onClick = { viewModel.clearBuffer() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear Buffer")
        }
    }
}

@Composable
fun TapButton(
    label: String,
    trickType: TrickType,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                when (trickType) {
                    TrickType.TAP_FRONT -> Icons.Default.CropSquare
                    TrickType.TAP_BACK -> Icons.Default.BackHand
                    TrickType.TAP_TOP -> Icons.Default.ArrowUpward
                    TrickType.TAP_BOTTOM -> Icons.Default.ArrowDownward
                    TrickType.TAP_LEFT -> Icons.Default.ArrowBack
                    TrickType.TAP_RIGHT -> Icons.Default.ArrowForward
                    else -> Icons.Default.Circle
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
