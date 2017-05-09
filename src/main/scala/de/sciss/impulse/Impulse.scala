/*
 *  Impulse.scala
 *  (Impulse)
 *
 *  Copyright (c) 2014-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.impulse

import de.sciss.desktop.Menu
import de.sciss.desktop.Menu.Root
import de.sciss.desktop.impl.SwingApplicationImpl
import de.sciss.submin.Submin

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
    new MainWindow
  }
}
