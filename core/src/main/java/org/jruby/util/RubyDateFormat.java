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

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.chrono.GJChronology;
import org.joda.time.chrono.JulianChronology;

import static org.jruby.util.RubyDateFormat.FieldType.*;
import static org.jruby.util.RubyDateFormat.Format.*;

public class RubyDateFormat {
    private boolean ruby_1_9;

    private final DateFormatSymbols formatSymbols;

    static enum Format {
        /** raw string, no formatting */
        FORMAT_STRING,
        /** formatter */
        FORMAT_OUTPUT,

        /** %A */
        FORMAT_WEEK_LONG,
        /** %a */
        FORMAT_WEEK_SHORT,
        /** %B */
        FORMAT_MONTH_LONG,
        /** %b, %h */
        FORMAT_MONTH_SHORT,
        /** %C */
        FORMAT_CENTURY,
        /** %d */
        FORMAT_DAY,
        /** %e */
        FORMAT_DAY_S,
        /** %G */
        FORMAT_WEEKYEAR,
        /** %g */
        FORMAT_WEEKYEAR_SHORT,
        /** %H */
        FORMAT_HOUR,
        /** %I */
        FORMAT_HOUR_M,
        /** %j */
        FORMAT_DAY_YEAR,
        /** %k */
        FORMAT_HOUR_BLANK,
        /** %L */
        FORMAT_MILLISEC,
        /** %l */
        FORMAT_HOUR_S,
        /** %M */
        FORMAT_MINUTES,
        /** %m */
        FORMAT_MONTH,
        /** %N */
        FORMAT_NANOSEC,
        /** %p */
        FORMAT_MERIDIAN,
        /** %P */
        FORMAT_MERIDIAN_LOWER_CASE,
        /** %S */
        FORMAT_SECONDS,
        /** %s */
        FORMAT_EPOCH,
        /** %U */
        FORMAT_WEEK_YEAR_S,
        /** %u */
        FORMAT_DAY_WEEK2,
        /** %V */
        FORMAT_WEEK_WEEKYEAR,
        /** %W */
        FORMAT_WEEK_YEAR_M,
        /** %w */
        FORMAT_DAY_WEEK,
        /** %Y */
        FORMAT_YEAR_LONG,
        /** %y */
        FORMAT_YEAR_SHORT,
        /** %Z */
        FORMAT_ZONE_ID,
        /** %z, %:z, %::z, %:::z */
        FORMAT_COLON_ZONE_OFF,

        /* Only for Date/DateTime from here */
        /** %Q */
        FORMAT_MICROSEC_EPOCH,
    }

    public static class Token {
        private final Format format;
        private final Object data;
        
        public Token(Format format) {
            this(format, null);
        }

        public Token(Format formatString, Object data) {
            this.format = formatString;
            this.data = data;
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
    }

    /**
     * Constructor for RubyDateFormat.
     */
    public RubyDateFormat(boolean ruby19) {
        super();
        this.formatSymbols = new DateFormatSymbols(Locale.US);
        this.ruby_1_9 = ruby19;
    }

    public List<Token> compilePattern(ByteList pattern, boolean dateLibrary) {
        List<Token> compiledPattern = new LinkedList<Token>();

        int len = pattern.length();
        boolean ignoredModifier = false;
        int next;
        for (int i = 0; i < len;) {
            if (pattern.get(i) == '%' || (ignoredModifier && !(ignoredModifier = false))) {
                i++;

                if (i == len) {
                    compiledPattern.add(new Token(FORMAT_STRING, "%"));
                } else {
                    i = addOutputFormatter(compiledPattern, pattern, i);

                    switch (pattern.get(i)) {
                    case 'A' :
                        compiledPattern.add(new Token(FORMAT_WEEK_LONG));
                        break;
                    case 'a' :
                        compiledPattern.add(new Token(FORMAT_WEEK_SHORT));
                        break;
                    case 'B' :
                        compiledPattern.add(new Token(FORMAT_MONTH_LONG));
                        break;
                    case 'b' :
                    case 'h' :
                        compiledPattern.add(new Token(FORMAT_MONTH_SHORT));
                        break;
                    case 'C' :
                        compiledPattern.add(new Token(FORMAT_CENTURY));
                        break;
                    case 'c' :
                        compiledPattern.add(new Token(FORMAT_WEEK_SHORT));
                        compiledPattern.add(new Token(FORMAT_STRING, " "));
                        compiledPattern.add(new Token(FORMAT_MONTH_SHORT));
                        compiledPattern.add(new Token(FORMAT_STRING, " "));
                        compiledPattern.add(new Token(FORMAT_DAY_S));
                        compiledPattern.add(new Token(FORMAT_STRING, " "));
                        compiledPattern.add(new Token(FORMAT_HOUR));
                        compiledPattern.add(new Token(FORMAT_STRING, ":"));
                        compiledPattern.add(new Token(FORMAT_MINUTES));
                        compiledPattern.add(new Token(FORMAT_STRING, ":"));
                        compiledPattern.add(new Token(FORMAT_SECONDS));
                        compiledPattern.add(new Token(FORMAT_STRING, " "));
                        compiledPattern.add(new Token(FORMAT_YEAR_LONG));
                        break;
                    case 'D':
                        compiledPattern.add(new Token(FORMAT_MONTH));
                        compiledPattern.add(new Token(FORMAT_STRING, "/"));
                        compiledPattern.add(new Token(FORMAT_DAY));
                        compiledPattern.add(new Token(FORMAT_STRING, "/"));
                        compiledPattern.add(new Token(FORMAT_YEAR_SHORT));
                        break;
                    case 'd':
                        compiledPattern.add(new Token(FORMAT_DAY));
                        break;
                    case 'E':
                        next = '\0';
                        if (i + 1 < len)
                            next = pattern.get(i+1);
                        switch (next) {
                            case 'c': case 'C': case 'x': case 'X': case 'y': case 'Y':
                                ignoredModifier = true;
                                i--;
                                break;
                            default:
                                compiledPattern.add(new Token(FORMAT_STRING, "%E"));
                                break;
                        }
                        break;
                    case 'e':
                        compiledPattern.add(new Token(FORMAT_DAY_S));
                        break;
                    case 'F':
                        compiledPattern.add(new Token(FORMAT_YEAR_LONG));
                        compiledPattern.add(new Token(FORMAT_STRING, "-"));
                        compiledPattern.add(new Token(FORMAT_MONTH));
                        compiledPattern.add(new Token(FORMAT_STRING, "-"));
                        compiledPattern.add(new Token(FORMAT_DAY));
                        break;
                    case 'G':
                        compiledPattern.add(new Token(FORMAT_WEEKYEAR));
                        break;
                    case 'g':
                        compiledPattern.add(new Token(FORMAT_WEEKYEAR_SHORT));
                        break;
                    case 'H':
                        compiledPattern.add(new Token(FORMAT_HOUR));
                        break;
                    case 'I':
                        compiledPattern.add(new Token(FORMAT_HOUR_M));
                        break;
                    case 'j':
                        compiledPattern.add(new Token(FORMAT_DAY_YEAR));
                        break;
                    case 'k':
                        compiledPattern.add(new Token(FORMAT_HOUR_BLANK));
                        break;
                    case 'L':
                        compiledPattern.add(new Token(FORMAT_MILLISEC));
                        break;
                    case 'l':
                        compiledPattern.add(new Token(FORMAT_HOUR_S));
                        break;
                    case 'M':
                        compiledPattern.add(new Token(FORMAT_MINUTES));
                        break;
                    case 'm':
                        compiledPattern.add(new Token(FORMAT_MONTH));
                        break;
                    case 'N':
                        compiledPattern.add(new Token(FORMAT_NANOSEC));
                        break;
                    case 'n':
                        compiledPattern.add(new Token(FORMAT_STRING, "\n"));
                        break;
                    case 'O':
                        next = '\0';
                        if (i + 1 < len)
                            next = pattern.get(i+1);
                        switch (next) {
                            case 'd': case 'e': case 'H': case 'k': case 'I': case 'l': case 'm':
                            case 'M': case 'S': case 'u': case 'U': case 'V': case 'w': case 'W':
                            case 'y':
                                ignoredModifier = true;
                                i--;
                                break;
                            default:
                                compiledPattern.add(new Token(FORMAT_STRING, "%O"));
                                break;
                        }
                        break;
                    case 'p':
                        compiledPattern.add(new Token(FORMAT_MERIDIAN));
                        break;
                    case 'P':
                        compiledPattern.add(new Token(FORMAT_MERIDIAN_LOWER_CASE));
                        break;
                    case 'Q':
                        if (dateLibrary)
                            compiledPattern.add(new Token(FORMAT_MICROSEC_EPOCH));
                        else
                            compiledPattern.add(new Token(FORMAT_STRING, "%Q"));
                        break;
                    case 'R':
                        compiledPattern.add(new Token(FORMAT_HOUR));
                        compiledPattern.add(new Token(FORMAT_STRING, ":"));
                        compiledPattern.add(new Token(FORMAT_MINUTES));
                        break;
                    case 'r':
                        compiledPattern.add(new Token(FORMAT_HOUR_M));
                        compiledPattern.add(new Token(FORMAT_STRING, ":"));
                        compiledPattern.add(new Token(FORMAT_MINUTES));
                        compiledPattern.add(new Token(FORMAT_STRING, ":"));
                        compiledPattern.add(new Token(FORMAT_SECONDS));
                        compiledPattern.add(new Token(FORMAT_STRING, " "));
                        compiledPattern.add(new Token(FORMAT_MERIDIAN));
                        break;
                    case 's':
                        compiledPattern.add(new Token(FORMAT_EPOCH));
                        break;
                    case 'S':
                        compiledPattern.add(new Token(FORMAT_SECONDS));
                        break;
                    case 'T':
                        compiledPattern.add(new Token(FORMAT_HOUR));
                        compiledPattern.add(new Token(FORMAT_STRING, ":"));
                        compiledPattern.add(new Token(FORMAT_MINUTES));
                        compiledPattern.add(new Token(FORMAT_STRING, ":"));
                        compiledPattern.add(new Token(FORMAT_SECONDS));
                        break;
                    case 't':
                        compiledPattern.add(new Token(FORMAT_STRING,"\t"));
                        break;
                    case 'u':
                        compiledPattern.add(new Token(FORMAT_DAY_WEEK2));
                        break;
                    case 'U':
                        compiledPattern.add(new Token(FORMAT_WEEK_YEAR_S));
                        break;
                    case 'v':
                        compiledPattern.add(new Token(FORMAT_DAY_S));
                        compiledPattern.add(new Token(FORMAT_STRING, "-"));
                        if (!dateLibrary)
                            compiledPattern.add(new Token(FORMAT_OUTPUT, new TimeOutputFormatter(ByteList.create("^"), 0)));
                        compiledPattern.add(new Token(FORMAT_MONTH_SHORT));
                        compiledPattern.add(new Token(FORMAT_STRING, "-"));
                        compiledPattern.add(new Token(FORMAT_YEAR_LONG));
                        break;
                    case 'V':
                        compiledPattern.add(new Token(FORMAT_WEEK_WEEKYEAR));
                        break;
                    case 'W':
                        compiledPattern.add(new Token(FORMAT_WEEK_YEAR_M));
                        break;
                    case 'w':
                        compiledPattern.add(new Token(FORMAT_DAY_WEEK));
                        break;
                    case 'X':
                        compiledPattern.add(new Token(FORMAT_HOUR));
                        compiledPattern.add(new Token(FORMAT_STRING, ":"));
                        compiledPattern.add(new Token(FORMAT_MINUTES));
                        compiledPattern.add(new Token(FORMAT_STRING, ":"));
                        compiledPattern.add(new Token(FORMAT_SECONDS));
                        break;
                    case 'x':
                        compiledPattern.add(new Token(FORMAT_MONTH));
                        compiledPattern.add(new Token(FORMAT_STRING, "/"));
                        compiledPattern.add(new Token(FORMAT_DAY));
                        compiledPattern.add(new Token(FORMAT_STRING, "/"));
                        compiledPattern.add(new Token(FORMAT_YEAR_SHORT));
                        break;
                    case 'Y':
                        compiledPattern.add(new Token(FORMAT_YEAR_LONG));
                        break;
                    case 'y':
                        compiledPattern.add(new Token(FORMAT_YEAR_SHORT));
                        break;
                    case 'Z':
                        if (dateLibrary) {
                            // +HH:MM in 'date', never zone name
                            compiledPattern.add(new Token(FORMAT_OUTPUT, new TimeOutputFormatter(ByteList.create(":"), 0)));
                            compiledPattern.add(new Token(FORMAT_COLON_ZONE_OFF));
                        } else {
                            compiledPattern.add(new Token(FORMAT_ZONE_ID));
                        }
                        break;
                    case 'z':
                        compiledPattern.add(new Token(FORMAT_COLON_ZONE_OFF));
                        break;
                    case '+':
                        if (!dateLibrary) {
                            compiledPattern.add(new Token(FORMAT_STRING, "%+"));
                            break;
                        }
                        // %a %b %e %H:%M:%S %Z %Y
                        compiledPattern.add(new Token(FORMAT_WEEK_SHORT));
                        compiledPattern.add(new Token(FORMAT_STRING, " "));
                        compiledPattern.add(new Token(FORMAT_MONTH_SHORT));
                        compiledPattern.add(new Token(FORMAT_STRING, " "));
                        compiledPattern.add(new Token(FORMAT_DAY_S));
                        compiledPattern.add(new Token(FORMAT_STRING, " "));
                        compiledPattern.add(new Token(FORMAT_HOUR));
                        compiledPattern.add(new Token(FORMAT_STRING, ":"));
                        compiledPattern.add(new Token(FORMAT_MINUTES));
                        compiledPattern.add(new Token(FORMAT_STRING, ":"));
                        compiledPattern.add(new Token(FORMAT_SECONDS));
                        compiledPattern.add(new Token(FORMAT_STRING, " "));
                        // %Z: +HH:MM in 'date', never zone name
                        compiledPattern.add(new Token(FORMAT_OUTPUT, new TimeOutputFormatter(ByteList.create(":"), 0)));
                        compiledPattern.add(new Token(FORMAT_COLON_ZONE_OFF));
                        compiledPattern.add(new Token(FORMAT_STRING, " "));
                        compiledPattern.add(new Token(FORMAT_YEAR_LONG));
                        break;
                    case '%':
                        compiledPattern.add(new Token(FORMAT_STRING, "%"));
                        break;
                    default:
                        compiledPattern.add(new Token(FORMAT_STRING, "%" + pattern.charAt(i)));
                    }
                    i++;
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (;i < len && pattern.get(i) != '%'; i++) {
                    sb.append(pattern.charAt(i));
                }
                compiledPattern.add(new Token(FORMAT_STRING, sb.toString()));
            }
        }
        return compiledPattern;
    }

    private int addOutputFormatter(List<Token> compiledPattern, ByteList pattern, int index) {
        TimeOutputFormatter outputFormatter = TimeOutputFormatter.getFormatter(pattern, index - 1);
        if (outputFormatter != null) {
            index += outputFormatter.getFormat().length();
            compiledPattern.add(new Token(FORMAT_OUTPUT, outputFormatter));
        }
        return index;
    }

    static enum FieldType {
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

    /** Convenience method when using no pattern caching */
    public ByteList compileAndFormat(ByteList pattern, boolean dateLibrary, DateTime dt, long nsec) {
        return format(compilePattern(pattern, dateLibrary), dt, nsec);
    }

    public ByteList format(List<Token> compiledPattern, DateTime dt, long nsec) {
        TimeOutputFormatter formatter = TimeOutputFormatter.DEFAULT_FORMATTER;
        ByteList toAppendTo = new ByteList();

        for (Token token: compiledPattern) {
            String output = null;
            long value = 0;
            FieldType type = TEXT;
            Format format = token.getFormat();

            switch (format) {
                case FORMAT_OUTPUT:
                    formatter = (TimeOutputFormatter) token.getData();
                    continue; // go to next token
                case FORMAT_STRING:
                    output = token.getData().toString();
                    break;
                case FORMAT_WEEK_LONG:
                    // This is GROSS, but Java API's aren't ISO 8601 compliant at all
                    int v = (dt.getDayOfWeek()+1)%8;
                    if(v == 0) {
                        v++;
                    }
                    output = formatSymbols.getWeekdays()[v];
                    break;
                case FORMAT_WEEK_SHORT:
                    // This is GROSS, but Java API's aren't ISO 8601 compliant at all
                    v = (dt.getDayOfWeek()+1)%8;
                    if(v == 0) {
                        v++;
                    }
                    output = formatSymbols.getShortWeekdays()[v];
                    break;
                case FORMAT_MONTH_LONG:
                    output = formatSymbols.getMonths()[dt.getMonthOfYear()-1];
                    break;
                case FORMAT_MONTH_SHORT:
                    output = formatSymbols.getShortMonths()[dt.getMonthOfYear()-1];
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

                    type = (format == FORMAT_HOUR_M) ? NUMERIC2 : NUMERIC2BLANK;
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
                    int colons = formatter.getNumberOfColons();
                    output = formatZone(colons, (int) value, formatter);
                    break;
                case FORMAT_ZONE_ID:
                    output = dt.getZone().getShortName(dt.getMillis());
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
                    value = dt.getMillisOfSecond() * 1000000;
                    if (ruby_1_9) value += nsec;
                    output = TimeOutputFormatter.formatNumber(value, 9, '0');

                    int defaultWidth = (format == FORMAT_NANOSEC) ? 9 : 3;
                    int width = formatter.getWidth(defaultWidth);
                    if (width < 9) {
                        output = output.substring(0, width);
                    } else {
                        while(output.length() < width)
                            output += "0";
                    }
                    formatter = TimeOutputFormatter.DEFAULT_FORMATTER; // no more formatting
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
            }

            output = formatter.format(output, value, type);
            // reset formatter
            formatter = TimeOutputFormatter.DEFAULT_FORMATTER;

            for (int i = 0; i < output.length(); i++) {
                toAppendTo.append(output.charAt(i));
            }
        }

        return toAppendTo;
    }

    /**
     * Ruby always follows Astronomical year numbering,
     * that is BC x is -x+1 and there is a year 0 (BC 1)
     * but Joda-time returns -x for year x BC in Julian chronology (no year 0) */
    private int year(DateTime dt, int year) {
        Chronology c;
        if (year < 0 && (
                (c = dt.getChronology()) instanceof JulianChronology ||
                (c instanceof GJChronology && ((GJChronology) c).getGregorianCutover().isAfter(dt))))
            return year + 1;
        return year;
    }

    private int formatWeekYear(DateTime dt, int firstDayOfWeek) {
        java.util.Calendar dtCalendar = dt.toGregorianCalendar();
        dtCalendar.setFirstDayOfWeek(firstDayOfWeek);
        dtCalendar.setMinimalDaysInFirstWeek(7);
        int value = dtCalendar.get(java.util.Calendar.WEEK_OF_YEAR);
        if ((value == 52 || value == 53) &&
                (dtCalendar.get(Calendar.MONTH) == Calendar.JANUARY )) {
            // MRI behavior: Week values are monotonous.
            // So, weeks that effectively belong to previous year,
            // will get the value of 0, not 52 or 53, as in Java.
            value = 0;
        }
        return value;
    }

    private String formatZone(int colons, int value, TimeOutputFormatter formatter) {
        int seconds = Math.abs(value);
        int hours = seconds / 3600;
        seconds %= 3600;
        int minutes = seconds / 60;
        seconds %= 60;

        if (value < 0 && hours != 0) { // see below when hours == 0
            hours = -hours;
        }

        String mm = TimeOutputFormatter.formatNumber(minutes, 2, '0');
        String ss = TimeOutputFormatter.formatNumber(seconds, 2, '0');

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
                if (minutes == 0) {
                    if (seconds == 0) { // +hh
                        defaultWidth = 3;
                        after = "";
                    } else { // +hh:mm
                        return formatZone(1, value, formatter);
                    }
                } else { // +hh:mm:ss
                    return formatZone(2, value, formatter);
                }
                break;
        }

        int minWidth = defaultWidth - 1;
        int width = formatter.getWidth(defaultWidth);
        if (width < minWidth) {
            width = minWidth;
        }
        width -= after.length();
        String before = TimeOutputFormatter.formatSignedNumber(hours, width, padder);

        if (value < 0 && hours == 0) // the formatter could not handle this case
            before = before.replace('+', '-');
        return before + after;
    }

    /**
     * @see DateFormat#parse(String, ParsePosition)
     */
    public Date parse(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }
}
