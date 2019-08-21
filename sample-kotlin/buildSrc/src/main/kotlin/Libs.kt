import kotlin.String

/**
 * Generated by https://github.com/jmfayard/buildSrcVersions
 *
 * Update this file with
 *   `$ ./gradlew buildSrcVersions`
 */
object Libs {
  /**
   * https://github.com/square/okhttp
   */
  const val okhttp: String = "com.squareup.okhttp3:okhttp:" + Versions.okhttp

  const val okio: String = "com.squareup.okio:okio:" + Versions.okio

  const val org_jetbrains_kotlin_jvm_gradle_plugin: String =
      "org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:" +
      Versions.org_jetbrains_kotlin_jvm_gradle_plugin

  /**
   * https://kotlinlang.org/
   */
  const val kotlin_scripting_compiler_embeddable: String =
      "org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:" +
      Versions.kotlin_scripting_compiler_embeddable

  /**
   * https://kotlinlang.org/
   */
  const val kotlin_stdlib_jdk8: String = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:" +
      Versions.kotlin_stdlib_jdk8

  /**
   * https://github.com/zTrap/Android-Iconics-Kt
   */
  const val core_ktx: String = "ru.ztrap.iconics:core-ktx:" + Versions.core_ktx
}
