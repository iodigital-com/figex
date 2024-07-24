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
        val configFile = File(projectDir, "config.json")
        val token = File("../figex/.figmatoken").readText().trim()
        Files.createDirectories(projectDir.toPath())
        writeString(File(projectDir, "settings.gradle"), "")
        writeString(configFile, """
            {
              "figmaFileKey": "vtX0CbFSpv5tt5D41fL3xe",
              "exports": []
            }
        """.trimIndent())
        writeString(
            File(projectDir, "build.gradle"),
            """
                plugins {
                    id('com.iodigital.figex')
                }
                
                figex {
                    figmaToken = "$token"
                    configFile = file("${configFile.absolutePath}")
                }
            """.trimIndent()
        )

        // Run the build
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("exportFigma")
            .withProjectDir(projectDir)
            .build()


//        // Verify the result
//        val bomFile = File("${projectDir.absolutePath}/build/outputs/bom.json")
//        val bom = bomFile.reader().readText().stabilizeUuid()
//        val expectedBom = File("test-inputs/expected-bom.json").reader().readText().stabilizeUuid()
//        val expectedOutput = "Wrote BOM file to: $bomFile"
//        Assert.assertTrue(
//            "Didn't find in output: \"$expectedOutput\"",
//            result.output.contains(expectedOutput),
//        )
//        Assert.assertEquals(
//            "Expected BOM to be correct, but got",
//            expectedBom,
//            bom
//        )
    }

    @Throws(IOException::class)
    private fun writeString(file: File, string: String) {
        FileWriter(file).use { writer ->
            writer.write(string)
        }
    }
}
