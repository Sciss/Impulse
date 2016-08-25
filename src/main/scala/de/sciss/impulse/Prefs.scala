/*
 *  Prefs.scala
 *  (Impulse)
 *
 *  Copyright (c) 2012-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.impulse

import de.sciss.desktop.Desktop
import de.sciss.desktop.Preferences.Entry
import Impulse.userPrefs
import de.sciss.file._

object Prefs {
  // ---- Hardware ----

  final val noDeviceName  = "<default>"

  def defaultAudioDevice            = if (Desktop.isLinux) "Impulse" else noDeviceName
  final val defaultAudioSampleRate  = 0
  final val defaultAudioNumInputs   = 8
  final val defaultAudioNumOutputs  = 8

  def audioDevice     : Entry[String ] = userPrefs("audio-device"       )
  def audioNumInputs  : Entry[Int    ] = userPrefs("audio-num-inputs"   )
  def audioNumOutputs : Entry[Int    ] = userPrefs("audio-num-outputs"  )
  def audioSampleRate : Entry[Int    ] = userPrefs("audio-sample-rate"  )

  // ---- Routing ----

  final val defaultOutputChannel    = 1
  final val defaultOutputAmplitude  = -6
  final val defaultInputChannel     = 1
  final val defaultNumChannels      = 1

  def outputChannel   : Entry[Int    ] = userPrefs("output-channel"     )
  def outputAmplitude : Entry[Int    ] = userPrefs("output-amplitude"   )
  def inputChannel    : Entry[Int    ] = userPrefs("input-channel-offset")
  def numChannels     : Entry[Int    ] = userPrefs("num-channels"       )

  // ---- Sweep ----

  final val defaultSweepDuration    = 10
  final val defaultLowFreq          = 20
  final val defaultHighFreq         = 20000
  final val defaultReverbTail       = 4

  def sweepDuration   : Entry[Int    ] = userPrefs("sweep-duration"     )
  def lowFreq         : Entry[Int    ] = userPrefs("low-frequency"      )
  def highFreq        : Entry[Int    ] = userPrefs("high-frequency"     )
  def reverbTail      : Entry[Int    ] = userPrefs("reverb-tail"        )

  // ---- Timer ----

  final val defaultTimerDelay       = 4
  final val defaultTimerBeep        = false

  def timerDelay      : Entry[Int    ] = userPrefs("timer-delay"        )
  def timerBeep       : Entry[Boolean] = userPrefs("timer-beep"         )

  // ---- Output ----

  def defaultOutputFolder = {
    val h = userHome
    val m = h / "Music"
    if (m.isDirectory) m else h
  }

  def outputFolder    : Entry[File   ] = userPrefs("output-folder"      )
}