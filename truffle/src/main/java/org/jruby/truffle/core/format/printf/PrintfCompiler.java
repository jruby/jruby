/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.printf;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatEncoding;
import org.jruby.truffle.core.format.FormatRootNode;
import org.jruby.truffle.language.RubyNode;

import java.util.List;

public class PrintfCompiler {

    private final RubyContext context;
    private final RubyNode currentNode;

    public PrintfCompiler(RubyContext context, RubyNode currentNode) {
        this.context = context;
        this.currentNode = currentNode;
    }

    public CallTarget compile(byte[] format, Object[] arguments, boolean isDebug) {
        final PrintfSimpleParser parser = new PrintfSimpleParser(bytesToChars(format), arguments, isDebug);
        final List<SprintfConfig> configs = parser.parse();
        final PrintfSimpleTreeBuilder builder = new PrintfSimpleTreeBuilder(context, configs);

        return Truffle.getRuntime().createCallTarget(
            new FormatRootNode(context, currentNode.getEncapsulatingSourceSection(),
                FormatEncoding.DEFAULT, builder.getNode()));
    }

    private static char[] bytesToChars(byte[] bytes) {
        final char[] chars = new char[bytes.length];

        for (int n = 0; n < bytes.length; n++) {
            chars[n] = (char) bytes[n];
        }

        return chars;
    }

}
