import com.codingfeline.buildkonfig.compiler.FieldSpec
import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.internal.config.LanguageFeature

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.download)
}

kotlin {
    androidLibrary {
        namespace = "cz.jaro.dpmcb"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        androidResources {
            enable = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        androidMain.get().dependencies {
            // Core Android
            implementation(libs.androidx.core)

            // Compose preview
            implementation(libs.org.jetbrains.compose.ui.ui.tooling.preview)

            // SVG Viewer
            implementation(libs.coil.svg)
            implementation(libs.zoomable.image.coil)

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

            implementation(libs.transportation.consumer)

            // Firebase
            // Realtime Database
            implementation(libs.google.firebase.database)
            // Storage
//            implementation(libs.google.firebase.storage)
            // Analytics
            implementation(libs.google.firebase.analytics)
            // Remote config
            implementation(libs.google.firebase.config)

            // Sqldelight
            implementation(libs.sqldelight.android.driver)
        }
        jsMain.get().dependencies {
            implementation(libs.html.core)

            // Multiplatform Settings

            // Supabase
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)

            // Sqldelight
            implementation(libs.sqldelight.web.worker.driver)
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
//            implementation(libs.firebase.storage)
            // Analytics
            implementation(libs.firebase.analytics)
            // Remote config
            implementation(libs.firebase.config)

            // Jetpack Compose
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.org.jetbrains.compose.ui.ui.tooling.preview)
            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // Material
            implementation(libs.material3)
            implementation(libs.material.icons.extended)

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

            // Sqldelight
            implementation(libs.sqldelight.primitive.adapters)
        }
        all {
            languageSettings {
                enableLanguageFeature(LanguageFeature.NestedTypeAliases.name)
                enableLanguageFeature(LanguageFeature.ContextParameters.name)
                enableLanguageFeature(LanguageFeature.WhenGuards.name)
                enableLanguageFeature(LanguageFeature.MultiDollarInterpolation.name)
                enableLanguageFeature(LanguageFeature.ExpectActualClasses.name)
                enableLanguageFeature(LanguageFeature.ContextSensitiveResolutionUsingExpectedType.name)
                enableLanguageFeature(LanguageFeature.ExplicitBackingFields.name)
                enableLanguageFeature(LanguageFeature.JsAllowExportingSuspendFunctions.name)
                enableLanguageFeature(LanguageFeature.JsAllowExportingSuspendFunctions.name)
            }
        }
        jsMain {
            resources.srcDir(layout.buildDirectory.dir("sqlite"))
        }
    }

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

sqldelight {
    databases {
        create("Database") {
            packageName.set("cz.jaro.dpmcb")
            generateAsync.set(true)
        }
    }
}

// https://sqlite.org/download.html
val sqlite = 3510000

val sqliteDownload = tasks.register("sqliteDownload", Download::class.java) {
    src("https://sqlite.org/2025/sqlite-wasm-$sqlite.zip")
    dest(layout.buildDirectory.dir("tmp"))
    onlyIfModified(true)
}

val sqliteUnzip = tasks.register("sqliteUnzip", Copy::class.java) {
    dependsOn(sqliteDownload)
    from(zipTree(layout.buildDirectory.dir("tmp/sqlite-wasm-$sqlite.zip"))) {
        include("sqlite-wasm-$sqlite/jswasm/**")
        exclude("**/*worker1*")

        eachFile {
            relativePath = RelativePath(true, *relativePath.segments.drop(2).toTypedArray())
        }
    }
    into(layout.buildDirectory.dir("sqlite"))
    includeEmptyDirs = false
}

tasks.named("jsProcessResources").configure {
    dependsOn(sqliteUnzip)
}