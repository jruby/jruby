/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.backtrace.InternalRootNode;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.parser.ParserContext;
import org.jruby.truffle.language.parser.jruby.TranslatorDriver;

import java.lang.ref.Reference;
import java.util.List;

public class LazyRubyRootNode extends RootNode implements InternalRootNode {

    private final Reference<RubyContext> contextReference;
    private final Source source;
    private final List<String> argumentNames;

    @CompilationFinal private RubyContext cachedContext;
    @CompilationFinal private DynamicObject mainObject;
    @CompilationFinal private InternalMethod method;

    @Child private DirectCallNode callNode;

    public LazyRubyRootNode(Reference<RubyContext> contextReference, Source source, List<String> argumentNames) {
        super(RubyLanguage.class, null, null);
        this.contextReference = contextReference;
        this.source = source;
        this.argumentNames = argumentNames;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyContext context = contextReference.get();

        if (callNode == null || context != cachedContext) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            final TranslatorDriver translator = new TranslatorDriver(context);

            final RubyRootNode rootNode = translator.parse(context, source, UTF8Encoding.INSTANCE,
                    ParserContext.TOP_LEVEL, argumentNames.toArray(new String[argumentNames.size()]), null, null, true, null);

            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

            callNode = insert(Truffle.getRuntime().createDirectCallNode(callTarget));
            callNode.forceInlining();

            mainObject = context.getCoreLibrary().getMainObject();
            method = new InternalMethod(rootNode.getSharedMethodInfo(), rootNode.getSharedMethodInfo().getName(),
                    context.getCoreLibrary().getObjectClass(), Visibility.PUBLIC, callTarget);
        }

        return callNode.call(frame, RubyArguments.pack(null, null, method, DeclarationContext.TOP_LEVEL, null,
                mainObject, null, frame.getArguments()));
    }

}
