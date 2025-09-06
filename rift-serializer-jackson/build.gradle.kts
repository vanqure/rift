plugins {
    id("io.github.rift.java")
    id("io.github.rift.publish")
}

dependencies {
    api(project(":rift-serializer-common"))
    api(libs.jackson.databind)
}

riftPublish {
    artifactId = "rift-serializer-jackson"
}