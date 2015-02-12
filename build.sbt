import AssemblyKeys._

name               := "Impulse"

version            := "0.1.0-SNAPSHOT"

organization       := "de.sciss"

scalaVersion       := "2.11.5"

crossScalaVersions := Seq("2.11.5", "2.10.4")

description        := "A simple tool to record sound impulse responses"

homepage           := Some(url("https://github.com/Sciss/" + name.value))

licenses           := Seq("GPL v3+" -> url("http://www.gnu.org/licenses/gpl-3.0.txt"))

lazy val lucreSynthVersion          = "2.14.1"

lazy val desktopVersion             = "0.6.0"

lazy val webLaFVersion              = "1.28"

lazy val scalaColliderSwingVersion  = "1.24.0"

lazy val fileUtilVersion            = "1.1.1"

libraryDependencies ++= Seq(
  "de.sciss" %% "lucresynth"              % lucreSynthVersion,
  "de.sciss" %% "desktop-mac"             % desktopVersion,
  "de.sciss" %  "weblaf"                  % webLaFVersion,
  "de.sciss" %% "scalacolliderswing-core" % scalaColliderSwingVersion,
  "de.sciss" %% "fileutil"                % fileUtilVersion
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture")

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some(if (isSnapshot.value)
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := { val n = name.value
<scm>
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
</scm>
<developers>
  <developer>
    <id>sciss</id>
    <name>Hanns Holger Rutz</name>
    <url>http://www.sciss.de</url>
  </developer>
</developers>
}

// ---- packaging ----

seq(assemblySettings: _*)

test    in assembly := ()

target  in assembly := baseDirectory.value

jarName in assembly := s"${name.value}.jar"

