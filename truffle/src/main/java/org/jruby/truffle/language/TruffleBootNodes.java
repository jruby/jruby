/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.CoreMethodNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.language.loader.SourceLoader;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.parser.ParserContext;
import org.jruby.truffle.platform.UnsafeGroup;
import org.jruby.util.ByteList;

import java.io.File;
import java.io.IOException;

@CoreClass(name = "Truffle::Boot")
public abstract class TruffleBootNodes {

    @CoreMethod(names = "jruby_home_directory", onSingleton = true)
    public abstract static class JRubyHomeDirectoryNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject jrubyHomeDirectory() {
            return createString(StringOperations.encodeRope(getContext().getJRubyRuntime().getJRubyHome(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "jruby_home_directory_protocol", onSingleton = true)
    public abstract static class JRubyHomeDirectoryProtocolNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject jrubyHomeDirectoryProtocol() {
            String home = getContext().getJRubyRuntime().getJRubyHome();

            if (home.startsWith("uri:classloader:")) {
                home = home.substring("uri:classloader:".length());

                while (home.startsWith("/")) {
                    home = home.substring(1);
                }

                home = SourceLoader.JRUBY_SCHEME + "/" + home;
            }

            return createString(StringOperations.encodeRope(home, UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "install_rubinius_primitive", isModuleFunction = true, required = 1)
    public abstract static class InstallRubiniusPrimitiveNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyMethod(rubyMethod)")
        public Object installRubiniusPrimitive(DynamicObject rubyMethod) {
            String name = Layouts.METHOD.getMethod(rubyMethod).getName();
            getContext().getRubiniusPrimitiveManager().installPrimitive(name, rubyMethod);
            return nil();
        }
    }

    @CoreMethod(names = "context", onSingleton = true)
    public abstract static class ContextNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public RubyContext context() {
            return getContext();
        }
    }

    @CoreMethod(names = "run_jruby_root", onSingleton = true)
    public abstract static class RunJRubyRootNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object runJRubyRootNode(VirtualFrame frame, @Cached("create()")IndirectCallNode callNode) {
            coreLibrary().getGlobalVariables().put(
                    "$0",
                    StringOperations.createString(getContext(),
                            ByteList.create(getContext().getJRubyInterop().getArg0())));

            String inputFile = getContext().getInitialJRubyRootNode().getPosition().getFile();

            final Source source;

            try {
                if (!inputFile.equals("-e")) {
                    inputFile = new File(inputFile).getCanonicalPath();
                }

                source = getContext().getSourceCache().getSource(inputFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

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
    }

    @CoreMethod(names = "original_argv", onSingleton = true)
    public abstract static class OriginalArgvNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject originalArgv() {
            final String[] argv = getContext().getJRubyInterop().getArgv();
            final Object[] array = new Object[argv.length];

            for (int n = 0; n < array.length; n++) {
                array[n] = StringOperations.createString(getContext(), StringOperations.encodeRope(argv[n], UTF8Encoding.INSTANCE));
            }

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), array, array.length);
        }

    }

    @CoreMethod(names = "original_load_path", onSingleton = true)
    public abstract static class OriginalLoadPathNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject originalLoadPath() {
            final String[] path = getContext().getJRubyInterop().getOriginalLoadPath();
            final Object[] array = new Object[path.length];

            for (int n = 0; n < array.length; n++) {
                array[n] = StringOperations.createString(getContext(), StringOperations.encodeRope(path[n], UTF8Encoding.INSTANCE));
            }

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), array, array.length);
        }

    }

    @CoreMethod(names = "require_core", isModuleFunction = true, required = 1)
    public abstract static class RequireCoreNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(feature)")
        public boolean requireRelative(VirtualFrame frame, DynamicObject feature, @Cached("create()") IndirectCallNode callNode) {
            final CoreLibrary coreLibrary = getContext().getCoreLibrary();
            if (!(coreLibrary.isLoadingRubyCore() || getContext().getOptions().PLATFORM_SAFE_LOAD)) {
                throw new RaiseException(coreExceptions().internalErrorUnsafe(this));
            }

            final CodeLoader codeLoader = getContext().getCodeLoader();
            final String path = coreLibrary.getCoreLoadPath() + "/" + feature.toString() + ".rb";
            try {
                final RubyRootNode rootNode = codeLoader.parse(getContext().getSourceCache().getSource(path), UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, null, true, this);
                final CodeLoader.DeferredCall deferredCall = codeLoader.prepareExecute(ParserContext.TOP_LEVEL, DeclarationContext.TOP_LEVEL, rootNode, null, coreLibrary.getMainObject());
                deferredCall.callWithoutCallNode();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return true;
        }
    }

}
