/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.parser;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.nodes.PackRootNode;
import org.jruby.truffle.pack.nodes.SourceNode;
import org.jruby.truffle.pack.nodes.control.BackNode;
import org.jruby.truffle.pack.nodes.control.NNode;
import org.jruby.truffle.pack.nodes.control.SequenceNode;
import org.jruby.truffle.pack.nodes.control.StarNode;
import org.jruby.truffle.pack.nodes.type.*;
import org.jruby.truffle.pack.runtime.Endianness;
import org.jruby.truffle.pack.runtime.Signedness;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.Pack;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class PackParser {

    private final RubyContext context;

    public PackParser(RubyContext context) {
        this.context = context;
    }

    public CallTarget parse(String format, boolean extended) {
        if (format.length() > 32) {
            format = recoverLoop(format);
            extended = true;
        }

        final PackTokenizer tokenizer = new PackTokenizer(format, extended);
        final PackNode body = parse(tokenizer, false);
        return Truffle.getRuntime().createCallTarget(new PackRootNode(describe(format), body));
    }

    public PackNode parse(PackTokenizer tokenizer, boolean inParens) {
        final List<PackNode> sequenceChildren = new ArrayList<>();

        tokenizerLoop: while (true) {
            Object token = tokenizer.next();

            if (token == null) {
                break;
            }

            PackNode node;

            if (token instanceof Character) {
                switch ((char) token) {
                    case '(':
                        node = parse(tokenizer, true);
                        break;
                    case ')':
                        if (inParens) {
                            break tokenizerLoop;
                        } else {
                            throw new UnsupportedOperationException("unbalanced parens");
                        }
                    case 'C':
                        node = writeInteger(8, Signedness.UNSIGNED, nativeEndianness());
                        break;
                    case 'c':
                        node = writeInteger(8, Signedness.SIGNED, nativeEndianness());
                        break;
                    case 'S':
                    case 'L':
                    case 'I':
                    case 'Q':
                    case 's':
                    case 'l':
                    case 'i':
                    case 'q': {
                        int size = 0;
                        Signedness signedness = null;
                        Endianness endianness = nativeEndianness();

                        switch ((char) token) {
                            case 'S':
                                size = 16;
                                signedness = Signedness.UNSIGNED;
                                break;
                            case 'L':
                            case 'I':
                                size = 32;
                                signedness = Signedness.UNSIGNED;
                                break;
                            case 'Q':
                                size = 64;
                                signedness = Signedness.UNSIGNED;
                                break;
                            case 's':
                                size = 16;
                                signedness = Signedness.SIGNED;
                                break;
                            case 'l':
                            case 'i':
                                size = 32;
                                signedness = Signedness.SIGNED;
                                break;
                            case 'q':
                                size = 64;
                                signedness = Signedness.SIGNED;
                                break;
                        }

                        if (tokenizer.peek('_') || tokenizer.peek('!')) {
                            size = 64;
                            tokenizer.next();
                        }

                        if (tokenizer.peek('<')) {
                            endianness = Endianness.LITTLE;
                            tokenizer.next();
                        } else if (tokenizer.peek('>')) {
                            endianness = Endianness.BIG;
                            tokenizer.next();
                        }

                        node = writeInteger(size, signedness, endianness);
                    } break;
                    case 'n':
                        node = writeInteger(16, Signedness.UNSIGNED, Endianness.BIG);
                        break;
                    case 'N':
                        node = writeInteger(32, Signedness.UNSIGNED, Endianness.BIG);
                        break;
                    case 'v':
                        node = writeInteger(16, Signedness.UNSIGNED, Endianness.LITTLE);
                        break;
                    case 'V':
                        node = writeInteger(32, Signedness.UNSIGNED, Endianness.LITTLE);
                        break;
                    case 'a':
                        final boolean pad;
                        final int width;
                        if (tokenizer.peek() instanceof Integer) {
                            pad = true;
                            width = (int) tokenizer.next();
                        } else {
                            pad = false;
                            width = 0;
                        }
                        node = WriteBinaryStringNodeGen.create(pad, width, (byte) 0, ReadStringNodeGen.create(context, new SourceNode()));
                        break;
                    case 'X':
                        node = new BackNode();
                        break;
                    case 'x':
                        node = new NullNode();
                        break;
                    default:
                        throw new UnsupportedOperationException(String.format("unexpected token %s", token));
                }
            } else {
                throw new UnsupportedOperationException(String.format("unexpected token %s", token));
            }

            if (tokenizer.peek('*')) {
                tokenizer.next();
                node = new StarNode(node);
            }

            if (tokenizer.peek() instanceof Integer) {
                node = new NNode((int) tokenizer.next(), node);
            }

            sequenceChildren.add(node);
        }

        return new SequenceNode(sequenceChildren.toArray(new PackNode[sequenceChildren.size()]));
    }

    /**
     * Format strings can sometimes be dynamically generated with code such as:
     * <p>
     *   <code>'x' + ('NX' * size)</code>
     * <p>
     * This is problematic for us as it expands to:
     * <p>
     *   <code>xNXNXNXNXNXNXNXNXNXNX...</code>
     * <p>
     * We will struggle to compile that with a large value for size because it
     * will generate a huge number of nodes. Even if we could compile it, it's
     * not great for memory consumption or the instruction cache. Instead, we'd
     * like to recover the loop in there and convert it to:
     * <p>
     *   <code>x(NX)1000</code>
     * <p>
     * We could try and do something really sophisticated here, with nested
     * loops and finding the optimal pattern, but for the moment we just look
     * for one simple loop.
     * <p>
     * To do that, for each character we look 1..n characters behind and see if
     * that pattern is repeated. If it is we have the loop. Nothing more
     * complicated than that.
     */
    private String recoverLoop(String format) {
        int break_point = 0;

        while (break_point < format.length()) {
            if ("0123456789*".indexOf(format.charAt(break_point)) != -1) {
                break_point++;
                continue;
            }

            int repeated_length = 1;
            int max_repeated_length = -1;

            while (repeated_length <= break_point && break_point + repeated_length <= format.length()) {
                if (format.substring(break_point - repeated_length, break_point).equals(format.substring(break_point, break_point + repeated_length))) {
                    max_repeated_length = repeated_length;
                }

                repeated_length++;
            }

            if (max_repeated_length == -1) {
                break_point++;
            } else {
                final String repeated = format.substring(break_point, break_point + max_repeated_length);

                int count = 2;
                int rep_point = break_point + max_repeated_length;

                while (rep_point + max_repeated_length <= format.length()) {
                    if (!format.substring(rep_point, rep_point + max_repeated_length).equals(repeated)) {
                        break;
                    }

                    count++;
                    rep_point += max_repeated_length;
                }

                final StringBuilder builder = new StringBuilder();
                builder.append(format.substring(0, break_point - max_repeated_length));
                builder.append('(');
                builder.append(repeated);
                builder.append(')');
                builder.append(count);
                builder.append(format.substring(rep_point));
                format = builder.toString();
            }

        }

        return format;
    }

    private String describe(String format) {
        format = format.replace("\\s+", "");

        if (format.length() > 10) {
            format = format.substring(0, 10) + "…";
        }

        return format;
    }

    private static Endianness nativeEndianness() {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            return Endianness.BIG;
        } else if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            return Endianness.LITTLE;
        } else {
            throw new UnsupportedOperationException(String.format("unknown byte order %s", ByteOrder.nativeOrder()));
        }
    }

    private PackNode writeInteger(int size, Signedness signedness, Endianness endianness) {
        final PackNode readNode = ReadIntegerNodeGen.create(context, new SourceNode());

        switch (size) {
            case 16:
                switch (signedness) {
                    case UNSIGNED:
                        switch (endianness) {
                            case LITTLE:
                                return Write16UnsignedLittleNodeGen.create(readNode);
                            case BIG:
                                return Write16UnsignedBigNodeGen.create(readNode);
                        }
                    default:
                        throw new UnsupportedOperationException();
                }
            case 32:
                switch (signedness) {
                    case UNSIGNED:
                        switch (endianness) {
                            case LITTLE:
                                return Write32UnsignedLittleNodeGen.create(readNode);
                            case BIG:
                                return Write32UnsignedBigNodeGen.create(readNode);
                        }
                    case SIGNED:
                        switch (endianness) {
                            case LITTLE:
                                // Can I just use the same node?
                                return Write32UnsignedLittleNodeGen.create(readNode);
                            case BIG:
                                // Can I just use the same node?
                                return Write32UnsignedBigNodeGen.create(readNode);
                        }
                    default:
                        throw new UnsupportedOperationException();
                }
            case 64:
                switch (signedness) {
                    case UNSIGNED:
                        switch (endianness) {
                            case LITTLE:
                                return Write64UnsignedLittleNodeGen.create(readNode);
                            case BIG:
                                return Write64UnsignedBigNodeGen.create(readNode);
                        }
                    case SIGNED:
                        switch (endianness) {
                            case LITTLE:
                                // Can I just use the same node?
                                return Write64UnsignedLittleNodeGen.create(readNode);
                            case BIG:
                                // Can I just use the same node?
                                return Write64UnsignedBigNodeGen.create(readNode);
                        }
                    default:
                        throw new UnsupportedOperationException();
                }
            default:
                throw new UnsupportedOperationException();
        }
    }

}
