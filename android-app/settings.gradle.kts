pluginManagement {
    repositories {
        google()
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

rootProject.name = "car-owner-assistant-android"

include(
    ":app",
    ":core-common",
    ":core-model",
    ":core-navigation",
    ":core-flags",
    ":core-database",
    ":core-datastore",
    ":core-files",
    ":feature-onboarding",
    ":feature-vehicle",
    ":feature-mileage",
    ":feature-fuel",
    ":feature-expense",
    ":feature-service",
    ":feature-statistics",
    ":feature-settings",
    ":feature-backup",
    ":feature-search",
    ":contract-auth",
    ":contract-cloud-sync",
    ":contract-premium",
    ":contract-family"
)
