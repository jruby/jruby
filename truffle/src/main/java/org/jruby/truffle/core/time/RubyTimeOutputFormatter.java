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
package org.jruby.truffle.core.time;

import org.jruby.truffle.core.time.RubyDateFormatter.FieldType;

/**
 * Support for GNU-C output formatters, see: http://www.gnu.org/software/libc/manual/html_node/Formatting-Calendar-Time.html
 */
public class RubyTimeOutputFormatter {
    final String flags;
    final int width;

    public final static RubyTimeOutputFormatter DEFAULT_FORMATTER = new RubyTimeOutputFormatter("", 0);

    public RubyTimeOutputFormatter(String flags, int width) {
        this.flags = flags;
        this.width = width;
    }

    public int getWidth(int defaultWidth) {
        if (flags.indexOf('-') != -1) { // no padding
            return 0;
        }
        return this.width != 0 ? this.width : defaultWidth; 
    }

    public char getPadder(char defaultPadder) {
        char padder = defaultPadder;
        for (int i = 0; i < flags.length(); i++) {
            switch (flags.charAt(i)) {
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

        for (int i = 0; i < flags.length(); i++) {
            switch (flags.charAt(i)) {
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

    public static String formatNumber(long value, int width, char padder) {
        if (value >= 0 || padder != '0') {
            return padding(Long.toString(value), width, padder);
        } else {
            return "-" + padding(Long.toString(-value), width - 1, padder);
        }
    }

    public static String formatSignedNumber(long value, int width, char padder) {
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

    private static final int SMALLBUF = 100;

    static String padding(String sequence, int width, char padder) {
        if (sequence.length() >= width) {
            return sequence;
        }

        if (width > SMALLBUF) throw new IndexOutOfBoundsException("padding width " + width + " too large");

        StringBuilder buf = new StringBuilder(width + sequence.length());
        for (int i = sequence.length(); i < width; i++) {
            buf.append(padder);
        }
        buf.append(sequence);
        return buf.toString();
    }
}
