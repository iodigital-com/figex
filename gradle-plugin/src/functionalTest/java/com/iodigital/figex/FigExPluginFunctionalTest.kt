package com.iodigital.figex

import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files

class FigExPluginFunctionalTest {

    @Test
    @Throws(IOException::class)
    fun canRunTask() {
        // Setup the test build
        val projectDir = File("build/functionalTest")
        setupTestProject(projectDir)

        // Run the build
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("exportFigma")
            .withProjectDir(projectDir)
            .build()

        // Check output (very rudamentary)
        val actual = File(projectDir, "full_dump.json").load()
        val expected = File("src/functionalTest/expected_full_dump.json.txt").load()
        assert(actual == expected) { "Expected result hash to be ${actual.hashCode()}, but was ${expected.hashCode()}"}
    }

    private fun File.load() = readText().filter { !it.isWhitespace() }

    private fun setupTestProject(projectDir: File) {
        val configFile = File(projectDir, "config.json")
        val token = File("../figex-cli/.figmatoken").readText().trim()
        Files.createDirectories(projectDir.toPath())
        writeString(File(projectDir, "settings.gradle"), "")
        writeString(
            configFile, """
            {
                "figmaFileKey": "pzylpIQutSDHgGaiLw0F29",
                "modeAliases": {
                    "209:0": "english",
                    "209:1": "german",
                    "209:2": "japanese",
                    "209:3": "standard",
                    "209:4": "expanded",
                    "209:5": "condensed",
                    "1125:0": "small",
                    "1125:1": "large",
                    "192:0": "light",
                    "194:4": "dark",
                    "59:0": "unused",
                    "59:3": "unused"
                },
                "exports": [
                    {
                        "type": "values",
                        "templatePath": "full_dump.json.figex",
                        "destinationPath": "full_dump.json"
                    }
                ]
            }
        """.trimIndent()
        )
        writeString(
            File(projectDir, "build.gradle"),
            """
                plugins {
                    id('com.iodigital.figex')
                }
                
                figex {
                    figmaToken = "$token"
                    configFile = file("${configFile.absolutePath}")
                    ignoreUnsupportedLinks = true
                }
            """.trimIndent()
        )

        File("../samples/full_dump.json.figex")
            .copyTo(File(projectDir, "full_dump.json.figex"), overwrite = true)

    }

    @Throws(IOException::class)
    private fun writeString(file: File, string: String) {
        FileWriter(file).use { writer ->
            writer.write(string)
        }
    }
}
