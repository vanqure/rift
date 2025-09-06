plugins {
    id("io.github.rift.java")
    id("io.github.rift.publish")
}

dependencies {
    api(project(":rift-bridge-common"))
    api(libs.lettuce.core)
}

riftPublish {
    artifactId = "rift-bridge-redis"
}