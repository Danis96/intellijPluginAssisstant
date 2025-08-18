plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "org.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
    type.set("IC")
    version.set("2024.1.4") // Stable version
    plugins.set(listOf("java", "Kotlin"))
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("252.*")
        changeNotes.set(
            """
            Initial AI Assistant with access token verification.
            """.trimIndent()
        )
    }
    
    runIde {
        autoReloadPlugins.set(true)
        jvmArgs = listOf(
            "-Xmx1024m",
            "-XX:ReservedCodeCacheSize=256m"
        )
    }

    buildPlugin {
        archiveFileName.set("${project.name}-${project.version}-dev.zip")
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}