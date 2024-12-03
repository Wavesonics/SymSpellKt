plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    applyDefaultHierarchyTemplate()

    val nativeTargets = listOf(macosX64(), macosArm64(), linuxX64(), mingwX64())
    nativeTargets.forEach { target ->
        target.binaries {
            executable {
                entryPoint = "main"
                baseName = "fdic"
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation(project(":Fdic"))
                api(libs.okio)
                api(libs.bundles.mordant)
                api(libs.clikt)
            }
        }

        nativeTargets.forEach { target ->
            getByName("${target.name}Main").dependsOn(nativeMain)
        }
    }
}