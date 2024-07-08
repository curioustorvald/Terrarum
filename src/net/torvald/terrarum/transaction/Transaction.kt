package net.torvald.terrarum.transaction

import net.torvald.terrarum.App.printdbg
import java.util.concurrent.atomic.AtomicReference

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
    val transactionLockingClass: AtomicReference<Transaction?> = AtomicReference(null)
    val transactionLocked: Boolean; get() = (transactionLockingClass.get() != null)

    private fun acquireLock(locker: Transaction) {
        synchronized(this) {
            transactionLockingClass.set(locker)
        }
    }

    private fun releaseLock() {
        synchronized(this) {
            transactionLockingClass.set(null)
        }
    }

    /**
     * Transaction modifies a given state to a new state, then applies the new state to the object.
     * The given `transaction` may only modify values which is passed to it.
     *
     * Transaction is fully unlocked and the previous locker is unknowable by the time `onFinally` executes.
     * Note that `onFinally` runs on the same thread the actual transaction has run (GL context not available).
     */
    fun runTransaction(transaction: Transaction, onFinally: () -> Unit = {}) {
        printdbg(this, "Accepting transaction $transaction")
        Thread {
            val state = getCurrentStatusForTransaction()
            var wasSuccessful = false
            val currentLock = transactionLockingClass.get()
            if (currentLock == null) {
                acquireLock(transaction)
                try {
                    transaction.start(state)
                    // if successful:
                    commitTransaction(state)

                    wasSuccessful = true
                }
                catch (e: Throwable) {
                    // if failed, notify the failure
                    System.err.println("Transaction failure: generic (failed transaction: $transaction)")
                    e.printStackTrace()
                    transaction.onFailure(e, state)
                }
                finally {
                    if (wasSuccessful) {
                        // notify the success
                        transaction.onSuccess(state)
                    }
                    releaseLock()
                    onFinally()
                }
            }
            else {
                System.err.println("Transaction failure: locked (failed transaction: $transaction)")
                transaction.onFailure(LockedException(this, currentLock), state)
            }
        }.start()
    }

    protected abstract fun getCurrentStatusForTransaction(): TransactionState
    protected abstract fun commitTransaction(state: TransactionState)
}

class LockedException(listener: TransactionListener, lockedBy: Transaction) :
    Exception("The class '$listener' is locked by '$lockedBy'")

@JvmInline value class TransactionState(val valueTable: HashMap<String, Any?>) {
    operator fun get(key: String) = valueTable[key]
    operator fun set(key: String, value: Any?) {
        valueTable[key] = value
    }
}