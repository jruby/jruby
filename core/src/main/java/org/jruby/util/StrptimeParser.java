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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.lexer.StrptimeLexer;

/**
 * This is Java implementation of ext/date/date_strptime.c in Ruby 2.3.1.
 * see https://github.com/ruby/ruby/blob/394fa89c67722d35bdda89f10c7de5c304a5efb1/ext/date/date_strptime.c
 */
public class StrptimeParser {
    // day_names
    private static final String[] DAY_NAMES = new String[] {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
            "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    // month_names
    private static final String[] MONTH_NAMES = new String[] {
            "January", "February", "March", "April", "May", "June", "July", "August", "September",
            "October", "November", "December", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    // merid_names
    private static final String[] MERID_NAMES = new String[] {
            "am", "pm", "a.m.", "p.m."
    };

    /**
     * Ported Date::Format::Bag from lib/ruby/stdlib/date/format.rb in JRuby 9.1.5.0.
     * see https://github.com/jruby/jruby/blob/036ce39f0476d4bd718e23e64caff36bb50b8dbc/lib/ruby/stdlib/date/format.rb
     */
    public static class FormatBag {
        private int mDay = Integer.MIN_VALUE;
        private int wDay = Integer.MIN_VALUE;
        private int cWDay = Integer.MIN_VALUE;
        private int yDay = Integer.MIN_VALUE;
        private int cWeek = Integer.MIN_VALUE;
        private long cWYear = Long.MIN_VALUE;
        private int min = Integer.MIN_VALUE;
        private int mon = Integer.MIN_VALUE;
        private int hour = Integer.MIN_VALUE;
        private long year = Long.MIN_VALUE;
        private int sec = Integer.MIN_VALUE;
        private int wNum0 = Integer.MIN_VALUE;
        private int wNum1 = Integer.MIN_VALUE;

        private String zone = null;

        private BigInteger secFraction = null; // Rational
        private int secFractionSize = Integer.MIN_VALUE;

        private BigInteger seconds = null; // Bignum or Rational
        private int secondsSize = Integer.MIN_VALUE;

        private int merid = Integer.MIN_VALUE;
        private long cent = Long.MIN_VALUE;

        private boolean fail = false;
        private String leftover = null;

        public int getMDay() {
            return mDay;
        }

        public int getWDay() {
            return wDay;
        }

        public int getCWDay() {
            return cWDay;
        }

        public int getYDay() {
            return yDay;
        }

        public int getCWeek() {
            return cWeek;
        }

        public long getCWYear() {
            return cWYear;
        }

        public int getMin() {
            return min;
        }

        public int getMon() {
            return mon;
        }

        public int getHour() {
            return hour;
        }

        public long getYear() {
            return year;
        }

        public int getSec() {
            return sec;
        }

        public int getWNum0() {
            return wNum0;
        }

        public int getWNum1() {
            return wNum1;
        }

        public String getZone() {
            return zone;
        }

        public BigInteger getSecFraction() {
            return secFraction;
        }

        public int getSecFractionSize() {
            return secFractionSize;
        }

        public BigInteger getSeconds() {
            return seconds;
        }

        public int getSecondsSize() {
            return secondsSize;
        }

        public int getMerid() {
            return merid;
        }

        public long getCent() {
            return cent;
        }

        void fail() {
            fail = true;
        }

        public String getLeftover() {
            return leftover;
        }

        public static boolean has(int v) {
            return v != Integer.MIN_VALUE;
        }

        public static boolean has(long v) {
            return v != Long.MIN_VALUE;
        }

        public static boolean has(BigInteger v) {
            return v != null;
        }
    }

    /**
     * Ported from RubyDateFormatter#addToPattern in JRuby 9.1.5.0.
     * see https://github.com/jruby/jruby/blob/036ce39f0476d4bd718e23e64caff36bb50b8dbc/core/src/main/java/org/jruby/util/RubyDateFormatter.java
     */
    private void addToPattern(final List<StrptimeToken> compiledPattern, final String str) {
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                compiledPattern.add(StrptimeToken.format(c));
            } else {
                compiledPattern.add(StrptimeToken.str(Character.toString(c)));
            }
        }
    }

    /**
     * Ported from RubyDateFormatter#compilePattern in JRuby 9.1.5.0.
     * see https://github.com/jruby/jruby/blob/036ce39f0476d4bd718e23e64caff36bb50b8dbc/core/src/main/java/org/jruby/util/RubyDateFormatter.java
     */
    public List<StrptimeToken> compilePattern(final String pattern) {
        final List<StrptimeToken> compiledPattern = new LinkedList<>();
        final Reader reader = new StringReader(pattern); // TODO Use try-with-resource statement
        StrptimeLexer lexer = new StrptimeLexer(reader);

        StrptimeToken token;
        try {
            while ((token = lexer.yylex()) != null) {
                if (token.getFormat() != StrptimeFormat.FORMAT_SPECIAL) {
                    compiledPattern.add(token);
                } else {
                    char c = (Character) token.getData();
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
                            compiledPattern.add(StrptimeToken.str("\n"));
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
                            compiledPattern.add(StrptimeToken.str("\t"));
                            break;
                        case 'v':
                            addToPattern(compiledPattern, "e-b-Y");
                            break;
                        case 'Z':
                            // +HH:MM in 'date', never zone name
                            compiledPattern.add(StrptimeToken.zoneOffsetColons(1));
                            break;
                        case '+':
                            addToPattern(compiledPattern, "a b e H:M:S ");
                            // %Z: +HH:MM in 'date', never zone name
                            compiledPattern.add(StrptimeToken.zoneOffsetColons(1));
                            addToPattern(compiledPattern, " Y");
                            break;
                        default:
                            throw new Error("Unknown special char: " + c);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return compiledPattern;
    }

    public FormatBag parse(final List<StrptimeToken> compiledPattern, final String text) {
        final FormatBag bag = new StringParser(text).parse(compiledPattern);
        if (bag == null) {
            return null;
        }

        if (FormatBag.has(bag.cent)) {
            if (FormatBag.has(bag.cWYear)) {
                bag.cWYear += bag.cent * 100;
            }
            if (FormatBag.has(bag.year)) {
                bag.year += bag.cent * 100;
            }

            // delete bag._cent
            bag.cent = Long.MIN_VALUE;
        }

        if (FormatBag.has(bag.merid)) {
            if (FormatBag.has(bag.hour)) {
                bag.hour %= 12;
                bag.hour += bag.merid;
            }

            // delete bag._merid
            bag.merid = Integer.MIN_VALUE;
        }

        return bag;
    }

    private static class StringParser {
        private static final Pattern ZONE_PARSE_REGEX = Pattern.compile("\\A(" +
                        "(?:gmt|utc?)?[-+]\\d+(?:[,.:]\\d+(?::\\d+)?)?" +
                        "|(?-i:[[\\p{Alpha}].\\s]+)(?:standard|daylight)\\s+time\\b" +
                        "|(?-i:[[\\p{Alpha}]]+)(?:\\s+dst)?\\b" +
                        ")", Pattern.CASE_INSENSITIVE);

        private final String text;
        private final FormatBag bag;

        private int pos;
        private boolean fail;

        private StringParser(String text) {
            this.text = text;
            this.bag = new FormatBag();

            this.pos = 0;
            this.fail = false;
        }

        private FormatBag parse(final List<StrptimeToken> compiledPattern) {
            for (int tokenIndex = 0; tokenIndex < compiledPattern.size(); tokenIndex++) {
                final StrptimeToken token = compiledPattern.get(tokenIndex);

                switch (token.getFormat()) {
                    case FORMAT_STRING: {
                        final String str = token.getData().toString();
                        for (int i = 0; i < str.length(); i++) {
                            final char c = str.charAt(i);
                            if (isSpace(c)) {
                                while (!isEndOfText(text, pos) && isSpace(text.charAt(pos))) {
                                    pos++;
                                }
                            } else {
                                if (isEndOfText(text, pos) || c != text.charAt(pos)) {
                                    fail = true;
                                }
                                pos++;
                            }
                        }
                        break;
                    }
                    case FORMAT_WEEK_LONG: // %A - The full weekday name (``Sunday'')
                    case FORMAT_WEEK_SHORT: { // %a - The abbreviated name (``Sun'')
                        final int dayIndex = findIndexInPatterns(DAY_NAMES);
                        if (dayIndex >= 0) {
                            bag.wDay = dayIndex % 7;
                            pos += DAY_NAMES[dayIndex].length();
                        } else {
                            fail = true;
                        }
                        break;
                    }
                    case FORMAT_MONTH_LONG: // %B - The full month name (``January'')
                    case FORMAT_MONTH_SHORT: { // %b, %h - The abbreviated month name (``Jan'')
                        final int monIndex = findIndexInPatterns(MONTH_NAMES);
                        if (monIndex >= 0) {
                            bag.mon = monIndex % 12 + 1;
                            pos += MONTH_NAMES[monIndex].length();
                        } else {
                            fail = true;
                        }
                        break;
                    }
                    case FORMAT_CENTURY: { // %C - year / 100 (round down.  20 in 2009)
                        if (isNumberPattern(compiledPattern, tokenIndex)) {
                            bag.cent = readDigits(2);
                        } else {
                            bag.cent = readDigitsMaxLong();
                        }
                        break;
                    }
                    case FORMAT_DAY: // %d, %Od - Day of the month, zero-padded (01..31)
                    case FORMAT_DAY_S: { // %e, %Oe - Day of the month, blank-padded ( 1..31)
                        final long day;
                        if (isBlank(text, pos)) {
                            pos += 1; // blank
                            day = readDigits(1);
                        } else {
                            day = readDigits(2);
                        }

                        if (!validRange(day, 1, 31)) {
                            fail = true;
                        }
                        bag.mDay = (int)day;
                        break;
                    }
                    case FORMAT_WEEKYEAR: { // %G - The week-based year
                        if (isNumberPattern(compiledPattern, tokenIndex)) {
                            bag.cWYear = readDigits(4);
                        } else {
                            bag.cWYear = readDigitsMaxLong();
                        }
                        break;
                    }
                    case FORMAT_WEEKYEAR_SHORT: { // %g - The last 2 digits of the week-based year (00..99)
                        final long v = readDigits(2);
                        if (!validRange(v, 0, 99)) {
                            fail = true;
                        }
                        bag.cWYear = v;
                        if (!bag.has(bag.cent)) {
                            bag.cent = v >= 69 ? 19 : 20;
                        }
                        break;
                    }
                    case FORMAT_HOUR: // %H, %OH - Hour of the day, 24-hour clock, zero-padded (00..23)
                    case FORMAT_HOUR_BLANK: { // %k - Hour of the day, 24-hour clock, blank-padded ( 0..23)
                        final long hour;
                        if (isBlank(text, pos)) {
                            pos += 1; // blank
                            hour = readDigits(1);
                        } else {
                            hour = readDigits(2);
                        }

                        if (!validRange(hour, 0, 24)) {
                            fail = true;
                        }
                        bag.hour = (int)hour;
                        break;
                    }
                    case FORMAT_HOUR_M: // %I, %OI - Hour of the day, 12-hour clock, zero-padded (01..12)
                    case FORMAT_HOUR_S: { // %l - Hour of the day, 12-hour clock, blank-padded ( 1..12)
                        final long hour;
                        if (isBlank(text, pos)) {
                            pos += 1; // blank
                            hour = readDigits(1);
                        } else {
                            hour = readDigits(2);
                        }

                        if (!validRange(hour, 1, 12)) {
                            fail = true;
                        }
                        bag.hour = (int)hour;
                        break;
                    }
                    case FORMAT_DAY_YEAR: { // %j - Day of the year (001..366)
                        final long day = readDigits(3);
                        if (!validRange(day, 1, 365)) {
                            fail = true;
                        }
                        bag.yDay = (int)day;
                        break;
                    }
                    case FORMAT_MILLISEC: // %L - Millisecond of the second (000..999)
                    case FORMAT_NANOSEC: { // %N - Fractional seconds digits, default is 9 digits (nanosecond)
                        boolean negative = false;
                        if (isSign(text, pos)) {
                            negative = text.charAt(pos) == '-';
                            pos++;
                        }

                        final BigInteger v;
                        final int initPos = pos;
                        if (isNumberPattern(compiledPattern, tokenIndex)) {
                            if (token.getFormat() == StrptimeFormat.FORMAT_MILLISEC) {
                                v = BigInteger.valueOf(readDigits(3));
                            } else {
                                v = BigInteger.valueOf(readDigits(9));
                            }
                        } else {
                            v = readDigitsMax();
                        }

                        bag.secFraction = !negative ? v : v.negate();
                        bag.secFractionSize = pos - initPos;
                        break;
                    }
                    case FORMAT_MINUTES: { // %M, %OM - Minute of the hour (00..59)
                        final long min = readDigits(2);
                        if (!validRange(min, 0, 59)) {
                            fail = true;
                        }
                        bag.min = (int)min;
                        break;
                    }
                    case FORMAT_MONTH: { // %m, %Om - Month of the year, zero-padded (01..12)
                        final long mon = readDigits(2);
                        if (!validRange(mon, 1, 12)) {
                            fail = true;
                        }
                        bag.mon = (int)mon;
                        break;
                    }
                    case FORMAT_MERIDIAN: // %P - Meridian indicator, lowercase (``am'' or ``pm'')
                    case FORMAT_MERIDIAN_LOWER_CASE: { // %p - Meridian indicator, uppercase (``AM'' or ``PM'')
                        final int meridIndex = findIndexInPatterns(MERID_NAMES);
                        if (meridIndex >= 0) {
                            bag.merid = meridIndex % 2 == 0 ? 0 : 12;
                            pos += MERID_NAMES[meridIndex].length();
                        } else {
                            fail = true;
                        }
                        break;
                    }
                    case FORMAT_MICROSEC_EPOCH: { // %Q - Number of microseconds since 1970-01-01 00:00:00 UTC.
                        boolean negative = false;
                        if (isMinus(text, pos)) {
                            negative = true;
                            pos++;
                        }

                        final BigInteger sec = readDigitsMax();
                        bag.seconds = !negative ? sec : sec.negate();
                        bag.secondsSize = 3;
                        break;
                    }
                    case FORMAT_SECONDS: { // %S - Second of the minute (00..59)
                        final long sec = readDigits(2);
                        if (!validRange(sec, 0, 60)) {
                            fail = true;
                        }
                        bag.sec = (int)sec;
                        break;
                    }
                    case FORMAT_EPOCH: { // %s - Number of seconds since 1970-01-01 00:00:00 UTC.
                        boolean negative = false;
                        if (isMinus(text, pos)) {
                            negative = true;
                            pos++;
                        }

                        final BigInteger sec = readDigitsMax();
                        bag.seconds = !negative ? sec : sec.negate();
                        break;
                    }
                    case FORMAT_WEEK_YEAR_S: // %U, %OU - Week number of the year.  The week starts with Sunday.  (00..53)
                    case FORMAT_WEEK_YEAR_M: { // %W, %OW - Week number of the year.  The week starts with Monday.  (00..53)
                        final long week = readDigits(2);
                        if (!validRange(week, 0, 53)) {
                            fail = true;
                        }

                        if (token.getFormat() == StrptimeFormat.FORMAT_WEEK_YEAR_S) {
                            bag.wNum0 = (int)week;
                        } else {
                            bag.wNum1 = (int)week;
                        }
                        break;
                    }
                    case FORMAT_DAY_WEEK2: { // %u, %Ou - Day of the week (Monday is 1, 1..7)
                        final long day = readDigits(1);
                        if (!validRange(day, 1, 7)) {
                            fail = true;
                        }
                        bag.cWDay = (int)day;
                        break;
                    }
                    case FORMAT_WEEK_WEEKYEAR: { // %V, %OV - Week number of the week-based year (01..53)
                        final long week = readDigits(2);
                        if (!validRange(week, 1, 53)) {
                            fail = true;
                        }
                        bag.cWeek = (int)week;
                        break;
                    }
                    case FORMAT_DAY_WEEK: { // %w - Day of the week (Sunday is 0, 0..6)
                        final long day = readDigits(1);
                        if (!validRange(day, 0, 6)) {
                            fail = true;
                        }
                        bag.wDay = (int)day;
                        break;
                    }
                    case FORMAT_YEAR_LONG: {
                        // %Y, %EY - Year with century (can be negative, 4 digits at least)
                        //           -0001, 0000, 1995, 2009, 14292, etc.
                        boolean negative = false;
                        if (isSign(text, pos)) {
                            negative = text.charAt(pos) == '-';
                            pos++;
                        }

                        final long year;
                        if (isNumberPattern(compiledPattern, tokenIndex)) {
                            year = readDigits(4);
                        } else {
                            year = readDigitsMaxLong();
                        }
                        bag.year = !negative ? year : -1 * year;
                        break;
                    }
                    case FORMAT_YEAR_SHORT: { // %y, %Ey, %Oy - year % 100 (00..99)
                        final long year = readDigits(2);
                        if (!validRange(year, 0, 99)) {
                            fail = true;
                        }
                        bag.year = year;
                        if (!bag.has(bag.cent)) {
                            bag.cent = year >= 69 ? 19 : 20;
                        }
                        break;
                    }
                    case FORMAT_ZONE_ID: // %Z - Time zone abbreviation name
                    case FORMAT_COLON_ZONE_OFF: {
                        // %z - Time zone as hour and minute offset from UTC (e.g. +0900)
                        //      %:z - hour and minute offset from UTC with a colon (e.g. +09:00)
                        //      %::z - hour, minute and second offset from UTC (e.g. +09:00:00)
                        //      %:::z - hour, minute and second offset from UTC
                        //          (e.g. +09, +09:30, +09:30:30)
                        if (isEndOfText(text, pos)) {
                            fail = true;
                            break;
                        }

                        final Matcher m = ZONE_PARSE_REGEX.matcher(text.substring(pos));
                        if (m.find()) {
                            // zone
                            String zone = text.substring(pos, pos + m.end());
                            bag.zone = zone;
                            pos += zone.length();
                        } else {
                            fail = true;
                        }
                        break;
                    }
                    case FORMAT_SPECIAL:
                    {
                        throw new Error("FORMAT_SPECIAL is a special token only for the lexer.");
                    }
                }
            }

            if (fail) {
                return null;
            }

            if (text.length() > pos) {
                bag.leftover = text.substring(pos, text.length());
            }

            return bag;
        }

        /**
         * Ports read_digits from ext/date/date_strptime.c in MRI 2.3.1 under BSDL.
         * see https://github.com/ruby/ruby/blob/394fa89c67722d35bdda89f10c7de5c304a5efb1/ext/date/date_strftime.c
         */
        private long readDigits(final int len) {
            char c;
            long v = 0;
            final int initPos = pos;

            for (int i = 0; i < len; i++) {
                if (isEndOfText(text, pos)) {
                    break;
                }

                c = text.charAt(pos);
                if (!isDigit(c)) {
                    break;
                } else {
                    v = v * 10 + toInt(c);
                }
                pos += 1;
            }

            if (pos == initPos) {
                fail = true;
            }

            return v;
        }

        /**
         * Ports READ_DIGITS_MAX from ext/date/date_strptime.c in MRI 2.3.1 under BSDL.
         * see https://github.com/ruby/ruby/blob/394fa89c67722d35bdda89f10c7de5c304a5efb1/ext/date/date_strftime.c
         */
        private BigInteger readDigitsMax() {
            char c;
            BigInteger v = BigInteger.ZERO;
            final int initPos = pos;

            while (true) {
                if (isEndOfText(text, pos)) {
                    break;
                }

                c = text.charAt(pos);
                if (!isDigit(c)) {
                    break;
                } else {
                    v = v.multiply(BigInteger.TEN).add(new BigInteger(Integer.toString(toInt(c))));
                }
                pos += 1;
            }

            if (pos == initPos) {
                fail = true;
            }

            return v;
        }

        private long readDigitsMaxLong() {
            char c;
            long v = 0L;
            final int initPos = pos;

            while (true) {
                if (isEndOfText(text, pos)) {
                    break;
                }

                c = text.charAt(pos);
                if (!isDigit(c)) {
                    break;
                } else {
                    v = v * 10 + toInt(c);
                }
                pos += 1;
            }

            if (pos == initPos) {
                fail = true;
            }

            return v;
        }

        /**
         * Returns -1 if text doesn't match with patterns.
         */
        private int findIndexInPatterns(final String[] patterns) {
            if (isEndOfText(text, pos)) {
                return -1;
            }

            for (int i = 0; i < patterns.length; i++) {
                final String pattern = patterns[i];
                final int len = pattern.length();
                if (!isEndOfText(text, pos + len - 1)
                        && pattern.equalsIgnoreCase(text.substring(pos, pos + len))) { // strncasecmp
                    return i;
                }
            }

            return -1; // text doesn't match at any patterns.
        }

        /**
         * Ports num_pattern_p from ext/date/date_strptime.c in MRI 2.3.1 under BSDL.
         * see https://github.com/ruby/ruby/blob/394fa89c67722d35bdda89f10c7de5c304a5efb1/ext/date/date_strftime.c
         */
        private static boolean isNumberPattern(final List<StrptimeToken> compiledPattern, final int i) {
            if (compiledPattern.size() <= i + 1) {
                return false;
            } else {
                final StrptimeToken nextToken = compiledPattern.get(i + 1);
                final StrptimeFormat f = nextToken.getFormat();
                if (f == StrptimeFormat.FORMAT_STRING && isDigit(((String) nextToken.getData()).charAt(0))) {
                    return true;
                } else if (NUMBER_PATTERNS.contains(f)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        // CDdeFGgHIjkLlMmNQRrSsTUuVvWwXxYy
        private static final EnumSet<StrptimeFormat> NUMBER_PATTERNS =
                EnumSet.copyOf(Arrays.asList(
                        StrptimeFormat.FORMAT_CENTURY, // 'C'
                        // D
                        StrptimeFormat.FORMAT_DAY, // 'd'
                        StrptimeFormat.FORMAT_DAY_S, // 'e'
                        // F
                        StrptimeFormat.FORMAT_WEEKYEAR, // 'G'
                        StrptimeFormat.FORMAT_WEEKYEAR_SHORT, // 'g'
                        StrptimeFormat.FORMAT_HOUR, // 'H'
                        StrptimeFormat.FORMAT_HOUR_M, // 'I'
                        StrptimeFormat.FORMAT_DAY_YEAR, // 'j'
                        StrptimeFormat.FORMAT_HOUR_BLANK, // 'k'
                        StrptimeFormat.FORMAT_MILLISEC, // 'L'
                        StrptimeFormat.FORMAT_HOUR_S, // 'l'
                        StrptimeFormat.FORMAT_MINUTES, // 'M'
                        StrptimeFormat.FORMAT_MONTH, // 'm'
                        StrptimeFormat.FORMAT_NANOSEC, // 'N'
                        // Q, R, r
                        StrptimeFormat.FORMAT_SECONDS, // 'S'
                        StrptimeFormat.FORMAT_EPOCH, // 's'
                        // T
                        StrptimeFormat.FORMAT_WEEK_YEAR_S, // 'U'
                        StrptimeFormat.FORMAT_DAY_WEEK2, // 'u'
                        StrptimeFormat.FORMAT_WEEK_WEEKYEAR, // 'V'
                        // v
                        StrptimeFormat.FORMAT_WEEK_YEAR_M, // 'W'
                        StrptimeFormat.FORMAT_DAY_WEEK, // 'w'
                        // X, x
                        StrptimeFormat.FORMAT_YEAR_LONG, // 'Y'
                        StrptimeFormat.FORMAT_YEAR_SHORT // 'y'
                ));

        /**
         * Ports valid_pattern_p in ext/date/date_strptime.c in MRI 2.3.1 under BSDL.
         * see https://github.com/ruby/ruby/blob/394fa89c67722d35bdda89f10c7de5c304a5efb1/ext/date/date_strftime.c
         */
        private static boolean validRange(long v, int lower, int upper) {
            return lower <= v && v <= upper;
        }

        private static boolean isSpace(char c) {
            return c == ' ' || c == '\t' || c == '\n' ||
                    c == '\u000b' || c == '\f' || c == '\r';
        }

        private static boolean isDigit(char c) {
            return '0' <= c && c <= '9';
        }

        private static boolean isEndOfText(String text, int pos) {
            return pos >= text.length();
        }

        private static boolean isSign(String text, int pos) {
            return !isEndOfText(text, pos) && (text.charAt(pos) == '+' || text.charAt(pos) == '-');
        }

        private static boolean isMinus(String text, int pos) {
            return !isEndOfText(text, pos) && text.charAt(pos) == '-';
        }

        private static boolean isBlank(String text, int pos) {
            return !isEndOfText(text, pos) && text.charAt(pos) == ' ';
        }

        private static int toInt(char c) {
            return c - '0';
        }
    }
}
