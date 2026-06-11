package com.aristidevs.habittracker.data.security

import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class HardcodedSecretsTest {

    @Test
    fun projectDoesNotContainHardcodedSecrets() {
        val workingDir = File(System.getProperty("user.dir"))
        val projectRoot = if (workingDir.name == "app") {
            workingDir.parentFile
        } else {
            workingDir
        }

        val forbiddenPatterns = listOf(
            Regex("""(?i)(password|passwd|pwd)\s*=\s*["'][^"']+["']"""),
            Regex("""(?i)(api[_-]?key|firebase[_-]?key|secret|token)\s*[:=]\s*["'][^"']{8,}["']"""),
            Regex("""AIza[0-9A-Za-z_\-]{35}"""),
            Regex("""-----BEGIN (RSA|EC|DSA|OPENSSH) PRIVATE KEY-----""")
        )

        val ignoredDirs = setOf("build", ".gradle", ".git", ".idea")
        val scanExtensions = setOf("kt", "kts", "xml", "json", "properties")
        val findings = projectRoot
            .walkTopDown()
            .onEnter { it.name !in ignoredDirs }
            .filter { it.isFile && it.extension in scanExtensions }
            .flatMap { file ->
                val text = file.readText()
                forbiddenPatterns.mapNotNull { pattern ->
                    pattern.find(text)?.let { match ->
                        "${file.relativeTo(projectRoot)} -> ${match.value.take(80)}"
                    }
                }
            }
            .toList()

        if (findings.isNotEmpty()) {
            fail("Potential hardcoded secrets found:\n${findings.joinToString("\n")}")
        }
    }
}
