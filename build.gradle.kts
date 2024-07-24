plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinSerialization).apply(false)
    alias(libs.plugins.jetbrainsKotlinJvm) apply false
}


val buildNumber = (findProperty("AzureBuildNumber") ?: "debug").toString().replace(".", "-")
val version = "1.0.$buildNumber"

println("##vso[build.updatebuildnumber]name=$version,code=$version,buildId=$buildNumber")
File(System.getenv("GITHUB_OUTPUT") ?: "/dev/null").appendText("build_version=$version")

project.ext["version"] = version