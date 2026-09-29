package dev.sadakat.qandeel.presentation.reader

/**
 * An ayah as text to copy or share: the Arabic, its translation when there is one, and where it's
 * from ([reference], e.g. "— Al-Baqarah 2:255"), each a paragraph of its own.
 */
fun ayahShareText(arabic: String, translation: String?, reference: String): String =
    listOfNotNull(arabic.trim(), translation?.trim()?.takeIf { it.isNotEmpty() }, reference).joinToString("\n\n")
