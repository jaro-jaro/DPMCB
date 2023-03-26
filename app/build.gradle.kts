plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    //id("dagger.hilt.android.plugin")
//    id("kotlin-kapt")
    id("com.google.devtools.ksp") version "1.8.10-1.0.9"
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
}

android {
    namespace = "cz.jaro.dpmcb"
    compileSdk = 33

    defaultConfig {
        applicationId = "cz.jaro.dpmcb"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0-pre1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            manifestPlaceholders += "logo" to "@mipmap/logo_dpmcb"
            manifestPlaceholders += "logoRound" to "@mipmap/logo_dpmcb_round"
        }
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders += "logo" to "@mipmap/logo_white"
            manifestPlaceholders += "logoRound" to "@mipmap/logo_white_round"
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.2"
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

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0-alpha02")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
//    implementation("androidx.legacy:legacy-support-v4:1.0.0")
//    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0")
//    implementation("androidx.preference:preference-ktx:1.2.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // PDF Viewer
    implementation("com.github.barteksc:android-pdf-viewer:2.8.2")

    // Coroutines
//    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    // Navigation
//    implementation("android.arch.navigation:navigation-fragment-ktx:1.0.0")
//    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
//    implementation("androidx.navigation:navigation-dynamic-features-fragment:2.5.3")
//    androidTestImplementation("androidx.navigation:navigation-testing:2.5.3")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:31.1.0"))
//    implementation("com.google.firebase:firebase-core")
//    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-common-ktx:20.3.1")
    implementation("com.google.firebase:firebase-storage-ktx:20.1.0")

    // Compose
    implementation("androidx.activity:activity-compose:1.6.1")
    implementation("androidx.compose.material3:material3:1.1.0-alpha08")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.0")
    implementation("androidx.compose.ui:ui:1.3.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.3.3")
//    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.3.3")
//    debugImplementation("androidx.compose.ui:ui-tooling:1.3.3")
//    debugImplementation("androidx.compose.ui:ui-test-manifest:1.3.3")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("androidx.compose.foundation:foundation:1.3.1")
    implementation("androidx.compose.material:material-icons-core:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.3.1")
//    implementation("androidx.compose.runtime:runtime-livedata:1.3.3")
//    implementation("androidx.compose.runtime:runtime-rxjava2:1.3.3")
//    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.3.3")

    // Room
    implementation("androidx.room:room-ktx:2.5.0")
    implementation("androidx.room:room-runtime:2.5.0")
    annotationProcessor("androidx.room:room-compiler:2.5.0")
    ksp("androidx.room:room-compiler:2.5.0")
    //ksp("androidx.room:room-compiler:2.4.3")

    // Dagger Hilt
    /*implementation("com.google.dagger:hilt-android:$daggerHiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$daggerHiltVersion")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")*/

    // Koin
    implementation("io.insert-koin:koin-android:3.3.0")
    implementation("io.insert-koin:koin-androidx-navigation:3.3.0")
    implementation("io.insert-koin:koin-androidx-compose:3.3.0")
//    testImplementation("io.insert-koin:koin-test-junit4:3.2.2")

    // Dagger
    /*implementation("com.google.dagger:dagger:2.40.1")
    implementation("com.google.dagger:dagger-android-support:2.11")
    annotationProcessor("com.google.dagger:dagger-compiler:2.40.1")
    annotationProcessor("com.google.dagger:dagger-android-processor:2.11")*/

    // Compose Destinations
    implementation("io.github.raamcosta.compose-destinations:core:1.8.33-beta")
    ksp("io.github.raamcosta.compose-destinations:ksp:1.8.33-beta")

    // Datastore
//    implementation("androidx.datastore:datastore-core:1.0.0")
//    implementation("androidx.datastore:datastore:1.0.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.google.accompanist:accompanist-flowlayout:0.27.1")
    implementation("com.google.accompanist:accompanist-placeholder-material:0.28.0")

    implementation("com.marosseleng.android:compose-material3-datetime-pickers:0.7.0")

    implementation("org.jsoup:jsoup:1.15.1")

    implementation(files("./libs/datum-cas.aar"))
}