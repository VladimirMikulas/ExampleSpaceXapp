pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SpaceXApp"
include(
    ":androidApp",
    ":core:commons",
    ":core:domain",
    ":core:data",
)

// testing helper submodules
include(
    ":core:data-test",
)
