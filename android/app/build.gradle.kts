plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.foodplan.ai"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.foodplan.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "0.4.1-cart-handoff-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // Signing is strictly opt-in via protected CI environment variables.
    // Normal PRs keep the existing unsigned release packaging gate.
    // Never put a production keystore or password into this repository.
    val releaseSigningVars = mapOf(
        "FOODPLAN_RELEASE_KEYSTORE_PATH" to System.getenv("FOODPLAN_RELEASE_KEYSTORE_PATH"),
        "FOODPLAN_RELEASE_STORE_PASSWORD" to System.getenv("FOODPLAN_RELEASE_STORE_PASSWORD"),
        "FOODPLAN_RELEASE_KEY_ALIAS" to System.getenv("FOODPLAN_RELEASE_KEY_ALIAS"),
        "FOODPLAN_RELEASE_KEY_PASSWORD" to System.getenv("FOODPLAN_RELEASE_KEY_PASSWORD")
    )
    val suppliedSigningVars = releaseSigningVars.values.count { !it.isNullOrBlank() }
    require(suppliedSigningVars == 0 || suppliedSigningVars == 4) {
        "Incomplete secure release signing configuration"
    }
    if (suppliedSigningVars == 4) {
        signingConfigs {
            create("foodplanPersistentRelease") {
                storeFile = file(requireNotNull(releaseSigningVars["FOODPLAN_RELEASE_KEYSTORE_PATH"]))
                storePassword = releaseSigningVars["FOODPLAN_RELEASE_STORE_PASSWORD"]
                keyAlias = releaseSigningVars["FOODPLAN_RELEASE_KEY_ALIAS"]
                keyPassword = releaseSigningVars["FOODPLAN_RELEASE_KEY_PASSWORD"]
            }
        }
        buildTypes.getByName("release").signingConfig =
            signingConfigs.getByName("foodplanPersistentRelease")
    }
}
dependencies {
    implementation(project(":domain"))
    implementation(platform("androidx.compose:compose-bom:2025.09.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("org.jsoup:jsoup:1.18.3")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.09.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
