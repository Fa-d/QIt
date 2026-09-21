package dev.sadakat.qit.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Root of the phone UI. Placeholder until the Quran screens land (W3). */
@Composable
fun QuranApp() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("QIt")
    }
}
