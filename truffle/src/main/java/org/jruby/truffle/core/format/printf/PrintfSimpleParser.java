package org.jruby.truffle.core.format.printf;

import org.jruby.truffle.core.format.exceptions.InvalidFormatException;

import java.util.ArrayList;
import java.util.List;

public class PrintfSimpleParser {

    private final char[] source;

    public PrintfSimpleParser(char[] source) {
        this.source = source;
    }

    public List<SprintfConfig> parse() {
        List<SprintfConfig> configs = new ArrayList<>();
        ArgType argType = ArgType.NONE;

        final int end = source.length;

        for (int i = 0; i < end; ) {

            // Add literal bytes up to the first %
            int literalEnd = i;
            for (; literalEnd < end && source[literalEnd] != '%'; literalEnd++) {
            }
            final int literalLength = literalEnd - i;
            if (literalLength > 0) {
                SprintfConfig config = new SprintfConfig();
                config.setLiteral(true);
                final char[] literalBytes = new char[literalLength];
                System.arraycopy(source, i, literalBytes, 0, literalLength);
                config.setLiteralBytes(charsToBytes(literalBytes));
                configs.add(config);
            }
            if (literalEnd >= end) {
                break; // format string ends with a literal
            }

            i = literalEnd + 1; // skip first %

            SprintfConfig config = new SprintfConfig();
            configs.add(config);

            boolean finished = false;
            boolean argTypeSet = false;

            while (!finished) {
                char p = i >= this.source.length ? '\0' : this.source[i];

                switch (p) {
                    case ' ':
                        config.checkForFlags();
                        config.setHasSpace(true);
                        i++;
                        break;
                    case '#':
                        config.checkForFlags();
                        config.setFsharp(true);
                        i++;
                        break;
                    case '+':
                        config.checkForFlags();
                        config.setPlus(true);
                        i++;
                        break;
                    case '-':
                        config.checkForFlags();
                        config.setMinus(true);
                        i++;
                        break;
                    case '0':
                        config.checkForFlags();
                        config.setZero(true);
                        i++;
                        break;
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        LookAheadResult r = getNum(i, end);
                        i = r.getNextI();
                        if (valueAt(i) != null && valueAt(i) == '$') {
                            if (config.getAbsoluteArgumentIndex() != null) {
                                throw new InvalidFormatException("value given twice - " + r.getNumber() + "$");
                            }
                            checkPosArg(argType, r.getNumber());
                            argType = ArgType.NUMBERED;
                            argTypeSet = true;
                            config.setAbsoluteArgumentIndex(r.getNumber());
                            i++;
                            break;
                        }

                        config.checkForWidth();
                        config.setWidth(r.getNumber());
                        break;
                    case '<':
                    case '{':
                        char term = (p == '<') ? '>' : '}';

                        int j = i;
                        for (; j < end && this.source[j] != term; ) {
                            j++;
                        }
                        if (j >= end) {
                            throw new InvalidFormatException("malformed name - unmatched parenthesis");
                        }
                        final int nameLength = j - (i + 1);
                        final char[] nameBytes = new char[nameLength];
                        System.arraycopy(this.source, (i + 1), nameBytes, 0, nameLength);
                        config.setNamesBytes(charsToBytes(nameBytes));
                        i = j + 1;
                        checkNameArg(argType, nameBytes);
                        argType = ArgType.NAMED;
                        argTypeSet = true;
                        if (term == '}') {
                            config.setFormatType(SprintfConfig.FormatType.OTHER);
                            config.setFormat('s');
                            finished = true;
                        }
                        break;
                    case '*':
                        config.checkForWidth();

                        LookAheadResult numberDollarWidth = getNumberDollar(i + 1, end);
                        if (numberDollarWidth.getNumber() != null) {
                            config.setArgWidth(true);
                            config.setWidth(numberDollarWidth.getNumber());
                            checkPosArg(argType, numberDollarWidth.getNumber());
                            argType = ArgType.NUMBERED;
                            i = numberDollarWidth.getNextI();
                        } else {
                            checkNextArg(argType, 1); // TODO index next args
                            argType = ArgType.UNNUMBERED;
                            config.setWidthStar(true);
                            i++;
                        }
                        break;
                    case '.':
                        if (config.hasPrecision()) {
                            throw new InvalidFormatException("precision given twice");
                        }
                        config.setPrecisionVisited(true);
                        if (valueAt(i + 1) != null && valueAt(i + 1) == '*') {
                            LookAheadResult numberDollar = getNumberDollar(i + 2, end);
                            if (numberDollar.getNumber() != null) {
                                config.setPrecision(numberDollar.getNumber());
                                config.setPrecisionArg(true);
                                checkPosArg(argType, numberDollar.getNumber());
                                argType = ArgType.NUMBERED;
                                i = numberDollar.getNextI();
                            } else {
                                checkNextArg(argType, 1); // TODO idx
                                argType = ArgType.UNNUMBERED;
                                config.setPrecisionStar(true);
                                i += 2;
                            }
                            break;
                        }

                        LookAheadResult re = getNum(i + 1, end);
                        config.setPrecision(re.getNumber());
                        i = re.getNextI();
                        break;
                    case '\n':
                    case '\0':
                        i--;
                    case '%':
                        if (config.hasFlags()) {
                            throw new InvalidFormatException("invalid format character - %");
                        }
                        config.setLiteral(true);
                        byte[] literal = {(byte) '%'};
                        config.setLiteralBytes(literal);
                        i++;
                        finished = true;
                        break;
                    case 'c':
                        config.setFormatType(SprintfConfig.FormatType.OTHER);
                        config.setFormat(p);
                        i++;
                        if (!argTypeSet) {
                            checkNextArg(argType, 1);
                            argType = ArgType.UNNUMBERED;
                        }
                        finished = true;
                        break;
                    case 's':
                    case 'p':
                        config.setFormatType(SprintfConfig.FormatType.OTHER);
                        config.setFormat(p);
                        i++;
                        if (!argTypeSet) { // Speculative
                            checkNextArg(argType, 1);
                            argType = ArgType.UNNUMBERED;
                        }
                        finished = true;
                        break;
                    case 'd':
                    case 'i':
                    case 'o':
                    case 'x':
                    case 'X':
                    case 'b':
                    case 'B':
                    case 'u':
                        if (!argTypeSet) {
                            checkNextArg(argType, 1); // TODO idx correctly
                            argType = ArgType.UNNUMBERED;
                        }
                        config.setFormatType(SprintfConfig.FormatType.INTEGER);
                        config.setFormat(p);
                        finished = true;
                        i++;
                        break;
                    case 'g':
                    case 'G':
                    case 'e':
                    case 'E':
                    case 'a':
                    case 'A':
                    case 'f':
                        if (!argTypeSet) {
                            checkNextArg(argType, 1);
                            argType = ArgType.UNNUMBERED;
                        }
                        config.setFormatType(SprintfConfig.FormatType.FLOAT);
                        config.setFormat(p);
                        finished = true;
                        i++;
                        break;
                    default:
                        throw new InvalidFormatException("malformed format string - %" + p);
                }
            }
        }
        return configs;
    }


    private static void checkNextArg(ArgType argType, int nextArgumentIndex) {
        switch (argType) {
            case NUMBERED:
                throw new InvalidFormatException("unnumbered(" + nextArgumentIndex + ") mixed with numbered");
            case NAMED:
                throw new InvalidFormatException("unnumbered(" + nextArgumentIndex + ") mixed with named");
        }
    }

    private static void checkPosArg(ArgType posarg, int nextArgumentIndex) {
        if (posarg == ArgType.UNNUMBERED) {
            throw new InvalidFormatException("numbered(" + nextArgumentIndex + ") after unnumbered(" + posarg + ")");
        }
        if (posarg == ArgType.NAMED) {
            throw new InvalidFormatException("numbered(" + nextArgumentIndex + ") after named");
        }
        if (nextArgumentIndex < 1) {
            throw new InvalidFormatException("invalid index - " + nextArgumentIndex + "$");
        }
    }

    private static void checkNameArg(ArgType argType, char[] name) {
        if (argType == ArgType.UNNUMBERED) {
            throw new InvalidFormatException("named" + new String(name) + " after unnumbered(%d)");
        }
        if (argType == ArgType.NUMBERED) {
            throw new InvalidFormatException("named" + new String(name) + " after numbered");
        }
    }

    private enum ArgType {
        NONE,
        NUMBERED,
        UNNUMBERED,
        NAMED
    }

    private void checkPosArg(int relativeArgumentIndex, int absoluteArgumentIndex) {
        if (relativeArgumentIndex > 0) {
            throw new InvalidFormatException("numbered(" + absoluteArgumentIndex + ") after unnumbered(" + relativeArgumentIndex + ")");
        }
        if (relativeArgumentIndex == -2) {
            throw new InvalidFormatException("numbered(" + absoluteArgumentIndex + ") after named");
        }
        if (absoluteArgumentIndex < 1) {
            throw new InvalidFormatException("invalid index - " + absoluteArgumentIndex + "$");
        }
    }

    public LookAheadResult getNum(int startI, int end) {
        StringBuilder sb = new StringBuilder();

        int moreChars = 0;
        for (int i = startI; i < end; i++) {
            char nextChar = source[i];
            if (!isDigit(nextChar)) {
                break;
            } else {
                sb.append(nextChar);
                moreChars += 1;
            }
        }

        final int nextI = startI + moreChars;

        if (nextI >= end) {
            throw new InvalidFormatException("malformed format string - %%*[0-9]");
        }

        Integer result;
        if (sb.length() > 0) {
            result = Integer.parseInt(sb.toString());
        } else {
            result = null;
        }
        return new LookAheadResult(result, nextI);
    }

    public LookAheadResult getNumberDollar(int startI, int end) {
        LookAheadResult lar = getNum(startI, end);
        Integer result = null;
        int newI = startI;
        if (lar.getNumber() != null) {
            final int nextI = lar.getNextI();
            if (valueAt(nextI) != null && valueAt(nextI) == '$') {
                result = lar.getNumber();
                newI = nextI + 1;
                if (result < 1) {
                    throw new InvalidFormatException("invalid index - " + result + "$");
                }
            }
        }
        return new LookAheadResult(result, newI);
    }

    public static class LookAheadResult {
        private Integer number;
        private int nextI;

        public LookAheadResult(Integer number, int nextI) {
            this.number = number;
            this.nextI = nextI;
        }

        public Integer getNumber() {
            return number;
        }

        public int getNextI() {
            return nextI;
        }
    }

    public static boolean isDigit(char c) {
        return c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9';
    }

    public static class SprintfConfig {

        public enum FormatType {
            INTEGER, FLOAT, OTHER
        }

        private boolean literal = false;
        private byte[] literalBytes;

        private byte[] namesBytes;
        private boolean argWidth = false;

        private Integer absoluteArgumentIndex;
        private Integer precision;
        private boolean precisionArg = false;
        private boolean precisionVisited = false;
        private Integer width;
        private boolean hasSpace = false;
        private boolean fsharp = false; // #
        private boolean plus = false;
        private boolean minus = false;
        private boolean zero = false;
        private boolean widthStar = false;
        private boolean precisionStar = false;
        private char format;
        private FormatType formatType;


        public void checkForFlags() {
            if (hasWidth()) {
                throw new InvalidFormatException("flag after width");
            }
            if (hasPrecision()) {
                throw new InvalidFormatException("flag after precision");
            }
        }

        public void checkForWidth() {
            if (hasWidth()) {
                throw new InvalidFormatException("width given twice");
            }
            if (hasPrecision()) {
                throw new InvalidFormatException("width after precision");
            }
        }

        public boolean isHasSpace() {
            return hasSpace;
        }

        public void setHasSpace(boolean hasSpace) {
            this.hasSpace = hasSpace;
        }

        public Integer getPrecision() {
            return precision;
        }

        public void setPrecision(int precision) {
            this.precision = precision;
        }

        public void setPrecision(Integer precision) {
            this.precision = precision;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public boolean isFsharp() {
            return fsharp;
        }

        public void setFsharp(boolean fsharp) {
            this.fsharp = fsharp;
        }

        public boolean isPlus() {
            return plus;
        }

        public void setPlus(boolean plus) {
            this.plus = plus;
        }

        public boolean isMinus() {
            return minus;
        }

        public void setMinus(boolean minus) {
            this.minus = minus;
        }

        public boolean isZero() {
            return zero;
        }

        public void setZero(boolean zero) {
            this.zero = zero;
        }

        public FormatType getFormatType() {
            return formatType;
        }

        public void setFormatType(FormatType formatType) {
            this.formatType = formatType;
        }

        public char getFormat() {
            return format;
        }

        public void setFormat(char format) {
            this.format = format;
        }

        public boolean isWidthStar() {
            return widthStar;
        }

        public void setWidthStar(boolean widthStar) {
            this.widthStar = widthStar;
        }

        public boolean isPrecisionStar() {
            return precisionStar;
        }

        public void setPrecisionStar(boolean precisionStar) {
            this.precisionStar = precisionStar;
        }

        public boolean hasPrecision() {
            return precision != null || precisionStar || precisionVisited;
        }

        public boolean hasWidth() {
            return width != null || widthStar;
        }

        public boolean isLiteral() {
            return literal;
        }

        public void setLiteral(boolean literal) {
            this.literal = literal;
        }

        public byte[] getLiteralBytes() {
            return literalBytes;
        }

        public void setLiteralBytes(byte[] literalBytes) {
            this.literalBytes = literalBytes;
        }

        public Integer getAbsoluteArgumentIndex() {
            return absoluteArgumentIndex;
        }

        public void setAbsoluteArgumentIndex(Integer absoluteArgumentIndex) {
            this.absoluteArgumentIndex = absoluteArgumentIndex;
        }

        public boolean isArgWidth() {
            return argWidth;
        }

        public void setArgWidth(boolean argWidth) {
            this.argWidth = argWidth;
        }


        public boolean isPrecisionVisited() {
            return precisionVisited;
        }

        public void setPrecisionVisited(boolean precisionVisited) {
            this.precisionVisited = precisionVisited;
        }

        public byte[] getNamesBytes() {
            return namesBytes;
        }

        public void setNamesBytes(byte[] namesBytes) {
            this.namesBytes = namesBytes;
        }

        public boolean isPrecisionArg() {
            return precisionArg;
        }

        public void setPrecisionArg(boolean precisionArg) {
            this.precisionArg = precisionArg;
        }

        public boolean hasFlags() {
            return literal || precision != null || precisionVisited || width != null || hasSpace || fsharp || plus || minus || zero || precisionStar || widthStar || formatType != null;
        }
    }

    public Character valueAt(int index) {
        assert index >= 0;
        if (index < this.source.length) {
            return this.source[index];
        } else {
            return null;
        }
    }

    private static byte[] charsToBytes(char[] chars) {
        final byte[] bytes = new byte[chars.length];

        for (int n = 0; n < chars.length; n++) {
            bytes[n] = (byte) chars[n];
        }

        return bytes;
    }

}
