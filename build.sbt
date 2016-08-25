name               := "Impulse"
version            := "0.1.0-SNAPSHOT"
organization       := "de.sciss"
scalaVersion       := "2.11.8"
crossScalaVersions := Seq("2.11.8", "2.10.6")
description        := "A simple tool to record sound impulse responses"
homepage           := Some(url("https://github.com/Sciss/" + name.value))
licenses           := Seq("GPL v3+" -> url("http://www.gnu.org/licenses/gpl-3.0.txt"))

lazy val lucreSynthVersion          = "3.5.2"
lazy val desktopVersion             = "0.7.2"
lazy val subminVersion              = "0.2.1"
lazy val scalaColliderSwingVersion  = "1.29.0"
lazy val fileUtilVersion            = "1.1.1"

libraryDependencies ++= Seq(
  "de.sciss" %% "lucresynth"              % lucreSynthVersion,
  "de.sciss" %% "desktop-mac"             % desktopVersion,
  "de.sciss" %  "submin"                  % subminVersion,
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

test            in assembly := ()
target          in assembly := baseDirectory.value
assemblyJarName in assembly := s"${name.value}.jar"

