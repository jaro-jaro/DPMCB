plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
}

android {
    namespace = "cz.jaro.dpmcb"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "cz.jaro.dpmcb"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = 36
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

/*configurations {
    implementation.get().exclude(group = "org.jetbrains", module = "annotations")
}*/

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xnested-type-aliases")
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        freeCompilerArgs.add("-Xenable-suspend-function-exporting")
        freeCompilerArgs.add("-Xreturn-value-checker=full")
    }
}

dependencies {
    implementation(projects.composeApp)
    debugImplementation(libs.ui.tooling)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Core Android
    implementation(libs.androidx.core)

    // Compose preview
    implementation(libs.org.jetbrains.compose.ui.ui.tooling.preview)

    // SVG Viewer
    implementation(libs.coil.svg)
    implementation(libs.zoomable.image.coil)

    // Firebase Crashlytics
    implementation(libs.firebase.crashlytics)

    // Jetpack Glance
    implementation(libs.androidx.jetpack.glance)

    // Insert Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Kotlinx Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Activity
    implementation(libs.androidx.activity.compose)

    // ChNT
    implementation(libs.androidx.browser)

    implementation(libs.transportation.consumer)

    // Firebase
    // Realtime Database
    implementation(libs.google.firebase.database)
    // Storage
//            implementation(libs.google.firebase.storage)
    // Analytics
    implementation(libs.google.firebase.analytics)
    // Crashlytics
    implementation(libs.google.firebase.crashlytics)
    // Remote config
    implementation(libs.google.firebase.config)

    // Sqldelight
    implementation(libs.sqldelight.android.driver)

    // Multiplatform Settings
    implementation(libs.multiplatform.settings)
    implementation(libs.multiplatform.settings.coroutines)
    implementation(libs.multiplatform.settings.make.observable)
}
