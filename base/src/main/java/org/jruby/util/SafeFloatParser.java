package org.jruby.util;

import java.math.BigDecimal;

/**
 * A safer way to parse float values
 * <p>
 * Prevents brute force attacks using the famous Java bug.
 */
public final class SafeFloatParser extends SafeDecimalParser {

    /**
     * Safe way of parsing a Float value from a String
     * 
     * @param s
     *            The input String
     * @return the Float value
     */
    public static Float valueOf(String s) {
        Float result = null;
        Double decimalValue = decimalValueOf(s);
        if (decimalValue != null) {
            result = Float.valueOf(decimalValue.floatValue());
        }
        return result;
    }

    /**
     * Safe way of parsing a Float value from a String
     * 
     * @param s
     *            The input String
     * @return the Float value
     */
    public static Float parseFloat(String s) {
        return valueOf(s);
    }

    /**
     * Safe way of getting the float value<br>
     * prevents BigDecimal from calling Float.parseFloat()
     * 
     * @param number
     * @return the float value
     */
    public static float floatValue(Number number) {
        return Float.valueOf((float)decimalValue(number));
    }

    /**
     * Safe way of getting the float value<br>
     * Prevents BigDecimal from calling Float.parseFloat()
     * 
     * @param bigDecimal
     * @return the float value
     */
    public static float floatValue(BigDecimal bigDecimal) {
        return Float.valueOf((float)decimalValue(bigDecimal));
    }
}
