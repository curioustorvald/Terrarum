package net.torvald.terrarum

import java.util.*

/**
 * Simplified version of YAML, only for the representation of a text tree.
 *
 * Example code:
 * ```
 * - File
 *  - New : Ctrl-N
 *  - Open : Ctrl-O
 *  - Open Recent
 *   - yaml_example.yaml
 *   - Yaml.kt
 *  - Close : Ctrl-W
 *  - Settings
 *  - Line Separators
 *   - CRLF
 *   - CR
 *   - LF
 * - Edit
 *  - Undo : Ctrl-Z
 *  - Redo : Shift-Ctrl-Z
 *  - Cut : Ctrl-X
 *  - Copy : Ctrl-C
 *  - Paste : Ctrl-V
 *  - Find
 *   - Find : Ctrl-F
 *   - Replace : Shift-Ctrl-F
 *  - Convert Indents
 *   - To Spaces
 *    - Set Project Indentation
 *   - To Tabs
 * - Refactor
 *  - Refactor This
 *  - Rename : Shift-Ctrl-R
 *  - Extract
 *   - Variable
 *   - Property
 *   - Function
 * ```
 *
 * - All lines are indented with one space
 * - All entries are preceded by '- ' (dash and a space)
 * - All propery are separated by ' : ' (space colon space)
 * - A line that does not start with '- ' are simply ignored, so you can freely make empty lines and/or comments.
 *
 * Any deviation to the above rule will cause a parse failure, because it's simple and dumb as that.
 *
 * Created by minjaesong on 2018-12-08.
 */
inline class Yaml(val text: String) {

    companion object {
        val SEPARATOR = Regex(" : ")
    }

    fun parse(): QNDTreeNode<String> {
        var currentIndentLevel = -1
        val root = QNDTreeNode<String>()
        var currentNode = root
        val nodesStack = Stack<QNDTreeNode<String>>()
        val validLineStartRe = Regex(""" *\- """)

        nodesStack.push(currentNode)

        text.split('\n') .forEach {
            if (validLineStartRe.containsMatchIn(it)) { // take partial match; do the task if the text's line is valid
                val indentLevel = it.countSpaces()
                val it = it.trimIndent()
                if (it.startsWith("- ")) { // just double check if indent-trimmed line looks valid
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
        }


        // test traverse resulting tree
        /*root.traversePreorder { node, depth ->
            repeat(depth + 1) { print("-") }
            println("${node.data} -> ${node.parent}")
        }*/


        return root
    }

    fun parseAsYamlInvokable(): QNDTreeNode<Pair<String, YamlInvokable?>> {
        var currentIndentLevel = -1
        val root = QNDTreeNode<Pair<String, YamlInvokable?>>()
        var currentNode = root
        val nodesStack = Stack<QNDTreeNode<Pair<String, YamlInvokable?>>>()
        val validLineStartRe = Regex(""" *\- """)

        nodesStack.push(currentNode)

        text.split('\n') .forEach {
            if (validLineStartRe.containsMatchIn(it)) { // take partial match; do the task if the text's line is valid
                val indentLevel = it.countSpaces()
                val it = it.trimIndent()
                if (it.startsWith("- ")) { // just double check if indent-trimmed line looks valid
                    val nodeString = it.drop(2)
                    val nodeNameAndInvocation = nodeString.split(SEPARATOR)
                    val nodeName = nodeNameAndInvocation[0]
                    val nodeInvocation = if (nodeNameAndInvocation.size == 2)
                        loadClass(nodeNameAndInvocation[1])
                    else
                        null

                    val nameInvokePair = nodeName to nodeInvocation

                    if (indentLevel == currentIndentLevel) {
                        val sibling = QNDTreeNode(nameInvokePair, currentNode.parent)
                        currentNode.parent!!.children.add(sibling)
                        currentNode = sibling
                    }
                    else if (indentLevel > currentIndentLevel) {
                        val childNode = QNDTreeNode(nameInvokePair, currentNode)
                        currentNode.children.add(childNode)
                        nodesStack.push(currentNode)
                        currentNode = childNode
                        currentIndentLevel = indentLevel
                    }
                    else {
                        repeat(currentIndentLevel - indentLevel) { currentNode = nodesStack.pop() }
                        currentIndentLevel = indentLevel
                        val sibling = QNDTreeNode(nameInvokePair, currentNode.parent)
                        currentNode.parent!!.children.add(sibling)
                        currentNode = sibling
                    }
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

    private fun loadClass(name: String): YamlInvokable {
        val newClass = Class.forName(name)
        val newClassConstructor = newClass.getConstructor(/* no args defined */)
        val newClassInstance = newClassConstructor.newInstance(/* no args defined */)
        return newClassInstance as YamlInvokable
    }

}

/**
 * A simple interface that meant to be attached with Yaml tree, so that the entry can be ```invoke()```d.
 *
 * Example usage in Yaml:
 * ```
 * - File
 *  - Import : net.torvald.terrarum.whatever.package.ImportFile
 * ```
 *
 */
interface YamlInvokable {
    operator fun invoke(args: Array<Any>)
}