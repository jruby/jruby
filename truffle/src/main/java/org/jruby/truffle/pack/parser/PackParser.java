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
import org.jruby.truffle.pack.nodes.control.*;
import org.jruby.truffle.pack.nodes.read.ReadDoubleNodeGen;
import org.jruby.truffle.pack.nodes.read.ReadLongNodeGen;
import org.jruby.truffle.pack.nodes.read.ReadLongOrBigIntegerNodeGen;
import org.jruby.truffle.pack.nodes.read.ReadStringNodeGen;
import org.jruby.truffle.pack.nodes.type.AsLongNodeGen;
import org.jruby.truffle.pack.nodes.type.AsSinglePrecisionNodeGen;
import org.jruby.truffle.pack.nodes.write.*;
import org.jruby.truffle.pack.runtime.Endianness;
import org.jruby.truffle.pack.runtime.PackEncoding;
import org.jruby.truffle.pack.runtime.Signedness;
import org.jruby.truffle.pack.runtime.exceptions.FormatException;
import org.jruby.truffle.runtime.RubyContext;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a pack format expression into a tree of Truffle nodes.
 */
public class PackParser {

    private final RubyContext context;

    private PackEncoding encoding = PackEncoding.DEFAULT;

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
        return Truffle.getRuntime().createCallTarget(new PackRootNode(describe(format), encoding, body));
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
                            default:
                                break;
                        }

                        if (tokenizer.peek('_') || tokenizer.peek('!')) {
                            switch ((char) token) {
                                case 'S':
                                case 's':
                                    size = 16;
                                    break;
                                case 'I':
                                case 'i':
                                    size = 32;
                                    break;
                                case 'L':
                                case 'l':
                                    size = 64;
                                    break;
                                default:
                                    throw new UnsupportedOperationException();
                            }

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
                    case 'A':
                    case 'Z':
                    case 'a': {
                        encoding = encoding.unifyWith(PackEncoding.ASCII_8BIT);

                        boolean padOnNull = true;
                        final byte padding;

                        switch ((char) token) {
                            case 'A':
                                padding = ' ';
                                break;
                            case 'Z':
                            case 'a':
                                padding = 0;
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }

                        final boolean pad;
                        final int width;

                        if (tokenizer.peek() instanceof Integer) {
                            pad = true;
                            width = (int) tokenizer.next();
                        } else {
                            pad = false;
                            width = 1;
                        }

                        final boolean takeAll;
                        final boolean appendNull;

                        if (tokenizer.peek('*')) {
                            tokenizer.next();

                            takeAll = true;
                            appendNull = (char) token == 'Z';

                            switch ((char) token) {
                                case 'A':
                                case 'a':
                                    padOnNull = false;
                                    break;
                                default:
                                    break;
                            }
                        } else {
                            takeAll = false;
                            appendNull = false;
                        }

                        node = WriteBinaryStringNodeGen.create(context, pad, padOnNull,
                                width, padding, takeAll, appendNull,
                                ReadStringNodeGen.create(context, true, "to_str",
                                        false, context.getCoreLibrary().getNilObject(), new SourceNode()));
                    } break;
                    case 'H':
                    case 'h': {
                        final Endianness endianness;

                        switch ((char) token) {
                            case 'H':
                                endianness = Endianness.BIG;
                                break;
                            case 'h':
                                endianness = Endianness.LITTLE;
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }

                        final int length;

                        if (tokenizer.peek('*')) {
                            length = -1;
                        } else if (tokenizer.peek() instanceof Integer) {
                            length = (int) tokenizer.next();
                        } else {
                            length = 1;
                        }

                        node = WriteHexStringNodeGen.create(context, endianness, length,
                                ReadStringNodeGen.create(context, true, "to_str",
                                        false, context.getCoreLibrary().getNilObject(), new SourceNode()));
                    } break;
                    case 'B':
                    case 'b': {
                        final Endianness endianness;

                        switch ((char) token) {
                            case 'B':
                                endianness = Endianness.BIG;
                                break;
                            case 'b':
                                endianness = Endianness.LITTLE;
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }

                        final boolean star;
                        final int length;

                        if (tokenizer.peek() instanceof Integer) {
                            star = false;
                            length = (int) tokenizer.next();
                        } else if (tokenizer.peek('*')) {
                            star = true;
                            length = 0;
                            tokenizer.next();
                        } else {
                            star = false;
                            length = 1;
                        }

                        node = WriteBitStringNodeGen.create(context, endianness, star, length,
                                ReadStringNodeGen.create(context, true, "to_str",
                                        false, context.getCoreLibrary().getNilObject(), new SourceNode()));
                    } break;
                    case 'M': {
                        encoding = encoding.unifyWith(PackEncoding.US_ASCII);

                        int length;

                        if (tokenizer.peek() instanceof Integer) {
                            length = (int) tokenizer.next();

                            if (length <= 1) {
                                length = 72;
                            }
                        } else {
                            length = 72;
                        }

                        node = WriteMIMEStringNodeGen.create(context, length,
                                ReadStringNodeGen.create(context, true, "to_s",
                                        true, context.getCoreLibrary().getNilObject(), new SourceNode()));
                    } break;
                    case 'm':
                    case 'u': {
                        encoding = encoding.unifyWith(PackEncoding.US_ASCII);

                        int length = 1;
                        boolean ignoreStar = false;

                        if (tokenizer.peek() instanceof Integer) {
                            length = (int) tokenizer.next();
                        } else if (tokenizer.peek('*')) {
                            tokenizer.next();
                            length = 0;
                            ignoreStar = true;
                        }

                        switch ((char) token) {
                            case 'm':
                                node = WriteBase64StringNodeGen.create(context, length, ignoreStar,
                                        ReadStringNodeGen.create(context, false, "to_str",
                                                false, context.getCoreLibrary().getNilObject(), new SourceNode()));
                                break;
                            case 'u':
                                node = WriteUUStringNodeGen.create(context, length, ignoreStar,
                                        ReadStringNodeGen.create(context, false, "to_str",
                                                false, context.getCoreLibrary().getNilObject(), new SourceNode()));
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }
                    } break;
                    case 'U':
                        encoding = encoding.unifyWith(PackEncoding.UTF_8);
                        node = WriteUTF8CharacterNodeGen.create(context, ReadLongNodeGen.create(context, new SourceNode()));
                        break;
                    case 'X':
                        node = new BackNode(context);
                        break;
                    case 'x':
                        node = new WriteByteNode(context, (byte) 0);
                        break;
                    case '@': {
                        final int position;
                        if (tokenizer.peek() instanceof Integer) {
                            position = (int) tokenizer.next();
                        } else {
                            position = 1;
                        }
                        node = new AtNode(context, position);
                    } break;
                    case 'D':
                    case 'd':
                        node = writeInteger(64, Signedness.UNSIGNED, nativeEndianness(),
                                AsLongNodeGen.create(context,
                                        ReadDoubleNodeGen.create(context, new SourceNode())));
                        break;
                    case 'F':
                    case 'f':
                        node = writeInteger(32, Signedness.UNSIGNED, nativeEndianness(),
                                AsLongNodeGen.create(context,
                                        AsSinglePrecisionNodeGen.create(context,
                                            ReadDoubleNodeGen.create(context, new SourceNode()))));
                        break;
                    case 'E':
                        node = writeInteger(64, Signedness.UNSIGNED, Endianness.LITTLE,
                                AsLongNodeGen.create(context,
                                        ReadDoubleNodeGen.create(context, new SourceNode())));
                        break;
                    case 'e':
                        node = writeInteger(32, Signedness.UNSIGNED, Endianness.LITTLE,
                                AsLongNodeGen.create(context,
                                        AsSinglePrecisionNodeGen.create(context,
                                                ReadDoubleNodeGen.create(context, new SourceNode()))));
                        break;
                    case 'G':
                        node = writeInteger(64, Signedness.UNSIGNED, Endianness.BIG,
                                AsLongNodeGen.create(context,
                                        ReadDoubleNodeGen.create(context, new SourceNode())));
                        break;
                    case 'g':
                        node = writeInteger(32, Signedness.UNSIGNED, Endianness.BIG,
                                AsLongNodeGen.create(context,
                                        AsSinglePrecisionNodeGen.create(context,
                                                ReadDoubleNodeGen.create(context, new SourceNode()))));
                        break;
                    case 'P':
                    case 'p':
                        node = writeInteger(64, Signedness.UNSIGNED, nativeEndianness(),
                                new PNode(context));
                        break;
                    case 'w':
                        node = WriteBERNodeGen.create(context,ReadLongOrBigIntegerNodeGen.create(context, new SourceNode()));
                        break;
                    default:
                        throw new UnsupportedOperationException(String.format("unexpected token %s", token));
                }

                if (tokenizer.peek('_') || tokenizer.peek('!')) {
                    throw new FormatException("'" + tokenizer.next() + "' allowed only after types sSiIlLqQ");
                }
            } else {
                throw new UnsupportedOperationException(String.format("unexpected token %s", token));
            }

            if (tokenizer.peek('*')) {
                tokenizer.next();

                if (node instanceof BackNode) {
                    continue;
                }

                node = new StarNode(context, node);
            }

            if (tokenizer.peek() instanceof Integer) {
                node = new NNode(context, (int) tokenizer.next(), node);
            }

            sequenceChildren.add(node);
        }

        return new SequenceNode(context, sequenceChildren.toArray(new PackNode[sequenceChildren.size()]));
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
                if (format.substring(break_point - repeated_length, break_point)
                        .equals(format.substring(break_point, break_point + repeated_length))) {
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

    /**
     * Provide a simple string describing the format expression that is short
     * enough to be used in Truffle and Graal diagnostics.
     */
    public static String describe(String format) {
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
        final PackNode readNode = ReadLongNodeGen.create(context, new SourceNode());
        return writeInteger(size, signedness, endianness, readNode);
    }

    private PackNode writeInteger(int size, Signedness signedness, Endianness endianness, PackNode readNode) {
        switch (size) {
            case 8:
                return Write8NodeGen.create(context, readNode);
            case 16:
                switch (signedness) {
                    case UNSIGNED:
                        switch (endianness) {
                            case LITTLE:
                                return Write16LittleNodeGen.create(context, readNode);
                            case BIG:
                                return Write16BigNodeGen.create(context, readNode);
                        }
                    case SIGNED:
                        switch (endianness) {
                            case LITTLE:
                                // Can I just use the same node?
                                return Write16LittleNodeGen.create(context, readNode);
                            case BIG:
                                // Can I just use the same node?
                                return Write16BigNodeGen.create(context, readNode);
                        }
                    default:
                        throw new UnsupportedOperationException();
                }
            case 32:
                switch (signedness) {
                    case UNSIGNED:
                        switch (endianness) {
                            case LITTLE:
                                return Write32LittleNodeGen.create(context, readNode);
                            case BIG:
                                return Write32BigNodeGen.create(context, readNode);
                        }
                    case SIGNED:
                        switch (endianness) {
                            case LITTLE:
                                // Can I just use the same node?
                                return Write32LittleNodeGen.create(context, readNode);
                            case BIG:
                                // Can I just use the same node?
                                return Write32BigNodeGen.create(context, readNode);
                        }
                    default:
                        throw new UnsupportedOperationException();
                }
            case 64:
                switch (signedness) {
                    case UNSIGNED:
                        switch (endianness) {
                            case LITTLE:
                                return Write64LittleNodeGen.create(context, readNode);
                            case BIG:
                                return Write64BigNodeGen.create(context, readNode);
                        }
                    case SIGNED:
                        switch (endianness) {
                            case LITTLE:
                                // Can I just use the same node?
                                return Write64LittleNodeGen.create(context, readNode);
                            case BIG:
                                // Can I just use the same node?
                                return Write64BigNodeGen.create(context, readNode);
                        }
                    default:
                        throw new UnsupportedOperationException();
                }
            default:
                throw new UnsupportedOperationException();
        }
    }

}
