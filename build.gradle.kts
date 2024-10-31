plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.jetbrainsKotlinJvm) apply false
    alias(libs.plugins.mavenPublishing) apply false
}


val buildNumber = (findProperty("AzureBuildNumber") ?: "debug").toString().replace(".", "-")
val buildVersion = "${project.property("VERSION_NAME")}.$buildNumber"

println("##vso[build.updatebuildnumber]name=$buildVersion,code=$buildVersion,buildId=$buildNumber")
File(System.getenv("GITHUB_OUTPUT") ?: "/dev/null").appendText("build_version=$buildVersion")

subprojects {
    version = buildVersion
}