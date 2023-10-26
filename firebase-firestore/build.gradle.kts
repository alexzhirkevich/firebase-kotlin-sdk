/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

version = project.property("firebase-firestore.version") as String

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.android.library")
    kotlin("native.cocoapods")
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.8.20"
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("FirestorePersistentStorage") {
            packageName.set("dev.gitlive.firebase.firestore")
        }
    }
}

android {
    compileSdk = property("targetSdkVersion") as Int
    defaultConfig {
        minSdk = property("minSdkVersion") as Int
        targetSdk = property("targetSdkVersion") as Int
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
        getByName("androidTest"){
            java.srcDir(file("src/androidAndroidTest/kotlin"))
            manifest.srcFile("src/androidAndroidTest/AndroidManifest.xml")
        }
    }
    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
    }
    packagingOptions {
        resources.pickFirsts.add("META-INF/kotlinx-serialization-core.kotlin_module")
        resources.pickFirsts.add("META-INF/AL2.0")
        resources.pickFirsts.add("META-INF/LGPL2.1")
        resources.pickFirsts.add("androidsupportmultidexversion.txt")
    }
    lint {
        abortOnError = false
    }
}

val supportIosTarget = project.property("skipIosTarget") != "true"

kotlin {

    android {
        publishAllLibraryVariants()
    }

    jvm {
        val main by compilations.getting {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
        val test by compilations.getting {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    if (supportIosTarget) {
        ios()
        iosSimulatorArm64()
        cocoapods {
            ios.deploymentTarget = "11.0"
            framework {
                baseName = "FirebaseFirestore"
            }
            noPodspec()
            pod("FirebaseFirestore") {
                version = "10.7.0"
            }
        }
    }

    js {
        useCommonJs()
        nodejs {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                apiVersion = "1.8"
                languageVersion = "1.8"
                progressiveMode = true
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.serialization.InternalSerializationApi")
            }
        }

        val commonMain by getting {
            dependencies {
                api(project(":firebase-app"))
                implementation(project(":firebase-common"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(project(":test-utils"))
            }
        }

        val androidMain by getting {
            dependencies {
                api("com.google.firebase:firebase-firestore")
            }
        }

        val jvmMain by getting

        if (supportIosTarget) {
            val iosMain by getting
            val iosSimulatorArm64Main by getting
            iosSimulatorArm64Main.dependsOn(iosMain)
            val iosTest by sourceSets.getting
            val iosSimulatorArm64Test by getting
            iosSimulatorArm64Test.dependsOn(iosTest)
        }

        val jsMain by getting

        val restMain by creating {
            dependsOn(commonMain)
            jvmMain.dependsOn(this)
            dependencies {
                implementation(project(":firebase-auth"))

                implementation(libs.serialization)
                implementation(libs.ktor.core)
                implementation(libs.ktor.negotiation)
                implementation(libs.ktor.negotiationjson)
                implementation(libs.ktor.auth)
                implementation(libs.ktor.logging)
                implementation(libs.datetime)
                implementation(libs.sqlidelight.coroutines)
            }
        }
    }
}

if (project.property("firebase-firestore.skipIosTests") == "true") {
    tasks.forEach {
        if (it.name.contains("ios", true) && it.name.contains("test", true)) { it.enabled = false }
    }
}

if (project.property("firebase-firestore.skipJsTests") == "true") {
    tasks.forEach {
        if (it.name.contains("js", true) && it.name.contains("test", true)) { it.enabled = false }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}