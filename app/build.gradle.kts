plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hbulut.nobettakvimi"
    compileSdk = 36

    // Load keystore properties
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = java.util.Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
    }

    defaultConfig {
        applicationId = "com.hbulut.nobettakvimi"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17  // 2025 standardı
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlin {
        jvmToolchain(17) // kotlinOptions yerine yeni yöntem (Kotlin 1.9+)
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15" // Direct version reference
    }
    
    // META-INF duplicate hatasını %100 çözen en güncel yöntem
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module",
                "androidx/annotation/experimental/R.class",
                "androidx/annotation/R.class"
            )
        }
        jniLibs {
            pickFirsts += setOf(
                "META-INF/androidx.annotation_annotation-experimental.version",
                "androidx/annotation/experimental/R.class",
                "androidx/annotation/R.class"
            )
            excludes += setOf(
                "androidx/annotation/experimental/R.class"
            )
        }
        resources {
            pickFirsts += setOf(
                "META-INF/androidx.annotation_annotation-experimental.version",
                "androidx/annotation/experimental/R.class",
                "androidx/annotation/R.class"
            )
            excludes += setOf(
                "androidx/annotation/experimental/R.class"
            )
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildToolsVersion = "36.1.0"
    ndkVersion = "27.0.12077973"
    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }

    // Add this to handle duplicate class issue more comprehensively
    configurations.all {
        resolutionStrategy {
            preferProjectModules()
            force("androidx.annotation:annotation:1.9.1")
            force("androidx.annotation:annotation-experimental:1.4.1")
        }
    }
}
// Add a task to delete the problematic file before build

dependencies {
    // Desugar – en güncel sürüm
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    
    // Force specific version of annotation library to avoid duplicates
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.annotation:annotation-experimental:1.5.1") {
        because("Resolving duplicate R.class issue")
    }
    
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation(libs.androidx.activity.compose)
    
    // Compose BOM – 2025.3 güncel
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    
    // Google Drive API – Using libs.versions.toml
    implementation(libs.google.auth)
    implementation(libs.google.api.client)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.http.client.gson) {
        exclude(group = "androidx.annotation", module = "annotation")
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    
    // Calendar & Utils
    implementation("io.github.boguszpawlowski.composecalendar:composecalendar:1.4.0") {
        exclude(group = "androidx.annotation", module = "annotation")
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
    implementation("com.google.code.gson:gson:2.13.2")
    
    // PDF Export – Using libs.versions.toml
    implementation(libs.itext7.core) {
        exclude(group = "androidx.annotation", module = "annotation")
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation("androidx.documentfile:documentfile:1.1.0") {
        exclude(group = "androidx.annotation", module = "annotation")
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    
    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    
    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}