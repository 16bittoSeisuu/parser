plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.kotlinxSerialization)

  alias(libs.plugins.kotest)
}

group = "net.japanesehunters"
version = "0.1"

repositories {
  mavenCentral()
}

kotlin {
  listOf(
    macosArm64(),
    macosX64(),
    linuxArm64(),
    linuxX64(),
    mingwX64(),
  ).forEach { target ->
    target.apply {
      binaries {
        executable {
          entryPoint = "main"
        }
      }
    }

    compilerOptions {
      freeCompilerArgs.add("-Xcontext-parameters")
      freeCompilerArgs.add("-Xpartial-linkage=enable")
    }
  }
  applyDefaultHierarchyTemplate()

  sourceSets {
    nativeMain.dependencies {
      implementation(libs.bundles.kotlinx)
      implementation(libs.clikt)
      implementation(libs.bundles.okio)
    }
    nativeTest.dependencies {
      implementation(libs.bundles.kotest)
    }
    commonMain.dependencies {
      implementation(libs.bundles.kotlinx)
      implementation(libs.bundles.arrow)
    }
    commonTest.dependencies {
      implementation(libs.bundles.kotest)
    }
  }
}
