import com.vanniktech.maven.publish.SonatypeHost

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
    testImplementation(libs.kotlin.test)
}

mavenPublishing {
    coordinates("com.iodigital", "figex-core", version as String)
}