
import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.internal.config.LanguageFeature
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    js(IR) {
        outputModuleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static(rootDirPath)
                    static(projectDirPath)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        androidMain.get().dependencies {
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

            // Kotlinx Coroutines
            implementation(libs.kotlinx.coroutines.android)

            // Activity
            implementation(libs.androidx.activity.compose)

            // ChNT
            implementation(libs.androidx.browser)

            // Room
            implementation(libs.androidx.jetpack.room)
            implementation(libs.androidx.jetpack.room.runtime)

            implementation(libs.transportation.consumer)

            // Firebase
            // Realtime Database
            implementation(libs.google.firebase.database)
            // Storage
            implementation(libs.google.firebase.storage)
            // Analytics
            implementation(libs.google.firebase.analytics)
            // Crashlytics
            implementation(libs.google.firebase.crashlytics)
            // Remote config
            implementation(libs.google.firebase.config)
        }
        jsMain.get().dependencies {
            implementation(compose.html.core)

            // Multiplatform Settings

            // Supabase
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
        }
        commonMain.get().dependencies {
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
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // Material
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)

            // Androidx adaptive
            implementation(libs.androidx.adaptive)

            // Jetpack Navigation
            implementation(libs.androidx.navigation.compose)

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

            // Multiplatform Settings
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.multiplatform.settings.make.observable)

            // Room
            implementation(libs.androidx.jetpack.room.common)
        }
        all {
            languageSettings {
                enableLanguageFeature(LanguageFeature.NestedTypeAliases.name)
                enableLanguageFeature(LanguageFeature.ContextParameters.name)
                enableLanguageFeature(LanguageFeature.WhenGuards.name)
//                enableLanguageFeature(LanguageFeature.NonLocalBreakContinue.name)
                enableLanguageFeature(LanguageFeature.MultiDollarInterpolation.name)
                enableLanguageFeature(LanguageFeature.ExpectActualClasses.name)
                enableLanguageFeature(LanguageFeature.ContextSensitiveResolutionUsingExpectedType.name)
            }
        }
    }

//    compilerOptions {
//        freeCompilerArgs.add("-Xnested-type-aliases")
//        freeCompilerArgs.add("-Xcontext-parameters")
//        freeCompilerArgs.add("-Xwhen-guards")
//        freeCompilerArgs.add("-Xnon-local-break-continue")
//        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
//        freeCompilerArgs.add("-Xexpect-actual-classes")
//        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
//    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
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
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        buildConfig = true
    }
}
configurations {
    implementation.get().exclude(group = "org.jetbrains", module = "annotations")
}

dependencies {
    add("kspAndroid", libs.androidx.jetpack.room.compiler)

    debugImplementation(compose.uiTooling)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

