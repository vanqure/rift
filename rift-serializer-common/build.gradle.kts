plugins {
    id("io.github.rift.java")
    id("io.github.rift.publish")
}

dependencies {
    api(libs.wisp)
}

riftPublish {
    artifactId = "rift-serializer-common"
}