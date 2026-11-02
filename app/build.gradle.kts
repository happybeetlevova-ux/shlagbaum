plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.vladimir.shlagbaum"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.vladimir.shlagbaum"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.8"
    }
    // Постоянный закрытый ключ подписи: благодаря ему новые версии ставятся поверх старых
    signingConfigs {
        create("release") {
            // Ключ и пароль приходят из секретов GitHub, в коде их нет
            storeFile = file("release.jks")
            storePassword = System.getenv("KEYSTORE_PASS")
            keyAlias = "shlagbaum"
            keyPassword = System.getenv("KEYSTORE_PASS")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
