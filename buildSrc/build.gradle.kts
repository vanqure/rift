plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("script-runtime"))
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}