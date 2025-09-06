plugins {
    id("io.github.rift.java")
    id("io.github.rift.publish")
}

dependencies {
    api(project(":rift-codec-common"))
    compileOnly(libs.caffeine)
}

riftPublish {
    artifactId = "rift-bridge-common"
}