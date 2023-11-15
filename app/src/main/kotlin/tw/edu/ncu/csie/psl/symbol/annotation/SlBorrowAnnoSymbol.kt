package tw.edu.ncu.csie.psl.symbol.annotation

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import tw.edu.ncu.csie.psl.error.SlError
import tw.edu.ncu.csie.psl.symbol.SlSymbol
import tw.edu.ncu.csie.psl.type.SlAnnoType
import java.nio.file.Path
import java.util.*

class SlBorrowAnnoSymbol : SlAnnoSymbol {
    private val mObjectCreationList = mutableListOf<ObjectCreationExpr>()
    private val mMethodCallList = mutableListOf<MethodCallExpr>()
    val borrowedStack = Stack<Node>()

    constructor(paraNode: Parameter, path: Path) : super(
        paraNode.nameAsString, paraNode, path, SlAnnoType.SLIME_BORROW
    )

    constructor(varNode: VariableDeclarationExpr, path: Path) : super(
        varNode.variables.first().nameAsString, varNode, path, SlAnnoType.SLIME_BORROW
    )

    override fun accept(cu: CompilationUnit, symbolTable: HashSet<SlSymbol>): List<SlError> {
        return emptyList()
    }
}