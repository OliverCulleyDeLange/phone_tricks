package ocd.phonetricks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CubeNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import ocd.phonetricks.data.RotationVector
import dev.romainguy.kotlin.math.Quaternion

@Composable
actual fun PhoneVisualization3D(
    rotationVector: RotationVector,
    modifier: Modifier
) {
    // Filament 3D Engine
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    // Get quaternion components from rotation vector
    // Android coordinate system: X=right, Y=up, Z=forward (into screen)
    // Filament coordinate system: X=right, Y=up, Z=backward (out of screen)
    // We need to remap the coordinates to match Filament's system
    val androidX = rotationVector.x.toFloat()
    val androidY = rotationVector.y.toFloat()
    val androidZ = rotationVector.z.toFloat()
    val androidW = (rotationVector.scalar ?: computeScalar(androidX.toDouble(), androidY.toDouble(), androidZ.toDouble())).toFloat()

    // Convert from Android coordinate system to Filament coordinate system
    // Negate W to invert rotation direction, keep Z negated for coordinate system conversion
    val x = androidX
    val y = androidY
    val z = -androidZ
    val w = -androidW

    // Create the cube node once and remember it
    val cubeNode = remember {
        CubeNode(
            engine = engine,
            size = dev.romainguy.kotlin.math.Float3(
                x = 0.8f,   // Width
                y = 1.6f,  // Height (phone-like aspect ratio)
                z = 0.1f    // Thin like a phone
            ),
            materialInstance = materialLoader.createColorInstance(
                color = Color.LightGray,
                metallic = 0.8f,
                roughness = 0.2f,
                reflectance = 0.5f
            )
        )
    }

    // Update the cube's rotation whenever the quaternion values change
    LaunchedEffect(x, y, z, w) {
        cubeNode.quaternion = dev.romainguy.kotlin.math.Quaternion(
            x = x,
            y = y,
            z = z,
            w = w
        )
    }

    Box(modifier = modifier) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,

            // Camera setup
            cameraNode = rememberCameraNode(engine) {
                position = Position(x = 0f, y = 0f, z = 4f)
            },

            // Create a box to represent the phone (aspect ratio ~2:1 for phone shape)
            childNodes = rememberNodes {
                add(cubeNode)
            }
        )

        // Debug overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(8.dp)
        ) {
            Text(
                text = "Quat: x=${x.format(2)} y=${y.format(2)} z=${z.format(2)} w=${w.format(2)}",
                color = Color.White,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Compute the scalar (w) component if not provided
 */
private fun computeScalar(x: Double, y: Double, z: Double): Float {
    val sumSquares = x * x + y * y + z * z
    return if (sumSquares < 1.0) {
        kotlin.math.sqrt(1.0 - sumSquares).toFloat()
    } else {
        0f
    }
}

private fun Float.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}
