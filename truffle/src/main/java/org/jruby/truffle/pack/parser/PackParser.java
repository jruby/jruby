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
                        return writeInteger(8, Signedness.UNSIGNED, nativeEndianness());
                    case 'S':
                        return writeInteger(16, Signedness.UNSIGNED, nativeEndianness());
                    case 'L':
                        return writeInteger(32, Signedness.UNSIGNED, nativeEndianness());
                    case 'Q':
                        return writeInteger(64, Signedness.UNSIGNED, nativeEndianness());
                    case 'c':
                        return writeInteger(8, Signedness.SIGNED, nativeEndianness());
                    case 's':
                        return writeInteger(16, Signedness.SIGNED, nativeEndianness());
                    case 'l':
                        return writeInteger(32, Signedness.SIGNED, nativeEndianness());
                    case 'q':
                        return writeInteger(64, Signedness.SIGNED, nativeEndianness());
                    case 'n':
                        return writeInteger(16, Signedness.UNSIGNED, Endianness.BIG);
                    case 'N':
                        return writeInteger(32, Signedness.UNSIGNED, Endianness.BIG);
                    case 'v':
                        return writeInteger(16, Signedness.UNSIGNED, Endianness.LITTLE);
                    case 'V':
                        return writeInteger(32, Signedness.UNSIGNED, Endianness.LITTLE);
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

            token = tokenizer.peek();

            if (token != null) {
                if (token instanceof Character) {
                    switch ((char) token) {
                        case '*':
                            tokenizer.next();
                            node = new StarNode(node);
                            break;
                    }
                } else if (token instanceof Integer) {
                    tokenizer.next();
                    node = new NNode((int) token, node);
                }
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
     * to recover the loop in there and convert it to:
     * <p>
     *   <code>x(NX)1000</code>
     * <p>
     * We could try and do something really sophisticated here, with nested
     * loops and finding the optimal pattern, but for the moment we just look
     * for...
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
                                return Write16UnsignedBigNodeGen.create(readNode);
                            case BIG:
                                return Write16UnsignedLittleNodeGen.create(readNode);
                        }
                    default:
                        throw new UnsupportedOperationException();
                }
            case 32:
                switch (signedness) {
                    case UNSIGNED:
                        switch (endianness) {
                            case LITTLE:
                                return Write32UnsignedBigNodeGen.create(readNode);
                            case BIG:
                                return Write32UnsignedLittleNodeGen.create(readNode);
                        }
                    default:
                        throw new UnsupportedOperationException();
                }
            default:
                throw new UnsupportedOperationException();
        }
    }

}
