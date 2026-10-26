package io.kdmtracker.app

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Keeps the production sources of every module readable: no file regrows into a mega-file and
 * comments carry no process history. The reasons live in `docs/ARCHITECTURE.md`.
 */
class SourceHygieneTest {
    private val root: File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }

    /** Hand-written production sources; test source sets and generated content tables are skipped. */
    private val sources: List<File> =
        listOf("core", "data", "app-android", "server")
            .flatMap { module -> File(root, "$module/src").walkTopDown().filter { it.extension == "kt" }.toList() }
            .filter { file ->
                val path = file.relativeTo(root).invariantSeparatorsPath
                val sourceSet = path.substringAfter("/src/").substringBefore('/')
                !sourceSet.contains("test", ignoreCase = true) && "/generated/" !in path
            }

    @Test
    fun sourcesAreFoundSoTheRulesCannotPassVacuously() {
        assertTrue(sources.size > 100, "expected the production sources of all modules, found ${sources.size}")
    }

    @Test
    fun noFileExceedsTheSizeLimit() {
        val offenders =
            sources.mapNotNull { file ->
                val path = file.relativeTo(root).invariantSeparatorsPath
                val limit = KNOWN_OVERSIZED[path] ?: MAX_LINES
                val lines = file.readLines().size
                "$path: $lines lines (limit $limit)".takeIf { lines > limit }
            }
        assertTrue(offenders.isEmpty(), "Split these files instead of growing them:\n" + offenders.joinToString("\n"))
    }

    @Test
    fun commentsCarryNoProcessHistory() {
        val offenders =
            sources.flatMap { file ->
                val path = file.relativeTo(root).invariantSeparatorsPath
                file.readLines().mapIndexedNotNull { index, line ->
                    val comment = commentPart(line) ?: return@mapIndexedNotNull null
                    val hit =
                        PROCESS_HISTORY.firstOrNull { it.containsMatchIn(comment) } ?: return@mapIndexedNotNull null
                    "$path:${index + 1}: matches ${hit.pattern}"
                }
            }
        assertTrue(
            offenders.isEmpty(),
            "Comments say why, not which task, ruling or PR asked for it (see docs/ARCHITECTURE.md):\n" +
                offenders.joinToString("\n"),
        )
    }

    /** The comment text of [line]: the whole line inside a block comment, or the part after a trailing `//`. */
    private fun commentPart(line: String): String? {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) return trimmed
        val trailing = Regex("""(?<!:)//""").find(line) ?: return null
        return line.substring(trailing.range.first)
    }

    private companion object {
        const val MAX_LINES = 500

        /** Files over [MAX_LINES] today. They may not grow; split them and delete the entry. */
        val KNOWN_OVERSIZED = mapOf("server/src/main/kotlin/io/kdmtracker/web/DashboardController.kt" to 600)

        val PROCESS_HISTORY =
            listOf(
                Regex("""Ruling [A-Z]{1,2}\b"""),
                Regex("""Review Focus"""),
                Regex("""\bS[0-9]{1,2}\b"""),
                Regex("""PR #[0-9]+"""),
                Regex("""\bODD\b"""),
            )
    }
}
