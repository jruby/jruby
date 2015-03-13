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
import org.jruby.ast.java_signature.CharacterLiteral;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.nodes.PackRootNode;
import org.jruby.truffle.pack.nodes.control.BackNode;
import org.jruby.truffle.pack.nodes.control.NNode;
import org.jruby.truffle.pack.nodes.control.SequenceNode;
import org.jruby.truffle.pack.nodes.control.StarNode;
import org.jruby.truffle.pack.nodes.type.NullNode;
import org.jruby.truffle.pack.nodes.type.UInt32BENode;
import org.jruby.truffle.pack.nodes.type.UInt32LENode;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class PackParser {

    public CallTarget parse(String format) {
        final PackTokenizer tokenizer = new PackTokenizer(format);
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
                    case 'N':
                        node = new UInt32BENode();
                        break;
                    case 'L':
                        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                            node = new UInt32BENode();
                        } else if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                            node = new UInt32LENode();
                        } else {
                            throw new UnsupportedOperationException(String.format("unknown byte order %s", ByteOrder.nativeOrder()));
                        }
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

    private String describe(String format) {
        format = format.replace("\\s+", "");

        if (format.length() > 10) {
            format = format.substring(0, 10) + "…";
        }

        return format;
    }

}
