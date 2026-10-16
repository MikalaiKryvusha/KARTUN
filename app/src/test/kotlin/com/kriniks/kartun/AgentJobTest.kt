package com.kriniks.kartun

import com.kriniks.kartun.agent.JobFiles
import com.kriniks.kartun.agent.JobSpec
import com.kriniks.kartun.agent.JobState
import com.kriniks.kartun.agent.JobStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The job contract of the ADB door (plans/07 step 3, criterion A6): what an agent may ask and what the app answers.
 *
 * @guard agent-job-contract
 * THREAT:         an agent's `in` reaching outside files/agent/ (`../`, a path), or a status file that is not valid JSON
 * PROVED-AGAINST: the `..` check removed from JobSpec.parse - pathTraversalIsRefused went red; restored
 * GAP:            the service's use of the spec (step 4) is not covered here
 * ON-REAL-PATH:   NOT YET - the A6 run of step 9
 */
class AgentJobTest {
    private val valid = mapOf("job" to "k3x9a1f0c2", "in" to "k3x9a1f0c2.in.jpg", "model" to "default", "scale" to 4, "format" to "png", "quality" to 92)

    private fun bad(extras: Map<String, Any?>) = JobSpec.parse(extras) as? JobSpec.Parsed.Bad

    @Test fun aValidSpecParses() {
        val ok = JobSpec.parse(valid) as JobSpec.Parsed.Ok
        assertEquals(JobSpec("k3x9a1f0c2", "k3x9a1f0c2.in.jpg", "default", 4, "png", 92, null), ok.spec)
    }

    @Test fun defaultsFillTheOptionalExtras() {
        val ok = JobSpec.parse(mapOf("job" to "abcdefgh", "in" to "abcdefgh.in.png")) as JobSpec.Parsed.Ok
        assertEquals(JobSpec("abcdefgh", "abcdefgh.in.png", "default", 4, "png", 92, null), ok.spec)
    }

    @Test fun pathTraversalIsRefused() {
        assertTrue(bad(valid + ("in" to "../x.png")) != null)
        assertTrue(bad(valid + ("in" to "..")) != null)
    }

    @Test fun aPathIsRefused() {
        assertTrue(bad(valid + ("in" to "a/b.png")) != null)
        assertTrue(bad(valid + ("in" to "a\\b.png")) != null)
    }

    @Test fun aScaleOutsideTheStepsIsRefused() {
        assertEquals("scale must be one of 1, 2, 4, 8, 16", bad(valid + ("scale" to 3))?.message)
    }

    @Test fun anUnknownFormatIsRefused() {
        assertEquals("format must be one of png, jpg, webp", bad(valid + ("format" to "gif"))?.message)
    }

    @Test fun aShortJobIdIsRefused() {
        assertTrue(bad(valid + ("job" to "abcdefg")) != null)
        assertTrue(bad(valid + ("job" to "abc def gh")) != null)
    }

    @Test fun qualityOutsideOneToHundredIsRefused() {
        assertTrue(bad(valid + ("quality" to 0)) != null)
        assertTrue(bad(valid + ("quality" to 101)) != null)
    }

    @Test fun aStatusEscapesQuotesAndNewlines() {
        val s = JobStatus("k3x9a1f0c2", JobState.Failed, "default", 4, "png", true,
            error = JobStatus.Error("decode_failed", "bad \"file\"\nline 2"))
        assertEquals(
            "{\"v\":1,\"job\":\"k3x9a1f0c2\",\"state\":\"failed\",\"model\":\"default\",\"scale\":4,\"format\":\"png\",\"gpu\":true," +
                "\"progress\":0.000,\"tiles_done\":0,\"tiles_total\":0,\"elapsed_ms\":0,\"estimate_ms\":0,\"in\":null,\"out\":null," +
                "\"error\":{\"code\":\"decode_failed\",\"message\":\"bad \\\"file\\\"\\nline 2\"}}",
            s.toJson(),
        )
    }

    @Test fun aSucceededStatusCarriesTheOutput() {
        val s = JobStatus("k3x9a1f0c2", JobState.Succeeded, "realesrgan-x4plus", 4, "png", true, progress = 1f,
            tilesDone = 50, tilesTotal = 50, inW = 256, inH = 256, out = JobStatus.Out("k3x9a1f0c2.out.png", 1024, 1024, 123))
        val json = s.toJson()
        assertTrue(json, json.contains("\"state\":\"succeeded\""))
        assertTrue(json, json.contains("\"progress\":1.000"))
        assertTrue(json, json.contains("\"in\":{\"w\":256,\"h\":256}"))
        assertTrue(json, json.contains("\"out\":{\"file\":\"k3x9a1f0c2.out.png\",\"w\":1024,\"h\":1024,\"bytes\":123}"))
    }

    @Test fun fileNames() {
        assertEquals("k3x9a1f0c2.json", JobFiles.status("k3x9a1f0c2"))
        assertEquals("k3x9a1f0c2.out.webp", JobFiles.output("k3x9a1f0c2", "webp"))
        assertTrue(JobFiles.isInputOf("k3x9a1f0c2.in.jpg", "k3x9a1f0c2"))
    }
}
