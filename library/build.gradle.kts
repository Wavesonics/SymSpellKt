import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidLibrary)
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
	@OptIn(ExperimentalWasmDsl::class)
	wasmJs {
		moduleName = "symspellkt"
		browser {
			commonWebpackConfig {
				outputFileName = "symspellLibrary.js"
			}
		}
		binaries.library()
	}
	iosX64()
	iosArm64()
	iosSimulatorArm64()
	linuxX64()
	macosX64()
	macosArm64()
	mingwX64()

	sourceSets {
		val commonMain by getting {
			dependencies {
				// Copied the code in until they add more targets
				//implementation(libs.murmurhash)
			}
		}
		val commonTest by getting {
			dependencies {
				implementation(libs.kotlin.test)
			}
		}
		val jvmMain by getting {
		}
		val jvmTest by getting {
			dependencies {
				implementation(libs.kotlin.test)
				implementation(libs.junit4)
				implementation(libs.cvsparser)
				implementation(libs.jmh)
				//configurations["kapt"].dependencies.add(libs.jmh.annprocess.get())
			}
		}
	}

	// The german test needs a lot of memory
	val jvmTest by sourceSets.getting
	tasks.withType<Test> {
		if (name == jvmTest.name) {
			jvmArgs = listOf("-Xms4g", "-Xmx8g")
		}
	}
}

android {
	namespace = "com.darkrockstudios.symspellkt"
	compileSdk = libs.versions.android.compileSdk.get().toInt()
	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
	}
	compileOptions {
		sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
		targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
	}
}
