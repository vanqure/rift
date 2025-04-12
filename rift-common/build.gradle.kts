plugins {
    `rift-java`
    `rift-repositories`
}

dependencies {
    api(project(":rift-codec"))
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
}