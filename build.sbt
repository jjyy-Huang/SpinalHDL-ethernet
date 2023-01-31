ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.12.13"

val spinalVersion = "1.7.1"
val spinalCore = "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion
val spinalLib = "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion
val spinalIdslPlugin = compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")
lazy val template = (project in file("."))
  .settings(
    name := "test",
    Compile / scalaSource := baseDirectory.value / "src" / "main" / "scala",
    libraryDependencies ++= Seq(spinalCore, spinalLib, spinalIdslPlugin)
  )


fork := true