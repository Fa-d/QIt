package dev.sadakat.qit.presentation.settings

import androidx.annotation.StringRes
import dev.sadakat.qit.R
import dev.sadakat.qit.core.domain.model.ThemeMode

/** The label of a page tone, in the reading sheet and in Appearance. */
@StringRes
internal fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.SEPIA -> R.string.theme_sepia
    ThemeMode.DARK -> R.string.theme_dark
}
