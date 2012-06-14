/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.util;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;

public class RubyDateFormat extends DateFormat {
    private static final long serialVersionUID = -250429218019023997L;

    private boolean ruby_1_9;
    private List<Token> compiledPattern;

    private final DateFormatSymbols formatSymbols;

    private static final int FORMAT_STRING = 0;
    private static final int FORMAT_WEEK_LONG = 1;
    private static final int FORMAT_WEEK_SHORT = 2;
    private static final int FORMAT_MONTH_LONG = 3;
    private static final int FORMAT_MONTH_SHORT = 4;
    private static final int FORMAT_DAY = 5;
    private static final int FORMAT_DAY_S = 6;
    private static final int FORMAT_HOUR = 7;
    private static final int FORMAT_HOUR_M = 8;
    private static final int FORMAT_HOUR_S = 9;
    private static final int FORMAT_DAY_YEAR = 10;
    private static final int FORMAT_MINUTES = 11;
    private static final int FORMAT_MONTH = 12;
    private static final int FORMAT_MERIDIAN = 13;
    private static final int FORMAT_MERIDIAN_LOWER_CASE = 14;
    private static final int FORMAT_SECONDS = 15;
    private static final int FORMAT_WEEK_YEAR_S = 16;
    private static final int FORMAT_WEEK_YEAR_M = 17;
    private static final int FORMAT_DAY_WEEK = 18;
    private static final int FORMAT_YEAR_LONG = 19;
    private static final int FORMAT_YEAR_SHORT = 20;
    private static final int FORMAT_ZONE_OFF = 21;
    private static final int FORMAT_ZONE_ID = 22;
    private static final int FORMAT_CENTURY = 23;
    private static final int FORMAT_HOUR_BLANK = 24;
    private static final int FORMAT_MILLISEC = 25;
    private static final int FORMAT_EPOCH = 26;
    private static final int FORMAT_DAY_WEEK2 = 27;
    private static final int FORMAT_WEEK_WEEKYEAR = 28;
    private static final int FORMAT_NANOSEC = 29;
    private static final int FORMAT_PRECISION = 30;
    private static final int FORMAT_WEEKYEAR = 31;
    private static final int FORMAT_OUTPUT = 32;
    private static final int FORMAT_COLON_ZONE_OFF = 33;
    private static final int FORMAT_COLON_COLON_ZONE_OFF = 34;
    private static final int FORMAT_COLON_COLON_COLON_ZONE_OFF = 35;


    private static class Token {
        private int format;
        private Object data;
        private TimeOutputFormatter outputFormatter;
        
        public Token(int format) {
            this(format, null);
        }

        public Token(int format, Object data) {
            this.format = format;
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
        public int getFormat() {
            return format;
        }
    }

    /**
     * Constructor for RubyDateFormat.
     */
    public RubyDateFormat() {
        this("", new DateFormatSymbols());
    }

    public RubyDateFormat(String pattern, Locale aLocale) {
        this(pattern, new DateFormatSymbols(aLocale));
    }

    public RubyDateFormat(String pattern, Locale aLocale, boolean ruby_1_9) {
        this(pattern, aLocale);
        this.ruby_1_9 = ruby_1_9;
    }
    
    public RubyDateFormat(String pattern, DateFormatSymbols formatSymbols) {
        super();

        this.formatSymbols = formatSymbols;
        applyPattern(pattern);
    }
    
    public void applyPattern(String pattern) {
        compilePattern(pattern);
    }
    
    private void compilePattern(String pattern) {
        compiledPattern = new LinkedList<Token>();
        
        int len = pattern.length();
        for (int i = 0; i < len;) {
            if (pattern.charAt(i) == '%') {
                i++;

                if(i == len) {
                    compiledPattern.add(new Token(FORMAT_STRING, "%"));
                } else {
                    i = addOutputFormatter(pattern, i);

                    switch (pattern.charAt(i)) {
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
                        compiledPattern.add(new Token(FORMAT_DAY));
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
                    case 'p':
                        compiledPattern.add(new Token(FORMAT_MERIDIAN));
                        break;
                    case 'P':
                        compiledPattern.add(new Token(FORMAT_MERIDIAN_LOWER_CASE));
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
                        compiledPattern.add(new Token(FORMAT_ZONE_ID));
                        break;
                    case 'z':
                        compiledPattern.add(new Token(FORMAT_ZONE_OFF));
                        break;
                    case '%':
                        compiledPattern.add(new Token(FORMAT_STRING, "%"));
                        break;
                    case ':':
                        i++;
                        if(i == len) {
                            compiledPattern.add(new Token(FORMAT_STRING, "%:"));
                        } else {
                            switch (pattern.charAt(i)) {
                                case 'z':
                                    compiledPattern.add(new Token(FORMAT_COLON_ZONE_OFF));
                                    break;
                                case ':':
                                    i++;
                                    if(i == len) {
                                        compiledPattern.add(new Token(FORMAT_STRING, "%::"));
                                    } else {
                                        switch (pattern.charAt(i)) {
                                            case 'z':
                                                compiledPattern.add(new Token(FORMAT_COLON_COLON_ZONE_OFF));
                                                break;
                                            case ':':
                                                i++;
                                                if(i == len) {
                                                    compiledPattern.add(new Token(FORMAT_STRING, "%:::"));
                                                } else {
                                                    switch (pattern.charAt(i)) {
                                                        case 'z':
                                                            compiledPattern.add(new Token(FORMAT_COLON_COLON_COLON_ZONE_OFF));
                                                            break;
                                                        case ':':
                                                        default:
                                                            compiledPattern.add(new Token(FORMAT_STRING, "%:::" + pattern.charAt(i)));
                                                    }
                                                }
                                                break;
                                            default:
                                                compiledPattern.add(new Token(FORMAT_STRING, "%::" + pattern.charAt(i)));
                                        }
                                    }
                                    break;
                                default:
                                    compiledPattern.add(new Token(FORMAT_STRING, "%:" + pattern.charAt(i)));
                            }
                        }
                        break;
                    default:
                        compiledPattern.add(new Token(FORMAT_STRING, "%" + pattern.charAt(i)));
                    }
                    i++;
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (;i < len && pattern.charAt(i) != '%'; i++) {
                    sb.append(pattern.charAt(i));
                }
                compiledPattern.add(new Token(FORMAT_STRING, sb.toString()));
            }
        }
    }

    private int addOutputFormatter(String pattern, int index) {
        if (ruby_1_9) {
            TimeOutputFormatter outputFormatter = TimeOutputFormatter.getFormatter(pattern.substring(index - 1));
            if (outputFormatter != null) {
                index += outputFormatter.getFormatter().length();
                compiledPattern.add(new Token(FORMAT_OUTPUT, outputFormatter));
            }
        }
        return index;
    }

    private String formatOutput(TimeOutputFormatter formatter, String output) {
        if (formatter == null) return output;
        output = formatter.format(output);
        formatter = null;
        return output;
    }

    private DateTime dt;
    private long nsec;

    public void setDateTime(final DateTime dt) {
        this.dt = dt;
    }

    public void setNSec(long nsec) {
        this.nsec = nsec;
    }
    
    // Much faster then generic String.format()
    private String twoCharDigit(long value) {
        if (value == 0) return "00";
        if (value < 10) return "0"+value;
        return ""+value;        
    }
    
    // Much faster then generic String.format()
    private String threeCharDigit(long value) {
        if (value == 0) return "000";
        if (value < 10) return "00"+value;
        if (value < 100) return "0"+value;
        return ""+value;
    }

    // Much faster then generic String.format()
    private String fourCharDigit(long value) {
        if (value == 0) return "0000";
        if (value < 10) return "000"+value;
        if (value < 100) return "00"+value;
        if (value < 1000) return "0"+value;
        return ""+value;
    }

    /**
     * @see DateFormat#format(Date, StringBuffer, FieldPosition)
     */
    public StringBuffer format(Date ignored, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        TimeOutputFormatter formatter = null;
        for (Token token: compiledPattern) {
            String output = null;
            long value = 0;
            boolean format = true;

            switch (token.getFormat()) {
                case FORMAT_OUTPUT:
                    formatter = (TimeOutputFormatter) token.getData();
                    break;
                case FORMAT_STRING:
                    output = token.getData().toString();
                    format = false;
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
                    output = twoCharDigit(dt.getDayOfMonth());
                    break;
                case FORMAT_DAY_S: 
                    value = dt.getDayOfMonth();
                    output = (value < 10 ? " " : "") + Long.toString(value);
                    break;
                case FORMAT_HOUR:
                case FORMAT_HOUR_BLANK:
                    value = dt.getHourOfDay();
                    output = "";
                    if (value < 10) {
                        output += token.getFormat() == FORMAT_HOUR ? "0" : " ";
                    }
                    output += value;
                    break;
                case FORMAT_HOUR_M:
                case FORMAT_HOUR_S:
                    value = dt.getHourOfDay();

                    if(value > 12) {
                        value-=12;
                    }

                    if(value == 0) {
                        output = "12";
                    } else {
                        output = "";
                        if (value < 10) {
                            output += token.getFormat() == FORMAT_HOUR_M ? "0" : " ";
                        }
                        output += value;
                    }
                    break;
                case FORMAT_DAY_YEAR:
                    output = threeCharDigit(dt.getDayOfYear());
                    break;
                case FORMAT_MINUTES:
                    output = twoCharDigit(dt.getMinuteOfHour());
                    break;
                case FORMAT_MONTH:
                    output = twoCharDigit(dt.getMonthOfYear());
                    break;
                case FORMAT_MERIDIAN:
                case FORMAT_MERIDIAN_LOWER_CASE:
                    if (dt.getHourOfDay() < 12) {
                        output = token.getFormat() == FORMAT_MERIDIAN ? "AM" : "am";
                    } else {
                        output = token.getFormat() == FORMAT_MERIDIAN ? "PM" : "pm";
                    }
                    break;
                case FORMAT_SECONDS:
                    value = dt.getSecondOfMinute();
                    output = (value < 10 ? "0" : "") + Long.toString(value);
                    break;
                case FORMAT_WEEK_YEAR_M:
                    output = formatWeekYear(java.util.Calendar.MONDAY);
                    break;
                case FORMAT_WEEK_YEAR_S:
                    output = formatWeekYear(java.util.Calendar.SUNDAY);
                    break;
                case FORMAT_DAY_WEEK:
                case FORMAT_DAY_WEEK2:
                    value = dt.getDayOfWeek() ;
                    if (token.getFormat() == FORMAT_DAY_WEEK) {
                        value = value % 7;
                    }
                    output = Long.toString(value);
                    break;
                case FORMAT_YEAR_LONG:
                    output = fourCharDigit(dt.getYear());
                    break;
                case FORMAT_YEAR_SHORT:
                    output = twoCharDigit(dt.getYear() % 100);
                    break;
                case FORMAT_ZONE_OFF:
                case FORMAT_COLON_ZONE_OFF:
                case FORMAT_COLON_COLON_ZONE_OFF:
                case FORMAT_COLON_COLON_COLON_ZONE_OFF:
                    value = dt.getZone().getOffset(dt.getMillis());
                    output = value < 0 ? "-" : "+";

                    value = Math.abs(value);

                    // hours
                    if (value / 3600000 < 10) {
                        output += "0";
                    }
                    output += (value / 3600000);

                    // :::z just shows hour
                    if (token.getFormat() == FORMAT_COLON_COLON_COLON_ZONE_OFF) break;

                    // :z and ::z have colon after hour
                    if (token.getFormat() == FORMAT_COLON_ZONE_OFF ||
                            token.getFormat() == FORMAT_COLON_COLON_ZONE_OFF) output += ':';

                    // minutes
                    if ((value % 3600000 / 60000) < 10) {
                        output += "0";
                    }
                    output += value % 3600000 / 60000;

                    // ::z includes colon and seconds
                    if (token.getFormat() == FORMAT_COLON_COLON_ZONE_OFF) {
                        // seconds
                        if ((value % 60000) < 10) {
                            output += "0";
                        }
                        output += value % 60000;
                    }
                    break;
                case FORMAT_ZONE_ID:
                    toAppendTo.append(dt.getZone().getShortName(dt.getMillis()));
                    break;
                case FORMAT_CENTURY:
                    toAppendTo.append(dt.getCenturyOfEra());
                    break;
                case FORMAT_MILLISEC:
                    output = threeCharDigit(dt.getMillisOfSecond());
                    break;
                case FORMAT_EPOCH:
                    output = Long.toString(dt.getMillis()/1000);
                    break;
                case FORMAT_WEEK_WEEKYEAR:
                    output = twoCharDigit(dt.getWeekOfWeekyear());
                    break;
                case FORMAT_NANOSEC:
                    value = dt.getMillisOfSecond() * 1000000;
                    if (ruby_1_9) value += nsec;
                    String width = ruby_1_9 ? "9" : "3";
                    if (formatter != null) width = formatter.getFormatter();
                    output = formatTruncate(String.valueOf(value), Integer.valueOf(width), "0");
                    formatter = null; // we are done with this formatter
                    break;
                case FORMAT_WEEKYEAR:
                    output = Integer.toString(dt.getWeekyear());
                    break;
            }

            if (output != null) {
                toAppendTo.append(format ? formatOutput(formatter, output) : output);
            }
        }

        return toAppendTo;
    }

	private String formatWeekYear(int firstDayOfWeek) {
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
            return twoCharDigit(value);
	}

    /**
     * @see DateFormat#parse(String, ParsePosition)
     */
    public Date parse(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Return the String obtained by truncating orig to the first len characters
     * 
     * @param orig Original string
     * @param len Maximum length for the returned String
     * @return String thus obtained
     */
    private String formatTruncate(String orig, int len, String pad) {
        if (len == 0) return "";
        if (orig.length() > len) {
            return orig.substring(0, len);
        } else {
            StringBuilder sb = new StringBuilder(len);
            sb.append(orig);
            while (sb.length() < len) {
                sb = sb.append(pad);
            }
            return sb.toString().substring(0,len);
        }
    }
}
