plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.feedmatepetfeedersystem"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.feedmatepetfeedersystem"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {


    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.material)

    implementation(libs.glide)
    annotationProcessor(libs.glideCompiler)

    implementation(platform(libs.firebase.bom)) // Ensure firebase library use compatible versions
    implementation(libs.firebase.auth)       // For login & signup
    implementation(libs.firebase.database)  // If you want to store feeding schedules/logs
    implementation(libs.firebase.storage)

}