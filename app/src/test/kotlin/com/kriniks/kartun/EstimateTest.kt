package com.kriniks.kartun

import com.kriniks.kartun.UpscaleViewModel.Companion.rateKey
import com.kriniks.kartun.UpscaleViewModel.Companion.startRate
import com.kriniks.kartun.UpscaleViewModel.Model
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM hygiene for the time estimate (the owner, 2026-10-14 ≈ 22:40: «пишет в кнопке 6 секунд», «а на самом деле больше
 * минуты» with the GPU off): the CPU and the GPU keep separate rates, and the CPU's starting rate is the slow measured
 * one. Whether the button shows the right words is read on the device.
 */
class EstimateTest {

    @Test fun cpuAndGpuLearnSeparately() {
        for (m in Model.entries) assertNotEquals(rateKey(m, gpu = true), rateKey(m, gpu = false))
    }

    @Test fun cpuStartsMuchSlowerThanGpu() {
        // Titan: the CPU is 15-16 times slower; anything under 10 times would bring back the «6 seconds» for a minute's work.
        for (m in Model.entries) assertTrue(m.name, startRate(m, gpu = false) > 10 * startRate(m, gpu = true))
    }

    @Test fun theOldMixedKeyIsNotReadAnyMore() {
        // The old key was the bare model file name; a CPU run had been averaged into it.
        for (m in Model.entries) for (gpu in listOf(true, false)) assertNotEquals(m.file, rateKey(m, gpu))
    }
}
