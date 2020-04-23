lazy val appName  = "Impulse"
lazy val appNameL = appName.toLowerCase

lazy val dep = new {
  val main = new {
    val audioFile           = "1.5.4"
    val audioWidgets        = "1.14.4"
    val desktop             = "0.10.6"
    val fileUtil            = "1.1.3"
    val lucre               = "3.17.0"
    val lucreSynth          = "3.35.2"
    val model               = "0.3.4"
    val numbers             = "0.2.0"
    val scalaCollider       = "1.28.5"
    val scalaColliderSwing  = "1.41.6"
    val scalaColliderUGens  = "1.19.6"
    val scalaOSC            = "1.2.1"
    val scalaSwing          = "2.1.1"
    val submin              = "0.3.4"
    val swingPlus           = "0.4.2"
  }
}

lazy val commonSettings = Seq(
  name               := appName,
  version            := "1.1.0",
  organization       := "de.sciss",
  scalaVersion       := "2.13.2",
  crossScalaVersions := Seq("2.12.11", "2.13.2"),
  description        := "A simple tool to record sound impulse responses",
  homepage           := Some(url(s"https://git.iem.at/sciss/${name.value}")),
  licenses           := Seq("GPL v3+" -> url("http://www.gnu.org/licenses/gpl-3.0.txt")),
  libraryDependencies ++= Seq(
    "de.sciss" %% "audiofile"               % dep.main.audioFile,
    "de.sciss" %% "audiowidgets-core"       % dep.main.audioWidgets,
    "de.sciss" %% "audiowidgets-swing"      % dep.main.audioWidgets,
    "de.sciss" %% "desktop-core"            % dep.main.desktop,
    "de.sciss" %% "desktop-linux"           % dep.main.desktop,
    "de.sciss" %% "desktop-mac"             % dep.main.desktop,
    "de.sciss" %% "fileutil"                % dep.main.fileUtil,
    "de.sciss" %% "lucre-base"              % dep.main.lucre,
    "de.sciss" %% "lucre-core"              % dep.main.lucre,
    "de.sciss" %% "lucre-synth"             % dep.main.lucreSynth,
    "de.sciss" %% "model"                   % dep.main.model,
    "de.sciss" %% "numbers"                 % dep.main.numbers,
    "de.sciss" %% "scalacollider"           % dep.main.scalaCollider,
    "de.sciss" %% "scalacolliderswing-core" % dep.main.scalaColliderSwing,
    "de.sciss" %% "scalacolliderugens-api"  % dep.main.scalaColliderUGens,
    "de.sciss" %% "scalacolliderugens-core" % dep.main.scalaColliderUGens,
    "de.sciss" %% "scalaosc"                % dep.main.scalaOSC,
    "de.sciss" %  "submin"                  % dep.main.submin,
    "de.sciss" %% "swingplus"               % dep.main.swingPlus,
    "org.scala-lang.modules" %% "scala-swing" % dep.main.scalaSwing,
  ),
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture")
)

lazy val root = project.in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(JlinkPlugin)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(assemblySettings)
  .settings(useNativeZip) // cf. https://github.com/sbt/sbt-native-packager/issues/334
  .settings(pkgUniversalSettings)
  .settings(
    jlinkIgnoreMissingDependency := JlinkIgnore.everything, // temporary for testing
    buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
      BuildInfoKey.map(homepage) { case (k, opt) => k -> opt.get },
      BuildInfoKey.map(licenses) { case (_, Seq( (lic, _) )) => "license" -> lic }
    ),
    buildInfoPackage := "de.sciss.impulse"
  )

// ---- publishing ----

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := { val n = name.value
    <scm>
      <url>git@git.iem.at:sciss/{n}.git</url>
      <connection>scm:git:git@git.iem.at:sciss/{n}.git</connection>
    </scm>
    <developers>
      <developer>
        <id>sciss</id>
        <name>Hanns Holger Rutz</name>
        <url>http://www.sciss.de</url>
      </developer>
    </developers>
  }
)

// ---- packaging ----

lazy val assemblySettings = Seq(
  test            in assembly := {},
  target          in assembly := baseDirectory.value,
  assemblyJarName in assembly := s"${name.value}.jar",
  assemblyMergeStrategy in assembly := {
    case "logback.xml" => MergeStrategy.last
    case PathList("org", "xmlpull", xs @ _*)              => MergeStrategy.first
    case PathList("org", "w3c", "dom", "events", xs @ _*) => MergeStrategy.first // bloody Apache Batik
    case x =>
      val old = (assemblyMergeStrategy in assembly).value
      old(x)
  }
)

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

