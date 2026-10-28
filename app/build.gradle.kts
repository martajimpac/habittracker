import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrainsKotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.kover)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.marta.habittracker"
    compileSdk = 37


    flavorDimensions += "environment"

    productFlavors {
        create("emulator") {
            dimension = "environment"

            buildConfigField(
                "String",
                "BLOOM_API_BASE_URL",
                "\"${localProperties.getProperty("BLOOM_API_BASE_URL_EMULATOR", "")}\"",
            )
        }

        create("phone") {
            dimension = "environment"

            buildConfigField(
                "String",
                "BLOOM_API_BASE_URL",
                "\"${localProperties.getProperty("BLOOM_API_BASE_URL_DEVICE", "")}\"", //ip de mi ordenador
            )
        }
    }

    defaultConfig {
        applicationId = "com.marta.habittracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\"",
        )
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", "")}\"",
        )
    }

    buildTypes {
        debug {
            // Cada flavor utiliza su propia URL local.
        }

        release {
            isMinifyEnabled = false

            buildConfigField(
                "String",
                "BLOOM_API_BASE_URL",
                "\"${localProperties.getProperty("BLOOM_API_BASE_URL_PRODUCTION", "")}\"",
            )

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
            freeCompilerArgs.add("-Xdeprecation")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

}

dependencies {
    implementation(libs.androidx.compose.animation.core)
    //Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.serialization.json)

    //Navigation
    implementation(libs.androidx.navigation.compose)

    //DI
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.junit.ktx)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)



    //Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.adapter)

    //Icons
    implementation(libs.androidx.compose.material.icons.extended)

    //DataStore
    implementation(libs.androidx.datastore.preferences)

    //Supabase
    implementation(libs.postgrest)
    implementation(libs.auth)
    implementation(libs.storage)
    implementation(libs.functions)
    implementation(libs.ktor.client.android)

    //Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    //Glance widgets
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    debugImplementation(libs.androidx.glance.preview)
    debugImplementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.work.runtime.ktx)

    // Coil — https avatar URLs in Preferences (Task 5 will reuse on Home/Friends/Profile)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    //Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
