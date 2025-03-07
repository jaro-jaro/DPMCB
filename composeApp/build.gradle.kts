import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    js(IR) {
        moduleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        
        androidMain.dependencies {
            // Core Android
            implementation(libs.androidx.core)

            // Compose preview
            implementation(compose.preview)

            // SVG Viewer
            implementation(libs.coil.svg)
            implementation(libs.zoomable.image.coil)

            // Firebase Crashlytics
            implementation(libs.firebase.crashlytics)

            // Jetpack Glance
            implementation(libs.androidx.jetpack.glance)

            // Insert Koin
            implementation(libs.koin.android)

            // SQLDelight
            implementation(libs.sqldelight.android)

            // Kotlinx Coroutines
            implementation(libs.kotlinx.coroutines.android)

            // Activity
            implementation(libs.androidx.activity.compose)

            implementation(libs.transportation.consumer)
        }
        jsMain.dependencies {
            implementation(compose.html.core)

            // Multiplatform Settings
            implementation(libs.multiplatform.settings.make.observable)
        }
        commonMain.dependencies {
            // Kotlin reflection
            implementation(kotlin("reflect"))

            // Kotlinx Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Firebase
            // Realtime Databse
            implementation(libs.firebase.database)
            // In-App Messaging
//            implementation(libs.firebase.inappmessaging.display)
            // Storage
            implementation(libs.firebase.storage)
            // Analytics
            implementation(libs.firebase.analytics)
            // Remote config
            implementation(libs.firebase.config)

            // Jetpack Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // Material
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            // 3rd-party
            implementation(libs.compose.material3.datetime.pickers)

            // Jetpack Navigation
            implementation(libs.androidx.navigation.compose)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            // Insert Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // Kotlinx Serialization
            implementation(libs.kotlinx.serialization)

            // Kotlinx DateTime
            implementation(libs.kotlinx.datetime)

            // Semantic versioning
            implementation(libs.semver)

            // Web scaping
            implementation(libs.ksoup)
            implementation(libs.ksoup.network)

            // ChNT
            implementation(libs.androidx.browser)

            // Multiplatform Settings
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
        freeCompilerArgs.add("-Xwhen-guards")
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("cz.jaro.dpmcb")
        }
    }
}

buildkonfig {
    packageName = "cz.jaro.dpmcb"

    defaultConfigs {
        buildConfigField(
            FieldSpec.Type.STRING, "versionName", libs.versions.appVersionName.get()
        )
        buildConfigField(
            FieldSpec.Type.INT, "versionCode", libs.versions.appVersionCode.get()
        )
    }
}

android {
    namespace = "cz.jaro.dpmcb"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "cz.jaro.dpmcb"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = 35
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

