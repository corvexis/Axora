import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.rikka.tools.refine)
    id("kotlin-parcelize")
}

android {
    namespace = "frb.axeron.manager"

    defaultConfig {
        applicationId = "frb.axeron.manager"
    }

    val keystorePropertiesFile = rootProject.file("local.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            storeFile = file("../axora-release.keystore")
            storePassword = keystoreProperties.getProperty("keystore.password", "")
            keyAlias = "axora"
            keyPassword = keystoreProperties.getProperty("key.password", "")
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
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "**"
        }
    }

    applicationVariants.all {
        outputs.all {
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl.outputFileName = "Axora_v${versionName}_${versionCode}-${buildType.name}.apk"
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {

    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)
    implementation(libs.colorpicker.compose)

    implementation(libs.compose.coil)
    implementation(libs.appiconloader.coil)

    implementation(libs.androidx.webkit)
    implementation(libs.androidx.core.ktx)

    implementation(libs.rikka.compatibility)
    implementation(libs.rikka.parcelablelist)
    implementation(libs.rikka.hidden.compat)
    compileOnly(libs.rikka.hidden.stub)

    implementation(libs.topjohnwu.libsu.core)
    implementation(libs.topjohnwu.libsu.io)

    implementation(libs.gson)
    implementation(libs.markdown)
    implementation(project(":server"))

    implementation(project(":aidl"))
    implementation(project(":api"))
    implementation(project(":adb"))
    implementation(project(":shared"))
    implementation(project(":provider"))
    implementation(project(":server-shared"))
    implementation(project(":axerish"))
    implementation(libs.sdp.android)
    implementation(libs.material)
    implementation(libs.mmrl.ui)
    implementation(libs.hiddenapibypass)
    implementation(libs.ansi.library)
    implementation(libs.ansi.library.ktx)

    implementation(libs.sheet.compose.dialogs.core)
    implementation(libs.sheet.compose.dialogs.list)
    implementation(libs.sheet.compose.dialogs.input)
}