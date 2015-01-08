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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.BytesDecoder;
import com.oracle.truffle.api.source.Source;

import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyNil;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.TruffleHooks;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.methods.SetMethodDeclarationContext;
import org.jruby.truffle.nodes.rubinius.RubiniusPrimitiveManager;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.subsystems.*;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.truffle.translator.TranslatorDriver;
import org.jruby.util.ByteList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The global state of a running Ruby system.
 */
public class RubyContext extends ExecutionContext {

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

    private final AtomicLong nextObjectID = new AtomicLong(ObjectIDOperations.FIRST_OBJECT_ID);

    private final ThreadLocal<Queue<Object>> throwTags = new ThreadLocal<Queue<Object>>() {

        @Override
        protected Queue<Object> initialValue() {
            return new ArrayDeque<>();
        }

    };

    public RubyContext(Ruby runtime) {
        assert runtime != null;

        compilerOptions = Truffle.getRuntime().createCompilerOptions();

        if (compilerOptions.supportsOption("MinTimeThreshold")) {
            compilerOptions.setOption("MinTimeThreshold", 100000000);
        }

        if (compilerOptions.supportsOption("MinInliningMaxCallerSize")) {
            compilerOptions.setOption("MinInliningMaxCallerSize", 5000);
        }

        safepointManager = new SafepointManager(this);

        this.runtime = runtime;
        translator = new TranslatorDriver();

        warnings = new Warnings(this);

        // Object space manager needs to come early before we create any objects
        objectSpaceManager = new ObjectSpaceManager(this);

        emptyShape = RubyBasicObject.LAYOUT.createShape(new RubyOperations(this));

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

        rubiniusPrimitiveManager = RubiniusPrimitiveManager.create();
    }

    public Shape getEmptyShape() {
        return emptyShape;
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

        load(source, currentNode, new NodeWrapper() {
            @Override
            public RubyNode wrap(RubyNode node) {
                return new SetMethodDeclarationContext(node.getContext(), node.getSourceSection(), "load", node);
            }
        });
    }

    public void load(Source source, RubyNode currentNode, NodeWrapper nodeWrapper) {
        execute(this, source, UTF8Encoding.INSTANCE, TranslatorDriver.ParserContext.TOP_LEVEL, coreLibrary.getMainObject(), null, currentNode, nodeWrapper);
    }

    public RubySymbol.SymbolTable getSymbolTable() {
        return symbolTable;
    }

    @CompilerDirectives.TruffleBoundary
    public RubySymbol newSymbol(String name) {
        return symbolTable.getSymbol(name);
    }

    @CompilerDirectives.TruffleBoundary
    public RubySymbol newSymbol(ByteList name) {
        return symbolTable.getSymbol(name);
    }

    public Object eval(ByteList code, RubyNode currentNode) {
        final Source source = Source.fromText(code, "(eval)");
        return execute(this, source, code.getEncoding(), TranslatorDriver.ParserContext.TOP_LEVEL, coreLibrary.getMainObject(), null, currentNode, NodeWrapper.IDENTITY);
    }

    public Object eval(ByteList code, Object self, RubyNode currentNode) {
        final Source source = Source.fromText(code, "(eval)");
        return execute(this, source, code.getEncoding(), TranslatorDriver.ParserContext.TOP_LEVEL, self, null, currentNode, NodeWrapper.IDENTITY);
    }

    public Object eval(ByteList code, RubyBinding binding, RubyNode currentNode) {
        final Source source = Source.fromText(code, "(eval)");
        return execute(this, source, code.getEncoding(), TranslatorDriver.ParserContext.TOP_LEVEL, binding.getSelf(), binding.getFrame(), currentNode, NodeWrapper.IDENTITY);
    }

    public Object execute(RubyContext context, Source source, Encoding defaultEncoding, TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, RubyNode currentNode, NodeWrapper wrapper) {
        final RubyRootNode rootNode = translator.parse(context, source, defaultEncoding, parserContext, parentFrame, currentNode, wrapper);
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
        return runtime.newString(string.getBytes().dup());
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
        return new RubyString(getCoreLibrary().getStringClass(), string.getByteList().dup());
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

    public CompilerOptions getCompilerOptions() {
        return compilerOptions;
    }

    public RubiniusPrimitiveManager getRubiniusPrimitiveManager() {
        return rubiniusPrimitiveManager;
    }

}
