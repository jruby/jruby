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
import org.jruby.truffle.pack.nodes.control.NNode;
import org.jruby.truffle.pack.nodes.control.SequenceNode;
import org.jruby.truffle.pack.nodes.control.StarNode;
import org.jruby.truffle.pack.nodes.type.NullNode;
import org.jruby.truffle.pack.nodes.type.UInt32NativeNode;

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
                case 'x':
                    node = new NullNode();
                    break;
                case 'L':
                    node = new UInt32NativeNode();
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
        return Truffle.getRuntime().createCallTarget(new PackRootNode(sequence));
    }

}
