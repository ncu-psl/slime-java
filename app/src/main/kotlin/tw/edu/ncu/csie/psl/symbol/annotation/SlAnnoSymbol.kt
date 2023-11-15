package tw.edu.ncu.csie.psl.symbol.annotation

import com.github.javaparser.ast.Node
import tw.edu.ncu.csie.psl.format.SlPositionFormatter
import tw.edu.ncu.csie.psl.symbol.SlSymbol
import tw.edu.ncu.csie.psl.type.SlAnnoType
import java.nio.file.Path

abstract class SlAnnoSymbol(name: String, node: Node, path: Path, private val annoType: SlAnnoType) :
    SlSymbol(name, node, path) {
    override fun compareTo(other: SlSymbol): Int {
        val parentResult = super.compareTo(other)
        val otherAnnoSymbol = other as? SlAnnoSymbol ?: return parentResult
        val result = annoType.compareTo(otherAnnoSymbol.annoType)
        return if (result == 0) parentResult else result
    }

    override fun toString() =
        "${node::class.java.simpleName}($annoType): $name (${SlPositionFormatter.format(node, path)})"
}