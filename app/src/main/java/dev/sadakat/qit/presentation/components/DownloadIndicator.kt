package dev.sadakat.qit.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.domain.repository.SurahDownloadState

/** A surah's offline state at a glance: nothing, progress, downloaded, or failed. */
@Composable
fun DownloadIndicator(state: SurahDownloadState, modifier: Modifier = Modifier) {
    val size = QItTheme.sizes.iconSmall
    when (state) {
        is SurahDownloadState.Downloaded -> Icon(
            imageVector = Icons.Rounded.DownloadDone,
            contentDescription = stringResource(R.string.cd_downloaded),
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(size),
        )

        is SurahDownloadState.Downloading -> {
            val downloadingText = stringResource(R.string.cd_downloading)
            CircularProgressIndicator(
                progress = { state.progress },
                strokeWidth = QItTheme.sizes.strokeThin,
                trackColor = QItTheme.colors.progressTrack,
                modifier = modifier
                    .size(size)
                    .semantics { contentDescription = downloadingText },
            )
        }

        is SurahDownloadState.Failed -> Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = stringResource(R.string.cd_download_failed),
            tint = MaterialTheme.colorScheme.error,
            modifier = modifier.size(size),
        )

        is SurahDownloadState.NotDownloaded -> Unit
    }
}
