/***** BEGIN LICENSE BLOCK *****
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import jnr.constants.platform.Errno;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
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

import static org.jruby.util.RubyDateFormatter.FieldType.*;

public class RubyDateFormatter {

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

    enum Format {
        /** encoding to give to output */
        FORMAT_ENCODING,
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

    public static class Token {
        private final Format format;
        private final Object data;
        
        protected Token(Format format) {
            this(format, null);
        }

        protected Token(Format formatString, Object data) {
            this.format = formatString;
            this.data = data;
        }

        public static Token str(String str) {
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
            return new Token(Format.FORMAT_OUTPUT, formatter);
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
        lexer = new StrftimeLexer((Reader) null);
    }

    private static void addToPattern(List<Token> compiledPattern, String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                compiledPattern.add(Token.format(c));
            } else {
                compiledPattern.add(Token.str(Character.toString(c)));
            }
        }
    }

    public List<Token> compilePattern(RubyString format, boolean dateLibrary) {
        return compilePattern(format.getByteList(), dateLibrary);
    }

    public List<Token> compilePattern(ByteList pattern, boolean dateLibrary) {
        Encoding enc = pattern.getEncoding();
        if (!enc.isAsciiCompatible()) {
            throw runtime.newArgumentError("format should have ASCII compatible encoding");
        }

        final List<Token> compiledPattern = new LinkedList<>();
        if (enc != ASCIIEncoding.INSTANCE) { // default for ByteList
            compiledPattern.add(new Token(Format.FORMAT_ENCODING, enc));
        }

        ByteArrayInputStream in = new ByteArrayInputStream(pattern.getUnsafeBytes(), pattern.getBegin(), pattern.getRealSize());
        Reader reader = new InputStreamReader(in, runtime.getEncodingService().charsetForEncoding(pattern.getEncoding()));
        lexer.yyreset(reader);

        Token token;
        RubyTimeOutputFormatter formatter = null;
        try {
            while ((token = lexer.yylex()) != null) {
                if (token.format == Format.FORMAT_OUTPUT) {
                    formatter = (RubyTimeOutputFormatter) token.data;
                } else if (token.format != Format.FORMAT_SPECIAL) {
                    if (formatter != null) {
                        compiledPattern.add(new Token(Format.FORMAT_OUTPUT, formatter));
                        formatter = null;
                    }
                    compiledPattern.add(token);
                } else {
                    char c = (Character) token.data;

                    // prepare padding if necessary
                    if (formatter != null) {
                        switch (c) {
                            default:
                                // force most formats to use spaces for padding unless otherwise specified
                                if (formatter.flags == "" || formatter.flags == null) {
                                    compiledPattern.add(new Token(Format.FORMAT_OUTPUT, new RubyTimeOutputFormatter("_", formatter.width)));
                                    break;
                                }
                                // fall through
                            case 'Q':
                            case '+':
                                compiledPattern.add(new Token(Format.FORMAT_OUTPUT, formatter));
                        }
                        formatter = null;
                    }

                    switch (c) {
                    case 'c':
                        addToPattern(compiledPattern, "a b e H:M:S Y");
                        break;
                    case 'D':
                    case 'x':
                        addToPattern(compiledPattern, "m/d/y");
                        break;
                    case 'F':
                        addToPattern(compiledPattern, "Y-m-d");
                        break;
                    case 'n':
                        compiledPattern.add(Token.str("\n"));
                        break;
                    case 'Q':
                        if (dateLibrary) {
                            compiledPattern.add(new Token(Format.FORMAT_MICROSEC_EPOCH));
                        } else {
                            compiledPattern.add(Token.str("%Q"));
                        }
                        break;
                    case 'R':
                        addToPattern(compiledPattern, "H:M");
                        break;
                    case 'r':
                        addToPattern(compiledPattern, "I:M:S p");
                        break;
                    case 'T':
                    case 'X':
                        addToPattern(compiledPattern, "H:M:S");
                        break;
                    case 't':
                        compiledPattern.add(Token.str("\t"));
                        break;
                    case 'v':
                        addToPattern(compiledPattern, "e-");
                        if (!dateLibrary)
                            compiledPattern.add(Token.formatter(new RubyTimeOutputFormatter("^", 0)));
                        addToPattern(compiledPattern, "b-Y");
                        break;
                    case 'Z':
                        if (dateLibrary) {
                            // +HH:MM in 'date', never zone name
                            compiledPattern.add(Token.zoneOffsetColons(1));
                        } else {
                            compiledPattern.add(new Token(Format.FORMAT_ZONE_ID));
                        }
                        break;
                    case '+':
                        if (!dateLibrary) {
                            compiledPattern.add(Token.str("%+"));
                            break;
                        }
                        addToPattern(compiledPattern, "a b e H:M:S ");
                        // %Z: +HH:MM in 'date', never zone name
                        compiledPattern.add(Token.zoneOffsetColons(1));
                        addToPattern(compiledPattern, " Y");
                        break;
                    default:
                        throw new AssertionError("Unknown special char: " + c);
                    }
                }
            }

            // if formatter is still set we didn't use it, add it as is
            if (formatter != null) {
                compiledPattern.add(new Token(Format.FORMAT_OUTPUT, formatter));
            }
        } catch (IOException e) {
            throw new AssertionError(e); // IOException never happens
        }

        return compiledPattern;
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
    public RubyString compileAndFormat(RubyString pattern, boolean dateLibrary, DateTime dt, long nsec, RubyNumeric sub_millis) {
        RubyString out = format(compilePattern(pattern, dateLibrary), dt, nsec, sub_millis);
        out.setEncoding(pattern.getEncoding());
        if (pattern.isTaint()) out.setTaint(true);
        return out;
    }

    public RubyString format(List<Token> compiledPattern, DateTime dt, long nsec, RubyNumeric sub_millis) {
        return runtime.newString(formatToByteList(compiledPattern, dt, nsec, sub_millis));
    }

    private ByteList formatToByteList(List<Token> compiledPattern, DateTime dt, long nsec, RubyNumeric sub_millis) {
        RubyTimeOutputFormatter formatter = RubyTimeOutputFormatter.DEFAULT_FORMATTER;
        final ByteList toAppendTo = new ByteList(24);

        for (Token token: compiledPattern) {
            CharSequence output = null;
            long value = 0;
            FieldType type = TEXT;
            Format format = token.getFormat();

            switch (format) {
                case FORMAT_ENCODING:
                    toAppendTo.setEncoding((Encoding) token.getData());
                    continue; // go to next token
                case FORMAT_OUTPUT:
                    formatter = (RubyTimeOutputFormatter) token.getData();
                    continue; // go to next token
                case FORMAT_STRING:
                    output = token.getData().toString();
                    break;
                case FORMAT_WEEK_LONG:
                    // This is GROSS, but Java API's aren't ISO 8601 compliant at all
                    int v = (dt.getDayOfWeek() + 1) % 8;
                    output = FORMAT_WEEKDAYS[v == 0 ? 1 : v];
                    break;
                case FORMAT_WEEK_SHORT:
                    // This is GROSS, but Java API's aren't ISO 8601 compliant at all
                    v = (dt.getDayOfWeek() + 1) % 8;
                    output = FORMAT_SHORT_WEEKDAYS[v == 0 ? 1 : v];
                    break;
                case FORMAT_MONTH_LONG:
                    output = FORMAT_MONTHS[dt.getMonthOfYear() - 1];
                    break;
                case FORMAT_MONTH_SHORT:
                    output = FORMAT_SHORT_MONTHS[dt.getMonthOfYear() - 1];
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
                    output = dt.getHourOfDay() < 12 ? "AM" : "PM";
                    break;
                case FORMAT_MERIDIAN_LOWER_CASE:
                    output = dt.getHourOfDay() < 12 ? "am" : "pm";
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
                    output = formatZone(colons, (int) value, formatter);
                    break;
                case FORMAT_ZONE_ID:
                    output = RubyTime.getRubyTimeZoneName(runtime, dt);
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

                    output = RubyTimeOutputFormatter.formatNumber(dt.getMillisOfSecond(), 3, '0');
                    if (width > 3) {
                        StringBuilder buff = new StringBuilder(output.length() + 6).append(output);
                        if (sub_millis == null) { // Time
                            buff.append(RubyTimeOutputFormatter.formatNumber(nsec, 6, '0'));
                        } else { // Date, DateTime
                            formatSubMillisGt3(runtime, buff, width, sub_millis);
                        }
                        output = buff;
                    }

                    if (width < output.length()) {
                        output = output.subSequence(0, width);
                    } else {
                        StringBuilder buff = new StringBuilder(width).append(output);
                        // Not enough precision, fill with 0
                        while (buff.length() < width) buff.append('0');
                        output = buff;
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

            final String formatted;
            try {
                formatted = formatter.format(output, value, type);
            } catch (IndexOutOfBoundsException ioobe) {
                throw runtime.newErrnoFromErrno(Errno.ERANGE, "strftime");
            }

            // reset formatter
            formatter = RubyTimeOutputFormatter.DEFAULT_FORMATTER;

            toAppendTo.append(formatted.getBytes(runtime.getEncodingService().charsetForEncoding(toAppendTo.getEncoding())));
        }

        return toAppendTo;
    }

    private static void formatSubMillisGt3(final Ruby runtime, final StringBuilder buff,
                                           final int width, RubyNumeric sub_millis) {
        final int prec = width - 3;
        final ThreadContext context = runtime.getCurrentContext();
        RubyNumeric power = (RubyNumeric) runtime.newFixnum(10).op_pow(context, prec);
        RubyNumeric truncated = (RubyNumeric) sub_millis.numerator(context).
                convertToInteger().op_mul(context, power);
        truncated = (RubyNumeric) truncated.idiv(context, sub_millis.denominator(context));
        long decimals = truncated.convertToInteger().getLongValue();
        buff.append(RubyTimeOutputFormatter.formatNumber(decimals, prec, '0'));
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

    private static StringBuilder formatZone(int colons, int value, RubyTimeOutputFormatter formatter) {
        int seconds = Math.abs(value);
        int hours = seconds / 3600;
        seconds %= 3600;
        int minutes = seconds / 60;
        seconds %= 60;

        if (value < 0 && hours != 0) { // see below when hours == 0
            hours = -hours;
        }

        CharSequence mm = RubyTimeOutputFormatter.formatNumber(minutes, 2, '0');
        CharSequence ss = RubyTimeOutputFormatter.formatNumber(seconds, 2, '0');

        char padder = formatter.getPadder('0');
        int defaultWidth = -1;
        CharSequence after = null;

        switch (colons) {
            case 0: // %z -> +hhmm
                defaultWidth = 5;
                after = mm;
                break;
            case 1: // %:z -> +hh:mm
                defaultWidth = 6;
                after = new StringBuilder(mm.length() + 1).append(':').append(mm);
                break;
            case 2: // %::z -> +hh:mm:ss
                defaultWidth = 9;
                after = new StringBuilder(mm.length() + ss.length() + 2).append(':').append(mm).append(':').append(ss);
                break;
            case 3: // %:::z -> +hh[:mm[:ss]]
                StringBuilder sb = new StringBuilder(mm.length() + ss.length() + 2);
                if (minutes != 0 || seconds != 0) sb.append(':').append(mm);
                if (seconds != 0) sb.append(':').append(ss);
                after = sb;
                defaultWidth = after.length() + 3;
                break;
        }

        int minWidth = defaultWidth - 1;
        int width = formatter.getWidth(defaultWidth);
        if (width < minWidth) {
            width = minWidth;
        }
        width -= after.length();
        StringBuilder before = RubyTimeOutputFormatter.formatSignedNumber(hours, width, padder);

        if (value < 0 && hours == 0) { // the formatter could not handle this case
            for (int i=0; i<before.length(); i++) { // replace('+', '-')
                if (before.charAt(i) == '+') before.setCharAt(i, '-');
            }
        }
        return new StringBuilder(before.length() + after.length()).append(before).append(after); // before + after
    }

    /**
     * @see DateFormat#parse(String, ParsePosition)
     */
    public Date parse(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

}
