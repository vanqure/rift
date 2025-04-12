plugins {
    `java-library`
}

group = "dev.esoterik.rift"
version = "1.0.0-SNAPSHOT"

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(emptyList<String>())
    }
    test {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}