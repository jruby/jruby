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
import com.oracle.truffle.api.CompilerAsserts;
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
import org.jruby.truffle.core.binding.BindingNodes;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
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

    public Object execute(final org.jruby.ast.RootNode rootNode) {
        context.getCoreLibrary().getGlobalVariablesObject().define("$0", StringOperations.createString(context, ByteList.create(context.getJRubyInterop().getArg0())), 0);

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

        context.getFeatureLoader().setMainScriptSource(source);

        final RubyRootNode originalRootNode = parse(source, UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, null, true, null);

        final SourceSection sourceSection = originalRootNode.getSourceSection();
        final RubyNode wrappedBody =
                new TopLevelRaiseHandler(context, sourceSection,
                        Translator.sequence(context, sourceSection, Arrays.asList(new SetTopLevelBindingNode(context, sourceSection), new LoadRequiredLibrariesNode(context, sourceSection), originalRootNode.getBody())));

        final RubyRootNode newRootNode = originalRootNode.withBody(wrappedBody);

        if (rootNode.hasEndPosition()) {
            final Object data = context.getCodeLoader().inlineRubyHelper(null, "Truffle::Primitive.get_data(file, offset)", "file", StringOperations.createString(context, ByteList.create(inputFile)), "offset", rootNode.getEndPosition());
            Layouts.MODULE.getFields(context.getCoreLibrary().getObjectClass()).setConstant(context, null, "DATA", data);
        }

        return execute(ParserContext.TOP_LEVEL, DeclarationContext.TOP_LEVEL, newRootNode, null, context.getCoreLibrary().getMainObject());
    }

    @CompilerDirectives.TruffleBoundary
    public Object inlineRubyHelper(Node currentNode, String expression, Object... arguments) {
        return inlineRubyHelper(currentNode, Truffle.getRuntime().getCurrentFrame().getFrame(FrameInstance.FrameAccess.MATERIALIZE, true), expression, arguments);
    }

    public Object inlineRubyHelper(Node currentNode, Frame frame, String expression, Object... arguments) {
        final MaterializedFrame evalFrame1 = Truffle.getRuntime().createMaterializedFrame(
                RubyArguments.pack(null, null, RubyArguments.getMethod(frame), DeclarationContext.INSTANCE_EVAL, null, RubyArguments.getSelf(frame), null, new Object[]{}),
                new FrameDescriptor(frame.getFrameDescriptor().getDefaultValue()));

        if (arguments.length % 2 == 1) {
            throw new UnsupportedOperationException("odd number of name-value pairs for arguments");
        }

        for (int n = 0; n < arguments.length; n += 2) {
            evalFrame1.setObject(evalFrame1.getFrameDescriptor().findOrAddFrameSlot(arguments[n]), arguments[n + 1]);
        }

        final MaterializedFrame evalFrame = evalFrame1;
        final DynamicObject binding = BindingNodes.createBinding(context, evalFrame);
        ByteList code = StringOperations.createByteList(expression);
        final Source source = Source.fromText(code, "inline-ruby");
        final MaterializedFrame frame1 = Layouts.BINDING.getFrame(binding);
        final DeclarationContext declarationContext = RubyArguments.getDeclarationContext(frame1);
        final RubyRootNode rootNode = context.getCodeLoader().parse(source, code.getEncoding(), ParserContext.INLINE, frame1, true, currentNode);
        return context.getCodeLoader().execute(ParserContext.INLINE, declarationContext, rootNode, frame1, RubyArguments.getSelf(frame1));
    }

}
