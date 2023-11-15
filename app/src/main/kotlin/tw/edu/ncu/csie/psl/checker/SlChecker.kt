package tw.edu.ncu.csie.psl.checker

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.MarkerAnnotationExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import tw.edu.ncu.csie.psl.error.SlError
import tw.edu.ncu.csie.psl.error.SlErrorPhase
import tw.edu.ncu.csie.psl.format.SlPositionFormatter
import tw.edu.ncu.csie.psl.logger.SlLogger
import tw.edu.ncu.csie.psl.symbol.SlSymbol
import tw.edu.ncu.csie.psl.symbol.annotation.SlBorrowAnnoSymbol
import tw.edu.ncu.csie.psl.symbol.annotation.SlCopyAnnoSymbol
import tw.edu.ncu.csie.psl.symbol.annotation.SlOwnedAnnoSymbol
import tw.edu.ncu.csie.psl.type.SlAnnoType
import java.nio.file.Path
import kotlin.jvm.optionals.getOrNull

object SlChecker {
    private val mLogger = SlLogger(SlChecker::class.java.simpleName)
    private val mErrorList = mutableListOf<SlError>()

    fun run(cuMap: Map<Path, CompilationUnit>, symbolTable: HashSet<SlSymbol>): List<SlError> {
        cuMap.forEach { (path, cu) ->
            mLogger.info("Checking $path")
            cu.findAll(AnnotationExpr::class.java).forEach { preCheck(path, it, symbolTable) }
        }
        mErrorList.ifEmpty { check(cuMap.values.toList(), symbolTable) }
        return mErrorList.toList()
    }

    private fun preCheck(path: Path, annoExpr: AnnotationExpr, symbolTable: HashSet<SlSymbol>) {
        mLogger.trace("Annotation expression: $annoExpr")
        // Check if the annotation is a slime annotation
        val type = SlAnnoType.fromValue(annoExpr.nameAsString) ?: return
        val loc = lazy { SlPositionFormatter.format(annoExpr, path) }
        // Check if the annotation has a parent node
        val parentNode = annoExpr.parentNode.getOrNull() ?: run {
            appendError(SlErrorPhase.ANNOTATION_CHECK, "Annotation $annoExpr has no parent node", loc.value)
            return
        }
        // Check if the annotation is a marker annotation
        if (annoExpr !is MarkerAnnotationExpr) {
            appendError(SlErrorPhase.ANNOTATION_CHECK, "Annotation $annoExpr is not a marker annotation", loc.value)
            return
        }
        // Check if the annotation is a valid declaration
        val parentName = parentNode::class.java.simpleName
        val symbol = extractSymbolFromNode(parentNode, type, path) ?: run {
            appendError(
                SlErrorPhase.ANNOTATION_CHECK, "Annotation $annoExpr in `$parentName` is not permitted", loc.value
            )
            return
        }
        mLogger.debug("Annotation found: $annoExpr with node $parentName `${symbol.name}`")
        // Check if the annotation is duplicated
        if (symbolTable.add(symbol)) {
            mLogger.debug("$annoExpr `${symbol.name}` is added to symbol table")
        } else {
            appendError(
                SlErrorPhase.ANNOTATION_CHECK,
                "Annotation $annoExpr is duplicated with `${symbol.name}`",
                loc.value
            )
            return
        }
    }

    private fun check(cuList: List<CompilationUnit>, symbolTable: HashSet<SlSymbol>) {
        symbolTable.forEach { symbol ->
            mLogger.info("Checking symbol `$symbol`")
            cuList.forEach { mErrorList += symbol.accept(it, symbolTable) }
        }
    }

    private fun appendError(phase: SlErrorPhase, msg: String, loc: String) {
        val err = SlError(phase, "$msg ($loc)")
        mLogger.error("$err")
        mErrorList += err
    }

    private fun extractSymbolFromNode(node: Node, type: SlAnnoType, path: Path): SlSymbol? {
        when (type) {
            SlAnnoType.SLIME_OWNED -> {
                if (node is FieldDeclaration) {
                    return SlOwnedAnnoSymbol(node, path)
                } else if (node is MethodDeclaration) {
                    return SlOwnedAnnoSymbol(node, path)
                }
            }

            SlAnnoType.SLIME_BORROW -> {
                if (node is Parameter) {
                    return SlBorrowAnnoSymbol(node, path)
                } else if (node is VariableDeclarationExpr) {
                    return SlBorrowAnnoSymbol(node, path)
                }
            }

            SlAnnoType.SLIME_COPY -> {
                if (node is VariableDeclarationExpr) {
                    return SlCopyAnnoSymbol(node, path)
                }
            }
        }
        return null
    }
}