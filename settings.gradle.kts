pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

rootProject.name = "parser"
include("sample", "utilities")
includeSample("pratt")

fun includeSample(vararg name: String) {
  include(*name.map { "sample:$it" }.toTypedArray())
}
