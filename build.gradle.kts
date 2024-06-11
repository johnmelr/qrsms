// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    extra.apply {
        set("lifecycle_version", "2.6.1")
        set("room_version", "2.6.1")
    }
}

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
    // kotlin("kapt") version "1.9.24"
    id("androidx.room") version "2.6.1" apply false
}
val myValue by extra("Ca%c4%sD8qQozBW4Hs*BWRa@")
val keystore by extra(myValue)
val keypass by extra("45^wrWn@!jPKDrkC5Skta7mB")
