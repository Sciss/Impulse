/*
 *  Impulse.scala
 *  (Impulse)
 *
 *  Copyright (c) 2014-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.impulse

import de.sciss.desktop.Menu.Root
import de.sciss.desktop.impl.SwingApplicationImpl
import de.sciss.desktop.{Desktop, Menu, OptionPane, PathField}
import de.sciss.file._
import de.sciss.submin.Submin
import de.sciss.synth.Server

import scala.swing.{BoxPanel, Label, Orientation, Swing}
import scala.util.control.NonFatal

object Impulse extends SwingApplicationImpl("Impulse") {
  type Document = Unit

  def version : String = buildInfString("version")
  def license : String = buildInfString("license")
  def homepage: String = buildInfString("homepage")

  private def buildInfString(key: String): String = try {
    val clazz = Class.forName("de.sciss.impulse.BuildInfo")
    val m     = clazz.getMethod(key)
    m.invoke(null).toString
  } catch {
    case NonFatal(_) => "?"
  }

  protected lazy val menuFactory: Root = {
    import Menu._
    val itQuit  = Item.Quit(Impulse)

    val res = Root()

    if (itQuit.visible) {
      Root().add(
        Group("file", "File").add(itQuit)
      )
    }
    res
  }

  override protected def init(): Unit = {
    Submin.install(false) // dark currently has glitches
    checkSuperCollider()
    new MainWindow
  }

  def checkSuperCollider(): Unit = {
    val config      = Server.Config()
    config.program  = Prefs.scsynth.getOrElse(Prefs.defaultScsynth)
    val found       = Server.version(config).isSuccess

    if (!found) {
      val scsynthName = if (Desktop.isWindows) "scsynth.exe" else "scsynth"
      val ggPath      = new PathField
      ggPath.value    = file(config.program)
      ggPath.title    = s"Locate $scsynthName (SuperCollider server)"

      val lbInfo = new Label(
        s"""<HTML><BODY>SuperCollider server ($scsynthName) is not found.<br>
           |Please specify its path:</BODY>"
           |""".stripMargin)
      val pane = new BoxPanel(Orientation.Vertical) {
        contents += lbInfo
        contents += Swing.VStrut(4)
        contents += ggPath
        contents += Swing.VStrut(4)
      }
      val opt   = OptionPane(pane, OptionPane.Options.OkCancel)
      opt.title = "Locate SuperCollider Server"
      val res   = opt.show()
      if (res == OptionPane.Result.Ok) {
        Prefs.scsynth.put(ggPath.value.path)
      }
    }
  }
}
