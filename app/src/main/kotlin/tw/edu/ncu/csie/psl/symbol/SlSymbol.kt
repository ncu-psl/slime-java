package tw.edu.ncu.csie.psl.symbol

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import tw.edu.ncu.csie.psl.error.SlError
import tw.edu.ncu.csie.psl.format.SlPositionFormatter
import java.nio.file.Path

abstract class SlSymbol(val name: String, val node: Node, val path: Path) : Comparable<SlSymbol> {
    abstract fun accept(cu: CompilationUnit, symbolTable: HashSet<SlSymbol>): List<SlError>

    override fun compareTo(other: SlSymbol) = name.compareTo(other.name)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        val symbol = other as? SlSymbol ?: return false
        return name == symbol.name && node == symbol.node && node.parentNode == symbol.node.parentNode
    }

    override fun hashCode(): Int {
        val nameHashCode = name.hashCode()
        return 31 * nameHashCode + node.hashCode() + node.parentNode.hashCode()
    }

    override fun toString() = "${node::class.java.simpleName}: $name (${SlPositionFormatter.format(node, path)})"
}
