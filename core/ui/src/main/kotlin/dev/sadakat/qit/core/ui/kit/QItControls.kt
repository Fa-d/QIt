package dev.sadakat.qit.core.ui.kit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.designsystem.component.KitTokens
import dev.sadakat.qit.core.ui.kit.glass.LocalQItBackdrop
import dev.sadakat.qit.core.ui.kit.glass.QItSurfaceRole
import dev.sadakat.qit.core.ui.kit.glass.glassLook
import dev.sadakat.qit.core.ui.kit.glass.qitGlass

/**
 * One choice among a few [options], as a row of segmented buttons. When a label wouldn't fit its
 * segment (large text, long translations) the choices stack as a list of radio buttons instead, so
 * no label is ever cut off.
 */
@Composable
fun <T> QItSegmentedToggle(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    val labels = options.map { label(it) }
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val segment = maxWidth / options.size
        // A segment pads its label and, when selected, shows a check mark beside it.
        val room = segment - KitTokens.SegmentChrome
        val fits = labels.all { text ->
            with(density) { measurer.measure(text, style, maxLines = 1).size.width.toDp() } <= room
        }
        if (fits) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        label = { Text(text = labels[index], maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().selectableGroup()) {
                options.forEachIndexed { index, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = QItTheme.sizes.touchTarget)
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            ),
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.width(QItTheme.spacing.sm))
                        Text(text = labels[index], style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

/** A settings row that reads as one switch: the whole row toggles, the switch only shows state. */
@Composable
fun QItSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = supporting?.let { text ->
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        // The row sits on whatever holds it: a sheet, a card, glass.
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .heightIn(min = QItTheme.sizes.touchTarget)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/** A pill-shaped search field; results are expected to filter as the user types. */
@Composable
fun QItSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val shape = MaterialTheme.shapes.extraLarge
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val glass = glassLook(container, QItSurfaceRole.CARD)
    val fill = if (glass == null) container else Color.Transparent
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = clearLabel)
                }
            }
        },
        singleLine = true,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = fill,
            unfocusedContainerColor = fill,
            // A pill has no underline.
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = if (glass == null) {
            modifier.fillMaxWidth()
        } else {
            modifier.fillMaxWidth().qitGlass(glass, shape, backdrop = null)
        },
    )
}

/**
 * A chip floating over the page (e.g. "back to the reciting ayah"): raised on a shadow when solid,
 * frosted over the page in glass.
 */
@Composable
fun QItFloatingChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    val glass = glassLook(container, QItSurfaceRole.CHROME)
    if (glass == null) {
        ElevatedSuggestionChip(onClick = onClick, label = label, modifier = modifier, icon = icon)
    } else {
        val shape = SuggestionChipDefaults.shape
        SuggestionChip(
            onClick = onClick,
            label = label,
            icon = icon,
            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.Transparent),
            border = BorderStroke(0.dp, Color.Transparent),
            modifier = modifier.qitGlass(glass, shape, LocalQItBackdrop.current),
        )
    }
}
