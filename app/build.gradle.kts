plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.chattaskai"
    compileSdk = 35
    val configuredVersionCode = providers.gradleProperty("VERSION_CODE").orNull?.toIntOrNull() ?: 1
    val configuredVersionName = providers.gradleProperty("VERSION_NAME").orNull ?: "1.0"
    val githubUpdateRepoOwner = providers.gradleProperty("APK_UPDATE_REPO_OWNER").orNull ?: ""
    val githubUpdateRepoName = providers.gradleProperty("APK_UPDATE_REPO_NAME").orNull ?: ""
    val githubUpdateAssetPrefix = providers.gradleProperty("APK_UPDATE_ASSET_PREFIX").orNull ?: "Taskline-vc"
    val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH") ?: providers.gradleProperty("ANDROID_KEYSTORE_PATH").orNull
    val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: providers.gradleProperty("ANDROID_KEYSTORE_PASSWORD").orNull
    val keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: providers.gradleProperty("ANDROID_KEY_ALIAS").orNull
    val keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: providers.gradleProperty("ANDROID_KEY_PASSWORD").orNull
    val hasReleaseSigning = !keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank() && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()

    defaultConfig {
        applicationId = "com.example.chattaskai"
        minSdk = 26
        targetSdk = 35
        versionCode = configuredVersionCode
        versionName = configuredVersionName

        buildConfigField("String", "APK_UPDATE_REPO_OWNER", "\"${githubUpdateRepoOwner.replace("\"", "\\\"")}\"")
        buildConfigField("String", "APK_UPDATE_REPO_NAME", "\"${githubUpdateRepoName.replace("\"", "\\\"")}\"")
        buildConfigField("String", "APK_UPDATE_ASSET_PREFIX", "\"${githubUpdateAssetPrefix.replace("\"", "\\\"")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
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
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.0")

    // Room DB
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Retrofit / OkHttp for Networking to Gemini/OpenAI
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
