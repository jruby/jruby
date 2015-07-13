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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerOptions;
import com.oracle.truffle.api.ExecutionContext;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.tools.CoverageTracker;
import jnr.ffi.LibraryLoader;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyNil;
import org.jruby.TruffleContextInterface;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Platform.OS_TYPE;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.exceptions.TopLevelRaiseHandler;
import org.jruby.truffle.nodes.instrument.RubyDefaultASTProber;
import org.jruby.truffle.nodes.methods.SetMethodDeclarationContext;
import org.jruby.truffle.nodes.rubinius.RubiniusPrimitiveManager;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.object.ObjectIDOperations;
import org.jruby.truffle.runtime.rubinius.RubiniusConfiguration;
import org.jruby.truffle.runtime.sockets.NativeSockets;
import org.jruby.truffle.runtime.subsystems.*;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.truffle.translator.TranslatorDriver;
import org.jruby.util.ByteList;
import org.jruby.util.cli.Options;

import java.io.File;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The global state of a running Ruby system.
 */
public class RubyContext extends ExecutionContext implements TruffleContextInterface {

    private static volatile RubyContext latestInstance;

    private static final boolean TRUFFLE_COVERAGE = Options.TRUFFLE_COVERAGE.load();
    private static final int INSTRUMENTATION_SERVER_PORT = Options.TRUFFLE_INSTRUMENTATION_SERVER_PORT.load();

    private final Ruby runtime;

    private final POSIX posix;
    private final NativeSockets nativeSockets;

    private final CoreLibrary coreLibrary;
    private final FeatureManager featureManager;
    private final TraceManager traceManager;
    private final ObjectSpaceManager objectSpaceManager;
    private final ThreadManager threadManager;
    private final AtExitManager atExitManager;
    private final SymbolTable symbolTable = new SymbolTable(this);
    private final Warnings warnings;
    private final SafepointManager safepointManager;
    private final LexicalScope rootLexicalScope;
    private final CompilerOptions compilerOptions;
    private final RubiniusPrimitiveManager rubiniusPrimitiveManager;
    private final CoverageTracker coverageTracker;
    private final InstrumentationServerManager instrumentationServerManager;
    private final AttachmentsManager attachmentsManager;
    private final SourceManager sourceManager;
    private final RubiniusConfiguration rubiniusConfiguration;

    private final AtomicLong nextObjectID = new AtomicLong(ObjectIDOperations.FIRST_OBJECT_ID);

    private final boolean runningOnWindows;

    private final PrintStream debugStandardOut;

    public RubyContext(Ruby runtime) {
        latestInstance = this;

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

        // JRuby+Truffle uses POSIX for all IO - we need the native version
        posix = POSIXFactory.getNativePOSIX(new TrufflePOSIXHandler(this));

        final LibraryLoader<NativeSockets> loader = LibraryLoader.create(NativeSockets.class);
        loader.library("c");
        nativeSockets = loader.load();

        warnings = new Warnings(this);

        // Object space manager needs to come early before we create any objects
        objectSpaceManager = new ObjectSpaceManager(this);

        coreLibrary = new CoreLibrary(this);
        rootLexicalScope = new LexicalScope(null, coreLibrary.getObjectClass());
        coreLibrary.initialize();

        featureManager = new FeatureManager(this);
        traceManager = new TraceManager();
        atExitManager = new AtExitManager(this);

        threadManager = new ThreadManager(this);
        threadManager.initialize();

        rubiniusPrimitiveManager = new RubiniusPrimitiveManager();
        rubiniusPrimitiveManager.addAnnotatedPrimitives();

        if (INSTRUMENTATION_SERVER_PORT != 0) {
            instrumentationServerManager = new InstrumentationServerManager(this, INSTRUMENTATION_SERVER_PORT);
            instrumentationServerManager.start();
        } else {
            instrumentationServerManager = null;
        }

        runningOnWindows = Platform.getPlatform().getOS() == OS_TYPE.WINDOWS;

        attachmentsManager = new AttachmentsManager(this);
        sourceManager = new SourceManager(this);
        rubiniusConfiguration = RubiniusConfiguration.create(this);

        final PrintStream configStandardOut = runtime.getInstanceConfig().getOutput();
        debugStandardOut = (configStandardOut == System.out) ? null : configStandardOut;
    }

    @Override
    public void initialize() {
        // Give the core library manager a chance to tweak some of those methods

        coreLibrary.initializeAfterMethodsAdded();

        // Set program arguments

        for (IRubyObject arg : ((org.jruby.RubyArray) runtime.getObject().getConstant("ARGV")).toJavaArray()) {
            assert arg != null;

            ArrayNodes.slowPush(coreLibrary.getArgv(), StringNodes.createString(coreLibrary.getStringClass(), arg.toString()));
        }

        // Set the load path

        final RubyBasicObject loadPath = (RubyBasicObject) coreLibrary.getGlobalVariablesObject().getInstanceVariable("$:");

        final String home = runtime.getInstanceConfig().getJRubyHome();

        // We don't want JRuby's stdlib paths, but we do want any extra paths set by -I and things like that

        final List<String> excludedLibPaths = new ArrayList<>();
        excludedLibPaths.add(new File(home, "lib/ruby/2.2/site_ruby").toString().replace('\\', '/'));
        excludedLibPaths.add(new File(home, "lib/ruby/shared").toString().replace('\\', '/'));
        excludedLibPaths.add(new File(home, "lib/ruby/stdlib").toString().replace('\\', '/'));

        for (IRubyObject path : ((org.jruby.RubyArray) runtime.getLoadService().getLoadPath()).toJavaArray()) {
            if (!excludedLibPaths.contains(path.toString())) {
                ArrayNodes.slowPush(loadPath, StringNodes.createString(coreLibrary.getStringClass(), new File(path.toString()).getAbsolutePath()));
            }
        }

        // Load our own stdlib path

        // Libraries copied unmodified from MRI
        ArrayNodes.slowPush(loadPath, StringNodes.createString(coreLibrary.getStringClass(), new File(home, "lib/ruby/truffle/mri").toString()));

        // Our own implementations
        ArrayNodes.slowPush(loadPath, StringNodes.createString(coreLibrary.getStringClass(), new File(home, "lib/ruby/truffle/truffle").toString()));

        // Libraries from RubySL
        for (String lib : Arrays.asList("rubysl-strscan", "rubysl-stringio",
                "rubysl-complex", "rubysl-date", "rubysl-pathname",
                "rubysl-tempfile", "rubysl-socket", "rubysl-securerandom",
                "rubysl-timeout", "rubysl-webrick")) {
            ArrayNodes.slowPush(loadPath, StringNodes.createString(coreLibrary.getStringClass(), new File(home, "lib/ruby/truffle/rubysl/" + lib + "/lib").toString()));
        }

        // Shims
        ArrayNodes.slowPush(loadPath, StringNodes.createString(coreLibrary.getStringClass(), new File(home, "lib/ruby/truffle/shims").toString()));
    }

    public static String checkInstanceVariableName(RubyContext context, String name, Node currentNode) {
        if (!name.startsWith("@")) {
            throw new RaiseException(context.getCoreLibrary().nameErrorInstanceNameNotAllowable(name, currentNode));
        }

        return name;
    }

    public static String checkClassVariableName(RubyContext context, String name, Node currentNode) {
        if (!name.startsWith("@@")) {
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

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public RubyBasicObject getSymbol(String name) {
        return symbolTable.getSymbol(name);
    }

    public RubyBasicObject getSymbol(ByteList name) {
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

    public Object eval(Source source) {
        return execute(source, UTF8Encoding.INSTANCE, TranslatorDriver.ParserContext.EVAL, getCoreLibrary().getMainObject(), null, null, new NodeWrapper() {
            @Override
            public RubyNode wrap(RubyNode node) {
                return new SetMethodDeclarationContext(node.getContext(), node.getSourceSection(), Visibility.PRIVATE, "simple eval", node);
            }
        });
    }

    @TruffleBoundary
    public Object eval(String code, RubyBasicObject binding, boolean ownScopeForAssignments, String filename, Node currentNode) {
        assert RubyGuards.isRubyBinding(binding);
        return eval(ByteList.create(code), binding, ownScopeForAssignments, filename, currentNode);
    }

    @TruffleBoundary
    public Object eval(ByteList code, RubyBasicObject binding, boolean ownScopeForAssignments, String filename, Node currentNode) {
        assert RubyGuards.isRubyBinding(binding);
        final Source source = Source.fromText(code, filename);
        return execute(source, code.getEncoding(), TranslatorDriver.ParserContext.EVAL, BindingNodes.getSelf(binding), BindingNodes.getFrame(binding), ownScopeForAssignments, currentNode, NodeWrapper.IDENTITY);
    }

    @TruffleBoundary
    public Object eval(ByteList code, RubyBasicObject binding, boolean ownScopeForAssignments, Node currentNode) {
        assert RubyGuards.isRubyBinding(binding);
        return eval(code, binding, ownScopeForAssignments, "(eval)", currentNode);
    }

    @TruffleBoundary
    public Object execute(Source source, Encoding defaultEncoding, TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, Node currentNode, NodeWrapper wrapper) {
        return execute(source, defaultEncoding, parserContext, self, parentFrame, true, currentNode, wrapper);
    }

    @TruffleBoundary
    public Object execute(Source source, Encoding defaultEncoding, TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, boolean ownScopeForAssignments, Node currentNode, NodeWrapper wrapper) {
        final TranslatorDriver translator = new TranslatorDriver(this);
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

    public void innerShutdown(boolean normalExit) {
        atExitManager.run(normalExit);

        if (instrumentationServerManager != null) {
            instrumentationServerManager.shutdown();
        }

        threadManager.shutdown();
    }

    public Object makeTuple(VirtualFrame frame, CallDispatchHeadNode newTupleNode, Object... values) {
        return newTupleNode.call(frame, getCoreLibrary().getTupleClass(), "create", null, values);
    }

    public IRubyObject toJRuby(Object object) {
        if (object == getCoreLibrary().getNilObject()) {
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
        } else if (RubyGuards.isRubyString(object)) {
            return toJRubyString((RubyBasicObject) object);
        } else if (RubyGuards.isRubyArray(object)) {
            return toJRubyArray((RubyBasicObject) object);
        } else if (object instanceof RubyEncoding) {
            return toJRuby((RubyEncoding) object);
        } else {
            throw getRuntime().newRuntimeError("cannot pass " + object + " (" + object.getClass().getName()  + ") to JRuby");
        }
    }

    public IRubyObject[] toJRuby(Object... objects) {
        final IRubyObject[] store = new IRubyObject[objects.length];

        for (int n = 0; n < objects.length; n++) {
            store[n] = toJRuby(objects[n]);
        }

        return store;
    }

    public org.jruby.RubyArray toJRubyArray(RubyBasicObject array) {
        assert RubyGuards.isRubyArray(array);
        return runtime.newArray(toJRuby(ArrayNodes.slowToArray(array)));
    }

    public IRubyObject toJRuby(RubyEncoding encoding) {
        return runtime.getEncodingService().rubyEncodingFromObject(runtime.newString(encoding.getName()));
    }

    public org.jruby.RubyString toJRubyString(RubyBasicObject string) {
        assert RubyGuards.isRubyString(string);

        final org.jruby.RubyString jrubyString = runtime.newString(StringNodes.getByteList(string).dup());

        final Object tainted = RubyBasicObject.getInstanceVariable(string, RubyBasicObject.TAINTED_IDENTIFIER);

        if (tainted instanceof Boolean && (boolean) tainted) {
            jrubyString.setTaint(true);
        }

        return jrubyString;
    }

    public Object toTruffle(IRubyObject object) {
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
                return value;
            }

            return (int) value;
        } else if (object instanceof org.jruby.RubyFloat) {
            return ((org.jruby.RubyFloat) object).getDoubleValue();
        } else if (object instanceof org.jruby.RubyBignum) {
            final BigInteger value = ((org.jruby.RubyBignum) object).getBigIntegerValue();

            return BignumNodes.createRubyBignum(coreLibrary.getBignumClass(), value);
        } else if (object instanceof org.jruby.RubyString) {
            return toTruffle((org.jruby.RubyString) object);
        } else if (object instanceof org.jruby.RubySymbol) {
            return getSymbolTable().getSymbol(object.toString());
        } else if (object instanceof org.jruby.RubyArray) {
            return toTruffle((org.jruby.RubyArray) object);
        } else if (object instanceof org.jruby.RubyException) {
            return toTruffle((org.jruby.RubyException) object, null);
        } else {
            throw object.getRuntime().newRuntimeError("cannot pass " + object.inspect() + " (" + object.getClass().getName()  + ") to Truffle");
        }
    }

    public RubyBasicObject toTruffle(org.jruby.RubyArray array) {
        final Object[] store = new Object[array.size()];

        for (int n = 0; n < store.length; n++) {
            store[n] = toTruffle(array.entry(n));
        }

        return ArrayNodes.fromObjects(coreLibrary.getArrayClass(), store);
    }

    public RubyBasicObject toTruffle(org.jruby.RubyString jrubyString) {
        final RubyBasicObject truffleString = StringNodes.createString(getCoreLibrary().getStringClass(), jrubyString.getByteList().dup());

        if (jrubyString.isTaint()) {
            RubyBasicObject.setInstanceVariable(truffleString, RubyBasicObject.TAINTED_IDENTIFIER, true);
        }

        return truffleString;
    }

    public RubyException toTruffle(org.jruby.RubyException jrubyException, RubyNode currentNode) {
        switch (jrubyException.getMetaClass().getName()) {
            case "ArgumentError":
                return getCoreLibrary().argumentError(jrubyException.getMessage().toString(), currentNode);
            case "Encoding::CompatibilityError":
                return getCoreLibrary().encodingCompatibilityError(jrubyException.getMessage().toString(), currentNode);
            case "TypeError":
                return getCoreLibrary().typeError(jrubyException.getMessage().toString(), currentNode);
            case "RegexpError":
                return getCoreLibrary().regexpError(jrubyException.getMessage().toString(), currentNode);
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

    public ThreadManager getThreadManager() {
        return threadManager;
    }

    public AtExitManager getAtExitManager() {
        return atExitManager;
    }

    @Override
    public String getLanguageShortName() {
        return "ruby";
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
        return ThreadLocalRandom.current();
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

    // TODO(mg): we need to find a better place for this:
    private TruffleObject multilanguageObject;

    public TruffleObject getMultilanguageObject() {
        return multilanguageObject;
    }

    public void setMultilanguageObject(TruffleObject multilanguageObject) {
        this.multilanguageObject = multilanguageObject;
    }

    public CoverageTracker getCoverageTracker() {
        return coverageTracker;
    }

    public static RubyContext getLatestInstance() {
        return latestInstance;
    }

    public AttachmentsManager getAttachmentsManager() {
        return attachmentsManager;
    }

    public SourceManager getSourceManager() {
        return sourceManager;
    }

    public RubiniusConfiguration getRubiniusConfiguration() {
        return rubiniusConfiguration;
    }

    public POSIX getPosix() {
        return posix;
    }

    public NativeSockets getNativeSockets() {
        return nativeSockets;
    }

    @Override
    public Object execute(final org.jruby.ast.RootNode rootNode) {
        RubyBasicObject.setInstanceVariable(
                coreLibrary.getGlobalVariablesObject(), "$0",
                toTruffle(runtime.getGlobalVariables().get("$0")));

        final String inputFile = rootNode.getPosition().getFile();
        final Source source;

        if (inputFile.equals("-e")) {
            // Assume UTF-8 for the moment
            source = Source.fromText(new String(runtime.getInstanceConfig().inlineScript(), StandardCharsets.UTF_8), "-e");
        } else {
            source = sourceManager.forFile(inputFile);
        }

        featureManager.setMainScriptSource(source);

        load(source, null, new NodeWrapper() {
            @Override
            public RubyNode wrap(RubyNode node) {
                RubyContext context = node.getContext();
                SourceSection sourceSection = node.getSourceSection();
                return new TopLevelRaiseHandler(context, sourceSection,
                        SequenceNode.sequence(context, sourceSection,
                                new SetTopLevelBindingNode(context, sourceSection),
                                new LoadRequiredLibrariesNode(context, sourceSection),
                                node));
            }
        });
        return coreLibrary.getNilObject();
    }

    @Override
    public void shutdown() {
        innerShutdown(true);
    }

    public PrintStream getDebugStandardOut() {
        return debugStandardOut;
    }

}
