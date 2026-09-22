package dev.sadakat.qit.presentation.settings

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.domain.model.ArabicTextSize
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.domain.model.WordByWord
import kotlin.math.roundToInt

/**
 * The reading-comfort sheet (Arabic size, translation, follow-along, word by word, theme), opened
 * from the home screen and the reader. Contract for the screens that host it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReadingSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        ReadingSettingsContent(
            state = state,
            // Wallpaper colors exist only where Material You does.
            showDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            onArabicTextSizeChange = viewModel::setArabicTextSize,
            onShowTranslationChange = viewModel::setShowTranslation,
            onFollowAlongChange = viewModel::setFollowAlong,
            onWordByWordChange = viewModel::setWordByWord,
            onThemeModeChange = viewModel::setThemeMode,
            onDynamicColorChange = viewModel::setDynamicColor,
        )
    }
}

/**
 * The sheet's stateless content: the Arabic text size with a live preview, the translation and
 * follow-along switches, the word-by-word language, the theme and (where supported) wallpaper
 * colors. Kept separate from the sheet so tests and goldens render it without a window.
 */
@Composable
fun ReadingSettingsContent(
    state: ReadingSettingsUiState,
    showDynamicColor: Boolean,
    onArabicTextSizeChange: (ArabicTextSize) -> Unit,
    onShowTranslationChange: (Boolean) -> Unit,
    onFollowAlongChange: (Boolean) -> Unit,
    onWordByWordChange: (WordByWord) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = state.prefs
    val arabicSizeLabel = stringResource(R.string.arabic_text_size)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.reading_settings_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(
                    top = QItTheme.spacing.sm,
                    bottom = QItTheme.spacing.sm,
                    start = QItTheme.spacing.screenGutter,
                    end = QItTheme.spacing.screenGutter,
                )
                .semantics { heading() },
        )

        SectionLabel(textRes = R.string.arabic_text_size)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = QItTheme.spacing.screenGutter,
                    end = QItTheme.spacing.screenGutter,
                ),
        ) {
            Text(
                text = stringResource(R.string.arabic_size_small),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = prefs.arabicTextSize.ordinal.toFloat(),
                onValueChange = { value ->
                    onArabicTextSizeChange(ArabicTextSize.entries[value.roundToInt()])
                },
                valueRange = 0f..ArabicTextSize.entries.lastIndex.toFloat(),
                // Five positions, so three steps between them.
                steps = ArabicTextSize.entries.size - SLIDER_ENDS,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = QItTheme.spacing.sm)
                    .semantics { contentDescription = arabicSizeLabel },
            )
            Text(
                text = stringResource(R.string.arabic_size_large),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The preview rescales live: the theme applies the very pref this slider writes.
        Text(
            text = stringResource(R.string.arabic_size_preview),
            style = QItTheme.arabic.body,
            color = QItTheme.colors.arabicText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = QItTheme.spacing.sm,
                    bottom = QItTheme.spacing.md,
                    start = QItTheme.spacing.screenGutter,
                    end = QItTheme.spacing.screenGutter,
                ),
        )

        SettingSwitchRow(
            title = stringResource(R.string.show_translation),
            checked = prefs.showTranslation,
            onCheckedChange = onShowTranslationChange,
        )
        SettingSwitchRow(
            title = stringResource(R.string.follow_along),
            supporting = stringResource(R.string.follow_along_supporting),
            checked = prefs.followAlong,
            onCheckedChange = onFollowAlongChange,
        )

        SectionLabel(textRes = R.string.word_by_word)
        Text(
            text = stringResource(R.string.word_by_word_supporting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                bottom = QItTheme.spacing.sm,
                start = QItTheme.spacing.screenGutter,
                end = QItTheme.spacing.screenGutter,
            ),
        )
        ChoiceRow(
            options = WordByWord.entries,
            selected = prefs.wordByWord,
            label = { it.labelRes() },
            onSelect = onWordByWordChange,
        )

        SectionLabel(textRes = R.string.theme)
        ChoiceRow(
            options = SheetThemeModes,
            selected = prefs.themeMode,
            label = { it.labelRes() },
            onSelect = onThemeModeChange,
        )

        if (showDynamicColor) {
            SettingSwitchRow(
                title = stringResource(R.string.wallpaper_colors),
                supporting = stringResource(R.string.wallpaper_colors_supporting),
                checked = prefs.dynamicColor,
                onCheckedChange = onDynamicColorChange,
            )
        }
        // The word pointer's timings are CC BY: credit them where the reading is set up.
        Text(
            text = stringResource(R.string.word_timings_credit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                top = QItTheme.spacing.lg,
                start = QItTheme.spacing.screenGutter,
                end = QItTheme.spacing.screenGutter,
            ),
        )
        Spacer(Modifier.height(QItTheme.spacing.xl))
    }
}

@Composable
private fun SectionLabel(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(
                top = QItTheme.spacing.md,
                bottom = QItTheme.spacing.xs,
                start = QItTheme.spacing.screenGutter,
                end = QItTheme.spacing.screenGutter,
            )
            .semantics { heading() },
    )
}

/** One choice among a few [options], as a row of segmented buttons labelled by [label]'s string. */
@Composable
private fun <T> ChoiceRow(options: List<T>, selected: T, label: (T) -> Int, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = QItTheme.spacing.screenGutter,
                end = QItTheme.spacing.screenGutter,
            ),
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = stringResource(label(option)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

/** A settings row that reads as one switch: the whole row toggles, the switch only shows state. */
@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supporting: String? = null,
) {
    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = supporting?.let { text ->
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier
            .heightIn(min = QItTheme.sizes.touchTarget)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    )
}

private fun WordByWord.labelRes(): Int = when (this) {
    WordByWord.OFF -> R.string.word_by_word_off
    WordByWord.ENGLISH -> R.string.word_by_word_english
    WordByWord.BANGLA -> R.string.word_by_word_bangla
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.SEPIA -> R.string.theme_sepia
    ThemeMode.DARK -> R.string.theme_dark
}

// Sepia is picked from Appearance until this sheet gets its page-tone row.
private val SheetThemeModes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)

private const val SLIDER_ENDS = 2
