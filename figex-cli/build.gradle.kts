plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

dependencies {
    implementation(libs.kotlinx.cli)
    implementation(libs.ktor.core)
    implementation(project(":figex-core"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "com.iodigital.figex.MainKt"
}

distributions {
    main {
        distributionBaseName.set("figex")
        contents {
            into("") {
                from(tasks.jar)
                from("src/figex")
            }
            exclude("**/figma-exporter")
            exclude("**/figma-exporter.bat")
        }
    }
}

tasks.jar {
    val runtimeClasspath = configurations.runtimeClasspath
    doFirst {
        manifest {
            attributes(
                "Main-Class" to "com.iodigital.figex.MainKt",
                "Class-Path" to runtimeClasspath.get().files.joinToString(" ") { "lib/" + it.name }
            )
        }
    }
}
