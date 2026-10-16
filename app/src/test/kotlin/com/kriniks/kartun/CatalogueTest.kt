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

    /** «Все модели» by author, UltraSharp's and UltraMix's author on top (the owner, chat 2026-10-16, before 10:48 +03:00). */
    @Test fun allModelsListGoesByAuthorUltraSharpsAuthorFirst() {
        val list = cat.byAuthor
        assertEquals(cat.models.toSet(), list.toSet())
        val lead = list.takeWhile { it.author == Catalogue.LEAD_AUTHOR }
        assertTrue("the lead author spelling left the catalogue", lead.any { it.name.contains("UltraSharp") } && lead.any { it.name.contains("UltraMix") })
        assertTrue(list.drop(lead.size).none { it.author == Catalogue.LEAD_AUTHOR })
        val others = list.drop(lead.size).map { it.author.lowercase() }
        assertEquals(others.sorted(), others)
        // one author's models stand together
        val runs = list.map { it.author }.fold(listOf<String>()) { acc, a -> if (acc.lastOrNull() == a) acc else acc + a }
        assertEquals(runs.distinct(), runs)
    }

    @Test fun filesComeFromTheModelsRelease() {
        val m = cat.models.first { !it.builtin }
        assertEquals("https://github.com/MikalaiKryvusha/KARTUN/releases/download/models/${m.bin.name}", cat.url(m.bin))
    }
}
