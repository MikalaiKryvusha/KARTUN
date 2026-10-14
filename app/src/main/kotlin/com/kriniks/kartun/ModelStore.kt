package com.kriniks.kartun

import android.content.Context
import java.io.File

/**
 * The models shipped inside the APK (assets/models, staged by tools/stage-models.mjs; provenance in
 * docs/model-licenses.md). The native engine loads a model from a file path, so the first use copies the pair into
 * filesDir/models; a copy whose size differs from the asset is replaced (an app update with a new model). [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */
object ModelStore {
    private const val ASSET_DIR = "models"

    /** Returns the `.param` and `.bin` files of [name], copying them out of the APK when needed. */
    fun files(context: Context, name: String): Pair<File, File> {
        val dir = File(context.filesDir, ASSET_DIR).apply { mkdirs() }
        val param = copyIfStale(context, dir, "$name.param")
        val bin = copyIfStale(context, dir, "$name.bin")
        return param to bin
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
