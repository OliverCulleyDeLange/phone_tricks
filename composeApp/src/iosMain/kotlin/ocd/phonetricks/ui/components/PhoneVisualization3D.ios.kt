package ocd.phonetricks.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.SceneKit.SCNBox
import platform.SceneKit.SCNCamera
import platform.SceneKit.SCNMaterial
import platform.SceneKit.SCNNode
import platform.SceneKit.SCNScene
import platform.SceneKit.SCNVector3Make
import platform.SceneKit.SCNVector4Make
import platform.SceneKit.SCNView
import platform.UIKit.UIColor
import ocd.phonetricks.data.RotationVector

private const val PHONE_NODE_NAME = "phone-orientation-node"

/**
 * A lightweight SceneKit phone model driven directly by the tared sensor
 * quaternion. Keeping this native avoids shipping a second 3D asset/rendering
 * dependency on iOS while preserving the same orientation feedback as Android.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PhoneVisualization3D(
    rotationVector: RotationVector,
    modifier: Modifier,
) {
    UIKitView(
        modifier = modifier,
        factory = { createPhoneSceneView() },
        update = { sceneView ->
            val phoneNode = sceneView.scene
                ?.rootNode
                ?.childNodeWithName(PHONE_NODE_NAME, recursively = true)
                ?: return@UIKitView
            phoneNode.orientation = SCNVector4Make(
                rotationVector.x,
                rotationVector.y,
                rotationVector.z,
                rotationVector.scalar ?: computeScalar(
                    rotationVector.x,
                    rotationVector.y,
                    rotationVector.z,
                ),
            )
        },
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun createPhoneSceneView(): SCNView {
    val scene = SCNScene()

    val bodyMaterial = SCNMaterial().apply {
        diffuse.contents = UIColor.darkGrayColor
        metalness.contents = 0.55
        roughness.contents = 0.35
    }
    val screenMaterial = SCNMaterial().apply {
        diffuse.contents = UIColor.blackColor
        metalness.contents = 0.1
        roughness.contents = 0.2
    }

    val phoneNode = SCNNode().apply {
        name = PHONE_NODE_NAME
        geometry = SCNBox.boxWithWidth(
            width = 1.05,
            height = 2.1,
            length = 0.18,
            chamferRadius = 0.12,
        ).apply {
            firstMaterial = bodyMaterial
        }
    }

    val screenNode = SCNNode().apply {
        geometry = SCNBox.boxWithWidth(
            width = 0.92,
            height = 1.82,
            length = 0.025,
            chamferRadius = 0.07,
        ).apply {
            firstMaterial = screenMaterial
        }
        position = SCNVector3Make(0f, 0f, 0.102f)
    }
    phoneNode.addChildNode(screenNode)
    scene.rootNode.addChildNode(phoneNode)

    val cameraNode = SCNNode().apply {
        camera = SCNCamera()
        position = SCNVector3Make(0f, 0f, 4.2f)
    }
    scene.rootNode.addChildNode(cameraNode)

    return SCNView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
        this.scene = scene
        pointOfView = cameraNode
        backgroundColor = UIColor.clearColor
        autoenablesDefaultLighting = true
        allowsCameraControl = false
        playing = true
    }
}

private fun computeScalar(x: Float, y: Float, z: Float): Float {
    val vectorLengthSquared = x * x + y * y + z * z
    return if (vectorLengthSquared < 1f) {
        kotlin.math.sqrt(1f - vectorLengthSquared)
    } else {
        0f
    }
}
