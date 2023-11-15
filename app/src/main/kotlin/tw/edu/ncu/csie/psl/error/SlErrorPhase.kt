package tw.edu.ncu.csie.psl.error

enum class SlErrorPhase(private val value: String) {
    ANNOTATION_CHECK("AnnotationCheck"),
    ALIAS_CHECK("AliasCheck");

    override fun toString() = value
}