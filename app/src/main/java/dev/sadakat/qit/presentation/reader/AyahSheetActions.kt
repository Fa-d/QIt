package dev.sadakat.qit.presentation.reader

/** What can be done with a long-pressed ayah. */
class AyahSheetActions(
    val onPlay: () -> Unit,
    val onRepeat: () -> Unit,
    val onCopy: () -> Unit,
    val onShare: () -> Unit,
)
