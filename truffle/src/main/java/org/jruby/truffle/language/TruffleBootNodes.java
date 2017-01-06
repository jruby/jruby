/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.collections.Memo;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.control.JavaException;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.parser.ParserContext;
import org.jruby.truffle.parser.TranslatorDriver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@CoreClass("Truffle::Boot")
public abstract class TruffleBootNodes {

    @CoreMethod(names = "ruby_home", onSingleton = true)
    public abstract static class RubyHomeNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject ruby_home() {
            if (getContext().getRubyHome() == null) {
                return nil();
            } else {
                return createString(StringOperations.encodeRope(getContext().getRubyHome(), UTF8Encoding.INSTANCE));
            }
        }

    }

    @CoreMethod(names = "context", onSingleton = true)
    public abstract static class ContextNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public RubyContext context() {
            return getContext();
        }
    }

    @CoreMethod(names = "main", onSingleton = true)
    public abstract static class MainNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object main(VirtualFrame frame, @Cached("create()") IndirectCallNode callNode) {
            String inputFile = getContext().getOriginalInputFile();

            final Source source = getMainSource(inputFile);

            final RubyRootNode rootNode = getContext().getCodeLoader().parse(
                    source,
                    UTF8Encoding.INSTANCE,
                    ParserContext.TOP_LEVEL_FIRST,
                    null,
                    true,
                    null);

            final CodeLoader.DeferredCall deferredCall = getContext().getCodeLoader().prepareExecute(
                    ParserContext.TOP_LEVEL,
                    DeclarationContext.TOP_LEVEL,
                    rootNode,
                    null,
                    coreLibrary().getMainObject());

            return deferredCall.call(frame, callNode);
        }

        @TruffleBoundary
        public synchronized Source getMainSource(String path) {
            try {
                return getContext().getSourceLoader().loadMain(path);
            } catch (IOException e) {
                throw new JavaException(e);
            }
        }

    }

    @CoreMethod(names = "original_argv", onSingleton = true)
    public abstract static class OriginalArgvNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject originalArgv() {
            final String[] argv = getContext().getOptions().ARGUMENTS;
            final Object[] array = new Object[argv.length];

            for (int n = 0; n < array.length; n++) {
                array[n] = StringOperations.createString(getContext(), StringOperations.encodeRope(argv[n], UTF8Encoding.INSTANCE));
            }

            return createArray(array, array.length);
        }

    }

    @CoreMethod(names = "original_input_file", onSingleton = true)
    public abstract static class OriginalInputFileNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject originalInputFile() {
            return createString(StringOperations.encodeRope(getContext().getOriginalInputFile(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "original_load_path", onSingleton = true)
    public abstract static class OriginalLoadPathNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject originalLoadPath() {
            final String[] paths = getContext().getOptions().LOAD_PATHS;
            final Object[] array = new Object[paths.length];

            for (int n = 0; n < array.length; n++) {
                array[n] = StringOperations.createString(getContext(), StringOperations.encodeRope(paths[n], UTF8Encoding.INSTANCE));
            }

            return createArray(array, array.length);
        }

    }
    
    @CoreMethod(names = "rubygems_enabled?", onSingleton = true)
    public abstract static class RubygemsEnabledNode extends CoreMethodNode {

        @Specialization
        public boolean isRubygemsEnabled() {
            return !getContext().getOptions().DISABLE_GEMS;
        }

    }

    @CoreMethod(names = "source_of_caller", isModuleFunction = true)
    public abstract static class SourceOfCallerNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject sourceOfCaller() {
            final Memo<Integer> frameCount = new Memo<>(0);

            final String source = Truffle.getRuntime().iterateFrames(frameInstance -> {
                if (frameCount.get() == 2) {
                    return frameInstance.getCallNode().getEncapsulatingSourceSection().getSource().getName();
                } else {
                    frameCount.set(frameCount.get() + 1);
                    return null;
                }
            });

            if (source == null) {
                return nil();
            }

            return createString(StringOperations.encodeRope(source, UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "inner_check_syntax", onSingleton = true)
    public abstract static class InnerCheckSyntaxNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject innerCheckSyntax() {
            final String inputFile = getContext().getOriginalInputFile();
            final InputStream inputStream = getContext().getSyntaxCheckInputStream();

            final Source source;

            try {
                source = Source.newBuilder(new InputStreamReader(inputStream)).name(inputFile).mimeType(RubyLanguage.MIME_TYPE).build();
            } catch (IOException e) {
                throw new JavaException(e);
            }

            final TranslatorDriver translator = new TranslatorDriver(getContext());

            translator.parse(getContext(), source, UTF8Encoding.INSTANCE,
                    ParserContext.TOP_LEVEL, new String[]{}, null, null, true, null);

            return nil();
        }
    }

}
