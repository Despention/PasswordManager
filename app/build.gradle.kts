plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("org.jetbrains.kotlin.kapt")}

android {
    namespace = "com.example.tutor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tutor"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("armeabi-v7a", "x86", "x86_64", "arm64-v8a")
        }
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
    buildFeatures {
        compose = true
    }

    buildFeatures {
        viewBinding = true
    }



}


dependencies {
    implementation ("androidx.biometric:biometric-ktx:1.2.0-alpha05")

    implementation ("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation ("androidx.preference:preference-ktx:1.2.1")

    implementation ("androidx.security:security-crypto:1.1.0-alpha06")
    implementation ("androidx.activity:activity-ktx:1.9.3")
    implementation ("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.lifecycle.process)

    // --- Room ---
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    // --- /Room ---

    // --- Hilt ---
    implementation ("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-compiler:2.51")
    // --- /Hilt ---

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation ("androidx.cardview:cardview:1.0.0")

    // --- Glide ---
    implementation("com.github.bumptech.glide:glide:4.12.0")
    // kapt("com.github.bumptech.glide:compiler:4.12.0") // <-- ДОБАВИТЬ, если используете @GlideModule и т.п.
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    // --- /Glide ---

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.slidingpanelayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}