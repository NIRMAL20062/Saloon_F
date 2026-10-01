import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

version = "0.1.0"

application {
    mainClass.set("com.glide.backend.ApplicationKt")
    // Netty loads native transport libraries; newer JDKs warn unless this is set.
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjdk-release=21")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(project(":shared"))

    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    implementation(libs.hikari)

    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}

// Local dev convenience: `./gradlew :backend:run` picks up the repo-root .env file.
// Deployed environments set real environment variables instead.
tasks.named<JavaExec>("run") {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile
            .readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
            .forEach { line -> environment(line.substringBefore('=').trim(), line.substringAfter('=').trim()) }
    }
}

// Bake the build version into the jar so /health can report which build is running.
tasks.processResources {
    val appVersion = project.version.toString()
    inputs.property("version", appVersion)
    filesMatching("version.properties") { expand("version" to appVersion) }
}

tasks.test {
    useJUnitPlatform()
}
