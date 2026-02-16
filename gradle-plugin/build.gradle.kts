plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    alias(libs.plugins.shadow)
    id("com.gradle.plugin-publish") version "1.2.1"
    `maven-publish`
}

dependencies {
    // Use JUnit test framework for unit tests
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(gradleTestKit())
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":figex-core"))
}

group = "com.iodigital.figex"

@Suppress("UnstableApiUsage")
gradlePlugin {
    website = "https://github.com/iodigital-com/figex"
    vcsUrl = "https://github.com/iodigital-com/figex"

    val figex by plugins.creating {
        id = "com.iodigital.figex"
        implementationClass = "com.iodigital.figex.FigExPlugin"
        displayName = "Gradle Figma Exporter"
        description = "Export colors, floats, icons and text styles from Figma design files"
        tags = listOf("figma", "export", "design", "icons")
    }
}

// Shadow JAR configuration: bundles all dependencies and relocates their packages
// to avoid conflicts with Gradle's embedded kotlin-stdlib.
// See PROBLEM_ANALYSIS.md for details on why this is needed.
tasks.shadowJar {
    archiveClassifier.set("")

    val prefix = "com.iodigital.figex.shadow"

    // Kotlin stdlib & coroutines
    relocate("kotlin", "$prefix.kotlin")
    relocate("kotlinx", "$prefix.kotlinx")

    // Ktor HTTP client
    relocate("io.ktor", "$prefix.io.ktor")

    // OkHttp + Okio (Ktor engine)
    relocate("okhttp3", "$prefix.okhttp3")
    relocate("okio", "$prefix.okio")

    // Jinjava template engine + transitive deps
    relocate("com.hubspot.jinjava", "$prefix.com.hubspot.jinjava")
    relocate("com.google", "$prefix.com.google")
    relocate("com.fasterxml", "$prefix.com.fasterxml")
    relocate("org.jsoup", "$prefix.org.jsoup")
    relocate("javassist", "$prefix.javassist")

    // Logging
    relocate("org.slf4j", "$prefix.org.slf4j")

    mergeServiceFiles()
}

// Disable the standard jar — shadowJar replaces it as the main artifact
tasks.jar {
    enabled = false
}

// Wire the shadow JAR into configurations so publications and tests use it
afterEvaluate {
    configurations.apiElements.get().outgoing.apply {
        artifacts.clear()
        artifact(tasks.shadowJar)
    }
    configurations.runtimeElements.get().outgoing.apply {
        artifacts.clear()
        artifact(tasks.shadowJar)
    }
}

// Add a source set and a task for a functional test suite
val functionalTest by sourceSets.creating
gradlePlugin.testSourceSets(functionalTest)

configurations[functionalTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())

val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath =
        configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output
}

tasks.check {
// Run the functional tests as part of `check`
    dependsOn(functionalTestTask)
}

kotlin {
    jvmToolchain(17)
}
