import net.torvald.terrarum.modulebasegame.redeemable.RedeemCodeMachine
import java.util.*

fun main() {
    val uuid = UUID.randomUUID()

    val code = RedeemCodeMachine.encode(
        "item@basegame:65511",
        6,
        true,
        null
    )

    println(code)

    val voucher = RedeemCodeMachine.decode(code, uuid)

    println(voucher)
}