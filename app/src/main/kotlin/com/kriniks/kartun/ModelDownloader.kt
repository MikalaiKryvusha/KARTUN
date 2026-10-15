package com.kriniks.kartun

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads a catalogue model into a directory (plans/05 step 5): each file goes to `<name>.part`, resumes from where an
 * interrupted download stopped (HTTP Range), and only after its SHA-256 equals the catalogue's is it renamed to its real
 * name - a file of unknown origin never sits under a model's name (plans/05 acceptance 1: «a file whose hash differs is
 * deleted»). The network is touched only here and in LinkDownloader, both on the user's own tap (interview #001, Q1 = A;
 * plans/01 §4.9). Blocking: call it off the main thread. [NOT-TESTED: on the device; the file:// path - DownloaderTest]
 */
object ModelDownloader {
    const val PART = ".part"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val BUFFER = 64 * 1024

    /** The downloaded bytes did not hash to the catalogue's value; the file was deleted. */
    class Damaged(name: String) : IOException("hash mismatch: $name")

    /** The user cancelled; the `.part` stays for a later resume. */
    class Cancelled : IOException("cancelled")

    /**
     * Downloads every file of [m] that is not already in [dir] from `url(file)`. [progress] gets the bytes on disk so far
     * and the model's total; [cancelled] is polled between buffers.
     */
    fun download(m: Model, dir: File, url: (ModelFile) -> String, progress: (Long, Long) -> Unit, cancelled: () -> Boolean = { false }) {
        dir.mkdirs()
        val total = m.bytes
        var before = 0L
        for (f in m.files) {
            val target = File(dir, f.name)
            if (target.length() == f.bytes) { before += f.bytes; progress(before, total); continue }
            val part = File(dir, f.name + PART)
            if (part.length() > f.bytes) part.delete()
            fetch(url(f), part, f.bytes) { got -> progress(before + got, total) ; cancelled() }
            if (!verify(part, f.sha256)) { part.delete(); throw Damaged(f.name) }
            target.delete()
            if (!part.renameTo(target)) throw IOException("cannot place ${f.name}")
            before += f.bytes
            progress(before, total)
        }
    }

    /** True when [file] hashes to [sha256] (lower-case hex). [TESTED: 2026-10-16 · DownloaderTest: a tampered byte is refused] */
    fun verify(file: File, sha256: String): Boolean = file.isFile && sha256(file) == sha256.lowercase()

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(BUFFER)
            while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Appends the rest of [url] to [part]; restarts from zero when the server ignores the Range request. Returns when the
     *  part holds [size] bytes; [onBytes] gets the part's length and answers "cancelled?". */
    private fun fetch(url: String, part: File, size: Long, onBytes: (Long) -> Boolean) {
        var have = part.length()
        if (have == size) return
        val conn = URL(url).openConnection()
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        if (have > 0) conn.setRequestProperty("Range", "bytes=$have-")
        if (conn is HttpURLConnection) {
            conn.instanceFollowRedirects = true  // GitHub release assets answer with a redirect to their storage
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) have = 0L                 // a full body: start over
            else if (code != HttpURLConnection.HTTP_PARTIAL) throw IOException("HTTP $code for $url")
        } else {
            have = 0L  // file:// (tests) has no Range: always the whole body
        }
        conn.getInputStream().use { input ->
            FileOutputStream(part, have > 0).use { out ->
                val buf = ByteArray(BUFFER)
                var got = have
                while (true) {
                    if (onBytes(got)) throw Cancelled()
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    got += n
                    if (got > size) throw IOException("more bytes than the catalogue says: $url")
                }
                onBytes(got)
            }
        }
        if (part.length() != size) throw IOException("short download: ${part.length()} of $size bytes")
    }
}
