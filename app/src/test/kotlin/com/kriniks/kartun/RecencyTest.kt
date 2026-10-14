package com.kriniks.kartun

import com.kriniks.kartun.UpscaleViewModel.Companion.byRecency
import com.kriniks.kartun.UpscaleViewModel.Companion.usedFirst
import com.kriniks.kartun.UpscaleViewModel.Model
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM hygiene for the model cards' order of plan 03 step 18 («Вверху последнюю использованную, ниже предпоследнюю»):
 * the stored order puts the last used model first, survives junk in the preference, and never loses a model. Whether
 * the cards actually move on the screen is read on the device.
 */
class RecencyTest {

    @Test fun nothingStoredKeepsTheCatalogueOrder() {
        assertEquals(Model.entries.toList(), byRecency(null))
        assertEquals(Model.entries.toList(), byRecency(""))
    }

    @Test fun aRunMovesItsModelToTheTop() {
        val afterMax = usedFirst(null, Model.Max)
        assertEquals(listOf(Model.Max, Model.Fast), byRecency(afterMax))
        assertEquals(listOf(Model.Fast, Model.Max), byRecency(usedFirst(afterMax, Model.Fast)))
    }

    @Test fun junkAndRepeatsInThePreferenceAreDropped() {
        assertEquals(listOf(Model.Max, Model.Fast), byRecency("Max,NoSuchModel,Max"))
    }

    @Test fun everyModelStaysInTheList() {
        assertEquals(Model.entries.size, byRecency("Fast").size)
    }
}
