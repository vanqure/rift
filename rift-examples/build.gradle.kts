plugins {
    `rift-java`
    `rift-repositories`
}

dependencies {
    api(project(":rift-codec-jackson"))
    api(project(":rift-redis"))
    api("com.github.ben-manes.caffeine:caffeine:3.2.0")
}