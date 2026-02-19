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

        // --- ADDED: MAPBOX REPOSITORY ---
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "mapbox"
                password = java.util.Properties()
                    .apply {
                        val localPropsFile = File(rootDir, "local.properties")
                        if (localPropsFile.exists()) load(java.io.FileInputStream(localPropsFile))
                    }
                    .getProperty("MAPBOX_DOWNLOADS_TOKEN")
            }
        }
        // --------------------------------
    }
}

rootProject.name = "MultiLink"
include(":app")