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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import ocd.phonetricks.audio.AudioEffect
import ocd.phonetricks.audio.FilterPreset
import ocd.phonetricks.audio.Waveform
import ocd.phonetricks.data.ControlMapping
import ocd.phonetricks.data.ControlParameter
import ocd.phonetricks.data.ControlSurface

private data class ParameterOption(val label: String, val create: () -> ControlParameter)

private val parameterOptions: List<ParameterOption> = buildList {
    add(ParameterOption("Pitch") { ControlParameter.Pitch() })
    add(ParameterOption("Volume") { ControlParameter.Volume() })
    add(ParameterOption("Waveform") { ControlParameter.Waveform() })
    AudioEffect.entries.forEach { effect ->
        add(ParameterOption(effect.displayName()) { ControlParameter.EffectWetDry(effect) })
    }
    FilterPreset.entries.forEach { preset ->
        add(ParameterOption("${preset.displayName()} Freq") { ControlParameter.FilterFrequency(preset) })
        add(ParameterOption("${preset.displayName()} Mix") { ControlParameter.FilterWetDry(preset) })
    }
}

private fun ControlParameter.displayLabel(): String = when (this) {
    is ControlParameter.Pitch -> "Pitch"
    is ControlParameter.Volume -> "Volume"
    is ControlParameter.Waveform -> "Waveform"
    is ControlParameter.EffectWetDry -> effect.displayName()
    is ControlParameter.FilterFrequency -> "${preset.displayName()} Freq"
    is ControlParameter.FilterWetDry -> "${preset.displayName()} Mix"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheetContent(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val mappings by settingsViewModel.mappings.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Control Mappings", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = {
                settingsViewModel.addMapping(
                    ControlMapping(ControlSurface.TOUCH_X, ControlParameter.Pitch())
                )
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add mapping")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    text = mapping.parameter.displayLabel(),
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
                is ControlParameter.Pitch -> PitchFields(
                    config = p,
                    onOutMinChange = { onParameterChange(p.copy(min = it)) },
                    onOutMaxChange = { onParameterChange(p.copy(max = it)) },
                    onInputMinChange = { onParameterChange(p.copy(inputMin = it)) },
                    onInputMaxChange = { onParameterChange(p.copy(inputMax = it)) },
                    onSnapToScaleChange = { onParameterChange(p.copy(snapToScale = it)) },
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
                is ControlParameter.EffectWetDry -> EffectWetDryFields(
                    config = p,
                    onEffectChange = { onParameterChange(p.copy(effect = it)) },
                    onInputMinChange = { onParameterChange(p.copy(inputMin = it)) },
                    onInputMaxChange = { onParameterChange(p.copy(inputMax = it)) },
                )
                is ControlParameter.FilterFrequency -> FilterFrequencyFields(
                    config = p,
                    onPresetChange = { onParameterChange(p.copy(preset = it)) },
                    onOutMinChange = { onParameterChange(p.copy(min = it)) },
                    onOutMaxChange = { onParameterChange(p.copy(max = it)) },
                    onInputMinChange = { onParameterChange(p.copy(inputMin = it)) },
                    onInputMaxChange = { onParameterChange(p.copy(inputMax = it)) },
                )
                is ControlParameter.FilterWetDry -> FilterWetDryFields(
                    config = p,
                    onPresetChange = { onParameterChange(p.copy(preset = it)) },
                    onInputMinChange = { onParameterChange(p.copy(inputMin = it)) },
                    onInputMaxChange = { onParameterChange(p.copy(inputMax = it)) },
                )
            }
        }
    }
}

@Composable
private fun PitchFields(
    config: ControlParameter.Pitch,
    onOutMinChange: (Float) -> Unit,
    onOutMaxChange: (Float) -> Unit,
    onInputMinChange: (Float) -> Unit,
    onInputMaxChange: (Float) -> Unit,
    onSnapToScaleChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Snap to scale", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = config.snapToScale, onCheckedChange = onSnapToScaleChange)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FloatField("Start", config.min, onOutMinChange, Modifier.weight(1f))
        FloatField("End", config.max, onOutMaxChange, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FloatField("In Min", config.inputMin, onInputMinChange, Modifier.weight(1f))
        FloatField("In Max", config.inputMax, onInputMaxChange, Modifier.weight(1f))
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
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayLabel(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Parameter") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            parameterOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.create())
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
private fun EffectWetDryFields(
    config: ControlParameter.EffectWetDry,
    onEffectChange: (AudioEffect) -> Unit,
    onInputMinChange: (Float) -> Unit,
    onInputMaxChange: (Float) -> Unit,
) {
    AudioEffectDropdown(selected = config.effect, onSelected = onEffectChange)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FloatField("In Min", config.inputMin, onInputMinChange, Modifier.weight(1f))
        FloatField("In Max", config.inputMax, onInputMaxChange, Modifier.weight(1f))
    }
}

@Composable
private fun FilterFrequencyFields(
    config: ControlParameter.FilterFrequency,
    onPresetChange: (FilterPreset) -> Unit,
    onOutMinChange: (Float) -> Unit,
    onOutMaxChange: (Float) -> Unit,
    onInputMinChange: (Float) -> Unit,
    onInputMaxChange: (Float) -> Unit,
) {
    FilterPresetDropdown(selected = config.preset, onSelected = onPresetChange)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FloatField("Hz Min", config.min, onOutMinChange, Modifier.weight(1f))
        FloatField("Hz Max", config.max, onOutMaxChange, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FloatField("In Min", config.inputMin, onInputMinChange, Modifier.weight(1f))
        FloatField("In Max", config.inputMax, onInputMaxChange, Modifier.weight(1f))
    }
}

@Composable
private fun FilterWetDryFields(
    config: ControlParameter.FilterWetDry,
    onPresetChange: (FilterPreset) -> Unit,
    onInputMinChange: (Float) -> Unit,
    onInputMaxChange: (Float) -> Unit,
) {
    FilterPresetDropdown(selected = config.preset, onSelected = onPresetChange)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FloatField("In Min", config.inputMin, onInputMinChange, Modifier.weight(1f))
        FloatField("In Max", config.inputMax, onInputMaxChange, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioEffectDropdown(
    selected: AudioEffect,
    onSelected: (AudioEffect) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Effect") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AudioEffect.entries.forEach { effect ->
                DropdownMenuItem(
                    text = { Text(effect.displayName()) },
                    onClick = { onSelected(effect); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterPresetDropdown(
    selected: FilterPreset,
    onSelected: (FilterPreset) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Filter Preset") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FilterPreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.displayName()) },
                    onClick = { onSelected(preset); expanded = false },
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
