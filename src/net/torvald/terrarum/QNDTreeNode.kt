package net.torvald.terrarum

import java.util.*
import kotlin.collections.ArrayList

class QNDTreeNode<T>(var data: T? = null, var parent: QNDTreeNode<T>? = null) {
    var children = ArrayList<QNDTreeNode<T>>()


    private fun traverse1(node: QNDTreeNode<T>, action: (QNDTreeNode<T>, Int) -> Unit, depth: Int = 0) {
        //if (node == null) return
        action(node, depth)
        node.children.forEach { traverse1(it, action, depth + 1) }
    }

    private fun traverse2(node: QNDTreeNode<T>, action: (QNDTreeNode<T>, Int) -> Unit, depth: Int = 0) {
        //if (node == null) return
        node.children.forEach { traverse2(it, action, depth + 1) }
        action(node, depth)
    }

    /**
     * (QNDTreeNode, Int) is Node-depth pair, starting from zero.
     */
    fun traversePreorder(action: (QNDTreeNode<T>, Int) -> Unit) {
        this.traverse1(this, action)
    }

    /**
     * (QNDTreeNode, Int) is Node-depth pair, starting from zero.
     */
    fun traversePostorder(action: (QNDTreeNode<T>, Int) -> Unit) {
        this.traverse2(this, action)
    }

    /**
     * (QNDTreeNode, Int) is Node-depth pair, starting from zero.
     */
    fun traverseLevelorder(action: (QNDTreeNode<T>, Int) -> Unit) {
        val q = ArrayList<Pair<QNDTreeNode<T>, Int>>() // node, depth
        q.add(this to 0)
        while (q.isNotEmpty()) {
            val node = q.removeAt(0)
            action(node.first, node.second)
            node.first.children.forEach {
                q.add(it to node.second + 1)
            }
        }
    }

    /**
     * Retrieves data in the node in a specific depth (level).
     * Probably only useful for level = 1
     */
    fun getLevelData(level: Int): List<T?> {
        val list = ArrayList<T?>()

        traversePreorder { node, i ->
            if (i == level) {
                list.add(node.data)
            }
        }

        return list
    }

    override fun toString() = data.toString()

    fun print() {
        TODO()
    }
}