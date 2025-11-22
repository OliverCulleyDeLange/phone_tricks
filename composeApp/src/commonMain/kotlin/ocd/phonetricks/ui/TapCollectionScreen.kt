package ocd.phonetricks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ocd.phonetricks.utils.currentTimeMillis

@Composable
fun TapCollectionScreen(viewModel: TapCollectionViewModel) {
    val isRecording by viewModel.isRecording.collectAsState()
    val tapTimestamps by viewModel.tapTimestamps.collectAsState()
    val timeRemainingMs by viewModel.timeRemainingMs.collectAsState()
    val savedSessionCount by viewModel.savedSessionCount.collectAsState()
    val lastSavedFile by viewModel.lastSavedFile.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

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
                detectTapGestures(
                    onTap = {
                        if (isRecording) {
                            viewModel.recordTap()
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Front Tap Collection",
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
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Taps: ${tapTimestamps.size}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )

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
                        text = "Tap anywhere on the screen",
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Start 10s Collection",
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
