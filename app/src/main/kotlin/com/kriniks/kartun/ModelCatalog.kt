package com.kriniks.kartun

import android.content.Context
import org.json.JSONObject

/** One file of a model with the SHA-256 and the size every download is checked against. */
data class ModelFile(val name: String, val sha256: String, val bytes: Long)

/**
 * One model of the catalogue - docs/model-catalogue.json, copied into assets/catalogue.json by tools/catalogue.mjs
 * (plans/05 step 2). [builtin] models ship inside the APK (assets/models, tools/stage-models.mjs), the rest are downloaded
 * on demand from the GitHub release `models` (the owner's variant B, MASTER_PLAN 2026-10-14 ≈ 22:04). [purpose] names the
 * card's description (string model_purpose_<purpose>), [kind] its filter chip; [gpuMsPerMp] / [cpuMsPerMp] start the time
 * estimate until the device learns its own rate. [NOT-TESTED]
 */
data class Model(
    val id: String,
    val name: String,
    val author: String,
    val licence: String,
    val commercial: Boolean,
    val purpose: String,
    val kind: String,
    val builtin: Boolean,
    val isDefault: Boolean,
    val gpuMsPerMp: Float,
    val cpuMsPerMp: Float,
    val files: List<ModelFile>,
) {
    val param: ModelFile get() = files.first { it.name.endsWith(".param") }
    val bin: ModelFile get() = files.first { it.name.endsWith(".bin") }
    val bytes: Long get() = files.sumOf { it.bytes }

    /** The licence as the badge shows it: «BSD-3», «CC BY 4.0», «Apache 2.0»… */
    val licenceLabel: String get() = LICENCE_LABELS[licence] ?: licence

    /** Which of the three badges (mockup .badge.os / .by / .nc): free, free with credit, not for commercial use. */
    val badge: Badge get() = when {
        !commercial -> Badge.NonCommercial
        licence.startsWith("CC-BY") -> Badge.Credit
        else -> Badge.Free
    }

    enum class Badge { Free, Credit, NonCommercial }

    companion object {
        private val LICENCE_LABELS = mapOf(
            "BSD-3-Clause" to "BSD-3", "CC-BY-4.0" to "CC BY 4.0", "CC-BY-NC-SA-4.0" to "CC BY-NC-SA 4.0",
            "Apache-2.0" to "Apache 2.0", "MIT" to "MIT", "WTFPL" to "WTFPL",
        )
    }
}

/** The whole catalogue: where the files are served from and the models in their display order. */
class Catalogue(val release: String, val models: List<Model>) {
    fun byId(id: String?): Model? = models.firstOrNull { it.id == id }

    /** The model a first launch starts with (the owner, MASTER_PLAN 2026-10-15 ≈ 14:21: «NomosWebPhoto по умолчанию»). */
    val default: Model get() = models.first { it.isDefault }

    /** Where [file] of a catalogue model is downloaded from. */
    fun url(file: ModelFile): String = release.trimEnd('/') + "/" + file.name
}

object ModelCatalog {
    private const val ASSET = "catalogue.json"
    @Volatile private var cached: Catalogue? = null

    /** The catalogue of the APK, read once per process. */
    fun load(context: Context): Catalogue =
        cached ?: synchronized(this) {
            cached ?: parse(context.assets.open(ASSET).bufferedReader().use { it.readText() }).also { cached = it }
        }

    /** Parses the app's copy of the catalogue. [TESTED: 2026-10-16 · CatalogueTest on the real assets/catalogue.json] */
    fun parse(json: String): Catalogue {
        val root = JSONObject(json)
        val arr = root.getJSONArray("models")
        val models = (0 until arr.length()).map { i ->
            val m = arr.getJSONObject(i)
            val files = m.getJSONArray("files").let { fa ->
                (0 until fa.length()).map { j ->
                    val f = fa.getJSONObject(j)
                    ModelFile(f.getString("name"), f.getString("sha256"), f.getLong("bytes"))
                }
            }
            Model(
                id = m.getString("id"), name = m.getString("name"), author = m.getString("author"),
                licence = m.getString("licence"), commercial = m.getBoolean("commercial"),
                purpose = m.getString("purpose"), kind = m.getString("kind"), builtin = m.getBoolean("builtin"),
                isDefault = m.optBoolean("default", false),
                gpuMsPerMp = m.getDouble("gpuMsPerMp").toFloat(), cpuMsPerMp = m.getDouble("cpuMsPerMp").toFloat(),
                files = files,
            )
        }
        return Catalogue(root.getString("release"), models)
    }
}
