package com.kriniks.kartun

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

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

    /** WebP stores at most 16 383 px per side (plans/01 §4.5): a larger result is offered PNG or JPEG instead. */
    const val WEBP_MAX_SIDE = 16_383
    private const val FOLDER = "Pictures/KARTUN"
    private const val SHARE_DIR = "shared"

    fun fits(format: Format, bmp: Bitmap) = format != Format.Webp || maxOf(bmp.width, bmp.height) <= WEBP_MAX_SIDE

    /** "IMG_2026_kartun_x4plus_4x.png" - the original's name, the model and the scale: never the original's own name. */
    fun fileName(original: String, model: String, scale: Int, ext: String): String {
        val base = original.substringBeforeLast('.').ifBlank { "photo" }.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").trim('_').ifBlank { "photo" }
        return "${base}_kartun_${model}_${scale}x.$ext"
    }

    /** Writes [bmp] into the gallery; returns the new item's uri. Pending until fully written, so the gallery never shows half a file. */
    fun saveToGallery(ctx: Context, bmp: Bitmap, name: String, format: Format, quality: Int): Uri {
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
            resolver.openOutputStream(uri)!!.use { check(bmp.compress(format.compress, quality, it)) { "encoder failed" } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
        return uri
    }

    /** Writes [bmp] as PNG into the cache and returns a content uri another app may read (the share sheet grants it). */
    fun shareCopy(ctx: Context, bmp: Bitmap, name: String): Uri {
        val dir = File(ctx.cacheDir, SHARE_DIR).apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val file = File(dir, name)
        file.outputStream().use { check(bmp.compress(Bitmap.CompressFormat.PNG, 100, it)) { "encoder failed" } }
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
    }
}
