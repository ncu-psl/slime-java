package tw.edu.ncu.csie.psl.error

open class SlError(val phase: SlErrorPhase, private val msg: String) {
    override fun toString() = "Error($phase): $msg"
}