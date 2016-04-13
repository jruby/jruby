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
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.MaterializedFrame;
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
import org.jruby.truffle.extra.AttachmentsManager;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.backtrace.InternalRootNode;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.parser.ParserContext;
import org.jruby.truffle.language.parser.jruby.TranslatorDriver;

public class LazyRubyRootNode extends RootNode implements InternalRootNode {

    private final Source source;
    private final String[] argumentNames;

    @CompilationFinal private RubyContext cachedContext;
    @CompilationFinal private DynamicObject mainObject;
    @CompilationFinal private InternalMethod method;

    @Child private Node findContextNode;
    @Child private DirectCallNode callNode;

    public LazyRubyRootNode(SourceSection sourceSection, FrameDescriptor frameDescriptor, Source source,
                            String[] argumentNames) {
        super(RubyLanguage.class, sourceSection, frameDescriptor);
        this.source = source;
        this.argumentNames = argumentNames;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (findContextNode == null) {
            CompilerDirectives.transferToInterpreter();
            findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
        }

        final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);

        if (cachedContext == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cachedContext = context;
        }

        if (callNode == null || context != cachedContext) {
            CompilerDirectives.transferToInterpreter();

            if (AttachmentsManager.ATTACHMENT_SOURCE == source) {
                final SourceSection sourceSection = (SourceSection) frame.getArguments()[getIndex("section")];
                final DynamicObject block = (DynamicObject) frame.getArguments()[getIndex("block")];

                final RootNode rootNode = new AttachmentsManager.AttachmentRootNode(RubyLanguage.class, cachedContext,
                        sourceSection, null, block);

                final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

                callNode = insert(Truffle.getRuntime().createDirectCallNode(callTarget));
                callNode.forceInlining();
            } else {
                final TranslatorDriver translator = new TranslatorDriver(context);

                final RubyRootNode rootNode = translator.parse(context, source, UTF8Encoding.INSTANCE,
                        ParserContext.TOP_LEVEL, argumentNames, null, true, null);

                final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

                callNode = insert(Truffle.getRuntime().createDirectCallNode(callTarget));
                callNode.forceInlining();

                mainObject = context.getCoreLibrary().getMainObject();
                method = new InternalMethod(rootNode.getSharedMethodInfo(), rootNode.getSharedMethodInfo().getName(),
                        context.getCoreLibrary().getObjectClass(), Visibility.PUBLIC, callTarget);
            }
        }

        if (method == null) {
            final MaterializedFrame callerFrame = Truffle.getRuntime().getCallerFrame()
                    .getFrame(FrameInstance.FrameAccess.MATERIALIZE, false).materialize();

            return callNode.call(frame, new Object[] { callerFrame });
        }

        return callNode.call(frame, RubyArguments.pack(null, null, method, DeclarationContext.TOP_LEVEL, null,
                mainObject, null, frame.getArguments()));
    }

    private int getIndex(String name) {
        for (int i = 0; i < argumentNames.length; i++) {
            if (name.equals(argumentNames[i])) {
                return i;
            }
        }
        return -1;
    }

}
