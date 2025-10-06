package org.jruby.util;

import java.math.BigDecimal;

@Deprecated(since = "10.0.4.0")
class SafeDecimalParser {
    /**
     * Safe parsing of a String into a Double
     * 
     * @param s The input String, can be null
     * @return The Double value
     *
     */
    @Deprecated(since = "10.0.4.0")
    final protected static Double decimalValueOf(String s) {
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
    final protected static double decimalValue(Number number) {
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
    final protected static double decimalValue(BigDecimal bigDecimal) {
        return bigDecimal != null ? bigDecimal.doubleValue() : 0.0;
    }
}
