/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.printf;

import org.jruby.truffle.core.format.exceptions.InvalidFormatException;
import org.jruby.truffle.language.RubyGuards;

import java.util.ArrayList;
import java.util.List;

public class PrintfSimpleParser {

    private final char[] source;
    private final Object[] arguments;
    private final boolean isDebug;

    public PrintfSimpleParser(char[] source, Object[] arguments, boolean isDebug) {
        this.source = source;
        this.arguments = arguments;
        this.isDebug = isDebug;
    }

    @SuppressWarnings("fallthrough")
    public List<SprintfConfig> parse() {
        List<SprintfConfig> configs = new ArrayList<>();
        ArgType argType = ArgType.NONE;

        final int end = source.length;
        int argCount = 0;

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
                        checkHash(arguments);
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
                            argCount += 1;
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
                                argCount += 1;
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
                            argCount += 1;
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
                            argCount += 1;
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
                            argCount += 1;
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
                            argCount += 1;
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
        if ((argType == ArgType.UNNUMBERED || argType == ArgType.NONE) &&
            arguments.length > argCount) {
            if (isDebug) {
                throw new InvalidFormatException("too many arguments for format string");
            }
        }

        return configs;
    }

    private static void checkHash(Object[] arguments) {
        if(arguments.length != 1  ||
            !RubyGuards.isRubyHash(arguments[0])) {
            throw new InvalidFormatException("one hash required");
        }
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
            try {
                result = Integer.parseInt(sb.toString());
            } catch (NumberFormatException nfe) {
                throw new InvalidFormatException("precision too big");
            }
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
