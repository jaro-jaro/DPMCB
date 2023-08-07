plugins {
    id("com.android.application")
    kotlin("android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp") version "1.8.20-1.0.11"
    kotlin("plugin.serialization")
    id("kotlin-parcelize")
}

android {
    namespace = "cz.jaro.dpmcb"
    compileSdk = 34

    defaultConfig {
        applicationId = "cz.jaro.dpmcb"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.3.0-alpha.6"

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

            manifestPlaceholders += "logo" to "@mipmap/logo_dpmcb"
            manifestPlaceholders += "logoRound" to "@mipmap/logo_dpmcb_round"
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
        viewBinding = true
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.6"
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
        sourceSets {
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
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    // Material
    implementation("com.google.android.material:material:1.9.0-beta01")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // PDF Viewer
    implementation("com.github.barteksc:android-pdf-viewer:2.8.2")

    // Kotlinx Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Jetpack Navigation
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:31.1.0"))
    implementation("com.google.firebase:firebase-common-ktx")
    // Realtime Databse
    implementation("com.google.firebase:firebase-database-ktx")
    // Storage
    implementation("com.google.firebase:firebase-storage-ktx")
    // Crashlytics
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2022.10.00"))
    implementation("androidx.compose.foundation:foundation:1.4.3")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")
    // Activity
    implementation("androidx.activity:activity-compose:1.7.1")
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    // Jetpack Navigation
    implementation("androidx.navigation:navigation-compose:2.5.3")
    // Material
    implementation("androidx.compose.material3:material3:1.2.0-alpha01")
    implementation("androidx.compose.material:material-icons-core:1.4.3")
    implementation("androidx.compose.material:material-icons-extended:1.4.3")
    // Jetpack Glance
    implementation("androidx.glance:glance-appwidget:1.0.0-beta01")
    // 3rd-party
    implementation("com.marosseleng.android:compose-material3-datetime-pickers:0.7.0")

    // Jetpack Room
    implementation("androidx.room:room-ktx:2.5.1")
    implementation("androidx.room:room-runtime:2.5.1")
    ksp("androidx.room:room-compiler:2.5.1")

    // Jetpack Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Insert-Koin
    implementation("io.insert-koin:koin-android:3.3.0")
    implementation("io.insert-koin:koin-annotations:1.2.0")
    implementation("io.insert-koin:koin-androidx-navigation:3.3.0")
    implementation("io.insert-koin:koin-androidx-compose:3.3.0")
    ksp("io.insert-koin:koin-ksp-compiler:1.2.0")

    // Compose Destinations
    implementation("io.github.raamcosta.compose-destinations:core:1.8.33-beta")
    ksp("io.github.raamcosta.compose-destinations:ksp:1.8.33-beta")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    // Semantic versioning
    implementation("io.github.z4kn4fein:semver:1.4.2")

    // Web scaping
    implementation("org.jsoup:jsoup:1.15.1")
}