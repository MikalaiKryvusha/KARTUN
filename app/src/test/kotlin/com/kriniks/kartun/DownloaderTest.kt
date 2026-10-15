package com.kriniks.kartun

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * JVM hygiene for the model download (plans/05 acceptance 1: «a file whose hash differs is deleted and the card says the
 * download failed»): a file is placed under its name only when its SHA-256 matches; a tampered byte leaves nothing behind.
 * The source is a file:// URL - the HTTP path (Range, redirects) is walked on the device.
 */
class DownloaderTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun model(server: File, param: ByteArray, bin: ByteArray, binHash: String? = null): Model {
        File(server, "m.param").writeBytes(param)
        File(server, "m.bin").writeBytes(bin)
        val files = listOf(
            ModelFile("m.param", ModelDownloader.sha256(File(server, "m.param")), param.size.toLong()),
            ModelFile("m.bin", binHash ?: ModelDownloader.sha256(File(server, "m.bin")), bin.size.toLong()),
        )
        return Model("m", "M", "A", "MIT", true, "photo", "photo", false, false, 1f, 20f, files)
    }

    private fun url(server: File): (ModelFile) -> String = { File(server, it.name).toURI().toString() }

    @Test fun aGoodDownloadLandsUnderItsNameWithFullProgress() {
        val server = tmp.newFolder("server"); val dir = tmp.newFolder("models")
        val m = model(server, ByteArray(1000) { it.toByte() }, ByteArray(300_000) { (it * 7).toByte() })
        var last = 0L to 0L
        ModelDownloader.download(m, dir, url(server), { d, t -> last = d to t })
        assertTrue(ModelDownloader.verify(File(dir, "m.bin"), m.bin.sha256))
        assertEquals(m.bytes to m.bytes, last)
        assertFalse(File(dir, "m.bin" + ModelDownloader.PART).exists())
    }

    @Test fun aTamperedByteIsRefusedAndDeleted() {
        val server = tmp.newFolder("server"); val dir = tmp.newFolder("models")
        val good = ByteArray(300_000) { (it * 7).toByte() }
        val goodHash = ModelDownloader.sha256(File(tmp.root, "good.bin").apply { writeBytes(good) })
        val bad = good.copyOf().also { it[123_456] = (it[123_456] + 1).toByte() }
        val m = model(server, ByteArray(1000), bad, binHash = goodHash)
        try {
            ModelDownloader.download(m, dir, url(server), { _, _ -> })
            fail("a tampered file was accepted")
        } catch (_: ModelDownloader.Damaged) {
        }
        assertFalse(File(dir, "m.bin").exists())
        assertFalse(File(dir, "m.bin" + ModelDownloader.PART).exists())
    }

    @Test fun verifyRejectsOneChangedByte() {
        val f = tmp.newFile("x.bin").apply { writeBytes(ByteArray(4096) { it.toByte() }) }
        val h = ModelDownloader.sha256(f)
        f.writeBytes(f.readBytes().also { it[0] = 9 })
        assertFalse(ModelDownloader.verify(f, h))
    }
}
