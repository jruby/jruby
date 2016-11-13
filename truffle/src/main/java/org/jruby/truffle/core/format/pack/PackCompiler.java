/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.pack;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatRootNode;
import org.jruby.truffle.core.format.LoopRecovery;
import org.jruby.truffle.language.RubyNode;

import java.nio.charset.StandardCharsets;

public class PackCompiler {

    private final RubyContext context;
    private final RubyNode currentNode;

    public PackCompiler(RubyContext context, RubyNode currentNode) {
        this.context = context;
        this.currentNode = currentNode;
    }

    public CallTarget compile(String format) {
        if (format.length() > context.getOptions().PACK_RECOVER_LOOP_MIN) {
            format = LoopRecovery.recoverLoop(format);
        }

        final SimplePackTreeBuilder builder = new SimplePackTreeBuilder(context, currentNode);

        builder.enterSequence();

        final SimplePackParser parser = new SimplePackParser(builder, format.getBytes(StandardCharsets.US_ASCII));
        parser.parse();

        builder.exitSequence();

        return Truffle.getRuntime().createCallTarget(
                new FormatRootNode(context, currentNode.getEncapsulatingSourceSection(), builder.getEncoding(), builder.getNode()));
    }

}
