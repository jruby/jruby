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
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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

import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jcodings.Encoding;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.*;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.EncodingUtils;


/**
 * @author Bill Dortch
 *
 */
public class Sprintf {
    private static final int FLAG_NONE        = 0;
    private static final int FLAG_SPACE       = 1;
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
    private static final String ERR_INCOMPLETE_FORMAT_SPEC = "incomplete format specifier; use %% (double %) instead";
    private static final String ERR_MALFORMED_NAME = "malformed name - unmatched parenthesis";

    private static final ThreadLocal<Map<Locale, NumberFormat>> LOCALE_NUMBER_FORMATS = new ThreadLocal<Map<Locale, NumberFormat>>();
    private static final ThreadLocal<Map<Locale, DecimalFormatSymbols>> LOCALE_DECIMAL_FORMATS = new ThreadLocal<Map<Locale, DecimalFormatSymbols>>();

    private static final class Args {
        private final Ruby runtime;
        private final Locale locale;
        private final IRubyObject rubyObject;
        private final RubyArray rubyArray;
        private final RubyHash rubyHash;
        private final int length;
        private int positionIndex; // last index (+1) accessed by next()
        private int nextIndex;
        private IRubyObject nextObject;

        Args(Locale locale, IRubyObject rubyObject) {
            if (rubyObject == null) throw new IllegalArgumentException("null IRubyObject passed to sprintf");
            this.locale = locale == null ? Locale.getDefault() : locale;
            this.rubyObject = rubyObject;
            if (rubyObject instanceof RubyArray) {
                this.rubyArray = (RubyArray)rubyObject;

                if (rubyArray.last() instanceof RubyHash) {
                    this.rubyHash = (RubyHash) rubyArray.pop(rubyArray.getRuntime().getCurrentContext());
                } else {
                    this.rubyHash = null;
                }

                this.length = rubyArray.size();
            } else if (rubyObject instanceof RubyHash) {
                // allow a hash for args if in 1.9 mode
                this.rubyHash = (RubyHash)rubyObject;
                this.rubyArray = null;
                this.length = 1;
            } else {
                this.length = 1;
                this.rubyArray = null;
                this.rubyHash = null;
            }

            positionIndex = 0;
            nextIndex = 1;

            this.runtime = rubyObject.getRuntime();
        }

        Args(IRubyObject rubyObject) {
            this(Locale.getDefault(),rubyObject);
        }

        // temporary hack to handle non-Ruby values
        // will come up with better solution shortly
        Args(Ruby runtime, long value) {
            this(RubyFixnum.newFixnum(runtime, value));
        }

        void raiseArgumentError(String message) {
            throw runtime.newArgumentError(message);
        }

        void raiseKeyError(String message, IRubyObject recv, IRubyObject key) {
            throw runtime.newKeyError(message, recv, key);
        }

        void warn(ID id, String message) {
            runtime.getWarnings().warn(id, message);
        }

        void warning(ID id, String message) {
            if (runtime.isVerbose()) runtime.getWarnings().warning(id, message);
        }

        private IRubyObject getHashValue(ByteList name, char startDelim, char endDelim) {
            // FIXME: get_hash does hash conversion of argv and arity check...this is a bit complicated with
            // our version.  Implement it.
            if (rubyHash == null) {
                raiseArgumentError("one hash required");
            }

            checkNameArg(name, startDelim, endDelim);
            RubySymbol nameSym = runtime.newSymbol(name);
            IRubyObject object = rubyHash.fastARef(nameSym);

            // if not found, try dispatching to pick up default hash value
            // MRI: spliced together bits from rb_hash_default_value
            if (object == null) {
                object = rubyHash.getIfNone();
                if (object == RubyBasicObject.UNDEF) {
                    RubyString nameStr = RubyString.newString(runtime, name);
                    raiseKeyError("key" + startDelim + nameStr + endDelim + " not found", rubyHash, nameSym);
                } else if (rubyHash.hasDefaultProc()) {
                    object = object.callMethod(runtime.getCurrentContext(), "call", nameSym);
                }

                if (object.isNil()) throw runtime.newKeyError("key" + startDelim + nameSym + endDelim + " not found", rubyHash, nameSym);
            }

            return object;
        }

        private IRubyObject getNthArg(int index) {
            if (index > length) {
                if (index == length + 1 && rubyHash != null) {
                    return rubyHash;
                }
                raiseArgumentError("too few arguments");
            }

            return rubyArray == null ? rubyObject : rubyArray.eltInternal(index - 1);
        }


        // MRI: GETARG
        private IRubyObject getArg() {
            final IRubyObject nextObject = this.nextObject;
            if (nextObject != null) {
                // This is different in MRI.  The do a retry and avoid part of the for loop
                // which resets nextvalue.  We do not do that so we null out here since we
                // cannot get same value twice.
                this.nextObject = null;
                return nextObject;
            }

            return getNextArg();
        }

        // MRI: GETNEXTARG
        private IRubyObject getNextArg() {
            checkNextArg();
            positionIndex = nextIndex++;
            return getNthArg(positionIndex);
        }

        // MRI: GETPOSARG
        private IRubyObject getPositionArg(int index) {
            checkPositionArg(index);
            positionIndex = -1;
            return getNthArg(index);
        }

        // MRI: check_next_arg
        private void checkNextArg() {
            if (positionIndex == -1) raiseArgumentError("unnumbered(" + nextIndex + ") mixed with numbered");
            if (positionIndex == -2) raiseArgumentError("unnumbered(" + nextIndex + ") mixed with named");
        }

        // MRI: check_pos_arg
        private void checkPositionArg(int nthArg) {
            if (positionIndex > 0) raiseArgumentError("numbered(" + nthArg + ") after unnumbered(" + positionIndex + ")");
            if (positionIndex == -2) raiseArgumentError("numbered(" + nthArg + ") after named");
            if (nthArg < 1) raiseArgumentError("invalid index - " + nthArg + "$");
        }

        // MRI: check_name_arg, CHECKNAMEARG
        private void checkNameArg(ByteList name, char startDelim, char endDelim) {
            if (positionIndex > 0) raiseArgumentError("named" + startDelim + RubyString.newString(runtime, name) + endDelim + " after unnumbered(" + positionIndex + ")");
            if (positionIndex == -1) raiseArgumentError("named" + startDelim + RubyString.newString(runtime, name) + endDelim + " after numbered");

            positionIndex = -2;
        }

        @Deprecated
        IRubyObject next(ByteList name) {
            // for 1.9 hash args
            if (name != null) {
                if (rubyHash == null && positionIndex == -1) raiseArgumentError("positional args mixed with named args");

                RubySymbol nameSym = runtime.newSymbol(name);
                IRubyObject object = rubyHash.fastARef(nameSym);

                // if not found, try dispatching to pick up default hash value
                // MRI: spliced together bits from rb_hash_default_value
                if (object == null) {
                    object = rubyHash.getIfNone();
                    if (object == RubyBasicObject.UNDEF) {
                        RubyString nameStr = RubyString.newString(runtime, name);
                        raiseKeyError("key<" + name + "> not found", rubyHash, nameSym);
                    } else if (rubyHash.hasDefaultProc()) {
                        object = object.callMethod(runtime.getCurrentContext(), "call", nameSym);
                    }

                    if (object.isNil()) {
                        throw runtime.newKeyError("key " + nameSym + " not found", rubyHash, nameSym);
                    }
                }

                return object;
            } else if (rubyHash != null) {
                raiseArgumentError("positional args mixed with named args");
            }

            // this is the order in which MRI does these two tests
            if (positionIndex == -1) raiseArgumentError("unnumbered" + (positionIndex + 1) + "mixed with numbered");
            if (positionIndex >= length) raiseArgumentError("too few arguments");
            IRubyObject object = rubyArray == null ? rubyObject : rubyArray.eltInternal(positionIndex);
            positionIndex++;
            return object;
        }

        @Deprecated
        IRubyObject get(int index) {
            return getPositionArg(index);
        }

        @Deprecated
        IRubyObject getNth(int formatIndex) {
            return getPositionArg(formatIndex);
        }

        @Deprecated
        int nextInt() {
            return intValue(next(null));
        }

        @Deprecated
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
            return (byte)getDecimalFormat(locale).getDecimalSeparator();
        }
    } // Args

    // static methods only
    private Sprintf () {}

    // Special form of sprintf that returns a RubyString.
    public static boolean sprintf(ByteList to, Locale locale, CharSequence format, IRubyObject args) {
        rubySprintfToBuffer(to, format, new Args(locale, args));
        return false;
    }

    // Special form of sprintf that returns a RubyString. Version for 1.9.
    public static boolean sprintf1_9(ByteList to, Locale locale, CharSequence format, IRubyObject args) {
        rubySprintfToBuffer(to, format, new Args(locale, args), false);
        return false;
    }

    public static boolean sprintf(ByteList to, CharSequence format, IRubyObject args) {
        rubySprintf(to, format, new Args(args));
        return false;
    }

    public static boolean sprintf(Ruby runtime, ByteList to, CharSequence format, int arg) {
        rubySprintf(to, format, new Args(runtime, (long)arg));
        return false;
    }

    public static boolean sprintf(Ruby runtime, ByteList to, CharSequence format, long arg) {
        rubySprintf(to, format, new Args(runtime, arg));
        return false;
    }

    public static boolean sprintf(ByteList to, RubyString format, IRubyObject args) {
        rubySprintf(to, format.getByteList(), new Args(args));
        return false;
    }

    private static void rubySprintf(ByteList to, CharSequence charFormat, Args args) {
        rubySprintfToBuffer(to, charFormat, args);
    }

    private static void rubySprintfToBuffer(ByteList buf, CharSequence charFormat, Args args) {
        rubySprintfToBuffer(buf, charFormat, args, true);
    }

    private static void rubySprintfToBuffer(final ByteList buf, final CharSequence charFormat,
                                               final Args args, final boolean usePrefixForZero) {
        final Ruby runtime = args.runtime;
        final byte[] format;
        final Encoding encoding;

        int offset, length, mark;

        if (charFormat instanceof ByteList) {
            ByteList list = (ByteList)charFormat;
            format = list.getUnsafeBytes();
            int begin = list.begin();
            offset = begin;
            length = begin + list.length();
            encoding = list.getEncoding();
        } else {
            format = stringToBytes(charFormat);
            offset = 0;
            length = charFormat.length();
            encoding = UTF8Encoding.INSTANCE;
        }

        while (offset < length) {
            ByteList name = null;
            final int start = offset;
            for ( ; offset < length && format[offset] != '%'; offset++) {}

            if (offset > start) {
                buf.append(format, start, offset - start);
                // start = offset;
            }
            if (offset++ >= length) break;

            IRubyObject arg;
            int flags = 0;
            int width = 0;
            int precision = 0;
            int number;
            byte fchar;
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

                case '0':
                    flags |= FLAG_ZERO;
                    offset++;
                    break;

                case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    // MRI doesn't flag it as an error if width is given multiple
                    // times as a number (but it does for *)
                    number = 0;
                    // GETNUM(n, width) :
                    for (; offset < length && isDigit(fchar = format[offset]); offset++) {
                        number = extendWidth(args, number, fchar, "width too big");
                    }
                    checkOffset(args, offset, length, ERR_MALFORMED_NUM);
                    //
                    if (fchar == '$') {
                        if (args.nextObject != null) {
                            raiseArgumentError(args,"value given twice - " + number + "$");
                        }
                        args.nextObject = args.getPositionArg(number);
                        offset++;
                        break;
                    }
                    if ((flags & FLAG_WIDTH) != 0) raiseArgumentError(args,"width given twice");
                    width = number;
                    flags |= FLAG_WIDTH;
                    break;

                case '<': {
                    int nameStart = ++offset;
                    int nameEnd = nameStart;

                    for ( ; offset < length ; offset++) {
                        if (format[offset] == '>') {
                            nameEnd = offset;
                            offset++;
                            break;
                        }
                    }

                    if (nameEnd == nameStart) raiseArgumentError(args, ERR_MALFORMED_NAME);
                    ByteList newName = new ByteList(format, nameStart, nameEnd - nameStart, encoding, false);
                    if (name != null) raiseArgumentError(args, "named<" + RubyString.newString(runtime, newName) + "> after <" + RubyString.newString(runtime, name) + ">");
                    name = newName;
                    // we retrieve value from hash so we can generate argument error as side-effect.
                    args.nextObject = args.getHashValue(name, '<', '>');

                    break;
                }

                case '{': {
                    int nameStart = ++offset;
                    int nameEnd = nameStart;

                    for ( ; offset < length ; offset++) {
                        if (format[offset] == '}') {
                            nameEnd = offset;
                            offset++;
                            break;
                        }
                    }

                    if (nameEnd == nameStart) raiseArgumentError(args, ERR_MALFORMED_NAME);

                    ByteList localName = new ByteList(format, nameStart, nameEnd - nameStart, encoding, false);
                    IRubyObject value = args.getHashValue(localName, '{', '}');
                    RubyString str = (RubyString) TypeConverter.convertToType(value, runtime.getString(), "to_s");
                    ByteList bytes = str.getByteList();

                    // peek to see if it has a format specifier.  If not we need to handle it now.
                    if (offset < length && !isFormatSpecifier(format[offset]) || offset == length) {
                        int len = bytes.length();
                        Encoding enc = RubyString.checkEncoding(runtime, buf, bytes);
                        if ((flags & (FLAG_PRECISION|FLAG_WIDTH)) != 0) {
                            int strLen = str.strLength();
                            if ((flags & FLAG_PRECISION) != 0 && precision < strLen) {
                                strLen = precision;
                                len = StringSupport.nth(enc, bytes.getUnsafeBytes(), bytes.begin(), bytes.begin() + bytes.getRealSize(), precision);
                                if (len == -1) len = 0; // we might return -1 but MRI's rb_enc_nth does 0 for not-found
                                len = len - bytes.begin();
                            }
                            /* need to adjust multi-byte string pos */
                            if ((flags & FLAG_WIDTH) != 0 && width > strLen) {
                                width -= strLen;
                                if ((flags & FLAG_MINUS) == 0) {
                                    buf.fill(' ', width);
                                    width = 0;
                                }
                                buf.append(bytes.getUnsafeBytes(), bytes.begin(), len);
                                if ((flags & FLAG_MINUS) != 0) {
                                    buf.fill(' ', width);
                                }
                                buf.setEncoding(enc);

                                offset++;
                                incomplete = false;
                                break;
                            }
                        } else {
                            buf.append(bytes);
                            incomplete = false;
                        }
                    } else {
                        buf.append(bytes);
                        incomplete = false;

                    }

                    break;
                }

                case '*':
                    if ((flags & FLAG_WIDTH) != 0) raiseArgumentError(args,"width given twice");
                    flags |= FLAG_WIDTH;
                    int[] p_width = GETASTER(args, format, offset, length, true);
                    offset = p_width[0]; width = p_width[1];
                    if (width < 0) {
                        flags |= FLAG_MINUS;
                        width = -width;
                        if (width < 0) throw runtime.newArgumentError("width too big");
                    }
                    break;

                case '.':
                    if ((flags & FLAG_PRECISION) != 0) {
                        raiseArgumentError(args,"precision given twice");
                    }
                    flags |= FLAG_PRECISION;
                    checkOffset(args, ++offset, length, ERR_MALFORMED_DOT_NUM);
                    fchar = format[offset];
                    if (fchar == '*') {
                        int[] p_prec = GETASTER(args, format, offset, length, false);
                        offset = p_prec[0]; precision = p_prec[1];
                        if (precision < 0) {
                            flags &= ~FLAG_PRECISION;
                        }
                        break;
                    }

                    // GETNUM(prec, precision) :
                    number = 0;
                    for ( ; offset < length && isDigit(fchar = format[offset]); offset++) {
                        number = extendWidth(args, number, fchar, "width too big");
                    }
                    checkOffset(args, offset, length, ERR_MALFORMED_DOT_NUM);
                    precision = number;
                    //
                    break;

                case '\n':
                    offset--;
                case '%':
                    if (flags != FLAG_NONE) {
                        raiseArgumentError(args, ERR_ILLEGAL_FORMAT_CHAR);
                    }
                    buf.append('%');
                    offset++;
                    incomplete = false;
                    break;

                case 'c': {
                    arg = args.getArg();
                    ThreadContext context = runtime.getCurrentContext();

                    int c; int n;
                    IRubyObject tmp = arg.checkStringType();
                    if (!tmp.isNil()) {
                        if (((RubyString) tmp).strLength() != 1) {
                            throw runtime.newArgumentError("%c requires a character");
                        }
                        ByteList bl = ((RubyString) tmp).getByteList();
                        c = StringSupport.codePoint(runtime, encoding, bl.unsafeBytes(), bl.begin(), bl.begin() + bl.realSize());
                        n = StringSupport.codeLength(bl.getEncoding(), c);
                    }
                    else {
                        c = (int) RubyNumeric.num2long(arg) & 0xFFFFFFFF;
                        try {
                            n = StringSupport.codeLength(encoding, c);
                        } catch (EncodingException e) {
                            n = -1;
                        }
                    }
                    if (n <= 0) {
                        throw runtime.newArgumentError("invalid character");
                    }
                    if ((flags & FLAG_WIDTH) == 0) {
                        buf.ensure(buf.length() + n);
                        EncodingUtils.encMbcput(context, c, buf.unsafeBytes(), buf.begin() + buf.realSize(), encoding);
                        buf.realSize(buf.realSize() + n);
                    }
                    else if ((flags & FLAG_MINUS) != 0) {
                        buf.ensure(buf.length() + n);
                        EncodingUtils.encMbcput(context, c, buf.unsafeBytes(), buf.begin() + buf.realSize(), encoding);
                        buf.realSize(buf.realSize() + n);
                        buf.fill(' ', width - 1);
                    }
                    else {
                        buf.fill(' ', width - 1);
                        buf.ensure(buf.length() + n);
                        EncodingUtils.encMbcput(context, c, buf.unsafeBytes(), buf.begin() + buf.realSize(), encoding);
                        buf.realSize(buf.realSize() + n);
                    }
                    offset++;
                    incomplete = false;
                    break;
                }


                case 'a':
                case 'A': {
                    arg = args.getArg();

                    ByteList bytes = new ByteList();
                    final boolean positive = isPositive(arg);
                    double fval = RubyKernel.new_float(runtime, arg).getDoubleValue();
                    boolean negative = fval < 0.0d || (fval == 0.0d && Double.doubleToLongBits(fval) == Double.doubleToLongBits(-0.0));
                    boolean isnan = Double.isNaN(fval);
                    boolean isinf = fval == Double.POSITIVE_INFINITY || fval == Double.NEGATIVE_INFINITY;

                    if (isnan || isinf) {
                        printSpecialValue(buf, flags, width, isnan, negative);

                        offset++;
                        incomplete = false;
                        break;
                    }

                    if ((flags & FLAG_MINUS) != 0) {
                        if (!positive) {
                            bytes.append('-');
                        } else if ((flags & FLAG_PLUS) != 0) {
                            bytes.append('+');
                        } else if ((flags & FLAG_SPACE) != 0) {
                            bytes.append(' ');
                        }

                        bytes.append('0');
                        bytes.append(fchar == 'a' ? 'x' : 'X');
                    }
                    precision = generateBinaryFloat(flags, precision, fchar, bytes, arg);
                    int bytesLength = bytes.length(); // We know numbers will be 7 bit ascii.

                    if ((flags & FLAG_MINUS) == 0) {
                        if (!positive) {
                            buf.append('-');
                        } else if ((flags & FLAG_PLUS) != 0) {
                            buf.append('+');
                        } else if ((flags & FLAG_SPACE) != 0) {
                            buf.append(' ');
                        }
                    }

                    if ((flags & FLAG_MINUS) == 0 && width <= bytesLength) {
                        buf.append('0');
                        buf.append(fchar == 'a' ? 'x' : 'X');
                    } else if (width > bytesLength) {
                        if ((flags & FLAG_ZERO) != 0) {
                            buf.append('0');
                            buf.append(fchar == 'a' ? 'x' : 'X');
                            buf.fill('0', width - bytesLength - 2);
                        } else if ((flags & FLAG_MINUS) == 0) {
                            buf.fill(' ', width - bytesLength - 2);
                            buf.append('0');
                            buf.append(fchar == 'a' ? 'x' : 'X');
                        }
                    }

                    buf.append(bytes);

                    if (width > bytesLength && ((flags & FLAG_MINUS) != 0)) {
                        buf.fill(' ', width - bytesLength);
                    }

                    offset++;
                    incomplete = false;
                    break;
                    }
                case 'p':
                case 's': { // format_s:
                    arg = args.getArg();

                    if (fchar == 'p') {
                        arg = arg.callMethod(runtime.getCurrentContext(), "inspect");
                    }
                    RubyString str = arg.asString();
                    ByteList bytes = str.getByteList();
                    int len = bytes.length();
                    Encoding enc = RubyString.checkEncoding(runtime, buf, bytes);
                    if ((flags & (FLAG_PRECISION|FLAG_WIDTH)) != 0) {
                        int strLen = str.strLength();
                        if ((flags & FLAG_PRECISION) != 0 && precision < strLen) {
                            strLen = precision;
                            len = StringSupport.nth(enc, bytes.getUnsafeBytes(), bytes.begin(), bytes.begin() + bytes.getRealSize(), precision);
                            if (len == -1) len = 0; // we might return -1 but MRI's rb_enc_nth does 0 for not-found
                            len = len - bytes.begin();
                        }
                        /* need to adjust multi-byte string pos */
                        if ((flags & FLAG_WIDTH) != 0 && width > strLen) {
                            width -= strLen;
                            if ((flags & FLAG_MINUS) == 0) {
                                buf.fill(' ', width);
                                width = 0;
                            }
                            buf.append(bytes.getUnsafeBytes(), bytes.begin(), len);
                            if ((flags & FLAG_MINUS) != 0) {
                                buf.fill(' ', width);
                            }
                            buf.setEncoding(enc);

                            offset++;
                            incomplete = false;
                            break;
                        }
                    }
                    buf.append(bytes.getUnsafeBytes(), bytes.begin(), len);
                    buf.setEncoding(enc);

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
                    arg = args.getArg();

                    switch (arg.getMetaClass().getClassIndex()) {
                    case INTEGER: // no-op
                        break;
                    case FLOAT:
                        arg = RubyNumeric.dbl2ival(runtime, ((RubyFloat) arg).getValue());
                        break;
                    case STRING:
                        arg = ((RubyString) arg).stringToInum(0, true);
                        break;
                    default:
                        arg = TypeConverter.convertToInteger(runtime.getCurrentContext(), arg, 0);
                        if (!(arg instanceof RubyInteger)) { // NOTE: likely redundant
                            throw runtime.newTypeError(arg, runtime.getInteger());
                        }
                        break;
                    }
                    byte[] bytes;
                    int first = 0;
                    final boolean sign, negative;
                    byte signChar = 0;
                    byte leadChar = 0;

                    switch (fchar) {
                    case 'd':
                    case 'i':
                        fchar = 'd'; // 'd' and 'i' are the same
                        sign = true; break;
                    case 'u':
                        if ((flags & (FLAG_PLUS|FLAG_SPACE)) != 0) fchar = 'd';
                        sign = true; break;
                    case 'o': case 'x': case 'X': case 'b': case 'B':
                        sign = (flags & (FLAG_PLUS|FLAG_SPACE)) != 0; break;
                    default:
                        sign = false; break;
                    }

                    final int base;
                    switch (fchar) {
                    case 'o':
                        base = 8; break;
                    case 'x': case 'X':
                        base = 16; break;
                    case 'b': case 'B':
                        base = 2; break;
                    // case 'u': case 'd': case 'i':
                    default:
                        base = 10; break;
                    }
                    // We depart here from strict adherence to MRI code, as MRI
                    // uses C-sprintf, in part, to format numeric output, while
                    // we'll use Java's numeric formatting code (and our own).
                    boolean zero;
                    if (arg instanceof RubyFixnum) {
                        final long v = ((RubyFixnum) arg).getLongValue();
                        negative = v < 0;
                        zero = v == 0;
                        if (negative && fchar == 'u') {
                            bytes = getUnsignedNegativeBytes(v);
                        } else {
                            bytes = getFixnumBytes(v, base, sign, fchar == 'X');
                        }
                    } else {
                        final BigInteger v = ((RubyBignum) arg).getValue();
                        negative = v.signum() < 0;
                        zero = v.signum() == 0;
                        if (negative && fchar == 'u' && usePrefixForZero) {
                            bytes = getUnsignedNegativeBytes(v);
                        } else {
                            bytes = getBignumBytes(v, base, sign, fchar == 'X');
                        }
                    }
                    //
                    byte[] prefix = null;
                    if ((flags & FLAG_SHARP) != 0) {
                        if (!zero || usePrefixForZero) {
                            switch (fchar) {
                            case 'o': if (!negative) prefix = PREFIX_OCTAL; break;
                            case 'x': prefix = PREFIX_HEX_LC; break;
                            case 'X': prefix = PREFIX_HEX_UC; break;
                            case 'b': prefix = PREFIX_BINARY_LC; break;
                            case 'B': prefix = PREFIX_BINARY_UC; break;
                            }
                            if (prefix != null) width -= prefix.length;
                        }
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

                            first = skipSignBits(bytes, base);
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
                        buf.fill(' ', width);
                        width = 0;
                    }
                    if (signChar != 0) buf.append(signChar);
                    if (prefix != null) buf.append(prefix);

                    if (len < precision) {
                        if (leadChar == 0) {
                            if (fchar != 'd' || usePrefixForZero || !negative ||
                                    (flags & FLAG_PRECISION) != 0 ||
                                    ((flags & FLAG_ZERO) != 0 && (flags & FLAG_MINUS) == 0)) {
                                buf.fill('0', precision - len);
                            }
                        } else if (leadChar == '.') {
                            buf.fill(leadChar, precision - len);
                            buf.append(PREFIX_NEGATIVE);
                        } else if (!usePrefixForZero) {
                            buf.append(PREFIX_NEGATIVE);
                            buf.fill(leadChar, precision - len - 1);
                        } else {
                            buf.fill(leadChar, precision - len + 1); // the 1 is for the stripped sign char
                        }
                    } else if (leadChar != 0) {
                        if (((flags & (FLAG_PRECISION | FLAG_ZERO)) == 0 && usePrefixForZero) ||
                                (!usePrefixForZero && "xXbBo".indexOf(fchar) != -1)) {
                            buf.append(PREFIX_NEGATIVE);
                        }
                        if (leadChar != '.') buf.append(leadChar);
                    }
                    buf.append(bytes, first, numlen);

                    if (width > 0) buf.fill(' ',width);
                    if (len < precision && fchar == 'd' && negative &&
                            !usePrefixForZero && (flags & FLAG_MINUS) != 0) {
                        buf.fill(' ', precision - len);
                    }

                    offset++;
                    incomplete = false;
                    break;
                }

                case 'f': {
                    arg = args.getArg();
                    RubyInteger num, den;
                    byte sign = (flags & FLAG_PLUS) != 0 ? (byte) 1 : (byte) 0; int zero = 0;

                    if (arg instanceof RubyInteger) {
                        den = RubyFixnum.one(runtime);
                        num = (RubyInteger) arg;
                    }
                    else if (arg instanceof RubyRational) {
                        den = ((RubyRational) arg).getDenominator();
                        num = ((RubyRational) arg).getNumerator();
                    }
                    else {
                        args.nextObject = arg;
                        // goto float_value;
                        num = null; den = null;
                    }

                    if (num != null) { // else -> goto float_value;
                        if ((flags & FLAG_PRECISION) == 0) precision = 6; // default_float_precision;

                        ThreadContext context = runtime.getCurrentContext();

                        if (num.isNegative()) {
                            num = (RubyInteger) num.op_uminus(context);
                            sign = -1;
                        }

                        if (!(den instanceof RubyFixnum) || den.getLongValue() != 1) {
                            num = (RubyInteger) num.op_mul(context, Numeric.int_pow(context, 10, precision));
                            num = (RubyInteger) num.op_plus(context, den.idiv(context, 2));
                            num = (RubyInteger) num.idiv(context, den);
                        }
                        else if (precision >= 0) {
                            zero = precision;
                        }

                        RubyString val = num.to_s();
                        int len = val.length() + zero;
                        if (precision >= len) len = precision + 1; // integer part 0
                        if (sign != 0 || (flags & FLAG_SPACE) != 0) ++len;
                        if (precision > 0) ++len; // period
                        int fill = width > len ? width - len : 0;

                        // CHECK(fill + len)
                        buf.ensure(buf.length() + fill + len);
                        if (fill > 0 && (flags & (FLAG_MINUS|FLAG_ZERO)) == 0) {
                            buf.fill(' ', fill);
                        }
                        if (sign != 0 || (flags & FLAG_SPACE) != 0) {
                            buf.append(sign > 0 ? '+' : sign < 0 ? '-' : ' ');
                        }
                        if (fill > 0 && (flags & (FLAG_MINUS|FLAG_ZERO)) == FLAG_ZERO) {
                            buf.fill('0', fill);
                        }
                        len = val.length() + zero;
                        // t = RSTRING_PTR(val);
                        if (len > precision) {
                            // PUSH_(t, len - prec) :
                            buf.append(val.getByteList(), 0, len - precision);
                        }
                        else {
                            buf.append('0');
                        }
                        if (precision > 0) {
                            buf.append('.');
                        }
                        if (zero > 0) {
                            buf.fill('0', zero);
                        }
                        else if (precision > len) {
                            buf.fill('0', precision - len);
                            // PUSH_(t, len) :
                            buf.append(val.getByteList(), 0, len);
                        }
                        else if (precision > 0) {
                            // PUSH_(t + len - prec, prec) :
                            buf.append(val.getByteList(), len - precision, precision);
                        }
                        if (fill > 0 && (flags & FLAG_MINUS) != 0) {
                            buf.fill(' ', fill);
                        }

                        offset++;
                        incomplete = false;
                        break;
                    }
                }

                case 'E':
                case 'e':
                case 'G':
                case 'g':
                float_value: {
                    arg = args.getArg();

                    double fval = RubyKernel.new_float(runtime, arg).getDoubleValue();
                    boolean isnan = Double.isNaN(fval);
                    boolean isinf = fval == Double.POSITIVE_INFINITY || fval == Double.NEGATIVE_INFINITY;
                    boolean negative = fval < 0.0d || (fval == 0.0d && Double.doubleToLongBits(fval) == Double.doubleToLongBits(-0.0));

                    byte[] digits;
                    int nDigits = 0;
                    int exponent = 0;

                    int len = 0;
                    byte sign;

                    if (isnan || isinf) {
                        printSpecialValue(buf, flags, width, isnan, negative);

                        offset++;
                        incomplete = false;
                        break;
                    }

                    NumberFormat nf = getNumberFormat(args.locale);
                    nf.setMaximumFractionDigits(Integer.MAX_VALUE);
                    String str = nf.format(fval);

                    // grrr, arghh, want to subclass sun.misc.FloatingDecimal, but can't,
                    // so we must do all this (the next 70 lines of code), which has already
                    // been done by FloatingDecimal.
                    int strlen = str.length();
                    digits = new byte[strlen];
                    int nTrailingZeroes = 0;
                    int i = negative ? 1 : 0;
                    int decPos;
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
                        sign = '-';
                        width--;
                    } else if ((flags & FLAG_PLUS) != 0) {
                        sign = '+';
                        width--;
                    } else if ((flags & FLAG_SPACE) != 0) {
                        sign = ' ';
                        width--;
                    } else {
                        sign = 0;
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
                            if (exponent > 99) {
                            	len += 5; // 5 -> e+nnn / e-nnn
                            } else {
                            	len += 4; // 4 -> e+nn / e-nn
                            }

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
                                buf.fill(' ', width);
                                width = 0;
                            }
                            if (sign != 0) {
                                buf.append(sign);
                            }
                            if (width > 0 && (flags & FLAG_MINUS) == 0) {
                                buf.fill('0', width);
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
                                buf.fill(' ', width);
                                width = 0;
                            }
                            if (sign != 0) {
                                buf.append(sign);
                            }
                            if (width > 0 && (flags & FLAG_MINUS) == 0) {
                                buf.fill('0', width);
                                width = 0;
                            }
                            // now some data...
                            if (intLength > 0){
                                if (intDigits > 0) { // s/b true, since intLength > 0
                                    buf.append(digits, 0, intDigits);
                                }
                                if (intZeroes > 0) {
                                    buf.fill('0', intZeroes);
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
                                    buf.fill('0', decZeroes);
                                    precision -= decZeroes;
                                }
                                if (decDigits > 0) {
                                    buf.append(digits, intDigits, decDigits);
                                    precision -= decDigits;
                                }
                                if ((flags & FLAG_SHARP) != 0 && precision > 0) {
                                    buf.fill('0', precision);
                                    precision = 0;
                                }
                            }
                            if ((flags & FLAG_SHARP) != 0 && precision > 0) buf.fill('0', precision);
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
                                int n = round(digits, nDigits, intDigits+precision-decZeroes-1, false);
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
                            buf.fill(' ', width);
                            width = 0;
                        }
                        if (sign != 0) {
                            buf.append(sign);
                        }
                        if (width > 0 && (flags & FLAG_MINUS) == 0) {
                            buf.fill('0', width);
                            width = 0;
                        }
                        // now some data...
                        if (intLength > 0){
                            if (intDigits > 0) { // s/b true, since intLength > 0
                                buf.append(digits, 0, intDigits);
                            }
                            if (intZeroes > 0) {
                                buf.fill('0', intZeroes);
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
                                buf.fill('0', decZeroes);
                                precision -= decZeroes;
                            }
                            if (decDigits > 0) {
                                buf.append(digits, intDigits, decDigits);
                                precision -= decDigits;
                            }
                            // fill up the rest with zeroes
                            if (precision > 0) {
                                buf.fill('0', precision);
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
                        if (exponent > 99) {
                            len += 5; // 5 -> e+nnn / e-nnn
                        } else {
                            len += 4; // 4 -> e+nn / e-nn
                        }

                        if (precision > 0) {
                            // '.' and all precision digits printed
                            len += 1 + precision;
                        } else  if (isSharp) {
                            len++;  // in this mode, '.' is always printed
                        }

                        width -= len;

                        if (width > 0 && (flags & (FLAG_ZERO|FLAG_MINUS)) == 0) {
                            buf.fill(' ', width);
                            width = 0;
                        }
                        if (sign != 0) {
                            buf.append(sign);
                        }
                        if (width > 0 && (flags & FLAG_MINUS) == 0) {
                            buf.fill('0', width);
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
                            if (precision > 0) buf.fill('0', precision);

                        } else if ((flags & FLAG_SHARP) != 0) {
                            buf.append(args.getDecimalSeparator());
                        }

                        writeExp(buf, exponent, expChar);

                        if (width > 0) buf.fill(' ', width);
                        break;
                    } // switch (format char E,e,f,G,g,a,A)

                    offset++;
                    incomplete = false;
                    break;
                } // block (case E,e,f,G,g,a,A)
                } // switch (each format char in spec)
            } // for (each format spec)

            // equivalent to MRI case '\0':
            if (incomplete) {
                if (flags == FLAG_NONE) {
                    // dangling '%' char
                    if (format[length - 1] == '%') raiseArgumentError(args,ERR_INCOMPLETE_FORMAT_SPEC);
                    buf.append('%');
                } else {
                    raiseArgumentError(args,ERR_ILLEGAL_FORMAT_CHAR);
                }
            }
        } // main while loop (offset < length)

        // MRI behavior: validate only the unnumbered arguments
        if (args.rubyHash == null && args.positionIndex >= 0 && args.nextIndex <= args.length) {
            if (args.runtime.isDebug()) {
                args.raiseArgumentError("too many arguments for format string");
            } else if (args.runtime.isVerbose()) {
                args.warn(ID.TOO_MANY_ARGUMENTS, "too many arguments for format string");
            }
        }
    }

    private static boolean isFormatSpecifier(byte b) {
        return "aAbBcdeEfgGiopsuxX".contains(""+b);
    }

    private static int generateBinaryFloat(int flags, int precision, byte fchar, ByteList bytes, IRubyObject arg) {
        long exponent = getExponent(arg);
        final byte[] mantissaBytes = getMantissaBytes(arg);

        if (mantissaBytes[0] == 0) {
            exponent = 0;
            bytes.append('0');
            if (precision > 0 || (flags & FLAG_SPACE) != 0) {
                bytes.append('.');
                while (precision > 0) {
                    bytes.append('0');
                    precision--;
                }
            }
        } else {
            int i = 0;
            int digit = getDigit(i++, mantissaBytes);
            if (digit == 0) {
                digit = getDigit(i++, mantissaBytes);
            }
            assert digit == 1;
            bytes.append('1');
            int digits = getNumberOfDigits(mantissaBytes);
            if (i < digits || (flags & FLAG_SPACE) != 0 || precision > 0) {
                bytes.append('.');
            }

            if ((flags & FLAG_PRECISION) == 0) {
                precision = -1;
            }

            while ((precision < 0 && i < digits) || precision > 0) {
                digit = getDigit(i++, mantissaBytes);
                bytes.append((fchar == 'a' ? HEX_DIGITS : HEX_DIGITS_UPPER_CASE)[digit]);
                precision--;
            }
        }

        bytes.append(fchar == 'a' ? 'p' : 'P');
        if (exponent >= 0) {
            bytes.append('+');
        }
        bytes.append(Long.toString(exponent).getBytes());
        return precision;
    }

    // prints nan or inf
    private static void printSpecialValue(ByteList buf, int flags, int width, boolean isnan, boolean negative) {
        byte sign;
        byte[] digits;
        int len;
        if (isnan) {
            digits = NAN_VALUE;
            len = NAN_VALUE.length;
        } else {
            digits = INFINITY_VALUE;
            len = INFINITY_VALUE.length;
        }
        if (negative) {
            sign = '-';
            width--;
        } else if ((flags & FLAG_PLUS) != 0) {
            sign = '+';
            width--;
        } else if ((flags & FLAG_SPACE) != 0) {
            sign = ' ';
            width--;
        } else {
            sign = 0;
        }
        width -= len;

        if (width > 0 && (flags & FLAG_MINUS) == 0) {
            buf.fill(' ', width);
            width = 0;
        }
        if (sign != 0) buf.append(sign);

        buf.append(digits);
        if (width > 0) buf.fill(' ', width);
    }

    public static NumberFormat getNumberFormat(Locale locale) {
        Map<Locale, NumberFormat> numberFormats = LOCALE_NUMBER_FORMATS.get();
        if (numberFormats == null) {
            numberFormats = new HashMap<Locale, NumberFormat>(4);
            LOCALE_NUMBER_FORMATS.set(numberFormats);
        }
        NumberFormat format = numberFormats.get(locale);
        if ( format == null ) {
            format = NumberFormat.getNumberInstance(locale);
            numberFormats.put(locale, format);
        }
        return format;
    }

    public static DecimalFormatSymbols getDecimalFormat(Locale locale) {
        Map<Locale, DecimalFormatSymbols> decimalFormats = LOCALE_DECIMAL_FORMATS.get();
        if (decimalFormats == null) {
            decimalFormats = new HashMap<Locale, DecimalFormatSymbols>(4);
            LOCALE_DECIMAL_FORMATS.set(decimalFormats);
        }
        DecimalFormatSymbols format = decimalFormats.get(locale);
        if (format == null) {
            format = new DecimalFormatSymbols(locale);
            decimalFormats.put(locale, format);
        }
        return format;
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

    private static void raiseArgumentError(Args args, String message) {
        args.raiseArgumentError(message);
    }

    private static void warning(ID id, Args args, String message) {
        args.warning(id, message);
    }

    private static void checkOffset(Args args, int offset, int length, String message) {
        if (offset >= length) {
            raiseArgumentError(args, message);
        }
    }

    private static int[] GETASTER(final Args args, final byte[] format, int offset, final int length,
                                  final boolean width) {
        checkOffset(args, ++offset, length, ERR_MALFORMED_STAR_NUM);

        final int mark = offset;
        int number = 0; byte fchar = '\0';
        final String errMessage = width ? "width too big" : "prec too big";
        for (; offset < length && isDigit(fchar = format[offset]); offset++) {
            number = extendWidth(args, number, fchar, errMessage);
        }
        checkOffset(args, offset, length, ERR_MALFORMED_STAR_NUM);

        IRubyObject tmp;
        if (fchar == '$') {
            tmp = args.getPositionArg(number);
            offset++;
        } else {
            tmp = args.getNextArg();
            offset = mark;
        }
        return new int[] { offset, args.intValue(tmp) }; // [ offset, prec/width ]
    }

    private static int extendWidth(Args args, int oldWidth, byte newChar, final String errMessage) {
        int newWidth = oldWidth * 10 + (newChar - '0');
        if (newWidth / 10 != oldWidth) raiseArgumentError(args,errMessage);
        return newWidth;
    }

    private static boolean isDigit(byte aChar) {
        return (aChar >= '0' && aChar <= '9');
    }

    private static boolean isPrintable(byte aChar) {
        return (aChar > 32 && aChar < 127);
    }

    private static int skipSignBits(byte[] bytes, int base) {
        int skip = 0;
        int length = bytes.length;
        byte b;
        switch(base) {
        case 2:
            for ( ; skip < length && bytes[skip] == '1'; skip++ ) {}
            break;
        case 8:
            if (length > 0 && bytes[0] == '3') skip++;
            for ( ; skip < length && bytes[skip] == '7'; skip++ ) {}
            break;
        case 10:
            if (length > 0 && bytes[0] == '-') skip++;
            break;
        case 16:
            for ( ; skip < length && ((b = bytes[skip]) == 'f' || b == 'F'); skip++ ) {}
        }
        return skip;
    }

    private static int round(byte[] bytes, final int nDigits, int roundPos, boolean roundDown) {
        final int nextPos = roundPos + 1;
        if (nextPos >= nDigits) return nDigits;
        if (bytes[nextPos] < '5') return nDigits;

        if (roundPos < 0) { // "%.0f" % 0.99
            System.arraycopy(bytes, 0, bytes, 1, nDigits);
            bytes[0] = '1';
            return nDigits + 1;
        }

        if (bytes[nextPos] == '5') {
            if (nextPos == nDigits - 1) {
                if (roundDown || (bytes[roundPos] - '0') % 2 == 0) {
                    return nDigits; // round down (half-to-even)
                }
            }
            // we only need to apply half-to-even rounding
            // if we're at last pos (^^ above) 0.25 -> 0.2
            // or all that is left are zeros 0.2500 -> 0.2 but 0.2501 -> 0.3
            int i = nextPos;
            while (++i < nDigits) {
                if (bytes[i] != '0') {
                    break;
                }
            }
            if (i == nDigits - 1 && (bytes[i] == '0')) {
                if ((bytes[roundPos] - '0') % 2 == 0) {
                    return nDigits; // round down (half-to-even)
                }
            }
        }

        bytes[roundPos] += 1;
        
        while (bytes[roundPos] > '9') {
            bytes[roundPos] = '0';
            roundPos--;
            if (roundPos >= 0) {
                bytes[roundPos] += 1;
            } else {
                System.arraycopy(bytes, 0, bytes, 1, nDigits);
                bytes[0] = '1';
                return nDigits + 1;
            }
        }
        return nDigits;
    }

    private static byte[] getFixnumBytes(final long val, int base, boolean sign, boolean upper) {
        // limit the length of negatives if possible (also faster)
        if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
            if (sign) {
                return ConvertBytes.intToByteArray((int) val, base, upper);
            }
            switch (base) {
                case 2:  return ConvertBytes.intToBinaryBytes((int) val);
                case 8:  return ConvertBytes.intToOctalBytes((int) val);
                case 10:
                default: return ConvertBytes.intToCharBytes((int) val);
                case 16: return ConvertBytes.intToHexBytes((int) val, upper);
            }
        } else {
            if (sign) {
                return ConvertBytes.longToByteArray(val,base,upper);
            }
            switch (base) {
                case 2:  return ConvertBytes.longToBinaryBytes(val);
                case 8:  return ConvertBytes.longToOctalBytes(val);
                case 10:
                default: return ConvertBytes.longToCharBytes(val);
                case 16: return ConvertBytes.longToHexBytes(val, upper);
            }
        }
    }

    private static byte[] getBignumBytes(final BigInteger val, int base, boolean sign, boolean upper) {
        if (sign || base == 10 || val.signum() >= 0) {
            return stringToBytes(val.toString(base),upper);
        }
        // negative values
        switch (base) {
            case 2:  return ConvertBytes.twosComplementToBinaryBytes(val.toByteArray());
            case 8:  return ConvertBytes.twosComplementToOctalBytes(val.toByteArray());
            case 16: return ConvertBytes.twosComplementToHexBytes(val.toByteArray(), upper);
            default: return stringToBytes(val.toString(base), upper);
        }
    }

    private static byte[] getUnsignedNegativeBytes(final long val) {
        if (val < 0) {
            return ConvertBytes.longToCharBytes(((Long.MAX_VALUE + 1L) << 1) + val);
        }
        return getUnsignedNegativeBytes(BigInteger.valueOf(val));
    }

    private static byte[] getUnsignedNegativeBytes(final BigInteger val) {
        int shift = 0;
        // go through negated powers of 32 until we find one small enough
        for (BigInteger minus = BIG_MINUS_64; val.compareTo(minus) < 0 ; minus = minus.shiftLeft(32), shift++) {}
        // add to the corresponding positive power of 32 for the result.
        // meaningful? no. conformant? yes. I just write the code...
        BigInteger nPower32 = shift > 0 ? BIG_64.shiftLeft(32 * shift) : BIG_64;
        return stringToBytes(nPower32.add(val).toString());
    }

    private static byte[] stringToBytes(CharSequence s, boolean upper) {
        if (upper) {
            int len = s.length();
            byte[] bytes = new byte[len];
            for (int i = len; --i >= 0; ) {
                int b = (byte)((int)s.charAt(i) & (int)0xff);
                if (b >= 'a' && b <= 'z') {
                    bytes[i] = (byte)(b & ~0x20);
                } else {
                    bytes[i] = (byte)b;
                }
            }
            return bytes;
        }
        return stringToBytes(s);
    }

    private static byte[] stringToBytes(CharSequence s) {
        return ByteList.plain(s);
    }

    private static int getNumberOfDigits(byte[] bytes) {
        int digits = bytes.length * 2;
        if (getDigit(digits - 1, bytes) == 0) {
            digits--;
        }
        return digits;
    }

    private static byte getDigit(int position, byte[] bytes) {
        int index = position / 2;
        if (index < bytes.length) {
            byte twoDigits = bytes[index];
            if (position % 2 == 0) {
                return (byte) ((twoDigits >> 4) & 0xf);
            } else {
                return (byte) (twoDigits & 0xf);
            }
        } else {
            return 0;
        }
    }

    private static boolean isPositive(Object value) {
        if (value instanceof RubyFloat) {
            final long bits = Double.doubleToRawLongBits(((RubyFloat) value).getDoubleValue());
            return (bits & SIGN_MASK) == 0;
        } else if (value instanceof RubyFixnum) {
            return ((RubyFixnum) value).getLongValue() >= 0;
        } else if (value instanceof RubyBignum) {
            return ((RubyBignum) value).signum() >= 0;
        }
        return true;
    }

    private static final long SIGN_MASK = 1L << 63;
    private static final long BIASED_EXP_MASK = 0x7ffL << 52;
    private static final long MANTISSA_MASK = ~(SIGN_MASK | BIASED_EXP_MASK);
    private static final byte[] HEX_DIGITS = new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    private static final byte[] HEX_DIGITS_UPPER_CASE = new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private static byte[] getMantissaBytes(IRubyObject value) {
        BigInteger bi;
        if (value instanceof RubyFloat) {
            final long bits = Double.doubleToRawLongBits(((RubyFloat) value).getDoubleValue());
            long biasedExp = ((bits & BIASED_EXP_MASK) >> 52);
            long mantissaBits = bits & MANTISSA_MASK;
            if (biasedExp > 0) {
                mantissaBits = mantissaBits | 0x10000000000000L;
            }
            bi = BigInteger.valueOf(mantissaBits);
        } else if (value instanceof RubyFixnum) {
            bi = BigInteger.valueOf(((RubyFixnum) value).getLongValue());
        } else if (value instanceof RubyBignum) {
            bi = ((RubyBignum) value).getValue();
        } else {
            bi = BigInteger.ZERO;
        }
        bi = bi.abs();
        if (BigInteger.ZERO.equals(bi)) {
            return new byte[1];
        }

        // Shift things to get rid of all the trailing zeros.
        bi = bi.shiftRight(bi.getLowestSetBit() - 1);

        // We want the bit length to be 4n + 1 so that things line up nicely for the printing routine.
        int bitLength = bi.bitLength() % 4;
        if (bitLength != 1) {
            bi = bi.shiftLeft(5 - bitLength);
        }
        return bi.toByteArray();
    }

    private static long getExponent(IRubyObject value) {
        if (value instanceof RubyBignum) {
            return ((RubyBignum) value).getValue().abs().bitLength() - 1;
        } else if (value instanceof RubyFixnum) {
            long lval = ((RubyFixnum) value).getLongValue();
            return lval == Long.MIN_VALUE ? 63 : 63 - Long.numberOfLeadingZeros(Math.abs(lval));
        } else if (value instanceof RubyFloat) {
            final long bits = Double.doubleToRawLongBits(((RubyFloat) value).getDoubleValue());
            long biasedExp = ((bits & BIASED_EXP_MASK) >> 52);
            long mantissaBits = bits & MANTISSA_MASK;
            if (biasedExp == 0) {
                // Sub normal cases are a little special.
                // Find the most significant bit in the mantissa
                final int lz = Long.numberOfLeadingZeros(mantissaBits);
                // Adjust the exponent to reflect this.
                biasedExp = biasedExp - (lz - 12);
            }
            return biasedExp - 1023;
        }
        return 0;
    }

}
