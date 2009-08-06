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
 * Copyright (C) 2007 William N. Dortch <bill.dortch@gmail.com>
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
import java.math.BigInteger;

import org.jruby.RubyNumeric.InvalidIntegerException;
import org.jruby.RubyNumeric.NumberTooLargeException;

/**
 * @author Bill Dortch
 * 
 * Primitive conversions adapted from java.lang.Integer/Long/Double (C) Sun Microsystems, Inc.
 *
 */
public class Convert {
    /**
     * Converts a ByteList containing a RubyString representation of a double
     * value to a double.  Equivalent to Double.parseDouble(String s), but accounts for
     * embedded underscore characters, as permitted in Ruby strings (single underscores
     * allowed between digits in strict mode, multiple in non-strict mode).
     *  
     * @param bytes the ByteList containing the RubyString value to convert
     * @param strict if true, strict rules (as required by Float(str)) are enforced;
     *               otherwise, the laxer rules of str.to_f are employed.
     * @return the converted double value
     */
    public static final double byteListToDouble(ByteList bytes, boolean strict) {
        return byteArrayToDouble(bytes.unsafeBytes(), bytes.begin(), bytes.length(), strict);
    }
    public static final double byteListToDouble(ByteList bytes) {
        return byteArrayToDouble(bytes.unsafeBytes(), bytes.begin(), bytes.length(), false);
    }
    /**
     * Converts a byte array containing a RubyString representation of a double
     * value to a double.  Equivalent to Double.parseDouble(String s), but accounts for
     * embedded underscore characters, as permitted in Ruby strings (single underscores
     * allowed between digits in strict mode, multiple in non-strict mode).
     *  
     * @param bytes the array containing the RubyString value to convert
     * @param buflen the length of the array to be used
     * @param strict if true, strict rules (as required by Float(str)) are enforced;
     *               otherwise, the laxer rules of str.to_f are employed.
     * @return the converted double value
     */
    public static final double byteArrayToDouble(byte[] bytes, int begin, int buflen, boolean strict) {
        // Simple cases  ( abs(exponent) <= 22 [up to 37 depending on significand length])
        // are converted directly, which is considerably faster than creating a Java
        // String and passing it to Double.parseDouble() (which in turn passes it to
        // sun.misc.FloatingDecimal); the latter approach involves 5 object allocations
        // (3 arrays + String + FloatingDecimal) and 3 array copies, two of them one byte/char
        // at a time (here and in FloatingDecimal).
        // However, the latter approach is employed for more difficult cases (generally
        // speaking, those that require rounding). (The code for the difficult cases is 
        // quite involved; see sun.misc.FloatingDecimal.java if you're interested.)

        // states
        final int SCOMPLETE            =  0;
        final int SBEGIN               =  1; // remove leading whitespace (includes _ for lax)
        final int SSIGN                =  2; // get sign, if any
        
        // optimistic pass - calculate value as digits are processed
        final int SOPTDIGIT            =  3; // digits - lax rules
        final int SOPTDECDIGIT         =  4; // decimal digits - lax rules
        final int SOPTEXP              =  9; // exponent sign/digits - lax rules
        final int SOPTDIGIT_STRICT     =  6; // digits - strict rules
        final int SOPTDECDIGIT_STRICT  =  7; // decimal digits - strict rules
        final int SOPTEXP_STRICT       =  8; // exponent sign/digits - strict rules
        final int SOPTCALC             =  5; // complete calculation if possible

        // fallback pass - gather chars into array and pass to Double.parseDouble()
        final int SDIGIT               = 10; // digits - lax rules
        final int SDECDIGIT            = 11; // decimal digits - lax rules
        final int SEXP                 = 12; // exponent sign/digits - lax rules
        final int SDIGIT_STRICT        = 13; // digits - strict rules
        final int SDECDIGIT_STRICT     = 14; // decimal digits - strict rules
        final int SEXP_STRICT          = 15; // exponent sign/digits - strict rules

        final int SERR_NOT_STRICT      = 16;

        // largest abs(exponent) we can (potentially) handle without
        // calling Double.parseDouble() (aka sun.misc.FloatingDecimal)
        final int MAX_EXP = MAX_DECIMAL_DIGITS + MAX_SMALL_10; // (37)
      
        if (bytes == null) {
            throw new IllegalArgumentException("null bytes");
        }
        if (buflen < 0 || buflen > bytes.length) {
            throw new IllegalArgumentException("invalid buflen specified");
        }
        // TODO: get rid of this (lax returns 0.0, strict will throw)
        if (buflen == 0) {
            throw new NumberFormatException();
        }
        int i = begin;
        buflen += begin;
        byte ival = -1;
        boolean negative = false;

        // fields used for direct (optimistic) calculation
        int nDigits = 0;         // number of significant digits, updated as parsed
        int nTrailingZeroes = 0; // zeroes that may go to significand or exponent
        int decPos = -1;         // offset of decimal pt from start (-1 -> no decimal)
        long significand = 0;    // significand, updated as parsed
        int exponent = 0;        // exponent, updated as parsed
        
        // fields used for fallback (send to Double.parseDouble())
        int startPos = 0;        // start of digits (or . if no leading digits)
        char[] chars = null;
        int offset = 0;
        int lastValidOffset = 0;

        int state = SBEGIN;
        while (state != SCOMPLETE) {
        states:
            switch(state) {
            case SBEGIN:
                if (strict) {
                    for (; i < buflen && isWhitespace(bytes[i]); i++) ;
                } else {
                    for (; i < buflen && (isWhitespace(ival = bytes[i]) || ival == '_'); i++) ;
                }
                if ( i >= buflen) {
                    state = strict ? SERR_NOT_STRICT : SCOMPLETE;
                    break;
                }
                // drop through for sign
            case SSIGN:
                switch(bytes[i]) {
                case '-':
                    negative = true;
                case '+':
                    if (++i >= buflen) {
                        // TODO: turn off the negative? will return -0.0 in lax mode
                        state = strict ? SERR_NOT_STRICT : SCOMPLETE;
                        break states;
                    }
                } // switch
                startPos = i; // will use this if we have to go back the slow way
                if (strict) {
                    state = SOPTDIGIT_STRICT;
                    break;
                }
                // drop through for non-strict digits
            case SOPTDIGIT:
                // first char must be digit or decimal point
                switch(ival = bytes[i++]) {
                case '0':
                    // ignore leading zeroes
                    break; // switch
                case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    significand = (long)((int)ival-(int)'0');
                    nDigits = 1;
                    break; // switch
                case '.':
                    state = SOPTDECDIGIT;
                    break states;
                default:
                    // no digits, go calc (will return +/- 0.0 for lax)
                    state = SOPTCALC;
                    break states;
                } // switch
                for ( ; i < buflen ;  ) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        // ignore leading zeroes
                        if (nDigits > 0) {
                            // just save a count of zeroes for now; if no digit
                            // ends up following them, they'll be applied to the
                            // exponent rather than the significand (and our max
                            // length for optimistic calc).
                            nTrailingZeroes++;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                         // ok, got a non-zero, have to own up to our horded zeroes
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            // oh, well, it was worth a try. go let
                            // Double/FloatingDecimal handle it 
                            state = SDIGIT;
                            break states;
                        }
                    case '.':
                        state = SOPTDECDIGIT;
                        break states;
                    case 'e':
                    case 'E':
                        state = SOPTEXP;
                        break states;
                    case '_':
                        // ignore
                        break; // switch
                    default:
                        // end of parseable data, go to calc
                        state = SOPTCALC;
                        break states;
                        
                    } // switch
                } // for
                state = SOPTCALC;
                break;

            case SOPTDECDIGIT:
                decPos = nDigits + nTrailingZeroes;
                for ( ; i < buflen && bytes[i] == '_'; i++ ) ;
                // first non_underscore char must be digit
                if (i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        if (nDigits > 0) {
                            nTrailingZeroes++;
                        } else {
                            exponent--;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            state = SDIGIT;
                            break states;
                        }
                    default:
                        // no dec digits, end of parseable data, go to calc
                        state = SOPTCALC;
                        break states;
                        
                    } // switch
                } // if
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        if (nDigits > 0) {
                            nTrailingZeroes++;
                        } else {
                            exponent--;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            state = SDIGIT;
                            break states;
                        }
                    case 'e':
                    case 'E':
                        state = SOPTEXP;
                        break states;
                    case '_':
                        // ignore
                        break; // switch
                    default:
                        // end of parseable data, go to calc
                        state = SOPTCALC;
                        break states;
                    } // switch
                } // for
                // no exponent, so drop through for calculation
            case SOPTCALC:
                // calculation for simple (and typical) case,
                // adapted from sun.misc.FloatingDecimal
                if (nDigits == 0) {
                    if (i + 1 < buflen) {
                        if ((ival == 'n' || ival == 'N') && 
                                (bytes[i] == 'a' || bytes[i] == 'A') &&
                                (bytes[i+1] == 'n' || bytes[i+1] == 'N')) {
                            return Double.NaN;
                        } else if ((ival == 'i' || ival == 'I') && 
                                (bytes[i] == 'n' || bytes[i] == 'N') &&
                                (bytes[i+1] == 'f' || bytes[i+1] == 'F')) {
                            return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                        }
                    }
                    return negative ? -0.0d : 0.0d;
                }
                if (decPos < 0) {
                    exponent += nTrailingZeroes;
                } else {
                    exponent += decPos - nDigits;
                }
                double dValue = (double)significand;
                if (exponent == 0 || dValue == 0.0) {
                    return negative ? -dValue : dValue;
                } else if ( exponent >= 0 ){
                    if ( exponent <= MAX_SMALL_10 ){
                        dValue *= SMALL_10_POWERS[exponent];
                        return negative ? -dValue : dValue;
                    }
                    int slop = MAX_DECIMAL_DIGITS - nDigits;
                    if ( exponent <= MAX_SMALL_10+slop ){
                        dValue = (dValue * SMALL_10_POWERS[slop]) * SMALL_10_POWERS[exponent-slop];
                        return negative ? -dValue : dValue;
                    }
                } else {
                    // TODO: it's not clear to me why, in FloatingDecimal, the
                    // "slop" calculation performed above for positive exponents
                    // isn't used for negative exponents as well. Will find out...
                    if ( exponent >= -MAX_SMALL_10 ){
                        dValue = dValue / SMALL_10_POWERS[-exponent];
                        return negative ? -dValue : dValue;
                    }
                }
                // difficult case, send to Double/FloatingDecimal
                state = SDIGIT;
                break;
                
            case SOPTEXP:
            {
                // lax (str.to_f) allows underscores between e/E and sign
                for ( ; i < buflen && bytes[i] == '_' ; i++ ) ;
                if (i >= buflen) {
                    state = SOPTCALC;
                    break;
                }
                int expSign = 1;
                int expSpec = 0;
                switch (bytes[i]) {
                case '-':
                    expSign = -1;
                case '+':
                    if (++i >= buflen) {
                        state = SOPTCALC;
                        break states;
                    }
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if ((expSpec = expSpec * 10 + ((int)ival-(int)'0')) >= MAX_EXP) {
                            // too big for us
                            state = SDIGIT;
                            break states;
                        }
                        break; //switch
                    case '_':
                        break; //switch
                    default:
                        exponent += expSign * expSpec;
                        state = SOPTCALC;
                        break states;
                    }
                }
                exponent += expSign * expSpec;
                state = SOPTCALC;
                break;
            } // block

            case SOPTDIGIT_STRICT:
                // first char must be digit or decimal point
                switch(ival = bytes[i++]) {
                case '0':
                    break; // switch
                case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    significand = (long)((int)ival-(int)'0');
                    nDigits = 1;
                    break; // switch
                case '.':
                    state = SOPTDECDIGIT_STRICT;
                    break states;
                default:
                    // no digits, error
                    state = SERR_NOT_STRICT;
                    break states;
                }
                for ( ; i < buflen ;  ) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        if (nDigits > 0) {
                            nTrailingZeroes++;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            state = SDIGIT;
                            break states;
                        }
                    case '.':
                        state = SOPTDECDIGIT_STRICT;
                        break states;
                    case 'e':
                    case 'E':
                        state = SOPTEXP_STRICT;
                        break states;
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; // switch
                    default:
                        // only whitespace allowed after value for strict
                        for ( --i; i < buflen && isWhitespace(bytes[i]); i++ );
                        state = i < buflen ? SERR_NOT_STRICT : SOPTCALC; 
                        break states;
                    } // switch
                } // for
                // no more data, OK for strict to go calc
                state = SOPTCALC;
                break;

            case SOPTDECDIGIT_STRICT:
                decPos = nDigits + nTrailingZeroes;
                // first char must be digit
                if (i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        if (nDigits > 0) {
                            nTrailingZeroes++;
                        } else {
                            exponent--;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            state = SDIGIT;
                            break states;
                        }
                    default:
                        // no dec digits after '.', error for strict
                        state = SERR_NOT_STRICT;
                        break states;
                        
                    } // switch
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        if (nDigits > 0) {
                            nTrailingZeroes++;
                        } else {
                            exponent--;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            state = SDIGIT;
                            break states;
                        }
                    case 'e':
                    case 'E':
                        state = SOPTEXP_STRICT;
                        break states;
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; // switch
                    default:
                        // only whitespace allowed after value for strict
                        for ( --i; i < buflen && isWhitespace(bytes[i]); i++);
                        state = i < buflen ? SERR_NOT_STRICT : SOPTCALC; 
                        break states;
                    } // switch
                } // for
                // no more data, OK for strict to go calc
                state = SOPTCALC;
                break;
            
            case SOPTEXP_STRICT:
            {
                int expSign = 1;
                int expSpec = 0;

                if ( i < buflen) {
                    switch (bytes[i]) {
                    case '-':
                        expSign = -1;
                    case '+':
                        if (++i >= buflen) {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                    }
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                // must be at least one digit for strict
                if ( i < buflen ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        expSpec = (int)ival-(int)'0';
                        break; //switch
                    default:
                        state = SERR_NOT_STRICT;
                        break states;
                    }
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if ((expSpec = expSpec * 10 + ((int)ival-(int)'0')) >= MAX_EXP) {
                            // too big for us
                            state = SDIGIT;
                            break states;
                        }
                        break; //switch
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; //switch
                    default:
                        exponent += expSign * expSpec;
                        // only whitespace allowed after value for strict
                        for ( --i; i < buflen && isWhitespace(bytes[i]);  i++);
                        state = i < buflen ? SERR_NOT_STRICT : SOPTCALC; 
                        break states;
                    } // switch
                } // for
                exponent += expSign * expSpec;
                state = SOPTCALC;
                break;
            } // block
                
            // fallback, copy non-whitespace chars to char buffer and send
            // to Double.parseDouble() (front for sun.misc.FloatingDecimal)
            case SDIGIT:
                i = startPos;
                if (negative) {
                    chars = new char[buflen - i + 1];
                    chars[0] = '-';
                    offset = 1;
                } else {
                    chars = new char[buflen - i];
                }
                if (strict) {
                    state = SDIGIT_STRICT;
                    break;
                }
                // first char must be digit or decimal point
                if (i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        // ignore leading zeroes
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case '.':
                        state = SDECDIGIT;
                        break states;
                    default:
                        state = SCOMPLETE;
                        break states;
                    } // switch
                } // if
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case '.':
                        state = SDECDIGIT;
                        break states;
                    case 'e':
                    case 'E':
                        state = SEXP;
                        break states;
                    case '_':
                        break; // switch
                    default:
                        state = SCOMPLETE;
                        break states;
                    } // switch
                } // for
                state = SCOMPLETE;
                break;
                
            case SDECDIGIT:
                chars[offset++] = '.';
                for ( ; i < buflen && bytes[i] == '_'; i++) ;
                if ( i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    default:
                        state = SCOMPLETE;
                        break states;
                    } // switch
                } // if
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case 'e':
                    case 'E':
                        state = SEXP;
                        break states;
                    case '_':
                        break; // switch
                    default:
                        state = SCOMPLETE;
                        break states;
                    } // switch
                } // for
                state = SCOMPLETE;
                break;

            case SEXP:
                chars[offset++] = 'E';
                for ( ; i < buflen && bytes[i] == '_'; i++) ;
                if (i >= buflen) {
                    state = SCOMPLETE;
                    break;
                }
                switch(bytes[i]) {
                case '-':
                case '+':
                    chars[offset++] = (char)bytes[i];
                    if (++i >= buflen) {
                        state = SCOMPLETE;
                        break states;
                    }
                }
                for ( ; i < buflen; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break;
                    case '_':
                        break;
                    default:
                        state = SCOMPLETE;
                        break states;
                    }
                }
                state = SCOMPLETE;
                break;
            
            case SDIGIT_STRICT:
                // first char must be digit or decimal point
                if (i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        // ignore leading zeroes
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case '.':
                        state = SDECDIGIT_STRICT;
                        break states;
                    default:
                        state = SERR_NOT_STRICT;
                        break states;
                    } // switch
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case '.':
                        state = SDECDIGIT_STRICT;
                        break states;
                    case 'e':
                    case 'E':
                        state = SEXP_STRICT;
                        break states;
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; //switch
                    default:
                        // only whitespace allowed after value for strict
                        for ( --i; i < buflen && isWhitespace(bytes[i]);  i++) ;
                        state = i < buflen ? SERR_NOT_STRICT : SCOMPLETE; 
                        break states;
                    } // switch
                } // for
                state = SCOMPLETE;
                break;
                
            case SDECDIGIT_STRICT:
                chars[offset++] = '.';
                if ( i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    default:
                        state = SERR_NOT_STRICT;
                        break states;
                    } // switch
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case 'e':
                    case 'E':
                        state = SEXP_STRICT;
                        break states;
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; //switch
                    default:
                        for ( --i; i < buflen && isWhitespace(bytes[i]);  i++) ;
                        state = i < buflen ? SERR_NOT_STRICT : SCOMPLETE; 
                        break states;
                    } // switch
                } // for
                state = SCOMPLETE;
                break;

            case SEXP_STRICT:
                chars[offset++] = 'E';
                if ( i < buflen ) {
                    switch (bytes[i]) {
                    case '-':
                    case '+':
                        chars[offset++] = (char)bytes[i];
                        if (++i >= buflen) {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                    }
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                // must be at least one digit for strict
                if ( i < buflen ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; //switch
                    default:
                        state = SERR_NOT_STRICT;
                        break states;
                    }
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break;
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; //switch
                    default:
                        for ( --i; i < buflen && isWhitespace(bytes[i]);  i++) ;
                        state = i < buflen ? SERR_NOT_STRICT : SCOMPLETE; 
                        break states;
                    }
                }
                state = SCOMPLETE;
                break;
            
            case SERR_NOT_STRICT:
                throw new NumberFormatException("does not meet strict criteria");
                
            } // switch
        } //while
        if (chars == null || lastValidOffset == 0) {
            return 0.0;
        } else {
            return Double.parseDouble(new String(chars,0,lastValidOffset));
        }
    }
    
    private static final boolean isWhitespace(final byte b) {
        return b == ' ' || (b <= 13 && b >= 9 && b != 11);
    }

    private static final long[] LONG_10_POWERS = {
      1L,
      10L,
      100L,
      1000L,
      10000L,
      100000L,
      1000000L,
      10000000L,
      100000000L,
      1000000000L,
      10000000000L,
      100000000000L,
      1000000000000L,
      10000000000000L,
      100000000000000L,
      1000000000000000L,
      10000000000000000L,
      100000000000000000L
    };
    
    /*
     * All the positive powers of 10 that can be
     * represented exactly in double/float.
     * (From sun.misc.FloatingDecimal.java)
     */
    private static final double[] SMALL_10_POWERS = {
        1.0e0,
        1.0e1, 1.0e2, 1.0e3, 1.0e4, 1.0e5,
        1.0e6, 1.0e7, 1.0e8, 1.0e9, 1.0e10,
        1.0e11, 1.0e12, 1.0e13, 1.0e14, 1.0e15,
        1.0e16, 1.0e17, 1.0e18, 1.0e19, 1.0e20,
        1.0e21, 1.0e22
    };
    private static final int MAX_SMALL_10 = SMALL_10_POWERS.length - 1;
    private static final int  MAX_DECIMAL_DIGITS = 15;

}
