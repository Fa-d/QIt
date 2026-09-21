package dev.sadakat.qit.core.data.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.domain.audio.QuranAudioUrls
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.testing.TestQuran
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuranMediaItemsTest {

    private val alBaqara = TestQuran.surah(2)

    @Test
    fun `build maps every queue entry to a media item`() {
        val items = QuranMediaItems.build(alBaqara, RecitationMode.ARABIC_ENGLISH)

        assertEquals(574, items.size)
        assertEquals("2:0:ar", items[0].mediaId)
        assertEquals("2:0:en", items[1].mediaId)
        assertEquals("2:286:en", items.last().mediaId)
    }

    @Test
    fun `build streams the file url of each track without a custom cache key`() {
        val items = QuranMediaItems.build(alBaqara, RecitationMode.ARABIC_BANGLA)

        // The Bangla intro, then 2:1 on both tracks (global ayah 8).
        assertEquals(
            "${QuranAudioUrls.HF_DATASET}/bangla/bangla-translation-verses/intro/002.mp3",
            items[0].localConfiguration?.uri.toString(),
        )
        assertEquals(
            QuranAudioUrls.verse(Track.ARABIC, 8).url,
            items[1].localConfiguration?.uri.toString(),
        )
        assertEquals(
            QuranAudioUrls.verse(Track.BANGLA, 8).url,
            items[2].localConfiguration?.uri.toString(),
        )
        assertNull(items[0].localConfiguration?.customCacheKey)
    }

    @Test
    fun `build titles the basmala and each ayah`() {
        val items = QuranMediaItems.build(alBaqara, RecitationMode.ARABIC_ENGLISH)

        assertEquals("Al-Baqara · Bismillah", items[0].mediaMetadata.title)
        assertEquals("Al-Baqara 2:1", items[2].mediaMetadata.title)
        assertEquals("Al-Baqara 2:286", items.last().mediaMetadata.title)
    }

    @Test
    fun `build sets the artist per track and the surah as album`() {
        val arabicEnglish = QuranMediaItems.build(alBaqara, RecitationMode.ARABIC_ENGLISH)
        val arabicBangla = QuranMediaItems.build(alBaqara, RecitationMode.ARABIC_BANGLA)

        assertEquals("Mishary Alafasy", arabicEnglish[0].mediaMetadata.artist)
        assertEquals("Saheeh International", arabicEnglish[1].mediaMetadata.artist)
        assertEquals("Bangla translation", arabicBangla[0].mediaMetadata.artist)
        assertEquals("Al-Baqara", arabicEnglish[0].mediaMetadata.albumTitle)
    }
}
