plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.powerassert)
    alias(libs.plugins.android.library)
    id("module.publication")
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = libs.versions.jvm.get()
        }
    }
    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.jvm.get()
            }
        }
    }
    wasmJs {
        moduleName = "fdic"
        browser {
            commonWebpackConfig {
                outputFileName = "fdicLibrary.js"
            }
        }
        binaries.library()
    }
    //wasmWasi {}
    js(IR)
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    mingwX64()
    macosX64()
    macosArm64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val okioMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.okio)
            }
        }

        val nativeMain by getting {
            dependsOn(okioMain)
        }

        val jvmMain by getting {
            dependsOn(okioMain)
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.korge.core)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependsOn(okioMain)
        }

        val korgeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.korge.core)
            }
        }

        val jsMain by getting {
            dependsOn(korgeMain)
        }

//        val wasmWasiMain by getting {
//            dependsOn(korgeMain)
//        }

        val wasmJsMain by getting {
            dependsOn(korgeMain)
        }
    }
}

android {
    namespace = "com.darkrockstudios.fdic"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
    }
}
