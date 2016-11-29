/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
package org.jruby.truffle.core.time;

import com.oracle.truffle.api.nodes.Node;
import jnr.constants.platform.Errno;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.joda.time.DateTime;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.encoding.EncodingManager;
import org.jruby.truffle.debug.DebugHelpers;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.util.ByteList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormatSymbols;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jruby.truffle.core.time.RubyDateFormatter.FieldType.NUMERIC;
import static org.jruby.truffle.core.time.RubyDateFormatter.FieldType.NUMERIC2;
import static org.jruby.truffle.core.time.RubyDateFormatter.FieldType.NUMERIC2BLANK;
import static org.jruby.truffle.core.time.RubyDateFormatter.FieldType.NUMERIC3;
import static org.jruby.truffle.core.time.RubyDateFormatter.FieldType.NUMERIC4;
import static org.jruby.truffle.core.time.RubyDateFormatter.FieldType.NUMERIC5;
import static org.jruby.truffle.core.time.RubyDateFormatter.FieldType.TEXT;

public class RubyDateFormatter {
    private static final DateFormatSymbols FORMAT_SYMBOLS = new DateFormatSymbols(Locale.US);
    private static final Token[] CONVERSION2TOKEN = new Token[256];

    private RubyContext context;
    private Node currentNode;
    private StrftimeLexer lexer;

    static enum Format {
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
            CONVERSION2TOKEN[conversion] = new Token(this);
        }
        Format(char conversion, char alias) {
            this(conversion);
            CONVERSION2TOKEN[alias] = CONVERSION2TOKEN[conversion];
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
            return CONVERSION2TOKEN[c];
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
    public RubyDateFormatter(RubyContext context, Node currentNode) {
        super();
        this.context = context;
        this.currentNode = currentNode;
        lexer = new StrftimeLexer((Reader) null);
    }

    private void addToPattern(List<Token> compiledPattern, String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                compiledPattern.add(Token.format(c));
            } else {
                compiledPattern.add(Token.str(Character.toString(c)));
            }
        }
    }

    public List<Token> compilePattern(ByteList pattern, boolean dateLibrary) {
        List<Token> compiledPattern = new LinkedList<>();

        Encoding enc = pattern.getEncoding();
        if (!enc.isAsciiCompatible()) {
            throw new RaiseException(context.getCoreExceptions().argumentError("format should have ASCII compatible encoding", currentNode));
        }
        if (enc != ASCIIEncoding.INSTANCE) { // default for ByteList
            compiledPattern.add(new Token(Format.FORMAT_ENCODING, enc));
        }

        ByteArrayInputStream in = new ByteArrayInputStream(pattern.getUnsafeBytes(), pattern.getBegin(), pattern.getRealSize());
        Reader reader = new InputStreamReader(in, EncodingManager.charsetForEncoding(pattern.getEncoding()));
        lexer.yyreset(reader);

        Token token;
        try {
            while ((token = lexer.yylex()) != null) {
                if (token.format != Format.FORMAT_SPECIAL) {
                    compiledPattern.add(token);
                } else {
                    char c = (Character) token.data;
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
                        throw new Error("Unknown special char: "+c);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

        char defaultPadder;
        int defaultWidth;
        FieldType(char padder, int width) {
            defaultPadder = padder;
            defaultWidth = width;
        }
    }

    public ByteList formatToByteList(List<Token> compiledPattern, ZonedDateTime dt) {
        RubyTimeOutputFormatter formatter = RubyTimeOutputFormatter.DEFAULT_FORMATTER;
        ByteList toAppendTo = new ByteList();

        for (Token token: compiledPattern) {
            String output = null;
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
                    int v = (dt.getDayOfWeek().getValue()+1)%8;
                    if(v == 0) {
                        v++;
                    }
                    output = FORMAT_SYMBOLS.getWeekdays()[v];
                    break;
                case FORMAT_WEEK_SHORT:
                    // This is GROSS, but Java API's aren't ISO 8601 compliant at all
                    v = (dt.getDayOfWeek().getValue()+1)%8;
                    if(v == 0) {
                        v++;
                    }
                    output = FORMAT_SYMBOLS.getShortWeekdays()[v];
                    break;
                case FORMAT_MONTH_LONG:
                    output = FORMAT_SYMBOLS.getMonths()[dt.getMonthValue()-1];
                    break;
                case FORMAT_MONTH_SHORT:
                    output = FORMAT_SYMBOLS.getShortMonths()[dt.getMonthValue()-1];
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
                    value = dt.getHour();
                    break;
                case FORMAT_HOUR_BLANK:
                    type = NUMERIC2BLANK;
                    value = dt.getHour();
                    break;
                case FORMAT_HOUR_M:
                case FORMAT_HOUR_S:
                    value = dt.getHour();
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
                    value = dt.getMinute();
                    break;
                case FORMAT_MONTH:
                    type = NUMERIC2;
                    value = dt.getMonthValue();
                    break;
                case FORMAT_MERIDIAN:
                    output = dt.getHour() < 12 ? "AM" : "PM";
                    break;
                case FORMAT_MERIDIAN_LOWER_CASE:
                    output = dt.getHour() < 12 ? "am" : "pm";
                    break;
                case FORMAT_SECONDS:
                    type = NUMERIC2;
                    value = dt.getSecond();
                    break;
                case FORMAT_WEEK_YEAR_M:
                    type = NUMERIC2;
                    value = formatWeekYear(dt, Calendar.MONDAY);
                    break;
                case FORMAT_WEEK_YEAR_S:
                    type = NUMERIC2;
                    value = formatWeekYear(dt, Calendar.SUNDAY);
                    break;
                case FORMAT_DAY_WEEK:
                    type = NUMERIC;
                    value = dt.getDayOfWeek().getValue() % 7;
                    break;
                case FORMAT_DAY_WEEK2:
                    type = NUMERIC;
                    value = dt.getDayOfWeek().getValue();
                    break;
                case FORMAT_YEAR_LONG:
                    value = dt.getYear();
                    type = (value >= 0) ? NUMERIC4 : NUMERIC5;
                    break;
                case FORMAT_YEAR_SHORT:
                    type = NUMERIC2;
                    value = dt.getYear() % 100;
                    break;
                case FORMAT_COLON_ZONE_OFF:
                    // custom logic because this is so weird
                    value = dt.getOffset().getTotalSeconds();
                    int colons = (Integer) token.getData();
                    output = formatZone(colons, (int) value, formatter);
                    break;
                case FORMAT_ZONE_ID:
                    output = getRubyTimeZoneName(dt);
                    break;
                case FORMAT_CENTURY:
                    type = NUMERIC;
                    value = dt.getYear() / 100;
                    break;
                case FORMAT_EPOCH:
                    type = NUMERIC;
                    value = dt.toInstant().getEpochSecond();
                    break;
                case FORMAT_WEEK_WEEKYEAR:
                    type = NUMERIC2;
                    value = dt.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
                    break;
                case FORMAT_MILLISEC:
                case FORMAT_NANOSEC:
                    int defaultWidth = (format == Format.FORMAT_NANOSEC) ? 9 : 3;
                    int width = formatter.getWidth(defaultWidth);

                    output = RubyTimeOutputFormatter.formatNumber(dt.getNano(), 9, '0');

                    if (width < output.length()) {
                        output = output.substring(0, width);
                    } else {
                        // Not enough precision, fill with 0
                        while(output.length() < width)
                            output += "0";
                    }
                    formatter = RubyTimeOutputFormatter.DEFAULT_FORMATTER; // no more formatting
                    break;
                case FORMAT_WEEKYEAR:
                    value = new DateTime(dt.toInstant().getEpochSecond() * 1_000).getWeekyear();
                    type = (value >= 0) ? NUMERIC4 : NUMERIC5;
                    break;
                case FORMAT_WEEKYEAR_SHORT:
                    type = NUMERIC2;
                    value = new DateTime(dt.toInstant().getEpochSecond() * 1_000).getWeekyear() % 100;
                    break;
                case FORMAT_MICROSEC_EPOCH:
                    // only available for Date
                    type = NUMERIC;
                    final Instant instant = dt.toInstant();
                    value = instant.getEpochSecond() * 1_000 + (instant.getNano() / 1_000_000);
                    break;
                case FORMAT_SPECIAL:
                    throw new Error("FORMAT_SPECIAL is a special token only for the lexer.");
            }

            try {
                output = formatter.format(output, value, type);
            } catch (IndexOutOfBoundsException ioobe) {
                throw new RaiseException(context.getCoreExceptions().errnoError(Errno.ERANGE.intValue(), "strftime", currentNode));
            }

            // reset formatter
            formatter = RubyTimeOutputFormatter.DEFAULT_FORMATTER;

            toAppendTo.append(output.getBytes(EncodingManager.charsetForEncoding(toAppendTo.getEncoding())));
        }

        return toAppendTo;
    }

    private int formatWeekYear(ZonedDateTime dt, int firstDayOfWeek) {
        Calendar dtCalendar = GregorianCalendar.from(dt);
        dtCalendar.setFirstDayOfWeek(firstDayOfWeek);
        dtCalendar.setMinimalDaysInFirstWeek(7);
        int value = dtCalendar.get(Calendar.WEEK_OF_YEAR);
        if ((value == 52 || value == 53) &&
                (dtCalendar.get(Calendar.MONTH) == Calendar.JANUARY )) {
            // MRI behavior: Week values are monotonous.
            // So, weeks that effectively belong to previous year,
            // will get the value of 0, not 52 or 53, as in Java.
            value = 0;
        }
        return value;
    }

    private String formatZone(int colons, int value, RubyTimeOutputFormatter formatter) {
        int seconds = Math.abs(value);
        int hours = seconds / 3600;
        seconds %= 3600;
        int minutes = seconds / 60;
        seconds %= 60;

        if (value < 0 && hours != 0) { // see below when hours == 0
            hours = -hours;
        }

        String mm = RubyTimeOutputFormatter.formatNumber(minutes, 2, '0');
        String ss = RubyTimeOutputFormatter.formatNumber(seconds, 2, '0');

        char padder = formatter.getPadder('0');
        int defaultWidth = -1;
        String after = null;

        switch (colons) {
            case 0: // %z -> +hhmm
                defaultWidth = 5;
                after = mm;
                break;
            case 1: // %:z -> +hh:mm
                defaultWidth = 6;
                after = ":" + mm;
                break;
            case 2: // %::z -> +hh:mm:ss
                defaultWidth = 9;
                after = ":" + mm + ":" + ss;
                break;
            case 3: // %:::z -> +hh[:mm[:ss]]
                StringBuilder sb = new StringBuilder();
                if (minutes != 0 || seconds != 0) sb.append(":").append(mm);
                if (seconds != 0) sb.append(":").append(ss);
                after = sb.toString();
                defaultWidth = after.length() + 3;
                break;
        }

        int minWidth = defaultWidth - 1;
        int width = formatter.getWidth(defaultWidth);
        if (width < minWidth) {
            width = minWidth;
        }
        width -= after.length();
        String before = RubyTimeOutputFormatter.formatSignedNumber(hours, width, padder);

        if (value < 0 && hours == 0) // the formatter could not handle this case
            before = before.replace('+', '-');
        return before + after;
    }

    public String getRubyTimeZoneName(ZonedDateTime dt) {
        return getRubyTimeZoneName(getEnvTimeZone().toString(), dt);
    }

    @SuppressWarnings("deprecation")
    private Object getEnvTimeZone() {
        return DebugHelpers.eval(context, "Time.now.zone");
    }

    /* JRUBY-3560
     * joda-time disallows use of three-letter time zone IDs.
     * Since MRI accepts these values, we need to translate them.
     */
    private static final Map<String, String> LONG_TZNAME = map(
            "MET", "CET", // JRUBY-2759
            "ROC", "Asia/Taipei", // Republic of China
            "WET", "Europe/Lisbon" // Western European Time
    );

    /* Some TZ values need to be overriden for Time#zone
     */
    private static final Map<String, String> SHORT_STD_TZNAME = map(
            "Etc/UCT", "UCT",
            "MET", "MET", // needs to be overriden
            "UCT", "UCT"
    );

    private static final Map<String, String> SHORT_DL_TZNAME = map(
            "Etc/UCT", "UCT",
            "MET", "MEST", // needs to be overriden
            "UCT", "UCT"
    );

    private static final Pattern TIME_OFFSET_PATTERN
            = Pattern.compile("([\\+-])(\\d\\d):(\\d\\d)(?::(\\d\\d))?");

    public static String getRubyTimeZoneName(String envTZ, ZonedDateTime dt) {
        // see declaration of SHORT_TZNAME
        if (SHORT_STD_TZNAME.containsKey(envTZ) && ! dt.getOffset().getRules().isDaylightSavings(dt.toInstant())) {
            return SHORT_STD_TZNAME.get(envTZ);
        }

        if (SHORT_DL_TZNAME.containsKey(envTZ) && dt.getOffset().getRules().isDaylightSavings(dt.toInstant())) {
            return SHORT_DL_TZNAME.get(envTZ);
        }

        String zone = dt.getZone().getId();

        Matcher offsetMatcher = TIME_OFFSET_PATTERN.matcher(zone);

        if (offsetMatcher.matches()) {
            if (zone.equals("+00:00")) {
                zone = "UTC";
            } else {
                // try non-localized time zone name
                zone = dt.getZone().getId();
                if (zone == null) {
                    zone = "";
                }
            }
        }

        return zone;
    }

    public static final Map<String, String> map(String... keyValues) {
        HashMap<String, String> map = new HashMap<>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length;) {
            map.put(keyValues[i++], keyValues[i++]);
        }
        return map;
    }

}
