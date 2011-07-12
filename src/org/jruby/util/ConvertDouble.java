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
 * Copyright (C) 2010 Thomas E Enebo (tom.enebo@gmail.com)
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

public class ConvertDouble {
    /**
     * Converts supplied ByteList into a double.  strict-mode will not like
     * extra text non-numeric text or multiple sequention underscores.
     */
    public static final double byteListToDouble(ByteList bytes, boolean strict) {
        return new DoubleConverter().parse(bytes, strict, false);
    }

    /**
     * Converts supplied ByteList into a double.  This is almost exactly the
     * same as byteListToDouble, but leading underscores are no longer skipped
     * in 1.9 (e.g. __1 == 0.0 in 1.9 while 1.8 parses it as 1.0).
     */
    public static final double byteListToDouble19(ByteList bytes, boolean strict) {
        return new DoubleConverter().parse(bytes, strict, true);
    }

    public static class DoubleConverter {
        private byte[] bytes;
        private int index;
        private int endIndex;
        private boolean isStrict;
        private boolean is19;
        private char[] chars; // result string we use to parse.
        private int charsIndex;

        public DoubleConverter() {}

        public void init(ByteList list, boolean isStrict, boolean is19) {
            bytes = list.getUnsafeBytes();
            index = list.begin();
            endIndex = index + list.length();
            this.isStrict = isStrict;
            this.is19 = is19;
            chars = new char[list.length()+1]; // +1 for implicit '0' for terminating 'E'
            charsIndex = 0;
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

        private boolean isDigit(byte b) {
            return b >= '0' && b <= '9';
        }

        private boolean isEOS() {
            return index >= endIndex;
        }

        private boolean isExponent(byte b) {
            return b == 'e' || b == 'E';
        }

        private boolean isWhitespace(byte b) {
            return b == ' ' || (b <= 13 && b >= 9 && b != 11);
        }

        private void addToResult(byte b) {
            chars[charsIndex++] = (char) b;
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
            // ENEBO: Not sure about the '-' check here.
            if (charsIndex == 0 || (charsIndex == 1 && chars[0] == '-')) {
                if (isStrict) strictError(); // Strict requires at least one digit.
                return 0.0;
            } else if (isExponent((byte) chars[charsIndex - 1])) { // Covers 12.0efrog
                if (isStrict) strictError();
                addToResult((byte) '0');
            } else if (isStrict && !isEOS()) {
                strictError(); // We know it is not whitespace at this point
            }

            return SafeDoubleParser.parseDouble(new String(chars));
        }
        
        private void strictError() {
            throw new NumberFormatException("does not meet strict criteria");
        }

        public double parse(ByteList list, boolean strict, boolean is19) {
            init(list, strict, is19);

            if (skipWhitespace()) return completeCalculation();
            if (parseOptionalSign()) return completeCalculation();

            parseDigits();

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
                if (value != '_' || isStrict || is19) return previous();
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
                    addToResult(value);
                } else if (value == '.') {
                    addToResult(value);
                    return parseDecimalDigits();
                } else {
                    return isEOS();
                }
            } else if (isStrict) {
                strictError();
            }
            
            while (!isEOS()) {
                byte value = next();

                if (isDigit(value)) {
                    addToResult(value);
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
                    if (isStrict) strictError();
                    
                    return isEOS();
                }
 
            }

            return true;
        }

        private boolean parseDecimalDigits() {
            if (isEOS()) {
                if (isStrict) strictError();
                
                return true;
            }
            
            byte value = next();

            if (isDigit(value)) {
                addToResult(value);
            } else if (value == '_') {
                if (isStrict) strictError();
                while (!isEOS()) {    // Wonky, but 12._2e2 is 1200.0 and 12.__e2 is 12.0
                    value = next();

                    if (isDigit(value)) {
                        addToResult(value);
                        break;
                    } else if (value != '_') {
                        return true;
                    }
                }
            } else {
                if (isStrict) strictError();
                return true; // Found garbage must be at end of string.
            } 

            while (!isEOS()) {
                value = next();

                if (isDigit(value)) {
                    addToResult(value);
                } else if (isExponent(value)) {
                    addToResult(value);
                    return parseExponent();
                } else if (value == '_') {
                    verifyNumberAfterUnderscore();
                } else if (isWhitespace(value)) {
                    return skipWhitespace();
                } else {
                    if (isStrict) strictError();

                    return true;
                }
            }

            return true;
        }

        private boolean parseExponent() {
            if (eatUnderscores()) return isEOS();

            byte value = next();

            if (value == '-' || value == '+') {
                addToResult(value);
            } else {
                previous();
            }

            while (!isEOS()) {
                value = next();

                if (isDigit(value)) {
                    addToResult(value);
                } else if (isWhitespace(value)) {
                    return skipWhitespace();
                } else if (value == '_') {
                    verifyNumberAfterUnderscore();
                } else {
                    if (isStrict) strictError();

                    return true;
                }
            } 

            return true; // Exponent end of double...let's finish.
        }

        private void verifyNumberAfterUnderscore() {
            if (isStrict) {
                if (isEOS()) strictError();

                byte value = next();
                
                if (!isDigit(value)) {
                    previous();
                    strictError();
                } else {
                    addToResult(value);
                }
            }
        }
    }
}
