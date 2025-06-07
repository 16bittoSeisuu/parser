plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.shadow)
  application
}

dependencies {
  implementation(project(":utilities"))
  implementation(libs.bundles.arrow)
}

application {
  mainClass.set("MainKt")
}

tasks {
  jar {
    manifest {
      attributes["Main-Class"] = "MainKt"
    }
  }
//  shadowJar {
//    archiveClassifier.set("")
//  }
}
