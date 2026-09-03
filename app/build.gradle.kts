plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun String.quotedForBuildConfig(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val productionAdMobAppId = providers.environmentVariable("WELDING_ADMOB_ANDROID_APP_ID").orNull ?: "MISSING_PRODUCTION_ADMOB_APP_ID"
val productionAdMobBannerId = providers.environmentVariable("WELDING_ANDROID_ADMOB_BANNER_ID").orNull ?: "MISSING_PRODUCTION_ADMOB_BANNER_ID"
val playLicenseKey = providers.environmentVariable("WELDING_PLAY_LICENSE_PUBLIC_KEY").orNull ?: ""

android {
    namespace = "com.goodusestudios.weldinggaswallet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.goodusestudios.weldinggaswallet"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "PLAY_LICENSE_PUBLIC_KEY", playLicenseKey.quotedForBuildConfig())
    }

    flavorDimensions += "advertising"
    productFlavors {
        create("ads") {
            dimension = "advertising"
            buildConfigField("boolean", "SHELL_ADS_ENABLED", "true")
            manifestPlaceholders["ADMOB_APP_ID"] = productionAdMobAppId
            buildConfigField("String", "ADMOB_APP_ID", productionAdMobAppId.quotedForBuildConfig())
            buildConfigField("String", "ADMOB_BANNER_ID", productionAdMobBannerId.quotedForBuildConfig())
        }
        create("noAds") {
            dimension = "advertising"
            buildConfigField("boolean", "SHELL_ADS_ENABLED", "false")
            buildConfigField("String", "ADMOB_APP_ID", "".quotedForBuildConfig())
            buildConfigField("String", "ADMOB_BANNER_ID", "".quotedForBuildConfig())
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713".quotedForBuildConfig())
            buildConfigField("String", "ADMOB_BANNER_ID", "ca-app-pub-3940256099942544/9214589741".quotedForBuildConfig())
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources { generateLocaleConfig = false }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

tasks.configureEach {
    if (name.contains("AdsRelease")) {
        doFirst {
            require(Regex("^ca-app-pub-8054612600809568~\\d{10}$").matches(productionAdMobAppId)) { "Release requires the publisher-owned WELDING_ADMOB_ANDROID_APP_ID." }
            require(Regex("^ca-app-pub-8054612600809568/\\d{10}$").matches(productionAdMobBannerId)) { "Release requires the publisher-owned WELDING_ANDROID_ADMOB_BANNER_ID." }
            require(playLicenseKey.isNotBlank()) { "Release requires WELDING_PLAY_LICENSE_PUBLIC_KEY." }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("com.android.billingclient:billing-ktx:9.1.0")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
}
