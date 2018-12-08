package net.torvald.terrarum

import java.util.ArrayList

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

    fun traversePreorder(action: (QNDTreeNode<T>, Int) -> Unit) {
        this.traverse1(this, action)
    }

    fun traversePostorder(action: (QNDTreeNode<T>, Int) -> Unit) {
        this.traverse2(this, action)
    }
}