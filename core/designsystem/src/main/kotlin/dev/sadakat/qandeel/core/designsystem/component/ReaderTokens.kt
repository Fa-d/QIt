package dev.sadakat.qandeel.core.designsystem.component

import androidx.compose.ui.unit.dp

/** Reader layout values that aren't steps of a shared scale. */
object ReaderTokens {
    /** Lines of Arabic longer than this get hard to follow; wide screens center a column this wide. */
    val MaxReadingWidth = 720.dp

    /** A long word meaning wraps under its word rather than pushing the word-by-word row apart. */
    val WordMeaningMaxWidth = 112.dp
}
