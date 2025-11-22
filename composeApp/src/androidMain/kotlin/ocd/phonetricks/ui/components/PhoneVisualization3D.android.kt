package ocd.phonetricks.ui.components

import android.R.attr.x
import android.R.attr.y
import android.util.Log.w
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import ocd.phonetricks.data.RotationVector
import dev.romainguy.kotlin.math.Quaternion

@Composable
actual fun PhoneVisualization3D(
    rotationVector: RotationVector,
    modifier: Modifier,
    onTareRequest: (() -> Unit)?
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    val x = rotationVector.x
    val y = rotationVector.y
    val z = rotationVector.z
    val w = (rotationVector.scalar ?: computeScalar(x.toDouble(), y.toDouble(), z.toDouble()))

    // Create the rotation quaternion from sensor data (tare already applied in ViewModel)
    val sensorQuat = Quaternion(x = x, y = y, z = z, w = w)

    val parentNode = remember { Node(engine = engine) }
    var phoneNode by remember { mutableStateOf<ModelNode?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val modelInstance = modelLoader.createModelInstance(
                assetFileLocation = "models/smartphone.glb"
            )

            val newPhoneNode = ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 2.0f,
                centerOrigin = Position(0f, 0f, 0f)
            )

            parentNode.addChildNode(newPhoneNode)
            phoneNode = newPhoneNode
        } catch (e: Exception) {
            errorMessage = "Failed to load model: ${e.message}"
            e.printStackTrace()
        }
    }

    LaunchedEffect(sensorQuat) {
        parentNode.quaternion = sensorQuat
    }

    Box(modifier = modifier) {
        when {
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            phoneNode == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    cameraNode = rememberCameraNode(engine) {
                        position = Position(x = 0f, y = 0f, z = 4f)
                    },
                    childNodes = rememberNodes {
                        add(parentNode)
                    }
                )

                if (onTareRequest != null) {
                    Button(
                        onClick = onTareRequest,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, "Tare")
                    }
                }
            }
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
