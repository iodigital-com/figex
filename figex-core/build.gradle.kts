import java.util.Properties

plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.mavenPublishing)
    `java-library`
}

repositories {
    mavenCentral()
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
   publications {

   }
}
