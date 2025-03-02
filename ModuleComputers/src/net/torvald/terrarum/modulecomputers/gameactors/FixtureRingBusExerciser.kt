package net.torvald.terrarum.modulecomputers.gameactors

import com.badlogic.gdx.utils.Queue
import net.torvald.random.HQRNG
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.BlockBox
import net.torvald.terrarum.modulebasegame.gameactors.Electric
import net.torvald.terrarum.modulebasegame.gameworld.NetFrame
import net.torvald.terrarum.modulebasegame.gameworld.NetRunner
import net.torvald.terrarum.serialise.Common
import org.dyn4j.geometry.Vector2
import kotlin.math.sign

/**
 * Created by minjaesong on 2025-03-01.
 */
class FixtureRingBusExerciser : Electric {

    @Transient private val rng = HQRNG()

    private val mac = rng.nextInt()

    companion object {
        const val INITIAL_LISTENING_TIMEOUT = 300
        const val SIGNAL_TOO_WEAK_THRESHOLD = 1.0 / 16.0
    }

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        nameFun = { Lang["ITEM_JUKEBOX"] },
        mainUI = UIRingBusExerciser()
    )

    private fun setEmitterAndSink() {
        clearStatus()
        setWireEmitterAt(0, 0, "10base2")
        setWireSinkAt(1, 0, "10base2")
    }

    init {
        setEmitterAndSink()
    }

    override fun reload() {
        super.reload()
        setEmitterAndSink()
    }

    private val msgQueue = Queue<Pair<Int, String>>()
    private val msgLog = Queue<Pair<Int, String>>()

    private var statusAbort = false // will "eat away" any receiving frames unless the frame is a ballot frame
    private var activeMonitorStatus = 0 // 0: unknown, 1: known and not me, 2: known and it's me

    private var lastAccessTime = -1L

    override fun updateSignal() {
        val time_t = INGAME.world.worldTime.TIME_T
        if (lastAccessTime == -1L) lastAccessTime = time_t


        // monitor the input port
        val inn = getWireStateAt(1, 0, "10base2")

        // if a signal is there
        if (inn.x >= SIGNAL_TOO_WEAK_THRESHOLD) {
            lastAccessTime = time_t

            val frameNumber = (inn.y + (0.5 * inn.y.sign)).toInt()

            if (frameNumber != 0) { // frame number must be non-zero
                // if not in abort state, process the incoming frames
                if (!statusAbort) {
                    // fetch frame from the world
                    try {
                        val frame = getFrameByNumber(frameNumber)

                        // fast init (voting) cancellation, if applicable
                        if (activeMonitorStatus == 0 && frame.getFrameType() == "token") {
                            activeMonitorStatus = 1
                        }

                        // if I have a message to send or the frame should be captured, do something with it
                        if (msgQueue.notEmpty() || frame.shouldIintercept(mac)) {
                            // do something with the received frame
                            val newFrame = doSomethingWithFrame(frame) ?: frameNumber
                            setWireEmissionAt(0, 0, Vector2(1.0, newFrame.toDouble()))

                            // if the "do something" processs returns a new frame, mark the old frame as to be destroyed
                            if (newFrame != frameNumber) {
                                frame.discardFrame()
                            }
                        }
                        // else, just pass it along
                        else {
                            setWireEmissionAt(0, 0, Vector2(1.0, frameNumber.toDouble()))
                        }
                    }
                    // frame lost due to poor savegame migration or something: send out ABORT signal
                    catch (e: NullPointerException) {
                        emitNewFrame(NetFrame.makeAbort(mac))
                        statusAbort = true
                    }
                }
                // else, still watch for the new valid token
                else {
                    // fetch frame from the world
                    try {
                        val frame = getFrameByNumber(frameNumber)

                        // not an Active Monitor
                        if (activeMonitorStatus < 2) {
                            if (frame.getFrameType() == "token") {
                                // unlock myself and pass the token
                                statusAbort = false
                                setWireEmissionAt(0, 0, Vector2(1.0, frameNumber.toDouble()))
                            }
                            else if (frame.getFrameType() == "abort") {
                                // lock myself (just in case) and pass the token
                                statusAbort = true
                                setWireEmissionAt(0, 0, Vector2(1.0, frameNumber.toDouble()))
                            }
                            else {
                                // discard anything that is not a token or yet another abort
                                setWireEmissionAt(0, 0, Vector2())
                            }
                        }
                        // am Active Monitor
                        else {
                            if (frame.getFrameType() == "abort") {
                                // send out a new token
                                emitNewFrame(NetFrame.makeToken(mac))
                                statusAbort = false
                            }
                            else {
                                // discard anything that is not an abort
                                setWireEmissionAt(0, 0, Vector2())
                            }
                        }
                    }
                    // frame lost due to poor savegame migration or something: discard token
                    catch (e: NullPointerException) {
                        setWireEmissionAt(0, 0, Vector2())
                    }
                }
            }
            else {
                setWireEmissionAt(0, 0, Vector2())
            }
        }
        // if a signal is not there
        else {
            setWireEmissionAt(0, 0, Vector2())

            // if no-signal for 5 in-game minutes (around 5 real-life seconds)
            if (time_t - lastAccessTime > INITIAL_LISTENING_TIMEOUT) {
                // initialise the voting process
                activeMonitorStatus = 0
                emitNewFrame(NetFrame.makeBallot(mac))
                lastAccessTime = time_t
            }
        }
    }

    protected fun doSomethingWithFrame(incomingFrame: NetFrame): Int? {
        return when (incomingFrame.getFrameType()) {
            "token" -> doSomethingWithToken(incomingFrame)
            "data" -> doSomethingWithData(incomingFrame)
            "ack" -> doSomethingWithAck(incomingFrame)
            "ballot" -> doSomethingWithBallot(incomingFrame)
            "abort" -> 0
            else -> null /* returns the frame untouched */
        }
    }

    private fun getFrameByNumber(number: Int) = (INGAME.world.extraFields["tokenring"] as NetRunner)[number]

    private fun emitNewFrame(frame: NetFrame): Int {
        return (INGAME.world.extraFields["tokenring"] as NetRunner).addFrame(frame).also {
            setWireEmissionAt(0, 0, Vector2(1.0, it.toDouble()))
        }
    }

    protected fun doSomethingWithToken(incomingFrame: NetFrame): Int? {
        if (msgQueue.isEmpty) return null

        val (recipient, msgStr) = msgQueue.removeFirst()
        val msgByte = msgStr.toByteArray(Common.CHARSET)

        return emitNewFrame(NetFrame.makeData(mac, recipient, msgByte))
    }

    protected fun doSomethingWithData(incomingFrame: NetFrame): Int? {
        val rec = incomingFrame.getDataRecipient()
        // if the message is for me, put incoming message into queue, then send out ack
        if (rec == mac) {
            val str = incomingFrame.getDataContents()?.toString(Common.CHARSET)
            msgLog.addLast(rec to (str ?: "(null)"))

            // make ack
            return emitNewFrame(NetFrame.makeAck(mac, incomingFrame.getSender()))
        }
        else return null
    }

    protected fun doSomethingWithAck(incomingFrame: NetFrame): Int? {
        if (msgQueue.isEmpty) return null

        val topMsg = msgQueue.first()

        // if the ACK is sent to me...
        if (incomingFrame.getDataRecipient() == mac && incomingFrame.getSender() == topMsg.first) {

            // ack or nak?
            val successful = (incomingFrame.getAckStatus() == 0)

            // if successful, remove the message from the queue, then send out empty token
            // if failed, keep the message, then send out empty token anyway
            if (successful) {
                msgQueue.removeFirst()
            }

            // make an empty token
            return emitNewFrame(NetFrame.makeToken(mac))
        }
        else return null
    }

    protected fun doSomethingWithBallot(incomingFrame: NetFrame): Int? {
        val ballotStatus = incomingFrame.getFrameNumber()

        // frame is in election phase
        if (ballotStatus == 0) {
            // if i'm also in the election phase, participate
            if (activeMonitorStatus == 0) {
                if (incomingFrame.getBallot() < mac) {
                    incomingFrame.setBallot(mac)
                }

                // check if the election must be finished
                if (incomingFrame.getSender() == mac && incomingFrame.getBallot() == mac) {
                    activeMonitorStatus = 2

                    // send out first empty token
                    return emitNewFrame(NetFrame.makeToken(mac))
                }
            }
            // if i'm in the winner announcement phase, kill the frame
            else {
                incomingFrame.discardFrame()
                return 0
            }
        }
        // frame is in winner announcement phase
        else if (ballotStatus == 1) {
            activeMonitorStatus = 1
        }

        return null
    }
}