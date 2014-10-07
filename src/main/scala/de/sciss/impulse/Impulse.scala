/*
 *  Impulse.scala
 *  (Impulse)
 *
 *  Copyright (c) 2012-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.impulse

import com.alee.laf.WebLookAndFeel
import de.sciss.desktop.Menu
import de.sciss.desktop.Menu.Root
import de.sciss.desktop.impl.SwingApplicationImpl
import de.sciss.osc
import de.sciss.lucre.synth.{Txn, Server}

import scala.concurrent.stm.TxnExecutor

object Impulse extends SwingApplicationImpl("Impulse") {
  type Document = Unit

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
    WebLookAndFeel.install()
    new MainWindow
  }
}
