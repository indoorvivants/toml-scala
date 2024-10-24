// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val Scala2_12  = "2.12.19"
val Scala2_13  = "2.13.14"
val Scala3     = "3.3.4"
val FastParse  = "3.1.1"
val Shapeless  = "2.3.12"
val ScalaCheck = "1.18.1"
val ScalaTest  = "3.2.19"
val AllScalaVersions = Seq(Scala2_12, Scala2_13, Scala3)

val ScalaTestScalaCheck  = s"$ScalaTest.0"

val SharedSettings = Seq(
  name         := "toml-scala",
  organization := "com.indoorvivants",

  // scalaVersion       := Scala2_13,
  // crossScalaVersions := Seq(Scala3, Scala2_13, Scala2_12),

  pomExtra :=
    <url>https://github.com/sparsetech/toml-scala</url>
    <licenses>
      <license>
        <name>MPL-2.0 License</name>
        <url>https://opensource.org/licenses/MPL-2.0</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:indoorvivants/toml-scala.git</url>
    </scm>
    <developers>
      <developer>
        <id>tindzk</id>
        <name>Tim Nieradzik</name>
        <url>http://github.com/tindzk</url>
      </developer>
    </developers>
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
    .settings(SharedSettings)
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
