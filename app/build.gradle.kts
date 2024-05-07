plugins {
    alias(libs.plugins.android.gradle)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "cz.jaro.dpmcb"
    compileSdk = 34

    defaultConfig {
        applicationId = "cz.jaro.dpmcb"
        minSdk = 26
        targetSdk = 34
        versionCode = 18
        versionName = "1.6.2-alpha.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")

            manifestPlaceholders += "logo" to "@mipmap/logo_chytra_cesta"
            manifestPlaceholders += "logoRound" to "@mipmap/logo_chytra_cesta_round"
        }
        debug {
            applicationIdSuffix = ".debug"

            manifestPlaceholders += "logo" to "@mipmap/logo_jaro"
            manifestPlaceholders += "logoRound" to "@mipmap/logo_jaro_round"
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xcontext-receivers"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    applicationVariants.all {
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    // Core Android
    //noinspection UseTomlInstead
    implementation("androidx.core:core-ktx:${libs.versions.androidx.core}")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // PDF Viewer
    implementation(libs.pdf.viewer)

    // Kotlinx Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Firebase
    implementation(libs.firebase.common)
    // Realtime Databse
    implementation(libs.firebase.database)
    // Storage
    implementation(libs.firebase.storage)
    // Crashlytics
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    // Remote config
    implementation(libs.firebase.config)

    // Jetpack Compose
    implementation(libs.androidx.jetpack.compose.foundation)
    implementation(libs.androidx.jetpack.compose.ui.graphics)
    implementation(libs.androidx.jetpack.compose.ui)
    implementation(libs.androidx.jetpack.compose.ui.tooling)
    // Activity
    implementation(libs.androidx.activity.compose)
    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Jetpack Navigation
    implementation(libs.androidx.jetpack.navigation.compose)
    // Material
    implementation(libs.androidx.jetpack.compose.material3)
    implementation(libs.androidx.jetpack.compose.material.icons.extended)
    // Jetpack Glance
    implementation(libs.androidx.jetpack.glance)
    // 3rd-party
    implementation(libs.compose.material3.datetime.pickers)

    // Jetpack Room
    implementation(libs.androidx.jetpack.room)
    implementation(libs.androidx.jetpack.room.runtime)
    ksp(libs.androidx.jetpack.room.compiler)

    // Jetpack Preferences DataStore
    implementation(libs.androidx.datastore)

    // Insert-Koin
    implementation(libs.koin.android)
    implementation(libs.koin.annotations)
    implementation(libs.koin.navigation)
    implementation(libs.koin.compose)
    ksp(libs.koin.annotations.ksp)

    // Compose Destinations
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization)

    // Semantic versioning
    implementation(libs.semver)

    // Web scaping
    implementation(libs.jsoup)
    implementation(libs.konsume.xml)
    implementation(libs.retrofit)

    implementation(libs.stax.api)
    implementation(libs.aalto.xml)

    implementation(libs.androidx.browser)
}