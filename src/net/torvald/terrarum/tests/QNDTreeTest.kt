package net.torvald.terrarum.tests

import net.torvald.terrarum.Yaml
import net.torvald.terrarum.modulebasegame.ui.UITitleRemoConYaml

/**
 * Created by minjaesong on 2018-12-08.
 */
class QNDTreeTest {

    val treeStr = """
- File
 - New : Ctrl-N
 - Open : Ctrl-O
 - Open Recent
  - yaml_example.yaml
  - Yaml.kt
 - Close : Ctrl-W
 - Settings
 - Line Separators
  - CRLF
  - CR
  - LF
- Edit
 - Undo : Ctrl-Z
 - Redo : Shift-Ctrl-Z
 - Cut : Ctrl-X
 - Copy : Ctrl-C
 - Paste : Ctrl-V
 - Find
  - Find : Ctrl-F
  - Replace : Shift-Ctrl-F
 - Convert Indents
  - To Spaces
   - Set Project Indentation
  - To Tabs
- Refactor
 - Refactor This
 - Rename : Shift-Ctrl-R
 - Extract
  - Variable
  - Property
  - Function
"""

    operator fun invoke() {
        val treeYaml = Yaml(treeStr)
        val tree = treeYaml.parse()

        println("\nTest traversePreorder()\n")
        tree.traversePreorder { qndTreeNode, i ->
            print("-".repeat(i))
            print(" ")
            println("$qndTreeNode <- ${qndTreeNode.parent}")
        }

        println("\nTest traversePostOrder()\n")
        tree.traversePostorder { qndTreeNode, i ->
            print("-".repeat(i))
            print(" ")
            println("$qndTreeNode <- ${qndTreeNode.parent}")
        }

        println("\nTest traverseLevelOrder()\n")
        tree.traverseLevelorder { qndTreeNode, i ->
            print("-".repeat(i))
            print(" ")
            println("$qndTreeNode <- ${qndTreeNode.parent}")
        }
        println("\nLevel 1 nodes:\n")
        println(tree.getLevelData(1))
    }

}

fun main(args: Array<String>) {
    QNDTreeTest().invoke()
}