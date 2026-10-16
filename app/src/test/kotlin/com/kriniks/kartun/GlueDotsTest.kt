package com.kriniks.kartun

import com.kriniks.kartun.ui.glueDots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** The card line never breaks right before or after «·» or after «≈» (the owner, 2026-10-16 ≈ 14:58: «разделители
 *  булет-дотом, которые остаются на строке в конце, и после него перенос … некрасиво»). */
class GlueDotsTest {
    @Test fun separatorKeepsNoBreakingSpace() {
        val s = glueDots("Очень медленная · максимум деталей на фото и рисунках · 85 МБ")
        assertFalse(s, s.contains(" ·") || s.contains("· "))
        assertEquals("рисунках · 85 МБ", s.substringAfterLast("и "))
    }

    @Test fun approxSignStaysWithItsNumber() {
        assertEquals("Быстрая · ≈ 12 минут", glueDots("Быстрая · ≈ 12 минут"))
    }

    @Test fun textWithoutSeparatorIsUnchanged() {
        assertEquals("Не для коммерческого использования", glueDots("Не для коммерческого использования"))
    }
}
