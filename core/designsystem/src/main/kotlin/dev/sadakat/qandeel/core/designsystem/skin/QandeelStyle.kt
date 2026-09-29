package dev.sadakat.qandeel.core.designsystem.skin

/**
 * The visual languages Qandeel can wear. A style changes the chrome (surfaces, shapes, type, motion,
 * the number badge), never the Quran's text: its font, size and ink stay the same in every style.
 */
enum class QandeelStyle {
    /** The brand: warm paper, deep green and gold, serif headings, the rub el hizb around numbers. */
    MUSHAF,

    /** Stock Material 3: the baseline palette, Material's shapes and the platform sans. */
    MATERIAL,

    /** Material 3 Expressive: bolder shapes, springy motion and emphasized type. */
    EXPRESSIVE,

    /** Frosted glass: translucent, blurred bars, player and sheets over a soft wash. */
    GLASS,
}

/** The page under the chrome: light, a warm low-glare sepia, or dark. */
enum class QandeelTone { LIGHT, SEPIA, DARK }
