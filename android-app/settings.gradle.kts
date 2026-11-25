pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Espejo público de Maven Central en Google Cloud Storage: Maven Central responde 429 a los
        // runners del CI con frecuencia. Si el espejo no tiene algo, se usa Maven Central.
        maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "MavenCentralGoogleMirror" }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "MavenCentralGoogleMirror" }
        mavenCentral()
    }
}

rootProject.name = "android-app"
include(":app")
