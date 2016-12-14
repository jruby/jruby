/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.debug;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.Source;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.parser.ParserContext;

public abstract class DebugHelpers {

    @Deprecated
    public static Object eval(String code, Object... arguments) {
        return eval(RubyContext.getInstance(), code, arguments);
    }

    @Deprecated
    @TruffleBoundary
    public static Object eval(RubyContext context, String code, Object... arguments) {
        final FrameInstance currentFrameInstance = Truffle.getRuntime().getCurrentFrame();

        final Frame currentFrame = currentFrameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE, true);

        final Object[] packedArguments = RubyArguments.pack(
                null,
                null,
                RubyArguments.getMethod(currentFrame),
                DeclarationContext.INSTANCE_EVAL,
                null,
                RubyArguments.getSelf(currentFrame),
                null,
                new Object[]{});

        final FrameDescriptor frameDescriptor = new FrameDescriptor(currentFrame.getFrameDescriptor().getDefaultValue());

        final MaterializedFrame evalFrame = Truffle.getRuntime().createMaterializedFrame(
                packedArguments,
                frameDescriptor);

        if (arguments.length % 2 == 1) {
            throw new UnsupportedOperationException("odd number of name-value pairs for arguments");
        }

        for (int n = 0; n < arguments.length; n += 2) {
            evalFrame.setObject(evalFrame.getFrameDescriptor().findOrAddFrameSlot(arguments[n]), arguments[n + 1]);
        }

        final Source source = Source.newBuilder(code).name("debug-eval").mimeType(RubyLanguage.MIME_TYPE).build();

        final RubyRootNode rootNode = context.getCodeLoader().parse(
                source,
                UTF8Encoding.INSTANCE,
                ParserContext.INLINE,
                evalFrame,
                true,
                null);

        final CodeLoader.DeferredCall deferredCall = context.getCodeLoader().prepareExecute(
                ParserContext.INLINE,
                DeclarationContext.INSTANCE_EVAL,
                rootNode,
                evalFrame,
                RubyArguments.getSelf(evalFrame));

        return deferredCall.callWithoutCallNode();
    }

}
