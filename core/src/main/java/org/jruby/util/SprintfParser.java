package org.jruby.util;

import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
                if (((FormatToken) token).format != 'd') return false;
            }
        }

        for (Token token: tokens) {
            if (token instanceof ByteToken) {
                buf.append(((ByteToken) token).getBytes());
            } else {
                FormatToken f = (FormatToken) token;

                switch (f.format) {
                    case 'd': // (%i, %u) %i is an alias for %d.  %u is largely %d.
                        format_idu(context, buf, args, f, usePrefixForZero);
                        break;
                    default:
                        return false;
                }
            }
        }
        return true;
    }

    private static IRubyObject getArg(FormatToken f, Sprintf.Args args) {
        if (f.indexedArg()) {
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

    private static void format_idu(ThreadContext context, ByteList buf, Sprintf.Args args, FormatToken f, boolean usePrefixForZero) {
        IRubyObject arg = TypeConverter.convertToInteger(context, getArg(f, args), 0);
        boolean negative;
        byte[] bytes;
        int width = f.width;

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
        int len = numlen;

        int precision = f.precision;
        if (f.zeroPad && f.width != 0) {
            precision = width;
            width = 0;
        } else {
            if (precision < len) precision = len;

            width -= precision;
        }

        if (!f.rightPad) {
            buf.fill(' ', width);
            width = 0;
        }
        if (signChar != 0) buf.append(signChar);

        if (len < precision) {
            if (!negative || f.width != 0 || (f.zeroPad && !f.rightPad)) {
                buf.fill('0', precision - len);
            }
        }
        buf.append(bytes, first, numlen);

        if (width > 0) buf.fill(' ',width);
        if (len < precision && negative && f.rightPad) {
            buf.fill(' ', precision - len);
        }
    }

    static class Token {
    }

    static class ByteToken extends Token {
        private ByteList bytes;

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
    static class FormatToken extends Token {
        public int base = 10;          // which base of number are we formatting
        public boolean spacePad;       // '% d' (FLAG_SPACE in MRI)
        public boolean plusPrefix;     // '%+d' (FLAG_PLUS in MRI)
        public boolean rightPad;       // (FLAG_MINUS in MRI)
        public boolean hexZero;
        public boolean zeroPad;
        public int format;
        public boolean hasWidth;       // width part expressed as syntax '%3d', '%*3d', '%3.1d' ...
        public boolean hasPrecision;   // precision part expressed as syntax '%.3d', '%3.1d', '%3.*1$d' ...
        public boolean precisionIndex; // Is the precision value referring to an index instead of a value?
        public int index = -1;         // positional index to use in this format
        public int width;              // numeric value if explicitly stated (index or explicit value)
        public int precision;          // numeric value if explicitly stated (index or explicit value)
        public int value;              // numeric value if explicitly stated (index only)
        public ByteList name;
        public int exponent;
        public boolean unsigned;       // 'u' will process argument value differently sometimes (otherwise like 'd').
        public byte[] prefix;          // put on front of value unless !0 or explicitly requested (useZeroForPrefix).
        public boolean angled;         // if name if is curly ('{') or angled ('<')?

        public boolean indexedArg() {
            return index >= 0;
        }

        public boolean isIndexed() {
            return indexedArg() || precisionIndex;
        }

        public String toString() {
            return "Format[%" + (char) format +
                    (name != null ?  ",name=" + (name.realSize() != 0 ? name : "''") : "") +
                    //(hasWidth ? ",width.prec=" + width + (widthIndex ? "$" : "") : "") +
                    (hasPrecision ? "." + precision + (precisionIndex ? "$" : "") : "") +
                    (isIndexed() ? ",index=" + value : "") +
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
        private static int EOF = -1;
        public static Token EOFToken = new Token();
        private ByteList format;
        private int index = 0;
        private int current = 0;
        private ThreadContext context;
        private boolean unnumberedSeen;

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

        private int processDigits() {
            int count =  current - '0';

            for (int character = nextChar(); character != EOF; character = nextChar()) {
                if (!isDecimalDigit(character)) break;

                count *= 10 + (character - '0');
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

            FormatToken token = new FormatToken();
            processModifiers(token);

            // Found %{name} or %<name>.  %{} has no formatting so just return now.
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
                    token.prefix = PREFIX_OCTAL;
                    break;
                case 'x': // Convert argument as a hexadecimal number.
                    token.base = 16;
                    token.format = current;
                    token.prefix = PREFIX_HEX_LC;
                    break;
                case 'X': // Equivalent to `x', but uses uppercase letters.
                    token.base = 16;
                    token.format = current;
                    token.prefix = PREFIX_HEX_UC;
                    break;
                case 'b': // binary number
                    token.base = 2;
                    token.format = current;
                    token.prefix = PREFIX_BINARY_LC;
                    break;
                case 'B': // binary number with 0B prefix
                    token.base = 2;
                    token.format = current;
                    token.prefix = PREFIX_BINARY_UC;
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

        // FIXME: multiple names should error
        private void processModifiers(FormatToken token) {
            boolean inPrecision = false;  // Are we processing precision part of the format modifier (e.g. after the '.').

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
                        token.zeroPad = true;
                        break;
                    case '*':    // use arg list to calculate pad
                        if (!token.hasPrecision) { // initial '*'.
                            if (token.hasWidth) error("width given twice");

                            token.hasWidth = true; // hasWidth with no widthIndex == bare '*'. Otherwise specific index.
                        } // Otherwise: '*' for '{width}.*'  [nothing to do here as it will got to 0-9 next.
                        break;
                    case '1': case '2': case '3': case '4': case '5':  // numbers representing specific field values '%3d'
                    case '6': case '7': case '8': case '9': {          // OR indices '%*1$2$d'
                        int amount = processDigits();

                        if (inPrecision) {
                            if (token.precisionIndex) {          // index of value for format '%3.*1$2$d'
                                if (current != '$') error("width given twice");
                                token.value = amount;
                            } else {                          // index of precision '%3.*1$'.
                                if (current == '$') token.precisionIndex = true;
                                token.precision = amount;
                            }
                        } else {
                            if (token.isIndexed()) {          // index of value for format '%*2$3$d'
                                if (current != '$') error("width given twice");
                                token.value = amount;
                            } else {
                                if (current == '$') {
                                    token.index = amount;
                                } else {
                                    token.width = amount;
                                }
                            }
                        }

                        if (current != '$') unread();

                        break;
                    }
                    case '.':
                        if (token.hasPrecision) error("precision given twice");

                        token.hasPrecision = true;
                        inPrecision = true;
                        break;
                    case '<':
                        if (token.name != null) throw new IllegalArgumentException("FIXME: make str() for mbs supported name");
                        token.angled = true;
                        token.name = bytelistUpto('>');
                        break;
                    case '{':
                        if (token.name != null) throw new IllegalArgumentException("FIXME: make str() for mbs supported name");
                        token.name = bytelistUpto('}');
                        return;
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



