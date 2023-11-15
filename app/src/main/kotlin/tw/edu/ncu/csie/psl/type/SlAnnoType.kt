package tw.edu.ncu.csie.psl.type

enum class SlAnnoType(val value: String) {
    /**
     * Declare the object is owned by the class
     */
    SLIME_OWNED("SlimeOwned"),

    /**
     * Declare the object borrow from [SLIME_OWNED] object
     */
    SLIME_BORROW("SlimeBorrow"),

    /**
     * Use `deepCopy()` method to copy the object
     */
    SLIME_COPY("SlimeCopy");

    companion object {
        fun fromValue(value: String) = when (value) {
            SLIME_OWNED.value -> SLIME_OWNED
            SLIME_BORROW.value -> SLIME_BORROW
            SLIME_COPY.value -> SLIME_COPY
            else -> null
        }
    }

    override fun toString() = value
}
