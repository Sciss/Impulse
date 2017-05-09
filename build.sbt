lazy val appName  = "Impulse"
lazy val appNameL = appName.toLowerCase

name               := appName
version            := "1.0.1"
organization       := "de.sciss"
scalaVersion       := "2.12.2"
crossScalaVersions := Seq("2.12.2", "2.11.11", "2.10.6")
description        := "A simple tool to record sound impulse responses"
homepage           := Some(url("https://github.com/Sciss/" + name.value))
licenses           := Seq("GPL v3+" -> url("http://www.gnu.org/licenses/gpl-3.0.txt"))

lazy val lucreSynthVersion          = "3.11.0"
lazy val desktopVersion             = "0.7.3"
lazy val subminVersion              = "0.2.1"
lazy val scalaColliderSwingVersion  = "1.32.2"
lazy val fileUtilVersion            = "1.1.2"

libraryDependencies ++= Seq(
  "de.sciss" %% "lucresynth"              % lucreSynthVersion,
  "de.sciss" %% "desktop-mac"             % desktopVersion,
  "de.sciss" %  "submin"                  % subminVersion,
  "de.sciss" %% "scalacolliderswing-core" % scalaColliderSwingVersion,
  "de.sciss" %% "fileutil"                % fileUtilVersion
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture")

// ---- build info ----

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
  BuildInfoKey.map(homepage) { case (k, opt) => k -> opt.get },
  BuildInfoKey.map(licenses) { case (_, Seq( (lic, _) )) => "license" -> lic }
)

buildInfoPackage := "de.sciss.impulse"

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

assemblyMergeStrategy in assembly := {
  case "logback.xml" => MergeStrategy.last
  case PathList("org", "xmlpull", xs @ _*)              => MergeStrategy.first
  case PathList("org", "w3c", "dom", "events", xs @ _*) => MergeStrategy.first // bloody Apache Batik
  case x =>
    val old = (assemblyMergeStrategy in assembly).value
    old(x)
}

// ---- universal ----

// ---- packaging ----

//////////////// universal (directory) installer
lazy val pkgUniversalSettings = Seq(
  executableScriptName /* in Universal */ := appNameL,
  // NOTE: doesn't work on Windows, where we have to
  // provide manual file `SCALACOLLIDER_config.txt` instead!
  javaOptions in Universal ++= Seq(
    // -J params will be added as jvm parameters
    "-J-Xmx1024m"
    // others will be added as app parameters
    // "-Dproperty=true",
  ),
  // Since our class path is very very long,
  // we use instead the wild-card, supported
  // by Java 6+. In the packaged script this
  // results in something like `java -cp "../lib/*" ...`.
  // NOTE: `in Universal` does not work. It therefore
  // also affects debian package building :-/
  // We need this settings for Windows.
  scriptClasspath /* in Universal */ := Seq("*")
)

enablePlugins(JavaAppPackaging)

seq(pkgUniversalSettings: _*)
