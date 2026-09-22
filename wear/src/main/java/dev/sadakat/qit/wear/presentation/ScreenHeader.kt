package dev.sadakat.qit.wear.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

/**
 * A list's title, as its first item. Wear lists open centered on their second item, so without a
 * header the first row would start half under the clock.
 */
@Composable
internal fun TransformingLazyColumnItemScope.ScreenHeader(text: String, modifier: Modifier = Modifier) {
    val transformationSpec = rememberTransformationSpec()
    ListHeader(
        modifier = modifier
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec)
            .semantics { heading() },
        transformation = SurfaceTransformation(transformationSpec),
    ) {
        Text(text)
    }
}
