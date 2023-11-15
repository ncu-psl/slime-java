package tw.edu.ncu.csie.psl.translator

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.type.ClassOrInterfaceType
import tw.edu.ncu.csie.psl.checker.SlChecker
import tw.edu.ncu.csie.psl.common.SlCommon
import tw.edu.ncu.csie.psl.logger.SlLogger
import tw.edu.ncu.csie.psl.symbol.SlSymbol
import tw.edu.ncu.csie.psl.symbol.annotation.SlCopyAnnoSymbol
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.jvm.optionals.getOrNull

object SlTranslator {
    private val mLogger = SlLogger(SlChecker::class.java.simpleName)

    fun run(cuMap: Map<Path, CompilationUnit>, symbolTable: HashSet<SlSymbol>) {
        val genPaths = cuMap.map { (path, cu) -> translate(path, cu, symbolTable) }
        compile(genPaths.firstOrNull() ?: return)
    }

    private fun translate(path: Path, cu: CompilationUnit, symbolTable: HashSet<SlSymbol>): Path {
        // TODO: Add annotation interface if not exist
        val newName = "${ArrayList::class.java.simpleName}_Copy"
        val newClass = cu.addClass(newName).setPublic(false)
        val newMethod = newClass.addMethod(SlCopyAnnoSymbol.COPY_METHOD_NAME).setStatic(true)
        symbolTable.filterIsInstance<SlCopyAnnoSymbol>().forEach inner@{ copy ->
            val varDecNode = copy.node as VariableDeclarationExpr
            varDecNode.findAll(VariableDeclarator::class.java).forEach { varNode ->
                val classType = varNode.type as? ClassOrInterfaceType ?: return@inner
                val argType = classType.typeArguments.getOrNull()?.first() ?: return@inner
                val listParameter = Parameter(classType, "list")
                newMethod.setType(classType).setParameters(NodeList(listParameter))
                val stateType = ClassOrInterfaceType().setTypeArguments(classType.typeArguments.getOrNull())
                    .setName(ArrayList::class.java.simpleName)
                val newListVar =
                    VariableDeclarator(stateType, "newList", ObjectCreationExpr(null, stateType, NodeList()))
                newMethod.createBody()
                    .addStatement(
                        VariableDeclarationExpr(newListVar)
                    ).addStatement(
                        MethodCallExpr(
                            listParameter.nameAsExpression, "forEach", NodeList(
                                LambdaExpr(
                                    NodeList(Parameter(argType, "item")),
                                    BlockStmt().addStatement(
                                        MethodCallExpr(
                                            newListVar.nameAsExpression, "add", NodeList(
                                                ObjectCreationExpr(
                                                    null,
                                                    ClassOrInterfaceType().setName("$argType"),
                                                    NodeList(NameExpr("item"))
                                                )
                                            )
                                        )
                                    ), true
                                )
                            )
                        )
                    ).addStatement(ReturnStmt(newListVar.nameAsExpression))
            }
            val init = varDecNode.variables.last().initializer.getOrNull() ?: return@inner
            val replaceInit = MethodCallExpr(newClass.nameAsExpression, newMethod.name).addArgument(init)
            varDecNode.variables.last().setInitializer(replaceInit)
        }
        val genDir = File(path.parent.toFile(), SlCommon.GEN_FOLDER_NAME)
        genDir.mkdirs()
        return Files.writeString(genDir.toPath().resolve(path.fileName), cu.toString(), StandardCharsets.UTF_8)
    }

    private fun compile(path: Path) {
        val process = ProcessBuilder("javac", path.toString()).start()
        val error = process.errorStream.bufferedReader().readText()
        if (error.isNotEmpty()) {
            mLogger.error(error)
            println(error)
        }
    }
}