pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "glide"
include(":shared")
include(":backend")

// The backend Docker image has no Android SDK; it sets GLIDE_BACKEND_ONLY=true to skip the app.
if (providers.environmentVariable("GLIDE_BACKEND_ONLY").orNull != "true") {
    include(":android:app")
}
