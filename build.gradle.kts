plugins {
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.kotlinJvm) apply false
}

allprojects {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}
