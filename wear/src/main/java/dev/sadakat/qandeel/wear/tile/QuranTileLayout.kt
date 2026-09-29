package dev.sadakat.qandeel.wear.tile

import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textDataCard
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.types.layoutString

/** The tile's words, resolved from resources by the service. */
data class TileLabels(
    val appName: String,
    val nothingYet: String,
    val open: String,
    val continueListening: String,
    val pause: String,
    val resume: String,
    /** "18:23" for a position. */
    val position: (surah: Int, ayah: Int) -> String,
    /** "Ayah 23 of 110". */
    val progress: (ayah: Int, ayahCount: Int) -> String,
)

/** What the tile's taps do: open the app, open it on Now playing (optionally resuming), or pause in place. */
data class TileClicks(val open: Clickable, val nowPlaying: Clickable, val resume: Clickable, val pause: Clickable)

/**
 * One glance and one tap: the surah, the position with its progress, and the edge button for the
 * next thing to do — continue, pause or resume.
 */
fun MaterialScope.quranTileLayout(state: TileState, labels: TileLabels, clicks: TileClicks): LayoutElement =
    when (state) {
        TileState.NothingYet -> primaryLayout(
            titleSlot = { text(labels.appName.layoutString) },
            mainSlot = { text(labels.nothingYet.layoutString, typography = Typography.BODY_MEDIUM, maxLines = 3) },
            bottomSlot = { textEdgeButton(onClick = clicks.open) { text(labels.open.layoutString) } },
        )

        is TileState.Continue -> listeningLayout(
            surahName = state.surahName,
            position = labels.position(state.ref.surah, state.ref.ayah),
            progress = labels.progress(state.ref.ayah, state.ayahCount),
            clicks = clicks,
            action = labels.continueListening to clicks.resume,
        )

        is TileState.Queued -> listeningLayout(
            surahName = state.surahName,
            position = labels.position(state.ref.surah, state.ref.ayah),
            progress = labels.progress(state.ref.ayah, state.ayahCount),
            clicks = clicks,
            action = if (state.isPlaying) labels.pause to clicks.pause else labels.resume to clicks.resume,
        )
    }

private fun MaterialScope.listeningLayout(
    surahName: String,
    position: String,
    progress: String,
    clicks: TileClicks,
    action: Pair<String, Clickable>,
): LayoutElement = primaryLayout(
    titleSlot = { text(surahName.layoutString) },
    mainSlot = {
        textDataCard(
            onClick = clicks.nowPlaying,
            title = { text(position.layoutString) },
            content = { text(progress.layoutString) },
            width = expand(),
            height = expand(),
            modifier = LayoutModifier.contentDescription("$surahName $position"),
        )
    },
    bottomSlot = {
        textEdgeButton(onClick = action.second) { text(action.first.layoutString) }
    },
)
