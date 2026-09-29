package dev.sadakat.qit.presentation.about

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.sadakat.qit.BuildConfig
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.ui.kit.QItScaffold
import dev.sadakat.qit.core.ui.kit.QItTopBar

@Composable
fun AboutRoute(onBack: () -> Unit, modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues()) {
    AboutScreen(
        versionName = BuildConfig.VERSION_NAME,
        onBack = onBack,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

/** One source: who or what it is, and where it came from and under which terms. */
private data class Credit(@param:StringRes val title: Int, @param:StringRes val detail: Int)

private val recitation = listOf(
    Credit(R.string.credit_alafasy, R.string.credit_alafasy_detail),
    Credit(R.string.credit_walk, R.string.credit_walk_detail),
    Credit(R.string.credit_islamic_foundation, R.string.credit_islamic_foundation_detail),
    Credit(R.string.credit_toha, R.string.credit_toha_detail),
    Credit(R.string.credit_basit, R.string.credit_basit_detail),
    Credit(R.string.credit_baezeed, R.string.credit_baezeed_detail),
    Credit(R.string.credit_sudais, R.string.credit_sudais_detail),
)

private val text = listOf(
    Credit(R.string.credit_uthmani, R.string.credit_uthmani_detail),
    Credit(R.string.credit_sahih, R.string.credit_sahih_detail),
    Credit(R.string.credit_muhiuddin, R.string.credit_muhiuddin_detail),
)

private val words = listOf(
    Credit(R.string.credit_quran_align, R.string.credit_quran_align_detail),
    Credit(R.string.credit_quran_com, R.string.credit_quran_com_detail),
)

private val software = listOf(
    Credit(R.string.credit_amiri, R.string.credit_amiri_detail),
    Credit(R.string.credit_libraries, R.string.credit_libraries_detail),
)

/**
 * The app's version, then where every voice, text and timing it plays or shows comes from (the
 * CC BY word timings are credited here), and what it does with your data: nothing leaves the device.
 */
@Composable
fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    QItScaffold(
        topBar = {
            QItTopBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
        contentPadding = contentPadding,
        modifier = modifier,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = QItTheme.spacing.screenGutter)
                .testTag("about"),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = QItTheme.spacing.md),
            )
            Text(
                text = stringResource(R.string.about_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = QItTheme.spacing.sm),
            )

            CreditSection(R.string.about_section_recitation, recitation)
            CreditSection(R.string.about_section_text, text)
            CreditSection(R.string.about_section_words, words)
            CreditSection(R.string.about_section_software, software)

            SectionLabel(stringResource(R.string.about_section_privacy))
            Text(text = stringResource(R.string.about_privacy), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(QItTheme.spacing.xl))
        }
    }
}

@Composable
private fun CreditSection(@StringRes title: Int, credits: List<Credit>) {
    SectionLabel(stringResource(title))
    credits.forEach { credit ->
        Column(Modifier.padding(vertical = QItTheme.spacing.xs)) {
            Text(text = stringResource(credit.title), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(credit.detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = QItTheme.spacing.lg, bottom = QItTheme.spacing.xs)
            .semantics { heading() },
    )
}
