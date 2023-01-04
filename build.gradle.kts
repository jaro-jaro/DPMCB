// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {

    repositories {
        google()
        maven { url = uri("https://jcenter.bintray.com") }
        mavenCentral()
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.3")
        classpath("com.google.gms:google-services:4.3.14")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.40.1")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.7.20")
    }
}

plugins {
    id("com.android.application") version "7.3.0" apply false
    id("com.android.library") version "7.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.7.20" apply false
}

task("generateVersionTxt") {
    doLast {
        file("./version.txt").text = android.defaultConfig.versionName
    }
}
