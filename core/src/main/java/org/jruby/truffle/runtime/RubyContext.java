/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.*;

import com.oracle.truffle.api.instrument.SourceCallback;
import org.jruby.Ruby;
import org.jruby.*;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.TruffleHooks;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.debug.RubyASTProber;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBinding;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.subsystems.*;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.truffle.translator.TranslatorDriver;
import org.jruby.util.ByteList;

/**
 * The global state of a running Ruby system.
 */
public class RubyContext extends ExecutionContext {

    private final Ruby runtime;
    private final TranslatorDriver translator;
    private final RubyASTProber astProber;
    private final CoreLibrary coreLibrary;
    private final FeatureManager featureManager;
    private final TraceManager traceManager;
    private final ObjectSpaceManager objectSpaceManager;
    private final ThreadManager threadManager;
    private final FiberManager fiberManager;
    private final AtExitManager atExitManager;
    private final RubySymbol.SymbolTable symbolTable = new RubySymbol.SymbolTable(this);
    private final Warnings warnings;
    private final SafepointManager safepointManager;
    private final Random random = new Random();
    private final LexicalScope rootLexicalScope;

    private SourceCallback sourceCallback = null;

    private final AtomicLong nextObjectID = new AtomicLong(ObjectIDOperations.FIRST_OBJECT_ID);

    private final ThreadLocal<Queue<Object>> throwTags = new ThreadLocal<Queue<Object>>() {

        @Override
        protected Queue<Object> initialValue() {
            return new ArrayDeque<>();
        }

    };

    public RubyContext(Ruby runtime) {
        assert runtime != null;

        safepointManager = new SafepointManager(this);

        this.runtime = runtime;
        translator = new TranslatorDriver(this);
        astProber = new RubyASTProber();

        warnings = new Warnings(this);

        // Object space manager needs to come early before we create any objects
        objectSpaceManager = new ObjectSpaceManager(this);

        // See note in CoreLibrary#initialize to see why we need to break this into two statements
        coreLibrary = new CoreLibrary(this);
        coreLibrary.initialize();

        featureManager = new FeatureManager(this);
        traceManager = new TraceManager();
        atExitManager = new AtExitManager();

        // Must initialize threads before fibers

        threadManager = new ThreadManager(this);
        fiberManager = new FiberManager(this);

        rootLexicalScope = new LexicalScope(null, coreLibrary.getObjectClass());
    }

    public static String checkInstanceVariableName(RubyContext context, String name, RubyNode currentNode) {
        RubyNode.notDesignedForCompilation();

        if (!name.startsWith("@")) {
            throw new RaiseException(context.getCoreLibrary().nameErrorInstanceNameNotAllowable(name, currentNode));
        }

        return name;
    }

    public static String checkClassVariableName(RubyContext context, String name, RubyNode currentNode) {
        RubyNode.notDesignedForCompilation();

        if (!name.startsWith("@@")) {
            throw new RaiseException(context.getCoreLibrary().nameErrorInstanceNameNotAllowable(name, currentNode));
        }

        return name;
    }

    public void load(Source source, RubyNode currentNode) {
        execute(this, source, TranslatorDriver.ParserContext.TOP_LEVEL, coreLibrary.getMainObject(), null, currentNode);
    }

    public void loadFile(String fileName, RubyNode currentNode) {
        if (new File(fileName).isAbsolute()) {
            loadFileAbsolute(fileName, currentNode);
        } else {
            loadFileAbsolute(this.getRuntime().getCurrentDirectory() + File.separator + fileName, currentNode);
        }
    }

    private void loadFileAbsolute(String fileName, RubyNode currentNode) {
        final byte[] bytes;

        try {
            bytes = Files.readAllBytes(Paths.get(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Assume UTF-8 for the moment

        final Source source = Source.fromBytes(bytes, fileName, new BytesDecoder.UTF8BytesDecoder());

        coreLibrary.getLoadedFeatures().slowPush(makeString(fileName));
        execute(this, source, TranslatorDriver.ParserContext.TOP_LEVEL, coreLibrary.getMainObject(), null, currentNode);
    }

    public RubySymbol.SymbolTable getSymbolTable() {
        return symbolTable;
    }

    @CompilerDirectives.SlowPath
    public RubySymbol newSymbol(String name) {
        return symbolTable.getSymbol(name);
    }

    @CompilerDirectives.SlowPath
    public RubySymbol newSymbol(ByteList name) {
        return symbolTable.getSymbol(name);
    }

    public Object eval(String code, RubyNode currentNode) {
        final Source source = Source.fromText(code, "(eval)");
        return execute(this, source, TranslatorDriver.ParserContext.TOP_LEVEL, coreLibrary.getMainObject(), null, currentNode);
    }

    public Object eval(String code, Object self, RubyNode currentNode) {
        final Source source = Source.fromText(code, "(eval)");
        return execute(this, source, TranslatorDriver.ParserContext.TOP_LEVEL, self, null, currentNode);
    }

    public Object eval(String code, RubyBinding binding, RubyNode currentNode) {
        final Source source = Source.fromText(code, "(eval)");
        return execute(this, source, TranslatorDriver.ParserContext.TOP_LEVEL, binding.getSelf(), binding.getFrame(), currentNode);
    }

    public Object execute(RubyContext context, Source source, TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, RubyNode currentNode) {
        return execute(context, source, parserContext, self, parentFrame, currentNode, NodeWrapper.IDENTITY);
    }

    public Object execute(RubyContext context, Source source, TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, RubyNode currentNode, NodeWrapper wrapper) {
        final RubyRootNode rootNode = translator.parse(context, source, parserContext, parentFrame, currentNode, wrapper);
        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

        // TODO(CS): we really need a method here - it's causing problems elsewhere
        return callTarget.call(RubyArguments.pack(null, parentFrame, self, null, new Object[]{}));
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

        threadManager.leaveGlobalLock();

        objectSpaceManager.shutdown();

        if (fiberManager != null) {
            fiberManager.shutdown();
        }
    }

    public RubyString makeString(String string) {
        return RubyString.fromJavaString(coreLibrary.getStringClass(), string);
    }

    public RubyString makeString(char string) {
        return makeString(Character.toString(string));
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
        return runtime.newString(string.getBytes());
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
        } else {
            throw object.getRuntime().newRuntimeError("cannot pass " + object.inspect() + " to Truffle");
        }
    }

    public RubyString toTruffle(org.jruby.RubyString string) {
        return new RubyString(getCoreLibrary().getStringClass(), string.getByteList());
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

    @Override
    public void setSourceCallback(SourceCallback sourceCallback) {
        this.sourceCallback = sourceCallback;
    }

    public SourceCallback getSourceCallback() {
        return sourceCallback;
    }

    public TruffleHooks getHooks() {
        return (TruffleHooks) runtime.getInstanceConfig().getTruffleHooks();
    }

    public RubyASTProber getASTProber() {
        return astProber;
    }

    public TraceManager getTraceManager() {
        return traceManager;
    }

    public Warnings getWarnings() {
        return warnings;
    }

    public Queue<Object> getThrowTags() {
        return throwTags.get();
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
}
