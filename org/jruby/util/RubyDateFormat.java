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

import java.text.*;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyDateFormat extends DateFormat {
    private String format;

    /**
     * Constructor for RubyDateFormat.
     */
    public RubyDateFormat(String format) {
        super();
        
        this.format = format;
    }

    /**
     * @see DateFormat#format(Date, StringBuffer, FieldPosition)
     */
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        return new SimpleDateFormat(getSimpleFormatString()).format(date, toAppendTo, fieldPosition);
    }

    /**
     * @see DateFormat#parse(String, ParsePosition)
     */
    public Date parse(String source, ParsePosition pos) {
        return new SimpleDateFormat(getSimpleFormatString()).parse(source, pos);
    }

    private String getSimpleFormatString() {
        StringBuffer result = new StringBuffer(100);

        int len = format.length();
        for (int i = 0; i < len;) {
            if (format.charAt(i) == '%') {
                i++;
                switch (format.charAt(i)) {
                    case 'A' :
                        result.append("EEEE");
                        break;
                    case 'a' :
                        result.append("EEE");
                        break;
                    case 'B' :
                        result.append("MMMM");
                        break;
                    case 'b' :
                        result.append("MMM");
                        break;
                    case 'c' :
                        result.append("EEE MMM dd HH:mm:ss yyyy");
                        break;
                    case 'd':
                        result.append("dd");
                        break;
                    case 'H':
                        result.append("HH");
                        break;
                    case 'I':
                        result.append("KK");
                        break;
                    case 'j':
                        result.append("DDD");
                        break;
                    case 'M':
                        result.append("mm");
                        break;
                    case 'm':
                        result.append("MM");
                        break;
                    case 'p':
                        result.append("aa");
                        break;
                    case 'S':
                        result.append("ss");
                        break;
                    case 'U':
                        result.append("ww");
                        break;
                    case 'W':
                        result.append("ww");
                        break;
                    case 'w':
                        result.append("EE");
                        break;
                    case 'X':
                        result.append("HH:mm:ss");
                        break;
                    case 'x':
                        result.append("MM/dd/YY");
                        break;
                    case 'Y':
                        result.append("yyyy");
                        break;
                    case 'y':
                        result.append("yy");
                        break;
                    case 'Z':
                        result.append("z");
                        break;
                    case 'z':
                        result.append("Z");
                        break;
                    case '%':
                        result.append("%");
                        break;
                    case '\'':
                        result.append("%\\'");
                        break;
                    default:
                        result.append("\'%");
                        result.append(format.charAt(i));
                        result.append('\'');
                }
                i++;
            } else {
                result.append('\'');
                for (;i < len && format.charAt(i) != '%'; i++) {
                    if (format.charAt(i) == '\'') {
                        result.append('\\');
                    }
                    result.append(format.charAt(i));
                }
                result.append('\'');
            }
        }

        return result.toString();
    }
}