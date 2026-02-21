package ocd.phonetricks

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ocd.phonetricks.audio.createAudioManager
import ocd.phonetricks.audio.createSamplePlayer
import ocd.phonetricks.data.SettingsRepository
import ocd.phonetricks.sensor.SensorManager
import ocd.phonetricks.ui.EqScreen
import ocd.phonetricks.ui.EqViewModel
import ocd.phonetricks.ui.FxScreen
import ocd.phonetricks.ui.FxViewModel
import ocd.phonetricks.ui.MainScreen
import ocd.phonetricks.ui.NoteSettingsScreen
import ocd.phonetricks.ui.NoteSettingsViewModel
import ocd.phonetricks.ui.SampleLooperScreen
import ocd.phonetricks.ui.SampleViewModel
import ocd.phonetricks.ui.SensorViewModel
import ocd.phonetricks.ui.SettingsSheetContent
import ocd.phonetricks.ui.SettingsViewModel
import ocd.phonetricks.ui.SynthesizerViewModel

private val AppColorScheme = darkColorScheme(
    primary = Color(0xFFCE93D8),
    onPrimary = Color(0xFF4A148C),
    primaryContainer = Color(0xFF6A1B9A),
    onPrimaryContainer = Color(0xFFF3E5F5),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF00695C),
    onSecondaryContainer = Color(0xFFE0F2F1),
    tertiary = Color(0xFFB39DDB),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(sensorManager: SensorManager, settingsRepository: SettingsRepository) {
    MaterialTheme(colorScheme = AppColorScheme) {
        val sensorViewModel = remember { SensorViewModel(sensorManager) }
        val audioManager = remember { createAudioManager() }
        val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
        val noteSettingsViewModel = remember { NoteSettingsViewModel(settingsRepository) }
        val synthesizerViewModel = remember { SynthesizerViewModel(sensorManager, audioManager, settingsViewModel, noteSettingsViewModel) }
        val fxViewModel = remember { FxViewModel(audioManager, settingsRepository) }
        val eqViewModel = remember { EqViewModel(audioManager, settingsRepository) }
        val sampleViewModel = remember { SampleViewModel(createSamplePlayer()) }

        var showSettings by remember { mutableStateOf(false) }
        var showFx by remember { mutableStateOf(false) }
        var showNoteSettings by remember { mutableStateOf(false) }
        var showEq by remember { mutableStateOf(false) }
        var showSampler by remember { mutableStateOf(false) }
        val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val fxSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val noteSettingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val eqSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val samplerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        MainScreen(
            sensorViewModel = sensorViewModel,
            synthesizerViewModel = synthesizerViewModel,
            eqViewModel = eqViewModel,
            sampleViewModel = sampleViewModel,
            onOpenSettings = { showSettings = true },
            onOpenFx = { showFx = true },
            onOpenNoteSettings = { showNoteSettings = true },
            onOpenEq = { showEq = true },
            onOpenSampler = { showSampler = true },
            modifier = Modifier.fillMaxSize(),
        )

        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = settingsSheetState,
            ) {
                SettingsSheetContent(
                    settingsViewModel = settingsViewModel,
                    modifier = Modifier.fillMaxHeight(0.85f),
                )
            }
        }

        if (showFx) {
            ModalBottomSheet(
                onDismissRequest = { showFx = false },
                sheetState = fxSheetState,
            ) {
                FxScreen(
                    fxViewModel = fxViewModel,
                    modifier = Modifier.fillMaxHeight(0.85f),
                )
            }
        }

        if (showNoteSettings) {
            ModalBottomSheet(
                onDismissRequest = { showNoteSettings = false },
                sheetState = noteSettingsSheetState,
            ) {
                NoteSettingsScreen(
                    viewModel = noteSettingsViewModel,
                    modifier = Modifier.fillMaxHeight(0.85f),
                )
            }
        }

        if (showEq) {
            ModalBottomSheet(
                onDismissRequest = { showEq = false },
                sheetState = eqSheetState,
            ) {
                EqScreen(
                    viewModel = eqViewModel,
                    modifier = Modifier.fillMaxHeight(0.85f),
                )
            }
        }

        if (showSampler) {
            ModalBottomSheet(
                onDismissRequest = { showSampler = false },
                sheetState = samplerSheetState,
            ) {
                SampleLooperScreen(
                    viewModel = sampleViewModel,
                    modifier = Modifier.fillMaxHeight(0.85f),
                )
            }
        }
    }
}
