plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    id("com.gradle.plugin-publish") version "1.2.1"
    `maven-publish`
}

dependencies {
    // Use JUnit test framework for unit tests
    testImplementation(libs.junit)
    testImplementation(libs.truth)
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