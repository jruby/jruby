/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.loader;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.LoadRequiredLibrariesNode;
import org.jruby.truffle.core.SetTopLevelBindingNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.exceptions.TopLevelRaiseHandler;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.parser.ParserContext;
import org.jruby.truffle.language.parser.jruby.Translator;
import org.jruby.truffle.language.parser.jruby.TranslatorDriver;
import org.jruby.util.ByteList;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class CodeLoader {

    private final RubyContext context;

    public CodeLoader(RubyContext context) {
        this.context = context;
    }

    @TruffleBoundary
    public RubyRootNode parse(Source source,
                              Encoding defaultEncoding,
                              ParserContext parserContext,
                              MaterializedFrame parentFrame,
                              boolean ownScopeForAssignments,
                              Node currentNode) {
        final TranslatorDriver translator = new TranslatorDriver(context);

        return translator.parse(context, source, defaultEncoding, parserContext, null, parentFrame,
                ownScopeForAssignments, currentNode);
    }

    @TruffleBoundary
    public Object execute(ParserContext parserContext,
                          DeclarationContext declarationContext,
                          RubyRootNode rootNode,
                          MaterializedFrame parentFrame,
                          Object self) {
        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

        final DynamicObject declaringModule;

        if (parserContext == ParserContext.EVAL && parentFrame != null) {
            declaringModule = RubyArguments.getMethod(parentFrame).getDeclaringModule();
        } else if (parserContext == ParserContext.MODULE) {
            declaringModule = (DynamicObject) self;
        } else {
            declaringModule = context.getCoreLibrary().getObjectClass();
        }

        final InternalMethod method = new InternalMethod(
                rootNode.getSharedMethodInfo(),
                rootNode.getSharedMethodInfo().getName(),
                declaringModule,
                Visibility.PUBLIC,
                callTarget);

        return callTarget.call(RubyArguments.pack(
                parentFrame,
                null,
                method,
                declarationContext,
                null,
                self,
                null,
                new Object[]{}));
    }

    public Object parseAndExecuteFirstFile(Source source, boolean hasEndPosition, int endPosition) {
        context.getCoreLibrary().getGlobalVariablesObject().define(
                "$0",
                StringOperations.createString(context, ByteList.create(context.getJRubyInterop().getArg0())));

        context.getFeatureLoader().setMainScriptSource(source);

        final RubyRootNode rootNode = parse(
                source,
                UTF8Encoding.INSTANCE,
                ParserContext.TOP_LEVEL_FIRST,
                null,
                true,
                null);

        if (hasEndPosition) {
            final Object data = inline(
                    null,
                    "Truffle::Primitive.get_data(file, offset)",
                    "file", StringOperations.createString(context, ByteList.create(source.getPath())),
                    "offset", endPosition);

            Layouts.MODULE.getFields(context.getCoreLibrary().getObjectClass()).setConstant(context, null, "DATA", data);
        }

        return execute(
                ParserContext.TOP_LEVEL,
                DeclarationContext.TOP_LEVEL,
                rootNode,
                null,
                context.getCoreLibrary().getMainObject());
    }

    public Object execute(final org.jruby.ast.RootNode rootNode) {
        String inputFile = rootNode.getPosition().getFile();

        final Source source;

        try {
            if (!inputFile.equals("-e")) {
                inputFile = new File(inputFile).getCanonicalPath();
            }

            source = context.getSourceCache().getSource(inputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return parseAndExecuteFirstFile(source, rootNode.hasEndPosition(), rootNode.getEndPosition());
    }

    @CompilerDirectives.TruffleBoundary
    public Object inline(Node currentNode, String expression, Object... arguments) {
        final Frame frame = Truffle.getRuntime().getCurrentFrame().getFrame(FrameInstance.FrameAccess.MATERIALIZE, true);
        return inline(currentNode, frame, expression, arguments);
    }

    public Object inline(Node currentNode, Frame frame, String expression, Object... arguments) {
        final Object[] packedArguments = RubyArguments.pack(
                null,
                null,
                RubyArguments.getMethod(frame),
                DeclarationContext.INSTANCE_EVAL,
                null,
                RubyArguments.getSelf(frame),
                null,
                new Object[]{});

        final FrameDescriptor frameDescriptor = new FrameDescriptor(frame.getFrameDescriptor().getDefaultValue());

        final MaterializedFrame evalFrame = Truffle.getRuntime().createMaterializedFrame(
                packedArguments,
                frameDescriptor);

        if (arguments.length % 2 == 1) {
            throw new UnsupportedOperationException("odd number of name-value pairs for arguments");
        }

        for (int n = 0; n < arguments.length; n += 2) {
            evalFrame.setObject(evalFrame.getFrameDescriptor().findOrAddFrameSlot(arguments[n]), arguments[n + 1]);
        }

        final Source source = Source.fromText(StringOperations.createByteList(expression), "inline-ruby");

        final RubyRootNode rootNode = context.getCodeLoader().parse(
                source,
                UTF8Encoding.INSTANCE,
                ParserContext.INLINE,
                evalFrame,
                true,
                currentNode);

        return context.getCodeLoader().execute(
                ParserContext.INLINE,
                DeclarationContext.INSTANCE_EVAL,
                rootNode,
                evalFrame,
                RubyArguments.getSelf(evalFrame));
    }

}
