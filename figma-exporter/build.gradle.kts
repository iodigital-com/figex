import java.util.Properties

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
            implementation(libs.ktor.logging)
            implementation(libs.ktor.contentnegotiation)
            implementation(libs.ktor.encoding)
            implementation(libs.ktor.serializationjson)
            implementation(libs.kotlinx.serialization)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.okhttp)
            implementation(libs.jinjava)
            implementation(libs.slf4j)
            implementation(libs.android.common)
            implementation(libs.android.tools)
            implementation(libs.twelvemonkeys.core)
            implementation(libs.twelvemonkeys.webp)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

val buildNumber = (project.findProperty("AzureBuildNumber") ?: "debug")
    .toString().replace(".", "-")

group = "com.iodigital"
version = "1.0.$buildNumber"

println("::set-output name=build_version::$version")
println("##vso[build.updatebuildnumber]name=${version},code=${buildNumber},buildId=${buildNumber}")

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

publishing {
    repositories {
        getLocalProperty("azure.url")?.let { uri(it) }?.let { azureUrl ->
            maven {
                url = azureUrl
                name = "Azure"
                credentials {
                    username = "iodigital"
                    password = getLocalProperty("azure.accessToken")
                        ?: System.getenv("SYSTEM_ACCESSTOKEN")
                                ?: throw IllegalStateException("Missing Azure token: Add to local.properties as 'azure.token=xxxxxx' or specify in SYSTEM_ACCESSTOKEN environment variable")
                }
            }
        }
    }
}

fun getLocalProperty(key: String): String? {
    val localPropertiesFile = File(rootDir, "local.properties")
    val props = Properties().apply {
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.reader())
        } else {
            println("WARNING: Missing $localPropertiesFile, did not check for Azure token")
        }
    }

    return props.getProperty(key)
}