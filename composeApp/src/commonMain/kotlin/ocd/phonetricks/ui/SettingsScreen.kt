package ocd.phonetricks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ocd.phonetricks.audio.Waveform
import ocd.phonetricks.data.ControlMapping
import ocd.phonetricks.data.ControlParameter
import ocd.phonetricks.data.ControlSurface

private val parameterTypes = listOf("Pitch", "Volume", "Waveform")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val mappings by settingsViewModel.mappings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Controls") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        settingsViewModel.addMapping(
                            ControlMapping(ControlSurface.TOUCH_X, ControlParameter.Pitch())
                        )
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add mapping")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            itemsIndexed(mappings) { index, mapping ->
                MappingCard(
                    mapping = mapping,
                    onSurfaceChange = { settingsViewModel.updateSurface(index, it) },
                    onParameterChange = { settingsViewModel.updateParameter(index, it) },
                    onRemove = { settingsViewModel.removeMapping(index) },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MappingCard(
    mapping: ControlMapping,
    onSurfaceChange: (ControlSurface) -> Unit,
    onParameterChange: (ControlParameter) -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = mapping.parameter::class.simpleName ?: "Control",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove")
                }
            }

            ControlSurfaceDropdown(selected = mapping.surface, onSelected = onSurfaceChange)

            HorizontalDivider()

            ParameterTypeDropdown(
                selected = mapping.parameter,
                onSelected = onParameterChange,
            )

            when (val p = mapping.parameter) {
                is ControlParameter.Pitch -> LinearFields(
                    outMin = p.min, outMax = p.max,
                    inputMin = p.inputMin, inputMax = p.inputMax,
                    onOutMinChange = { onParameterChange(p.copy(min = it)) },
                    onOutMaxChange = { onParameterChange(p.copy(max = it)) },
                    onInputMinChange = { onParameterChange(p.copy(inputMin = it)) },
                    onInputMaxChange = { onParameterChange(p.copy(inputMax = it)) },
                )
                is ControlParameter.Volume -> LinearFields(
                    outMin = p.min, outMax = p.max,
                    inputMin = p.inputMin, inputMax = p.inputMax,
                    onOutMinChange = { onParameterChange(p.copy(min = it)) },
                    onOutMaxChange = { onParameterChange(p.copy(max = it)) },
                    onInputMinChange = { onParameterChange(p.copy(inputMin = it)) },
                    onInputMaxChange = { onParameterChange(p.copy(inputMax = it)) },
                )
                is ControlParameter.Waveform -> WaveformFields(
                    config = p,
                    onStartChange = { onParameterChange(p.copy(startWaveform = it)) },
                    onEndChange = { onParameterChange(p.copy(endWaveform = it)) },
                    onInputMinChange = { onParameterChange(p.copy(inputMin = it)) },
                    onInputMaxChange = { onParameterChange(p.copy(inputMax = it)) },
                )
            }
        }
    }
}

@Composable
private fun LinearFields(
    outMin: Float, outMax: Float,
    inputMin: Float, inputMax: Float,
    onOutMinChange: (Float) -> Unit,
    onOutMaxChange: (Float) -> Unit,
    onInputMinChange: (Float) -> Unit,
    onInputMaxChange: (Float) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FloatField("Start", outMin, onOutMinChange, Modifier.weight(1f))
        FloatField("End", outMax, onOutMaxChange, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FloatField("In Min", inputMin, onInputMinChange, Modifier.weight(1f))
        FloatField("In Max", inputMax, onInputMaxChange, Modifier.weight(1f))
    }
}

@Composable
private fun WaveformFields(
    config: ControlParameter.Waveform,
    onStartChange: (Waveform) -> Unit,
    onEndChange: (Waveform) -> Unit,
    onInputMinChange: (Float) -> Unit,
    onInputMaxChange: (Float) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WaveformDropdown("Start", config.startWaveform, onStartChange, Modifier.weight(1f))
        WaveformDropdown("End", config.endWaveform, onEndChange, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FloatField("In Min", config.inputMin, onInputMinChange, Modifier.weight(1f))
        FloatField("In Max", config.inputMax, onInputMaxChange, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParameterTypeDropdown(
    selected: ControlParameter,
    onSelected: (ControlParameter) -> Unit,
) {
    val selectedName = selected::class.simpleName ?: ""
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Parameter") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            parameterTypes.forEach { typeName ->
                DropdownMenuItem(
                    text = { Text(typeName) },
                    onClick = {
                        val new: ControlParameter = when (typeName) {
                            "Pitch" -> ControlParameter.Pitch()
                            "Volume" -> ControlParameter.Volume()
                            else -> ControlParameter.Waveform()
                        }
                        onSelected(new)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlSurfaceDropdown(
    selected: ControlSurface,
    onSelected: (ControlSurface) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Control Surface") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ControlSurface.entries.forEach { surface ->
                DropdownMenuItem(
                    text = { Text(surface.name) },
                    onClick = { onSelected(surface); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaveformDropdown(
    label: String,
    selected: Waveform,
    onSelected: (Waveform) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Waveform.entries.forEach { waveform ->
                DropdownMenuItem(
                    text = { Text(waveform.name) },
                    onClick = { onSelected(waveform); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun FloatField(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf<String>(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { newText: String ->
            text = newText
            newText.toFloatOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier,
    )
}
