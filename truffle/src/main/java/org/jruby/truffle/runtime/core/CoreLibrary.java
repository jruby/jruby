/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.transcode.EConvFlags;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Platform.OS_TYPE;
import org.jruby.runtime.Constants;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.core.array.ArrayNodesFactory;
import org.jruby.truffle.nodes.core.fixnum.FixnumNodesFactory;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.nodes.core.hash.HashNodesFactory;
import org.jruby.truffle.nodes.ext.BigDecimalNodes;
import org.jruby.truffle.nodes.ext.BigDecimalNodesFactory;
import org.jruby.truffle.nodes.ext.DigestNodesFactory;
import org.jruby.truffle.nodes.ext.ZlibNodesFactory;
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.rubinius.*;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.rubinius.RubiniusTypes;
import org.jruby.truffle.runtime.signal.SignalOperations;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.util.cli.Options;
import org.jruby.util.cli.OutputStrings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CoreLibrary {

    private static final String CLI_RECORD_SEPARATOR = Options.CLI_RECORD_SEPARATOR.load();

    private final RubyContext context;

    private final RubyBasicObject argumentErrorClass;
    private final RubyBasicObject arrayClass;
    private final RubyBasicObject basicObjectClass;
    private final RubyBasicObject bignumClass;
    private final RubyBasicObject bindingClass;
    private final RubyBasicObject classClass;
    private final RubyBasicObject complexClass;
    private final RubyBasicObject dirClass;
    private final RubyBasicObject encodingClass;
    private final RubyBasicObject encodingErrorClass;
    private final RubyBasicObject exceptionClass;
    private final RubyBasicObject falseClass;
    private final RubyBasicObject fiberClass;
    private final RubyBasicObject fixnumClass;
    private final RubyBasicObject floatClass;
    private final RubyBasicObject floatDomainErrorClass;
    private final RubyBasicObject hashClass;
    private final RubyBasicObject integerClass;
    private final RubyBasicObject indexErrorClass;
    private final RubyBasicObject ioErrorClass;
    private final RubyBasicObject loadErrorClass;
    private final RubyBasicObject localJumpErrorClass;
    private final RubyBasicObject lookupTableClass;
    private final RubyBasicObject matchDataClass;
    private final RubyBasicObject moduleClass;
    private final RubyBasicObject nameErrorClass;
    private final RubyBasicObject nilClass;
    private final RubyBasicObject noMethodErrorClass;
    private final RubyBasicObject notImplementedErrorClass;
    private final RubyBasicObject numericClass;
    private final RubyBasicObject objectClass;
    private final RubyBasicObject procClass;
    private final RubyBasicObject processModule;
    private final RubyBasicObject rangeClass;
    private final RubyBasicObject rangeErrorClass;
    private final RubyBasicObject rationalClass;
    private final RubyBasicObject regexpClass;
    private final RubyBasicObject regexpErrorClass;
    private final RubyBasicObject rubyTruffleErrorClass;
    private final RubyBasicObject runtimeErrorClass;
    private final RubyBasicObject securityErrorClass;
    private final RubyBasicObject standardErrorClass;
    private final RubyBasicObject stringClass;
    private final RubyBasicObject stringDataClass;
    private final RubyBasicObject symbolClass;
    private final RubyBasicObject syntaxErrorClass;
    private final RubyBasicObject systemCallErrorClass;
    private final RubyBasicObject threadClass;
    private final RubyBasicObject threadBacktraceClass;
    private final RubyBasicObject threadBacktraceLocationClass;
    private final RubyBasicObject timeClass;
    private final RubyBasicObject transcodingClass;
    private final RubyBasicObject trueClass;
    private final RubyBasicObject tupleClass;
    private final RubyBasicObject typeErrorClass;
    private final RubyBasicObject zeroDivisionErrorClass;
    private final RubyBasicObject enumerableModule;
    private final RubyBasicObject errnoModule;
    private final RubyBasicObject kernelModule;
    private final RubyBasicObject rubiniusModule;
    private final RubyBasicObject rubiniusChannelClass;
    private final RubyBasicObject rubiniusFFIModule;
    private final RubyBasicObject rubiniusFFIPointerClass;
    private final RubyBasicObject rubiniusMirrorClass;
    private final RubyBasicObject signalModule;
    private final RubyBasicObject truffleModule;
    private final RubyBasicObject bigDecimalClass;
    private final RubyBasicObject encodingConverterClass;
    private final RubyBasicObject encodingCompatibilityErrorClass;
    private final RubyBasicObject methodClass;
    private final RubyBasicObject unboundMethodClass;
    private final RubyBasicObject byteArrayClass;
    private final RubyBasicObject fiberErrorClass;
    private final RubyBasicObject threadErrorClass;
    private final RubyBasicObject ioBufferClass;

    private final RubyBasicObject argv;
    private final RubyBasicObject globalVariablesObject;
    private final RubyBasicObject mainObject;
    private final RubyBasicObject nilObject;
    private final RubyBasicObject rubiniusUndefined;

    private final ArrayNodes.MinBlock arrayMinBlock;
    private final ArrayNodes.MaxBlock arrayMaxBlock;

    private final RubyBasicObject rubyInternalMethod;
    private final Map<Errno, RubyBasicObject> errnoClasses = new HashMap<>();

    @CompilationFinal private InternalMethod basicObjectSendMethod;

    private enum State {
        INITIALIZING,
        LOADING_RUBY_CORE,
        LOADED
    }

    private State state = State.INITIALIZING;

    public final Allocator NO_ALLOCATOR = new Allocator() {
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyBasicObject rubyClass, Node currentNode) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(typeError(String.format("allocator undefined for %s", ModuleNodes.getModel(rubyClass).getName()), currentNode));
        }
    };

    private static class CoreLibraryNode extends RubyNode {

        @Child SingletonClassNode singletonClassNode;
        @Child FreezeNode freezeNode;

        public CoreLibraryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
            this.freezeNode = FreezeNodeGen.create(context, sourceSection, null);
            adoptChildren();
        }

        public SingletonClassNode getSingletonClassNode() {
            return singletonClassNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return nil();
        }

    }

    private final CoreLibraryNode node;

    public CoreLibrary(RubyContext context) {
        this.context = context;
        this.node = new CoreLibraryNode(context, new CoreSourceSection("CoreLibrary", "initialize"));

        // Nothing in this constructor can use RubyContext.getCoreLibrary() as we are building it!
        // Therefore, only initialize the core classes and modules here.

        // Create the cyclic classes and modules

        classClass = ClassNodes.createClassClass(context, new ClassNodes.ClassAllocator());
        basicObjectClass = ClassNodes.createBootClass(classClass, null, "BasicObject", new RubyBasicObject.BasicObjectAllocator());
        objectClass = ClassNodes.createBootClass(classClass, basicObjectClass, "Object", ClassNodes.getAllocator(((RubyClass) basicObjectClass)));
        moduleClass = ClassNodes.createBootClass(classClass, objectClass, "Module", new ModuleNodes.ModuleAllocator());

        // Close the cycles
        ModuleNodes.getModel(classClass).unsafeSetSuperclass(moduleClass);

        ModuleNodes.getModel(classClass).getAdoptedByLexicalParent(objectClass, "Class", node);
        ModuleNodes.getModel(basicObjectClass).getAdoptedByLexicalParent(objectClass, "BasicObject", node);
        ModuleNodes.getModel(objectClass).getAdoptedByLexicalParent(objectClass, "Object", node);
        ModuleNodes.getModel(moduleClass).getAdoptedByLexicalParent(objectClass, "Module", node);

        // Create Exception classes

        // Exception
        exceptionClass = defineClass("Exception", new ExceptionNodes.ExceptionAllocator());

        // NoMemoryError
        defineClass(exceptionClass, "NoMemoryError");

        // RubyTruffleError
        rubyTruffleErrorClass = defineClass(exceptionClass, "RubyTruffleError");

        // StandardError
        standardErrorClass = defineClass(exceptionClass, "StandardError");
        argumentErrorClass = defineClass(standardErrorClass, "ArgumentError");
        encodingErrorClass = defineClass(standardErrorClass, "EncodingError");
        fiberErrorClass = defineClass(standardErrorClass, "FiberError");
        ioErrorClass = defineClass(standardErrorClass, "IOError");
        localJumpErrorClass = defineClass(standardErrorClass, "LocalJumpError");
        regexpErrorClass = defineClass(standardErrorClass, "RegexpError");
        runtimeErrorClass = defineClass(standardErrorClass, "RuntimeError");
        threadErrorClass = defineClass(standardErrorClass, "ThreadError");
        typeErrorClass = defineClass(standardErrorClass, "TypeError");
        zeroDivisionErrorClass = defineClass(standardErrorClass, "ZeroDivisionError");

        // StandardError > RangeError
        rangeErrorClass = defineClass(standardErrorClass, "RangeError");
        floatDomainErrorClass = defineClass(rangeErrorClass, "FloatDomainError");

        // StandardError > IndexError
        indexErrorClass = defineClass(standardErrorClass, "IndexError");
        defineClass(indexErrorClass, "KeyError");

        // StandardError > IOError
        defineClass(ioErrorClass, "EOFError");

        // StandardError > NameError
        nameErrorClass = defineClass(standardErrorClass, "NameError");
        noMethodErrorClass = defineClass(nameErrorClass, "NoMethodError");

        // StandardError > SystemCallError
        systemCallErrorClass = defineClass(standardErrorClass, "SystemCallError");

        errnoModule = defineModule("Errno");

        for (Errno errno : Errno.values()) {
            if (errno.name().startsWith("E")) {
                errnoClasses.put(errno, defineClass(errnoModule, systemCallErrorClass, errno.name()));
            }
        }

        // ScriptError
        RubyBasicObject scriptErrorClass = defineClass(exceptionClass, "ScriptError");
        loadErrorClass = defineClass(scriptErrorClass, "LoadError");
        notImplementedErrorClass = defineClass(scriptErrorClass, "NotImplementedError");
        syntaxErrorClass = defineClass(scriptErrorClass, "SyntaxError");

        // SecurityError
        securityErrorClass = defineClass(exceptionClass, "SecurityError");

        // SignalException
        RubyBasicObject signalExceptionClass = defineClass(exceptionClass, "SignalException");
        defineClass(signalExceptionClass, "Interrupt");

        // SystemExit
        defineClass(exceptionClass, "SystemExit");

        // SystemStackError
        defineClass(exceptionClass, "SystemStackError");

        // Create core classes and modules

        numericClass = defineClass("Numeric");
        complexClass = defineClass(numericClass, "Complex");
        floatClass = defineClass(numericClass, "Float", NO_ALLOCATOR);
        integerClass = defineClass(numericClass, "Integer", NO_ALLOCATOR);
        fixnumClass = defineClass(integerClass, "Fixnum");
        bignumClass = defineClass(integerClass, "Bignum");
        rationalClass = defineClass(numericClass, "Rational");

        // Classes defined in Object

        arrayClass = defineClass("Array", new ArrayNodes.ArrayAllocator());
        bindingClass = defineClass("Binding", new RubyBinding.BindingAllocator());
        dirClass = defineClass("Dir");
        encodingClass = defineClass("Encoding", NO_ALLOCATOR);
        falseClass = defineClass("FalseClass", NO_ALLOCATOR);
        fiberClass = defineClass("Fiber", new FiberNodes.FiberAllocator());
        defineModule("FileTest");
        hashClass = defineClass("Hash", new HashNodes.HashAllocator());
        matchDataClass = defineClass("MatchData");
        methodClass = defineClass("Method", NO_ALLOCATOR);
        defineClass("Mutex", new MutexNodes.MutexAllocator());
        nilClass = defineClass("NilClass", NO_ALLOCATOR);
        procClass = defineClass("Proc", new ProcNodes.ProcAllocator());
        processModule = defineModule("Process");
        RubyBasicObject queueClass = defineClass("Queue", new QueueNodes.QueueAllocator());
        defineClass(queueClass, "SizedQueue", new SizedQueueNodes.SizedQueueAllocator());
        rangeClass = defineClass("Range", new RangeNodes.RangeAllocator());
        regexpClass = defineClass("Regexp", new RegexpNodes.RegexpAllocator());
        stringClass = defineClass("String", new StringNodes.StringAllocator());
        symbolClass = defineClass("Symbol", NO_ALLOCATOR);
        threadClass = defineClass("Thread", new ThreadNodes.ThreadAllocator());
        threadBacktraceClass = defineClass(threadClass, objectClass, "Backtrace");
        threadBacktraceLocationClass = defineClass(threadBacktraceClass, objectClass, "Location", NO_ALLOCATOR);
        timeClass = defineClass("Time", new TimeNodes.TimeAllocator());
        trueClass = defineClass("TrueClass", NO_ALLOCATOR);
        unboundMethodClass = defineClass("UnboundMethod", NO_ALLOCATOR);
        final RubyBasicObject ioClass = defineClass("IO", new IOPrimitiveNodes.IOAllocator());
        ioBufferClass = defineClass(ioClass, objectClass, "InternalBuffer");

        // Modules

        RubyBasicObject comparableModule = defineModule("Comparable");
        defineModule("Config");
        enumerableModule = defineModule("Enumerable");
        defineModule("GC");
        kernelModule = defineModule("Kernel");
        defineModule("Math");
        defineModule("ObjectSpace");
        signalModule = defineModule("Signal");

        // The rest

        encodingCompatibilityErrorClass = defineClass(encodingClass, encodingErrorClass, "CompatibilityError");

        encodingConverterClass = defineClass(encodingClass, objectClass, "Converter", new EncodingConverterNodes.EncodingConverterAllocator());

        truffleModule = defineModule("Truffle");
        defineModule(truffleModule, "Interop");
        defineModule(truffleModule, "Debug");
        defineModule(truffleModule, "Primitive");
        defineModule(truffleModule, "Digest");
        defineModule(truffleModule, "Zlib");
        bigDecimalClass = defineClass(truffleModule, numericClass, "BigDecimal", new BigDecimalNodes.RubyBigDecimalAllocator());

        // Rubinius

        rubiniusModule = defineModule("Rubinius");

        rubiniusFFIModule = defineModule(rubiniusModule, "FFI");
        defineModule(defineModule(rubiniusFFIModule, "Platform"), "POSIX");
        rubiniusFFIPointerClass = defineClass(rubiniusFFIModule, objectClass, "Pointer", new PointerNodes.PointerAllocator());

        rubiniusChannelClass = defineClass(rubiniusModule, objectClass, "Channel");
        rubiniusMirrorClass = defineClass(rubiniusModule, objectClass, "Mirror");
        defineModule(rubiniusModule, "Type");

        byteArrayClass = defineClass(rubiniusModule, objectClass, "ByteArray");
        lookupTableClass = defineClass(rubiniusModule, hashClass, "LookupTable");
        stringDataClass = defineClass(rubiniusModule, objectClass, "StringData");
        transcodingClass = defineClass(encodingClass, objectClass, "Transcoding");
        tupleClass = defineClass(rubiniusModule, arrayClass, "Tuple");

        // Interop

        rubyInternalMethod = null;

        // Include the core modules

        includeModules(comparableModule);

        // Create some key objects

        mainObject = new RubyBasicObject(objectClass);
        nilObject = new RubyBasicObject(nilClass);
        argv = ArrayNodes.createEmptyArray(arrayClass);
        rubiniusUndefined = new RubyBasicObject(objectClass);

        globalVariablesObject = new RubyBasicObject(objectClass);

        arrayMinBlock = new ArrayNodes.MinBlock(context);
        arrayMaxBlock = new ArrayNodes.MaxBlock(context);
    }

    private void includeModules(RubyBasicObject comparableModule) {
        assert RubyGuards.isRubyModule(comparableModule);

        ModuleNodes.getModel(objectClass).include(node, kernelModule);

        ModuleNodes.getModel(numericClass).include(node, comparableModule);
        ModuleNodes.getModel(symbolClass).include(node, comparableModule);

        ModuleNodes.getModel(arrayClass).include(node, enumerableModule);
        ModuleNodes.getModel(dirClass).include(node, enumerableModule);
        ModuleNodes.getModel(hashClass).include(node, enumerableModule);
        ModuleNodes.getModel(rangeClass).include(node, enumerableModule);
    }

    /**
     * Initializations which may access {@link RubyContext#getCoreLibrary()}.
     */
    public void initialize() {
        addCoreMethods();
        initializeGlobalVariables();
        initializeConstants();
        initializeEncodingConstants();
        initializeSignalConstants();
    }

    private void addCoreMethods() {
        // Bring in core method nodes
        CoreMethodNodeManager coreMethodNodeManager = new CoreMethodNodeManager(objectClass, node.getSingletonClassNode());

        coreMethodNodeManager.addCoreMethodNodes(ArrayNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(BasicObjectNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(BindingNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(BignumNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ClassNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ExceptionNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(FalseClassNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(FiberNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(FixnumNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(FloatNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(HashNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(IntegerNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(KernelNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(MainNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(MatchDataNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(MathNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ModuleNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(MutexNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ObjectSpaceNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ProcessNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ProcNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(QueueNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(RangeNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(RegexpNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(SizedQueueNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(StringNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(SymbolNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ThreadNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(TrueClassNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(TrufflePrimitiveNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(EncodingNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(EncodingConverterNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(TruffleInteropNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(MethodNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(UnboundMethodNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ByteArrayNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(TimeNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(PosixNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(RubiniusTypeNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ThreadBacktraceLocationNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(DigestNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(BigDecimalNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ZlibNodesFactory.getFactories());
        coreMethodNodeManager.allMethodInstalled();

        basicObjectSendMethod = ModuleNodes.getModel(basicObjectClass).getMethods().get("__send__");
        assert basicObjectSendMethod != null;
    }

    private void initializeGlobalVariables() {
        RubyBasicObject globals = globalVariablesObject;

        RubyBasicObject.setInstanceVariable(globals, "$LOAD_PATH", ArrayNodes.createEmptyArray(arrayClass));
        RubyBasicObject.setInstanceVariable(globals, "$LOADED_FEATURES", ArrayNodes.createEmptyArray(arrayClass));
        RubyBasicObject.setInstanceVariable(globals, "$:", globals.getInstanceVariable("$LOAD_PATH"));
        RubyBasicObject.setInstanceVariable(globals, "$\"", globals.getInstanceVariable("$LOADED_FEATURES"));
        RubyBasicObject.setInstanceVariable(globals, "$,", nilObject);
        RubyBasicObject.setInstanceVariable(globals, "$0", context.toTruffle(context.getRuntime().getGlobalVariables().get("$0")));

        RubyBasicObject.setInstanceVariable(globals, "$DEBUG", context.getRuntime().isDebug());

        Object value = context.getRuntime().warningsEnabled() ? context.getRuntime().isVerbose() : nilObject;
        RubyBasicObject.setInstanceVariable(globals, "$VERBOSE", value);

        final RubyBasicObject defaultRecordSeparator = StringNodes.createString(stringClass, CLI_RECORD_SEPARATOR);
        node.freezeNode.executeFreeze(defaultRecordSeparator);

        // TODO (nirvdrum 05-Feb-15) We need to support the $-0 alias as well.
        RubyBasicObject.setInstanceVariable(globals, "$/", defaultRecordSeparator);

        RubyBasicObject.setInstanceVariable(globals, "$SAFE", 0);
    }

    private void initializeConstants() {
        // Set constants

        ModuleNodes.getModel(objectClass).setConstant(node, "RUBY_VERSION", StringNodes.createString(stringClass, Constants.RUBY_VERSION));
        ModuleNodes.getModel(objectClass).setConstant(node, "JRUBY_VERSION", StringNodes.createString(stringClass, Constants.VERSION));
        ModuleNodes.getModel(objectClass).setConstant(node, "RUBY_PATCHLEVEL", 0);
        ModuleNodes.getModel(objectClass).setConstant(node, "RUBY_REVISION", Constants.RUBY_REVISION);
        ModuleNodes.getModel(objectClass).setConstant(node, "RUBY_ENGINE", StringNodes.createString(stringClass, Constants.ENGINE + "+truffle"));
        ModuleNodes.getModel(objectClass).setConstant(node, "RUBY_PLATFORM", StringNodes.createString(stringClass, Constants.PLATFORM));
        ModuleNodes.getModel(objectClass).setConstant(node, "RUBY_RELEASE_DATE", StringNodes.createString(stringClass, Constants.COMPILE_DATE));
        ModuleNodes.getModel(objectClass).setConstant(node, "RUBY_DESCRIPTION", StringNodes.createString(stringClass, OutputStrings.getVersionString()));
        ModuleNodes.getModel(objectClass).setConstant(node, "RUBY_COPYRIGHT", StringNodes.createString(stringClass, OutputStrings.getCopyrightString()));

        // BasicObject knows itself
        ModuleNodes.getModel(basicObjectClass).setConstant(node, "BasicObject", basicObjectClass);

        ModuleNodes.getModel(objectClass).setConstant(node, "ARGV", argv);

        ModuleNodes.getModel(rubiniusModule).setConstant(node, "UNDEFINED", rubiniusUndefined);
        ModuleNodes.getModel(rubiniusModule).setConstant(node, "LIBC", Platform.LIBC);

        ModuleNodes.getModel(processModule).setConstant(node, "CLOCK_MONOTONIC", ProcessNodes.CLOCK_MONOTONIC);
        ModuleNodes.getModel(processModule).setConstant(node, "CLOCK_REALTIME", ProcessNodes.CLOCK_REALTIME);

        if (Platform.getPlatform().getOS() == OS_TYPE.LINUX) {
            ModuleNodes.getModel(processModule).setConstant(node, "CLOCK_THREAD_CPUTIME_ID", ProcessNodes.CLOCK_THREAD_CPUTIME_ID);
        }

        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "INVALID_MASK", EConvFlags.INVALID_MASK);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "INVALID_REPLACE", EConvFlags.INVALID_REPLACE);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "UNDEF_MASK", EConvFlags.UNDEF_MASK);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "UNDEF_REPLACE", EConvFlags.UNDEF_REPLACE);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "UNDEF_HEX_CHARREF", EConvFlags.UNDEF_HEX_CHARREF);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "PARTIAL_INPUT", EConvFlags.PARTIAL_INPUT);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "AFTER_OUTPUT", EConvFlags.AFTER_OUTPUT);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "UNIVERSAL_NEWLINE_DECORATOR", EConvFlags.UNIVERSAL_NEWLINE_DECORATOR);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "CRLF_NEWLINE_DECORATOR", EConvFlags.CRLF_NEWLINE_DECORATOR);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "CR_NEWLINE_DECORATOR", EConvFlags.CR_NEWLINE_DECORATOR);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "XML_TEXT_DECORATOR", EConvFlags.XML_TEXT_DECORATOR);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "XML_ATTR_CONTENT_DECORATOR", EConvFlags.XML_ATTR_CONTENT_DECORATOR);
        ModuleNodes.getModel(encodingConverterClass).setConstant(node, "XML_ATTR_QUOTE_DECORATOR", EConvFlags.XML_ATTR_QUOTE_DECORATOR);
    }

    private void initializeSignalConstants() {
        Object[] signals = new Object[SignalOperations.SIGNALS_LIST.size()];

        int i = 0;
        for (Map.Entry<String, Integer> signal : SignalOperations.SIGNALS_LIST.entrySet()) {
            RubyBasicObject signalName = StringNodes.createString(context.getCoreLibrary().getStringClass(), signal.getKey());
            signals[i++] = ArrayNodes.fromObjects(arrayClass, signalName, signal.getValue());
        }

        ModuleNodes.getModel(signalModule).setConstant(node, "SIGNAL_LIST", ArrayNodes.createArray(arrayClass, signals, signals.length));
    }

    private RubyBasicObject defineClass(String name) {
        return defineClass(objectClass, name, ClassNodes.getAllocator(((RubyClass) objectClass)));
    }

    private RubyBasicObject defineClass(String name, Allocator allocator) {
        return defineClass(objectClass, name, allocator);
    }

    private RubyBasicObject defineClass(RubyBasicObject superclass, String name) {
        assert RubyGuards.isRubyClass(superclass);
        return new RubyClass(context, objectClass, superclass, name, ClassNodes.getAllocator(((RubyClass) superclass)));
    }

    private RubyBasicObject defineClass(RubyBasicObject superclass, String name, Allocator allocator) {
        assert RubyGuards.isRubyClass(superclass);
        return new RubyClass(context, objectClass, superclass, name, allocator);
    }

    private RubyBasicObject defineClass(RubyBasicObject lexicalParent, RubyBasicObject superclass, String name) {
        assert RubyGuards.isRubyModule(lexicalParent);
        assert RubyGuards.isRubyClass(superclass);
        return new RubyClass(context, lexicalParent, superclass, name, ClassNodes.getAllocator(((RubyClass) superclass)));
    }

    private RubyBasicObject defineClass(RubyBasicObject lexicalParent, RubyBasicObject superclass, String name, Allocator allocator) {
        assert RubyGuards.isRubyModule(lexicalParent);
        assert RubyGuards.isRubyClass(superclass);
        return new RubyClass(context, lexicalParent, superclass, name, allocator);
    }

    private RubyBasicObject defineModule(String name) {
        return defineModule(objectClass, name);
    }

    private RubyBasicObject defineModule(RubyBasicObject lexicalParent, String name) {
        assert RubyGuards.isRubyModule(lexicalParent);
        return ModuleNodes.createRubyModule(context, moduleClass, lexicalParent, name, node);
    }

    public void initializeAfterMethodsAdded() {
        initializeRubiniusFFI();

        // Load Ruby core

        try {
            state = State.LOADING_RUBY_CORE;
            loadRubyCore("core.rb");
        } catch (RaiseException e) {
            final Object rubyException = e.getRubyException();

            for (String line : Backtrace.DISPLAY_FORMATTER.format(getContext(), (RubyBasicObject) rubyException, ExceptionNodes.getBacktrace((RubyBasicObject) rubyException))) {
                System.err.println(line);
            }

            throw new TruffleFatalException("couldn't load the core library", e);
        } finally {
            state = State.LOADED;
        }
    }

    private void initializeRubiniusFFI() {
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_CHAR", RubiniusTypes.TYPE_CHAR);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_UCHAR", RubiniusTypes.TYPE_UCHAR);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_BOOL", RubiniusTypes.TYPE_BOOL);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_SHORT", RubiniusTypes.TYPE_SHORT);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_USHORT", RubiniusTypes.TYPE_USHORT);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_INT", RubiniusTypes.TYPE_INT);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_UINT", RubiniusTypes.TYPE_UINT);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_LONG", RubiniusTypes.TYPE_LONG);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_ULONG", RubiniusTypes.TYPE_ULONG);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_LL", RubiniusTypes.TYPE_LL);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_ULL", RubiniusTypes.TYPE_ULL);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_FLOAT", RubiniusTypes.TYPE_FLOAT);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_DOUBLE", RubiniusTypes.TYPE_DOUBLE);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_PTR", RubiniusTypes.TYPE_PTR);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_VOID", RubiniusTypes.TYPE_VOID);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_STRING", RubiniusTypes.TYPE_STRING);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_STRPTR", RubiniusTypes.TYPE_STRPTR);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_CHARARR", RubiniusTypes.TYPE_CHARARR);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_ENUM", RubiniusTypes.TYPE_ENUM);
        ModuleNodes.getModel(rubiniusFFIModule).setConstant(node, "TYPE_VARARGS", RubiniusTypes.TYPE_VARARGS);
    }

    public void loadRubyCore(String fileName) {
        loadRubyCore(fileName, "core:/");
    }

    public void loadRubyCore(String fileName, String prefix) {
        final Source source;

        try {
            // TODO CS 28-Feb-15 need to use SourceManager here so that the debugger knows about the core files
            source = Source.fromReader(new InputStreamReader(getRubyCoreInputStream(fileName), StandardCharsets.UTF_8), prefix + fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        context.load(source, node, NodeWrapper.IDENTITY);
    }

    public InputStream getRubyCoreInputStream(String fileName) {
        final InputStream resource = getClass().getResourceAsStream("/" + fileName);

        if (resource == null) {
            throw new RuntimeException("couldn't load Truffle core library " + fileName);
        }

        return resource;
    }

    public void initializeEncodingConstants() {
        getContext().getRuntime().getEncodingService().defineEncodings(new EncodingService.EncodingDefinitionVisitor() {
            @Override
            public void defineEncoding(EncodingDB.Entry encodingEntry, byte[] name, int p, int end) {
                Encoding e = encodingEntry.getEncoding();

                RubyBasicObject re = EncodingNodes.newEncoding(encodingClass, e, name, p, end, encodingEntry.isDummy());
                EncodingNodes.storeEncoding(encodingEntry.getIndex(), re);
            }

            @Override
            public void defineConstant(int encodingListIndex, String constName) {
                ModuleNodes.getModel(encodingClass).setConstant(node, constName, EncodingNodes.getEncoding(encodingListIndex));
            }
        });

        getContext().getRuntime().getEncodingService().defineAliases(new EncodingService.EncodingAliasVisitor() {
            @Override
            public void defineAlias(int encodingListIndex, String constName) {
                RubyBasicObject re = EncodingNodes.getEncoding(encodingListIndex);
                EncodingNodes.storeAlias(constName, re);
            }

            @Override
            public void defineConstant(int encodingListIndex, String constName) {
                ModuleNodes.getModel(encodingClass).setConstant(node, constName, EncodingNodes.getEncoding(encodingListIndex));
            }
        });
    }

    public RubyBasicObject getMetaClass(Object object) {
        if (object instanceof RubyBasicObject) {
            return ((RubyBasicObject) object).getMetaClass();
        } else if (object instanceof Boolean) {
            if ((boolean) object) {
                return trueClass;
            } else {
                return falseClass;
            }
        } else if (object instanceof Integer) {
            return fixnumClass;
        } else if (object instanceof Long) {
            return fixnumClass;
        } else if (object instanceof Double) {
            return floatClass;
        } else if (object == null) {
            throw new RuntimeException("Can't get metaclass for null");
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException(String.format("Don't know how to get the metaclass for %s", object.getClass()));
        }
    }

    public RubyBasicObject getLogicalClass(Object object) {
        if (object instanceof RubyBasicObject) {
            return ((RubyBasicObject) object).getLogicalClass();
        } else if (object instanceof Boolean) {
            if ((boolean) object) {
                return trueClass;
            } else {
                return falseClass;
            }
        } else if (object instanceof Integer) {
            return fixnumClass;
        } else if (object instanceof Long) {
            return fixnumClass;
        } else if (object instanceof Double) {
            return floatClass;
        } else if (object == null) {
            throw new RuntimeException();
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException(String.format("Don't know how to get the logical class for %s", object.getClass()));
        }
    }

    /**
     * Convert a value to a {@code Float}, without doing any lookup.
     */
    public static double toDouble(Object value, RubyBasicObject nil) {
        assert value != null;

        if (value == nil) {
            return 0;
        }

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof Long) {
            return (long) value;
        }

        if (RubyGuards.isRubyBignum(value)) {
            return BignumNodes.getBigIntegerValue((RubyBasicObject) value).doubleValue();
        }

        if (value instanceof Double) {
            return (double) value;
        }

        CompilerDirectives.transferToInterpreter();
        throw new UnsupportedOperationException();
    }

    public static boolean fitsIntoInteger(long value) {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    public RubyBasicObject runtimeError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(runtimeErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject frozenError(String className, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return runtimeError(String.format("can't modify frozen %s", className), currentNode);
    }

    public RubyBasicObject argumentError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(argumentErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject argumentErrorOutOfRange(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError("out of range", currentNode);
    }

    public RubyBasicObject argumentErrorInvalidRadix(int radix, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("invalid radix %d", radix), currentNode);
    }

    public RubyBasicObject argumentErrorMissingKeyword(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("missing keyword: %s", name), currentNode);
    }

    public RubyBasicObject argumentError(int passed, int required, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("wrong number of arguments (%d for %d)", passed, required), currentNode);
    }

    public RubyBasicObject argumentError(int passed, int required, int optional, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("wrong number of arguments (%d for %d..%d)", passed, required, required + optional), currentNode);
    }

    public RubyBasicObject argumentErrorEmptyVarargs(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError("wrong number of arguments (0 for 1+)", currentNode);
    }

    public RubyBasicObject argumentErrorWrongArgumentType(Object object, String expectedType, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String badClassName = ModuleNodes.getModel(getLogicalClass(object)).getName();
        return argumentError(String.format("wrong argument type %s (expected %s)", badClassName, expectedType), currentNode);
    }

    public RubyBasicObject errnoError(int errno, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();

        Errno errnoObj = Errno.valueOf(errno);
        if (errnoObj == null) {
            return systemCallError(String.format("Unknown Error (%s)", errno), currentNode);
        }

        return ExceptionNodes.createRubyException(getErrnoClass(errnoObj), StringNodes.createString(context.getCoreLibrary().getStringClass(), errnoObj.description()), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject indexError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(indexErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject indexTooSmallError(String type, int index, int length, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return indexError(String.format("index %d too small for %s; minimum: -%d", index, type, length), currentNode);
    }

    public RubyBasicObject indexNegativeLength(int length, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return indexError(String.format("negative length (%d)", length), currentNode);
    }

    public RubyBasicObject localJumpError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(localJumpErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject noBlockGiven(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return localJumpError("no block given", currentNode);
    }

    public RubyBasicObject unexpectedReturn(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return localJumpError("unexpected return", currentNode);
    }

    public RubyBasicObject noBlockToYieldTo(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return localJumpError("no block given (yield)", currentNode);
    }

    public RubyBasicObject typeError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(typeErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject typeErrorCantDefineSingleton(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError("can't define singleton", currentNode);
    }

    public RubyBasicObject typeErrorNoClassToMakeAlias(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError("no class to make alias", currentNode);
    }

    public RubyBasicObject typeErrorShouldReturn(String object, String method, String expectedType, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("%s#%s should return %s", object, method, expectedType), currentNode);
    }

    public RubyBasicObject typeErrorCantConvertTo(Object from, RubyBasicObject to, String methodUsed, Object result, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyClass(to);
        String fromClass = ModuleNodes.getModel(getLogicalClass(from)).getName();
        return typeError(String.format("can't convert %s to %s (%s#%s gives %s)",
                fromClass, ModuleNodes.getModel(to).getName(), fromClass, methodUsed, getLogicalClass(result).toString()), currentNode);
    }

    public RubyBasicObject typeErrorCantConvertInto(Object from, RubyBasicObject to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyClass(to);
        return typeError(String.format("can't convert %s into %s", ModuleNodes.getModel(getLogicalClass(from)).getName(), ModuleNodes.getModel(to).getName()), currentNode);
    }

    public RubyBasicObject typeErrorIsNotA(String value, String expectedType, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("%s is not a %s", value, expectedType), currentNode);
    }

    public RubyBasicObject typeErrorNoImplicitConversion(Object from, String to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("no implicit conversion of %s into %s", ModuleNodes.getModel(getLogicalClass(from)).getName(), to), currentNode);
    }

    public RubyBasicObject typeErrorMustBe(String variable, String type, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("value of %s must be %s", variable, type), currentNode);
    }

    public RubyBasicObject typeErrorBadCoercion(Object from, String to, String coercionMethod, Object coercedTo, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String badClassName = ModuleNodes.getModel(getLogicalClass(from)).getName();
        return typeError(String.format("can't convert %s to %s (%s#%s gives %s)",
                badClassName,
                to,
                badClassName,
                coercionMethod,
                ModuleNodes.getModel(getLogicalClass(coercedTo)).getName()), currentNode);
    }

    public RubyBasicObject typeErrorCantCoerce(Object from, String to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("%s can't be coerced into %s", from, to), currentNode);
    }

    public RubyBasicObject typeErrorCantDump(Object object, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String logicalClass = ModuleNodes.getModel(getLogicalClass(object)).getName();
        return typeError(String.format("can't dump %s", logicalClass), currentNode);
    }

    public RubyBasicObject typeErrorWrongArgumentType(Object object, String expectedType, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String badClassName = ModuleNodes.getModel(getLogicalClass(object)).getName();
        return typeError(String.format("wrong argument type %s (expected %s)", badClassName, expectedType), currentNode);
    }

    public RubyBasicObject nameError(String message, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        RubyBasicObject nameError = ExceptionNodes.createRubyException(nameErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
        RubyBasicObject.setInstanceVariable(nameError, "@name", context.getSymbolTable().getSymbol(name));
        return nameError;
    }

    public RubyBasicObject nameErrorConstantNotDefined(RubyBasicObject module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("constant %s::%s not defined", ModuleNodes.getModel(module).getName(), name), name, currentNode);
    }

    public RubyBasicObject nameErrorUninitializedConstant(RubyBasicObject module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        final String message;
        if (module == objectClass) {
            message = String.format("uninitialized constant %s", name);
        } else {
            message = String.format("uninitialized constant %s::%s", ModuleNodes.getModel(module).getName(), name);
        }
        return nameError(message, name, currentNode);
    }

    public RubyBasicObject nameErrorUninitializedClassVariable(RubyBasicObject module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return nameError(String.format("uninitialized class variable %s in %s", name, ModuleNodes.getModel(module).getName()), name, currentNode);
    }

    public RubyBasicObject nameErrorPrivateConstant(RubyBasicObject module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return nameError(String.format("private constant %s::%s referenced", ModuleNodes.getModel(module).getName(), name), name, currentNode);
    }

    public RubyBasicObject nameErrorInstanceNameNotAllowable(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("`%s' is not allowable as an instance variable name", name), name, currentNode);
    }

    public RubyBasicObject nameErrorReadOnly(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("%s is a read-only variable", name), name, currentNode);
    }

    public RubyBasicObject nameErrorUndefinedLocalVariableOrMethod(String name, String object, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("undefined local variable or method `%s' for %s", name, object), name, currentNode);
    }

    public RubyBasicObject nameErrorUndefinedMethod(String name, RubyBasicObject module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return nameError(String.format("undefined method `%s' for %s", name, ModuleNodes.getModel(module).getName()), name, currentNode);
    }

    public RubyBasicObject nameErrorMethodNotDefinedIn(RubyBasicObject module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("method `%s' not defined in %s", name, ModuleNodes.getModel(module).getName()), name, currentNode);
    }

    public RubyBasicObject nameErrorPrivateMethod(String name, RubyBasicObject module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("method `%s' for %s is private", name, ModuleNodes.getModel(module).getName()), name, currentNode);
    }

    public RubyBasicObject nameErrorLocalVariableNotDefined(String name, RubyBasicObject binding, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyBinding(binding);
        return nameError(String.format("local variable `%s' not defined for %s", name, binding.toString()), name, currentNode);
    }

    public RubyBasicObject nameErrorClassVariableNotDefined(String name, RubyBasicObject module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return nameError(String.format("class variable `%s' not defined for %s", name, ModuleNodes.getModel(module).getName()), name, currentNode);
    }

    public RubyBasicObject noMethodError(String message, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        RubyBasicObject noMethodError = ExceptionNodes.createRubyException(context.getCoreLibrary().getNoMethodErrorClass(), StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
        RubyBasicObject.setInstanceVariable(noMethodError, "@name", context.getSymbolTable().getSymbol(name));
        return noMethodError;
    }

    public RubyBasicObject noSuperMethodError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String message = "super called outside of method";
        RubyBasicObject noMethodError = ExceptionNodes.createRubyException(context.getCoreLibrary().getNoMethodErrorClass(),
                StringNodes.createString(context.getCoreLibrary().getStringClass(), message),
                RubyCallStack.getBacktrace(currentNode));
        RubyBasicObject.setInstanceVariable(noMethodError, "@name", nilObject);
        return noMethodError;
    }

    public RubyBasicObject noMethodErrorOnModule(String name, RubyBasicObject module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return noMethodError(String.format("undefined method `%s' for %s", name, ModuleNodes.getModel(module).getName()), name, currentNode);
    }

    public RubyBasicObject noMethodErrorOnReceiver(String name, Object receiver, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        RubyBasicObject logicalClass = getLogicalClass(receiver);
        String repr = ModuleNodes.getModel(logicalClass).getName();
        if (RubyGuards.isRubyModule(receiver)) {
            repr = ModuleNodes.getModel(((RubyBasicObject) receiver)).getName() + ":" + repr;
        }
        return noMethodError(String.format("undefined method `%s' for %s", name, repr), name, currentNode);
    }

    public RubyBasicObject privateMethodError(String name, RubyBasicObject module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return noMethodError(String.format("private method `%s' called for %s", name, ModuleNodes.getModel(module).getName()), name, currentNode);
    }

    public RubyBasicObject loadError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(context.getCoreLibrary().getLoadErrorClass(), StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject loadErrorCannotLoad(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return loadError(String.format("cannot load such file -- %s", name), currentNode);
    }

    public RubyBasicObject zeroDivisionError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(context.getCoreLibrary().getZeroDivisionErrorClass(), StringNodes.createString(context.getCoreLibrary().getStringClass(), "divided by 0"), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject notImplementedError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(notImplementedErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Method %s not implemented", message)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject syntaxError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(syntaxErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject floatDomainError(String value, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(floatDomainErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), value), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject mathDomainError(String method, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EDOM), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Numerical argument is out of domain - \"%s\"", method)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject invalidArgumentError(String value, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EINVAL), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Invalid argument -  %s", value)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject ioError(String fileName, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(ioErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Error reading file -  %s", fileName)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject badAddressError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EFAULT), StringNodes.createString(context.getCoreLibrary().getStringClass(), "Bad address"), RubyCallStack.getBacktrace(currentNode));
    }


    public RubyBasicObject badFileDescriptor(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EBADF), StringNodes.createString(context.getCoreLibrary().getStringClass(), "Bad file descriptor"), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject fileExistsError(String fileName, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EEXIST), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("File exists - %s", fileName)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject fileNotFoundError(String fileName, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.ENOENT), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("No such file or directory -  %s", fileName)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject dirNotEmptyError(String path, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.ENOTEMPTY), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Directory not empty - %s", path)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject operationNotPermittedError(String path, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EPERM), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Operation not permitted - %s", path)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject permissionDeniedError(String path, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EACCES), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Permission denied - %s", path)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject notDirectoryError(String path, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.ENOTDIR), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Not a directory - %s", path)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject rangeError(int code, RubyBasicObject encoding, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyEncoding(encoding);
        return rangeError(String.format("invalid codepoint %x in %s", code, EncodingNodes.getEncoding(encoding)), currentNode);
    }

    public RubyBasicObject rangeError(String type, String value, String range, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return rangeError(String.format("%s %s out of range of %s", type, value, range), currentNode);
    }

    public RubyBasicObject rangeError(RubyBasicObject range, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isIntegerFixnumRange(range);
        return rangeError(String.format("%d..%s%d out of range",
                RangeNodes.getBegin(((RubyIntegerFixnumRange) range)),
                RangeNodes.isExcludeEnd(((RubyIntegerFixnumRange) range)) ? "." : "",
                RangeNodes.getEnd(((RubyIntegerFixnumRange) range))), currentNode);
    }

    public RubyBasicObject rangeError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(rangeErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject internalError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(context.getCoreLibrary().getRubyTruffleErrorClass(), StringNodes.createString(context.getCoreLibrary().getStringClass(), "internal implementation error - " + message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject regexpError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(regexpErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject encodingCompatibilityErrorIncompatible(String a, String b, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return encodingCompatibilityError(String.format("incompatible character encodings: %s and %s", a, b), currentNode);
    }

    public RubyBasicObject encodingCompatibilityError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(encodingCompatibilityErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject fiberError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(fiberErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject deadFiberCalledError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return fiberError("dead fiber called", currentNode);
    }

    public RubyBasicObject yieldFromRootFiberError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return fiberError("can't yield from root fiber", currentNode);
    }

    public RubyBasicObject threadError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(threadErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject securityError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(securityErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyBasicObject systemCallError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(systemCallErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyContext getContext() {
        return context;
    }

    public RubyBasicObject getArrayClass() {
        return arrayClass;
    }

    public RubyBasicObject getBasicObjectClass() {
        return basicObjectClass;
    }

    public RubyBasicObject getBignumClass() {
        return bignumClass;
    }

    public RubyBasicObject getBigDecimalClass() {
        return bigDecimalClass;
    }

    public RubyBasicObject getBindingClass() {
        return bindingClass;
    }

    public RubyBasicObject getClassClass() {
        return classClass;
    }

    public RubyBasicObject getFalseClass() {
        return falseClass;
    }

    public RubyBasicObject getFiberClass() {
        return fiberClass;
    }

    public RubyBasicObject getFixnumClass() {
        return fixnumClass;
    }

    public RubyBasicObject getFloatClass() {
        return floatClass;
    }

    public RubyBasicObject getHashClass() {
        return hashClass;
    }

    public RubyBasicObject getStandardErrorClass() { return standardErrorClass; }

    public RubyBasicObject getLoadErrorClass() {
        return loadErrorClass;
    }

    public RubyBasicObject getMatchDataClass() {
        return matchDataClass;
    }

    public RubyBasicObject getModuleClass() {
        return moduleClass;
    }

    public RubyBasicObject getNameErrorClass() {
        return nameErrorClass;
    }

    public RubyBasicObject getNilClass() {
        return nilClass;
    }

    public RubyBasicObject getRubyInternalMethod() {
        return rubyInternalMethod;
    }

    public RubyBasicObject getNoMethodErrorClass() {
        return noMethodErrorClass;
    }

    public RubyBasicObject getObjectClass() {
        return objectClass;
    }

    public RubyBasicObject getProcClass() {
        return procClass;
    }

    public RubyBasicObject getRangeClass() {
        return rangeClass;
    }

    public RubyBasicObject getRationalClass() {
        return rationalClass;
    }

    public RubyBasicObject getRegexpClass() {
        return regexpClass;
    }

    public RubyBasicObject getRubyTruffleErrorClass() {
        return rubyTruffleErrorClass;
    }

    public RubyBasicObject getRuntimeErrorClass() {
        return runtimeErrorClass;
    }

    public RubyBasicObject getStringClass() {
        return stringClass;
    }

    public RubyBasicObject getThreadClass() {
        return threadClass;
    }

    public RubyBasicObject getTimeClass() {
        return timeClass;
    }

    public RubyBasicObject getTypeErrorClass() { return typeErrorClass; }

    public RubyBasicObject getTrueClass() {
        return trueClass;
    }

    public RubyBasicObject getZeroDivisionErrorClass() {
        return zeroDivisionErrorClass;
    }

    public RubyBasicObject getKernelModule() {
        return kernelModule;
    }

    public RubyBasicObject getArgv() {
        return argv;
    }

    public RubyBasicObject getGlobalVariablesObject() {
        return globalVariablesObject;
    }

    public RubyBasicObject getLoadPath() {
        return (RubyBasicObject) globalVariablesObject.getInstanceVariable("$LOAD_PATH");
    }

    public RubyBasicObject getLoadedFeatures() {
        return (RubyBasicObject) globalVariablesObject.getInstanceVariable("$LOADED_FEATURES");
    }

    public RubyBasicObject getMainObject() {
        return mainObject;
    }

    public RubyBasicObject getNilObject() {
        return nilObject;
    }

    public RubyBasicObject getENV() {
        return (RubyBasicObject) ModuleNodes.getModel(objectClass).getConstants().get("ENV").getValue();
    }

    public ArrayNodes.MinBlock getArrayMinBlock() {
        return arrayMinBlock;
    }

    public ArrayNodes.MaxBlock getArrayMaxBlock() {
        return arrayMaxBlock;
    }

    public RubyBasicObject getNumericClass() {
        return numericClass;
    }

    public RubyBasicObject getIntegerClass() {
        return integerClass;
    }

    public RubyBasicObject getEncodingConverterClass() {
        return encodingConverterClass;
    }

    public RubyBasicObject getUnboundMethodClass() {
        return unboundMethodClass;
    }

    public RubyBasicObject getMethodClass() {
        return methodClass;
    }

    public RubyBasicObject getComplexClass() {
        return complexClass;
    }

    public RubyBasicObject getByteArrayClass() {
        return byteArrayClass;
    }

    public RubyBasicObject getLookupTableClass() {
        return lookupTableClass;
    }

    public RubyBasicObject getStringDataClass() {
        return stringDataClass;
    }

    public RubyBasicObject getTranscodingClass() {
        return transcodingClass;
    }

    public RubyBasicObject getTupleClass() {
        return tupleClass;
    }

    public RubyBasicObject getRubiniusChannelClass() {
        return rubiniusChannelClass;
    }

    public RubyBasicObject getRubiniusFFIPointerClass() {
        return rubiniusFFIPointerClass;
    }

    public RubyBasicObject getRubiniusMirrorClass() {
        return rubiniusMirrorClass;
    }

    public RubyBasicObject getRubiniusUndefined() {
        return rubiniusUndefined;
    }

    public RubyBasicObject getErrnoClass(Errno errno) {
        return errnoClasses.get(errno);
    }

    public RubyBasicObject getSymbolClass() {
        return symbolClass;
    }

    public RubyBasicObject getThreadBacktraceLocationClass() {
        return threadBacktraceLocationClass;
    }

    public RubyBasicObject getIOBufferClass() {
        return ioBufferClass;
    }

    public boolean isLoadingRubyCore() {
        return state == State.LOADING_RUBY_CORE;
    }

    public boolean isLoaded() {
        return state == State.LOADED;
    }

    public boolean isSend(InternalMethod method) {
        return method.getCallTarget() == basicObjectSendMethod.getCallTarget();
    }
}
