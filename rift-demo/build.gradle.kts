plugins {
    id("io.github.rift.java")
}

dependencies {
    api(project(":rift-serializer-jackson"))
    api(project(":rift-bridge-redis"))
    api(libs.caffeine)
}