import scala.language.postfixOps

val sharedSettings = Seq(
  name := "libpq4s",
  organization := "com.github.david-bouyssie",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.12",

  // TODO: for dev only
  scalacOptions ++= Seq("-deprecation", "-feature"),

  libraryDependencies ++= Seq(
    // "com.outr" %%% "scribe" % "2.7.12",
    // "com.lihaoyi" %%% "utest" % "0.7.4" % "test" // TODO: update me when the same version is available for both the JVM and SN
  )
)

lazy val makeLibraries = taskKey[Unit]("Building native components")

lazy val libpq4s = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("libpq4s"))
  .settings(sharedSettings)

  /*.jvmSettings(
    javaOptions in test += s"-Djava.library.path=D:\\Dev\\wsl\\scala-native\\libpq4s\\swig",
    libraryDependencies ++= Seq(
      "com.outr" %% "scribe" % "2.7.9",
      "com.lihaoyi" %%% "utest" % "0.6.8" % Test,
      "com.opentable.components" % "otj-pg-embedded" % "0.13.3" % Test
      // Alternative: https://github.com/zonkyio/embedded-postgres
    ),
    testFrameworks += new TestFramework("com.github.libpq4s.PgServerTestFramework"),
    fork := true
  )*/

  // configure Scala-Native settings
  .nativeSettings( // defined in sbt-scala-native

    libraryDependencies ++= Seq(
      "com.outr" %%% "scribe" % "2.7.12",
      "dev.whaling" % "native-loop-core_native0.4.0-M2_2.11" % "0.1.1-SNAPSHOT",
      "com.lihaoyi" %%% "utest" % "0.7.4" % Test
    ),

    testFrameworks += new TestFramework("utest.runner.Framework"),

    // Customize Scala Native settings
    nativeLinkStubs := true, // Set to false or remove if you want to show stubs as linking errors
    nativeMode := "debug", //"debug", //"release-fast", //"release-full",
    nativeLTO := "thin",
    nativeGC := "immix" //"boehm"

  )

lazy val libpq4sJVM    = libpq4s.jvm
lazy val libpq4sNative = libpq4s.native

// TODO: uncomment this when ready for publishing
/*
val publishSettings = Seq(

  // Your profile name of the sonatype account. The default is the same with the organization value
  sonatypeProfileName := "david-bouyssie",

  scmInfo := Some(
    ScmInfo(
      url("https://github.com/david-bouyssie/libpq4s"),
      "scm:git@github.com:david-bouyssie/libpq4s.git"
    )
  ),

  developers := List(
    Developer(
      id    = "david-bouyssie",
      name  = "David Bouyssié",
      email = "",
      url   = url("https://github.com/david-bouyssie")
    )
  ),
  description := "",
  licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")), // FIXME: update license
  homepage := Some(url("https://github.com/david-bouyssie/libpq4s")),
  pomIncludeRepository := { _ => false },
  publishMavenStyle := true,

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },

  // Workaround for issue https://github.com/sbt/sbt/issues/3570
  updateOptions := updateOptions.value.withGigahorse(false),

  useGpg := true,
  pgpPublicRing := file("~/.gnupg/pubring.kbx"),
  pgpSecretRing := file("~/.gnupg/pubring.kbx"),

  Test / skip in publish := true
)*/
