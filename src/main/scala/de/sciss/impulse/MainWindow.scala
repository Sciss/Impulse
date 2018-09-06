/*
 *  MainWindow.scala
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

import java.awt.Color
import java.text.SimpleDateFormat
import java.util.{Date, Locale}
import javax.swing.{JPanel, Timer}

import de.sciss.audiowidgets.{LCDFont, LCDPanel}
import de.sciss.desktop.impl.WindowImpl
import de.sciss.desktop.{DialogSource, FileDialog, OptionPane, Preferences, PrefsGUI, Util, Window, WindowHandler}
import de.sciss.file._
import de.sciss.lucre.synth.{Buffer, InMemory, Server, Synth}
import de.sciss.swingplus.{GroupPanel, Separator}
import de.sciss.synth.swing.ServerStatusPanel
import de.sciss.synth.{ControlSet, ServerConnection, SynthGraph, addToHead}
import de.sciss.{numbers, osc, synth}

import scala.swing.Swing._
import scala.swing.event.EditDone
import scala.swing.{Action, Alignment, BoxPanel, Button, Component, FlowPanel, Label, Orientation, Swing, TextField}
import scala.util.control.NonFatal

final class MainWindow extends WindowImpl { win =>

  def handler: WindowHandler = Impulse.windowHandler

  private def bold(s: String) = s"<html><body><b>$s</b></body></html>"

  private def section(s: String) = {
    val res = new Label(bold(s), Swing.EmptyIcon, Alignment.Leading)
    res.border = Swing.EmptyBorder(0, 0, 6, 0)
    res
  }

  private def folderField(prefs: Preferences.Entry[File], default: => File, title: String,
                accept: File => Option[File] = Some(_)): Component = {
    def fixDefault: File = default  // XXX TODO: Scalac bug?
    val tx = new TextField(prefs.getOrElse(default).getPath, 32 /* 16 */)
    tx.listenTo(tx)
    tx.reactions += {
      case EditDone(_) =>
        if (tx.text.isEmpty) tx.text = fixDefault.getPath
        prefs.put(new File(tx.text))
    }
    val bt = Button("…") {
      val dlg = FileDialog.folder(init = prefs.get, title = title)
      dlg.show(None).flatMap(accept).foreach { f =>
        tx.text = f.getPath
        prefs.put(f)
      }
    }
    bt.peer.putClientProperty("JButton.buttonType", "square")
    val gg = new FlowPanel(tx, bt) {
      override lazy val peer: JPanel =
        new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.TRAILING, 0, 0)) with SuperMixin {
          override def getBaseline(width: Int, height: Int): Int = {
            val res = tx.peer.getBaseline(width, height)
            res + tx.peer.getY
          }
        }
    }
    gg
  }

  private[this] val txtRecord = "Record Sweep"

  private[this] val actionRecord = new Action(txtRecord) {
    enabled = false

    def apply(): Unit = selfTimer()
  }

  private[this] val actionStop = new Action("Stop") {
    enabled = false

    def apply(): Unit = stop()
  }

  private[this] val audioServerPane = new ServerStatusPanel()
  private[this] val ggTimer         = new Label {
    font  = LCDFont()
    text  = "-000"
    preferredSize = preferredSize
    minimumSize   = preferredSize
    maximumSize   = preferredSize
    text  = "--"
  }

  private[this] val parOutChannel   = "Output Channel"
  private[this] val parFolder       = "Output Folder"

  private[this] val fileFormat = new SimpleDateFormat("'sweep'yyMMdd'_'HHmmss'.aif'", Locale.US)

  private[this] val timer = new Timer(1000, ActionListener(_ => timerBang()))

  locally {
    import PrefsGUI._

    // ---- Hardware ----

    val secHardware     = section("Hardware Settings")
    val lbAudioDevice   = label("Audio Device")
    val ggAudioDevice   = textField(Prefs.audioDevice    , Prefs.defaultAudioDevice     )
    val lbAudioChannels = label("Channels")
    val ggNumInputs     = intField(Prefs.audioNumInputs  , Prefs.defaultAudioNumInputs  , min = 1, max = 8192)
    val ggNumOutputs    = intField(Prefs.audioNumOutputs , Prefs.defaultAudioNumOutputs , min = 1, max = 8192)
    val pnAudioChannels = new BoxPanel(Orientation.Horizontal) {
      contents += new Label("In")
      contents += HStrut(4)  // GroupPanel.Gap(4)
      contents += ggNumInputs
      contents += HStrut(12)
      contents += new Label("Out")
      contents += HStrut(4)
      contents += ggNumOutputs
    }
    val lbSampleRate    = label("Sample Rate [Hz]")
    val ggSampleRate    = intField(Prefs.audioSampleRate , Prefs.defaultAudioSampleRate, max = 384000)
    audioServerPane.background = Color.lightGray
    audioServerPane.bootAction = Some(() => boot())

    // ---- Routing ----

    val secRouting      = section("Routing")
    val sepRouting      = Separator()
    val lbOutChannel    = label(parOutChannel)
    val ggOutChannel    = intField(Prefs.outputChannel   , Prefs.defaultOutputChannel  , min = 1, max = 8192)
    val lbOutAmplitude  = label("Output Amplitude [dB]")
    val ggOutAmplitude  = intField(Prefs.outputAmplitude , Prefs.defaultOutputAmplitude, min = -80, max = 0)
    val lbInputs        = label("Input Channels")
    val ggInChannel     = intField(Prefs.inputChannel    , Prefs.defaultInputChannel   , min = 1, max = 8192)
    val ggNumChannels   = intField(Prefs.numChannels     , Prefs.defaultNumChannels    , min = 1, max = 8192)
    val pnInputs        = new BoxPanel(Orientation.Horizontal) {
      contents += new Label("Off")
      contents += HStrut(4)
      contents += ggInChannel
      contents += HStrut(12)
      contents += new Label("Num")
      contents += HStrut(4)
      contents += ggNumChannels
    }

    // ---- Sweep ----

    val secSweep        = section("Sweep Parameters")
    val sepSweep        = Separator()
    val lbSweepDur      = label("Sweep Duration [sec]")
    val ggSweepDur      = intField(Prefs.sweepDuration   , Prefs.defaultSweepDuration  , min = 1, max = 3600)
    val lbFreq          = label("Frequency Range [Hz]")
    val ggLowFreq       = intField(Prefs.lowFreq         , Prefs.defaultLowFreq        , min = 1, max = 384000)
    val ggHighFreq      = intField(Prefs.highFreq        , Prefs.defaultHighFreq       , min = 1, max = 384000)
    val lbReverbTail    = label("Reverb Tail [sec]")
    val ggReverbTail    = intField(Prefs.reverbTail      , Prefs.defaultReverbTail     , min = 1, max = 360)
    val pnFreq = new BoxPanel(Orientation.Horizontal) {
      contents += new Label("Low")
      contents += HStrut(4)
      contents += ggLowFreq
      contents += HStrut(12)
      contents += new Label("Freq")
      contents += HStrut(4)
      contents += ggHighFreq
    }

    // ---- Timer ----

    val secTimer        = section("Self Timer")
    val sepTimer        = Separator()
    val lbTimerDelay    = label("Pre-Delay [sec]")
    val ggTimerDelay    = intField(Prefs.timerDelay      , Prefs.defaultTimerDelay)
    val lbTimerBeep     = label("Beep Signal")
    val ggTimerBeep     = checkBox(Prefs.timerBeep       , Prefs.defaultTimerBeep)
ggTimerBeep.enabled = false  // XXX TODO

    // ---- Output ----

    val secOutput       = section(parFolder)
    val sepOutput       = Separator()
    val ggOutput        = folderField(Prefs.outputFolder, Prefs.defaultOutputFolder, "Output Folder")

    // ---- Action ----

    val secAction       = section("Action")
    val sepAction       = Separator()
    val ggRecord        = new Button(actionRecord)
    ggRecord.tooltip    = "Ctrl-R"
    val ggStop          = new Button(actionStop  )
    ggStop.tooltip      = "Ctrl-."
    val pnActions      = new BoxPanel(Orientation.Horizontal) {
      background = Color.lightGray
      contents += ggRecord
      contents += HStrut(8)
      contents += ggStop
      contents += HStrut(8)
      contents += new LCDPanel {
        contents += ggTimer
      }
    }

    val box = new GroupPanel {
      horizontal = Par(
        secHardware,
        audioServerPane,
        sepRouting, secRouting,
        sepSweep, secSweep,
        sepTimer, secTimer, sepOutput, secOutput, ggOutput, sepAction, secAction,
        pnActions,
        Seq(
          Par(lbAudioDevice, lbAudioChannels, lbSampleRate, lbOutChannel, lbOutAmplitude, lbInputs,
            lbSweepDur, lbFreq, lbReverbTail, lbTimerDelay, lbTimerBeep),
          Par(ggAudioDevice, pnAudioChannels, ggSampleRate, ggOutChannel, ggOutAmplitude, pnInputs,
            ggSweepDur, pnFreq, ggReverbTail, ggTimerDelay, ggTimerBeep)
        )
      )
      vertical = Seq(
        secHardware,
        Par(Baseline)(lbAudioDevice     , ggAudioDevice     ),
        Par(Baseline)(lbAudioChannels   , pnAudioChannels   ),
        Par(Baseline)(lbSampleRate      , ggSampleRate      ),
        audioServerPane,
        sepRouting, secRouting,
        Par(Baseline)(lbOutChannel      , ggOutChannel      ),
        Par(Baseline)(lbOutAmplitude    , ggOutAmplitude    ),
        Par(Baseline)(lbInputs          , pnInputs          ),
        sepSweep  , secSweep,
        Par(Baseline)(lbSweepDur        , ggSweepDur        ),
        Par(Baseline)(lbFreq            , pnFreq            ),
        Par(Baseline)(lbReverbTail      , ggReverbTail      ),
        sepTimer  , secTimer,
        Par(Baseline)(lbTimerDelay      , ggTimerDelay      ),
        Par(Baseline)(lbTimerBeep       , ggTimerBeep       ),
        sepOutput , secOutput,
        ggOutput,
        sepAction , secAction,
        pnActions
      )
    }

    contents = box
  }

  closeOperation = Window.CloseIgnore
  reactions += {
    case Window.Closing(_) =>
      // XXX TODO - check ongoing recording
      val so = serverOption
      serverOption = None
      so.foreach(_.peer.quit())
      Impulse.quit()
  }

  title = s"${Impulse.name} v${Impulse.version}"
  pack()
  Util.centerOnScreen(this)
  front()

  // ---- timer ----

  def selfTimer(): Unit = {
    val dly     = Prefs.timerDelay.getOrElse(Prefs.defaultTimerDelay)
    val beep    = Prefs.timerBeep .getOrElse(Prefs.defaultTimerBeep )
    val tail    = Prefs.reverbTail.getOrElse(Prefs.defaultReverbTail)
    timerCount = -(if (beep) dly + tail + 1 else dly)
    updateTimerDisplay()
    if (timerCount < 0) timer.restart() else tryRecord()
  }

  private def updateTimerDisplay(): Unit =
    ggTimer.text = timerCount.toString

  private var timerCount = 0

  private def timerBang(): Unit = {
    timerCount += 1
    updateTimerDisplay()
    if (timerCount == 0) tryRecord()
  }

  private def tryRecord(): Unit = try {
      record()
    } catch {
      case InvalidParameter(name, value, explanation) =>
        val message = new Label(
          s"<html><body>Parameter '<b>$name</b>' has an invalid setting <b>$value</b>.<p>$explanation</body></html")
        val opt = OptionPane.message(message, OptionPane.Message.Error)
        opt.show(Some(win), txtRecord)
      case NonFatal(e) =>
        DialogSource.Exception(e -> txtRecord).show(Some(win))
    }

  // ---- boot ----

  def boot(): Unit = {
    val config                = Server.Config()
    val audioDevice           = Prefs.audioDevice     .getOrElse(Prefs.defaultAudioDevice)
    if (audioDevice != Prefs.noDeviceName) config.deviceName = Some(audioDevice)
    val numOutputs            = Prefs.audioNumOutputs .getOrElse(Prefs.defaultAudioNumOutputs)
    val numInputs             = Prefs.audioNumInputs  .getOrElse(Prefs.defaultAudioNumInputs )
    config.program            = Prefs.scsynth         .getOrElse(Prefs.defaultScsynth        )
    config.outputBusChannels  = numOutputs
    config.inputBusChannels   = numInputs
    config.sampleRate         = Prefs.audioSampleRate .getOrElse(Prefs.defaultAudioSampleRate)
    config.transport          = osc.TCP
    config.pickPort()

    //    TxnExecutor.defaultAtomic { implicit itx =>
    //      implicit val tx = Txn.wrap(itx)
    //      // auralSystem.start(config)
    //    }

    val con = synth.Server.boot("Impulse", config) {
      case ServerConnection.Running(s) => booted(s)
      case ServerConnection.Aborted =>
    }
    audioServerPane.booting = Some(con)
  }

  private[this] var serverOption = Option.empty[Server]

  private[this] val cursor    = InMemory()

  private[this] val ctlLoFreq = "lo-freq"
  private[this] val ctlHiFreq = "hi-freq"
  private[this] val ctlDur    = "dur"
  private[this] val ctlAmp    = "amp"
  private[this] val ctlTail   = "tail"
  private[this] val ctlOut    = "out"
  private[this] val ctlIn     = "in"
  private[this] val ctlBuf    = "buf"

  private def mkSynthGraph(numInChannels: Int) = SynthGraph {
    import synth._
    import ugen._
    import Ops._

    val low   = ctlLoFreq.ir(20f)
    val high  = ctlHiFreq.ir(20000f)
    val dur   = ctlDur.ir(10f)
    val freq  = Line.ar(low, high, dur)
    val amp   = ctlAmp.ir(0.5f)
    val tail  = ctlTail.ir(1f)
    val out   = ctlOut.ir
    val env   = EnvGen.ar(Env.linen(0.02, dur - 0.04, 0.02, curve = Curve.sine), levelScale = amp)
    val osc   = SinOsc.ar(freq) * env
    Out.ar(out, osc)

    val in    = In.ar(NumOutputBuses.ir + ctlIn.ir, numInChannels)
    val buf   = ctlBuf.ir
    val rec   = Flatten(Seq(osc, in))
    DiskOut.ar(buf, rec)

    val tEnd  = TDelay.kr(Done.kr(env), tail)
    FreeSelf.kr(tEnd)
  }

  private def booted(s: synth.Server): Unit = {
    audioServerPane.booting = None
    audioServerPane.server  = Some(s)
    val s1 = Server(s)
    s.addListener {
      case synth.Server.Offline =>
//        cursor.step { implicit tx =>
//          NodeGraph.removeServer(s1)
//        }
        onEDT {
          serverOption            = None
          audioServerPane.server  = None
          actionRecord.enabled    = false
          actionStop  .enabled    = false
        }
    }
//    cursor.step { implicit tx =>
//      NodeGraph.addServer(s1)
//    }
    onEDT {
      serverOption            = Some(s1)
      actionRecord.enabled    = true
      actionStop  .enabled    = true
    }
  }

  private def synthChanged(active: Boolean): Unit = onEDT {
    actionRecord.enabled = !active && serverOption.isDefined
    if (!active) {
      timer.stop()
      ggTimer.text = "--"
    }
  }

  // ---- record ----

  def record(): Unit = serverOption.foreach { s =>
    val numInChannels = Prefs.numChannels     .getOrElse(Prefs.defaultNumChannels     )
    val loFreq        = Prefs.lowFreq         .getOrElse(Prefs.defaultLowFreq         )
    val hiFreq        = Prefs.highFreq        .getOrElse(Prefs.defaultHighFreq        )
    val dur           = Prefs.sweepDuration   .getOrElse(Prefs.defaultSweepDuration   )
    val ampDb         = Prefs.outputAmplitude .getOrElse(Prefs.defaultOutputAmplitude )
    val tail          = Prefs.reverbTail      .getOrElse(Prefs.defaultReverbTail      )
    val outCh         = Prefs.outputChannel   .getOrElse(Prefs.defaultOutputChannel   )
    val outIndex      = outCh - 1
    val inCh          = Prefs.inputChannel    .getOrElse(Prefs.defaultInputChannel    )
    val inIndex       = inCh - 1
    val folder        = Prefs.outputFolder    .getOrElse(Prefs.defaultOutputFolder    )

    if (numInChannels < 1) throw InvalidParameter("Number of Input Channels", numInChannels, "Must be greater than zero")
    if (numInChannels > s.config.inputBusChannels)
      throw InvalidParameter("Number of Input Channels", numInChannels,
        s"Exceeds number of audio input bus channels (${s.config.inputBusChannels})")

    if (outCh < 1) throw InvalidParameter(parOutChannel, outCh, "Must be greater than zero")
    if (outCh > s.config.outputBusChannels)
      throw InvalidParameter(parOutChannel, outCh,
        s"Exceeds number of audio output bus channels (${s.config.outputBusChannels})")

    if (inCh < 1) throw InvalidParameter("Input Channel Offset", inCh, "Must be greater than zero")
    if (inCh > s.config.inputBusChannels)
      throw InvalidParameter("Input Channel Offset", inCh,
        s"Exceeds number of audio input bus channels (${s.config.inputBusChannels})")

    if (inIndex + numInChannels > s.config.inputBusChannels)
      throw InvalidParameter("Input Channel Range", inCh until (inCh + numInChannels),
        s"Exceeds number of audio input bus channels (${s.config.inputBusChannels})")

    if (!folder.isDirectory || !folder.canWrite)
      throw InvalidParameter(parFolder, folder, "Must be a writable directory")

    val date  = new Date()
    val f     = folder / fileFormat.format(date)
    println(f.path)

    import numbers.Implicits._

    cursor.step { implicit tx =>
      val x = Synth(s, mkSynthGraph(numInChannels), Some(s"Sweep-$numInChannels"))
      val b = Buffer.diskOut(s)(path = f.absolutePath, numChannels = numInChannels + 1)
      val args = List[ControlSet](
        ctlLoFreq -> loFreq,
        ctlHiFreq -> hiFreq,
        ctlDur    -> dur,
        ctlAmp    -> ampDb.dbamp,
        ctlTail   -> tail,
        ctlOut    -> outIndex,
        ctlIn     -> inIndex,
        ctlBuf    -> b.id
      )

      x.play(s.defaultGroup, args, addToHead, b :: Nil)
      x.onEndTxn { implicit tx =>
        b.dispose()
        tx.afterCommit {
          synthChanged(active = false)
        }
      }
    }
    onEDT(synthChanged(active = true))
  }

  def stop(): Unit = {
    serverOption.foreach { s =>
      cursor.step { implicit tx =>
        s.defaultGroup.freeAll()
      }
    }
    // timer.stop()
  }
}