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
import org.jruby.truffle.pack.nodes.control.*;
import org.jruby.truffle.pack.nodes.write.*;
import org.jruby.truffle.pack.runtime.PackEncoding;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a pack format expression into a tree of Truffle nodes.
 */
public class FormatParser {

    private enum State {
        READY,
        START
    }

    private final RubyContext context;

    private PackEncoding encoding = PackEncoding.DEFAULT;

    public FormatParser(RubyContext context) {
        this.context = context;
    }

    public CallTarget parse(ByteList format) {
        final FormatTokenizer tokenizer = new FormatTokenizer(format);
        final PackNode body = parse(tokenizer);
        return Truffle.getRuntime().createCallTarget(new PackRootNode(PackParser.describe(format.toString()), encoding, body));
    }

    public PackNode parse(FormatTokenizer tokenizer) {
        State state = State.READY;

        final List<PackNode> sequenceChildren = new ArrayList<>();

        while (true) {
            Object token = tokenizer.next();

            if (token == null) {
                break;
            }

            if (token instanceof ByteList) {
                switch (state) {
                    case READY:
                        sequenceChildren.add(new WriteBytesNode((ByteList) token));
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            } else if (token instanceof Character) {
                if ((char) token == '%') {
                    switch (state) {
                        case READY:
                            state = State.START;
                            break;
                        case START:
                            sequenceChildren.add(new WriteByteNode((byte) '%'));
                            state = State.READY;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                } else {
                    throw new UnsupportedOperationException();
                }
            } else {
                throw new UnsupportedOperationException();
            }
        }

        return new SequenceNode(sequenceChildren.toArray(new PackNode[sequenceChildren.size()]));
    }

}
