package org.jruby.truffle.parser;

import java.math.BigDecimal;

/**
 * A safer way to parse double values
 * <p>
 * Prevents brute force attacks using the famous Java bug.
 */
final public class SafeDoubleParser extends SafeDecimalParser {

    /**
     * Safe way of parsing a Double value from a String
     * 
     * @param s
     *            The input String
     * @return the Double value
     */
    public static Double valueOf(String s) {
        return decimalValueOf(s);
    }

    /**
     * Safe way of parsing a Double value from a String
     * 
     * @param s
     *            The input String
     * @return the Double value
     */
    public static Double parseDouble(String s) {
        return valueOf(s);
    }

    /**
     * Safe way of getting the double value<br>
     * prevents BigDecimal from calling Double.parseDouble()
     * 
     * @param number
     * @return the double value
     */
    public static double doubleValue(Number number) {
        return decimalValue(number);
    }

    /**
     * Safe way of getting the double value<br>
     * Prevents BigDecimal from calling Double.parseDouble()
     * 
     * @param bigDecimal
     * @return the double value
     */
    public static double doubleValue(BigDecimal bigDecimal) {
        return decimalValue(bigDecimal);
    }
}
