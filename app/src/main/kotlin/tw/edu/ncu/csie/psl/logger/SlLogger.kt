package tw.edu.ncu.csie.psl.logger

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator

class SlLogger(tag: String, level: Level = Level.TRACE) {
    companion object {
        const val CONSOLE = "Console"
        const val FILE = "File"
        val sGlobal by lazy { SlLogger("Global") }
    }

    private val mLogger = LogManager.getLogger(tag)

    var level = mLogger.level
        set(value) {
            Configurator.setLevel(mLogger, value)
            field = value
        }
        get() = mLogger.level

    init {
        this.level = level
    }

    fun trace(msg: String, ex: Throwable? = null) = ex?.also { mLogger.trace(msg, it) } ?: mLogger.trace(msg)

    fun debug(msg: String, ex: Throwable? = null) = ex?.also { mLogger.debug(msg, it) } ?: mLogger.debug(msg)

    fun info(msg: String, ex: Throwable? = null) = ex?.also { mLogger.info(msg, it) } ?: mLogger.info(msg)

    fun warn(msg: String, ex: Throwable? = null) = ex?.also { mLogger.warn(msg, it) } ?: mLogger.warn(msg)

    fun error(msg: String, ex: Throwable? = null) = ex?.also { mLogger.error(msg, it) } ?: mLogger.error(msg)
}
