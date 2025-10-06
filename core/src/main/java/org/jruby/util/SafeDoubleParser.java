package org.jruby.util;

import java.math.BigDecimal;

/**
 * A safer way to parse double values
 * <p>
 * Prevents brute force attacks using the famous Java bug.
 */
@Deprecated(since = "10.0.4.0")
final public class SafeDoubleParser extends SafeDecimalParser {

    /**
     * Safe way of parsing a Double value from a String
     * 
     * @param s
     *            The input String
     * @return the Double value
     */
    @Deprecated(since = "10.0.4.0")
    public static Double valueOf(String s) {
        return s != null ? Double.valueOf(s) : null;
    }

    /**
     * Safe way of parsing a Double value from a String
     * 
     * @param s
     *            The input String
     * @return the Double value
     */
    @Deprecated(since = "10.0.4.0")
    public static Double parseDouble(String s) {
        return s != null ? Double.valueOf(s) : null;
    }

    /**
     * Safe way of getting the double value<br>
     * prevents BigDecimal from calling Double.parseDouble()
     * 
     * @param number
     * @return the double value
     */
    @Deprecated(since = "10.0.4.0")
    public static double doubleValue(Number number) {
        return number != null ? number.doubleValue() : 0.0;
    }

    /**
     * Safe way of getting the double value<br>
     * Prevents BigDecimal from calling Double.parseDouble()
     * 
     * @param bigDecimal
     * @return the double value
     */
    @Deprecated(since = "10.0.4.0")
    public static double doubleValue(BigDecimal bigDecimal) {
        return bigDecimal != null ? bigDecimal.doubleValue() : 0.0;
    }
}
