/**
 * Gradle Files authored with guidelines from following resources
 * https://developer.android.com/studio/build#build-files
 * https://developer.android.com/studio/build/gradle-tips
 */

plugins {
    alias(libs.plugins.android.application)
}

val appVersionName = libs.versions.app.main.get()
val appVersionCode = appVersionName.split(".")
    .map(String::toInt).reduce { acc, i -> acc * 100 + i }

android {
    namespace = "pos.modetest"
    compileSdk = libs.versions.app.sdk.compile.get().toInt()

    defaultConfig {
        applicationId = "pos.modetest"
        minSdk = libs.versions.app.sdk.min.get().toInt()
        targetSdk = libs.versions.app.sdk.target.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
        base.archivesName = "${rootProject.name}_v$appVersionName"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles += getDefaultProguardFile("proguard-android-optimize.txt")
        }
    }

    lint {
        abortOnError = false
        checkAllWarnings = true
        checkDependencies = true
        checkTestSources = true
        warningsAsErrors = true
        htmlOutput =
            layout.buildDirectory.file("reports/${rootProject.name}_lint.html").get().asFile
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.google.material)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.intents)
}
