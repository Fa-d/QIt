package dev.sadakat.qandeel.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.qandeel.R
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.core.domain.model.ArabicTextSize
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.model.ThemeMode
import dev.sadakat.qandeel.core.domain.model.WordByWord
import dev.sadakat.qandeel.core.ui.kit.QandeelRadioRow
import dev.sadakat.qandeel.core.ui.kit.QandeelSegmentedToggle
import dev.sadakat.qandeel.core.ui.kit.QandeelSheet
import dev.sadakat.qandeel.core.ui.kit.QandeelSwitchRow
import kotlin.math.roundToInt

/**
 * The reading-comfort sheet (Arabic size, translation, follow-along, the Bangla voice, word by word,
 * the page tone),
 * opened from the home screen and the reader. The rest of the look (the style, wallpaper colors)
 * lives in Appearance, one tap away; the credits and privacy note in About, at the bottom.
 */
@Composable
fun ReadingSettingsSheet(
    onDismiss: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReadingSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    QandeelSheet(onDismissRequest = onDismiss, modifier = modifier) {
        ReadingSettingsContent(
            state = state,
            onArabicTextSizeChange = viewModel::setArabicTextSize,
            onShowTranslationChange = viewModel::setShowTranslation,
            onFollowAlongChange = viewModel::setFollowAlong,
            onBanglaVoiceChange = viewModel::setBanglaVoice,
            onWordByWordChange = viewModel::setWordByWord,
            onThemeModeChange = viewModel::setThemeMode,
            onOpenAppearance = onOpenAppearance,
            onOpenAbout = onOpenAbout,
        )
    }
}

/**
 * The sheet's stateless content: the Arabic text size with a live preview, the translation and
 * follow-along switches, the Bangla voice, the word-by-word language, the page tone, and the ways to
 * Appearance and About. Kept
 * separate from the sheet so tests and goldens render it without a window.
 */
@Composable
fun ReadingSettingsContent(
    state: ReadingSettingsUiState,
    onArabicTextSizeChange: (ArabicTextSize) -> Unit,
    onShowTranslationChange: (Boolean) -> Unit,
    onFollowAlongChange: (Boolean) -> Unit,
    onBanglaVoiceChange: (BanglaVoice) -> Unit,
    onWordByWordChange: (WordByWord) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = state.prefs
    val gutter = QandeelTheme.spacing.screenGutter
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.reading_settings_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(top = QandeelTheme.spacing.sm, bottom = QandeelTheme.spacing.sm, start = gutter, end = gutter)
                .semantics { heading() },
        )

        SectionLabel(textRes = R.string.arabic_text_size)
        ArabicSizeSlider(size = prefs.arabicTextSize, onChange = onArabicTextSizeChange)
        // The preview rescales live: the theme applies the very pref this slider writes.
        Text(
            text = stringResource(R.string.arabic_size_preview),
            style = QandeelTheme.arabic.body,
            color = QandeelTheme.colors.arabicText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = QandeelTheme.spacing.sm, bottom = QandeelTheme.spacing.md, start = gutter, end = gutter),
        )

        QandeelSwitchRow(
            title = stringResource(R.string.show_translation),
            checked = prefs.showTranslation,
            onCheckedChange = onShowTranslationChange,
        )
        QandeelSwitchRow(
            title = stringResource(R.string.follow_along),
            supporting = stringResource(R.string.follow_along_supporting),
            checked = prefs.followAlong,
            onCheckedChange = onFollowAlongChange,
        )

        SectionLabel(textRes = R.string.bangla_voice)
        Column(Modifier.selectableGroup()) {
            BanglaVoice.entries.forEach { voice ->
                QandeelRadioRow(
                    title = stringResource(voice.titleRes()),
                    supporting = stringResource(voice.supportingRes()),
                    selected = voice == state.voice,
                    onSelect = { onBanglaVoiceChange(voice) },
                )
            }
        }

        SectionLabel(textRes = R.string.word_by_word)
        Text(
            text = stringResource(R.string.word_by_word_supporting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = QandeelTheme.spacing.sm, start = gutter, end = gutter),
        )
        QandeelSegmentedToggle(
            options = WordByWord.entries,
            selected = prefs.wordByWord,
            onSelect = onWordByWordChange,
            label = { stringResource(it.labelRes()) },
            modifier = Modifier.padding(horizontal = gutter),
        )

        SectionLabel(textRes = R.string.appearance_page)
        QandeelSegmentedToggle(
            options = ThemeMode.entries,
            selected = prefs.themeMode,
            onSelect = onThemeModeChange,
            label = { stringResource(it.labelRes()) },
            modifier = Modifier.padding(horizontal = gutter),
        )
        TextButton(
            onClick = onOpenAppearance,
            modifier = Modifier.padding(start = QandeelTheme.spacing.sm, top = QandeelTheme.spacing.xs),
        ) {
            Icon(
                Icons.Rounded.Palette,
                contentDescription = null,
                modifier = Modifier.size(QandeelTheme.sizes.iconSmall),
            )
            Spacer(Modifier.size(QandeelTheme.spacing.sm))
            Text(stringResource(R.string.appearance_more))
        }

        // Where every voice, text and timing comes from (the word pointer's timings are CC BY).
        TextButton(
            onClick = onOpenAbout,
            modifier = Modifier.padding(start = QandeelTheme.spacing.sm, top = QandeelTheme.spacing.sm),
        ) {
            Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(QandeelTheme.sizes.iconSmall))
            Spacer(Modifier.size(QandeelTheme.spacing.sm))
            Text(stringResource(R.string.about_open))
        }
        Spacer(Modifier.height(QandeelTheme.spacing.xl))
    }
}

/**
 * Five sizes between a small and a large "A". A screen reader hears the slider as "Arabic text
 * size, Large": the size's name, not a position; the two A's are only a picture of the scale.
 */
@Composable
private fun ArabicSizeSlider(size: ArabicTextSize, onChange: (ArabicTextSize) -> Unit) {
    val label = stringResource(R.string.arabic_text_size)
    val sizeName = stringResource(size.labelRes())
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = QandeelTheme.spacing.screenGutter),
    ) {
        Text(
            text = stringResource(R.string.arabic_size_small),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Slider(
            value = size.ordinal.toFloat(),
            onValueChange = { value -> onChange(ArabicTextSize.entries[value.roundToInt()]) },
            valueRange = 0f..ArabicTextSize.entries.lastIndex.toFloat(),
            // Five positions, so three steps between them.
            steps = ArabicTextSize.entries.size - SLIDER_ENDS,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = QandeelTheme.spacing.sm)
                .semantics {
                    contentDescription = label
                    stateDescription = sizeName
                },
        )
        Text(
            text = stringResource(R.string.arabic_size_large),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {},
        )
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
                top = QandeelTheme.spacing.md,
                bottom = QandeelTheme.spacing.xs,
                start = QandeelTheme.spacing.screenGutter,
                end = QandeelTheme.spacing.screenGutter,
            )
            .semantics { heading() },
    )
}

private fun BanglaVoice.titleRes(): Int = when (this) {
    BanglaVoice.ISLAMIC_FOUNDATION -> R.string.bangla_voice_islamic_foundation
    BanglaVoice.SAYED_ISMAT_TOHA -> R.string.bangla_voice_toha
    BanglaVoice.SHAREEF_BAEZEED_MAHMOOD -> R.string.bangla_voice_baezeed
}

private fun BanglaVoice.supportingRes(): Int = when (this) {
    BanglaVoice.ISLAMIC_FOUNDATION -> R.string.bangla_voice_islamic_foundation_supporting
    BanglaVoice.SAYED_ISMAT_TOHA -> R.string.bangla_voice_toha_supporting
    BanglaVoice.SHAREEF_BAEZEED_MAHMOOD -> R.string.bangla_voice_baezeed_supporting
}

private fun WordByWord.labelRes(): Int = when (this) {
    WordByWord.OFF -> R.string.word_by_word_off
    WordByWord.ENGLISH -> R.string.word_by_word_english
    WordByWord.BANGLA -> R.string.word_by_word_bangla
}

private fun ArabicTextSize.labelRes(): Int = when (this) {
    ArabicTextSize.SMALL -> R.string.arabic_size_name_small
    ArabicTextSize.MEDIUM -> R.string.arabic_size_name_medium
    ArabicTextSize.LARGE -> R.string.arabic_size_name_large
    ArabicTextSize.XLARGE -> R.string.arabic_size_name_xlarge
    ArabicTextSize.XXLARGE -> R.string.arabic_size_name_xxlarge
}

private const val SLIDER_ENDS = 2
