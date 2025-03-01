package net.torvald.terrarum.modulecomputers.gameactors

import com.badlogic.gdx.utils.Queue
import net.torvald.random.HQRNG
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.BlockBox
import net.torvald.terrarum.modulebasegame.gameactors.Electric
import net.torvald.terrarum.modulebasegame.gameworld.IngameNetPacket
import net.torvald.terrarum.modulebasegame.gameworld.PacketRunner
import net.torvald.terrarum.serialise.Common
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2025-03-01.
 */
class FixtureRingBusExerciser : Electric {

    @Transient private val rng = HQRNG()

    private val mac = rng.nextInt()

    companion object {
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


    private var statusAbort = false // will "eat away" any receiving packets unless the packet is a ballot packet
    private var activeMonitorStatus = 0 // 0: unknown, 1: known and not me, 2: known and it's me

    override fun updateSignal() {
        // monitor the input port
        val inn = getWireStateAt(1, 0, "10base2")

        // if a signal is there
        if (inn.x >= SIGNAL_TOO_WEAK_THRESHOLD) {
            val packetNumber = (inn.y + 0.5).toInt()

            if (packetNumber != 0) { // packet number must be non-zero
                // if not in abort state, process the incoming packets
                if (!statusAbort) {
                    // fetch packet from the world
                    try {
                        val packet = getPacketByNumber(packetNumber)

                        if (msgQueue.notEmpty() || packet.shouldIintercept(mac)) {
                            val newPacket = doSomethingWithPacket(packet) ?: packetNumber
                            setWireEmissionAt(0, 0, Vector2(1.0, newPacket.toDouble()))

                            // mark the old packet as to be destroyed
                            if (newPacket != packetNumber) {
                                packet.discardPacket()
                            }
                        }
                        else {
                            setWireEmissionAt(0, 0, Vector2(1.0, packetNumber.toDouble()))
                        }
                    }
                    // packet lost due to poor savegame migration or something: send out ABORT signal
                    catch (e: NullPointerException) {
                        val abortPacket = IngameNetPacket.makeAbort(mac)
                        emitNewPacket(abortPacket)
                        statusAbort = true
                    }
                }
                // else, still watch for the new valid token
                else {
                    // fetch packet from the world
                    try {
                        val packet = getPacketByNumber(packetNumber)

                        // not an Active Monitor
                        if (activeMonitorStatus < 2) {
                            if (packet.getFrameType() == "token") {
                                // unlock myself and pass the token
                                statusAbort = false
                                setWireEmissionAt(0, 0, Vector2(1.0, packetNumber.toDouble()))
                            }
                            else if (packet.getFrameType() == "abort") {
                                // unlock myself (just in case) and pass the token
                                statusAbort = true
                                setWireEmissionAt(0, 0, Vector2(1.0, packetNumber.toDouble()))
                            }
                            else {
                                // discard anything that is not a token
                                setWireEmissionAt(0, 0, Vector2())
                            }
                        }
                        // am Active Monitor
                        else {
                            if (packet.getFrameType() == "abort") {
                                // send out a new token
                                emitNewPacket(IngameNetPacket.makeToken(mac))
                                statusAbort = false
                            }
                            else {
                                // discard anything that is not a token
                                setWireEmissionAt(0, 0, Vector2())
                            }
                        }
                    }
                    // packet lost due to poor savegame migration or something: discard token
                    catch (e: NullPointerException) {
                        setWireEmissionAt(0, 0, Vector2())
                    }
                }
            }
        }
        // if a signal is not there
        else {
            setWireEmissionAt(0, 0, Vector2())
        }
    }

    protected fun doSomethingWithPacket(incomingPacket: IngameNetPacket): Int? {
        return when (incomingPacket.getFrameType()) {
            "token" -> doSomethingWithToken(incomingPacket)
            "data" -> doSomethingWithData(incomingPacket)
            "ack" -> doSomethingWithAck(incomingPacket)
            "ballot" -> doSomethingWithBallot(incomingPacket)
            "abort" -> 0
            else -> null /* returns the packet untouched */
        }
    }

    private fun getPacketByNumber(number: Int) = (INGAME.world.extraFields["tokenring"] as PacketRunner)[number]

    private fun emitNewPacket(packet: IngameNetPacket): Int {
        return (INGAME.world.extraFields["tokenring"] as PacketRunner).addPacket(packet).also {
            setWireEmissionAt(0, 0, Vector2(1.0, it.toDouble()))
        }
    }

    protected fun doSomethingWithToken(incomingPacket: IngameNetPacket): Int? {
        if (msgQueue.isEmpty) return null

        val (recipient, msgStr) = msgQueue.removeFirst()
        val msgByte = msgStr.toByteArray(Common.CHARSET)

        val newPacket = IngameNetPacket.makeData(mac, recipient, msgByte)
        return emitNewPacket(newPacket)
    }
}