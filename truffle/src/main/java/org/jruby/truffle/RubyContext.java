/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerOptions;
import com.oracle.truffle.api.ExecutionContext;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.Ruby;
import org.jruby.truffle.builtins.PrimitiveManager;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.exception.CoreExceptions;
import org.jruby.truffle.core.kernel.AtExitManager;
import org.jruby.truffle.core.kernel.TraceManager;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.core.objectspace.ObjectSpaceManager;
import org.jruby.truffle.core.rope.RopeTable;
import org.jruby.truffle.core.string.CoreStrings;
import org.jruby.truffle.core.string.FrozenStrings;
import org.jruby.truffle.core.symbol.SymbolTable;
import org.jruby.truffle.core.thread.ThreadManager;
import org.jruby.truffle.extra.AttachmentsManager;
import org.jruby.truffle.interop.InteropManager;
import org.jruby.truffle.interop.JRubyInterop;
import org.jruby.truffle.language.CallStackManager;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.Options;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.SafepointManager;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.language.loader.FeatureLoader;
import org.jruby.truffle.language.loader.SourceCache;
import org.jruby.truffle.language.loader.SourceLoader;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.platform.NativePlatform;
import org.jruby.truffle.platform.NativePlatformFactory;
import org.jruby.truffle.stdlib.CoverageManager;
import org.jruby.truffle.tools.CallFrequency;
import org.jruby.truffle.tools.InstrumentationServerManager;
import org.jruby.truffle.tools.callgraph.CallGraph;
import org.jruby.truffle.tools.callgraph.SimpleWriter;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class RubyContext extends ExecutionContext {

    private static volatile RubyContext latestInstance;

    private final TruffleLanguage.Env env;
    private final Ruby jrubyRuntime;

    private final Options options = new Options();
    private final RopeTable ropeTable = new RopeTable();
    private final PrimitiveManager primitiveManager = new PrimitiveManager();
    private final JRubyInterop jrubyInterop = new JRubyInterop(this);
    private final SafepointManager safepointManager = new SafepointManager(this);
    private final SymbolTable symbolTable = new SymbolTable(this);
    private final InteropManager interopManager = new InteropManager(this);
    private final CodeLoader codeLoader = new CodeLoader(this);
    private final FeatureLoader featureLoader = new FeatureLoader(this);
    private final TraceManager traceManager;
    private final ObjectSpaceManager objectSpaceManager = new ObjectSpaceManager(this);
    private final AtExitManager atExitManager = new AtExitManager(this);
    private final SourceCache sourceCache = new SourceCache(new SourceLoader(this));
    private final CallStackManager callStack = new CallStackManager(this);
    private final CoreStrings coreStrings = new CoreStrings(this);
    private final FrozenStrings frozenStrings = new FrozenStrings(this);
    private final CoreExceptions coreExceptions = new CoreExceptions(this);

    private final CompilerOptions compilerOptions = Truffle.getRuntime().createCompilerOptions();

    private final NativePlatform nativePlatform;
    private final CoreLibrary coreLibrary;
    private final ThreadManager threadManager;
    private final LexicalScope rootLexicalScope;
    private final InstrumentationServerManager instrumentationServerManager;
    private final CallGraph callGraph;
    private final PrintStream debugStandardOut;
    private final CoverageManager coverageManager;
    private final CallFrequency callFrequency;

    private final Object classVariableDefinitionLock = new Object();

    private final AttachmentsManager attachmentsManager;

    public RubyContext(Ruby jrubyRuntime, TruffleLanguage.Env env) {
        latestInstance = this;

        this.jrubyRuntime = jrubyRuntime;
        this.env = env;

        if (options.CALL_GRAPH) {
            callGraph = new CallGraph();
        } else {
            callGraph = null;
        }

        // Stuff that needs to be loaded before we load any code

        /*
         * The Graal option TimeThreshold sets how long a method has to become hot after it has started running, in ms.
         * This is designed to not try to compile cold methods that just happen to be called enough times during a
         * very long running program. We haven't worked out the best value of this for Ruby yet, and the default value
         * produces poor benchmark results. Here we just set it to a very high value, to effectively disable it.
         */

        if (compilerOptions.supportsOption("MinTimeThreshold")) {
            compilerOptions.setOption("MinTimeThreshold", 100000000);
        }

        /*
         * The Graal option InliningMaxCallerSize sets the maximum size of a method for where we consider to inline
         * calls from that method. So it's the caller method we're talking about, not the called method. The default
         * value doesn't produce good results for Ruby programs, but we aren't sure why yet. Perhaps it prevents a few
         * key methods from the core library from inlining other methods.
         */

        if (compilerOptions.supportsOption("MinInliningMaxCallerSize")) {
            compilerOptions.setOption("MinInliningMaxCallerSize", 5000);
        }

        // Load the core library classes

        coreLibrary = new CoreLibrary(this);
        coreLibrary.initialize();

        // Create objects that need core classes

        nativePlatform = NativePlatformFactory.createPlatform(this);
        rootLexicalScope = new LexicalScope(null, coreLibrary.getObjectClass());

        threadManager = new ThreadManager(this);
        threadManager.initialize();

        // Load the nodes

        org.jruby.Main.printTruffleTimeMetric("before-load-nodes");
        coreLibrary.addCoreMethods();
        primitiveManager.addAnnotatedPrimitives();
        org.jruby.Main.printTruffleTimeMetric("after-load-nodes");

        // Systems which need to be loaded before we load Ruby core

        if (options.CALL_FREQUENCY) {
            callFrequency = new CallFrequency(this);
            callFrequency.start();
        } else {
            callFrequency = null;
        }

        // Load the reset of the core library

        coreLibrary.initializeAfterBasicMethodsAdded();

        // Load other subsystems

        final PrintStream configStandardOut = jrubyRuntime.getInstanceConfig().getOutput();
        debugStandardOut = (configStandardOut == System.out) ? null : configStandardOut;

        if (options.INSTRUMENTATION_SERVER_PORT != 0) {
            instrumentationServerManager = new InstrumentationServerManager(this, options.INSTRUMENTATION_SERVER_PORT);
            instrumentationServerManager.start();
        } else {
            instrumentationServerManager = null;
        }

        final Instrumenter instrumenter = env.lookup(Instrumenter.class);
        attachmentsManager = new AttachmentsManager(this, instrumenter);
        traceManager = new TraceManager(this, instrumenter);
        coverageManager = new CoverageManager(this, instrumenter);

        coreLibrary.initializePostBoot();
    }

    public Object send(Object object, String methodName, DynamicObject block, Object... arguments) {
        CompilerAsserts.neverPartOfCompilation();

        assert block == null || RubyGuards.isRubyProc(block);

        final InternalMethod method = ModuleOperations.lookupMethod(coreLibrary.getMetaClass(object), methodName);

        if (method == null || method.isUndefined()) {
            return null;
        }

        return method.getCallTarget().call(
                RubyArguments.pack(null, null, method, DeclarationContext.METHOD, null, object, block, arguments));
    }

    public void shutdown() {
        if (getOptions().ROPE_PRINT_INTERN_STATS) {
            System.out.println("Ropes re-used: " + getRopeTable().getRopesReusedCount());
            System.out.println("Rope byte arrays re-used: " + getRopeTable().getByteArrayReusedCount());
            System.out.println("Rope bytes saved: " + getRopeTable().getRopeBytesSaved());
            System.out.println("Total ropes interned: " + getRopeTable().totalRopes());
        }

        atExitManager.runSystemExitHooks();

        if (instrumentationServerManager != null) {
            instrumentationServerManager.shutdown();
        }

        threadManager.shutdown();

        if (options.COVERAGE_GLOBAL) {
            coverageManager.print(System.out);
        }

        if (callGraph != null) {
            callGraph.resolve();

            if (options.CALL_GRAPH_WRITE != null) {
                try (PrintStream stream = new PrintStream(options.CALL_GRAPH_WRITE, StandardCharsets.UTF_8.name())) {
                    new SimpleWriter(callGraph, stream).write();
                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Options getOptions() {
        return options;
    }

    public TruffleLanguage.Env getEnv() {
        return env;
    }

    public NativePlatform getNativePlatform() {
        return nativePlatform;
    }

    public JRubyInterop getJRubyInterop() {
        return jrubyInterop;
    }

    public Ruby getJRubyRuntime() {
        return jrubyRuntime;
    }

    public CoreLibrary getCoreLibrary() {
        return coreLibrary;
    }

    public PrintStream getDebugStandardOut() {
        return debugStandardOut;
    }

    public FeatureLoader getFeatureLoader() {
        return featureLoader;
    }

    public ObjectSpaceManager getObjectSpaceManager() {
        return objectSpaceManager;
    }

    public ThreadManager getThreadManager() {
        return threadManager;
    }

    public AtExitManager getAtExitManager() {
        return atExitManager;
    }

    public TraceManager getTraceManager() {
        return traceManager;
    }

    public SafepointManager getSafepointManager() {
        return safepointManager;
    }

    public LexicalScope getRootLexicalScope() {
        return rootLexicalScope;
    }

    public CompilerOptions getCompilerOptions() {
        return compilerOptions;
    }

    public PrimitiveManager getPrimitiveManager() {
        return primitiveManager;
    }

    public CoverageManager getCoverageManager() {
        return coverageManager;
    }

    public static RubyContext getLatestInstance() {
        return latestInstance;
    }

    public AttachmentsManager getAttachmentsManager() {
        return attachmentsManager;
    }

    public SourceCache getSourceCache() {
        return sourceCache;
    }

    public RopeTable getRopeTable() {
        return ropeTable;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public CallGraph getCallGraph() {
        return callGraph;
    }

    public CodeLoader getCodeLoader() {
        return codeLoader;
    }

    public InteropManager getInteropManager() {
        return interopManager;
    }

    public CallStackManager getCallStack() {
        return callStack;
    }

    public CoreStrings getCoreStrings() {
        return coreStrings;
    }

    public FrozenStrings getFrozenStrings() {
        return frozenStrings;
    }

    public Object getClassVariableDefinitionLock() {
        return classVariableDefinitionLock;
    }

    public Instrumenter getInstrumenter() {
        return env.lookup(Instrumenter.class);
    }

    public CoreExceptions getCoreExceptions() {
        return coreExceptions;
    }

    public CallFrequency getCallFrequency() {
        return callFrequency;
    }
}
