plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

dependencies {
    implementation("com.google.code.gson:gson:2.13.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}