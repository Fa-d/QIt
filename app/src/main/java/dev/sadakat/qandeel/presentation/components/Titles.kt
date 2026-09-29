package dev.sadakat.qandeel.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sadakat.qandeel.R

/** "Al-Baqara 2:255" — how a playing position is named across the UI. */
@Composable
fun ayahTitleText(surahName: String, surahNumber: Int, ayah: Int): String =
    stringResource(R.string.ayah_title, surahName, surahNumber, ayah)

/** "Al-Baqara · Bismillah" — ayah 0, the basmala played before verse 1. */
@Composable
fun bismillahTitleText(surahName: String): String = stringResource(R.string.bismillah_title, surahName)
