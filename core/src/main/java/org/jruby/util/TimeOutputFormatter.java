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

import org.jruby.util.RubyDateFormat.FieldType;

/**
 * Support for GNU-C output formatters, see: http://www.gnu.org/software/libc/manual/html_node/Formatting-Calendar-Time.html
 */
@Deprecated
public class TimeOutputFormatter {
    final String format;
    final int width;

    public final static TimeOutputFormatter DEFAULT_FORMATTER = new TimeOutputFormatter("", 0);

    public TimeOutputFormatter(String format, int width) {
        this.format = format;
        this.width = width;
    }

    // Really ugly stop-gap method to eliminate using regexp for create TimeOutputFormatter.
    // FIXME: Make all of strftime an honest to goodness parser
    public static TimeOutputFormatter getFormatter(String pattern) {
        int length = pattern.length();
        
        if (length <= 1 || pattern.charAt(0) != '%') {
           return null;
        }

        int width = 0;
        
        int i = 1;
        boolean done = false;
        boolean formatterFound = false;
        
        for (; i < length && !done; i++) {
            char c = pattern.charAt(i);
            switch(c) {
                case '-': case '_': case '0': case '^': case '#': case ':':
                    formatterFound = true;
                    break;
                case '1': case '2': case '3': case '4': case '5': 
                case '6': case '7': case '8': case '9':
                    width = Character.getNumericValue(c);
                    done = true;
                    break;
                default:
                    done = true;
                    break;
            }
        }
        
        if (width == 0 && !formatterFound) return null;
        
        String format;
        if (i > 2) { // found something
            format = pattern.substring(1, i-1);
        } else {
            format = "";
        }

        // We found some formatting instructions but no padding values.
        if (width == 0 && i > 2) {
            return new TimeOutputFormatter(format, 0);
        }
        
        done = false;
        for (; i < length && !done; i++) {
            char c = pattern.charAt(i);
            
            switch(c) {
                case '1': case '2': case '3': case '4': case '5': 
                case '6': case '7': case '8': case '9': case '0':
                    width = 10 * width + Character.getNumericValue(c);
                    break;
                case ':':
                    format += ':'; /* Make it part of the format */
                    break;
                default:
                    done = true;
                    break;
            }
        }
        
        if (width != 0) {
            return new TimeOutputFormatter(format, width);
        }

        return null;
    }

    public String getFormat() {
        return format + (width > 0 ? width : "");
    }

    public int getNumberOfColons() {
        int colons = 0;
        for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) == ':') {
                colons++;
            }
        }
        return colons;
    }

    public int getWidth(int defaultWidth) {
        if (format.contains("-")) { // no padding
            return 0;
        }
        return this.width != 0 ? this.width : defaultWidth; 
    }

    public char getPadder(char defaultPadder) {
        char padder = defaultPadder;
        for (int i = 0; i < format.length(); i++) {
            switch (format.charAt(i)) {
                case '_':
                    padder = ' ';
                    break;
                case '0':
                    padder = '0';
                    break;
                case '-': // no padding
                    padder = '\0';
                    break;
            }
        }
        return padder;
    }

    public String format(String sequence, long value, FieldType type) {
        int width = getWidth(type.defaultWidth);
        char padder = getPadder(type.defaultPadder);

        if (sequence == null) {
            sequence = formatNumber(value, width, padder);
        } else {
            sequence = padding(sequence, width, padder);
        }

        for (int i = 0; i < format.length(); i++) {
            switch (format.charAt(i)) {
                case '^':
                    sequence = sequence.toUpperCase();
                    break;
                case '#': // change case
                    char last = sequence.charAt(sequence.length() - 1);
                    if (Character.isLowerCase(last)) {
                        sequence = sequence.toUpperCase();
                    } else {
                        sequence = sequence.toLowerCase();
                    }
                    break;
            }
        }

        return sequence;
    }

    static String formatNumber(long value, int width, char padder) {
        if (value >= 0 || padder != '0') {
            return padding(Long.toString(value), width, padder);
        } else {
            return "-" + padding(Long.toString(-value), width - 1, padder);
        }
    }

    static String formatSignedNumber(long value, int width, char padder) {
        if (padder == '0') {
            if (value >= 0) {
                return "+" + padding(Long.toString(value), width - 1, padder);
            } else {
                return "-" + padding(Long.toString(-value), width - 1, padder);
            }
        } else {
            if (value >= 0) {
                return padding("+" + Long.toString(value), width, padder);
            } else {
                return padding(Long.toString(value), width, padder);
            }
        }
    }

    static String padding(String sequence, int width, char padder) {
        if (sequence.length() >= width) {
            return sequence;
        }

        StringBuilder buf = new StringBuilder(width + sequence.length());
        for (int i = sequence.length(); i < width; i++) {
            buf.append(padder);
        }
        buf.append(sequence);
        return buf.toString();
    }
}
