package dev.sadakat.qit.wear.presentation.nowplaying

// qit:legacy-ui — predates the design tokens; its UX slice replaces it.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import dev.sadakat.qit.core.designsystem.type.QItFonts
import dev.sadakat.qit.wear.R

@Composable
fun NowPlayingRoute(modifier: Modifier = Modifier, viewModel: NowPlayingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    NowPlayingScreen(
        uiState = uiState,
        onPrevious = viewModel::previousAyah,
        onTogglePlayPause = viewModel::togglePlayPause,
        onNext = viewModel::nextAyah,
        modifier = modifier,
    )
}

@Composable
fun NowPlayingScreen(
    uiState: NowPlayingViewModel.UiState,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Amiri carries the vowel marks of the Uthmani script properly.
    val arabicFont = QItFonts.AmiriQuran
    val previousDescription = stringResource(R.string.previous_ayah)
    val playPauseDescription = stringResource(R.string.play_pause)
    val pauseDescription = stringResource(R.string.pause)
    val nextDescription = stringResource(R.string.next_ayah)

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val surahNumber = uiState.surahNumber
            if (surahNumber == null) {
                Text(stringResource(R.string.nothing_playing), style = MaterialTheme.typography.title3)
            } else {
                Text(
                    text = uiState.surahName.orEmpty(),
                    style = MaterialTheme.typography.title3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                    if (uiState.ayah == 0) {
                        stringResource(R.string.bismillah)
                    } else {
                        stringResource(R.string.ayah_position, surahNumber, uiState.ayah)
                    },
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.secondary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = uiState.ayahText ?: stringResource(R.string.bismillah_arabic),
                    fontFamily = arabicFont,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                if (uiState.isBuffering) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption1,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(ButtonDefaults.DefaultButtonSize)
                        .semantics { contentDescription = previousDescription },
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = null)
                }
                Button(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(ButtonDefaults.LargeButtonSize)
                        .semantics {
                            contentDescription =
                                if (uiState.isPlaying) pauseDescription else playPauseDescription
                        },
                ) {
                    Icon(
                        if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                    )
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .size(ButtonDefaults.DefaultButtonSize)
                        .semantics { contentDescription = nextDescription },
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = null)
                }
            }
        }
        TimeText()
    }
}
