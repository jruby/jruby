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
import com.oracle.truffle.api.Truffle;
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
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.SequenceNode;
import org.jruby.truffle.language.exceptions.TopLevelRaiseHandler;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.translator.TranslatorDriver;
import org.jruby.util.ByteList;

import java.io.File;
import java.io.IOException;

public class CodeLoader {

    private final RubyContext context;

    public CodeLoader(RubyContext context) {
        this.context = context;
    }

    public void loadFile(String fileName, Node currentNode) throws IOException {
        load(context.getSourceCache().getSource(fileName), currentNode);
    }

    public void load(Source source, Node currentNode) {
        parseAndExecute(source, UTF8Encoding.INSTANCE, TranslatorDriver.ParserContext.TOP_LEVEL, context.getCoreLibrary().getMainObject(), null, true, DeclarationContext.TOP_LEVEL, currentNode);
    }

    @CompilerDirectives.TruffleBoundary
    public Object instanceEval(ByteList code, Object self, String filename, Node currentNode) {
        final Source source = Source.fromText(code, filename);
        return parseAndExecute(source, code.getEncoding(), TranslatorDriver.ParserContext.EVAL, self, null, true, DeclarationContext.INSTANCE_EVAL, currentNode);
    }

    @CompilerDirectives.TruffleBoundary
    public Object eval(TranslatorDriver.ParserContext parserContext, ByteList code, DynamicObject binding, boolean ownScopeForAssignments, String filename, Node currentNode) {
        assert RubyGuards.isRubyBinding(binding);
        final Source source = Source.fromText(code, filename);
        final MaterializedFrame frame = Layouts.BINDING.getFrame(binding);
        final DeclarationContext declarationContext = RubyArguments.getDeclarationContext(frame.getArguments());
        return parseAndExecute(source, code.getEncoding(), parserContext, RubyArguments.getSelf(frame.getArguments()), frame, ownScopeForAssignments, declarationContext, currentNode);
    }

    @CompilerDirectives.TruffleBoundary
    public Object parseAndExecute(Source source, Encoding defaultEncoding, TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, boolean ownScopeForAssignments,
                                  DeclarationContext declarationContext, Node currentNode) {
        final RubyRootNode rootNode = parse(source, defaultEncoding, parserContext, parentFrame, ownScopeForAssignments, currentNode);
        return execute(parserContext, declarationContext, rootNode, parentFrame, self);
    }

    @CompilerDirectives.TruffleBoundary
    public RubyRootNode parse(Source source, Encoding defaultEncoding, TranslatorDriver.ParserContext parserContext, MaterializedFrame parentFrame, boolean ownScopeForAssignments, Node currentNode) {
        final TranslatorDriver translator = new TranslatorDriver(context);
        return translator.parse(context, source, defaultEncoding, parserContext, null, parentFrame, ownScopeForAssignments, currentNode);
    }

    @CompilerDirectives.TruffleBoundary
    public Object execute(TranslatorDriver.ParserContext parserContext, DeclarationContext declarationContext, RubyRootNode rootNode, MaterializedFrame parentFrame, Object self) {
        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

        final DynamicObject declaringModule;
        if (parserContext == TranslatorDriver.ParserContext.EVAL && parentFrame != null) {
            declaringModule = RubyArguments.getMethod(parentFrame.getArguments()).getDeclaringModule();
        } else if (parserContext == TranslatorDriver.ParserContext.MODULE) {
            assert RubyGuards.isRubyModule(self);
            declaringModule = (DynamicObject) self;
        } else {
            declaringModule = context.getCoreLibrary().getObjectClass();
        }

        final InternalMethod method = new InternalMethod(rootNode.getSharedMethodInfo(), rootNode.getSharedMethodInfo().getName(),
                declaringModule, Visibility.PUBLIC, callTarget);

        return callTarget.call(RubyArguments.pack(parentFrame, null, method, declarationContext, null, self, null, new Object[]{}));
    }

    public Object execute(final org.jruby.ast.RootNode rootNode) {
        context.getCoreLibrary().getGlobalVariablesObject().define("$0", context.getJRubyInterop().toTruffle(context.getJRubyRuntime().getGlobalVariables().get("$0")), 0);

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

        final RubyRootNode originalRootNode = parse(source, UTF8Encoding.INSTANCE, TranslatorDriver.ParserContext.TOP_LEVEL, null, true, null);

        final SourceSection sourceSection = originalRootNode.getSourceSection();
        final RubyNode wrappedBody =
                new TopLevelRaiseHandler(context, sourceSection,
                        SequenceNode.sequence(context, sourceSection,
                                new SetTopLevelBindingNode(context, sourceSection),
                                new LoadRequiredLibrariesNode(context, sourceSection),
                                originalRootNode.getBody()));

        final RubyRootNode newRootNode = originalRootNode.withBody(wrappedBody);

        if (rootNode.hasEndPosition()) {
            final Object data = context.inlineRubyHelper(null, "Truffle::Primitive.get_data(file, offset)", "file", StringOperations.createString(context, ByteList.create(inputFile)), "offset", rootNode.getEndPosition());
            Layouts.MODULE.getFields(context.getCoreLibrary().getObjectClass()).setConstant(context, null, "DATA", data);
        }

        return execute(TranslatorDriver.ParserContext.TOP_LEVEL, DeclarationContext.TOP_LEVEL, newRootNode, null, context.getCoreLibrary().getMainObject());
    }

}
