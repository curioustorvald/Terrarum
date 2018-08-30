package net.torvald.terrarum.modulebasegame.ui

import java.util.*



object UITitleRemoConYaml {

    // YAML indent with a space, separate label and class with " : " verbatim!

    val menus = """
        - MENU_MODE_SINGLEPLAYER
         - MENU_LABEL_RETURN
        - MENU_MODE_MULTIPLAYER
         - MENU_LABEL_RETURN
        - MENU_OPTIONS
         - MENU_OPTIONS_GRAPHICS
         - MENU_OPTIONS_CONTROLS
         - MENU_OPTIONS_SOUND
         - MENU_LABEL_RETURN
        - MENU_MODULES : net.torvald.terrarum.modulebasegame.ui.UITitleModules
         - MENU_LABEL_RETURN
        - MENU_LABEL_LANGUAGE
         - MENU_LABEL_RETURN
        - MENU_LABEL_CREDITS : net.torvald.terrarum.modulebasegame.ui.UITitleCredits
         - MENU_LABEL_CREDITS : net.torvald.terrarum.modulebasegame.ui.UITitleCredits
         - MENU_CREDIT_GPL_DNT : net.torvald.terrarum.modulebasegame.ui.UITitleGPL3
         - MENU_LABEL_RETURN
        - MENU_LABEL_QUIT
        """.trimIndent()

    val debugTools = """
        -  Development Tools $
        - Building Maker
        """.trimIndent()

    operator fun invoke() = parseYamlList(menus)

    fun parseYamlList(yaml: String): QNDTreeNode<String> {
        var currentIndentLevel = -1
        val root = QNDTreeNode<String>()
        var currentNode = root
        val nodesStack = Stack<QNDTreeNode<String>>()

        nodesStack.push(currentNode)

        yaml.split('\n') .forEach {
            val indentLevel = it.countSpaces()
            val it = it.trimIndent()
            if (it.startsWith("- ")) {
                val nodeName = it.drop(2)

                if (indentLevel == currentIndentLevel) {
                    val sibling = QNDTreeNode(nodeName, currentNode.parent)
                    currentNode.parent!!.children.add(sibling)
                    currentNode = sibling
                }
                else if (indentLevel > currentIndentLevel) {
                    val childNode = QNDTreeNode(nodeName, currentNode)
                    currentNode.children.add(childNode)
                    nodesStack.push(currentNode)
                    currentNode = childNode
                    currentIndentLevel = indentLevel
                }
                else {
                    repeat(currentIndentLevel - indentLevel) { currentNode = nodesStack.pop() }
                    currentIndentLevel = indentLevel
                    val sibling = QNDTreeNode(nodeName, currentNode.parent)
                    currentNode.parent!!.children.add(sibling)
                    currentNode = sibling
                }
            }
        }


        // test traverse resulting tree
        /*root.traversePreorder { node, depth ->
            repeat(depth + 1) { print("-") }
            println("${node.data} -> ${node.parent}")
        }*/


        return root
    }

    private fun String.countSpaces(): Int {
        var c = 0
        while (c <= this.length) {
            if (this[c] == ' ')
                c++
            else
                break
        }

        return c
    }
}

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