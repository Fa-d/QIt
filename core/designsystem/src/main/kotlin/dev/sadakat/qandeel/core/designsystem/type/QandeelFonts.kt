package dev.sadakat.qandeel.core.designsystem.type

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.sadakat.qandeel.core.designsystem.R

/** The app's fonts; the font files live in this module only. */
object QandeelFonts {
    /** Amiri Quran (SIL OFL): an Uthmani-style naskh that carries the Quranic marks properly. */
    val AmiriQuran: FontFamily = FontFamily(Font(R.font.amiri_quran))

    /** Latin headings: the platform serif, for a printed-page feel next to the Arabic. */
    val Heading: FontFamily = FontFamily.Serif

    /** Latin body and labels: the platform sans (nothing to download, instant). */
    val Body: FontFamily = FontFamily.Default
}
