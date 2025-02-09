val Scala2_12 = "2.12.20"
val Scala2_13 = "2.13.16"
val Scala3 = "3.3.5"
val FastParse = "3.1.1"
val Shapeless = "2.3.12"
val Shapeless3 = "3.4.3"
val ScalaCheck = "1.18.1"
val ScalaTest = "3.2.19"
val ScalaJavaTime = "2.6.0"
val AllScalaVersions = Seq(Scala2_12, Scala2_13, Scala3)

val ScalaTestScalaCheck = s"$ScalaTest.0"

inThisBuild(
  List(
    organization := "com.indoorvivants",
    organizationName := "Anton Sviridov",
    homepage := Some(
      url("https://github.com/indoorvivants/toml-scala"),
    ),
    startYear := Some(2022),
    licenses := List(
      "MPL-2.0" -> url("https://opensource.org/licenses/MPL-2.0"),
    ),
    developers := List(
      Developer(
        "tindzk",
        "Tim Nieradzik",
        "",
        url("http://github.com/tindzk"),
      ),
      Developer(
        "keynmol",
        "Anton Sviridov",
        "keynmol@gmail.com",
        url("https://blog.indoorvivants.com"),
      ),
    ),
  ),
)

lazy val root = project
  .in(file("."))
  .aggregate(toml.projectRefs*)
  .settings(publish / skip := true)
  .settings(sources := Seq.empty)

lazy val toml =
  projectMatrix
    .jvmPlatform(AllScalaVersions)
    .jsPlatform(AllScalaVersions)
    .nativePlatform(AllScalaVersions)
    .in(file("core"))
    .settings(superMatrix)
    .settings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "fastparse" % FastParse,
        "org.scalacheck" %%% "scalacheck" % ScalaCheck % Test,
        "org.scalatest" %%% "scalatest" % ScalaTest % Test,
        "org.scalatestplus" %%% s"scalacheck-${ScalaCheck.split('.').take(2).mkString("-")}" % ScalaTestScalaCheck % Test,
      ),
      libraryDependencies ++= {
        if (scalaVersion.value.startsWith("3."))
          Seq("org.typelevel" %%% "shapeless3-deriving" % Shapeless3)
        else
          Seq("com.chuusai" %%% "shapeless" % Shapeless)
      },
      libraryDependencies ++= {
        if (virtualAxes.value.contains(VirtualAxis.jvm)) Seq.empty
        else
          Seq(
            "io.github.cquiroz" %%% "scala-java-time" % ScalaJavaTime,
          )
      },
      scalacOptions ++= {
        val opts = Seq.newBuilder[String]
        if (scalaBinaryVersion.value.startsWith("2.")) opts += "-Xsource:3"
        if (scalaBinaryVersion.value.startsWith("2.")) opts += "-Ywarn-unused"
        if (scalaBinaryVersion.value.startsWith("2.12"))
          opts += "-Ywarn-unused-import"
        if (scalaBinaryVersion.value.startsWith("3")) opts += "-Wunused:imports"
        opts.result()
      },
    )

lazy val docs = project
  .in(file("target/docs-mdoc"))
  .dependsOn(toml.jvm(Scala3))
  .enablePlugins(MdocPlugin)
  .settings(scalaVersion := Scala3)

lazy val superMatrix = Seq((Compile / unmanagedSourceDirectories) ++= {
  val allCombos = List("js", "jvm", "native").combinations(2).toList
  val dis =
    virtualAxes.value.collectFirst { case p: VirtualAxis.PlatformAxis =>
      p.directorySuffix
    }.get

  allCombos
    .filter(_.contains(dis))
    .map { suff =>
      val suffixes = "scala" + suff.mkString("", "-", "")

      (Compile / sourceDirectory).value / suffixes
    }
})

concurrentRestrictions in Global ++= {
  if (sys.env.contains("CI"))
    Seq(
      Tags.limit(Tags.Test, 1),
      // By default dependencies of test can be run in parallel, it includeds Scala Native/Scala.js linkers
      // Limit them to lower memory usage, especially when targetting LLVM
      Tags.limit(NativeTags.Link, 1),
      Tags.limit(ScalaJSTags.Link, 1),
    )
  else Seq.empty
}

val scalafixRules = Seq(
  "OrganizeImports",
  "DisableSyntax",
  "LeakingImplicitClassVal",
  "NoValInForComprehension",
).mkString(" ")

val CICommands = Seq(
  "clean",
  "scalafixEnable",
  "compile",
  "test",
  "docs/mdoc --in README.md",
  "scalafmtCheckAll",
  "scalafmtSbtCheck",
  s"scalafix --check $scalafixRules",
).mkString(";")

val PrepareCICommands = Seq(
  "clean",
  "scalafixEnable",
  "compile",
  "Test/compile",
  s"scalafix --rules $scalafixRules",
  "scalafmtAll",
  "scalafmtSbt",
).mkString(";")

addCommandAlias("ci", CICommands)

addCommandAlias("preCI", PrepareCICommands)
