plugins {
    `rift-java`
    `rift-repositories`
}

dependencies {
    api(project(":rift-codec"))
    api("com.fasterxml.jackson.core:jackson-databind:2.19.2")
}