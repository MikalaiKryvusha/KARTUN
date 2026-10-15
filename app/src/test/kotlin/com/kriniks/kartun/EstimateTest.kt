package com.kriniks.kartun

import com.kriniks.kartun.UpscaleViewModel.Companion.rateKey
import com.kriniks.kartun.UpscaleViewModel.Companion.startRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM hygiene for the time estimate (the owner, 2026-10-14 ≈ 22:40: «пишет в кнопке 6 секунд», «а на самом деле больше
 * минуты» with the GPU off): the CPU and the GPU keep separate rates, and every catalogue model's CPU starting rate is the
 * slow one. Whether the button shows the right words is read on the device.
 */
class EstimateTest {
    private val models = TestCatalogue.catalogue.models

    @Test fun cpuAndGpuLearnSeparately() {
        for (m in models) assertNotEquals(rateKey(m, gpu = true), rateKey(m, gpu = false))
    }

    @Test fun cpuStartsMuchSlowerThanGpu() {
        // Titan: the CPU is 15-16 times slower; anything under 10 times would bring back the «6 seconds» for a minute's work.
        for (m in models) assertTrue(m.id, startRate(m, gpu = false) > 10 * startRate(m, gpu = true))
    }

    @Test fun theOldMixedKeyIsNotReadAnyMore() {
        // The old key was the bare model file name; a CPU run had been averaged into it.
        for (m in models) for (gpu in listOf(true, false)) assertNotEquals(m.id, rateKey(m, gpu))
    }

    @Test fun theRateLearnedBeforeTheCatalogueStillCounts() {
        // The enum's keys were rate_<file>_gpu/cpu; the catalogue ids equal those file names, so a device keeps what it learned.
        val general = TestCatalogue.catalogue.byId("realesr-general-x4v3")!!
        assertEquals("rate_realesr-general-x4v3_gpu", rateKey(general, gpu = true))
    }
}
