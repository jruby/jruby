package org.jruby.util;

import java.math.BigDecimal;

/**
 * A safer way to parse float values
 * <p>
 * Prevents brute force attacks using the famous Java bug.
 */
@Deprecated(since = "10.0.4.0")
public final class SafeFloatParser extends SafeDecimalParser {

    /**
     * Safe way of parsing a Float value from a String
     * 
     * @param s
     *            The input String
     * @return the Float value
     */
    @Deprecated(since = "10.0.4.0")
    public static Float valueOf(String s) {
        return s != null ? Float.valueOf(Double.valueOf(s).floatValue()) : null;
    }

    /**
     * Safe way of parsing a Float value from a String
     * 
     * @param s
     *            The input String
     * @return the Float value
     */
    @Deprecated(since = "10.0.4.0")
    public static Float parseFloat(String s) {
        return s != null ? Float.valueOf(Double.valueOf(s).floatValue()) : null;
    }

    /**
     * Safe way of getting the float value<br>
     * prevents BigDecimal from calling Float.parseFloat()
     * 
     * @param number
     * @return the float value
     */
    @Deprecated(since = "10.0.4.0")
    public static float floatValue(Number number) {
        return number != null ? (float) number.doubleValue() : 0.0f;
    }

    /**
     * Safe way of getting the float value<br>
     * Prevents BigDecimal from calling Float.parseFloat()
     * 
     * @param bigDecimal
     * @return the float value
     */
    @Deprecated(since = "10.0.4.0")
    public static float floatValue(BigDecimal bigDecimal) {
        return bigDecimal != null ? (float) bigDecimal.doubleValue() : 0.0f;
    }
}
