package ocd.phonetricks.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ocd.phonetricks.data.RotationVector

/**
 * Platform-specific 3D phone visualization
 * Android: Uses SceneView with Google Filament for 3D rendering
 * iOS: Would use SceneKit or similar
 */
@Composable
expect fun PhoneVisualization3D(
    rotationVector: RotationVector,
    modifier: Modifier = Modifier,
)
