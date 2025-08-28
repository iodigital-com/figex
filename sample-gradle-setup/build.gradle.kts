plugins {
    id("com.iodigital.figex") version "1.0.16936499539"
}

figex {
    figmaToken = rootProject.rootDir.resolve("../figex-cli/.figmatoken").readText().trim()
    configFile = rootProject.rootDir.resolve("../samples/config.json")
    ignoreUnsupportedLinks = true
}