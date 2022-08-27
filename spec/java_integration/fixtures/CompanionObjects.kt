package java_integration.fixtures;

public class CompanionObject {
    companion object {
        const val COMPANION_CONST = "Constant"

	val value = "Value"

	@JvmStatic
	val jvmStaticValue = "JvmStaticValue"

        fun companionMethod(): String {
            return "Method"
        }

        @JvmStatic
        fun jvmStaticMethod(): String {
            return "JvmStaticMethod"
        }
    }
}
