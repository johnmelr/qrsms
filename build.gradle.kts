// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    extra.apply {
        set("lifecycle_version", "2.6.2")
        set("room_version", "2.6.1")
    }
}

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.44" apply false
}
val myValue by extra("Ca%c4%sD8qQozBW4Hs*BWRa@")
val keystore by extra(myValue)
val keypass by extra("45^wrWn@!jPKDrkC5Skta7mB")
