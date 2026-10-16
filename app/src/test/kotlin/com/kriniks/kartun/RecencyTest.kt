package com.kriniks.kartun

import com.kriniks.kartun.UpscaleViewModel.Companion.RECENT_CARDS
import com.kriniks.kartun.UpscaleViewModel.Companion.pickedFirst
import com.kriniks.kartun.UpscaleViewModel.Companion.recent
import com.kriniks.kartun.UpscaleViewModel.Companion.usedFirst
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM hygiene for the model cards of «Улучшение» (plan 03 step 18: «показывать две последние использованные модели.
 * Вверху последнюю использованную, ниже предпоследнюю»): the two models on the device used last, the last on top; the
 * built-in ones (the default first) fill the list before anything was used; junk in the preference and models that left
 * the device are skipped. Whether the cards move on the screen is read on the device.
 */
class RecencyTest {
    private val cat = TestCatalogue.catalogue
    private val builtIn = cat.models.filter { it.builtin }.map { it.id }.toSet()
    private val default = cat.default.id
    private val other = cat.models.first { it.builtin && !it.isDefault }.id
    private val downloaded = "realesr-general-x4v3"

    @Test fun nothingStoredShowsTheBuiltInOnesDefaultFirst() {
        assertEquals(listOf(default, other), recent(null, cat, builtIn))
        assertEquals(listOf(default, other), recent("", cat, builtIn))
    }

    @Test fun aRunMovesItsModelToTheTop() {
        val onDevice = builtIn + downloaded
        val afterDownloaded = usedFirst(null, downloaded)
        assertEquals(listOf(downloaded, default), recent(afterDownloaded, cat, onDevice))
        assertEquals(listOf(other, downloaded), recent(usedFirst(afterDownloaded, other), cat, onDevice))
    }

    @Test fun junkRepeatsAndDeletedModelsAreSkipped() {
        // «NoSuchModel» is not in the catalogue; the downloaded model was deleted (not on the device any more).
        assertEquals(listOf(other, default), recent("$other,NoSuchModel,$other,$downloaded", cat, builtIn))
    }

    /** A pick in «Все модели» takes the top card, the former top card moves down (the owner, chat 2026-10-16, before
     *  10:48 +03:00) - also when the top card was a built-in filler nobody ran yet. */
    @Test fun aPickInAllModelsTakesTheTopCardAndPushesTheTopOneDown() {
        val onDevice = builtIn + downloaded
        assertEquals(listOf(downloaded, default), recent(pickedFirst(null, cat, onDevice, downloaded), cat, onDevice))
        val stored = usedFirst(null, other) // shown: other, default
        assertEquals(listOf(downloaded, other), recent(pickedFirst(stored, cat, onDevice, downloaded), cat, onDevice))
        // picking the lower card swaps the two
        assertEquals(listOf(default, other), recent(pickedFirst(stored, cat, onDevice, default), cat, onDevice))
    }

    @Test fun alwaysTwoCards() {
        assertEquals(RECENT_CARDS, recent(downloaded, cat, builtIn + downloaded).size)
    }
}
