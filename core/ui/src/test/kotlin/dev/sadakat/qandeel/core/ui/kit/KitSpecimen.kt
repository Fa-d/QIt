package dev.sadakat.qandeel.core.ui.kit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.core.designsystem.shape.shape

/**
 * Every kit component in one screen, the way the app composes them: a large top bar over a list
 * that scrolls behind it, a card, a search field, a segmented toggle, rows with number badges, a
 * floating chip and the bottom bar. Glass shows here what it does over real content.
 */
@Composable
internal fun KitSpecimen() {
    QandeelAppShell(bottomBar = { SpecimenBottomBar() }) { shellPadding ->
        QandeelScaffold(
            topBar = {
                QandeelTopBar(
                    title = { Text("Quran") },
                    size = QandeelTopBarSize.LARGE,
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
                    },
                )
            },
            contentPadding = shellPadding,
            overlay = { padding ->
                QandeelFloatingChip(
                    onClick = {},
                    label = { Text("Back to the reciting ayah") },
                    icon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(padding)
                        .padding(bottom = QandeelTheme.spacing.md),
                )
            },
        ) { padding ->
            SpecimenList(padding)
        }
    }
}

@Composable
private fun SpecimenList(padding: PaddingValues) {
    val gutter = QandeelTheme.spacing.screenGutter
    LazyColumn(contentPadding = padding) {
        item {
            QandeelCard(
                emphasis = QandeelEmphasis.PRIMARY,
                modifier = Modifier.padding(gutter, QandeelTheme.spacing.sm),
            ) {
                Column(Modifier.padding(QandeelTheme.spacing.lg)) {
                    Text("Continue listening", style = MaterialTheme.typography.labelLarge)
                    Text("Al-Kahf 18:10", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        item {
            QandeelSearchField(
                query = "",
                onQueryChange = {},
                placeholder = "Surah, number or 2:255",
                clearLabel = "Clear",
                modifier = Modifier.padding(gutter, QandeelTheme.spacing.sm),
            )
        }
        item {
            QandeelSegmentedToggle(
                options = listOf("Surahs", "Juz"),
                selected = "Surahs",
                onSelect = {},
                label = { it },
                modifier = Modifier.padding(gutter, QandeelTheme.spacing.sm),
            )
        }
        items(SpecimenSurahs.size) { index ->
            SpecimenRow(index + 1, SpecimenSurahs[index])
        }
    }
}

@Composable
private fun SpecimenRow(number: Int, name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QandeelTheme.spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .height(QandeelTheme.sizes.touchTarget + QandeelTheme.spacing.lg)
            .padding(horizontal = QandeelTheme.spacing.screenGutter),
    ) {
        Badge(number)
        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text("${number * 7} ayahs", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Badge(number: Int) {
    val badge = QandeelTheme.badge
    val colors = QandeelTheme.colors
    QandeelSurface(
        shape = badge.shape.shape,
        color = if (badge.filled) colors.secondaryContainer else colors.surface.copy(alpha = 0f),
        modifier = Modifier.height(QandeelTheme.sizes.numberBadge),
    ) {
        Box(Modifier.padding(QandeelTheme.spacing.sm), contentAlignment = Alignment.Center) {
            Text("$number", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SpecimenBottomBar() {
    QandeelBottomBar {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(QandeelTheme.sizes.touchTarget + QandeelTheme.spacing.lg)
                .padding(start = QandeelTheme.spacing.lg, end = QandeelTheme.spacing.xs),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Al-Fatihah 1:3", style = MaterialTheme.typography.titleSmall)
                Text("Ayah 3 of 7 · Arabic", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = {}) { Icon(Icons.Rounded.Pause, contentDescription = "Pause") }
        }
    }
}

private val SpecimenSurahs = listOf(
    "Al-Fatihah", "Al-Baqarah", "Ali 'Imran", "An-Nisa", "Al-Ma'idah", "Al-An'am", "Al-A'raf", "Al-Anfal",
    "At-Tawbah", "Yunus", "Hud", "Yusuf",
)
