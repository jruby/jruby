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
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInteger;
import org.jruby.RubyKernel;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.builtin.IRubyObject;


/**
 * @author Bill Dortch
 *
 */
public class Sprintf {
    private static final int FLAG_NONE        = 0;
    private static final int FLAG_SPACE       = 1 << 0;
    private static final int FLAG_ZERO        = 1 << 1;
    private static final int FLAG_PLUS        = 1 << 2;
    private static final int FLAG_MINUS       = 1 << 3;
    private static final int FLAG_SHARP       = 1 << 4;
    private static final int FLAG_WIDTH       = 1 << 5;
    private static final int FLAG_PRECISION   = 1 << 6;
    
    private static final byte[] PREFIX_OCTAL     = {'0'};
    private static final byte[] PREFIX_HEX_LC    = {'0','x'};
    private static final byte[] PREFIX_HEX_UC    = {'0','X'};
    private static final byte[] PREFIX_BINARY_LC = {'0','b'};
    private static final byte[] PREFIX_BINARY_UC = {'0','B'};
    
    private static final byte[] PREFIX_NEGATIVE = {'.','.'};
    
    private static final byte[] NAN_VALUE       = {'N','a','N'};
    private static final byte[] INFINITY_VALUE  = {'I','n','f'};
       
    private static final BigInteger BIG_32 = BigInteger.valueOf(((long)Integer.MAX_VALUE + 1L) << 1);
    private static final BigInteger BIG_64 = BIG_32.shiftLeft(32);
    private static final BigInteger BIG_MINUS_32 = BigInteger.valueOf((long)Integer.MIN_VALUE << 1);
    private static final BigInteger BIG_MINUS_64 = BIG_MINUS_32.shiftLeft(32);

    private static final String ERR_MALFORMED_FORMAT = "malformed format string";
    private static final String ERR_MALFORMED_NUM = "malformed format string - %[0-9]";
    private static final String ERR_MALFORMED_DOT_NUM = "malformed format string - %.[0-9]";
    private static final String ERR_MALFORMED_STAR_NUM = "malformed format string - %*[0-9]";
    private static final String ERR_ILLEGAL_FORMAT_CHAR = "illegal format character - %";
    
    
    private static final class Args {
        private final Ruby runtime;
        private final Locale locale;
        private final IRubyObject rubyObject;
        private final RubyArray rubyArray;
        private final int length;
        private int unnumbered; // last index (+1) accessed by next()
        private int numbered;   // last index (+1) accessed by get()
        
        Args(Locale locale, IRubyObject rubyObject) {
            if (rubyObject == null) throw new IllegalArgumentException("null IRubyObject passed to sprintf");
            this.locale = locale == null ? Locale.getDefault() : locale;
            this.rubyObject = rubyObject;
            if (rubyObject instanceof RubyArray) {
                this.rubyArray = ((RubyArray)rubyObject);
                this.length = rubyArray.size();
            } else {
                this.length = 1;
                this.rubyArray = null;
            }
            this.runtime = rubyObject.getRuntime();
        }
        
        Args(IRubyObject rubyObject) {
            this(Locale.getDefault(),rubyObject);
        }

        // temporary hack to handle non-Ruby values
        // will come up with better solution shortly
        Args(Ruby runtime, long value) {
            this(RubyFixnum.newFixnum(runtime,value));
        }
        
        void raiseArgumentError(String message) {
            throw runtime.newArgumentError(message);
        }
        
        void warn(ID id, String message) {
            runtime.getWarnings().warn(id, message);
        }
        
        void warning(ID id, String message) {
            if (runtime.isVerbose()) runtime.getWarnings().warning(id, message);
        }
        
        IRubyObject next() {
            // this is the order in which MRI does these two tests
            if (numbered > 0) raiseArgumentError("unnumbered" + (unnumbered + 1) + "mixed with numbered");
            if (unnumbered >= length) raiseArgumentError("too few arguments");
            IRubyObject object = rubyArray == null ? rubyObject : rubyArray.eltInternal(unnumbered);
            unnumbered++;
            return object;
        }
        
        IRubyObject get(int index) {
            // this is the order in which MRI does these tests
            if (unnumbered > 0) raiseArgumentError("numbered("+numbered+") after unnumbered("+unnumbered+")");
            if (index < 0) raiseArgumentError("invalid index - " + (index + 1) + '$');
            if (index >= length) raiseArgumentError("too few arguments");
            numbered = index + 1;
            return rubyArray == null ? rubyObject : rubyArray.eltInternal(index);
        }
        
        IRubyObject getNth(int formatIndex) {
            return get(formatIndex - 1);
        }
        
        int nextInt() {
            return intValue(next());
        }
        
        int getInt(int index) {
            return intValue(get(index));
        }

        int getNthInt(int formatIndex) {
            return intValue(get(formatIndex - 1));
        }
        
        int intValue(IRubyObject obj) {
            if (obj instanceof RubyNumeric) return (int)((RubyNumeric)obj).getLongValue();

            // basically just forcing a TypeError here to match MRI
            obj = TypeConverter.convertToType(obj, obj.getRuntime().getFixnum(), "to_int", true);
            return (int)((RubyFixnum)obj).getLongValue();
        }
        
        byte getDecimalSeparator() {
            // not saving DFS instance, as it will only be used once (at most) per call
            return (byte)new DecimalFormatSymbols(locale).getDecimalSeparator();
        }
    } // Args

    // static methods only
    private Sprintf () {}
    
    // Special form of sprintf that returns a RubyString and handles
    // tainted strings correctly.
    public static boolean sprintf(ByteList to, Locale locale, CharSequence format, IRubyObject args) {
        return rubySprintfToBuffer(to, format, new Args(locale, args));
    }

    // Special form of sprintf that returns a RubyString and handles
    // tainted strings correctly. Version for 1.9.
    public static boolean sprintf1_9(ByteList to, Locale locale, CharSequence format, IRubyObject args) {
        return rubySprintfToBuffer(to, format, new Args(locale, args), false);
    }

    public static boolean sprintf(ByteList to, CharSequence format, IRubyObject args) {
        return rubySprintf(to, format, new Args(args));
    }

    public static boolean sprintf(Ruby runtime, ByteList to, CharSequence format, int arg) {
        return rubySprintf(to, format, new Args(runtime, (long)arg));
    }

    public static boolean sprintf(ByteList to, RubyString format, IRubyObject args) {
        return rubySprintf(to, format.getByteList(), new Args(args));
    }

    private static boolean rubySprintf(ByteList to, CharSequence charFormat, Args args) {
        return rubySprintfToBuffer(to, charFormat, args);
    }

    private static boolean rubySprintfToBuffer(ByteList buf, CharSequence charFormat, Args args) {
        return rubySprintfToBuffer(buf, charFormat, args, true);
    }

    private static boolean rubySprintfToBuffer(ByteList buf, CharSequence charFormat, Args args, boolean usePrefixForZero) {
        boolean tainted = false;
        final byte[] format;

        int offset;
        int length;
        int start;
        int mark;        

        if (charFormat instanceof ByteList) {
            ByteList list = (ByteList)charFormat;
            format = list.unsafeBytes();
            int begin = list.begin(); 
            offset = begin;
            length = begin + list.length();
            start = begin;
            mark = begin;
        } else {
            format = stringToBytes(charFormat, false);
            offset = 0;
            length = charFormat.length();
            start = 0;
            mark = 0;             
        }

        while (offset < length) {
            start = offset;
            for ( ; offset < length && format[offset] != '%'; offset++) ;

            if (offset > start) {
                buf.append(format,start,offset-start);
                start = offset;
            }
            if (offset++ >= length) break;

            IRubyObject arg = null;
            int flags = 0;
            int width = 0;
            int precision = 0;
            int number = 0;
            byte fchar = 0;
            boolean incomplete = true;
            for ( ; incomplete && offset < length ; ) {
                switch (fchar = format[offset]) {
                default:
                    if (fchar == '\0' && flags == FLAG_NONE) {
                        // MRI 1.8.6 behavior: null byte after '%'
                        // leads to "%" string. Null byte in
                        // other places, like "%5\0", leads to error.
                        buf.append('%');
                        buf.append(fchar);
                        incomplete = false;
                        offset++;
                        break;
                    } else if (isPrintable(fchar)) {
                        raiseArgumentError(args,"malformed format string - %" + (char)fchar);
                    } else {
                        raiseArgumentError(args,ERR_MALFORMED_FORMAT);
                    }
                    break;

                case ' ':
                    flags |= FLAG_SPACE;
                    offset++;
                    break;
                case '0':
                    flags |= FLAG_ZERO;
                    offset++;
                    break;
                case '+':
                    flags |= FLAG_PLUS;
                    offset++;
                    break;
                case '-':
                    flags |= FLAG_MINUS;
                    offset++;
                    break;
                case '#':
                    flags |= FLAG_SHARP;
                    offset++;
                    break;
                case '1':case '2':case '3':case '4':case '5':
                case '6':case '7':case '8':case '9':
                    // MRI doesn't flag it as an error if width is given multiple
                    // times as a number (but it does for *)
                    number = 0;
                    for ( ; offset < length && isDigit(fchar = format[offset]); offset++) {
                        number = extendWidth(args, number, fchar);
                    }
                    checkOffset(args,offset,length,ERR_MALFORMED_NUM);
                    if (fchar == '$') {
                        if (arg != null) {
                            raiseArgumentError(args,"value given twice - " + number + "$");
                        }
                        arg = args.getNth(number);
                        offset++;
                    } else {
                        width = number;
                        flags |= FLAG_WIDTH;
                    }
                    break;
                
                case '*':
                    if ((flags & FLAG_WIDTH) != 0) {
                        raiseArgumentError(args,"width given twice");
                    }
                    flags |= FLAG_WIDTH;
                    // TODO: factor this chunk as in MRI/YARV GETASTER
                    checkOffset(args,++offset,length,ERR_MALFORMED_STAR_NUM);
                    mark = offset;
                    number = 0;
                    for ( ; offset < length && isDigit(fchar = format[offset]); offset++) {
                        number = extendWidth(args,number,fchar);
                    }
                    checkOffset(args,offset,length,ERR_MALFORMED_STAR_NUM);
                    if (fchar == '$') {
                        width = args.getNthInt(number);
                        if (width < 0) {
                            flags |= FLAG_MINUS;
                            width = -width;
                        }
                        offset++;
                    } else {
                        width = args.nextInt();
                        if (width < 0) {
                            flags |= FLAG_MINUS;
                            width = -width;
                        }
                        // let the width (if any), get processed in the next loop,
                        // so any leading 0 gets treated correctly 
                        offset = mark;
                    }
                    break;
                
                case '.':
                    if ((flags & FLAG_PRECISION) != 0) {
                        raiseArgumentError(args,"precision given twice");
                    }
                    flags |= FLAG_PRECISION;
                    checkOffset(args,++offset,length,ERR_MALFORMED_DOT_NUM);
                    fchar = format[offset];
                    if (fchar == '*') {
                        // TODO: factor this chunk as in MRI/YARV GETASTER
                        checkOffset(args,++offset,length,ERR_MALFORMED_STAR_NUM);
                        mark = offset;
                        number = 0;
                        for ( ; offset < length && isDigit(fchar = format[offset]); offset++) {
                            number = extendWidth(args,number,fchar);
                        }
                        checkOffset(args,offset,length,ERR_MALFORMED_STAR_NUM);
                        if (fchar == '$') {
                            precision = args.getNthInt(number);
                            if (precision < 0) {
                                flags &= ~FLAG_PRECISION;
                            }
                            offset++;
                        } else {
                            precision = args.nextInt();
                            if (precision < 0) {
                                flags &= ~FLAG_PRECISION;
                            }
                            // let the width (if any), get processed in the next loop,
                            // so any leading 0 gets treated correctly 
                            offset = mark;
                        }
                    } else {
                        number = 0;
                        for ( ; offset < length && isDigit(fchar = format[offset]); offset++) {
                            number = extendWidth(args,number,fchar);
                        }
                        checkOffset(args,offset,length,ERR_MALFORMED_DOT_NUM);
                        precision = number;
                    }
                    break;

                case '\n':
                    offset--;
                case '%':
                    if (flags != FLAG_NONE) {
                        raiseArgumentError(args,ERR_ILLEGAL_FORMAT_CHAR);
                    }
                    buf.append('%');
                    offset++;
                    incomplete = false;
                    break;

                case 'c': {
                    if (arg == null) arg = args.next();
                    
                    int c = 0;
                    // MRI 1.8.5-p12 doesn't support 1-char strings, but
                    // YARV 0.4.1 does. I don't think it hurts to include
                    // this; sprintf('%c','a') is nicer than sprintf('%c','a'[0])
                    if (arg instanceof RubyString) {
                        ByteList bytes = ((RubyString)arg).getByteList();
                        if (bytes.length() == 1) {
                            c = bytes.unsafeBytes()[bytes.begin()];
                        } else {
                            raiseArgumentError(args,"%c requires a character");
                        }
                    } else {
                        c = args.intValue(arg);
                    }
                    if ((flags & FLAG_WIDTH) != 0 && width > 1) {
                        if ((flags & FLAG_MINUS) != 0) {
                            buf.append(c);
                            buf.fill(' ', width-1);
                        } else {
                            buf.fill(' ',width-1);
                            buf.append(c);
                        }
                    } else {
                        buf.append(c);
                    }
                    offset++;
                    incomplete = false;
                    break;
                }
                case 'p':
                case 's': {
                    if (arg == null) arg = args.next();

                    if (fchar == 'p') {
                        arg = arg.callMethod(arg.getRuntime().getCurrentContext(),"inspect");
                    }
                    ByteList bytes = arg.asString().getByteList();
                    int len = bytes.length();
                    if (arg.isTaint()) tainted = true;
                    if ((flags & FLAG_PRECISION) != 0 && precision < len) {
                        len = precision;
                    }
                    // TODO: adjust length so it won't fall in the middle 
                    // of a multi-byte character. MRI's sprintf.c uses tables
                    // in a modified version of regex.c, which assume some
                    // particular  encoding for a given installation/application.
                    // (See regex.c#re_mbcinit in ruby-1.8.5-p12) 
                    //
                    // This is only an issue if the user specifies a precision
                    // that causes the string to be truncated. The same issue
                    // would arise taking a substring of a ByteList-backed RubyString.

                    if ((flags & FLAG_WIDTH) != 0 && width > len) {
                        width -= len;
                        if ((flags & FLAG_MINUS) != 0) {
                            buf.append(bytes.unsafeBytes(),bytes.begin(),len);
                            buf.fill(' ',width);
                        } else {
                            buf.fill(' ',width);
                            buf.append(bytes.unsafeBytes(),bytes.begin(),len);
                        }
                    } else {
                        buf.append(bytes.unsafeBytes(),bytes.begin(),len);
                    }
                    offset++;
                    incomplete = false;
                    break;
                }
                case 'd':
                case 'i':
                case 'o':
                case 'x':
                case 'X':
                case 'b':
                case 'B':
                case 'u': {
                    if (arg == null) arg = args.next();

                    int type = arg.getMetaClass().index;
                    if (type != ClassIndex.FIXNUM && type != ClassIndex.BIGNUM) {
                        switch(type) {
                        case ClassIndex.FLOAT:
                            arg = RubyNumeric.dbl2num(arg.getRuntime(),((RubyFloat)arg).getValue());
                            break;
                        case ClassIndex.STRING:
                            arg = ((RubyString)arg).stringToInum(0, true);
                            break;
                        default:
                            if (arg.respondsTo("to_int")) {
                                arg = TypeConverter.convertToType(arg, arg.getRuntime().getInteger(), "to_int", true);
                            } else {
                                arg = TypeConverter.convertToType(arg, arg.getRuntime().getInteger(), "to_i", true);
                            }
                            break;
                        }
                        type = arg.getMetaClass().index;
                    }
                    byte[] bytes = null;
                    int first = 0;
                    byte[] prefix = null;
                    boolean sign;
                    boolean negative;
                    byte signChar = 0;
                    byte leadChar = 0;
                    int base;

                    // 'd' and 'i' are the same
                    if (fchar == 'i') fchar = 'd';

                    // 'u' with space or plus flags is same as 'd'
                    if (fchar == 'u' && (flags & (FLAG_SPACE | FLAG_PLUS)) != 0) {
                        fchar = 'd';
                    }
                    sign = (fchar == 'd' || (flags & (FLAG_SPACE | FLAG_PLUS)) != 0);

                    switch (fchar) {
                    case 'o':
                        base = 8; break;
                    case 'x':
                    case 'X':
                        base = 16; break;
                    case 'b':
                    case 'B':
                        base = 2; break;
                    case 'u':
                    case 'd':
                    default:
                        base = 10; break;
                    }
                    // We depart here from strict adherence to MRI code, as MRI
                    // uses C-sprintf, in part, to format numeric output, while
                    // we'll use Java's numeric formatting code (and our own).
                    boolean zero;
                    if (type == ClassIndex.FIXNUM) {
                        negative = ((RubyFixnum)arg).getLongValue() < 0;
                        zero = ((RubyFixnum)arg).getLongValue() == 0;
                        if (negative && fchar == 'u') {
                            bytes = getUnsignedNegativeBytes((RubyFixnum)arg);
                        } else {
                            bytes = getFixnumBytes((RubyFixnum)arg,base,sign,fchar=='X');
                        }
                    } else {
                        negative = ((RubyBignum)arg).getValue().signum() < 0;
                        zero = ((RubyBignum)arg).getValue().equals(BigInteger.ZERO);
                        if (negative && fchar == 'u') {
                            bytes = getUnsignedNegativeBytes((RubyBignum)arg);
                        } else {
                            bytes = getBignumBytes((RubyBignum)arg,base,sign,fchar=='X');
                        }
                    }
                    if ((flags & FLAG_SHARP) != 0) {
                        if (!zero || usePrefixForZero) {
                            switch (fchar) {
                            case 'o': prefix = PREFIX_OCTAL; break;
                            case 'x': prefix = PREFIX_HEX_LC; break;
                            case 'X': prefix = PREFIX_HEX_UC; break;
                            case 'b': prefix = PREFIX_BINARY_LC; break;
                            case 'B': prefix = PREFIX_BINARY_UC; break;
                            }
                        }
                        if (prefix != null) width -= prefix.length;
                    }
                    int len = 0;
                    if (sign) {
                        if (negative) {
                            signChar = '-';
                            width--;
                            first = 1; // skip '-' in bytes, will add where appropriate
                        } else if ((flags & FLAG_PLUS) != 0) {
                            signChar = '+';
                            width--;
                        } else if ((flags & FLAG_SPACE) != 0) {
                            signChar = ' ';
                            width--;
                        }
                    } else if (negative) {
                        if (base == 10) {
                            warning(ID.NEGATIVE_NUMBER_FOR_U, args, "negative number for %u specifier");
                            leadChar = '.';
                            len += 2;
                        } else {
                            if ((flags & (FLAG_PRECISION | FLAG_ZERO)) == 0) len += 2; // ..

                            first = skipSignBits(bytes,base);
                            switch(fchar) {
                            case 'b':
                            case 'B':
                                leadChar = '1';
                                break;
                            case 'o':
                                leadChar = '7';
                                break;
                            case 'x':
                                leadChar = 'f';
                                break;
                            case 'X':
                                leadChar = 'F';
                                break;
                            }
                            if (leadChar != 0) len++;
                        }
                    }
                    int numlen = bytes.length - first;
                    len += numlen;
                    
                    if ((flags & (FLAG_ZERO|FLAG_PRECISION)) == FLAG_ZERO) {
                        precision = width;
                        width = 0;
                    } else {
                        if (precision < len) precision = len;

                        width -= precision;
                    }
                    if ((flags & FLAG_MINUS) == 0) {
                        buf.fill(' ',width);
                        width = 0;
                    }
                    if (signChar != 0) buf.append(signChar);
                    if (prefix != null) buf.append(prefix);

                    if (len < precision) {
                        if (leadChar == 0) {
                            buf.fill('0', precision - len);
                        } else if (leadChar == '.') {
                            buf.fill(leadChar,precision-len);
                            buf.append(PREFIX_NEGATIVE);
                        } else {
                            buf.fill(leadChar,precision-len+1); // the 1 is for the stripped sign char
                        }
                    } else if (leadChar != 0) {
                        if ((flags & (FLAG_PRECISION | FLAG_ZERO)) == 0) {
                            buf.append(PREFIX_NEGATIVE);
                        }
                        if (leadChar != '.') buf.append(leadChar);
                    }
                    buf.append(bytes,first,numlen);

                    if (width > 0) buf.fill(' ',width);
                                        
                    offset++;
                    incomplete = false;
                    break;
                }
                case 'E':
                case 'e':
                case 'f':
                case 'G':
                case 'g': {
                    if (arg == null) arg = args.next();
                    
                    if (!(arg instanceof RubyFloat)) {
                        // FIXME: what is correct 'recv' argument?
                        // (this does produce the desired behavior)
                        arg = RubyKernel.new_float(arg,arg);
                    }
                    double dval = ((RubyFloat)arg).getDoubleValue();
                    boolean nan = dval != dval;
                    boolean inf = dval == Double.POSITIVE_INFINITY || dval == Double.NEGATIVE_INFINITY;
                    boolean negative = dval < 0.0d || (dval == 0.0d && (new Float(dval)).equals(new Float(-0.0)));
                    byte[] digits;
                    int nDigits = 0;
                    int exponent = 0;

                    int len = 0;
                    byte signChar;
                    
                    if (nan || inf) {
                        if (nan) {
                            digits = NAN_VALUE;
                            len = NAN_VALUE.length;
                        } else {
                            digits = INFINITY_VALUE;
                            len = INFINITY_VALUE.length;
                        }
                        if (negative) {
                            signChar = '-';
                            width--;
                        } else if ((flags & FLAG_PLUS) != 0) {
                            signChar = '+';
                            width--;
                        } else if ((flags & FLAG_SPACE) != 0) {
                            signChar = ' ';
                            width--;
                        } else {
                            signChar = 0;
                        }
                        width -= len;
                        
                        if (width > 0 && (flags & (FLAG_ZERO|FLAG_MINUS)) == 0) {
                            buf.fill(' ',width);
                            width = 0;
                        }
                        if (signChar != 0) buf.append(signChar);

                        if (width > 0 && (flags & FLAG_MINUS) == 0) {
                            buf.fill('0',width);
                            width = 0;
                        }
                        buf.append(digits);
                        if (width > 0) buf.fill(' ', width);

                        offset++;
                        incomplete = false;
                        break;
                    }

                    
                    String str = Double.toString(dval);
                    
                    // grrr, arghh, want to subclass sun.misc.FloatingDecimal, but can't,
                    // so we must do all this (the next 70 lines of code), which has already
                    // been done by FloatingDecimal.
                    int strlen = str.length();
                    digits = new byte[strlen];
                    int nTrailingZeroes = 0;
                    int i = negative ? 1 : 0;
                    int decPos = 0;
                    byte ival;
                int_loop:
                    for ( ; i < strlen ; ) {
                        switch(ival = (byte)str.charAt(i++)) {
                        case '0':
                            if (nDigits > 0) nTrailingZeroes++;

                            break; // switch
                        case '1': case '2': case '3': case '4':
                        case '5': case '6': case '7': case '8': case '9':
                            if (nTrailingZeroes > 0) {
                                for ( ; nTrailingZeroes > 0 ; nTrailingZeroes-- ) {
                                    digits[nDigits++] = '0';
                                }
                            }
                            digits[nDigits++] = ival;
                            break; // switch
                        case '.':
                            break int_loop;
                        }
                    }
                    decPos = nDigits + nTrailingZeroes;
                dec_loop:
                    for ( ; i < strlen ; ) {
                        switch(ival = (byte)str.charAt(i++)) {
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
                                for ( ; nTrailingZeroes > 0 ; nTrailingZeroes--  ) {
                                    digits[nDigits++] = '0';
                                }
                            }
                            digits[nDigits++] = ival;
                            break; // switch
                        case 'E':
                            break dec_loop;
                        }
                    }
                    if (i < strlen) {
                        int expSign;
                        int expVal = 0;
                        if (str.charAt(i) == '-') {
                            expSign = -1;
                            i++;
                        } else {
                            expSign = 1;
                        }
                        for ( ; i < strlen ; ) {
                            expVal = expVal * 10 + ((int)str.charAt(i++)-(int)'0');
                        }
                        exponent += expVal * expSign;
                    }
                    exponent += decPos - nDigits;

                    // gotta have at least a zero...
                    if (nDigits == 0) {
                        digits[0] = '0';
                        nDigits = 1;
                        exponent = 0;
                    }

                    // OK, we now have the significand in digits[0...nDigits]
                    // and the exponent in exponent.  We're ready to format.

                    int intDigits, intZeroes, intLength;
                    int decDigits, decZeroes, decLength;
                    byte expChar;

                    if (negative) {
                        signChar = '-';
                        width--;
                    } else if ((flags & FLAG_PLUS) != 0) {
                        signChar = '+';
                        width--;
                    } else if ((flags & FLAG_SPACE) != 0) {
                        signChar = ' ';
                        width--;
                    } else {
                        signChar = 0;
                    }
                    if ((flags & FLAG_PRECISION) == 0) {
                        precision = 6;
                    }
                    
                    switch(fchar) {
                    case 'E':
                    case 'G':
                        expChar = 'E';
                        break;
                    case 'e':
                    case 'g':
                        expChar = 'e';
                        break;
                    default:
                        expChar = 0;
                    }

                    switch (fchar) {
                    case 'g':
                    case 'G':
                        // an empirically derived rule: precision applies to
                        // significand length, irrespective of exponent

                        // an official rule, clarified: if the exponent
                        // <clarif>after adjusting for exponent form</clarif>
                        // is < -4,  or the exponent <clarif>after adjusting 
                        // for exponent form</clarif> is greater than the
                        // precision, use exponent form
                        boolean expForm = (exponent + nDigits - 1 < -4 ||
                            exponent + nDigits > (precision == 0 ? 1 : precision));
                        // it would be nice (and logical!) if exponent form 
                        // behaved like E/e, and decimal form behaved like f,
                        // but no such luck. hence: 
                        if (expForm) {
                            // intDigits isn't used here, but if it were, it would be 1
                            /* intDigits = 1; */
                            decDigits = nDigits - 1;
                            // precision for G/g includes integer digits
                            precision = Math.max(0,precision - 1);

                            if (precision < decDigits) {
                                int n = round(digits,nDigits,precision,precision!=0);
                                if (n > nDigits) nDigits = n;
                                decDigits = Math.min(nDigits - 1,precision);
                            }
                            exponent += nDigits - 1;
                            
                            boolean isSharp = (flags & FLAG_SHARP) != 0;

                            // deal with length/width
			    
                            len++; // first digit is always printed

                            // MRI behavior: Be default, 2 digits
                            // in the exponent. Use 3 digits
                            // only when necessary.
                            // See comment for writeExp method for more details.
                            if (exponent > 99)
                            	len += 5; // 5 -> e+nnn / e-nnn
                            else
                            	len += 4; // 4 -> e+nn / e-nn

                            if (isSharp) {
                            	// in this mode, '.' is always printed
                            	len++;
                            }

                            if (precision > 0) {
                            	if (!isSharp) {
                            	    // MRI behavior: In this mode
                            	    // trailing zeroes are removed:
                            	    // 1.500E+05 -> 1.5E+05 
                            	    int j = decDigits;
                            	    for (; j >= 1; j--) {
                            	        if (digits[j]== '0') {
                            	            decDigits--;
                            	        } else {
                            	            break;
                            	        }
                            	    }

                            	    if (decDigits > 0) {
                            	        len += 1; // '.' is printed
                            	        len += decDigits;
                            	    }
                            	} else  {
                            	    // all precision numebers printed
                            	    len += precision;
                            	}
                            }

                            width -= len;

                            if (width > 0 && (flags & (FLAG_ZERO|FLAG_MINUS)) == 0) {
                                buf.fill(' ',width);
                                width = 0;
                            }
                            if (signChar != 0) {
                                buf.append(signChar);
                            }
                            if (width > 0 && (flags & FLAG_MINUS) == 0) {
                                buf.fill('0',width);
                                width = 0;
                            }

                            // now some data...
                            buf.append(digits[0]);

                            boolean dotToPrint = isSharp
                                    || (precision > 0 && decDigits > 0);

                            if (dotToPrint) {
                            	buf.append(args.getDecimalSeparator()); // '.'
                            }

                            if (precision > 0 && decDigits > 0) {
                            	buf.append(digits, 1, decDigits);
                            	precision -= decDigits;
                            }

                            if (precision > 0 && isSharp) {
                            	buf.fill('0', precision);
                            }

                            writeExp(buf, exponent, expChar);

                            if (width > 0) {
                                buf.fill(' ', width);
                            }
                        } else { // decimal form, like (but not *just* like!) 'f'
                            intDigits = Math.max(0,Math.min(nDigits + exponent,nDigits));
                            intZeroes = Math.max(0,exponent);
                            intLength = intDigits + intZeroes;
                            decDigits = nDigits - intDigits;
                            decZeroes = Math.max(0,-(decDigits + exponent));
                            decLength = decZeroes + decDigits;
                            precision = Math.max(0,precision - intLength);
                            
                            if (precision < decDigits) {
                                int n = round(digits,nDigits,intDigits+precision-1,precision!=0);
                                if (n > nDigits) {
                                    // digits array shifted, update all
                                    nDigits = n;
                                    intDigits = Math.max(0,Math.min(nDigits + exponent,nDigits));
                                    intLength = intDigits + intZeroes;
                                    decDigits = nDigits - intDigits;
                                    decZeroes = Math.max(0,-(decDigits + exponent));
                                    precision = Math.max(0,precision-1);
                                }
                                decDigits = precision;
                                decLength = decZeroes + decDigits;
                            }
                            len += intLength;
                            if (decLength > 0) {
                                len += decLength + 1;
                            } else {
                                if ((flags & FLAG_SHARP) != 0) {
                                    len++; // will have a trailing '.'
                                    if (precision > 0) { // g fills trailing zeroes if #
                                        len += precision;
                                    }
                                }
                            }
                            
                            width -= len;
                            
                            if (width > 0 && (flags & (FLAG_ZERO|FLAG_MINUS)) == 0) {
                                buf.fill(' ',width);
                                width = 0;
                            }
                            if (signChar != 0) {
                                buf.append(signChar);
                            }
                            if (width > 0 && (flags & FLAG_MINUS) == 0) {
                                buf.fill('0',width);
                                width = 0;
                            }
                            // now some data...
                            if (intLength > 0){
                                if (intDigits > 0) { // s/b true, since intLength > 0
                                    buf.append(digits,0,intDigits);
                                }
                                if (intZeroes > 0) {
                                    buf.fill('0',intZeroes);
                                }
                            } else {
                                // always need at least a 0
                                buf.append('0');
                            }
                            if (decLength > 0 || (flags & FLAG_SHARP) != 0) {
                                buf.append(args.getDecimalSeparator());
                            }
                            if (decLength > 0) {
                                if (decZeroes > 0) {
                                    buf.fill('0',decZeroes);
                                    precision -= decZeroes;
                                }
                                if (decDigits > 0) {
                                    buf.append(digits,intDigits,decDigits);
                                    precision -= decDigits;
                                }
                                if ((flags & FLAG_SHARP) != 0 && precision > 0) {
                                    buf.fill('0',precision);
                                }
                            }
                            if ((flags & FLAG_SHARP) != 0 && precision > 0) buf.fill('0',precision);
                            if (width > 0) buf.fill(' ', width);
                        }
                        break;
                    
                    case 'f':
                        intDigits = Math.max(0,Math.min(nDigits + exponent,nDigits));
                        intZeroes = Math.max(0,exponent);
                        intLength = intDigits + intZeroes;
                        decDigits = nDigits - intDigits;
                        decZeroes = Math.max(0,-(decDigits + exponent));
                        decLength = decZeroes + decDigits;
                        
                        if (precision < decLength) {
                            if (precision < decZeroes) {
                                decDigits = 0;
                                decZeroes = precision;
                            } else {
                                int n = round(digits,nDigits,intDigits+precision-decZeroes-1,precision!=0);
                                if (n > nDigits) {
                                    // digits arr shifted, update all
                                    nDigits = n;
                                    intDigits = Math.max(0,Math.min(nDigits + exponent,nDigits));
                                    intLength = intDigits + intZeroes;
                                    decDigits = nDigits - intDigits;
                                    decZeroes = Math.max(0,-(decDigits + exponent));
                                    decLength = decZeroes + decDigits;
                                }
                                decDigits = precision - decZeroes;
                            }
                            decLength = decZeroes + decDigits;
                        }
                        if (precision > 0) {
                            len += Math.max(1,intLength) + 1 + precision;
                            // (1|intlen).prec
                        } else {
                            len += Math.max(1,intLength);
                            // (1|intlen)
                            if ((flags & FLAG_SHARP) != 0) {
                                len++; // will have a trailing '.'
                            }
                        }
                        
                        width -= len;
                        
                        if (width > 0 && (flags & (FLAG_ZERO|FLAG_MINUS)) == 0) {
                            buf.fill(' ',width);
                            width = 0;
                        }
                        if (signChar != 0) {
                            buf.append(signChar);
                        }
                        if (width > 0 && (flags & FLAG_MINUS) == 0) {
                            buf.fill('0',width);
                            width = 0;
                        }
                        // now some data...
                        if (intLength > 0){
                            if (intDigits > 0) { // s/b true, since intLength > 0
                                buf.append(digits,0,intDigits);
                            }
                            if (intZeroes > 0) {
                                buf.fill('0',intZeroes);
                            }
                        } else {
                            // always need at least a 0
                            buf.append('0');
                        }
                        if (precision > 0 || (flags & FLAG_SHARP) != 0) {
                            buf.append(args.getDecimalSeparator());
                        }
                        if (precision > 0) {
                            if (decZeroes > 0) {
                                buf.fill('0',decZeroes);
                                precision -= decZeroes;
                            }
                            if (decDigits > 0) {
                                buf.append(digits,intDigits,decDigits);
                                precision -= decDigits;
                            }
                            // fill up the rest with zeroes
                            if (precision > 0) {
                                buf.fill('0',precision);
                            }
                        }
                        if (width > 0) {
                            buf.fill(' ', width);
                        }
                        break;
                    case 'E':
                    case 'e':
                        // intDigits isn't used here, but if it were, it would be 1
                        /* intDigits = 1; */
                        decDigits = nDigits - 1;
                        
                        if (precision < decDigits) {
                            int n = round(digits,nDigits,precision,precision!=0);
                            if (n > nDigits) {
                                nDigits = n;
                            }
                            decDigits = Math.min(nDigits - 1,precision);
                        }
                        exponent += nDigits - 1;

                        boolean isSharp = (flags & FLAG_SHARP) != 0;

                        // deal with length/width

                        len++; // first digit is always printed

                        // MRI behavior: Be default, 2 digits
                        // in the exponent. Use 3 digits
                        // only when necessary.
                        // See comment for writeExp method for more details.
                        if (exponent > 99)
                            len += 5; // 5 -> e+nnn / e-nnn
                        else
                            len += 4; // 4 -> e+nn / e-nn

                        if (precision > 0) {
                            // '.' and all precision digits printed
                            len += 1 + precision;
                        } else  if (isSharp) {
                            len++;  // in this mode, '.' is always printed
                        }

                        width -= len;

                        if (width > 0 && (flags & (FLAG_ZERO|FLAG_MINUS)) == 0) {
                            buf.fill(' ',width);
                            width = 0;
                        }
                        if (signChar != 0) {
                            buf.append(signChar);
                        }
                        if (width > 0 && (flags & FLAG_MINUS) == 0) {
                            buf.fill('0',width);
                            width = 0;
                        }
                        // now some data...
                        buf.append(digits[0]);
                        if (precision > 0) {
                            buf.append(args.getDecimalSeparator()); // '.'
                            if (decDigits > 0) {
                                buf.append(digits,1,decDigits);
                                precision -= decDigits;
                            }
                            if (precision > 0) buf.fill('0',precision);

                        } else if ((flags & FLAG_SHARP) != 0) {
                            buf.append(args.getDecimalSeparator());
                        }

                        writeExp(buf, exponent, expChar);

                        if (width > 0) buf.fill(' ', width);
                        break;
                    } // switch (format char E,e,f,G,g)
                    
                    offset++;
                    incomplete = false;
                    break;
                } // block (case E,e,f,G,g)
                } // switch (each format char in spec)
            } // for (each format spec)
            
            // equivalent to MRI case '\0':
            if (incomplete) {
                if (flags == FLAG_NONE) {
                    // dangling '%' char
                    buf.append('%');
                } else {
                    raiseArgumentError(args,ERR_ILLEGAL_FORMAT_CHAR);
                }
            }
        } // main while loop (offset < length)

        // MRI behavior: validate only the unnumbered arguments
        if ((args.numbered == 0) && args.unnumbered < args.length) {
            if (args.runtime.getDebug().isTrue()) {
                args.raiseArgumentError("too many arguments for format string");
            } else if (args.runtime.isVerbose()) {
                args.warn(ID.TOO_MANY_ARGUMENTS, "too many arguments for format string");
            }
        }

        return tainted;
    }

    private static void writeExp(ByteList buf, int exponent, byte expChar) {
        // Unfortunately, the number of digits in the exponent is
        // not clearly defined in Ruby documentation. This is a
        // platform/version-dependent behavior. On Linux/Mac/Cygwin/*nix,
        // two digits are used. On Windows, 3 digits are used.
        // It is desirable for JRuby to have consistent behavior, and
        // the two digits behavior was selected. This is also in sync
        // with "Java-native" sprintf behavior (java.util.Formatter).
        buf.append(expChar); // E or e
        buf.append(exponent >= 0 ? '+' : '-');
        if (exponent < 0) {
            exponent = -exponent;
        }
        if (exponent > 99) {                                
            buf.append(exponent / 100 + '0');
            buf.append(exponent % 100 / 10 + '0');
        } else {
            buf.append(exponent / 10 + '0');
        }
        buf.append(exponent % 10 + '0');
    }

    // debugging code, keeping for now
    /*
    private static final void showLiteral(byte[] format, int start, int offset) {
        System.out.println("literal: ["+ new String(format,start,offset-start)+ "], " +
                " s="+ start + " o="+ offset);
    }
    
    // debugging code, keeping for now
    private static final void showVals(byte[] format,int start,int offset, byte fchar,
            int flags, int width, int precision, Object arg) {
        System.out.println(new StringBuffer()
        .append("value: ").append(new String(format,start,offset-start+1)).append('\n')
        .append("type: ").append((char)fchar).append('\n')
        .append("start: ").append(start).append('\n')
        .append("length: ").append(offset-start).append('\n')
        .append("flags: ").append(Integer.toBinaryString(flags)).append('\n')
        .append("width: ").append(width).append('\n')
        .append("precision: ").append(precision).append('\n')
        .append("arg: ").append(arg).append('\n')
        .toString());
        
    }
    */
    
    private static final void raiseArgumentError(Args args, String message) {
        args.raiseArgumentError(message);
    }
    
    private static final void warning(ID id, Args args, String message) {
        args.warning(id, message);
    }
    
    private static final void checkOffset(Args args, int offset, int length, String message) {
        if (offset >= length) {
            raiseArgumentError(args,message);
        }
    }

    private static final int extendWidth(Args args, int oldWidth, byte newChar) {
        int newWidth = oldWidth * 10 + (newChar - '0');
        if (newWidth / 10 != oldWidth) raiseArgumentError(args,"width too big");
        return newWidth;
    }
    
    private static final boolean isDigit(byte aChar) {
        return (aChar >= '0' && aChar <= '9');
    }
    
    private static final boolean isPrintable(byte aChar) {
        return (aChar > 32 && aChar < 127);
    }

    private static final int skipSignBits(byte[] bytes, int base) {
        int skip = 0;
        int length = bytes.length;
        byte b;
        switch(base) {
        case 2:
            for ( ; skip < length && bytes[skip] == '1'; skip++ ) ;
            break;
        case 8:
            if (length > 0 && bytes[0] == '3') skip++;
            for ( ; skip < length && bytes[skip] == '7'; skip++ ) ;
            break;
        case 10:
            if (length > 0 && bytes[0] == '-') skip++;
            break;
        case 16:
            for ( ; skip < length && ((b = bytes[skip]) == 'f' || b == 'F'); skip++ ) ;
        }
        return skip;
    }
    
    private static final int round(byte[] bytes, int nDigits, int roundPos, boolean roundDown) {
        int next = roundPos + 1;
        if (next >= nDigits || bytes[next] < '5' ||
                // MRI rounds up on nnn5nnn, but not nnn5 --
                // except for when they do
                (roundDown && bytes[next] == '5' && next == nDigits - 1)) {
            return nDigits;
        }
        if (roundPos < 0) { // "%.0f" % 0.99
            System.arraycopy(bytes,0,bytes,1,nDigits);
            bytes[0] = '1';
            return nDigits + 1;
        }
        bytes[roundPos] += 1;
        while (bytes[roundPos] > '9') {
            bytes[roundPos] = '0';
            roundPos--;
            if (roundPos >= 0) {
                bytes[roundPos] += 1;
            } else {
                System.arraycopy(bytes,0,bytes,1,nDigits);
                bytes[0] = '1';
                return nDigits + 1;
            }
        }
        return nDigits;
    }

    private static final byte[] getFixnumBytes(RubyFixnum arg, int base, boolean sign, boolean upper) {
        long val = arg.getLongValue();

        // limit the length of negatives if possible (also faster)
        if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
            if (sign) {
                return Convert2.intToByteArray((int)val,base,upper);
            } else {
                switch(base) {
                case 2:  return Convert2.intToBinaryBytes((int)val);
                case 8:  return Convert2.intToOctalBytes((int)val);
                case 10:
                default: return Convert2.intToCharBytes((int)val);
                case 16: return Convert2.intToHexBytes((int)val,upper);
                }
            }
        } else {
            if (sign) {
                return Convert2.longToByteArray(val,base,upper);
            } else {
                switch(base) {
                case 2:  return Convert2.longToBinaryBytes(val);
                case 8:  return Convert2.longToOctalBytes(val);
                case 10:
                default: return Convert2.longToCharBytes(val);
                case 16: return Convert2.longToHexBytes(val,upper);
                }
            }
        }
    }
    
    private static final byte[] getBignumBytes(RubyBignum arg, int base, boolean sign, boolean upper) {
        BigInteger val = arg.getValue();
        if (sign || base == 10 || val.signum() >= 0) {
            return stringToBytes(val.toString(base),upper);
        }

        // negative values
        byte[] bytes = val.toByteArray();
        switch(base) {
        case 2:  return Convert2.twosComplementToBinaryBytes(bytes);
        case 8:  return Convert2.twosComplementToOctalBytes(bytes);
        case 16: return Convert2.twosComplementToHexBytes(bytes,upper);
        default: return stringToBytes(val.toString(base),upper);
        }
    }
    
    private static final byte[] getUnsignedNegativeBytes(RubyInteger arg) {
        // calculation for negatives when %u specified
        // for values >= Integer.MIN_VALUE * 2, MRI uses (the equivalent of)
        //   long neg_u = (((long)Integer.MAX_VALUE + 1) << 1) + val
        // for smaller values, BigInteger math is required to conform to MRI's
        // result.
        long longval;
        BigInteger bigval;
        
        if (arg instanceof RubyFixnum) {
            // relatively cheap test for 32-bit values
            longval = ((RubyFixnum)arg).getLongValue();
            if (longval >= Long.MIN_VALUE << 1) {
                return Convert2.longToCharBytes(((Long.MAX_VALUE + 1L) << 1) + longval);
            }
            // no such luck...
            bigval = BigInteger.valueOf(longval);
        } else {
            bigval = ((RubyBignum)arg).getValue();
        }
        // ok, now it gets expensive...
        int shift = 0;
        // go through negated powers of 32 until we find one small enough 
        for (BigInteger minus = BIG_MINUS_64 ;
                bigval.compareTo(minus) < 0 ;
                minus = minus.shiftLeft(32), shift++) ;
        // add to the corresponding positive power of 32 for the result.
        // meaningful? no. conformant? yes. I just write the code...
        BigInteger nPower32 = shift > 0 ? BIG_64.shiftLeft(32 * shift) : BIG_64;
        return stringToBytes(nPower32.add(bigval).toString(),false);
    }
    
    private static final byte[] stringToBytes(CharSequence s, boolean upper) {
        int len = s.length();
        byte[] bytes = new byte[len];
        if (upper) {
            for (int i = len; --i >= 0; ) {
                int b = (byte)((int)s.charAt(i) & (int)0xff);
                if (b >= 'a' && b <= 'z') {
                    bytes[i] = (byte)(b & ~0x20);
                } else {
                    bytes[i] = (byte)b;
                }
            }
        } else {
            for (int i = len; --i >= 0; ) {
                bytes[i] = (byte)((int)s.charAt(i) & (int)0xff); 
            }
        }
        return bytes;
    }
}
