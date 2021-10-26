package org.jruby.util;

import org.jcodings.Encoding;
import org.jcodings.exception.EncodingException;
import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyString;
<<<<<<< HEAD
import org.jruby.exceptions.ArgumentError;
=======
import org.jruby.common.IRubyWarnings;
>>>>>>> Add in too few args error checking
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.EncodingUtils;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import static org.jruby.util.Sprintf.*;

public class SprintfParser {
    public static ByteToken PERCENT = new ByteToken(new ByteList(new byte[] {'%'}));

    public static List<Token> lex(ThreadContext context, ByteList format) {
        List<Token> list = new LinkedList<>();
        Lexer lexer = new Lexer(context, format);

        for (Token token = lexer.next(); token != Lexer.EOFToken; token = lexer.next()) {
            list.add(token);
        }

        return list;
    }

    public static boolean sprintf(ByteList buf, ByteList format, Sprintf.Args args, boolean usePrefixForZero) {
        ThreadContext context = args.runtime.getCurrentContext();
        // FIXME: after working completely we need to make sprintfCompile which will save pre-lexed data into callsite so we stop reparsing format strings.
        List<Token> tokens = lex(context, format);

        // For now only allow what is implemented.
        for (Token token: tokens) {
            if (token instanceof FormatToken) {
                if ("pscbBdoxX".indexOf(((FormatToken) token).format) == -1) return false;
            }
        }

        // For special case of "" where no tokens are produced.
        if (tokens.isEmpty() && args.length > 0) tooManyArguments(args);

        for (Token token: tokens) {
            if (token instanceof ByteToken) {
                buf.append(((ByteToken) token).getBytes());
            } else {
                FormatToken f = (FormatToken) token;

                switch (f.format) {
                    case 'd': // (%i, %u) %i is an alias for %d.  %u is largely %d.
                        format_idu(context, buf, args, f, usePrefixForZero);
                        break;
                    case 'c':
                        format_c(context, buf, args, f, usePrefixForZero);
                        break;
                    case 'p':
                    case 's':
                        format_ps(context, buf, args, f, usePrefixForZero);
                        break;
                    case 'b':
                    case 'B':
                    case 'o':
                    case 'x':
                    case 'X':
                        format_bBxX(context, buf, args, f, usePrefixForZero);
                        break;
                    default:
                        return false;
                }
            }

            if (args.positionIndex >= 0 && args.nextIndex < args.length) tooManyArguments(args);
        }
        return true;
    }

    private static void tooManyArguments(Args args) {
        if (args.runtime.isDebug()) {
            args.raiseArgumentError("too many arguments for format string");
        } else if (args.runtime.isVerbose()) {
            args.warn(IRubyWarnings.ID.TOO_MANY_ARGUMENTS, "too many arguments for format string");
        }
    }

    // FIXME: Once precision is added look at all three get*Arg versions and maybe just make a single one with extra params.
    private static IRubyObject getArg(FormatToken f, Sprintf.Args args) {
        if (f.isArgIndexed()) {
            return args.getPositionArg(f.index);
        } else if (f.name != null) {
            if (f.angled) {
                return args.getHashValue(f.name, '<', '>');
            } else {
                return args.getHashValue(f.name, '{', '}');
            }
        } else {
            return args.getArg();
        }
    }

    private static IRubyObject getPrecisionArg(FormatToken f, Sprintf.Args args) {
        if (f.hasPrecisionIndex) { // numbered
            return args.getPositionArg(f.precision);
        } else if (f.name != null) {
            if (f.angled) {  // name, key
                return args.getHashValue(f.name, '<', '>');
            } else {
                return args.getHashValue(f.name, '{', '}');
            }
        } else { // unnumbered
            return args.getArg();
        }
    }

    private static int getPrecisionArg(ThreadContext context, FormatToken f, Sprintf.Args args) {
        // FIXME:  This is form fitted nonsense.  I need to make a helper and simplify the fields in FormatToken.
        IRubyObject precision = (f.hasPrecision && f.precision == -1) || f.hasPrecisionIndex ? TypeConverter.convertToInteger(context, getPrecisionArg(f, args), 0) : null;

        return precision == null ? f.precision : args.intValue(precision);
    }

    private static IRubyObject getWidthArg(FormatToken f, Sprintf.Args args) {
        if (f.isWidthIndexed()) { // numbered
            return args.getPositionArg(f.width);
        } else if (f.name != null) {
            if (f.angled) {  // name, key
                return args.getHashValue(f.name, '<', '>');
            } else {
                return args.getHashValue(f.name, '{', '}');
            }
        } else { // unnumbered
            return args.getArg();
        }
    }

    private static int getWidthArg(ThreadContext context, FormatToken f, Sprintf.Args args) {
        IRubyObject starWidth = f.hasWidth ? TypeConverter.convertToInteger(context, getWidthArg(f, args), 0) : null;

        return starWidth == null ? f.width : args.intValue(starWidth);
    }

    private static void format_bBxX(ThreadContext context, ByteList buf, Sprintf.Args args, FormatToken f, boolean usePrefixForZero) {
        int width = getWidthArg(context, f, args);
        int precision = getPrecisionArg(context, f, args);
        boolean rightPad = f.rightPad; // args can modify rightPad so we use a local instead.
        IRubyObject arg = TypeConverter.convertToInteger(context, getArg(f, args), 0);
        boolean negative;
        boolean zero;
        byte[] bytes;
        boolean sign = f.plusPrefix || f.spacePad;

        if (width < 0) { // Negative widths pad from the right.
            width *= -1;
            rightPad = true;
        }

        if (arg instanceof RubyFixnum) {
            final long v = ((RubyFixnum) arg).getLongValue();
            negative = v < 0;
            zero = v == 0;
            bytes = getFixnumBytes(v, f.base, sign, f.format == 'X');
        } else {
            final BigInteger v = ((RubyBignum) arg).getValue();
            negative = v.signum() < 0;
            zero = v.signum() == 0;
            bytes = getBignumBytes(v, f.base, sign, f.format == 'X');
        }

        byte[] prefix = null;
        // Only %o will not put the prefix in front of it in the case of negative values????
        if (f.hexZero && !(f.format == 'o' && negative) && (!zero || usePrefixForZero)) {
            prefix = f.prefix;
            width -= f.prefix.length;
        }

        int leadChar = 0;
        int first = 0;
        int signChar = 0;

        int len = 0;
        if (sign) {
            if (negative) {
                signChar = '-';
                width--;
                first = 1; // skip '-' in bytes, will add where appropriate
            } else if (f.plusPrefix) {
                signChar = '+';
                width--;
            } else if (f.spacePad) {
                signChar = ' ';
                width--;
            }
        } else if (negative) {
            if (!f.hasPrecision && !f.zeroPad) len += 2; // '..'

            first = skipSignBits(bytes, f.base);
            leadChar = f.leadChar;
            len++; // for f.leadChar we will be adding
        }
        int numlen = bytes.length - first;
        len += numlen;

        if (f.zeroPad && !f.hasPrecision) {
            precision = width;
            width = 0;
        } else {
            if (precision < len) precision = len;

            width -= precision;
        }

        if (!rightPad) {
            buf.fill(' ', width);
            width = 0;
        }
        if (signChar != 0) buf.append(signChar);
        if (prefix != null) buf.append(prefix);

        if (len < precision) {
            if (leadChar == 0) {
                buf.fill('0', precision - len);
            } else if (!usePrefixForZero) {
                buf.append(PREFIX_NEGATIVE);
                buf.fill(f.leadChar, precision - len - 1);
            } else {
                buf.fill(leadChar, precision - len + 1); // the 1 is for the stripped sign char
            }
        } else if (leadChar != 0) {
            if (!f.hasPrecision && !f.zeroPad || !usePrefixForZero) buf.append(PREFIX_NEGATIVE);
            buf.append(leadChar);
        }
        buf.append(bytes, first, numlen);

        if (width > 0) buf.fill(' ', width);
    }

    private static void format_ps(ThreadContext context, ByteList buf, Sprintf.Args args, FormatToken f, boolean usePrefixForZero) {
        int width = getWidthArg(context, f, args);
        int precision = getPrecisionArg(context, f, args);
        boolean hasPrecision = precision >= 0;
        boolean hasWidth = width > 0;
        boolean rightPad = f.rightPad;
        IRubyObject arg = getArg(f, args);

        if (width < 0) {
            throw context.runtime.newArgumentError("width to %s too big");
        }

        if (f.format == 'p') {
            arg = arg.callMethod(context, "inspect");
        }

        RubyString str = arg.asString();
        int strLen = str.strLength();
        ByteList bytes = str.getByteList();
        int len = bytes.length();
        Encoding encoding = RubyString.checkEncoding(context.runtime, buf, bytes);

        if (hasPrecision || hasWidth) {
            if (hasPrecision && precision < strLen) {
                strLen = precision;
                len = StringSupport.nth(encoding, bytes.getUnsafeBytes(), bytes.begin(), bytes.begin() + bytes.realSize(), precision);
                if (len == -1) len = 0;
                len = len - bytes.begin();
            }
            if (hasWidth && width > strLen) {
                width -= strLen;
                if (!rightPad) {
                     buf.fill(' ', width);
                     width = 0;
                }
                buf.append(bytes.getUnsafeBytes(), bytes.begin(), len);
                if (rightPad) {
                    buf.fill(' ', width);
                }
                buf.setEncoding(encoding);
                return;
            }
        }
        buf.append(bytes.getUnsafeBytes(), bytes.begin(), len);
        buf.setEncoding(encoding);
    }

    private static void format_c(ThreadContext context, ByteList buf, Sprintf.Args args, FormatToken f, boolean usePrefixForZero) {
        int width = getWidthArg(context, f, args);
        boolean rightPad = f.rightPad;
        IRubyObject arg = getArg(f, args);
        Encoding encoding;
        int codePoint, codeLen;

        encoding = buf.getEncoding();

        if (arg instanceof RubyString) {
            final RubyString rs = ((RubyString) arg);
            ByteList bl = rs.getByteList();
            if (rs.strLength() != 1) {
                throw context.runtime.newArgumentError("%c requires a character");
            }
            codePoint = StringSupport.codePoint(context.runtime, encoding, bl.unsafeBytes(), bl.begin(), bl.begin() + bl.realSize());
            codeLen = StringSupport.codeLength(bl.getEncoding(), codePoint);
        } else {
            codePoint = (int) arg.convertToInteger().getLongValue() & 0xFFFFFFFF;
            try {
                codeLen = StringSupport.codeLength(encoding, codePoint);
            } catch (EncodingException e) {
                codeLen = -1;
            }
        }

        if (codeLen <= 0) {
            throw context.runtime.newArgumentError("invalid character");
        }

        if (!rightPad) {
            buf.fill(' ', width-1);
            width = 0;
        }

        buf.ensure(buf.length() + codeLen);
        EncodingUtils.encMbcput(codePoint, buf.unsafeBytes(), buf.realSize(), encoding);
        buf.realSize(buf.realSize() + codeLen);

        if (width > 0) buf.fill(' ', width-1);
    }

    private static void format_idu(ThreadContext context, ByteList buf, Sprintf.Args args, FormatToken f, boolean usePrefixForZero) {
        int width = getWidthArg(context, f, args);
        int precision = getPrecisionArg(context, f, args);
        boolean rightPad = f.rightPad; // args can modify rightPad so we use a local instead.
        IRubyObject arg = TypeConverter.convertToInteger(context, getArg(f, args), 0);
        boolean negative;
        byte[] bytes;

        if (width < 0) { // Negative widths pad from the right.
            width *= -1;
            rightPad = true;
        }

        if (arg instanceof RubyFixnum) {
            final long v = ((RubyFixnum) arg).getLongValue();
            negative = v < 0;
            if (negative && f.unsigned) {
                bytes = getUnsignedNegativeBytes(v);
            } else if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                bytes = ConvertBytes.intToByteArray((int) v, 10, false);
            } else {
                bytes = ConvertBytes.longToCharBytes(v);
            }
        } else {
            final BigInteger v = ((RubyBignum) arg).getValue();
            negative = v.signum() < 0;
            if (negative && f.unsigned && usePrefixForZero) {
                bytes = getUnsignedNegativeBytes(v);
            } else {
                bytes = stringToBytes(v.toString(10), false);
            }
        }

        int first = 0;
        int signChar = 0;

        if (negative) {
            signChar = '-';
            width--;
            first = 1; // skip '-' in bytes, will add where appropriate
        } else if (f.plusPrefix) {
            signChar = '+';
            width--;
        } else if (f.spacePad) {
            signChar = ' ';
            width--;
        }

        int numlen = bytes.length - first;

        if (f.zeroPad && f.width != 0) {
            precision = width;
            width = 0;
        } else {
            if (precision < numlen) precision = numlen;

            width -= precision;
        }

        if (!rightPad) {
            buf.fill(' ', width);
            width = 0;
        }
        if (signChar != 0) buf.append(signChar);

        if (numlen < precision) {
            if (!negative || f.width != 0 || f.hasPrecision || (f.zeroPad && !rightPad)) {
                buf.fill('0', precision - numlen);
            }
        }
        buf.append(bytes, first, numlen);

        if (width > 0) buf.fill(' ', width);
        if (numlen < precision && negative && rightPad) {
            buf.fill(' ', precision - numlen);
        }
    }

    static class Token {
    }

    static class ByteToken extends Token {
        private final ByteList bytes;

        public ByteToken(ByteList bytes) {
            this.bytes = bytes;
        }

        public ByteList getBytes() {
            return bytes;
        }

        public String toString() {
            return "BYTES[" + (bytes.realSize() != 0 ? bytes.toString() : "''") + "]";
        }
    }

    // FIXME: none of these fields need to be public.
    // FIXME: we have integralPad which probably should be combined with argumentIndex as long as it is not confusing (like use access methods).
    /*
     * Data structure for how an argument should be formatted.  This structure is not meant to be modified
     * after it leaves the parser.  Any modifiers which rely on arguments passed to sprintf should not
     * change this structure.  Adding code to enforce this property would be a pain so we will just work
     * with convention.
     *
     * The reason this is meant to be immutable once parsed is that we need this data to preserve across
     * multiple sprintf invocations.  If a previous call was capable of modifying it we would start seeing
     * things like values go from left padding to right padding.
     */
    static class FormatToken extends Token {
        int base = 10;          // which base of number are we formatting
        boolean spacePad;       // '% d' (FLAG_SPACE in MRI)
        boolean plusPrefix;     // '%+d' (FLAG_PLUS in MRI)
        boolean rightPad;       // (FLAG_MINUS in MRI)
        boolean hexZero;        // (FLAG_SHARP in MRI)
        boolean zeroPad;
        int format;
        boolean hasWidth;          // width part expressed as syntax '%3d', '%*3d', '%3.1d' ...
        boolean hasPrecision;      // precision part expressed as syntax '%.3d', '%3.1d', '%3.*1$d' ...
        boolean hasPrecisionIndex; // Is the precision value referring to an index instead of a value?
        int index = -1;         // positional index to use in this format
        int width;              // numeric value if explicitly stated (index or explicit value)
        int precision = -1;          // numeric value if explicitly stated (index or explicit value)
        ByteList name;
        int exponent;
        boolean unsigned;       // 'u' will process argument value differently sometimes (otherwise like 'd').
        byte[] prefix;          // put on front of value unless !0 or explicitly requested (useZeroForPrefix).
        boolean angled;         // if name if is curly ('{') or angled ('<')?
        byte leadChar;

        public boolean isArgIndexed() {
            return index >= 0;
        }

        public boolean isIndexed() {
            return isArgIndexed() || hasPrecisionIndex;
        }

        public boolean isNamed() {
            return name != null;
        }

        public boolean isWidthIndexed() {
            return hasWidth && width > 0;
        }

        public String toString() {
            return "Format[%" + (char) format +
                    (name != null ?  ",name=" + (name.realSize() != 0 ? name : "''") : "") +
                    //(hasWidth ? ",width.prec=" + width + (widthIndex ? "$" : "") : "") +
                    (hasPrecision ? "." + precision + (hasPrecisionIndex ? "$" : "") : "") +
                    (isArgIndexed() ? ",index=" + index: "") +
                    (spacePad ? ",space_pad" : "") +
                    (plusPrefix ? ",plus" : "") +
                    (rightPad ? ",right_pad" : "") +
                    (hexZero ? ",hex_zero" : "") +
                    (zeroPad ? ",zero_pad" : "") +
                    (exponent != 0 ? ",exponent: " +(char) exponent : "") +
                    "]";
        }
    }

    static class Lexer {
        enum First { UNNUMBERED, NUMBERED, NAMED}

        private static final int EOF = -1;
        public static Token EOFToken = new Token();
        private final ByteList format;
        private int index = 0;
        private int current = 0;
        private final ThreadContext context;
        private First foundFirst;
        private int formatTokensFound = 0;

        public Lexer(ThreadContext context, ByteList format) {
            this.context = context;
            this.format = format;
        }

        public Token next() {
            int character = nextChar();

            if (character == '%') {
                return processPercent();
            } else if (character == EOF) {
                return EOFToken;
            } else {
                Token test = processTextToken();
                if (current != EOF) index--;
                return test;
            }
        }

        private static boolean isDecimalDigit(int character) {
            return character >= '0' && character <= '9';
        }

        private int nextChar() {
            if (index < format.realSize()) {
                // FIXME: MBS vs ASCII
                current = format.get(index);
                index++;
            } else {
                current = EOF;
            }

            return current;
        }

        // unread so nextChar() call will continue to return the current 'current'.
        private void unread() {
            index--;
        }

        private ByteList bytelistUpto(char delimiter) {
            int character = nextChar();
            int count = 1;

            for (; character != delimiter && character != EOF; count++) {
                character = nextChar();
            }

            // start index should only be subtracted by real characters (not EOF).
            int startIndex = index - count + (character == EOF ? 1 : 0);
            return format.makeShared(startIndex, count - 1);
        }

        private int processDigits(boolean inWidth, boolean inPrecision) {
            int count =  current - '0';

            for (int character = nextChar(); character != EOF; character = nextChar()) {
                if (!isDecimalDigit(character)) break;

                count = count * 10 + (character - '0');
                if (count < 0) {
                    if (inPrecision) argumentError("prec too big");
                    if (inWidth) argumentError("width too big");
                    // This is a bit odd but we are requesting soo many arguments we overflow so we cannot have enough.
                    argumentError("too few arguments");
                }
            }

            return count;
        }

        private Token processTextToken() {
            index--;
            return new ByteToken(bytelistUpto('%'));
        }

        // %[flags][width][.precision]type
        private Token processPercent() {
            int character = nextChar();

            if (character == '%') return PERCENT;
            if (character == '\n' || character == '\0') { // '%(\n|\0)'
                unread();
                return PERCENT;
            }

            FormatToken token = new FormatToken();
            processModifiers(token);
            verifyHomogeneous(token);

            // Found %{name} or %<name>.  %{} has no additional formatting so just return now.
            if (token.name != null && !token.angled) return token;

            switch (current) {
                case '%':
                    // %{somemods}% is illegal
                    break;
                case 'c': // Numeric code for a single character or a single character string itself.
                case 's': // Argument is a string to be substituted
                case 'p': // The valuing of argument.inspect.
                case 'd': // Convert argument as a decimal number.
                    token.base = 10;
                    token.format = current;
                    break;
                case 'o': // Convert argument as an octal number.
                    token.base = 8;
                    token.format = current;
                    token.leadChar = '7';
                    if (token.hexZero) token.prefix = PREFIX_OCTAL;
                    break;
                case 'x': // Convert argument as a hexadecimal number.
                    token.base = 16;
                    token.format = current;
                    token.leadChar = 'f';
                    if (token.hexZero) token.prefix = PREFIX_HEX_LC;
                    break;
                case 'X': // Equivalent to `x', but uses uppercase letters.
                    token.base = 16;
                    token.format = current;
                    token.leadChar = 'F';
                    if (token.hexZero) token.prefix = PREFIX_HEX_UC;
                    break;
                case 'b': // binary number
                    token.base = 2;
                    token.format = current;
                    token.leadChar = '1';
                    if (token.hexZero) token.prefix = PREFIX_BINARY_LC;
                    break;
                case 'B': // binary number with 0B prefix
                    token.base = 2;
                    token.format = current;
                    token.leadChar = '1';
                    if (token.hexZero) token.prefix = PREFIX_BINARY_UC;
                    break;
                case 'f': // Convert floating point argument as [-]ddd.dddddd,
                    token.format = current;
                    break;
                case 'E': // Equivalent to `e', but uses an uppercase E to indicate the exponent.
                    token.exponent = 'E';
                    token.format = 'e';
                    break;
                case 'e': // Convert floating point argument into exponential notation.
                    token.exponent = 'e';
                    token.format = 'e';
                    break;
                case 'G': // Equivalent to `g', but use an uppercase `E' in exponent form.
                    token.exponent = 'E';
                    token.format = 'g';
                    break;
                case 'g': // Convert a floating point number using exponential form.
                    token.exponent = 'e';
                    token.format = 'g';
                    break;
                case 'a': // Convert floating point argument as [-]0xh.hhhhp[+-]dd.
                    token.exponent = 'p';
                    token.format = 'a';
                    break;
                case 'A': // Equivalent to `a', but use uppercase `X' and `P'.
                    token.exponent = 'P';
                    token.format = 'A';
                case 'i': // Same as 'd'
                    token.format = 'd';
                    break;
                case 'u': // 'u' is almost exactly 'u' but can do some unsigned math on arg.
                    if (!token.plusPrefix && !token.spacePad) token.unsigned = true;
                    token.format = 'd';
                    break;
                case -1: // Why can't I use EOF
                    break;
                default:
                    error("unexpected: " + (char) current); // illegal format character.
            }

            return token;
        }

        private void argumentError(String message) {
            throw context.runtime.newArgumentError(message);
        }

        private void verifyHomogeneous(FormatToken token) {
            formatTokensFound++;

            if (foundFirst == null) {
                foundFirst = token.isNamed() ? First.NAMED : (token.isIndexed() ? First.NUMBERED : First.UNNUMBERED);
            } else {
                if (token.isIndexed()) {
                    switch (foundFirst) {
                        case UNNUMBERED:
                            argumentError("numbered(" + token.index + ") after unnumbered(" + (formatTokensFound - 1) + ")");
                        case NAMED:
                            argumentError("numbered(" + token.index + ") after named");
                    }
                } else if (token.isNamed()) {
                    char start = token.angled ? '<' : '{';
                    char end = token.angled ? '>' : '}';
                    switch (foundFirst) {
                        case UNNUMBERED:
                            // FIXME: Names should be mbs
                            argumentError("named" + start + RubyString.newString(context.runtime, token.name) + end + " after unnumbered(" + (formatTokensFound - 1) + ")");
                        case NUMBERED:
                            // FIXME: Names should be mbs
                            argumentError("named" + start + RubyString.newString(context.runtime, token.name) + end + " after numbered");
                    }
                } else {
                    switch (foundFirst) {
                        case NUMBERED:
                            argumentError("unnumbered(" + formatTokensFound + ") mixed with numbered");
                        case NAMED:
                            argumentError("unnumbered(" + formatTokensFound + ") mixed with named");

                    }
                }
            }
        }

        // FIXME: multiple names should error
        private void processModifiers(FormatToken token) {
            boolean inPrecision = false;  // Are we processing precision part of the format modifier (e.g. after the '.').
            boolean inWidth = false;

            for (int character = current; character != EOF; character = nextChar()) {
                switch (character) {
                    case ' ':    // space pad values
                        token.spacePad = true;
                        break;
                    case '+':    // + prefix positive values
                        token.plusPrefix = true;
                        break;
                    case '-':    // right pad space
                        token.rightPad = true;
                        break;
                    case '#':    // first 0 digit in hex/binary display
                        token.hexZero = true;
                        break;
                    case '0':    // zero pad
                        if (inPrecision) {  // %5.0d
                            token.precision = 0;
                            inPrecision = false;
                        } else if (!token.rightPad) { // '-' takes precedence over '0'
                            token.zeroPad = true;
                        }
                        break;
                    case '*':    // use arg list to calculate pad ('%1$*2$d', '%1$*2$d')
                        if (!token.hasPrecision) { // initial '*'.
                            if (token.hasWidth) error("width given twice");

                            token.hasWidth = true; // hasWidth with no width == bare '*'. Otherwise specific index.
                            inWidth = true;
                        } // Otherwise: '*' for '{width}.*'  [nothing to do here as it will got to 0-9 next.
                        break;
                    case '1': case '2': case '3': case '4': case '5':  // numbers representing specific field values '%3d'
                    case '6': case '7': case '8': case '9': {          // OR indices '%*1$2$d'
                        int amount = processDigits(inWidth, inPrecision);

                        if (inPrecision) {
                            inPrecision = false;
                            if (token.hasPrecisionIndex) {          // index of value for format '%3.*1$2$d'
                                if (current != '$') error("width given twice");
                                token.index = amount;
                            } else {                          // index of precision '%3.*1$'.
                                if (current == '$') token.hasPrecisionIndex = true;
                                token.precision = amount;
                            }
                        } else if (inWidth) {
                            inWidth = false;
                            if (current != '$') error("width given twice");
                            token.width = amount;
                        } else {
                            if (current == '$') {
                                token.index = amount;
                            } else {
                                token.width = amount;
                            }
                        }

                        if (current != '$') {
                            if (token.isWidthIndexed()) error("width given twice");
                            if (current == EOF) return;
                            unread();
                        }

                        break;
                    }
                    case '.':
                        if (token.hasPrecision) error("precision given twice");

                        token.hasPrecision = true;
                        inPrecision = true;
                        break;
                    case '<': {
                        token.angled = true;
                        ByteList name = bytelistUpto('>');
                        // MBS for string names
                        if (token.name != null) argumentError("named<" + RubyString.newString(context.runtime, name) + "> after <" + RubyString.newString(context.runtime, token.name) + ">");
                        token.name = name;
                        break;
                    }
                    case '{': {
                        ByteList name = bytelistUpto('}');
                        if (token.name != null) argumentError("named<" + RubyString.newString(context.runtime, name) + "> after <" + RubyString.newString(context.runtime, token.name) + ">");
                        token.name = name;
                        return;
                    }
                    default:
                        return;
                }
            }
        }

        private void error(String message) {
            throw context.runtime.newArgumentError(message);
        }
    }
}



