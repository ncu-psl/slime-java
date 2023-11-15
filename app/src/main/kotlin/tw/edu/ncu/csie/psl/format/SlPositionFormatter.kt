package tw.edu.ncu.csie.psl.format

import com.github.javaparser.ast.Node
import tw.edu.ncu.csie.psl.logger.SlLogger
import java.nio.file.Path
import kotlin.jvm.optionals.getOrNull

object SlPositionFormatter {
    private val mLogger = SlLogger(SlPositionFormatter::class.java.simpleName)

    fun format(node: Node, path: Path): String {
        val begin = node.begin.getOrNull() ?: run {
            mLogger.warn("Node $node has no begin position (${path.fileName})")
            return ""
        }
        return "${path.fileName}:L${begin.line}:C${begin.column}"
    }
}