package tw.edu.ncu.csie.psl

import com.github.javaparser.ParseProblemException
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import tw.edu.ncu.csie.psl.checker.SlChecker
import tw.edu.ncu.csie.psl.common.SlCommon
import tw.edu.ncu.csie.psl.error.SlError
import tw.edu.ncu.csie.psl.logger.SlLogger
import tw.edu.ncu.csie.psl.symbol.SlSymbol
import tw.edu.ncu.csie.psl.translator.SlTranslator
import java.io.File
import java.io.FileNotFoundException
import kotlin.jvm.optionals.getOrElse
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    if (!SlCommon.IS_DEBUG_MODE) {
        (LogManager.getContext(false) as? LoggerContext)?.apply {
            configuration.rootLogger.removeAppender(SlLogger.CONSOLE)
        }
    }
    val cuMap = args.mapNotNull { arg ->
        val file = File(arg)
        try {
            StaticJavaParser.setConfiguration(ParserConfiguration().apply {
                isAttributeComments = false
            })
            SlLogger.sGlobal.info("Parsing file: ${file.absolutePath}")
            StaticJavaParser.parse(file).let { cu ->
                return@mapNotNull cu.storage.map(CompilationUnit.Storage::getPath).getOrElse {
                    SlLogger.sGlobal.error("CompilationUnit has no storage path ${file.absolutePath}")
                    return@mapNotNull null
                } to cu
            }
        } catch (fnfe: FileNotFoundException) {
            SlLogger.sGlobal.error("File not found: ${file.absolutePath}", fnfe)
            println("File not found: ${file.absolutePath}")
        } catch (ppe: ParseProblemException) {
            SlLogger.sGlobal.error("Parsed with unexpected problem: ", ppe)
            println("Parse Error in ${file.absolutePath}, check log for more information")
        }
        null
    }.toMap()
    if (cuMap.size != args.size) {
        exitProcess(1)
    }
    val symbolTable = linkedSetOf<SlSymbol>()
    val errList = SlChecker.run(cuMap, symbolTable)
    if (errList.isNotEmpty()) {
        errList.sortedBy(SlError::phase).forEach(::println)
        exitProcess(0)
    }
    SlTranslator.run(cuMap, symbolTable)
    SlLogger.sGlobal.info("No errors found in checker")
}
