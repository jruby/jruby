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

import org.jruby.util.RubyDateFormatter.FieldType;

/**
 * Support for GNU-C output formatters, see: http://www.gnu.org/software/libc/manual/html_node/Formatting-Calendar-Time.html
 */
public class RubyTimeOutputFormatter extends RubyDateFormatter.Token {
    final ByteList flags;
    final int width;

    public final static RubyTimeOutputFormatter DEFAULT_FORMATTER = new RubyTimeOutputFormatter(ByteList.EMPTY_BYTELIST, 0);

    public RubyTimeOutputFormatter(ByteList flags, int width) {
        super(RubyDateFormatter.Format.FORMAT_OUTPUT, null);
        this.flags = flags;
        this.width = width;
        this.data = this;
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

    public void format(ByteList out, long value, FieldType type) {
        int width = getWidth(type.defaultWidth);
        char padder = getPadder(type.defaultPadder);

        formatNumber(out, value, width, padder);
    }

    // FIXME: I think this should not be done with CharSequence but ByteList but I didn't want to mess with it
    public void format(ByteList out, CharSequence sequence) {
        int width = getWidth(0);
        char padder = getPadder(' ');

        for (int i = 0; i < flags.length(); i++) {
            switch (flags.charAt(i)) {
                case '^':
                    sequence = sequence.toString().toUpperCase();
                    break;
                case '#': // change case
                    char last = sequence.charAt(sequence.length() - 1);
                    if (Character.isLowerCase(last)) {
                        sequence = sequence.toString().toUpperCase();
                    } else {
                        sequence = sequence.toString().toLowerCase();
                    }
                    break;
            }
        }

        padding(out, sequence.length(), width, padder);
        if (sequence instanceof ByteList) {
            out.append((ByteList) sequence);
        } else {
            out.append(sequence.toString().getBytes());
        }
    }

    static void outputLong(ByteList out, int length, long value) {
        out.ensure(out.length() + length);
        if (value < 0) {
            out.append('-');
            length -= 1;
            value = -value;
        } else if (value == 0) {
            out.append('0');
            return;
        }

        byte[] unsafe = out.unsafeBytes();
        int begin = out.getBegin() + out.realSize() - 1;
        for (int i = begin + length; i > begin; i--) {
            unsafe[i] = (byte) ('0' + (value % 10));
            value /= 10;
        }
        out.setRealSize(out.realSize() + length);
    }

    private static final int MAX_DIGITS = 19;  // 9,223,372,036,854,775,807

    static int longSize(long number) {
        return number < 0 ? longSizeInner(-number) + 1 /* for '-' */ : longSizeInner(number);
    }

    static int longSizeInner(long number) {
        long largerNumber = 10;
        for (int digits = 1; digits < MAX_DIGITS; digits++, largerNumber *= 10) {
            if (number < largerNumber) return digits;
        }

        return MAX_DIGITS;
    }

    // FIXME: longSize and width gives us mechanism to combine padding and outputLong
    static void formatNumber(ByteList out, long value, int width, char padder) {
        if (value >= 0 || padder != '0') {
            int size = longSize(value);
            padding(out, size, width, padder);
            outputLong(out, size, value);
        } else {
            int size = longSize(-value);
            out.append('-');
            padding(out, size, width-1, padder);
            outputLong(out, size, -value);
        }
    }

    static void formatSignedNumber(ByteList out, long value, long second, int width, char padder) {
        if (padder == '0') {
            if (value == 0) {
                out.append(value == 0 && second < 0 ? '-' : '+'); // -0 needs to be -0
                padding(out, 0, width - 1, padder);
            } else if (value > 0) {
                String num = Long.toString(value);
                out.append('+');
                padding(out, num.length(), width - 1, padder);
                out.append(num.getBytes());
            } else {
                String num = Long.toString(-value);
                out.append('-');
                padding(out, num.length(), width - 1, padder);
                out.append(num.getBytes());
            }
        } else {
            String num = Long.toString(value);
            if (value == 0) {
                out.append(value == 0 && second < 0 ? '-' : '+'); // -0 needs to be -0
            } else if (value > 0) {
                padding(out, num.length(), width - 1, padder);
                out.append('+');
            } else {
                padding(out, num.length(), width, padder);
            }
            out.append(num.getBytes());
        }
    }

    private static final int SMALLBUF = 100;

    // sequence is assumed to be clean 7bit ASCII
    private static void padding(ByteList out, int len, final int width, final char padder) {
        if (len >= width) return;

        if (width > SMALLBUF) throw new IndexOutOfBoundsException("padding width " + width + " too large");

        // can pre-calc common pads like ' ' or '0'.
        for (int i = len; i < width; i++) out.append(padder);
    }

    public String toString() {
        return "RTOF - flags: " + flags + ", width: " + width;
    }
}
