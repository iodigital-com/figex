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
    publishToMavenCentral(host = SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    coordinates("com.iodigital", "figex-core", version as String)
    pom {
        name = "FigEx Core"
        description = "FigEx is a utility tool to export styles and icons from Figma using the Figma REST API."
        inceptionYear = "2024"
        url = "https://github.com/iodigital-com/figex"
    }
}