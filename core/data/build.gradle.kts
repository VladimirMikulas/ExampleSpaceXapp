plugins {
    id("template.kotlin.feature")
    id("template.coroutines")
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

dependencies {
    implementation(project(":core:commons"))
    implementation(project(":core:domain"))
    api(libs.bundles.network)
    implementation(libs.datastore)

    testImplementation(project(":core:data-test"))
}
