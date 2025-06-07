package net.japanesehunters.util

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class JvmNameJvmOnly(
  val name: String,
)
