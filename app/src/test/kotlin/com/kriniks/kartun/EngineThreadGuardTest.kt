package com.kriniks.kartun

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Guard for bugs/01: the engine (static libomp inside) is called from ONE process-wide thread that is never stopped.
 * A screen that makes its own engine thread and stops it lets libomp shut down; the next thread's model load then aborts
 * in `kmp_affinity.cpp(4755)`.
 *
 * @guard engine-thread-single
 * THREAT:         a new executor for engine calls, or the shared one shut down / closed, in app sources
 * PROVED-AGAINST: the sources of commit eda6a40 (UpscaleViewModel's own `newSingleThreadExecutor` and `engineThread.close()`)
 * GAP:            a raw `Thread { … }` calling the engine is not seen; nor an engine call from a coroutine pool
 * ON-REAL-PATH:   NOT YET
 */
class EngineThreadGuardTest {
    private val roots = listOf(File("src/main/kotlin"), File("src/debug/kotlin"))
    private val executor = Regex("""Executors\.new\w+""")
    private val stop = Regex("""(?i)engine\w*\.(executor\.)?(close|shutdown\w*)\(""")

    private fun offenders(): List<String> = roots.flatMap { r -> r.walk().filter { it.extension == "kt" }.toList() }.flatMap { f ->
        f.readLines().mapIndexedNotNull { i, line ->
            val code = line.substringBefore("//")
            val badExecutor = executor.containsMatchIn(code) && f.name != "EngineThread.kt"
            if (badExecutor || stop.containsMatchIn(code)) "${f.path}:${i + 1}: ${line.trim()}" else null
        }
    }

    @Test fun theEngineHasOneThreadAndNobodyStopsIt() {
        assertEquals("engine thread made or stopped outside EngineThread.kt (bugs/01)", emptyList<String>(), offenders())
    }
}
