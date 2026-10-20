plugins {
    alias(libs.plugins.android.application)
}

// DeepSeek API key for meal recognition. Read from the git-ignored .env at the
// repository root (or the process environment) at build time.
// NOTE: this embeds the key in the APK. That is fine for a local prototype but
// a production app must call DeepSeek through a backend proxy instead.
val localEnv: Map<String, String> = rootProject.file(".env")
    .takeIf { it.isFile }
    ?.readLines()
    ?.mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
        val separator = trimmed.indexOf('=')
        if (separator <= 0) return@mapNotNull null
        val name = trimmed.substring(0, separator).trim()
        val value = trimmed.substring(separator + 1).trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
        name to value
    }
    ?.toMap()
    .orEmpty()

val deepSeekApiKey: String = localEnv["DEEPSEEK_API_KEY"]
    ?: System.getenv("DEEPSEEK_API_KEY")
    ?: ""

fun buildConfigString(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "")
        .replace("\n", "\\n")
    return "\"$escaped\""
}

// Firebase needs app/google-services.json (download it from the Firebase Console).
// The plugin is only applied when the file exists so the project still builds and
// runs (in local demo-auth mode) for anyone without it.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.example.meallens"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.meallens"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DEEPSEEK_API_KEY", buildConfigString(deepSeekApiKey))
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    // Real org.json implementation for JVM unit tests; Android provides it at runtime.
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
