package ocd.phonetricks.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SensorDataCard(
    title: String,
    xValue: Float,
    yValue: Float,
    zValue: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ValueDisplay("X", xValue, Color.Red)
                ValueDisplay("Y", yValue, Color.Green)
                ValueDisplay("Z", zValue, Color.Blue)
            }
        }
    }
}

@Composable
private fun ValueDisplay(axis: String, value: Float, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = axis,
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
        Text(
            text = "${(value * 100).toInt() / 100.0}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
