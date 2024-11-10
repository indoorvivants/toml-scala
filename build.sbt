val Scala2_12  = "2.12.19"
val Scala2_13  = "2.13.14"
val Scala3     = "3.3.4"
val FastParse  = "3.1.1"
val Shapeless  = "2.3.12"
val ScalaCheck = "1.18.1"
val ScalaTest  = "3.2.19"
val ScalaJavaTime = "2.6.0"
val AllScalaVersions = Seq(Scala2_12, Scala2_13, Scala3)

val ScalaTestScalaCheck  = s"$ScalaTest.0"


inThisBuild(
  List(
    organization               := "com.indoorvivants",
    organizationName           := "Anton Sviridov",
    homepage := Some(
      url("https://github.com/indoorvivants/toml-scala")
    ),
    startYear := Some(2022),
    licenses := List(
      "MPL-2.0" -> url("https://opensource.org/licenses/MPL-2.0")
    ),
    developers := List(
      Developer(
        "tindzk",
        "Tim Nieradzik",
        "",
        url("http://github.com/tindzk")
      ),
      Developer(
        "keynmol",
        "Anton Sviridov",
        "keynmol@gmail.com",
        url("https://blog.indoorvivants.com")
      )
    )
  )
)

lazy val root = project.in(file("."))
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
        "org.scalatest"  %%% "scalatest"  % ScalaTest  % Test,
        "org.scalatestplus" %%% s"scalacheck-${ScalaCheck.split('.').take(2).mkString("-")}" % ScalaTestScalaCheck % Test
      ),
      libraryDependencies ++= {
        if(scalaVersion.value.startsWith("3."))
          Seq.empty
        else
          Seq("com.chuusai" %%% "shapeless" % Shapeless)
      },
      libraryDependencies ++= {
        if(virtualAxes.value.contains(VirtualAxis.jvm)) Seq.empty else 
        Seq(
          "io.github.cquiroz" %%% "scala-java-time" % ScalaJavaTime,
        )
      }
    )


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
  val parallelism = 
    if(sys.env.contains("CI")) 1 
    else (java.lang.Runtime.getRuntime().availableProcessors() - 2).max(1)
  Seq(
    Tags.limit(Tags.Test, parallelism),
    // By default dependencies of test can be run in parallel, it includeds Scala Native/Scala.js linkers
    // Limit them to lower memory usage, especially when targetting LLVM
    Tags.limit(NativeTags.Link, parallelism),
    Tags.limit(ScalaJSTags.Link, parallelism)
  )
}

