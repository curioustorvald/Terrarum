import net.torvald.terrarum.ModMgr

/**
 * Created by SKYHi14 on 2017-04-26.
 */

static void invoke(String module) {
    ModMgr.GameBlockLoader.invoke(module)
    ModMgr.GameItemLoader.invoke(module)
}
