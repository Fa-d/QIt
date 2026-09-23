package dev.sadakat.qit.presentation.reader

import android.content.ClipData
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import dev.sadakat.qit.R
import kotlinx.coroutines.launch

/** Copies or shares an ayah of the reader's surah, as [ayahShareText] writes it. */
@Stable
class AyahSharing(val copy: (ayah: Int) -> Unit, val share: (ayah: Int) -> Unit)

/**
 * Copy and share for the ayahs of [state]'s surah. They need a Context, so they live in the UI, not
 * the ViewModel. [onCopy] confirms a copy where the system doesn't (Android 12L and older; newer
 * versions show their own confirmation).
 */
@Composable
fun rememberAyahSharing(state: SurahReaderUiState, onCopy: () -> Unit): AyahSharing {
    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(state)
    val currentOnCopied by rememberUpdatedState(onCopy)
    return remember(context, resources, clipboard, scope) {
        fun textOf(number: Int): String? {
            val surah = current.surah ?: return null
            val ayah = current.ayahs.firstOrNull { it.number == number } ?: return null
            val translation = current.mode.translation?.let(ayah::translation) ?: ayah.english
            val reference = resources.getString(R.string.ayah_share_reference, surah.nameEnglish, surah.number, number)
            return ayahShareText(ayah.arabic, translation, reference)
        }
        AyahSharing(
            copy = { number ->
                textOf(number)?.let { text ->
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(text, text))) }
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) currentOnCopied()
                }
            },
            share = { number ->
                textOf(number)?.let { text ->
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, null))
                }
            },
        )
    }
}

/** An ayah's long-press options as screen-reader actions, so they don't need a long press. */
@Composable
fun ayahAccessibilityActions(
    onRepeat: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
): List<CustomAccessibilityAction> {
    val repeat = stringResource(R.string.ayah_action_repeat)
    val copy = stringResource(R.string.ayah_action_copy)
    val share = stringResource(R.string.ayah_action_share)
    return listOf(
        CustomAccessibilityAction(repeat) { onRepeat().let { true } },
        CustomAccessibilityAction(copy) { onCopy().let { true } },
        CustomAccessibilityAction(share) { onShare().let { true } },
    )
}
