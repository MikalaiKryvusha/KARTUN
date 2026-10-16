package com.kriniks.kartun

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Where a result goes (plans/01 §2.8–2.9, MASTER_PLAN phase 3): the gallery through MediaStore into Pictures/KARTUN as a
 * NEW file - no storage permission on Android 10+, the original is never touched - or another app through the system
 * share sheet via a FileProvider copy in the cache. [NOT-TESTED: device run deferred by the owner («потом потестируем»)]
 */
object ResultStore {
    /** The three formats of the MVP («обязательно webP, Png, Jpeg с настройкой качества» - the owner's order in GOAL.md). */
    enum class Format(val ext: String, val mime: String, val compress: Bitmap.CompressFormat, val hasQuality: Boolean) {
        Png("png", "image/png", Bitmap.CompressFormat.PNG, false),
        Jpeg("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG, true),
        Webp("webp", "image/webp", Bitmap.CompressFormat.WEBP_LOSSY, true),
    }

    /** WebP stores at most 16 383 px per side (plans/01 §4.5). */
    const val WEBP_MAX_SIDE = 16_383
    /** JPEG: libjpeg's JPEG_MAX_DIMENSION, 65 500 px per side. PNG has no limit KARTUN can reach (536 MP at most). */
    const val JPEG_MAX_SIDE = 65_500
    private const val FOLDER = "Pictures/KARTUN"
    private const val SHARE_DIR = "shared"

    /** The longest side [format] can store. */
    fun maxSide(format: Format): Int = when (format) {
        Format.Webp -> WEBP_MAX_SIDE
        Format.Jpeg -> JPEG_MAX_SIDE
        Format.Png -> Int.MAX_VALUE
    }

    fun fits(format: Format, bmp: Bitmap) = fits(format, bmp.width, bmp.height)
    fun fits(format: Format, w: Int, h: Int) = maxOf(w, h) <= maxSide(format)

    /**
     * [w] x [h] reduced in proportion so that the longer side is exactly [format]'s limit; unchanged when it fits (the
     * owner, chat 2026-10-16 ≈ 15:14 +03:00: «если при сохранении пользователь упирается в лимит формата, то нужно
     * предупреждать в диалоге сохранения, и предложить выполнить сжатие до допустимого размера»).
     * [TESTED: SaveLimitTest - WebP and JPEG boundaries, the aspect kept]
     */
    fun fitSize(format: Format, w: Int, h: Int): Pair<Int, Int> {
        val side = maxSide(format)
        if (maxOf(w, h) <= side) return w to h
        val k = side.toDouble() / maxOf(w, h)
        return if (w >= h) side to maxOf(1, Math.round(h * k).toInt()) else maxOf(1, Math.round(w * k).toInt()) to side
    }

    /** Sample patches per side for [estimateBytes] and their edge, px. */
    private const val SAMPLE_GRID = 3
    private const val SAMPLE_PX = 192

    /**
     * The file size [bmp] will have in [format] at [quality], estimated from the picture itself: a 3×3 grid of 192 px
     * patches at full resolution is encoded the same way and the bytes are scaled by area (the owner, chat 2026-10-15
     * ≈ 12:00 +03:00: «на виджете сохранения с выбором формата нужно писать эстимацию размера файла в мегабайтах»).
     * Patches keep the real detail per pixel, which a scaled-down copy would not. [NOT-TESTED]
     */
    fun estimateBytes(bmp: Bitmap, format: Format, quality: Int): Long {
        val side = minOf(SAMPLE_PX, bmp.width, bmp.height)
        var bytes = 0L
        var area = 0L
        val out = java.io.ByteArrayOutputStream()
        for (gy in 0 until SAMPLE_GRID) for (gx in 0 until SAMPLE_GRID) {
            val x = ((bmp.width - side) * (gx + 0.5f) / SAMPLE_GRID).toInt()
            val y = ((bmp.height - side) * (gy + 0.5f) / SAMPLE_GRID).toInt()
            val patch = Bitmap.createBitmap(bmp, x, y, side, side)
            out.reset()
            patch.compress(format.compress, quality, out)
            if (patch !== bmp) patch.recycle()
            bytes += out.size()
            area += side.toLong() * side
        }
        return bytes * (bmp.width.toLong() * bmp.height) / area
    }

    /** Thrown out of the encoder when «Отменить» under «Рендер…» is tapped; the half-written file is deleted. */
    class RenderCancelled : IOException("render cancelled")

    /**
     * The encoder's output with a stop: Bitmap.compress cannot be interrupted, so its stream throws on the next write once
     * [cancel] is set (the owner, chat 2026-10-15 ≈ 11:53 +03:00: «в диалог Рендер нужно добавить маленькую кнопку
     * Отменить - чтобы можно было остановить долгие рендеры» · «с удалением файла, который рендерился»). [NOT-TESTED]
     */
    private class Cancellable(out: OutputStream, private val cancel: AtomicBoolean) : FilterOutputStream(out) {
        override fun write(b: Int) { if (cancel.get()) throw RenderCancelled(); out.write(b) }
        override fun write(b: ByteArray, off: Int, len: Int) { if (cancel.get()) throw RenderCancelled(); out.write(b, off, len) }
    }

    /** "IMG_2026_kartun_x4plus_4x.png" - the original's name, the model and the scale: never the original's own name. */
    fun fileName(original: String, model: String, scale: Int, ext: String): String {
        val base = original.substringBeforeLast('.').ifBlank { "photo" }.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").trim('_').ifBlank { "photo" }
        return "${base}_kartun_${model}_${scale}x.$ext"
    }

    /** Writes [full] into the gallery; returns the new item's uri. Pending until fully written, so the gallery never shows
     *  half a file. A result over [format]'s side limit is first reduced to [fitSize] - the save dialog said so. */
    fun saveToGallery(ctx: Context, full: Bitmap, name: String, format: Format, quality: Int, cancel: AtomicBoolean): Uri {
        val (tw, th) = fitSize(format, full.width, full.height)
        val bmp = if (tw == full.width && th == full.height) full else Bitmap.createScaledBitmap(full, tw, th, true)
        try {
            return saveFitting(ctx, bmp, name, format, quality, cancel)
        } finally {
            if (bmp !== full) bmp.recycle()
        }
    }

    private fun saveFitting(ctx: Context, bmp: Bitmap, name: String, format: Format, quality: Int, cancel: AtomicBoolean): Uri {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, FOLDER)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: error("MediaStore refused the new image")
        try {
            Cancellable(resolver.openOutputStream(uri)!!, cancel).use {
                val ok = bmp.compress(format.compress, quality, it)
                if (cancel.get()) throw RenderCancelled() // some encoders swallow the stream's exception and return false
                check(ok) { "encoder failed" }
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
        return uri
    }

    /**
     * Writes [bmp] into [file] in [format] for the agent door (plans/07 step 5): encoded into `<file>.tmp` and renamed, so
     * an agent polling the folder never pulls half a picture. A WebP over [WEBP_MAX_SIDE] is refused before encoding
     * ([IllegalArgumentException] naming PNG/JPEG). Returns the bytes written. [NOT-TESTED]
     */
    fun writeFile(bmp: Bitmap, file: File, format: Format, quality: Int, cancel: AtomicBoolean): Long {
        require(fits(format, bmp)) { "${format.ext} stores at most ${maxSide(format)} px per side; this result is ${bmp.width} x ${bmp.height} - use png" }
        val tmp = File(file.parentFile, file.name + ".tmp")
        try {
            Cancellable(tmp.outputStream().buffered(), cancel).use {
                val ok = bmp.compress(format.compress, quality, it)
                if (cancel.get()) throw RenderCancelled()
                check(ok) { "encoder failed" }
            }
            check(tmp.renameTo(file)) { "rename failed: ${tmp.name}" }
        } catch (t: Throwable) {
            tmp.delete()
            throw t
        }
        return file.length()
    }

    /** Writes [bmp] as PNG into the cache and returns a content uri another app may read (the share sheet grants it). */
    fun shareCopy(ctx: Context, bmp: Bitmap, name: String, cancel: AtomicBoolean): Uri {
        val dir = File(ctx.cacheDir, SHARE_DIR).apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val file = File(dir, name)
        try {
            Cancellable(file.outputStream(), cancel).use {
                val ok = bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                if (cancel.get()) throw RenderCancelled()
                check(ok) { "encoder failed" }
            }
        } catch (t: Throwable) {
            file.delete()
            throw t
        }
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
    }
}
