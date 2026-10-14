package com.kriniks.kartun

import android.content.Context
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * «Вставить ссылку на фото» (GOAL.md, the owner's order: «скачать изображение по ссылке вставив на нее ссылку прямо из
 * буфера обмена»; network rule `[OWNER]` interview #001, Q1 = A: «сеть работает только по кнопке „Вставить ссылку“»).
 * Downloads one image into the app cache and returns a file uri for [UpscaleViewModel.open]; the upscale itself stays
 * offline. [NOT-TESTED: device run deferred by the owner («потом потестируем»)]
 */
object LinkDownloader {
    /** Refuses a download larger than this - a link to a huge file must not fill the phone. */
    private const val MAX_BYTES = 60L * 1024 * 1024
    private const val TIMEOUT_MS = 15_000
    private const val MAX_REDIRECTS = 5
    private const val DIR = "link"

    /** The first http(s) link in [text], or null - the clipboard may hold a sentence around the link. */
    fun findUrl(text: CharSequence?): String? =
        text?.let { Regex("""https?://\S+""").find(it)?.value?.trimEnd('.', ',', ')', '»', '"') }

    /** Downloads [url] (following up to five redirects) into the cache; throws on a non-image or an oversized answer. */
    fun download(ctx: Context, url: String): Uri {
        var current = URL(url)
        repeat(MAX_REDIRECTS) {
            val conn = (current.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "KARTUN")
            }
            try {
                when (conn.responseCode) {
                    in 300..399 -> { current = URL(current, conn.getHeaderField("Location") ?: error("redirect without Location")); return@repeat }
                    in 200..299 -> Unit
                    else -> error("HTTP ${conn.responseCode}")
                }
                val type = conn.contentType.orEmpty()
                check(type.startsWith("image/") || type.isEmpty() || type == "application/octet-stream") { "not an image: $type" }
                check(conn.contentLengthLong <= MAX_BYTES) { "too large: ${conn.contentLengthLong}" }
                val dir = File(ctx.cacheDir, DIR).apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                val name = current.path.substringAfterLast('/').ifBlank { "photo" }.take(80)
                val file = File(dir, name)
                conn.inputStream.use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            total += n
                            check(total <= MAX_BYTES) { "too large" }
                            out.write(buf, 0, n)
                        }
                    }
                }
                return Uri.fromFile(file)
            } finally {
                conn.disconnect()
            }
        }
        error("too many redirects")
    }
}
