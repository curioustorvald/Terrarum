package net.torvald.terrarum.debuggerapp

import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.SetAV
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorValue
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.mapdrawer.MapDrawer
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*

/**
 * Created by SKYHi14 on 2016-12-29.
 */
class ActorValueTracker constructor() : JFrame() {

    constructor(actor: Actor) : this() {
        setTrackingActor(actor)
    }

    private val selectedActorLabel = JLabel("Actor not selected")
    private val avInfoArea = JTextArea()
    private val avInfoScroller = JScrollPane(avInfoArea)
    private val avPosArea = JTextArea()
    private val avPosScroller = JScrollPane(avPosArea)
    private var actor: ActorWithBody? = null
    private var actorValue: ActorValue? = null

    private val modavInputKey = JTextField()
    private val modavInputValue = JTextField()

    private val buttonAddAV = JButton("Add/Mod")
    private val buttonDelAV = JButton("Delete")

    init {
        title = "Actor value tracker"
        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE

        val divPanel = JPanel()
        divPanel.layout = BorderLayout(0, 2)

        avInfoArea.highlighter = null // prevent text-drag-crash
        avPosArea.highlighter = null // prevent text-drag-crash

        avInfoScroller.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        avPosScroller.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER


        // button listener for buttons
        buttonAddAV.addMouseListener(object : MouseListener {
            override fun mouseEntered(e: MouseEvent?) { }
            override fun mouseClicked(e: MouseEvent?) { }
            override fun mouseReleased(e: MouseEvent?) { }
            override fun mouseExited(e: MouseEvent?) { }
            override fun mousePressed(e: MouseEvent?) {
                if (actor != null && modavInputKey.text.isNotBlank() && modavInputValue.text.isNotBlank()) {
                    SetAV.execute((
                            "setav;" +
                            "${actor!!.referenceID};" +
                            "${modavInputKey.text};" +
                            "${modavInputValue.text}"
                                  ).split(';').toTypedArray())
                }
            }
        })
        buttonDelAV.addMouseListener(object : MouseListener {
            override fun mouseEntered(e: MouseEvent?) { }
            override fun mouseClicked(e: MouseEvent?) { }
            override fun mouseReleased(e: MouseEvent?) { }
            override fun mouseExited(e: MouseEvent?) { }
            override fun mousePressed(e: MouseEvent?) {
                if (actorValue != null && modavInputKey.text.isNotBlank()) {
                    actorValue!!.remove(modavInputKey.text)
                    Echo("${SetAV.ccW}Removed key ${SetAV.ccG}${modavInputKey.text} ${SetAV.ccW}of ${SetAV.ccY}${actor!!.referenceID}")
                    println("[ActorValueTracker] Removed key '${modavInputKey.text}' of $actor")
                }
            }
        })


        // panel elements
        divPanel.add(selectedActorLabel, BorderLayout.PAGE_START)
        val posAndAV = JPanel()
        posAndAV.layout = BorderLayout()
        posAndAV.add(avPosScroller, BorderLayout.PAGE_START)
        posAndAV.add(avInfoScroller, BorderLayout.CENTER)

        divPanel.add(posAndAV, BorderLayout.CENTER)

        val toolbox = JPanel()
        toolbox.layout = BorderLayout()

        val toolpanel = JPanel()
        toolpanel.layout = GridLayout(1, 0)
        toolpanel.add(buttonAddAV)
        toolpanel.add(buttonDelAV)

        val modpanelLabels = JPanel()
        modpanelLabels.layout = BorderLayout(4, 0)
        modpanelLabels.add(JLabel("Key"), BorderLayout.PAGE_START)
        modpanelLabels.add(JLabel("Value"), BorderLayout.CENTER)

        val modpanelFields = JPanel()
        modpanelFields.layout = BorderLayout(4, 2)
        modpanelFields.add(modavInputKey, BorderLayout.PAGE_START)
        modpanelFields.add(modavInputValue, BorderLayout.CENTER)

        val modpanel = JPanel()
        modpanel.layout = BorderLayout(4, 2)
        modpanel.add(modpanelLabels, BorderLayout.LINE_START)
        modpanel.add(modpanelFields, BorderLayout.CENTER)

        toolbox.add(toolpanel, BorderLayout.PAGE_START)
        toolbox.add(modpanel, BorderLayout.CENTER)
        modpanel.add(JLabel(
                "<html>Messed-up type or careless delete will crash the game.</html>"
        ), BorderLayout.PAGE_END)

        divPanel.add(toolbox, BorderLayout.PAGE_END)


        this.add(divPanel)
        this.setSize(300, 600)
        this.isVisible = true
    }

    fun setTrackingActor(actor: Actor) {
        this.actorValue = actor.actorValue

        selectedActorLabel.text = "Actor: $actor"
        this.title = "AVTracker — $actor"

        if (actor is ActorWithBody) {
            this.actor = actor
        }

        setInfoLabel()
    }

    fun setInfoLabel() {
        val sb = StringBuilder()

        if (actor != null) {
            sb.append("X: ${actor!!.hitbox.pointedX} (${(actor!!.hitbox.pointedX / MapDrawer.TILE_SIZE).toInt()})\n")
            sb.append("Y: ${actor!!.hitbox.pointedY} (${(actor!!.hitbox.pointedY / MapDrawer.TILE_SIZE).toInt()})")

            avPosArea.text = "$sb"
            sb.setLength(0) // clear stringbuffer
        }

        if (actorValue != null) {
            for (key in actorValue!!.keySet) {
                val value = actorValue!![key.toString()]

                sb.append("$key = ${
                if (value is String)
                    "\"$value\"" // name = "Sigrid"
                else if (value is Boolean)
                    "_$value" // intelligent = __true
                else
                    "$value" // scale = 1.0
                }\n")
            }

            sb.deleteCharAt(sb.length - 1) // delete trailing \n

            avInfoArea.text = "$sb"
        }
        else {
            avInfoArea.text = ""
        }
    }
}
/*

+--------------------------------+
| Actor: 5333533 (Sigrid)    LBL |
+--------------------------------+
| X: 65532.655654747 (4095)  LBL |
| Y: 3050.4935465 (190)      LBL |
| ...                            |
+--------------------------------+
| < TOOLBOX >                BTN |
+--------------------------------+
| Key    [                     ] |
| Value  [                     ] |
+--------------------------------+







 */