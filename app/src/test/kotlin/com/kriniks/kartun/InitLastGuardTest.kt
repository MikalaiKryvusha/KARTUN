package com.kriniks.kartun

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Guard for the first-launch crash class of 2026-10-16: Kotlin runs property initializers and `init` blocks in source
 * order, so an `init` that reaches a property declared BELOW it finds that property still null. Twice in one day the
 * first launch died in the ViewModel constructor this way (`benchRunning` 20:06 on the Redmi; `benchSaver` 22:32 in RC4 on
 * the Titan and the Redmi). The rule by form: a class-level `init` block stands below every class-level property.
 *
 * @guard init-last
 * THREAT:         a class-level `val`/`var` declared below a class-level `init {` in app sources (the RC4 launch crash)
 * PROVED-AGAINST: the sources of commit 3c8ca15 (UpscaleViewModel: `benchSaver`, `saverWatch` and five more below `init`)
 * GAP:            only 4-space-indented members of top-level classes are read; a nested class's init is not seen
 * ON-REAL-PATH:   NOT YET
 */
class InitLastGuardTest {
    private val roots = listOf(File("src/main/kotlin"), File("src/debug/kotlin"), File("src/nodebug/kotlin"))
    private val init = Regex("""^    init\s*\{""")
    private val property = Regex("""^    (?:(?:private|internal|protected|public|override|lateinit|const|open)\s+)*(?:val|var)\s""")
    private val classEnd = Regex("""^(}|    companion object)""")

    private fun offenders(): List<String> = roots.flatMap { r -> r.walk().filter { it.extension == "kt" }.toList() }.flatMap { f ->
        var afterInit = false
        f.readLines().mapIndexedNotNull { i, line ->
            when {
                init.containsMatchIn(line) -> { afterInit = true; null }
                classEnd.containsMatchIn(line) -> { afterInit = false; null }
                afterInit && property.containsMatchIn(line) -> "${f.path}:${i + 1}: ${line.trim()}"
                else -> null
            }
        }
    }

    @Test fun initStandsBelowEveryProperty() {
        assertEquals("class-level property declared below `init` - still null while init runs", emptyList<String>(), offenders())
    }
}
