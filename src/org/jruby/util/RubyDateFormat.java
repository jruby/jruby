/*
 * RubyDateFormat.java - An util class, which provides Time formating.
 * Created on 21.05.2002, 17:49:33
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.util;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyDateFormat extends DateFormat {
    private List compiledPattern;

    private DateFormatSymbols formatSymbols;

    private static final int FORMAT_STRING = 0;
    private static final int FORMAT_WEEK_LONG = 1;
    private static final int FORMAT_WEEK_SHORT = 2;
    private static final int FORMAT_MONTH_LONG = 3;
    private static final int FORMAT_MONTH_SHORT = 4;
    private static final int FORMAT_DAY = 5;
    private static final int FORMAT_HOUR = 6;
    private static final int FORMAT_HOUR_M = 7;
    private static final int FORMAT_DAY_YEAR = 8;
    private static final int FORMAT_MINUTES = 9;
    private static final int FORMAT_MONTH = 10;
    private static final int FORMAT_MERIDIAN = 11;
    private static final int FORMAT_SECONDS = 12;
    private static final int FORMAT_WEEK_YEAR_S = 13;
    private static final int FORMAT_WEEK_YEAR_M = 14;
    private static final int FORMAT_DAY_WEEK = 15;
    private static final int FORMAT_YEAR_LONG = 16;
    private static final int FORMAT_YEAR_SHORT = 17;
    private static final int FORMAT_ZONE_OFF = 18;
    private static final int FORMAT_ZONE_ID = 19;

    private static class Token {
        private int format;
        private Object data;
        
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
    
    public RubyDateFormat(String pattern, DateFormatSymbols formatSymbols) {
        super();

        this.formatSymbols = formatSymbols;
        applyPattern(pattern);
    }
    
    public void applyPattern(String pattern) {
        compilePattern(pattern);
    }

    private void compilePattern(String pattern) {
        compiledPattern = new LinkedList();
        
        int len = pattern.length();
        for (int i = 0; i < len;) {
            if (pattern.charAt(i) == '%') {
                i++;
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
                        compiledPattern.add(new Token(FORMAT_MONTH_SHORT));
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
                    case 'd':
                        compiledPattern.add(new Token(FORMAT_DAY));
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
                    case 'M':
                        compiledPattern.add(new Token(FORMAT_MINUTES));
                        break;
                    case 'm':
                        compiledPattern.add(new Token(FORMAT_MONTH));
                        break;
                    case 'p':
                        compiledPattern.add(new Token(FORMAT_MERIDIAN));
                        break;
                    case 'S':
                        compiledPattern.add(new Token(FORMAT_SECONDS));
                        break;
                    case 'U':
                        compiledPattern.add(new Token(FORMAT_WEEK_YEAR_S));
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
                    default:
                        compiledPattern.add(new Token(FORMAT_STRING, "%" + pattern.charAt(i)));
                }
                i++;
            } else {
                StringBuffer sb = new StringBuffer();
                for (;i < len && pattern.charAt(i) != '%'; i++) {
                    sb.append(pattern.charAt(i));
                }
                compiledPattern.add(new Token(FORMAT_STRING, sb.toString()));
            }
        }
    }

    /**
     * @see DateFormat#format(Date, StringBuffer, FieldPosition)
     */
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        calendar.setTime(date);
        
        Iterator iter = compiledPattern.iterator();
        while (iter.hasNext()) {
            Token token = (Token) iter.next();
            
            switch (token.getFormat()) {
                case FORMAT_STRING:
                    toAppendTo.append(token.getData());
                    break;
                case FORMAT_WEEK_LONG:
                    toAppendTo.append(formatSymbols.getWeekdays()[calendar.get(Calendar.DAY_OF_WEEK)]);
                    break;
                case FORMAT_WEEK_SHORT:
                    toAppendTo.append(formatSymbols.getShortWeekdays()[calendar.get(Calendar.DAY_OF_WEEK)]);
                    break;
                case FORMAT_MONTH_LONG:
                    toAppendTo.append(formatSymbols.getMonths()[calendar.get(Calendar.MONTH)]);
                    break;
                case FORMAT_MONTH_SHORT:
                    toAppendTo.append(formatSymbols.getShortMonths()[calendar.get(Calendar.MONTH)]);
                    break;
                case FORMAT_DAY:
                    int value = calendar.get(Calendar.DAY_OF_MONTH);
                    if (value < 10) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value);
                    break;
                case FORMAT_HOUR:
                    value = calendar.get(Calendar.HOUR_OF_DAY);
                    if (value < 10) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value);
                    break;
                case FORMAT_HOUR_M:
                    value = calendar.get(Calendar.HOUR);
                    if (value < 10) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value);
                    break;
                case FORMAT_DAY_YEAR:
                    value = calendar.get(Calendar.DAY_OF_YEAR);
                    if (value < 10) {
                        toAppendTo.append("00");
                    } else if (value < 100) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value);
                    break;
                case FORMAT_MINUTES:
                    value = calendar.get(Calendar.MINUTE);
                    if (value < 10) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value);
                    break;
                case FORMAT_MONTH:
                    value = calendar.get(Calendar.MONTH) + 1;
                    if (value < 10) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value);
                    break;
                case FORMAT_MERIDIAN:
                    if (calendar.get(Calendar.AM_PM) == Calendar.AM) {
                        toAppendTo.append("AM");
                    } else {
                        toAppendTo.append("PM");
                    }
                    break;
                case FORMAT_SECONDS:
                    value = calendar.get(Calendar.SECOND);
                    if (value < 10) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value);
                    break;
                case FORMAT_WEEK_YEAR_M:
                	// GregorianCalendar uses Sunday by default; switch to Monday and back
                	calendar.setFirstDayOfWeek(Calendar.MONDAY);
                	// intentional fall-through
                case FORMAT_WEEK_YEAR_S:
                	// Ruby uses zero-based weeks
                    value = calendar.get(Calendar.WEEK_OF_YEAR) - 1;
                    if (value < 10) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value);
                    // to clear MONDAY from above
                    calendar.setFirstDayOfWeek(Calendar.SUNDAY);
                    break;
                case FORMAT_DAY_WEEK:
                    value = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                    toAppendTo.append(value);
                    break;
                case FORMAT_YEAR_LONG:
                    value = calendar.get(Calendar.YEAR);
                    if (value < 10) {
                        toAppendTo.append("000");
                    } else if (value < 100) {
                        toAppendTo.append("00");
                    } else if (value < 1000) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value);
                    break;
                case FORMAT_YEAR_SHORT:
                    value = calendar.get(Calendar.YEAR) % 100;
                    if (value < 10) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value);
                    break;
                case FORMAT_ZONE_OFF:
                    value = calendar.getTimeZone().getOffset(calendar.getTimeInMillis());
                    if (value < 0) {
                        toAppendTo.append('+');
                    } else {
                        toAppendTo.append('-');
                    }
                    value = Math.abs(value);
                    if (value / 3600000 < 10) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value / 3600000);
                    value = value % 3600000 / 60000;
                    if (value < 10) {
                        toAppendTo.append('0');
                    }
                    toAppendTo.append(value);
                    break;
                case FORMAT_ZONE_ID:
                    toAppendTo.append(calendar.getTimeZone().getDisplayName(calendar.get(Calendar.DST_OFFSET) != 0, TimeZone.SHORT));
                    break;
            }
        }

        return toAppendTo;
    }

    /**
     * @see DateFormat#parse(String, ParsePosition)
     */
    public Date parse(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }
}