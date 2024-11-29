import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                //implementation(project(":Fdic"))
                implementation(libs.okio)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}