package com.kriniks.kartun.agent

/**
 * The job contract of the ADB door (plans/06 §4.4, plans/07 step 3) in pure Kotlin, so a JVM test checks it without a
 * device: what an agent may ask ([JobSpec.parse] of the intent extras), what the app answers ([JobStatus.toJson] -
 * the `<job>.json` file), and the names of the job's files in `files/agent/`. JSON is written by hand ([q]): `org.json`
 * is a stub in JVM unit tests. [NOT-TESTED]
 */

/** The error codes of a failed job (plans/07 protocol block); the CLI adds `no_device`, `adb_missing`, `timeout`. */
object ErrorCode {
    const val BAD_REQUEST = "bad_request"
    const val INPUT_MISSING = "input_missing"
    const val DECODE_FAILED = "decode_failed"
    const val TOO_BIG = "too_big"
    const val MODEL_NOT_INSTALLED = "model_not_installed"
    const val MODEL_LOAD_FAILED = "model_load_failed"
    const val ENGINE_FAILED = "engine_failed"
    const val BUSY = "busy"
    const val QUOTA = "quota"
    const val INTERNAL = "internal"
}

enum class JobState(val wire: String) { Queued("queued"), Running("running"), Succeeded("succeeded"), Failed("failed"), Canceled("canceled") }

/** One UPSCALE request: the job id the CLI made, the input file name inside `files/agent/`, and the choices. */
data class JobSpec(
    val job: String,
    val input: String,
    /** A catalogue id, or [DEFAULT_MODEL] for the app's default model. */
    val model: String,
    val scale: Int,
    /** `png`, `jpg` or `webp` - also the output file's extension. */
    val format: String,
    val quality: Int,
    /** null = the app's «Ускорение GPU» setting. */
    val gpu: Boolean?,
) {
    /** The outcome of [parse]: a spec, or the message of a `bad_request`. */
    sealed class Parsed {
        data class Ok(val spec: JobSpec) : Parsed()
        data class Bad(val message: String) : Parsed()
    }

    companion object {
        const val DEFAULT_MODEL = "default"
        const val DEFAULT_SCALE = 4
        /** The same 92 as «Сохранить» (UpscaleViewModel.DEFAULT_QUALITY, the accepted mockup 'save'). */
        const val DEFAULT_QUALITY = 92
        val FORMATS = listOf("png", "jpg", "webp")
        /** The owner's scale steps: «x1, x2 (по умолчанию), x4, x8, x16» (interview #002) - UpscaleViewModel.SCALES. */
        val SCALES = listOf(1, 2, 4, 8, 16)
        private val JOB_ID = Regex("[A-Za-z0-9_-]{8,64}")

        /** Validates the intent extras of an UPSCALE start; anything outside the contract is a `bad_request`. */
        fun parse(extras: Map<String, Any?>): Parsed {
            val job = extras["job"] as? String ?: return Parsed.Bad("job is missing")
            if (!JOB_ID.matches(job)) return Parsed.Bad("job must match [A-Za-z0-9_-]{8,64}")
            val input = extras["in"] as? String ?: return Parsed.Bad("in is missing")
            // A bare file name inside files/agent/: no path, no way out of the folder.
            if (input.isEmpty() || '/' in input || '\\' in input || ".." in input) return Parsed.Bad("in must be a bare file name")
            val model = (extras["model"] as? String)?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
            val scale = (extras["scale"] as? Int) ?: DEFAULT_SCALE
            if (scale !in SCALES) return Parsed.Bad("scale must be one of ${SCALES.joinToString(", ")}")
            val format = (extras["format"] as? String)?.lowercase() ?: FORMATS.first()
            if (format !in FORMATS) return Parsed.Bad("format must be one of ${FORMATS.joinToString(", ")}")
            val quality = (extras["quality"] as? Int) ?: DEFAULT_QUALITY
            if (quality !in 1..100) return Parsed.Bad("quality must be 1..100")
            val gpu = extras["gpu"] as? Boolean
            return Parsed.Ok(JobSpec(job, input, model, scale, format, quality, gpu))
        }
    }
}

/** The names of a job's files in `files/agent/`. */
object JobFiles {
    fun status(job: String) = "$job.json"
    fun output(job: String, format: String) = "$job.out.$format"
    /** The input the CLI pushed: `<job>.in.<ext>`. */
    fun isInputOf(name: String, job: String) = name.startsWith("$job.in.")
}

/** The `<job>.json` the app writes (plans/07 protocol block). [out] and [error] are null until the job ends. */
data class JobStatus(
    val job: String,
    val state: JobState,
    val model: String,
    val scale: Int,
    val format: String,
    val gpu: Boolean,
    val progress: Float = 0f,
    val tilesDone: Int = 0,
    val tilesTotal: Int = 0,
    val elapsedMs: Long = 0,
    val estimateMs: Long = 0,
    val inW: Int? = null,
    val inH: Int? = null,
    val out: Out? = null,
    val error: Error? = null,
) {
    data class Out(val file: String, val w: Int, val h: Int, val bytes: Long)
    data class Error(val code: String, val message: String)

    fun toJson(): String = buildString {
        append("{\"v\":1,\"job\":").append(q(job))
        append(",\"state\":").append(q(state.wire))
        append(",\"model\":").append(q(model))
        append(",\"scale\":").append(scale)
        append(",\"format\":").append(q(format))
        append(",\"gpu\":").append(gpu)
        append(",\"progress\":").append("%.3f".format(java.util.Locale.ROOT, progress))
        append(",\"tiles_done\":").append(tilesDone)
        append(",\"tiles_total\":").append(tilesTotal)
        append(",\"elapsed_ms\":").append(elapsedMs)
        append(",\"estimate_ms\":").append(estimateMs)
        append(",\"in\":").append(if (inW != null && inH != null) "{\"w\":$inW,\"h\":$inH}" else "null")
        append(",\"out\":").append(out?.let { "{\"file\":${q(it.file)},\"w\":${it.w},\"h\":${it.h},\"bytes\":${it.bytes}}" } ?: "null")
        append(",\"error\":").append(error?.let { "{\"code\":${q(it.code)},\"message\":${q(it.message)}}" } ?: "null")
        append('}')
    }
}

/** A JSON string literal: quotes, backslashes and control characters escaped. */
fun q(s: String): String = buildString {
    append('"')
    for (c in s) when {
        c == '"' -> append("\\\"")
        c == '\\' -> append("\\\\")
        c == '\n' -> append("\\n")
        c == '\r' -> append("\\r")
        c == '\t' -> append("\\t")
        c < ' ' -> append("\\u%04x".format(c.code))
        else -> append(c)
    }
    append('"')
}
