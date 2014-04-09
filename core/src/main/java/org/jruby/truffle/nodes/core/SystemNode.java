/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import java.io.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;

/**
 * Represents an expression that is evaluated by running it as a system command via forking and
 * execing, and then taking stdout as a string.
 */
@NodeInfo(shortName = "system")
public class SystemNode extends RubyNode {

    @Child protected RubyNode child;

    public SystemNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyContext context = getContext();

        final String command = child.execute(frame).toString();

        Process process;

        try {
            // We need to run via bash to get the variable and other expansion we expect
            process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final InputStream stdout = process.getInputStream();
        final InputStreamReader reader = new InputStreamReader(stdout);

        final StringBuilder resultBuilder = new StringBuilder();

        // TODO(cs): this isn't great for binary output

        try {
            int c;

            while ((c = reader.read()) != -1) {
                resultBuilder.append((char) c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.err.println("COMMAND");
        System.err.println(command);
        System.err.println("HERE " + resultBuilder.toString());

        return context.makeString(resultBuilder.toString());
    }
}
