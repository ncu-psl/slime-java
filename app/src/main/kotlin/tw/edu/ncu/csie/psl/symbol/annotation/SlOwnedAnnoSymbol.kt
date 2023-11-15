package tw.edu.ncu.csie.psl.symbol.annotation

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.ReturnStmt
import tw.edu.ncu.csie.psl.error.SlError
import tw.edu.ncu.csie.psl.error.SlErrorPhase
import tw.edu.ncu.csie.psl.format.SlPositionFormatter
import tw.edu.ncu.csie.psl.symbol.SlSymbol
import tw.edu.ncu.csie.psl.type.SlAnnoType
import java.nio.file.Path
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass

class SlOwnedAnnoSymbol : SlAnnoSymbol {
    private val mAssignNodeList = mutableListOf<AssignExpr>()
    private val mVarNodeList = mutableListOf<VariableDeclarator>()
    private val mReturnNodeList = mutableListOf<ReturnStmt>()

    constructor(fieldNode: FieldDeclaration, path: Path) : super(
        fieldNode.variables.first().nameAsString, fieldNode, path, SlAnnoType.SLIME_OWNED
    ) {
        val classNode = fieldNode.parentNode.getOrNull() as? ClassOrInterfaceDeclaration ?: return
        mAssignNodeList.addAll(classNode.findAll(AssignExpr::class.java).filter { assignNode ->
            assignNode.findAll(SimpleName::class.java).any { "$it" == name }
        })
        mVarNodeList.addAll(classNode.findAll(VariableDeclarator::class.java).filter { varNode ->
            varNode.findAll(SimpleName::class.java).any { "$it" == name } && !varNode.isDescendantOf(node)
        })
        mReturnNodeList.addAll(classNode.findAll(ReturnStmt::class.java).filter { returnNode ->
            returnNode.findAll(SimpleName::class.java).any { "$it" == name }
        })
    }

    constructor(methodNode: MethodDeclaration, path: Path) : super(
        methodNode.nameAsString, methodNode, path, SlAnnoType.SLIME_OWNED
    ) {
        mReturnNodeList.addAll(methodNode.findAll(ReturnStmt::class.java))
    }

    override fun accept(cu: CompilationUnit, symbolTable: HashSet<SlSymbol>): List<SlError> {
        val errList = mutableListOf<SlError>()
        when (node) {
            is FieldDeclaration -> {
                mAssignNodeList.forEach { assignNode ->
                    // Assignment expression: target = value
                    val targetScope = assignNode.target
                    val targetName = getNameFromScope(targetScope).ifEmpty { return@forEach }
                    val isTargetBorrowSymbol = lazy {
                        isSymbolExistInScope<SlBorrowAnnoSymbol>(
                            symbolTable, targetScope, ConstructorDeclaration::class, MethodDeclaration::class
                        )
                    }
                    val loc = lazy { SlPositionFormatter.format(assignNode, path) }
                    if (targetName == name) {
                        // Owned symbol on left side
                        val valueScope = assignNode.value
                        val valueName = getNameFromScope(valueScope).ifEmpty { return@forEach }
                        val isValueBorrowSymbol = isSymbolExistInScope<SlBorrowAnnoSymbol>(
                            symbolTable, valueScope, ConstructorDeclaration::class, MethodDeclaration::class
                        )
                        if (isValueBorrowSymbol) {
                            // Borrow symbol on right side
                            errList += SlError(
                                SlErrorPhase.ALIAS_CHECK,
                                "Cannot assign borrow `$valueScope` to owned `$name` (${loc.value})"
                            )
                        } else if (targetName == valueName) {
                            // Self assignment
                            errList += SlError(
                                SlErrorPhase.ALIAS_CHECK, "Cannot self assign owned `$name` (${loc.value})"
                            )
                        }
                    } else if (!isTargetBorrowSymbol.value) {
                        // Owned symbol on right side and target symbol is not borrow
                        errList += SlError(
                            SlErrorPhase.ALIAS_CHECK,
                            "Cannot assign owned `$name` to non-borrow `$targetScope` (${loc.value})"
                        )
                    }
                }
                mVarNodeList.forEach { varNode ->
                    // Owned symbol will only on right side because var-declaration excluded owned symbol
                    val nameExpr = varNode.nameAsExpression.apply {
                        setParentNode(varNode)
                    }
                    val isTargetBorrowSymbol = isSymbolExistInScope<SlBorrowAnnoSymbol>(
                        symbolTable, nameExpr, ConstructorDeclaration::class, MethodDeclaration::class
                    )
                    val isTargetCopySymbol = isSymbolExistInScope<SlCopyAnnoSymbol>(
                        symbolTable, nameExpr, ConstructorDeclaration::class, MethodDeclaration::class
                    )
                    if (!(isTargetBorrowSymbol || isTargetCopySymbol)) {
                        val loc = SlPositionFormatter.format(varNode, path)
                        errList += SlError(
                            SlErrorPhase.ALIAS_CHECK, "Cannot assign owned `$name` to symbol `$nameExpr` ($loc)"
                        )
                    }
                }
                mReturnNodeList.forEach { returnNode ->
                    // Owned return value check method
                    val expr = returnNode.expression.getOrNull() ?: return@forEach
                    val isValidReturn = when (expr) {
                        // Exactly return owned symbol
                        is NameExpr, is FieldAccessExpr -> symbolTable.filterIsInstance<SlOwnedAnnoSymbol>()
                            .any { owned -> owned.node.isAncestorOf(expr) }
                        // Owned symbol is included in method call
                        is MethodCallExpr -> {
                            var isMethodOwned = false
                            var isCallerOwned = false
                            symbolTable.filterIsInstance<SlOwnedAnnoSymbol>().forEach { owned ->
                                if (owned.node.isAncestorOf(expr)) {
                                    isMethodOwned = true
                                } else if (owned.name == "${expr.name}" && owned.node is MethodDeclaration) {
                                    isCallerOwned = true
                                }
                            }
                            // No care about if caller non-owned because it will be checked in method declaration
                            if (!isCallerOwned || isMethodOwned) {
                                return@forEach
                            }
                            false
                        }

                        else -> return@forEach
                    }
                    if (!isValidReturn) {
                        val loc = SlPositionFormatter.format(returnNode, path)
                        val methodName =
                            expr.findAncestor(MethodDeclaration::class.java).getOrNull()?.nameAsString.orEmpty()
                        errList += SlError(
                            SlErrorPhase.ALIAS_CHECK,
                            "Cannot return owned `$expr` to non-owned method `$methodName` ($loc)"
                        )
                    }
                }
            }

            is MethodDeclaration -> {
                mReturnNodeList.forEach { returnNode ->
                    // Owned method check return value
                    val expr = returnNode.expression.getOrNull() ?: return@forEach
                    val isValidReturn = when (expr) {
                        // Exactly return owned symbol
                        is NameExpr, is FieldAccessExpr -> symbolTable.filterIsInstance<SlOwnedAnnoSymbol>()
                            .any { owned ->
                                expr.findAll(SimpleName::class.java).any { "$it" == owned.name }
                                        && owned.node.findAncestor(ClassOrInterfaceDeclaration::class.java)
                                    .getOrNull()?.isAncestorOf(node) == true
                            }
                        // Owned symbol is included in method call
                        is MethodCallExpr -> symbolTable.filterIsInstance<SlOwnedAnnoSymbol>().any { owned ->
                            owned.name == "${expr.name}" && owned.node is MethodDeclaration
                        }

                        else -> return@forEach
                    }
                    if (!isValidReturn) {
                        val loc = SlPositionFormatter.format(returnNode, path)
                        errList += SlError(
                            SlErrorPhase.ALIAS_CHECK,
                            "Cannot return non-owned `$expr` to owned method `${node.name}` ($loc)"
                        )
                    }
                }
            }
        }
        return errList
    }

    private inline fun <reified T : SlAnnoSymbol> isSymbolExistInScope(
        symbolTable: HashSet<SlSymbol>, scope: Expression, vararg scopeFilterClazz: KClass<out Node>
    ) = symbolTable.filterIsInstance<T>().any { symbol ->
        if (symbol.name != "$scope") {
            return@any false
        }
        scopeFilterClazz.any { symbol.node.findAncestor(it.java).getOrNull()?.isAncestorOf(scope) == true }
    }

    private fun getNameFromScope(scope: Expression): String {
        return when (scope) {
            is NameExpr -> scope.nameAsString
            is FieldAccessExpr -> getNameFromScope(scope.scope)
            is MethodCallExpr -> getNameFromScope(scope.scope.getOrNull() ?: return "")
            else -> ""
        }
    }
}