plugins {
    id("io.github.rift.java")
}

dependencies {
    api(project(":rift-codec-jackson"))
    api(project(":rift-bridge-redis"))
    api(libs.caffeine)
}