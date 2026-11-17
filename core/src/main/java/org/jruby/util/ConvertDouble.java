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
 * Copyright (C) 2010 Thomas E Enebo (tom.enebo@gmail.com)
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

import ch.randelshofer.fastdoubleparser.ConfigurableDoubleParser;
import org.jruby.anno.CRuby;
import ch.randelshofer.fastdoubleparser.NumberFormatSymbols;

import java.util.Collections;
import java.util.Set;

public class ConvertDouble {

    /**
     * Performance optimisation:
     * By reusing an instance of the parser, we can save the costly initialisation time.
     */
    private static final ConfigurableDoubleParser FAST_PARSER = new ConfigurableDoubleParser(
            NumberFormatSymbols.fromDefault()
                    .withGroupingSeparator(Set.<Character>of('_'))
                    .withInfinity(Collections.EMPTY_SET)
                    .withNaN(Collections.EMPTY_SET));


    /**
     * Converts supplied ByteList into a double.  strict-mode will not like
     * extra text non-numeric text or multiple sequention underscores.
     */
    public static double byteListToDouble(ByteList bytes, boolean strict) {
        byte[] b = bytes.getUnsafeBytes();
        int begin = bytes.begin();
        int length = bytes.length();

        if (bytes.indexOf('_') >= 0) {
            bytes = squeezeUnderscores(b, begin, length, strict);
            b = bytes.getUnsafeBytes();
            begin = bytes.begin();
            length = bytes.length();
        }

        if (!rejectedByRuby(b, begin, length)) {
            try {
                // Fast path:
                // Optimistically parse the entire input using the fast parser.
                return FAST_PARSER.parseDouble(b, begin, length);
            } catch (NumberFormatException e) {
            }
        }
        // Slow path:
        // Determine how many bytes we can safely consume, and then
        // call the fast parser again.
        return new DoubleConverter().parse(bytes, strict, true);
    }

    // The most bytes of a number's sign, digits and point kept, and room for its exponent after them
    private static final int SQUEEZE_DIGITS = 15 * 4;
    private static final int SQUEEZE_SIZE = SQUEEZE_DIGITS + 10;

    /**
     * Copy a number without the underscores between its digits, as CRuby does before parsing one
     * that has them. An underscore that isn't between two digits ends the number, or is an error
     * when strict, and so does any other character that can't continue it. Like CRuby, digits past
     * the first 60 bytes of the number before its exponent are dropped.
     *
     * @param b the bytes
     * @param begin where the number's text starts
     * @param length how long its text is
     * @param strict whether to throw for text that does not continue the number
     * @return the number without its underscores
     */
    @CRuby(value = "rb_cstr_to_dbl_raise", file = "object.c")
    static ByteList squeezeUnderscores(byte[] b, int begin, int length, boolean strict) {
        byte[] buf = new byte[SQUEEZE_SIZE];
        int n = 0;
        int e = SQUEEZE_DIGITS;
        int p = begin;
        int end = begin + length;
        byte prev = 0;
        int dotSeen = 0;
        int base = 10;
        byte expLetter = 'e';

        while (p < end && isSpace(b[p])) p++;

        if (p < end && (b[p] == '+' || b[p] == '-')) prev = buf[n++] = b[p++];
        if (p < end && b[p] == '0') {
            prev = buf[n++] = '0';
            p++;
            if (p < end && (b[p] == 'x' || b[p] == 'X')) {
                prev = buf[n++] = 'x';
                base = 16;
                expLetter = 'p';
                p++;
                if (p < end && b[p] == '0') {
                    while (p < end && b[p] == '0') p++;
                }
            } else {
                while (p < end && b[p] == '0') p++; // squeeze successive zeros
            }
        }

        while (p < end) {
            if (b[p] == '_') {
                // remove an underscore between digits
                if (n == 0 || !isDigit(prev, base) || ++p == end || !isDigit(b[p], base)) {
                    if (strict) throw new DoubleConverter.LightweightNumberFormatException("invalid underscore");
                    break;
                }
            }
            prev = b[p++];
            if (e == SQUEEZE_DIGITS && Character.toLowerCase(prev) == expLetter) {
                e = SQUEEZE_SIZE - 1;
                buf[n++] = prev;
                if (p < end && (b[p] == '+' || b[p] == '-')) prev = buf[n++] = b[p++];
                if (p < end && b[p] == '0') {
                    prev = buf[n++] = '0';
                    while (++p < end && b[p] == '0');
                }
                base = 10; // the exponent is decimal, even for a hex number
                continue;
            } else if (isSpace(prev)) {
                while (p < end && isSpace(b[p])) p++;
                if (p < end) {
                    if (strict) throw new DoubleConverter.LightweightNumberFormatException("text after number");
                    break;
                }
            } else if (prev == '.' ? dotSeen++ > 0 : !isDigit(prev, base)) {
                if (strict) throw new DoubleConverter.LightweightNumberFormatException("not a number");
                break;
            }
            if (n < e) buf[n++] = prev;
        }

        return new ByteList(buf, 0, n, false);
    }

    private static boolean isSpace(byte c) {
        return c == ' ' || (c >= '\t' && c <= '\r');
    }

    // an ASCII digit of the base, since a negative byte is part of a multibyte character
    private static boolean isDigit(byte c, int base) {
        return c >= 0 && Character.digit((char) c, base) >= 0;
    }

    /**
     * Efficiently reject strings with leading [_], trailing [_-+], or any of /__|-_|+_|_e|e_|_E|E_/.
     * @param b incoming bytes
     * @param begin starting offset
     * @param length effective length
     * @return true if the string is rejected by Ruby, false otherwise.
     */
    private static boolean rejectedByRuby(byte[] b, int begin, int length) {
        if (length == 0) return true;

        // reject invalid prefix or suffix
        byte first = b[begin];
        int end = length - 1;
        int last = b[end];
        if (first == '_' || last == '_' || last == '+' || last == '-') return true;

        // scan remaining chars to reject invalid underscores
        for (int i = 1, cur = first; i <= end; i++) {
            byte next = b[begin + i];

            if (cur == '_' && (next == 'e' || next == 'E' || next == '_' || next == '.')) return true;
            if (next == '_' && (cur == 'e' || cur == 'E' || cur == '-' || cur == '+')) return true;

            cur = next;
        }

        return false;
    }

    public static class DoubleConverter {
        private byte[] bytes;
        private int index;
        private int endIndex;
        private boolean isStrict;
        private char[] chars; // result string we use to parse.
        private int charsIndex;
        private int significantDigitsProcessed;
        private int adjustExponent;
        private boolean wroteExponent;
        private double result;

        // To match MRI so that we end up rounding on values greater than being storable in a 64
        // bit value in the same fashion we are using 30.  Based on wikipedia a note recommends 
        //17 + 3 (or 20), but I don't know why that is recommended and we would rather be compatible 
        //behaviorally with MRI.
        private final static int SIGNIFICANT_DIGITS_LIMIT = 30;
        // Maximal exponent is +308, minimal -308, -324 with subnormal float
        private final static int EXPONENT_DIGITS_LIMIT = 3;
        private final static int MAX_EXPONENT = (int) (Math.pow(10, EXPONENT_DIGITS_LIMIT)) - 1;
        // Max length is '-' + significant_digits + '.' + 'E' + '-' + exponent_digits
        // + 1 (first digit before point) + 1 (first decimal digit).
        private final static int MAX_LENGTH = SIGNIFICANT_DIGITS_LIMIT + EXPONENT_DIGITS_LIMIT + 6;

        public DoubleConverter() {}

        public void init(ByteList list, boolean isStrict) {
            bytes = list.getUnsafeBytes();
            index = list.begin();
            endIndex = index + list.length();
            this.isStrict = isStrict;
            // +2 for added exponent: E...
            // The algorithm trades digits for inc/dec exponent.
            // Worse case is adding E-1 when no exponent,
            // it trades one digit for 3 chars.
            chars = new char[Math.min(list.length()+2, MAX_LENGTH)];
            charsIndex = 0;
            significantDigitsProcessed = 0;
            adjustExponent = 0;
            wroteExponent = false;
            result = -1.0; // -1.0 is "parse String"
        }

        private byte next() {
            return bytes[index++];
        }

        /**
         * Shift back to previous character in the incoming bytes
         * @return false to indicate we are not in an EOS condition
         */
        private boolean previous() {
            index--;
            return false;
        }

        private boolean isEOS() {
            return index >= endIndex;
        }

        private boolean stopParsing() {
            index = endIndex;
            return true;
        }

        private static boolean isDigit(byte b) {
            return b >= '0' && b <= '9';
        }

        private static boolean isExponent(byte b) {
            return b == 'e' || b == 'E';
        }

        private static boolean isWhitespace(byte b) {
            return b == ' ' || (b <= 13 && b >= 9 && b != 11);
        }

        private void addToResult(byte b) {
            chars[charsIndex++] = (char) b;
        }

        private void addExponentToResult(int exponent) {
            String exp = Integer.toString(exponent);
            for (int i = 0; i < exp.length(); i++) {
                addToResult((byte) exp.charAt(i));
            }
        }

        private boolean eatUnderscores() {
            while (!isEOS()) {
                byte value = next();

                if (value != '_') {
                    previous();
                    return isEOS();
                } else if (isStrict) {
                    strictError();
                }
            }

            return true;
        }

        private double completeCalculation() {
            if (charsIndex == 0 || (charsIndex == 1 && chars[0] == '-')) { // "" or "-"
                strictError(); // Strict requires at least one digit.
                return 0.0; // Treated as 0.0 (not -0.0) in non-strict.
            } else if (isExponent((byte) chars[charsIndex - 1])) { // Covers 12.0efrog
                strictError();
                addExponentToResult(adjustExponent);
            } else if (isStrict && !isEOS()) {
                strictError(); // We know it is not whitespace at this point
            } else if (!wroteExponent && adjustExponent != 0) {
                addToResult((byte) 'E');
                addExponentToResult(adjustExponent);
            }

            return FAST_PARSER.parseDouble(new String(chars, 0, charsIndex));
        }

        static class LightweightNumberFormatException extends NumberFormatException {
            public LightweightNumberFormatException(String message) {
                super(message);
            }
            @Override
            public Throwable fillInStackTrace() {
                return this;
            }
        }
        
        private boolean strictError() {
            if (isStrict) {
                throw new LightweightNumberFormatException("does not meet strict criteria");
            } else {
                return true; // means EOS for non-strict
            }
        }

        /**
         * Everything runs in 1.9+ mode now, so the `is19` parameter is vestigial.
         * However, in order to maintain binary compatibility with extensions we can't
         * just change the signature either.
         */
        public double parse(ByteList list, boolean strict, boolean is19) {
            init(list, strict);

            if (skipWhitespace()) return completeCalculation();
            if (parseOptionalSign()) return completeCalculation();

            parseDigits();

            if (result != -1.0) { // abnormal result
                return result;
            }

            return completeCalculation();
        }

        /**
         * Consume initial whitespace and underscores so that next character
         * examined is not whitespace.  1.9 and strict do not allow leading
         * underscores.  Returns whether next position is at the end of the
         * string or not.
         *
         * Trivia: " _ _ _ _ 1".to_f == 1.0 in Ruby 1.8
         */
        private boolean skipWhitespace() {
            while (!isEOS()) {
                byte value = next();

                if (isWhitespace(value)) continue;
                return previous();
            }

            return true;
        }

        private boolean parseOptionalSign() {
            byte sign = next();

            if (sign == '-') {
                addToResult(sign);
            } else if (sign != '+') {
                previous();  // backup...not a sign-char
            }

            return isEOS();
        }

        private boolean parseDigits() {
            if (!isEOS()) {
                byte value = next();

                if (isDigit(value)) {
                    // Always add the first digit, in case it is the only digit
                    if (value != '0') {
                        significantDigitsProcessed++;
                    }
                    addToResult(value);
                } else if (value == '.') {
                    addToResult(value);
                    return parseDecimalDigits();
                } else {
                    return isEOS();
                }
            } else {
                strictError();
            }
            
            while (!isEOS()) {
                byte value = next();

                if (isDigit(value)) {
                    if (significantDigitsProcessed < SIGNIFICANT_DIGITS_LIMIT) {
                        // Ignore leading 0s
                        if (value != '0' || significantDigitsProcessed > 0) {
                            significantDigitsProcessed++;
                            addToResult(value);
                        }
                    } else {
                        adjustExponent++;
                    }
                } else if (value == '.') {
                    addToResult(value);
                    return parseDecimalDigits();
                } else if (value == '_') {
                    verifyNumberAfterUnderscore();
                } else if (isExponent(value)) {
                    addToResult(value);
                    return parseExponent();
                } else if (isWhitespace(value)) {
                    return skipWhitespace();
                } else {
                    return strictError();
                }
 
            }

            return true;
        }

        private boolean parseDecimalDigits() {
            if (isEOS()) return strictError();
            
            byte value = next();

            // Wonky, but 12._2e2 is 1200.0 and 12.__e2 is 12.0
            if (value == '_') {
                strictError();
                if (isEOS()) return strictError();
                value = next();
            }

            if (isDigit(value)) {
                if (significantDigitsProcessed < SIGNIFICANT_DIGITS_LIMIT) {
                    if (!(value == '0' && significantDigitsProcessed == 0)) { // not a leading 0
                        significantDigitsProcessed++;
                    }
                } else {
                    value = '0';
                }
                // Always add a digit after the .
                addToResult(value);
            } else {
                return strictError();
            } 

            while (!isEOS()) {
                value = next();

                if (isDigit(value)) {
                    if (significantDigitsProcessed < SIGNIFICANT_DIGITS_LIMIT) {
                        if (value == '0' && significantDigitsProcessed == 0) { // leading 0
                            adjustExponent--;
                        } else {
                            significantDigitsProcessed++;
                            addToResult(value);
                        }
                    } // otherwise ignore it
                } else if (isExponent(value)) {
                    addToResult(value);
                    return parseExponent();
                } else if (value == '_') {
                    verifyNumberAfterUnderscore();
                } else if (isWhitespace(value)) {
                    return skipWhitespace();
                } else {
                    return strictError();
                }
            }

            return true;
        }

        private boolean parseExponent() {
            if (eatUnderscores()) return isEOS();

            byte value = next();

            int exponent = 0;
            int digits = 0;
            boolean sawDigit = false; // digits does not count leading 0s
            boolean negative = false;

            if (value == '-') {
                negative = true;
            } else if (value != '+') {
                previous(); // backup...not a sign-char
            }

            while (!isEOS()) {
                value = next();

                if (isDigit(value)) {
                    sawDigit = true;
                    if (digits < EXPONENT_DIGITS_LIMIT) {
                        // Ignore leading 0s
                        if (value != '0' || digits > 0) {
                            digits++;
                            exponent = 10 * exponent + (value - '0');
                        }
                    } else {
                        return tooLargeExponent(chars[0] == '-', negative);
                    }
                } else if (isWhitespace(value)) {
                    skipWhitespace();
                    break;
                } else if (value == '_') {
                    verifyNumberAfterUnderscore();
                } else {
                    strictError();
                    stopParsing();
                    break;
                }
            } 

            if (isStrict && !sawDigit) strictError(); // an exponent needs a digit

            if (negative) {
                exponent = -exponent;
            }

            exponent += adjustExponent;

            if (-MAX_EXPONENT <= exponent && exponent <= MAX_EXPONENT) {
                addExponentToResult(exponent);
                wroteExponent = true;
                return isEOS(); // Exponent end of double...let's finish.
            } else {
                return tooLargeExponent(chars[0] == '-', negative);
            }
        }

        private boolean tooLargeExponent(boolean negativeFloat, boolean negativeExponent) {
            if (negativeExponent) {
                if (negativeFloat) {
                    result = -0.0;
                } else {
                    result = 0.0;
                }
            } else {
                if (negativeFloat) {
                    result = Double.NEGATIVE_INFINITY;
                } else {
                    result = Double.POSITIVE_INFINITY;
                }
            }
            verifyOnlyNumbers();
            return stopParsing();
        }

        private void verifyOnlyNumbers() {
            while (!isEOS()) {
                byte value = next();

                if (!isDigit(value)) throw new NumberFormatException();
            }
        }

        private void verifyNumberAfterUnderscore() {
            if (isStrict && (isEOS() || !isDigit(bytes[index]))) {
                strictError();
            }
        }
    }
}
