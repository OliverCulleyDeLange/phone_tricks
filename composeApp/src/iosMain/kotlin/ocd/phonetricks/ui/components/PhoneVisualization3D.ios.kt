package ocd.phonetricks.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ocd.phonetricks.data.RotationVector

@Composable
actual fun PhoneVisualization3D(
    rotationVector: RotationVector,
    modifier: Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "iOS 3D Visualization\n(Not yet implemented)",
                color = Color.Gray
            )
        }
    }
}
