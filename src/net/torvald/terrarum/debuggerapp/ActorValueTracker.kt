package net.torvald.terrarum.debuggerapp

import net.torvald.terrarum.gameactors.ActorValue
import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.SetAV
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorWithPhysics
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * Created by minjaesong on 2016-12-29.
 */
class ActorValueTracker constructor() : JFrame() {

    constructor(actor: Actor?) : this() {
        setTrackingActor(actor)
    }

    private val avInfoArea = JTextArea()
    private val avInfoScroller = JScrollPane(avInfoArea)
    private val avPosArea = JTextArea()
    private val avPosScroller = JScrollPane(avPosArea)

    private var actor: ActorWithPhysics? = null
    private var actorValue: ActorValue? = null

    private val modavInputKey = JTextField()
    private val modavInputValue = JTextField()

    private val buttonAddAV = JButton("Add/Mod")
    private val buttonDelAV = JButton("Delete")

    //private val selectedActorLabel = JLabel("Selected actor: ")
    private val actorIDField = JTextField()
    private val buttonChangeActor = JButton("Change")

    init {
        title = "Actor value tracker"
        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE

        val divPanel = JPanel()
        divPanel.layout = BorderLayout(0, 2)

        avInfoArea.highlighter = null // prevent text-drag-crash
        avPosArea.highlighter = null // prevent text-drag-crash

        avInfoScroller.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        avPosScroller.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER

        if (actor != null) {
            actorIDField.text = "${actor!!.referenceID}"
        }

        // button listener for buttons
        buttonAddAV.addMouseListener(object : MouseAdapter() {
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
        buttonDelAV.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (actorValue != null && modavInputKey.text.isNotBlank()) {
                    actorValue!!.remove(modavInputKey.text)
                    Echo("${SetAV.ccW}Removed ${SetAV.ccM}${modavInputKey.text} ${SetAV.ccW}of ${SetAV.ccY}${actor!!.referenceID}")
                    println("[ActorValueTracker] Removed ActorValue '${modavInputKey.text}' of $actor")
                }
            }
        })
        buttonChangeActor.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (actorIDField.text.toLowerCase() == "player") {
                    actor = TerrarumGDX.ingame!!.player
                    actorValue = actor!!.actorValue
                }
                else if (actorIDField.text.isNotBlank()) {
                    actor = TerrarumGDX.ingame!!.getActorByID(actorIDField.text.toInt()) as ActorWithPhysics
                    actorValue = actor!!.actorValue
                }
            }
        })


        // panel elements
        val actorNameBar = JPanel()
        actorNameBar.layout = BorderLayout(2, 0)
        actorNameBar.add(JLabel("RefID: "), BorderLayout.LINE_START)
        actorNameBar.add(actorIDField, BorderLayout.CENTER)
        actorNameBar.add(buttonChangeActor, BorderLayout.LINE_END)

        val posAndAV = JPanel()
        posAndAV.layout = BorderLayout()
        posAndAV.add(avPosScroller, BorderLayout.PAGE_START)
        posAndAV.add(avInfoScroller, BorderLayout.CENTER)

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
        modpanel.add(JLabel(
                "<html>Messed-up type or careless delete will crash the game.<br>" +
                "Prepend two underscores for boolean literals.</html>"
        ), BorderLayout.PAGE_END)

        toolbox.add(toolpanel, BorderLayout.PAGE_START)
        toolbox.add(modpanel, BorderLayout.CENTER)

        divPanel.add(actorNameBar, BorderLayout.PAGE_START)
        divPanel.add(posAndAV, BorderLayout.CENTER)
        divPanel.add(toolbox, BorderLayout.PAGE_END)


        this.add(divPanel)
        this.setSize(300, 600)
        this.isVisible = true
    }

    fun setTrackingActor(actor: Actor?) {
        this.actorValue = actor?.actorValue

        this.title = "AVTracker — $actor"

        if (actor is ActorWithPhysics) {
            this.actor = actor
        }

        update()
    }

    fun update() {
        val sb = StringBuilder()

        if (actor != null) {
            sb.append("toString: ${actor!!}\n")
            sb.append("X: ${actor!!.hitbox.canonicalX} (${(actor!!.hitbox.canonicalX / FeaturesDrawer.TILE_SIZE).toInt()})\n")
            sb.append("Y: ${actor!!.hitbox.canonicalY} (${(actor!!.hitbox.canonicalY / FeaturesDrawer.TILE_SIZE).toInt()})")

            avPosArea.text = "$sb"
            sb.setLength(0) // clear stringbuffer
        }

        if (actorValue != null) {
            for (key in actorValue!!.keySet) {
                val value = actorValue!![key.toString()]!!
                val type = value.javaClass.simpleName

                sb.append("$key = $value ($type)\n")
            }

            if (sb.isNotEmpty()) {
                sb.deleteCharAt(sb.length - 1) // delete trailing \n
            }

            avInfoArea.text = "$sb"
        }
        else {
            avInfoArea.text = ""
        }
    }
}
/*

+--------------------------------+
| Actor: [5333533     ] [Change] | LBL TFL BTN
+--------------------------------+
| X: 65532.655654747 (4095)      | TAR
| Y: 3050.4935465 (190)          |
| ...                            |
+--------------------------------+
| [   Add/Mod   ] [   Delete   ] | BTN BTN
+--------------------------------+
| Key    [                     ] | LBL TFL
| Value  [                     ] | LBL TFL
+--------------------------------+


 */