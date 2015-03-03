/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.BytesDecoder;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.tools.CoverageTracker;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyNil;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.TruffleHooks;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.instrument.RubyDefaultASTProber;
import org.jruby.truffle.nodes.methods.SetMethodDeclarationContext;
import org.jruby.truffle.nodes.rubinius.RubiniusPrimitiveManager;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.subsystems.*;
import org.jruby.truffle.runtime.util.FileUtils;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.truffle.translator.TranslatorDriver;
import org.jruby.util.ByteList;
import org.jruby.util.cli.Options;

import java.io.File;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The global state of a running Ruby system.
 */
public class RubyContext extends ExecutionContext {

    private static final boolean TRUFFLE_COVERAGE = Options.TRUFFLE_COVERAGE.load();
    private static final int INSTRUMENTATION_SERVER_PORT = Options.TRUFFLE_INSTRUMENTATION_SERVER_PORT.load();

    private final Ruby runtime;
    private final TranslatorDriver translator;
    private final CoreLibrary coreLibrary;
    private final FeatureManager featureManager;
    private final TraceManager traceManager;
    private final ObjectSpaceManager objectSpaceManager;
    private final ThreadManager threadManager;
    private final FiberManager fiberManager;
    private final AtExitManager atExitManager;
    private final RubySymbol.SymbolTable symbolTable = new RubySymbol.SymbolTable(this);
    private final Shape emptyShape;
    private final Warnings warnings;
    private final SafepointManager safepointManager;
    private final Random random = new Random();
    private final LexicalScope rootLexicalScope;
    private final CompilerOptions compilerOptions;
    private final RubiniusPrimitiveManager rubiniusPrimitiveManager;
    private final CoverageTracker coverageTracker;
    private final InstrumentationServerManager instrumentationServerManager;
    private final AttachmentsManager attachmentsManager;
    private final SourceManager sourceManager;

    private final AtomicLong nextObjectID = new AtomicLong(ObjectIDOperations.FIRST_OBJECT_ID);

    private final boolean runningOnWindows;

    public RubyContext(Ruby runtime) {
        assert runtime != null;

        compilerOptions = Truffle.getRuntime().createCompilerOptions();

        if (compilerOptions.supportsOption("MinTimeThreshold")) {
            compilerOptions.setOption("MinTimeThreshold", 100000000);
        }

        if (compilerOptions.supportsOption("MinInliningMaxCallerSize")) {
            compilerOptions.setOption("MinInliningMaxCallerSize", 5000);
        }

        // TODO CS 28-Feb-15 this is global
        Probe.registerASTProber(new RubyDefaultASTProber());

        // TODO(CS, 28-Jan-15) this is global
        // TODO(CS, 28-Jan-15) maybe not do this for core?
        if (TRUFFLE_COVERAGE) {
            coverageTracker = new CoverageTracker();
        } else {
            coverageTracker = null;
        }

        safepointManager = new SafepointManager(this);

        this.runtime = runtime;
        translator = new TranslatorDriver();

        warnings = new Warnings(this);

        // Object space manager needs to come early before we create any objects
        objectSpaceManager = new ObjectSpaceManager(this);

        emptyShape = RubyBasicObject.LAYOUT.createShape(new RubyOperations(this));

        coreLibrary = new CoreLibrary(this);
        coreLibrary.initialize();

        featureManager = new FeatureManager(this);
        traceManager = new TraceManager();
        atExitManager = new AtExitManager();

        // Must initialize threads before fibers

        threadManager = new ThreadManager(this);
        fiberManager = new FiberManager(this);

        rootLexicalScope = new LexicalScope(null, coreLibrary.getObjectClass());

        rubiniusPrimitiveManager = RubiniusPrimitiveManager.create();

        if (INSTRUMENTATION_SERVER_PORT != 0) {
            instrumentationServerManager = new InstrumentationServerManager(this, INSTRUMENTATION_SERVER_PORT);
            instrumentationServerManager.start();
        } else {
            instrumentationServerManager = null;
        }

        runningOnWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).indexOf("win") >= 0;

        attachmentsManager = new AttachmentsManager(this);

        sourceManager = new SourceManager(this);
    }

    public Shape getEmptyShape() {
        return emptyShape;
    }

    public static String checkInstanceVariableName(RubyContext context, String name, Node currentNode) {
        RubyNode.notDesignedForCompilation();

        if (!name.startsWith("@")) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreLibrary().nameErrorInstanceNameNotAllowable(name, currentNode));
        }

        return name;
    }

    public static String checkClassVariableName(RubyContext context, String name, Node currentNode) {
        RubyNode.notDesignedForCompilation();

        if (!name.startsWith("@@")) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreLibrary().nameErrorInstanceNameNotAllowable(name, currentNode));
        }

        return name;
    }

    public boolean isRunningOnWindows() {
        return runningOnWindows;
    }

    public void loadFile(String fileName, Node currentNode) {
        if (new File(fileName).isAbsolute()) {
            loadFileAbsolute(fileName, currentNode);
        } else {
            loadFileAbsolute(this.getRuntime().getCurrentDirectory() + File.separator + fileName, currentNode);
        }
    }

    private void loadFileAbsolute(String fileName, Node currentNode) {
        final Source source = sourceManager.forFile(fileName);
        load(source, currentNode, NodeWrapper.IDENTITY);
    }

    public void load(Source source, Node currentNode, final NodeWrapper nodeWrapper) {
        final NodeWrapper loadWrapper = new NodeWrapper() {
            @Override
            public RubyNode wrap(RubyNode node) {
                return new SetMethodDeclarationContext(node.getContext(), node.getSourceSection(), Visibility.PRIVATE, "load", node);
            }
        };

        final NodeWrapper composed = new NodeWrapper() {
            @Override
            public RubyNode wrap(RubyNode node) {
                return nodeWrapper.wrap(loadWrapper.wrap(node));
            }
        };

        execute(source, UTF8Encoding.INSTANCE, TranslatorDriver.ParserContext.TOP_LEVEL, coreLibrary.getMainObject(), null, currentNode, composed);
    }

    public RubySymbol.SymbolTable getSymbolTable() {
        return symbolTable;
    }


    @CompilerDirectives.TruffleBoundary
    public RubySymbol newSymbol(String name) {
        return symbolTable.getSymbol(name, ASCIIEncoding.INSTANCE);
    }

    @CompilerDirectives.TruffleBoundary
    public RubySymbol newSymbol(String name, Encoding encoding) {
        return symbolTable.getSymbol(name, encoding);
    }

    @CompilerDirectives.TruffleBoundary
    public RubySymbol newSymbol(ByteList name) {
        return symbolTable.getSymbol(name);
    }

    @TruffleBoundary
    public Object instanceEval(ByteList code, Object self, String filename, Node currentNode) {
        final Source source = Source.fromText(code, filename);
        return execute(source, code.getEncoding(), TranslatorDriver.ParserContext.EVAL, self, null, currentNode, new NodeWrapper() {
            @Override
            public RubyNode wrap(RubyNode node) {
                return new SetMethodDeclarationContext(node.getContext(), node.getSourceSection(), Visibility.PUBLIC, "instance_eval", node);
            }
        });
    }

    public Object instanceEval(ByteList code, Object self, Node currentNode) {
        return instanceEval(code, self, "(eval)", currentNode);
    }

    @TruffleBoundary
    public Object eval(ByteList code, RubyBinding binding, boolean ownScopeForAssignments, String filename, Node currentNode) {
        final Source source = Source.fromText(code, filename);
        return execute(source, code.getEncoding(), TranslatorDriver.ParserContext.EVAL, binding.getSelf(), binding.getFrame(), ownScopeForAssignments, currentNode, NodeWrapper.IDENTITY);
    }

    public Object eval(ByteList code, RubyBinding binding, boolean ownScopeForAssignments, Node currentNode) {
        return eval(code, binding, ownScopeForAssignments, "(eval)", currentNode);
    }

    public Object execute(Source source, Encoding defaultEncoding, TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, Node currentNode, NodeWrapper wrapper) {
        return execute(source, defaultEncoding, parserContext, self, parentFrame, true, currentNode, wrapper);
    }

    @TruffleBoundary
    public Object execute(Source source, Encoding defaultEncoding, TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, boolean ownScopeForAssignments, Node currentNode, NodeWrapper wrapper) {
        final RubyRootNode rootNode = translator.parse(this, source, defaultEncoding, parserContext, parentFrame, ownScopeForAssignments, currentNode, wrapper);
        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

        final InternalMethod method = new InternalMethod(rootNode.getSharedMethodInfo(), rootNode.getSharedMethodInfo().getName(),
                getCoreLibrary().getObjectClass(), Visibility.PUBLIC, false, callTarget, parentFrame);

        return callTarget.call(RubyArguments.pack(method, parentFrame, self, null, new Object[]{}));
    }

    public long getNextObjectID() {
        // TODO(CS): We can theoretically run out of long values

        final long id = nextObjectID.getAndAdd(2);

        if (id < 0) {
            nextObjectID.set(Long.MIN_VALUE);
            throw new RuntimeException("Object IDs exhausted");
        }

        return id;
    }

    public void shutdown() {
        atExitManager.run();

        if (instrumentationServerManager != null) {
            instrumentationServerManager.shutdown();
        }

        fiberManager.shutdown();

        threadManager.shutdown();
    }

    public RubyString makeString(String string) {
        return RubyString.fromJavaString(coreLibrary.getStringClass(), string);
    }

    public RubyString makeString(String string, Encoding encoding) {
        return RubyString.fromJavaString(coreLibrary.getStringClass(), string, encoding);
    }

    public RubyString makeString(char string) {
        return makeString(Character.toString(string));
    }

    public RubyString makeString(char string, Encoding encoding) {
        return makeString(Character.toString(string), encoding);
    }

    public RubyString makeString(ByteList bytes) {
        return RubyString.fromByteList(coreLibrary.getStringClass(), bytes);
    }

    public IRubyObject toJRuby(Object object) {
        RubyNode.notDesignedForCompilation();

        if (object instanceof RubyNilClass) {
            return runtime.getNil();
        } else if (object == getCoreLibrary().getKernelModule()) {
            return runtime.getKernel();
        } else if (object == getCoreLibrary().getMainObject()) {
            return runtime.getTopSelf();
        } else if (object instanceof Boolean) {
            return runtime.newBoolean((boolean) object);
        } else if (object instanceof Integer) {
            return runtime.newFixnum((int) object);
        } else if (object instanceof Long) {
            return runtime.newFixnum((long) object);
        } else if (object instanceof Double) {
            return runtime.newFloat((double) object);
        } else if (object instanceof RubyString) {
            return toJRuby((RubyString) object);
        } else if (object instanceof RubyArray) {
            return toJRuby((RubyArray) object);
        } else {
            throw getRuntime().newRuntimeError("cannot pass " + object + " to JRuby");
        }
    }

    public org.jruby.RubyArray toJRuby(RubyArray array) {
        RubyNode.notDesignedForCompilation();

        final Object[] objects = array.slowToArray();
        final IRubyObject[] store = new IRubyObject[objects.length];

        for (int n = 0; n < objects.length; n++) {
            store[n] = toJRuby(objects[n]);
        }

        return runtime.newArray(store);
    }

    public org.jruby.RubyString toJRuby(RubyString string) {
        final org.jruby.RubyString jrubyString = runtime.newString(string.getBytes().dup());

        final Object tainted = string.getOperations().getInstanceVariable(string, RubyBasicObject.TAINTED_IDENTIFIER);

        if (tainted instanceof Boolean && (boolean) tainted) {
            jrubyString.setTaint(true);
        }

        return jrubyString;
    }

    public Object toTruffle(IRubyObject object) {
        RubyNode.notDesignedForCompilation();

        if (object == runtime.getTopSelf()) {
            return getCoreLibrary().getMainObject();
        } else if (object == runtime.getKernel()) {
            return getCoreLibrary().getKernelModule();
        } else if (object instanceof RubyNil) {
            return getCoreLibrary().getNilObject();
        } else if (object instanceof org.jruby.RubyBoolean.True) {
            return true;
        } else if (object instanceof org.jruby.RubyBoolean.False) {
            return false;
        } else if (object instanceof org.jruby.RubyFixnum) {
            final long value = ((org.jruby.RubyFixnum) object).getLongValue();

            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                throw new UnsupportedOperationException();
            }

            return (int) value;
        } else if (object instanceof org.jruby.RubyFloat) {
            return ((org.jruby.RubyFloat) object).getDoubleValue();
        } else if (object instanceof org.jruby.RubyString) {
            return toTruffle((org.jruby.RubyString) object);
        } else if (object instanceof org.jruby.RubySymbol) {
            return getSymbolTable().getSymbol(object.toString());
        } else if (object instanceof org.jruby.RubyArray) {
            final org.jruby.RubyArray jrubyArray = (org.jruby.RubyArray) object;

            final Object[] truffleArray = new Object[jrubyArray.size()];

            for (int n = 0; n < truffleArray.length; n++) {
                truffleArray[n] = toTruffle((IRubyObject) jrubyArray.get(n));
            }

            return new RubyArray(coreLibrary.getArrayClass(), truffleArray, truffleArray.length);
        } else if (object instanceof org.jruby.RubyException) {
            return toTruffle((org.jruby.RubyException) object, null);
        } else {
            throw object.getRuntime().newRuntimeError("cannot pass " + object.inspect() + " to Truffle");
        }
    }

    public RubyString toTruffle(org.jruby.RubyString jrubyString) {
        final RubyString truffleString = new RubyString(getCoreLibrary().getStringClass(), jrubyString.getByteList().dup());

        if (jrubyString.isTaint()) {
            truffleString.getOperations().setInstanceVariable(truffleString, RubyBasicObject.TAINTED_IDENTIFIER, true);
        }

        return truffleString;
    }

    public RubyException toTruffle(org.jruby.RubyException jrubyException, RubyNode currentNode) {
        switch (jrubyException.getMetaClass().getName()) {
            case "ArgumentError":
                return getCoreLibrary().argumentError(jrubyException.getMessage().toString(), currentNode);
            case "TypeError":
                return getCoreLibrary().typeError(jrubyException.getMessage().toString(), currentNode);
        }

        throw new UnsupportedOperationException("Don't know how to translate " + jrubyException.getMetaClass().getName());
    }

    public Ruby getRuntime() {
        return runtime;
    }

    public CoreLibrary getCoreLibrary() {
        return coreLibrary;
    }

    public FeatureManager getFeatureManager() {
        return featureManager;
    }

    public ObjectSpaceManager getObjectSpaceManager() {
        return objectSpaceManager;
    }

    public FiberManager getFiberManager() {
        return fiberManager;
    }

    public ThreadManager getThreadManager() {
        return threadManager;
    }

    public TranslatorDriver getTranslator() {
        return translator;
    }

    public AtExitManager getAtExitManager() {
        return atExitManager;
    }

    @Override
    public String getLanguageShortName() {
        return "ruby";
    }

    public TruffleHooks getHooks() {
        return (TruffleHooks) runtime.getInstanceConfig().getTruffleHooks();
    }

    public TraceManager getTraceManager() {
        return traceManager;
    }

    public Warnings getWarnings() {
        return warnings;
    }

    public SafepointManager getSafepointManager() {
        return safepointManager;
    }

    public Random getRandom() {
        return random;
    }

    public LexicalScope getRootLexicalScope() {
        return rootLexicalScope;
    }

    public CompilerOptions getCompilerOptions() {
        return compilerOptions;
    }

    public RubiniusPrimitiveManager getRubiniusPrimitiveManager() {
        return rubiniusPrimitiveManager;
    }

    public CoverageTracker getCoverageTracker() {
        return coverageTracker;
    }

    public AttachmentsManager getAttachmentsManager() {
        return attachmentsManager;
    }

    public SourceManager getSourceManager() {
        return sourceManager;
    }
}
