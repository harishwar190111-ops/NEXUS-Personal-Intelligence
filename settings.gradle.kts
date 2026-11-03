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

rootProject.name = "NEXUS-Personal-Intelligence"

include(":app-tv")
include(":app-mobile")
include(":core:common")
include(":core:remote")
include(":core:ai")
include(":core:voice")
include(":core:memory")
include(":core:automation")
include(":core:design")
include(":core:storage")
