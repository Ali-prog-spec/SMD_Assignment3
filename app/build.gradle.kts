plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id ("kotlin-kapt")

}

android {
    namespace = "com.example.assignment1"
    compileSdk = 36   // ✅ Update this

    defaultConfig {
        applicationId = "com.example.assignment1"
        minSdk = 24
        targetSdk = 36   // ✅ Update this too
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

    kotlinOptions {
        jvmTarget = "11"
    }
}

repositories {
    google()
    mavenCentral()
}
dependencies {
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("io.agora.rtc:full-sdk:4.6.0")
    // Add the dependencies for the Firebase products you want to use
    // without specifying versions. The BoM will manage them.
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx") // Use the ktx version for consistency
    implementation("com.google.firebase:firebase-storage-ktx")
    // Your other dependencies
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation(libs.volley)
    implementation(libs.androidx.tools.core)
    implementation(libs.androidx.work.runtime.ktx)
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    testImplementation(libs.junit)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")


    implementation("com.squareup.picasso:picasso:2.8")


    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}