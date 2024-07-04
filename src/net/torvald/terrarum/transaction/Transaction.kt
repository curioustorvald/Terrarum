package net.torvald.terrarum.transaction

/**
 * Created by minjaesong on 2024-06-28.
 */
interface Transaction {

    /**
     * Call this function to begin the transaction.
     *
     * When started using [TransactionListener.runTransaction], the transaction runs on a separate thread,
     * and thus any operation that requires GL Context will fail.
     */
    fun start(state: TransactionState)

    /**
     * Called by [TransactionListener.runTransaction], when the transaction was successful.
     */
    fun onSuccess(state: TransactionState)

    /**
     * Called by [TransactionListener.runTransaction], when the transaction failed.
     */
    fun onFailure(e: Throwable, state: TransactionState)

}

abstract class TransactionListener {

    /** `null` if not locked, a class that acquired the lock if locked */
    var transactionLockedBy: Any? = null; private set
    val transactionLocked: Boolean; get() = (transactionLockedBy != null)


    /**
     * Transaction modifies a given state to a new state, then applies the new state to the object.
     * The given `transaction` may only modify values which is passed to it.
     */
    fun runTransaction(transaction: Transaction, onFinally: () -> Unit = {}) {
        Thread { synchronized(this) {
            val state = getCurrentStatusForTransaction()
            if (!transactionLocked) {
                try {
                    transaction.start(state)
                    // if successful:
                    commitTransaction(state)
                    // notify the success
                    transaction.onSuccess(state)
                }
                catch (e: Throwable) {
                    // if failed, notify the failure
                    transaction.onFailure(e, state)
                }
                finally {
                    onFinally()
                }
            }
            else {
                transaction.onFailure(LockedException(this, transactionLockedBy), state)
            }
        } }.start()
    }

    protected abstract fun getCurrentStatusForTransaction(): TransactionState
    protected abstract fun commitTransaction(state: TransactionState)
}

class LockedException(listener: TransactionListener, lockedBy: Any?) :
    Exception("Transaction is rejected because the class '${listener.javaClass.canonicalName}' is locked by '${lockedBy?.javaClass?.canonicalName}'")

@JvmInline value class TransactionState(val valueTable: MutableMap<String, Any?>) {
    operator fun get(key: String) = valueTable[key]
    operator fun set(key: String, value: Any?) {
        valueTable[key] = value
    }
}