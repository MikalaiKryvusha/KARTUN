package com.kriniks.kartun

import com.kriniks.kartun.ui.purposeResOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM hygiene for the catalogue the APK carries: it parses, the owner's picks hold (NomosWebPhoto by default, NomosWebPhoto
 * and Siax inside the APK - MASTER_PLAN 2026-10-15 14:12-14:21), every card has a description string and a known badge, and
 * a non-commercial licence never shows the green badge.
 */
class CatalogueTest {
    private val cat = TestCatalogue.catalogue

    @Test fun theOwnersPicksHold() {
        assertEquals("4xNomosWebPhoto-RealPLKSR-fp16", cat.default.id)
        assertEquals(setOf("4xNomosWebPhoto-RealPLKSR-fp16", "4x-NMKD-Siax-fp16"), cat.models.filter { it.builtin }.map { it.id }.toSet())
    }

    @Test fun everyModelHasTwoFilesAndADescription() {
        for (m in cat.models) {
            assertEquals(m.id, 2, m.files.size)
            assertNotNull("${m.id}: no string for purpose ${m.purpose}", purposeResOrNull(m.purpose))
            assertTrue(m.id, m.bytes > 0)
        }
    }

    @Test fun nonCommercialNeverShowsTheGreenBadge() {
        for (m in cat.models) if (m.licence.contains("-NC-")) assertEquals(m.id, Model.Badge.NonCommercial, m.badge)
    }

    @Test fun filesComeFromTheModelsRelease() {
        val m = cat.models.first { !it.builtin }
        assertEquals("https://github.com/MikalaiKryvusha/KARTUN/releases/download/models/${m.bin.name}", cat.url(m.bin))
    }
}
