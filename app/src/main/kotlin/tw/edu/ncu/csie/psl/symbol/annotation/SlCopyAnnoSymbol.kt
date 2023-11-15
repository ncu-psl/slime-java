package tw.edu.ncu.csie.psl.symbol.annotation

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import tw.edu.ncu.csie.psl.error.SlError
import tw.edu.ncu.csie.psl.symbol.SlSymbol
import tw.edu.ncu.csie.psl.type.SlAnnoType
import java.nio.file.Path

class SlCopyAnnoSymbol(varNode: VariableDeclarationExpr, path: Path) : SlAnnoSymbol(
    varNode.variables.first().nameAsString, varNode, path, SlAnnoType.SLIME_COPY
) {
    companion object {
        const val COPY_METHOD_NAME = "deepCopy"
    }

    override fun accept(cu: CompilationUnit, symbolTable: HashSet<SlSymbol>): List<SlError> {
        return emptyList()
    }
}