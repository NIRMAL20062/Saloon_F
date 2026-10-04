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
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.body.limit)
    implementation(libs.ktor.server.auth)
    implementation(libs.nimbus.jose.jwt)
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
    testImplementation(libs.openapi.validator)
}

// Local dev convenience: `./gradlew :backend:run` (and the commands below) pick up the repo-root .env file.
// Deployed environments set real environment variables instead.
fun JavaExec.useDotEnv() {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile
            .readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
            .forEach { line -> environment(line.substringBefore('=').trim(), line.substringAfter('=').trim()) }
    }
}

tasks.named<JavaExec>("run") { useDotEnv() }

// One-off: make the first admin (BE-020). ./gradlew :backend:addFirstAdmin --args="admin@example.com"
tasks.register<JavaExec>("addFirstAdmin") {
    group = "glide"
    description = "Makes the first admin of the admin website (later admins are invited there)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.glide.backend.admins.AddFirstAdminKt")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    useDotEnv()
}

// Bake the build version into the jar so /health can report which build is running.
tasks.processResources {
    val appVersion = project.version.toString()
    inputs.property("version", appVersion)
    filesMatching("version.properties") { expand("version" to appVersion) }
}

tasks.test {
    useJUnitPlatform()
    // The contract test validates real responses against the API spec; re-run it whenever the spec changes.
    val openApiSpec = rootProject.file("docs/api/openapi.yaml")
    inputs.file(openApiSpec).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("openapi.spec", openApiSpec.absolutePath)
}
