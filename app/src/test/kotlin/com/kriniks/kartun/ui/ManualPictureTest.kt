package com.kriniks.kartun.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** JVM hygiene for the guide's picture line `![caption](assets path)` (ManualScreen.PICTURE): the caption and the path
 *  come out as written; ordinary lines with brackets are not pictures. Whether the picture is drawn is read on the device. */
class ManualPictureTest {
    @Test fun aPictureLineGivesCaptionAndPath() {
        val (caption, path) = PICTURE.find("![Главный экран](manual/ru/01_home.webp)")!!.destructured
        assertEquals("Главный экран", caption)
        assertEquals("manual/ru/01_home.webp", path)
    }

    @Test fun anEmptyCaptionIsAllowed() {
        assertEquals("manual/en/02.webp", PICTURE.find("![](manual/en/02.webp)")!!.groupValues[2])
    }

    @Test fun textWithBracketsIsNotAPicture() {
        assertFalse(PICTURE.matches("See [the guide](https://example.org) for more."))
        assertFalse(PICTURE.matches("- ![inline](x.webp) in a list"))
    }
}
