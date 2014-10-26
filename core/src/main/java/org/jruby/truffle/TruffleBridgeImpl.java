/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.source.BytesDecoder;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import org.jruby.TruffleBridge;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.methods.MethodDefinitionNode;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.util.Supplier;
import org.jruby.truffle.translator.TranslatorDriver;
import org.jruby.util.cli.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TruffleBridgeImpl implements TruffleBridge {

    private final org.jruby.Ruby runtime;
    private final RubyContext truffleContext;

    public TruffleBridgeImpl(org.jruby.Ruby runtime) {
        assert runtime != null;

        this.runtime = runtime;

        // Set up a context

        truffleContext = new RubyContext(runtime);
    }

    @Override
    public void init() {
        if (Options.TRUFFLE_PRINT_RUNTIME.load()) {
            runtime.getInstanceConfig().getError().println("jruby: using " + Truffle.getRuntime().getName());
        }

        // Bring in core method nodes

        RubyClass rubyObjectClass = truffleContext.getCoreLibrary().getObjectClass();
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, ArrayNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, BasicObjectNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, BindingNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, BignumNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, ClassNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, ContinuationNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, ComparableNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, DirNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, ExceptionNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, FalseClassNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, FiberNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, FileNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, FixnumNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, FloatNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, HashNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, GCNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, IONodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, KernelNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, MainNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, MatchDataNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, MathNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, ModuleNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, NilClassNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, ObjectSpaceNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, ProcessNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, ProcNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, RangeNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, RegexpNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, SignalNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, StringNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, SymbolNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, ThreadNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, TimeNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, TrueClassNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, TruffleDebugNodesFactory.getFactories());
        CoreMethodNodeManager.addCoreMethodNodes(rubyObjectClass, EncodingNodesFactory.getFactories());

        // Give the core library manager a chance to tweak some of those methods

        truffleContext.getCoreLibrary().initializeAfterMethodsAdded();

        // Set program arguments

        for (IRubyObject arg : ((org.jruby.RubyArray) runtime.getObject().getConstant("ARGV")).toJavaArray()) {
            assert arg != null;

            truffleContext.getCoreLibrary().getArgv().slowPush(truffleContext.makeString(arg.toString()));
        }

        // Set the load path

        final RubyArray loadPath = (RubyArray) truffleContext.getCoreLibrary().getGlobalVariablesObject().getInstanceVariable("$:");

        for (IRubyObject path : ((org.jruby.RubyArray) runtime.getLoadService().getLoadPath()).toJavaArray()) {
            loadPath.slowPush(truffleContext.makeString(path.toString()));
        }

        // Hook

        if (truffleContext.getHooks() != null) {
            truffleContext.getHooks().afterInit(truffleContext);
        }
    }

    @Override
    public TruffleMethod truffelize(DynamicMethod originalMethod, org.jruby.ast.ArgsNode argsNode, org.jruby.ast.Node bodyNode) {
        final MethodDefinitionNode methodDefinitionNode = truffleContext.getTranslator().parse(truffleContext, null, argsNode, bodyNode, null);
        return new TruffleMethod(originalMethod, Truffle.getRuntime().createCallTarget(methodDefinitionNode.getMethodRootNode()));
    }

    @Override
    public Object execute(final TranslatorDriver.ParserContext parserContext, final Object self, final MaterializedFrame parentFrame, final org.jruby.ast.RootNode rootNode) {
        return truffleContext.handlingTopLevelRaise(new Supplier<Object>() {

            @Override
            public Object get() {
                final String inputFile = rootNode.getPosition().getFile();

                final Source source;

                if (inputFile.equals("-e")) {
                    // Assume UTF-8 for the moment
                    source = Source.fromBytes(runtime.getInstanceConfig().inlineScript(), "-e", new BytesDecoder.UTF8BytesDecoder());
                } else {
                    final byte[] bytes;

                    try {
                        bytes = Files.readAllBytes(Paths.get(inputFile));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    // Assume UTF-8 for the moment

                    source = Source.fromBytes(bytes, inputFile, new BytesDecoder.UTF8BytesDecoder());
                }

                final RubyRootNode parsedRootNode = truffleContext.getTranslator().parse(truffleContext, source, parserContext, parentFrame, null);
                final CallTarget callTarget = Truffle.getRuntime().createCallTarget(parsedRootNode);
                return callTarget.call(RubyArguments.pack(null, parentFrame, self, null, new Object[]{}));
            }

        }, truffleContext.getCoreLibrary().getNilObject());
    }

    @Override
    public IRubyObject toJRuby(Object object) {
        return truffleContext.toJRuby(object);
    }

    @Override
    public Object toTruffle(IRubyObject object) {
        return truffleContext.toTruffle(object);
    }

    @Override
    public void shutdown() {
        truffleContext.shutdown();
    }

}
