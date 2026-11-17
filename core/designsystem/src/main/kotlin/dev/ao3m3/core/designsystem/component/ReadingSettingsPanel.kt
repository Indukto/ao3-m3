package dev.ao3m3.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.ao3m3.core.designsystem.theme.Ao3Theme
import dev.ao3m3.core.designsystem.theme.Spacing
import dev.ao3m3.core.model.ReadingFont
import dev.ao3m3.core.model.ReadingSettings
import dev.ao3m3.core.model.ReadingTheme
import kotlin.math.roundToInt

/**
 * The one reading-settings UI, shared by Settings and the reader sheet. Values are local while the
 * slider moves and committed when the finger lifts, so dragging does not write DataStore on every
 * pixel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingSettingsPanel(
    settings: ReadingSettings,
    onChange: (ReadingSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingLabel("Typeface")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ReadingFont.entries.forEach { font ->
                FilterChip(
                    selected = settings.font == font,
                    onClick = { onChange(settings.copy(font = font)) },
                    label = { Text(font.label) },
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        CommitSlider(
            label = "Text size",
            value = settings.fontSizeSp.toFloat(),
            range = 14f..28f,
            steps = 6,
            format = { "${it.roundToInt()} sp" },
        ) { onChange(settings.copy(fontSizeSp = it.roundToInt())) }

        Spacer(Modifier.height(Spacing.md))
        CommitSlider(
            label = "Line height",
            value = settings.lineHeight,
            range = 1.3f..1.9f,
            format = { "%.2f".format(it) },
        ) { onChange(settings.copy(lineHeight = it)) }

        Spacer(Modifier.height(Spacing.md))
        CommitSlider(
            label = "Margins",
            value = settings.marginDp.toFloat(),
            range = 12f..36f,
            steps = 3,
            format = { "${it.roundToInt()} dp" },
        ) { onChange(settings.copy(marginDp = it.roundToInt())) }

        Spacer(Modifier.height(Spacing.sm))
        ToggleLine("Justify text", settings.justify) {
            onChange(settings.copy(justify = it))
        }
        ToggleLine("Keep screen on", settings.keepScreenOn) {
            onChange(settings.copy(keepScreenOn = it))
        }

        Spacer(Modifier.height(Spacing.lg))
        SettingLabel("Paper")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ReadingTheme.entries.forEach { theme ->
                FilterChip(
                    selected = settings.theme == theme,
                    onClick = { onChange(settings.copy(theme = theme)) },
                    label = { Text(theme.label) },
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Spacing.sm),
    )
}

@Composable
private fun CommitSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    format: (Float) -> String,
    onCommit: (Float) -> Unit,
) {
    var local by remember(value) {
        mutableFloatStateOf(value.coerceIn(range.start, range.endInclusive))
    }
    Text(
        text = "$label · ${format(local)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Slider(
        value = local,
        onValueChange = { local = it },
        onValueChangeFinished = { onCommit(local) },
        valueRange = range,
        steps = steps,
    )
}

@Composable
private fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadingSettingsPanelPreview() {
    Ao3Theme {
        ReadingSettingsPanel(
            settings = ReadingSettings(),
            onChange = {},
        )
    }
}
