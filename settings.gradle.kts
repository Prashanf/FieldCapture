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
        maven {
            url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
            credentials {
                // TODO: If GitHub Packages requires authentication, set gpr.user and gpr.token in local.properties or environment variables
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_USER") ?: ""
                password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}

rootProject.name = "FieldCapture"
include(":app")
