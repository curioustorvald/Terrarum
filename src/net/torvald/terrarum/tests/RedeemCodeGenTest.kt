import net.torvald.terrarum.modulebasegame.redeemable.RedeemCodeMachine

fun main() {
    val code = RedeemCodeMachine.encode(
        "item@basegame:65511",
        6,
        true
    )

    println(code)

    val voucher = RedeemCodeMachine.decode(code)

    println(voucher)
}