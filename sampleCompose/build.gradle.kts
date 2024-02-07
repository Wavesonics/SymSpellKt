import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
}

kotlin {
    applyDefaultHierarchyTemplate()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "sampleCompose"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }
    
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = libs.versions.jvm.get()
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":library"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}


compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.darkrockstudios.symspellkt.sample"
            packageVersion = "1.0.0"
        }
    }
}

compose.experimental {
    web.application {}
}
