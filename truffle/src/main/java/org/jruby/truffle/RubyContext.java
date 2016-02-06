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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.tools.CoverageTracker;
import jnr.ffi.LibraryLoader;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Platform.OS_TYPE;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.core.*;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.kernel.AtExitManager;
import org.jruby.truffle.core.kernel.TraceManager;
import org.jruby.truffle.core.objectspace.ObjectSpaceManager;
import org.jruby.truffle.core.rope.RopeTable;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.symbol.SymbolTable;
import org.jruby.truffle.core.thread.ThreadManager;
import org.jruby.truffle.extra.AttachmentsManager;
import org.jruby.truffle.language.*;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.platform.TrufflePOSIXHandler;
import org.jruby.truffle.platform.posix.TruffleJavaPOSIX;
import org.jruby.truffle.tools.InstrumentationServerManager;
import org.jruby.truffle.tools.callgraph.CallGraph;
import org.jruby.truffle.tools.callgraph.SimpleWriter;
import org.jruby.truffle.language.control.SequenceNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.exceptions.TopLevelRaiseHandler;
import org.jruby.truffle.instrument.RubyDefaultASTProber;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.core.rubinius.RubiniusPrimitiveManager;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.core.ffi.LibCClockGetTime;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.loader.FeatureLoader;
import org.jruby.truffle.language.loader.SourceCache;
import org.jruby.truffle.language.loader.SourceLoader;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.ObjectIDOperations;
import org.jruby.truffle.platform.darwin.CrtExterns;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.platform.RubiniusConfiguration;
import org.jruby.truffle.stdlib.sockets.NativeSockets;
import org.jruby.truffle.language.translator.TranslatorDriver;
import org.jruby.truffle.language.translator.TranslatorDriver.ParserContext;
import org.jruby.util.ByteList;
import org.jruby.util.IdUtil;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The global state of a running Ruby system.
 */
public class RubyContext extends ExecutionContext {

    private static volatile RubyContext latestInstance;

    private final Ruby runtime;

    private final Options options;

    private final POSIX posix;
    private final NativeSockets nativeSockets;
    private final LibCClockGetTime libCClockGetTime;
    private CrtExterns crtExterns;

    private final CoreLibrary coreLibrary;
    private final FeatureLoader featureLoader;
    private final TraceManager traceManager;
    private final ObjectSpaceManager objectSpaceManager;
    private final ThreadManager threadManager;
    private final AtExitManager atExitManager;
    private final RopeTable ropeTable = new RopeTable();
    private final SymbolTable symbolTable = new SymbolTable(this);
    private final Warnings warnings;
    private final SafepointManager safepointManager;
    private final LexicalScope rootLexicalScope;
    private final CompilerOptions compilerOptions;
    private final RubiniusPrimitiveManager rubiniusPrimitiveManager;
    private final CoverageTracker coverageTracker;
    private final InstrumentationServerManager instrumentationServerManager;
    private final AttachmentsManager attachmentsManager;
    private final SourceCache sourceCache;
    private final RubiniusConfiguration rubiniusConfiguration;
    private final CallGraph callGraph;

    private final AtomicLong nextObjectID = new AtomicLong(ObjectIDOperations.FIRST_OBJECT_ID);

    private final boolean runningOnWindows;

    private final PrintStream debugStandardOut;

    private final Map<String, TruffleObject> exported = new HashMap<>();
    private final TruffleLanguage.Env env;

    private org.jruby.ast.RootNode initialJRubyRootNode;

    public RubyContext(Ruby runtime, TruffleLanguage.Env env) {
        options = new Options();

        if (options.CALL_GRAPH) {
            callGraph = new CallGraph();
        } else {
            callGraph = null;
        }

        latestInstance = this;

        assert runtime != null;
        this.env = env;

        compilerOptions = Truffle.getRuntime().createCompilerOptions();

        if (!onGraal() && options.GRAAL_WARNING_UNLESS) {
            System.err.println("WARNING: JRuby+Truffle is designed to be used with a JVM that has the Graal compiler. " +
                    "Without the Graal compiler, performance will be drastically reduced. " +
                    "See https://github.com/jruby/jruby/wiki/Truffle-FAQ#how-do-i-get-jrubytruffle");
        }

        if (compilerOptions.supportsOption("MinTimeThreshold")) {
            compilerOptions.setOption("MinTimeThreshold", 100000000);
        }

        if (compilerOptions.supportsOption("MinInliningMaxCallerSize")) {
            compilerOptions.setOption("MinInliningMaxCallerSize", 5000);
        }

        env.instrumenter().registerASTProber(new RubyDefaultASTProber(env.instrumenter()));

        // TODO(CS, 28-Jan-15) this is global
        // TODO(CS, 28-Jan-15) maybe not do this for core?
        if (options.COVERAGE || options.COVERAGE_GLOBAL) {
            coverageTracker = new CoverageTracker();

            if (options.COVERAGE_GLOBAL) {
                env.instrumenter().install(coverageTracker);
            }
        } else {
            coverageTracker = null;
        }

        safepointManager = new SafepointManager(this);

        this.runtime = runtime;

        if (options.POSIX_USE_JAVA) {
            posix = new TruffleJavaPOSIX(this, POSIXFactory.getJavaPOSIX(new TrufflePOSIXHandler(this)));
        } else {
            posix = POSIXFactory.getNativePOSIX(new TrufflePOSIXHandler(this));
        }

        nativeSockets = LibraryLoader.create(NativeSockets.class).library("c").load();

        try {
            crtExterns = LibraryLoader.create(CrtExterns.class).failImmediately().library("libSystem.B.dylib").load();
        } catch (UnsatisfiedLinkError e) {
            crtExterns = null;
        }

        if (Platform.getPlatform().getOS() == OS_TYPE.LINUX) {
            libCClockGetTime = LibraryLoader.create(LibCClockGetTime.class).library("c").load();
        } else {
            libCClockGetTime = null;
        }

        warnings = new Warnings(this);

        // Object space manager needs to come early before we create any objects
        objectSpaceManager = new ObjectSpaceManager(this);

        coreLibrary = new CoreLibrary(this);
        rootLexicalScope = new LexicalScope(null, coreLibrary.getObjectClass());
        coreLibrary.initialize();

        featureLoader = new FeatureLoader(this);
        traceManager = new TraceManager(this);
        atExitManager = new AtExitManager(this);

        threadManager = new ThreadManager(this);
        threadManager.initialize();

        rubiniusPrimitiveManager = new RubiniusPrimitiveManager();
        rubiniusPrimitiveManager.addAnnotatedPrimitives();

        if (options.INSTRUMENTATION_SERVER_PORT != 0) {
            instrumentationServerManager = new InstrumentationServerManager(this, options.INSTRUMENTATION_SERVER_PORT);
            instrumentationServerManager.start();
        } else {
            instrumentationServerManager = null;
        }

        runningOnWindows = Platform.getPlatform().getOS() == OS_TYPE.WINDOWS;

        attachmentsManager = new AttachmentsManager(this);
        sourceCache = new SourceCache(new SourceLoader(this));
        rubiniusConfiguration = RubiniusConfiguration.create(this);

        final PrintStream configStandardOut = runtime.getInstanceConfig().getOutput();
        debugStandardOut = (configStandardOut == System.out) ? null : configStandardOut;

        initialize();
    }

    public boolean onGraal() {
        return Truffle.getRuntime().getName().toLowerCase(Locale.ENGLISH).contains("graal");
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

    /* For debugging in Java. */
    public static Object debugEval(String code) {
        CompilerAsserts.neverPartOfCompilation();
        final FrameInstance currentFrameInstance = Truffle.getRuntime().getCurrentFrame();
        final Frame currentFrame = currentFrameInstance.getFrame(FrameAccess.MATERIALIZE, true);
        return getLatestInstance().inlineRubyHelper(null, currentFrame, code);
    }

    @TruffleBoundary
    public Object inlineRubyHelper(Node currentNode, String expression, Object... arguments) {
        return inlineRubyHelper(currentNode, Truffle.getRuntime().getCurrentFrame().getFrame(FrameAccess.MATERIALIZE, true), expression, arguments);
    }

    public Object inlineRubyHelper(Node currentNode, Frame frame, String expression, Object... arguments) {
        final MaterializedFrame evalFrame = setupInlineRubyFrame(frame, arguments);
        final DynamicObject binding = BindingNodes.createBinding(this, evalFrame);
        return eval(ParserContext.INLINE, StringOperations.createByteList(expression), binding, true, "inline-ruby", currentNode);
    }

    private MaterializedFrame setupInlineRubyFrame(Frame frame, Object... arguments) {
        CompilerDirectives.transferToInterpreter();
        final MaterializedFrame evalFrame = Truffle.getRuntime().createMaterializedFrame(
                RubyArguments.pack(null, null, RubyArguments.getMethod(frame.getArguments()), DeclarationContext.INSTANCE_EVAL, null, RubyArguments.getSelf(frame.getArguments()), null, new Object[]{}),
                new FrameDescriptor(frame.getFrameDescriptor().getDefaultValue()));

        if (arguments.length % 2 == 1) {
            throw new UnsupportedOperationException("odd number of name-value pairs for arguments");
        }

        for (int n = 0; n < arguments.length; n += 2) {
            evalFrame.setObject(evalFrame.getFrameDescriptor().findOrAddFrameSlot(arguments[n]), arguments[n + 1]);
        }

        return evalFrame;
    }

    private void initialize() {
        // Give the core library manager a chance to tweak some of those methods

        coreLibrary.initializeAfterMethodsAdded();

        // Set program arguments

        for (IRubyObject arg : ((org.jruby.RubyArray) runtime.getObject().getConstant("ARGV")).toJavaArray()) {
            assert arg != null;

            ArrayOperations.append(coreLibrary.getArgv(), StringOperations.createString(this, StringOperations.encodeRope(arg.toString(), UTF8Encoding.INSTANCE)));
        }

        // Set the load path

        DynamicObject receiver = coreLibrary.getGlobalVariablesObject();
        final DynamicObject loadPath = (DynamicObject) receiver.get("$:", coreLibrary.getNilObject());

        for (IRubyObject path : ((org.jruby.RubyArray) runtime.getLoadService().getLoadPath()).toJavaArray()) {
            String pathString = path.toString();

            if (!(pathString.endsWith("lib/ruby/2.2/site_ruby")
                    || pathString.endsWith("lib/ruby/shared")
                    || pathString.endsWith("lib/ruby/stdlib"))) {

                if (pathString.startsWith("uri:classloader:")) {
                    pathString = SourceLoader.JRUBY_SCHEME + pathString.substring("uri:classloader:".length());
                }

                ArrayOperations.append(loadPath, StringOperations.createString(this, StringOperations.encodeRope(pathString, UTF8Encoding.INSTANCE)));
            }
        }

        // Load our own stdlib path

        String home = runtime.getInstanceConfig().getJRubyHome();

        if (home.startsWith("uri:classloader:")) {
            home = home.substring("uri:classloader:".length());

            while (home.startsWith("/")) {
                home = home.substring(1);
            }

            home = SourceLoader.JRUBY_SCHEME + "/" + home;
        }

        home = home + "/";

        // Libraries copied unmodified from MRI
        ArrayOperations.append(loadPath, StringOperations.createString(this, StringOperations.encodeRope(home + "lib/ruby/truffle/mri", UTF8Encoding.INSTANCE)));

        // Our own implementations
        ArrayOperations.append(loadPath, StringOperations.createString(this, StringOperations.encodeRope(home + "lib/ruby/truffle/truffle", UTF8Encoding.INSTANCE)));

        // Libraries from RubySL
        for (String lib : Arrays.asList("rubysl-strscan", "rubysl-stringio",
                "rubysl-complex", "rubysl-date", "rubysl-pathname",
                "rubysl-tempfile", "rubysl-socket", "rubysl-securerandom",
                "rubysl-timeout", "rubysl-webrick")) {
            ArrayOperations.append(loadPath, StringOperations.createString(this, StringOperations.encodeRope(home + "lib/ruby/truffle/rubysl/" + lib + "/lib", UTF8Encoding.INSTANCE)));
        }

        // Shims
        ArrayOperations.append(loadPath, StringOperations.createString(this, StringOperations.encodeRope(home + "lib/ruby/truffle/shims", UTF8Encoding.INSTANCE)));
    }

    // TODO (eregon, 10/10/2015): this check could be done when a Symbol is created to be much cheaper
    public static String checkInstanceVariableName(RubyContext context, String name, Node currentNode) {
        // if (!IdUtil.isValidInstanceVariableName(name)) {

        // check like Rubinius does for compatibility with their Struct Ruby implementation.
        if (!(name.startsWith("@") && name.length() > 1 && IdUtil.isInitialCharacter(name.charAt(1)))) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreLibrary().nameErrorInstanceNameNotAllowable(name, currentNode));
        }
        return name;
    }

    public static String checkClassVariableName(RubyContext context, String name, Node currentNode) {
        if (!IdUtil.isValidClassVariableName(name)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreLibrary().nameErrorInstanceNameNotAllowable(name, currentNode));
        }
        return name;
    }

    public boolean isRunningOnWindows() {
        return runningOnWindows;
    }

    public void loadFile(String fileName, Node currentNode) throws IOException {
        loadFileAbsolute(fileName, currentNode);
    }

    private void loadFileAbsolute(String fileName, Node currentNode) throws IOException {
        final Source source = sourceCache.getSource(fileName);
        load(source, currentNode);
    }

    public void load(Source source, Node currentNode) {
        parseAndExecute(source, UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, coreLibrary.getMainObject(), null, true, DeclarationContext.TOP_LEVEL, currentNode);
    }

    public RopeTable getRopeTable() {
        return ropeTable;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public DynamicObject getSymbol(String name) {
        return symbolTable.getSymbol(name);
    }

    public DynamicObject getSymbol(ByteList name) {
        return symbolTable.getSymbol(name);
    }

    public DynamicObject getSymbol(Rope name) {
        return symbolTable.getSymbol(name);
    }

    @TruffleBoundary
    public Object instanceEval(ByteList code, Object self, String filename, Node currentNode) {
        final Source source = Source.fromText(code, filename);
        return parseAndExecute(source, code.getEncoding(), ParserContext.EVAL, self, null, true, DeclarationContext.INSTANCE_EVAL, currentNode);
    }

    @TruffleBoundary
    public Object eval(ParserContext parserContext, ByteList code, DynamicObject binding, boolean ownScopeForAssignments, String filename, Node currentNode) {
        assert RubyGuards.isRubyBinding(binding);
        final Source source = Source.fromText(code, filename);
        final MaterializedFrame frame = Layouts.BINDING.getFrame(binding);
        final DeclarationContext declarationContext = RubyArguments.getDeclarationContext(frame.getArguments());
        return parseAndExecute(source, code.getEncoding(), parserContext, RubyArguments.getSelf(frame.getArguments()), frame, ownScopeForAssignments, declarationContext, currentNode);
    }

    @TruffleBoundary
    public Object parseAndExecute(Source source, Encoding defaultEncoding, ParserContext parserContext, Object self, MaterializedFrame parentFrame, boolean ownScopeForAssignments,
                                  DeclarationContext declarationContext, Node currentNode) {
        final RubyRootNode rootNode = parse(source, defaultEncoding, parserContext, parentFrame, ownScopeForAssignments, currentNode);
        return execute(parserContext, declarationContext, rootNode, parentFrame, self);
    }

    @TruffleBoundary
    private RubyRootNode parse(Source source, Encoding defaultEncoding, ParserContext parserContext, MaterializedFrame parentFrame, boolean ownScopeForAssignments, Node currentNode) {
        final TranslatorDriver translator = new TranslatorDriver(this);
        return translator.parse(this, source, defaultEncoding, parserContext, null, parentFrame, ownScopeForAssignments, currentNode);
    }

    @TruffleBoundary
    private Object execute(ParserContext parserContext, DeclarationContext declarationContext, RubyRootNode rootNode, MaterializedFrame parentFrame, Object self) {
        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

        final DynamicObject declaringModule;
        if (parserContext == ParserContext.EVAL && parentFrame != null) {
            declaringModule = RubyArguments.getMethod(parentFrame.getArguments()).getDeclaringModule();
        } else if (parserContext == ParserContext.MODULE) {
            assert RubyGuards.isRubyModule(self);
            declaringModule = (DynamicObject) self;
        } else {
            declaringModule = getCoreLibrary().getObjectClass();
        }

        final InternalMethod method = new InternalMethod(rootNode.getSharedMethodInfo(), rootNode.getSharedMethodInfo().getName(),
                declaringModule, Visibility.PUBLIC, callTarget);

        return callTarget.call(RubyArguments.pack(parentFrame, null, method, declarationContext, null, self, null, new Object[]{}));
    }

    public long getNextObjectID() {
        final long id = nextObjectID.getAndAdd(2);

        if (id < 0) {
            CompilerDirectives.transferToInterpreter();
            nextObjectID.set(Long.MIN_VALUE);
            throw new RuntimeException("Object IDs exhausted");
        }

        return id;
    }

    public Object makeTuple(VirtualFrame frame, CallDispatchHeadNode newTupleNode, Object... values) {
        return newTupleNode.call(frame, getCoreLibrary().getTupleClass(), "create", null, values);
    }

    @TruffleBoundary
    public IRubyObject toJRuby(Object object) {
        if (object == getCoreLibrary().getNilObject()) {
            return runtime.getNil();
        } else if (object instanceof Boolean) {
            return runtime.newBoolean((boolean) object);
        } else if (RubyGuards.isRubyString(object)) {
            return toJRubyString((DynamicObject) object);
        } else if (RubyGuards.isRubyEncoding(object)) {
            return runtime.getEncodingService().rubyEncodingFromObject(runtime.newString(Layouts.ENCODING.getName((DynamicObject) object)));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @TruffleBoundary
    public org.jruby.RubyString toJRubyString(DynamicObject string) {
        assert RubyGuards.isRubyString(string);
        return runtime.newString(StringOperations.rope(string).toByteListCopy());
    }

    @TruffleBoundary
    public Object toTruffle(IRubyObject object) {
        if (object instanceof org.jruby.RubyFixnum) {
            final long value = ((org.jruby.RubyFixnum) object).getLongValue();

            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                return value;
            }

            return (int) value;
        } else if (object instanceof org.jruby.RubyBignum) {
            final BigInteger value = ((org.jruby.RubyBignum) object).getBigIntegerValue();
            return Layouts.BIGNUM.createBignum(coreLibrary.getBignumFactory(), value);
        } else if (object instanceof org.jruby.RubyString) {
            return StringOperations.createString(this, ((org.jruby.RubyString) object).getByteList().dup());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @TruffleBoundary
    public DynamicObject toTruffle(org.jruby.RubyException jrubyException, RubyNode currentNode) {
        switch (jrubyException.getMetaClass().getName()) {
            case "ArgumentError":
                return getCoreLibrary().argumentError(jrubyException.getMessage().toString(), currentNode);
            case "Encoding::CompatibilityError":
                return getCoreLibrary().encodingCompatibilityError(jrubyException.getMessage().toString(), currentNode);
            case "RegexpError":
                return getCoreLibrary().regexpError(jrubyException.getMessage().toString(), currentNode);
        }

        throw new UnsupportedOperationException();
    }

    public Ruby getRuntime() {
        return runtime;
    }

    public CoreLibrary getCoreLibrary() {
        return coreLibrary;
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

    public Warnings getWarnings() {
        return warnings;
    }

    public SafepointManager getSafepointManager() {
        return safepointManager;
    }

    public ThreadLocalRandom getRandom() {
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

    public CoverageTracker getCoverageTracker() {
        return coverageTracker;
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

    public RubiniusConfiguration getRubiniusConfiguration() {
        return rubiniusConfiguration;
    }

    public POSIX getPosix() {
        return posix;
    }

    public NativeSockets getNativeSockets() {
        return nativeSockets;
    }

    public LibCClockGetTime getLibCClockGetTime() {
        return libCClockGetTime;
    }

    public Object execute(final org.jruby.ast.RootNode rootNode) {
        coreLibrary.getGlobalVariablesObject().define("$0", toTruffle(runtime.getGlobalVariables().get("$0")), 0);

        String inputFile = rootNode.getPosition().getFile();
        final Source source;
        try {
            if (!inputFile.equals("-e")) {
                inputFile = new File(inputFile).getCanonicalPath();
            }
            source = sourceCache.getSource(inputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        featureLoader.setMainScriptSource(source);

        final RubyRootNode originalRootNode = parse(source, UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, null, true, null);

        final SourceSection sourceSection = originalRootNode.getSourceSection();
        final RubyNode wrappedBody =
                new TopLevelRaiseHandler(this, sourceSection,
                        SequenceNode.sequence(this, sourceSection,
                                new SetTopLevelBindingNode(this, sourceSection),
                                new LoadRequiredLibrariesNode(this, sourceSection),
                                originalRootNode.getBody()));

        final RubyRootNode newRootNode = originalRootNode.withBody(wrappedBody);

        if (rootNode.hasEndPosition()) {
            final Object data = inlineRubyHelper(null, "Truffle::Primitive.get_data(file, offset)", "file", StringOperations.createString(this, ByteList.create(inputFile)), "offset", rootNode.getEndPosition());
            Layouts.MODULE.getFields(coreLibrary.getObjectClass()).setConstant(this, null, "DATA", data);
        }

        return execute(ParserContext.TOP_LEVEL, DeclarationContext.TOP_LEVEL, newRootNode, null, coreLibrary.getMainObject());
    }

    public DynamicObject runAtExitHooks() {
        return atExitManager.runAtExitHooks();
    }

    public void shutdown() {
        atExitManager.runSystemExitHooks();

        if (instrumentationServerManager != null) {
            instrumentationServerManager.shutdown();
        }

        threadManager.shutdown();

        if (options.COVERAGE_GLOBAL) {
            coverageTracker.print(System.out);
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

    public PrintStream getDebugStandardOut() {
        return debugStandardOut;
    }

    public void exportObject(DynamicObject name, TruffleObject object) {
        assert RubyGuards.isRubyString(name);
        exported.put(name.toString(), object);
    }

    public Object findExportedObject(String name) {
        return exported.get(name);
    }

    public Object importObject(DynamicObject name) {
        assert RubyGuards.isRubyString(name);
        return env.importSymbol(name.toString());
    }

    public Options getOptions() {
        return options;
    }

    public TruffleLanguage.Env getEnv() {
        return env;
    }

    public void setInitialJRubyRootNode(org.jruby.ast.RootNode initialJRubyRootNode) {
        this.initialJRubyRootNode = initialJRubyRootNode;
    }

    public org.jruby.ast.RootNode getInitialJRubyRootNode() {
        return initialJRubyRootNode;
    }

    public DynamicObject createHandle(Object object) {
        return Layouts.HANDLE.createHandle(coreLibrary.getHandleFactory(), object);
    }

    public CrtExterns getCrtExterns() {
        return crtExterns;
    }

    public static void appendToFile(String fileName, String message) {
        try (PrintStream stream = new PrintStream(new FileOutputStream(fileName, true), true, StandardCharsets.UTF_8.name())) {
            stream.println(message);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public CallGraph getCallGraph() {
        return callGraph;
    }
}
