package dev.sadakat.qit.presentation.appearance

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.domain.model.UiStyle
import dev.sadakat.qit.core.ui.kit.QItScaffold
import dev.sadakat.qit.core.ui.kit.QItSegmentedToggle
import dev.sadakat.qit.core.ui.kit.QItSwitchRow
import dev.sadakat.qit.core.ui.kit.QItTopBar
import dev.sadakat.qit.presentation.settings.labelRes
import dev.sadakat.qit.ui.theme.toQItStyle

@Composable
fun AppearanceRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: AppearanceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppearanceScreen(
        state = state,
        // Wallpaper colors exist only where Material You does.
        showDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        onStyleChange = viewModel::setStyle,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColor,
        onBack = onBack,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

/**
 * How the app looks: the style, each shown as a live miniature of the app in it; the page tone;
 * and wallpaper colors. A choice applies at once, so the screen itself restyles as the user picks.
 */
@Composable
fun AppearanceScreen(
    state: AppearanceUiState,
    showDynamicColor: Boolean,
    onStyleChange: (UiStyle) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val tone = state.themeMode.tone(isSystemInDarkTheme())
    QItScaffold(
        topBar = {
            QItTopBar(
                title = { Text(stringResource(R.string.appearance_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
        contentPadding = contentPadding,
        modifier = modifier,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = QItTheme.spacing.screenGutter)
                .testTag("appearance"),
        ) {
            SectionLabel(stringResource(R.string.appearance_style))
            Text(
                text = stringResource(R.string.appearance_style_supporting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = QItTheme.spacing.md),
            )
            StyleGrid(selected = state.style, tone = tone, dynamicColor = state.dynamicColor, onSelect = onStyleChange)

            SectionLabel(stringResource(R.string.appearance_page))
            QItSegmentedToggle(
                options = ThemeMode.entries,
                selected = state.themeMode,
                onSelect = onThemeModeChange,
                label = { stringResource(it.labelRes()) },
            )
            if (showDynamicColor) {
                Spacer(Modifier.height(QItTheme.spacing.sm))
                QItSwitchRow(
                    title = stringResource(R.string.wallpaper_colors),
                    supporting = stringResource(
                        if (state.themeMode == ThemeMode.SEPIA) {
                            R.string.wallpaper_colors_sepia_supporting
                        } else {
                            R.string.wallpaper_colors_supporting
                        },
                    ),
                    checked = state.dynamicColor,
                    onCheckedChange = onDynamicColorChange,
                )
            }
            Spacer(Modifier.height(QItTheme.spacing.xl))
        }
    }
}

/** The four styles, two by two; one of them is chosen, like radio buttons. */
@Composable
private fun StyleGrid(selected: UiStyle, tone: QItTone, dynamicColor: Boolean, onSelect: (UiStyle) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(QItTheme.spacing.md),
        modifier = Modifier.selectableGroup(),
    ) {
        UiStyle.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(QItTheme.spacing.md)) {
                pair.forEach { style ->
                    StyleCard(
                        style = style,
                        selected = style == selected,
                        tone = tone,
                        dynamicColor = dynamicColor,
                        onClick = { onSelect(style) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleCard(
    style: UiStyle,
    selected: Boolean,
    tone: QItTone,
    dynamicColor: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (selected) QItTheme.sizes.strokeThin else QItTheme.sizes.ornamentStroke / 2,
                color = if (selected) scheme.primary else scheme.outlineVariant,
                shape = shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(QItTheme.spacing.sm)
            .testTag("style_${style.name.lowercase()}"),
    ) {
        StylePreview(
            style = style.toQItStyle(),
            tone = tone,
            dynamicColor = dynamicColor,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PREVIEW_ASPECT)
                .clip(MaterialTheme.shapes.medium),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = QItTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(style.nameRes()),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(QItTheme.sizes.iconSmall),
                )
            }
        }
        Text(
            text = stringResource(style.supportingRes()),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = QItTheme.spacing.lg, bottom = QItTheme.spacing.xs)
            .semantics { heading() },
    )
}

/** The page tone this setting gives, with the system's light or dark for [ThemeMode.SYSTEM]. */
fun ThemeMode.tone(systemDark: Boolean): QItTone = when (this) {
    ThemeMode.SYSTEM -> if (systemDark) QItTone.DARK else QItTone.LIGHT
    ThemeMode.LIGHT -> QItTone.LIGHT
    ThemeMode.SEPIA -> QItTone.SEPIA
    ThemeMode.DARK -> QItTone.DARK
}

private fun UiStyle.nameRes(): Int = when (this) {
    UiStyle.MUSHAF -> R.string.style_mushaf
    UiStyle.MATERIAL -> R.string.style_material
    UiStyle.EXPRESSIVE -> R.string.style_expressive
    UiStyle.GLASS -> R.string.style_glass
}

private fun UiStyle.supportingRes(): Int = when (this) {
    UiStyle.MUSHAF -> R.string.style_mushaf_supporting
    UiStyle.MATERIAL -> R.string.style_material_supporting
    UiStyle.EXPRESSIVE -> R.string.style_expressive_supporting
    UiStyle.GLASS -> R.string.style_glass_supporting
}

/** A phone screen's proportions, a little squatter. */
private const val PREVIEW_ASPECT = 0.62f
