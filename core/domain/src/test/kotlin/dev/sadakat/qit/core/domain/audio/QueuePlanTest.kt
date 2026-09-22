package dev.sadakat.qit.core.domain.audio

import dev.sadakat.qit.core.domain.model.BanglaVoice
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueuePlanTest {

    @Test
    fun `plan of surah 1 has no basmala prefix`() {
        val queue = QueuePlan.plan(1, RecitationMode.ARABIC_BANGLA, BanglaVoice.DEFAULT)

        assertEquals(14, queue.size) // 7 ayahs x 2 tracks
        assertEquals(QueueItemId(1, 1, Track.ARABIC), queue[0].id)
        assertEquals(QueueItemId(1, 1, Track.BANGLA), queue[1].id)
        assertEquals(QueueItemId(1, 7, Track.BANGLA), queue.last().id)
    }

    @Test
    fun `plan of surah 2 arabic only prefixes the basmala`() {
        val queue = QueuePlan.plan(2, RecitationMode.ARABIC_ONLY, BanglaVoice.DEFAULT)

        assertEquals(287, queue.size) // basmala + 286 ayahs
        assertEquals(QueueItemId(2, 0, Track.ARABIC), queue[0].id)
        assertEquals(QueueItemId(2, 1, Track.ARABIC), queue[1].id)
        assertEquals(QueueItemId(2, 286, Track.ARABIC), queue.last().id)
    }

    @Test
    fun `plan of surah 2 arabic and english prefixes both basmalas`() {
        val queue = QueuePlan.plan(2, RecitationMode.ARABIC_ENGLISH, BanglaVoice.DEFAULT)

        assertEquals(574, queue.size) // 2 basmalas + 286 ayahs x 2 tracks
        assertEquals(QueueItemId(2, 0, Track.ARABIC), queue[0].id)
        assertEquals(QueueItemId(2, 0, Track.ENGLISH), queue[1].id)
        assertEquals(QueueItemId(2, 1, Track.ARABIC), queue[2].id)
        assertEquals(QueueItemId(2, 286, Track.ENGLISH), queue.last().id)
    }

    @Test
    fun `plan of surah 2 arabic and bangla prefixes only the bangla intro`() {
        val queue = QueuePlan.plan(2, RecitationMode.ARABIC_BANGLA, BanglaVoice.DEFAULT)

        assertEquals(573, queue.size) // intro + 286 ayahs x 2 tracks
        assertEquals(QueueItemId(2, 0, Track.BANGLA), queue[0].id)
        assertEquals("bn/intro/2", queue[0].file.id)
        assertEquals(
            "${QuranAudioUrls.HF_DATASET}/bangla/bangla-translation-verses/intro/002.mp3",
            queue[0].file.url,
        )
        assertEquals(QueueItemId(2, 1, Track.ARABIC), queue[1].id)
    }

    @Test
    fun `plan of surah 9 arabic and bangla has no basmala`() {
        val queue = QueuePlan.plan(9, RecitationMode.ARABIC_BANGLA, BanglaVoice.DEFAULT)

        assertEquals(258, queue.size) // 129 ayahs x 2 tracks
        assertEquals(QueueItemId(9, 1, Track.ARABIC), queue[0].id)
        assertEquals(QueueItemId(9, 129, Track.BANGLA), queue.last().id)
    }

    @Test
    fun `plan plays each ayah's tracks contiguously with the verse files`() {
        val queue = QueuePlan.plan(2, RecitationMode.ARABIC_BANGLA, BanglaVoice.DEFAULT)

        // 2:1 is global ayah 8; after the intro come 1:ar, 1:bn, 2:ar, 2:bn...
        assertEquals(QuranAudioUrls.verse(Track.ARABIC, 8), queue[1].file)
        assertEquals(QuranAudioUrls.verse(Track.BANGLA, 8), queue[2].file)
        assertEquals(QuranAudioUrls.verse(Track.ARABIC, 9), queue[3].file)
    }

    @Test
    fun `plan of surah 2 with Toha pairs Abdul Basit with Toha and prefixes both basmalas`() {
        val queue = QueuePlan.plan(2, RecitationMode.ARABIC_BANGLA, BanglaVoice.SAYED_ISMAT_TOHA)

        assertEquals(574, queue.size) // 2 basmalas + 286 ayahs x 2 tracks
        assertEquals(QueueItemId(2, 0, Track.ARABIC_BASIT_MUJAWWAD), queue[0].id)
        assertEquals(QueueItemId(2, 0, Track.BANGLA_TOHA), queue[1].id)
        // Their basmala is 1:1, the basmala and its meaning.
        assertEquals(QuranAudioUrls.verse(Track.ARABIC_BASIT_MUJAWWAD, 1), queue[0].file)
        assertEquals(QuranAudioUrls.verse(Track.BANGLA_TOHA, 1), queue[1].file)
        assertEquals(QuranAudioUrls.verse(Track.ARABIC_BASIT_MUJAWWAD, 8), queue[2].file)
        assertEquals(QuranAudioUrls.verse(Track.BANGLA_TOHA, 8), queue[3].file)
        assertEquals(QueueItemId(2, 286, Track.BANGLA_TOHA), queue.last().id)
    }

    @Test
    fun `the Bangla voice only changes Arabic and Bangla`() {
        for (voice in BanglaVoice.entries) {
            assertEquals(
                QueuePlan.plan(2, RecitationMode.ARABIC_ENGLISH, BanglaVoice.DEFAULT),
                QueuePlan.plan(2, RecitationMode.ARABIC_ENGLISH, voice),
            )
            assertEquals(
                QueuePlan.plan(2, RecitationMode.ARABIC_ONLY, BanglaVoice.DEFAULT),
                QueuePlan.plan(2, RecitationMode.ARABIC_ONLY, voice),
            )
        }
    }

    private val alBaqaraEnglish =
        QueuePlan.plan(2, RecitationMode.ARABIC_ENGLISH, BanglaVoice.DEFAULT).map { it.id }

    @Test
    fun `indexOfAyah finds the first item of the ayah`() {
        assertEquals(0, QueuePlan.indexOfAyah(alBaqaraEnglish, ayah = 0))
        assertEquals(2, QueuePlan.indexOfAyah(alBaqaraEnglish, ayah = 1))
        assertEquals(510, QueuePlan.indexOfAyah(alBaqaraEnglish, ayah = 255))
        assertEquals(572, QueuePlan.indexOfAyah(alBaqaraEnglish, ayah = 286))
    }

    @Test
    fun `indexOfAyah of an absent ayah is zero`() {
        assertEquals(0, QueuePlan.indexOfAyah(alBaqaraEnglish, ayah = 287))
        assertEquals(0, QueuePlan.indexOfAyah(alBaqaraEnglish, ayah = -1))
        assertEquals(0, QueuePlan.indexOfAyah(alBaqaraEnglish, ayah = 999))
    }

    @Test
    fun `nextAyahIndex skips the translation of the current ayah`() {
        assertEquals(2, QueuePlan.nextAyahIndex(alBaqaraEnglish, currentIndex = 0))
        assertEquals(4, QueuePlan.nextAyahIndex(alBaqaraEnglish, currentIndex = 2))
        assertEquals(4, QueuePlan.nextAyahIndex(alBaqaraEnglish, currentIndex = 3))
    }

    @Test
    fun `nextAyahIndex is null at the last ayah`() {
        assertNull(QueuePlan.nextAyahIndex(alBaqaraEnglish, currentIndex = 572))
        assertNull(QueuePlan.nextAyahIndex(alBaqaraEnglish, currentIndex = 573))
    }

    @Test
    fun `previousAyahIndex is null at the very start of the queue`() {
        assertNull(QueuePlan.previousAyahIndex(alBaqaraEnglish, currentIndex = 0, positionMs = 0))
        assertNull(QueuePlan.previousAyahIndex(alBaqaraEnglish, currentIndex = 0, positionMs = 3000))
    }

    @Test
    fun `previousAyahIndex restarts the basmala after more than 3 seconds`() {
        assertEquals(0, QueuePlan.previousAyahIndex(alBaqaraEnglish, currentIndex = 0, positionMs = 3001))
    }

    @Test
    fun `previousAyahIndex from the first ayah goes back to the basmala`() {
        assertEquals(0, QueuePlan.previousAyahIndex(alBaqaraEnglish, currentIndex = 2, positionMs = 0))
    }

    @Test
    fun `previousAyahIndex restarts the current ayah when past its first item`() {
        // On 2:1's English item: back to the Arabic item of the same ayah.
        assertEquals(2, QueuePlan.previousAyahIndex(alBaqaraEnglish, currentIndex = 3, positionMs = 0))
    }

    @Test
    fun `previousAyahIndex goes to the start of the previous ayah`() {
        assertEquals(2, QueuePlan.previousAyahIndex(alBaqaraEnglish, currentIndex = 4, positionMs = 0))
        assertEquals(2, QueuePlan.previousAyahIndex(alBaqaraEnglish, currentIndex = 4, positionMs = 3000))
    }

    @Test
    fun `previousAyahIndex restarts the current ayah after more than 3 seconds`() {
        assertEquals(4, QueuePlan.previousAyahIndex(alBaqaraEnglish, currentIndex = 4, positionMs = 3001))
        assertEquals(572, QueuePlan.previousAyahIndex(alBaqaraEnglish, currentIndex = 573, positionMs = 3001))
    }
}
