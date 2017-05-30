package org.jruby.util;

import static org.jruby.util.StrptimeFormat.*;

/**
 * This class is ported from RubyDateFormatter.Token in JRuby 9.1.5.0.
 * @see https://github.com/jruby/jruby/blob/036ce39f0476d4bd718e23e64caff36bb50b8dbc/core/src/main/java/org/jruby/util/RubyDateFormatter.java
 */
public class StrptimeToken {
    static final StrptimeToken[] CONVERSION2TOKEN = new StrptimeToken[256];

    static {
        CONVERSION2TOKEN['A'] = new StrptimeToken(FORMAT_WEEK_LONG);
        CONVERSION2TOKEN['a'] = new StrptimeToken(FORMAT_WEEK_SHORT);
        CONVERSION2TOKEN['B'] = new StrptimeToken(FORMAT_MONTH_LONG);
        CONVERSION2TOKEN['b'] = new StrptimeToken(FORMAT_MONTH_SHORT);
        CONVERSION2TOKEN['h'] = CONVERSION2TOKEN['b'];
        CONVERSION2TOKEN['C'] = new StrptimeToken(FORMAT_CENTURY);
        CONVERSION2TOKEN['d'] = new StrptimeToken(FORMAT_DAY);
        CONVERSION2TOKEN['e'] = new StrptimeToken(FORMAT_DAY_S);
        CONVERSION2TOKEN['G'] = new StrptimeToken(FORMAT_WEEKYEAR);
        CONVERSION2TOKEN['g'] = new StrptimeToken(FORMAT_WEEKYEAR_SHORT);
        CONVERSION2TOKEN['H'] = new StrptimeToken(FORMAT_HOUR);
        CONVERSION2TOKEN['I'] = new StrptimeToken(FORMAT_HOUR_M);
        CONVERSION2TOKEN['j'] = new StrptimeToken(FORMAT_DAY_YEAR);
        CONVERSION2TOKEN['k'] = new StrptimeToken(FORMAT_HOUR_BLANK);
        CONVERSION2TOKEN['L'] = new StrptimeToken(FORMAT_MILLISEC);
        CONVERSION2TOKEN['l'] = new StrptimeToken(FORMAT_HOUR_S);
        CONVERSION2TOKEN['M'] = new StrptimeToken(FORMAT_MINUTES);
        CONVERSION2TOKEN['m'] = new StrptimeToken(FORMAT_MONTH);
        CONVERSION2TOKEN['N'] = new StrptimeToken(FORMAT_NANOSEC);
        CONVERSION2TOKEN['P'] = new StrptimeToken(FORMAT_MERIDIAN_LOWER_CASE);
        CONVERSION2TOKEN['p'] = new StrptimeToken(FORMAT_MERIDIAN);
        CONVERSION2TOKEN['Q'] = new StrptimeToken(FORMAT_MICROSEC_EPOCH);
        CONVERSION2TOKEN['S'] = new StrptimeToken(FORMAT_SECONDS);
        CONVERSION2TOKEN['s'] = new StrptimeToken(FORMAT_EPOCH);
        CONVERSION2TOKEN['U'] = new StrptimeToken(FORMAT_WEEK_YEAR_S);
        CONVERSION2TOKEN['u'] = new StrptimeToken(FORMAT_DAY_WEEK2);
        CONVERSION2TOKEN['V'] = new StrptimeToken(FORMAT_WEEK_WEEKYEAR);
        CONVERSION2TOKEN['W'] = new StrptimeToken(FORMAT_WEEK_YEAR_M);
        CONVERSION2TOKEN['w'] = new StrptimeToken(FORMAT_DAY_WEEK);
        CONVERSION2TOKEN['Y'] = new StrptimeToken(FORMAT_YEAR_LONG);
        CONVERSION2TOKEN['y'] = new StrptimeToken(FORMAT_YEAR_SHORT);
    }

    private final StrptimeFormat format;
    private final Object data;

    StrptimeToken(StrptimeFormat format) {
        this(format, null);
    }

    StrptimeToken(StrptimeFormat formatString, Object data) {
        this.format = formatString;
        this.data = data;
    }

    public static StrptimeToken str(String str) {
        return new StrptimeToken(StrptimeFormat.FORMAT_STRING, str);
    }

    public static StrptimeToken format(char c) {
        return CONVERSION2TOKEN[c];
    }

    public static StrptimeToken zoneOffsetColons(int colons) {
        return new StrptimeToken(StrptimeFormat.FORMAT_COLON_ZONE_OFF, colons);
    }

    public static StrptimeToken special(char c) {
        return new StrptimeToken(StrptimeFormat.FORMAT_SPECIAL, c);
    }

    /**
     * Gets the data.
     * @return Returns a Object
     */
    Object getData() {
        return data;
    }

    /**
     * Gets the format.
     * @return Returns a int
     */
    StrptimeFormat getFormat() {
        return format;
    }

    @Override
    public String toString() {
        return "<Token "+format+ " "+data+">";
    }
}
