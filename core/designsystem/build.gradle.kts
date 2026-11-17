plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.ao3m3.core.designsystem"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":core:model"))
    api(platform(libs.compose.bom))
    api(libs.compose.material3)
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.compose.ui.tooling.preview)
    api(libs.compose.icons.extended)
    api(libs.compose.adaptive)
    api(libs.compose.adaptive.layout)
    implementation(libs.androidx.core.ktx)
    debugImplementation(libs.compose.ui.tooling)
}
