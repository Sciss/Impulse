/*
 *  InvalidParameter.scala
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

final case class InvalidParameter(name: String, value: Any, explanation: String) extends RuntimeException
