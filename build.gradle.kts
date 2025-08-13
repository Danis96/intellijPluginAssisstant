plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "org.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
    type.set("IC")
    version.set("2024.2")
    plugins.set(listOf("java"))
}

dependencies {
    testImplementation(kotlin("test"))
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
}

tasks {
    patchPluginXml {
        sinceBuild.set("242")
        changeNotes.set(
            """
            Initial AI Assistant with access token verification.
            """.trimIndent()
        )
    }
    runIde {
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