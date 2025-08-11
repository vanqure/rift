plugins {
    `rift-java`
    `rift-repositories`
}

dependencies {
    api(project(":rift-common"))
    api("io.lettuce:lettuce-core:6.8.0.RELEASE")
}