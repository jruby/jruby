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
        final List<PackNode> sequenceChildren = new ArrayList<>();

        int n = 0;

        charLoop: while (n < format.length()) {
            PackNode node;

            switch (format.charAt(n)) {
                case ' ':
                    continue charLoop;
                case 'N':
                    node = new UInt32BENode();
                    break;
                case 'L':
                    if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                        node = new UInt32BENode();
                    } else if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                        node = new UInt32LENode();
                    } else {
                        throw new UnsupportedOperationException();
                    }
                    break;
                case 'X':
                    node = new BackNode();
                    break;
                case 'x':
                    node = new NullNode();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            n++;

            if (n < format.length()) {
                if (format.charAt(n) == '*') {
                    n++;
                    node = new StarNode(node);
                } else if (Character.isDigit(format.charAt(n))) {
                    final int start = n;
                    n++;
                    while (n < format.length() && Character.isDigit(format.charAt(n))) {
                        n++;
                    }
                    node = new NNode(Integer.parseInt(format.substring(start, n)), node);
                }
            }

            sequenceChildren.add(node);
        }

        final PackNode sequence = new SequenceNode(sequenceChildren.toArray(new PackNode[sequenceChildren.size()]));
        return Truffle.getRuntime().createCallTarget(new PackRootNode(describe(format), sequence));
    }

    private String describe(String format) {
        format = format.replace("\\s+", "");

        if (format.length() > 10) {
            format = format.substring(0, 10) + "…";
        }

        return format;
    }

}
