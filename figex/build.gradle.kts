plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.mavenPublishing)
    application
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.cli)
            implementation(libs.ktor.core)
            implementation(project(":figex-core"))
        }
    }
}

distributions {
    main {
        distributionBaseName.set("figex")
        contents {
            into("") {
                val jvmJar by tasks.getting
                from(jvmJar)
                from("src/figex")
            }
            into("lib/") {
                val main by kotlin.jvm().compilations.getting
                from(main.runtimeDependencyFiles)
            }
            exclude("**/figma-exporter")
            exclude("**/figma-exporter.bat")
        }
    }
}

tasks.withType<Jar> {
    doFirst {
        manifest {
            val main by kotlin.jvm().compilations.getting
            attributes(
                "Main-Class" to "com.iodigital.figex.MainKt",
                "Class-Path" to main.runtimeDependencyFiles.files.joinToString(" ") { "lib/" + it.name }
            )
        }
    }
}