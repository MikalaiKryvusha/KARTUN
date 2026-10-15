package com.kriniks.kartun

import android.content.Context
import java.io.File

/**
 * Where the model files live on the device: `filesDir/models/` - private to KARTUN and removed with it (the owner, plans/05
 * step 5: «хранить их в директории приложения»). The native engine loads a model from a file path, so a [Model.builtin]
 * model is copied out of the APK (assets/models, staged by tools/stage-models.mjs) on first use - a copy whose size differs
 * from the asset is replaced (an app update with a new model); a catalogue model is there after [ModelDownloader] put it
 * there, hash-checked. A model is "on the device" when both its files are present at their catalogue size.
 * [TESTED: 2026-10-14 · built-in copy walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 * [NOT-TESTED: the downloaded-model half (2026-10-16)]
 */
object ModelStore {
    private const val ASSET_DIR = "models"

    fun dir(context: Context): File = File(context.filesDir, ASSET_DIR).apply { mkdirs() }

    /** Returns the `.param` and `.bin` files of [m]; a built-in one is copied out of the APK when needed. */
    fun files(context: Context, m: Model): Pair<File, File> {
        val dir = dir(context)
        if (m.builtin) return copyIfStale(context, dir, m.param.name) to copyIfStale(context, dir, m.bin.name)
        check(isDownloaded(context, m)) { "model ${m.id} is not on the device" }
        return File(dir, m.param.name) to File(dir, m.bin.name)
    }

    /** A catalogue model whose two files are on the device at their catalogue size. */
    fun isDownloaded(context: Context, m: Model): Boolean =
        !m.builtin && m.files.all { File(dir(context), it.name).length() == it.bytes }

    fun isAvailable(context: Context, m: Model): Boolean = m.builtin || isDownloaded(context, m)

    /** «Удалить» on a downloaded card (the owner: «нужно давтаь возможность удалять файл модели»); built-in ones stay. */
    fun delete(context: Context, m: Model) {
        if (m.builtin) return
        val dir = dir(context)
        for (f in m.files) {
            File(dir, f.name).delete()
            File(dir, f.name + ModelDownloader.PART).delete()
        }
    }

    private fun copyIfStale(context: Context, dir: File, file: String): File {
        val target = File(dir, file)
        val assetPath = "$ASSET_DIR/$file"
        val assetSize = context.assets.openFd(assetPath).use { it.length }
        if (target.length() != assetSize) {
            val tmp = File(dir, "$file.tmp")
            context.assets.open(assetPath).use { input -> tmp.outputStream().use { input.copyTo(it) } }
            check(tmp.renameTo(target) || (target.delete() && tmp.renameTo(target))) { "cannot place $file" }
        }
        return target
    }
}
