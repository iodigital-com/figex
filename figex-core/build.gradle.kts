import java.util.Properties

plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.mavenPublishing)
    `java-library`
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation(libs.ktor.core)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.contentnegotiation)
    implementation(libs.ktor.encoding)
    implementation(libs.ktor.serializationjson)
    implementation(libs.kotlinx.serialization)
    implementation(libs.ktor.okhttp)
    implementation(libs.jinjava)
    implementation(libs.slf4j)
    implementation(libs.android.common)
    implementation(libs.android.tools)
    implementation(libs.twelvemonkeys.core)
    implementation(libs.twelvemonkeys.webp)

    testImplementation(libs.kotlin.test)
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