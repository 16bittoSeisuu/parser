plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.kotlinxSerialization)

  alias(libs.plugins.kotest)
}

group = "net.japanesehunters"
version = "0.1"

kotlin {
  jvm()
  macosArm64()
  macosX64()
  linuxArm64()
  linuxX64()
  mingwX64()

  compilerOptions {
    freeCompilerArgs.add("-Xcontext-parameters")
    freeCompilerArgs.add("-Xexpect-actual-classes")
    freeCompilerArgs.add("-Xpartial-linkage=enable")
  }

  sourceSets {
    commonMain.dependencies {
      implementation(libs.bundles.kotlinx)
      implementation(libs.bundles.arrow)
    }
    commonTest.dependencies {
      implementation(libs.bundles.kotest)
    }
  }
}
