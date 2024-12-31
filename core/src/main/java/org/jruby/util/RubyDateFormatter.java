/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002, 2009 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.util;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import jnr.constants.platform.Errno;
import org.jcodings.Encoding;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.chrono.GJChronology;
import org.joda.time.chrono.JulianChronology;
import org.jruby.Ruby;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.lexer.StrftimeLexer;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.numToInt;
import static org.jruby.api.Convert.numToLong;
import static org.jruby.api.Error.argumentError;
import static org.jruby.util.CommonByteLists.*;
import static org.jruby.util.RubyDateFormatter.FieldType.*;

public class RubyDateFormatter {
    private static final ByteList AM = new ByteList(new byte[] {'a', 'm'});
    private static final ByteList PM = new ByteList(new byte[] {'p', 'm'});
    private static final ByteList CAPITAL_AM = new ByteList(new byte[] {'A', 'M'});
    private static final ByteList CAPITAL_PM = new ByteList(new byte[] {'P', 'M'});

    private static final String[] FORMAT_MONTHS;
    private static final String[] FORMAT_SHORT_MONTHS;
    private static final String[] FORMAT_WEEKDAYS;
    private static final String[] FORMAT_SHORT_WEEKDAYS;
    static {
        final DateFormatSymbols FORMAT_SYMBOLS = new DateFormatSymbols(Locale.US);
        FORMAT_MONTHS = FORMAT_SYMBOLS.getMonths();
        FORMAT_SHORT_MONTHS = FORMAT_SYMBOLS.getShortMonths();
        FORMAT_WEEKDAYS = FORMAT_SYMBOLS.getWeekdays();
        FORMAT_SHORT_WEEKDAYS = FORMAT_SYMBOLS.getShortWeekdays();
    }

    private static final Token[] CONVERSION2TOKEN = new Token[256];

    private final Ruby runtime;
    private final StrftimeLexer lexer;

    public enum Format {
        /** raw string, no formatting */
        FORMAT_STRING,
        /** formatter */
        FORMAT_OUTPUT,
        /** composition of other formats, or depends on library */
        FORMAT_SPECIAL,

        /** %A */
        FORMAT_WEEK_LONG('A'),
        /** %a */
        FORMAT_WEEK_SHORT('a'),
        /** %B */
        FORMAT_MONTH_LONG('B'),
        /** %b, %h */
        FORMAT_MONTH_SHORT('b', 'h'),
        /** %C */
        FORMAT_CENTURY('C'),
        /** %d */
        FORMAT_DAY('d'),
        /** %e */
        FORMAT_DAY_S('e'),
        /** %G */
        FORMAT_WEEKYEAR('G'),
        /** %g */
        FORMAT_WEEKYEAR_SHORT('g'),
        /** %H */
        FORMAT_HOUR('H'),
        /** %I */
        FORMAT_HOUR_M('I'),
        /** %j */
        FORMAT_DAY_YEAR('j'),
        /** %k */
        FORMAT_HOUR_BLANK('k'),
        /** %L */
        FORMAT_MILLISEC('L'),
        /** %l */
        FORMAT_HOUR_S('l'),
        /** %M */
        FORMAT_MINUTES('M'),
        /** %m */
        FORMAT_MONTH('m'),
        /** %N */
        FORMAT_NANOSEC('N'),
        /** %P */
        FORMAT_MERIDIAN_LOWER_CASE('P'),
        /** %p */
        FORMAT_MERIDIAN('p'),
        /** %S */
        FORMAT_SECONDS('S'),
        /** %s */
        FORMAT_EPOCH('s'),
        /** %U */
        FORMAT_WEEK_YEAR_S('U'),
        /** %u */
        FORMAT_DAY_WEEK2('u'),
        /** %V */
        FORMAT_WEEK_WEEKYEAR('V'),
        /** %W */
        FORMAT_WEEK_YEAR_M('W'),
        /** %w */
        FORMAT_DAY_WEEK('w'),
        /** %Y */
        FORMAT_YEAR_LONG('Y'),
        /** %y */
        FORMAT_YEAR_SHORT('y'),
        /** %z, %:z, %::z, %:::z */
        FORMAT_COLON_ZONE_OFF, // must be given number of colons as data

        /* Change between Time and Date */
        /** %Z */
        FORMAT_ZONE_ID,

        /* Only for Date/DateTime from here */
        /** %Q */
        FORMAT_MICROSEC_EPOCH;

        Format() {}

        Format(char conversion) {
            addToConversions(conversion, new Token(this));
        }

        Format(char conversion, char alias) {
            this(conversion);
            addToConversions(alias, conversionToToken(conversion));
        }

        // This is still an ugly side effect but avoids jruby/jruby#5179 or class init hacks by forcing all accesses
        // to initialize the Format class via the Token class.
        private static void addToConversions(char conversion, Token token) {
            CONVERSION2TOKEN[conversion] = token;
        }

        private static Token conversionToToken(int conversion) {
            return CONVERSION2TOKEN[conversion];
        }
    }

    public static Token COLON_TOKEN = new Token(Format.FORMAT_STRING, COLON);
    public static Token DASH_TOKEN = new Token(Format.FORMAT_STRING, DASH);
    public static Token DOT_TOKEN = new Token(Format.FORMAT_STRING, DOT);
    public static Token SLASH_TOKEN = new Token(Format.FORMAT_STRING, SLASH);

    public static class Token {
        private final Format format;
        protected Object data;
        
        protected Token(Format format) {
            this(format, null);
        }

        public Token(Format formatString, Object data) {
            this.format = formatString;
            this.data = data;
        }

        public static Token str(ByteList str) {
            if (str.length() == 1) {
                switch (str.charAt(0)) {
                    case ':':
                        return COLON_TOKEN;
                    case '.':
                        return DOT_TOKEN;
                    case '-':
                        return DASH_TOKEN;
                    case '/':
                        return SLASH_TOKEN;
                }
            }
            return new Token(Format.FORMAT_STRING, str);
        }

        public static Token format(char c) {
            return Format.conversionToToken(c);
        }

        public static Token zoneOffsetColons(int colons) {
            return new Token(Format.FORMAT_COLON_ZONE_OFF, colons);
        }

        public static Token special(char c) {
            return new Token(Format.FORMAT_SPECIAL, c);
        }

        public static Token formatter(RubyTimeOutputFormatter formatter) {
            return formatter;
        }

        /**
         * Gets the data.
         * @return Returns a Object
         */
        public Object getData() {
            return data;
        }

        /**
         * Gets the format.
         * @return Returns a int
         */
        public Format getFormat() {
            return format;
        }

        @Override
        public String toString() {
            return "<Token "+format+ " "+data+">";
        }
    }

    /**
     * Constructor for RubyDateFormatter.
     */
    public RubyDateFormatter(ThreadContext context) {
        super();
        this.runtime = context.runtime;
        lexer = new StrftimeLexer();
    }

    private void addToPattern(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                addToken(Token.format(c));
            } else {
                addToken(Token.str(new ByteList(new byte[] { (byte) c }))); // FIXME broken patterns hsould pass in bytelists
            }
        }
    }

    private Token[] compiledPattern = new Token[256];
    private int compiledPatternLength = 0;
    private Encoding patternEncoding;

    private void addToken(Token token) {
        if (compiledPatternLength >= compiledPattern.length) growTokens();

        compiledPattern[compiledPatternLength] = token;
        compiledPatternLength++;
    }

    // This will never happen...
    private void growTokens() {
        Token[] newCompiledPattern = new Token[compiledPattern.length * 2];
        System.arraycopy(compiledPattern, 0, newCompiledPattern, 0, compiledPattern.length);
        compiledPattern = newCompiledPattern;
    }

    public void compilePattern(ByteList pattern, boolean dateLibrary) {
        compiledPatternLength = 0;
        patternEncoding = pattern.getEncoding();
        if (!patternEncoding.isAsciiCompatible()) {
            throw argumentError(runtime.getCurrentContext(), "format should have ASCII compatible encoding");
        }

        lexer.reset(pattern);

        Token token;
        RubyTimeOutputFormatter formatter = null;
            while ((token = lexer.yylex()) != null) {
                if (token.format == Format.FORMAT_OUTPUT) {
                    formatter = (RubyTimeOutputFormatter) token.data;
                } else if (token.format != Format.FORMAT_SPECIAL) {
                    if (formatter != null) {
                        addToken(formatter);
                        formatter = null;
                    }
                    addToken(token);
                } else {
                    char c = (Character) token.data;

                    // prepare padding if necessary
                    if (formatter != null) {
                        switch (c) {
                            default:
                                // force most formats to use spaces for padding unless otherwise specified
                                if (formatter.flags == ByteList.EMPTY_BYTELIST || formatter.flags == null) {
                                    addToken(new RubyTimeOutputFormatter(UNDERSCORE, formatter.width));
                                    break;
                                }
                                // fall through
                            case 'Q':
                            case '+':
                                addToken(formatter);
                        }
                        formatter = null;
                    }

                    switch (c) {
                    case 'c':
                        addToPattern("a b e H:M:S Y");
                        break;
                    case 'D':
                    case 'x':
                        addToPattern("m/d/y");
                        break;
                    case 'F':
                        addToPattern("Y-m-d");
                        break;
                    case 'n':
                        addToken(Token.str(NEWLINE));
                        break;
                    case 'Q':
                        if (dateLibrary) {
                            addToken(new Token(Format.FORMAT_MICROSEC_EPOCH));
                        } else {
                            addToken(Token.str(PERCENT_Q));
                        }
                        break;
                    case 'R':
                        addToPattern("H:M");
                        break;
                    case 'r':
                        addToPattern("I:M:S p");
                        break;
                    case 'T':
                    case 'X':
                        addToPattern("H:M:S");
                        break;
                    case 't':
                        addToken(Token.str(TAB));
                        break;
                    case 'v':
                        addToPattern("e-");
                        if (!dateLibrary)
                            addToken(Token.formatter(new RubyTimeOutputFormatter(CARET, 0)));
                        addToPattern("^b-Y");
                        break;
                    case 'Z':
                        if (dateLibrary) {
                            // +HH:MM in 'date', never zone name
                            addToken(Token.zoneOffsetColons(1));
                        } else {
                            addToken(new Token(Format.FORMAT_ZONE_ID));
                        }
                        break;
                    case '+':
                        if (!dateLibrary) {
                            addToken(Token.str(PERCENT_PLUS));
                            break;
                        }
                        addToPattern("a b e H:M:S ");
                        // %Z: +HH:MM in 'date', never zone name
                        addToken(Token.zoneOffsetColons(1));
                        addToPattern(" Y");
                        break;
                    default:
                        throw new AssertionError("Unknown special char: " + c);
                    }
                }
            }

            // if formatter is still set we didn't use it, add it as is
            if (formatter != null) {
                addToken(formatter);
            }
    }

    enum FieldType {
        NUMERIC('0', 0),
        NUMERIC2('0', 2),
        NUMERIC2BLANK(' ', 2),
        NUMERIC3('0', 3),
        NUMERIC4('0', 4),
        NUMERIC5('0', 5),
        TEXT(' ', 0);

        final char defaultPadder;
        final int defaultWidth;
        FieldType(char padder, int width) {
            defaultPadder = padder;
            defaultWidth = width;
        }
    }

    /** Convenience method when using no pattern caching */
    public RubyString compileAndFormat(ByteList pattern, boolean dateLibrary, DateTime dt, long nsec, RubyNumeric sub_millis) {
        compilePattern(pattern, dateLibrary);
        RubyString out = format(compiledPattern, dt, nsec, sub_millis);
        return out;
    }

    public RubyString format(Token[] compiledPattern, DateTime dt, long nsec, RubyNumeric sub_millis) {
        return runtime.newString(formatToByteList(compiledPattern, dt, nsec, sub_millis));
    }

    private ByteList formatToByteList(Token[] compiledPattern, DateTime dt, long nsec, RubyNumeric sub_millis) {
        RubyTimeOutputFormatter formatter = RubyTimeOutputFormatter.DEFAULT_FORMATTER;
        final ByteList output = new ByteList(27, patternEncoding); // Typical length produced by logger by default
        final ByteList tmp = new ByteList(48);

        boolean toUpper = false;

        for (int ti = 0; ti < compiledPatternLength; ti++) {
            Token token = compiledPattern[ti];
            CharSequence data = null;
            long value = 0;
            FieldType type = TEXT;
            Format format = token.getFormat();

            switch (format) {
                case FORMAT_OUTPUT:
                    formatter = (RubyTimeOutputFormatter) token.getData();
                    continue; // go to next token
                case FORMAT_STRING:
                    data = (ByteList) token.getData();
                    if ("^".equals(data.toString())) {
                        toUpper = true;
                        continue;
                    }
                    break;
                case FORMAT_WEEK_LONG:
                    // This is GROSS, but Java API's aren't ISO 8601 compliant at all
                    int v = (dt.getDayOfWeek() + 1) % 8;
                    data = FORMAT_WEEKDAYS[v == 0 ? 1 : v];
                    break;
                case FORMAT_WEEK_SHORT:
                    // This is GROSS, but Java API's aren't ISO 8601 compliant at all
                    v = (dt.getDayOfWeek() + 1) % 8;
                    data = FORMAT_SHORT_WEEKDAYS[v == 0 ? 1 : v];
                    break;
                case FORMAT_MONTH_LONG:
                    data = FORMAT_MONTHS[dt.getMonthOfYear() - 1];
                    break;
                case FORMAT_MONTH_SHORT:
                    data = FORMAT_SHORT_MONTHS[dt.getMonthOfYear() - 1];
                    break;
                case FORMAT_DAY:
                    type = NUMERIC2;
                    value = dt.getDayOfMonth();
                    break;
                case FORMAT_DAY_S:
                    type = NUMERIC2BLANK;
                    value = dt.getDayOfMonth();
                    break;
                case FORMAT_HOUR:
                    type = NUMERIC2;
                    value = dt.getHourOfDay();
                    break;
                case FORMAT_HOUR_BLANK:
                    type = NUMERIC2BLANK;
                    value = dt.getHourOfDay();
                    break;
                case FORMAT_HOUR_M:
                case FORMAT_HOUR_S:
                    value = dt.getHourOfDay();
                    if (value == 0) {
                        value = 12;
                    } else if (value > 12) {
                        value -= 12;
                    }

                    type = (format == Format.FORMAT_HOUR_M) ? NUMERIC2 : NUMERIC2BLANK;
                    break;
                case FORMAT_DAY_YEAR:
                    type = NUMERIC3;
                    value = dt.getDayOfYear();
                    break;
                case FORMAT_MINUTES:
                    type = NUMERIC2;
                    value = dt.getMinuteOfHour();
                    break;
                case FORMAT_MONTH:
                    type = NUMERIC2;
                    value = dt.getMonthOfYear();
                    break;
                case FORMAT_MERIDIAN:
                    data = dt.getHourOfDay() < 12 ? CAPITAL_AM : CAPITAL_PM;
                    break;
                case FORMAT_MERIDIAN_LOWER_CASE:
                    data = dt.getHourOfDay() < 12 ? AM : PM;
                    break;
                case FORMAT_SECONDS:
                    type = NUMERIC2;
                    value = dt.getSecondOfMinute();
                    break;
                case FORMAT_WEEK_YEAR_M:
                    type = NUMERIC2;
                    value = formatWeekYear(dt, java.util.Calendar.MONDAY);
                    break;
                case FORMAT_WEEK_YEAR_S:
                    type = NUMERIC2;
                    value = formatWeekYear(dt, java.util.Calendar.SUNDAY);
                    break;
                case FORMAT_DAY_WEEK:
                    type = NUMERIC;
                    value = dt.getDayOfWeek() % 7;
                    break;
                case FORMAT_DAY_WEEK2:
                    type = NUMERIC;
                    value = dt.getDayOfWeek();
                    break;
                case FORMAT_YEAR_LONG:
                    value = year(dt, dt.getYear());
                    type = (value >= 0) ? NUMERIC4 : NUMERIC5;
                    break;
                case FORMAT_YEAR_SHORT:
                    type = NUMERIC2;
                    value = year(dt, dt.getYear()) % 100;
                    break;
                case FORMAT_COLON_ZONE_OFF:
                    // custom logic because this is so weird
                    value = dt.getZone().getOffset(dt.getMillis()) / 1000;
                    int colons = (Integer) token.getData();
                    data = formatZone(colons, (int) value, formatter);
                    break;
                case FORMAT_ZONE_ID:
                    // Should be safe to assume all time zone labels will be ASCII 7bit.
                    data = RubyTime.getRubyTimeZoneName(runtime.getCurrentContext(), dt);
                    break;
                case FORMAT_CENTURY:
                    type = NUMERIC;
                    value = year(dt, dt.getYear()) / 100;
                    break;
                case FORMAT_EPOCH:
                    type = NUMERIC;
                    value = dt.getMillis() / 1000;
                    break;
                case FORMAT_WEEK_WEEKYEAR:
                    type = NUMERIC2;
                    value = dt.getWeekOfWeekyear();
                    break;
                case FORMAT_MILLISEC:
                case FORMAT_NANOSEC:
                    int defaultWidth = (format == Format.FORMAT_NANOSEC) ? 9 : 3;
                    int width = formatter.getWidth(defaultWidth);

                    tmp.setRealSize(0);
                    data = tmp;//new ByteList(width + width);
                    RubyTimeOutputFormatter.formatNumber((ByteList) data, dt.getMillisOfSecond(), 3, '0');
                    if (width > 3) {
                        if (sub_millis == null) { // Time
                            RubyTimeOutputFormatter.formatNumber((ByteList) data, nsec, 6, '0');
                        } else { // Date, DateTime
                            formatSubMillisGt3(runtime.getCurrentContext(), (ByteList) data, width, sub_millis);
                        }
                    }

                    if (width < data.length()) {
                        ((ByteList) data).setRealSize(width);
                    } else {
                        int padLength = width - data.length();
                        // FIXME: length - width fill of 0's can be pre-calc'd
                        for (int i = 0; i < padLength; i++) {  // Not enough precision, fill with 0
                            ((ByteList) data).append('0');
                        }
                    }
                    formatter = RubyTimeOutputFormatter.DEFAULT_FORMATTER; // no more formatting
                    break;
                case FORMAT_WEEKYEAR:
                    value = year(dt, dt.getWeekyear());
                    type = (value >= 0) ? NUMERIC4 : NUMERIC5;
                    break;
                case FORMAT_WEEKYEAR_SHORT:
                    type = NUMERIC2;
                    value = year(dt, dt.getWeekyear()) % 100;
                    break;
                case FORMAT_MICROSEC_EPOCH:
                    // only available for Date
                    type = NUMERIC;
                    value = dt.getMillis();
                    break;
                case FORMAT_SPECIAL:
                    throw new Error("FORMAT_SPECIAL is a special token only for the lexer.");
            }

            try {
                if (data == null) {
                    formatter.format(output, value, type);
                } else {
                    if (toUpper) {
                        formatter.format(output, data.toString().toUpperCase());
                        toUpper = false;
                    } else {
                        formatter.format(output, data);
                    }
                }
            } catch (IndexOutOfBoundsException ioobe) {
                throw runtime.newErrnoFromErrno(Errno.ERANGE, "strftime");
            }

            // reset formatter
            formatter = RubyTimeOutputFormatter.DEFAULT_FORMATTER;
        }

        return output;
    }

    private static void formatSubMillisGt3(ThreadContext context, final ByteList buff,
                                           final int width, RubyNumeric sub_millis) {
        final int prec = width - 3;
        RubyNumeric power = (RubyNumeric) asFixnum(context, 10).op_pow(context, prec);
        RubyNumeric truncated = (RubyNumeric) sub_millis.numerator(context).
                convertToInteger().op_mul(context, power);
        truncated = (RubyNumeric) truncated.idiv(context, sub_millis.denominator(context));
        long decimals = numToLong(context, truncated);
        RubyTimeOutputFormatter.formatNumber(buff, decimals, prec, '0');
    }

    /**
     * Ruby always follows Astronomical year numbering,
     * that is BC x is -x+1 and there is a year 0 (BC 1)
     * but Joda-time returns -x for year x BC in Julian chronology (no year 0) */
    private static int year(DateTime dt, int year) {
        Chronology c;
        if (year < 0 && (
                (c = dt.getChronology()) instanceof JulianChronology ||
                (c instanceof GJChronology && ((GJChronology) c).getGregorianCutover().isAfter(dt))))
            return year + 1;
        return year;
    }

    private static int formatWeekYear(DateTime dt, int firstDayOfWeek) {
        java.util.Calendar dtCalendar = dt.toGregorianCalendar();
        dtCalendar.setFirstDayOfWeek(firstDayOfWeek);
        dtCalendar.setMinimalDaysInFirstWeek(7);
        int value = dtCalendar.get(java.util.Calendar.WEEK_OF_YEAR);
        if ((value == 52 || value == 53) && (dtCalendar.get(Calendar.MONTH) == Calendar.JANUARY )) {
            // MRI behavior: Week values are monotonous.
            // So, weeks that effectively belong to previous year,
            // will get the value of 0, not 52 or 53, as in Java.
            value = 0;
        }
        return value;
    }

    private static ByteList formatZone(int colons, int value, RubyTimeOutputFormatter formatter) {
        int seconds = Math.abs(value);
        int hours = seconds / 3600;
        seconds %= 3600;
        int minutes = seconds / 60;
        seconds %= 60;

        if (value < 0 && hours != 0) { // see below when hours == 0
            hours = -hours;
        }

        char padder = formatter.getPadder('0');
        int defaultWidth = -1;
        ByteList after = new ByteList(12);

        switch (colons) {
            case 0: // %z -> +hhmm
                defaultWidth = 5;
                RubyTimeOutputFormatter.formatNumber(after, minutes, 2, '0');
                break;
            case 1: // %:z -> +hh:mm
                defaultWidth = 6;
                after.append(':');
                RubyTimeOutputFormatter.formatNumber(after, minutes, 2, '0');
                break;
            case 2: // %::z -> +hh:mm:ss
                defaultWidth = 9;
                after.append(':');
                RubyTimeOutputFormatter.formatNumber(after, minutes, 2, '0');
                after.append(':');
                RubyTimeOutputFormatter.formatNumber(after, seconds, 2, '0');
                break;
            case 3: // %:::z -> +hh[:mm[:ss]]
                if (minutes != 0 || seconds != 0) {
                    after.append(':');
                    RubyTimeOutputFormatter.formatNumber(after, minutes, 2, '0');
                }
                if (seconds != 0) {
                    after.append(':');
                    RubyTimeOutputFormatter.formatNumber(after, seconds, 2, '0');
                }
                defaultWidth = after.length() + 3;
                break;
        }

        int minWidth = defaultWidth - 1;
        int width = formatter.getWidth(defaultWidth);
        if (width < minWidth) {
            width = minWidth;
        }
        ByteList result = new ByteList(width);
        width -= after.length();
        RubyTimeOutputFormatter.formatSignedNumber(result, hours, value, width, padder);
        result.append(after); // before + after
        return result;
    }

    /**
     * @see DateFormat#parse(String, ParsePosition)
     */
    public Date parse(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

}
