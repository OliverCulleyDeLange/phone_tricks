package ocd.phonetricks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ocd.phonetricks.utils.currentTimeMillis

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TapCollectionScreen(viewModel: TapCollectionViewModel) {
    val isRecording by viewModel.isRecording.collectAsState()
    val tapTimestamps by viewModel.tapTimestamps.collectAsState()
    val timeRemainingMs by viewModel.timeRemainingMs.collectAsState()
    val savedSessionCount by viewModel.savedSessionCount.collectAsState()
    val lastSavedFile by viewModel.lastSavedFile.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val sessionTag by viewModel.sessionTag.collectAsState()
    val selectedSurfaceTags by viewModel.selectedSurfaceTags.collectAsState()
    val selectedTapTags by viewModel.selectedTapTags.collectAsState()
    val collectionMode by viewModel.collectionMode.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isRecording)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.surface
            )
            .pointerInput(isRecording) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (isRecording && event.type == PointerEventType.Press) {
                            viewModel.recordTap()
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            if (!isRecording) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = collectionMode == CollectionMode.POSITIVE,
                            onClick = { viewModel.setCollectionMode(CollectionMode.POSITIVE) },
                            label = { Text("Positive (Taps)") },
                            modifier = Modifier.weight(1f),
                            enabled = !isRecording
                        )
                        FilterChip(
                            selected = collectionMode == CollectionMode.NEGATIVE,
                            onClick = { viewModel.setCollectionMode(CollectionMode.NEGATIVE) },
                            label = { Text("Negative (No Taps)") },
                            modifier = Modifier.weight(1f),
                            enabled = !isRecording
                        )
                    }

                    TextField(
                        value = sessionTag,
                        onValueChange = { viewModel.updateSessionTag(it) },
                        label = { Text("Session Tag") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter session tag") },
                        singleLine = true,
                        enabled = !isRecording
                    )

                    if (collectionMode == CollectionMode.POSITIVE) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Surface:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.surfaceTags.forEach { tag ->
                                    FilterChip(
                                        selected = selectedSurfaceTags.contains(tag),
                                        onClick = { viewModel.toggleSurfaceTag(tag) },
                                        label = { Text(tag) },
                                        enabled = !isRecording
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Taps:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.tapTags.forEach { tag ->
                                    FilterChip(
                                        selected = selectedTapTags.contains(tag),
                                        onClick = { viewModel.toggleTapTag(tag) },
                                        label = { Text(tag) },
                                        enabled = !isRecording
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Activity Type:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.negativeSampleTags.forEach { tag ->
                                    FilterChip(
                                        selected = selectedTapTags.contains(tag),
                                        onClick = { viewModel.toggleTapTag(tag) },
                                        label = { Text(tag) },
                                        enabled = !isRecording
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = if (collectionMode == CollectionMode.NEGATIVE) "Negative Sample Collection" else "Tap Collection",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isRecording) {
                    val secondsRemaining = (timeRemainingMs / 100.0) / 10.0
                    val seconds = (timeRemainingMs / 1000).toInt()
                    val tenths = ((timeRemainingMs % 1000) / 100).toInt()
                    Text(
                        text = "$seconds.$tenths",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "seconds",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(
                        if (collectionMode == CollectionMode.NEGATIVE) Icons.Default.TouchApp else Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (collectionMode == CollectionMode.POSITIVE) {
                    Text(
                        text = "Taps: ${tapTimestamps.size}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                if (savedSessionCount > 0 && !isRecording) {
                    Text(
                        text = "✓ $savedSessionCount sessions saved",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isRecording) {
                    Text(
                        text = if (collectionMode == CollectionMode.NEGATIVE)
                            "Wave, move, or use the phone normally WITHOUT tapping"
                        else
                            "Tap anywhere on the screen",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                } else if (isSaving) {
                    CircularProgressIndicator()
                    Text(
                        text = "Saving...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                if (!isRecording && !isSaving) {
                    Button(
                        onClick = { viewModel.startRecording() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        enabled = sessionTag.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sessionTag.trim()
                                    .isNotEmpty()
                            ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Start 20s Collection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (tapTimestamps.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.clearSession() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }
}
