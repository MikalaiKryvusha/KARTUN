package com.kriniks.kartun

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * JVM hygiene for the saved file's name (plans/01 §2.8: a NEW file, the original never overwritten): the original's name
 * without its extension, then the model and the scale, so a save can never take the original's name. Hygiene only:
 * whether the gallery shows the file is read on the device.
 */
class FileNameTest {

    @Test fun keepsTheOriginalNameAndAddsModelAndScale() =
        assertEquals("IMG_20260317_kartun_realesr-general-x4v3_2x.png", ResultStore.fileName("IMG_20260317.jpg", "realesr-general-x4v3", 2, "png"))

    @Test fun neverEqualsTheOriginal() = assertNotEquals("photo.jpg", ResultStore.fileName("photo.jpg", "m", 4, "jpg"))

    // Spaces, brackets and other path-unfriendly characters become one underscore; letters of any script stay.
    @Test fun cleansUnsafeCharacters() = assertEquals("Фото_мамы_1_kartun_m_4x.webp", ResultStore.fileName("Фото мамы (1).jpeg", "m", 4, "webp"))

    @Test fun emptyNameFallsBackToPhoto() = assertEquals("photo_kartun_m_1x.png", ResultStore.fileName(".jpg", "m", 1, "png"))
}
