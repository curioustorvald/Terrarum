package net.torvald.reflection

/**
 * Created by minjaesong on 2023-03-25.
 */
fun Any.extortField(name: String): Any? { // yes I'm deliberately using negative words for the function name
    return this.javaClass.getDeclaredField(name).let {
        it.isAccessible = true
        it.get(this)
    }
}
fun Any.forceInvoke(name: String, params: Array<Any>): Any? { // yes I'm deliberately using negative words for the function name
    return this.javaClass.getDeclaredMethod(name, *(params.map { it.javaClass }.toTypedArray())).let {
        it.isAccessible = true
        it.invoke(this, *params)
    }
}