plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.firebase)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.taskcommadmin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.taskcommadmin"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read Supabase config from Gradle properties (or env as fallback)
        val supabaseUrl = (project.properties["SUPABASE_URL"] as? String)
            ?: (System.getenv("SUPABASE_URL") ?: "")
        val supabaseAnonKey = (project.properties["SUPABASE_ANON_KEY"] as? String)
            ?: (System.getenv("SUPABASE_ANON_KEY") ?: "")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
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
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.navigation.compose)
    implementation("androidx.compose.material:material-icons-extended")
    
    // Firebase (kept for Storage/Analytics if needed)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation("com.google.firebase:firebase-analytics")
    
    // Supabase + Ktor
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.kt)
    implementation(libs.gotrue.kt)
    implementation(libs.postgrest.kt)
    implementation(libs.ktor.client.android)
    implementation("io.ktor:ktor-client-okhttp:2.3.12")

    // Kotlinx Serialization JSON
    implementation(libs.kotlinx.serialization.json)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Image Loading
    implementation(libs.coil.compose)
    
    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    
    // Permissions
    implementation(libs.accompanist.permissions)
    
    // File Picker
    implementation(libs.documentfile)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}