plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    id("kotlin-parcelize")
}

android {
    namespace = "com.nhlstenden.appdev"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.nhlstenden.appdev"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("androidx.cardview:cardview:1.0.0")
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Supabase
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.json:json:20231013")

    // Retrofit & Gson for Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Force specific versions compatible with compileSdk 33
    constraints {
        implementation("androidx.activity:activity:1.6.1") {
            because("compileSdk 33 requirement")
        }
        implementation("androidx.core:core:1.10.1") {
            because("compileSdk 33 requirement")
        }
        implementation("androidx.annotation:annotation-experimental:1.3.0") {
            because("compileSdk 33 requirement")
        }
    }

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}