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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;

import jnr.constants.platform.Errno;
import jnr.posix.FileStat;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.transcode.EConvFlags;
import org.jruby.runtime.Constants;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.runtime.load.LoadServiceResource;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ArrayNodes;
import org.jruby.truffle.nodes.core.MutexNodes;
import org.jruby.truffle.nodes.core.ProcessNodes;
import org.jruby.truffle.nodes.methods.SetMethodDeclarationContext;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.nodes.rubinius.NativeFunctionPrimitiveNodes;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.truffle.runtime.signal.SignalOperations;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.util.cli.Options;
import org.jruby.util.cli.OutputStrings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CoreLibrary {

    private final RubyContext context;

    private final RubyClass argumentErrorClass;
    private final RubyClass arrayClass;
    private final RubyClass basicObjectClass;
    private final RubyClass bignumClass;
    private final RubyClass bindingClass;
    private final RubyClass classClass;
    private final RubyClass complexClass;
    private final RubyClass dirClass;
    private final RubyClass encodingClass;
    private final RubyClass encodingErrorClass;
    private final RubyClass eofErrorClass;
    private final RubyClass exceptionClass;
    private final RubyClass falseClass;
    private final RubyClass fiberClass;
    private final RubyClass fileClass;
    private final RubyClass fixnumClass;
    private final RubyClass floatClass;
    private final RubyClass floatDomainErrorClass;
    private final RubyClass hashClass;
    private final RubyClass integerClass;
    private final RubyClass indexErrorClass;
    private final RubyClass ioClass;
    private final RubyClass ioErrorClass;
    private final RubyClass keyErrorClass;
    private final RubyClass loadErrorClass;
    private final RubyClass localJumpErrorClass;
    private final RubyClass lookupTableClass;
    private final RubyClass matchDataClass;
    private final RubyClass moduleClass;
    private final RubyClass nameErrorClass;
    private final RubyClass nilClass;
    private final RubyClass noMemoryErrorClass;
    private final RubyClass noMethodErrorClass;
    private final RubyClass notImplementedErrorClass;
    private final RubyClass numericClass;
    private final RubyClass objectClass;
    private final RubyClass procClass;
    private final RubyModule processModule;
    private final RubyClass rangeClass;
    private final RubyClass rangeErrorClass;
    private final RubyClass rationalClass;
    private final RubyClass regexpClass;
    private final RubyClass regexpErrorClass;
    private final RubyClass rubyTruffleErrorClass;
    private final RubyClass runtimeErrorClass;
    private final RubyClass securityErrorClass;
    private final RubyClass standardErrorClass;
    private final RubyClass stringClass;
    private final RubyClass stringDataClass;
    private final RubyClass symbolClass;
    private final RubyClass syntaxErrorClass;
    private final RubyClass systemCallErrorClass;
    private final RubyClass systemExitClass;
    private final RubyClass systemStackErrorClass;
    private final RubyClass threadClass;
    private final RubyClass timeClass;
    private final RubyClass transcodingClass;
    private final RubyClass trueClass;
    private final RubyClass tupleClass;
    private final RubyClass typeErrorClass;
    private final RubyClass zeroDivisionErrorClass;
    private final RubyModule configModule;
    private final RubyModule enumerableModule;
    private final RubyModule errnoModule;
    private final RubyModule gcModule;
    private final RubyModule kernelModule;
    private final RubyModule mathModule;
    private final RubyModule objectSpaceModule;
    private final RubyModule rubiniusModule;
    private final RubyModule rubiniusFFIModule;
    private final RubyModule signalModule;
    private final RubyModule truffleModule;
    private final RubyModule truffleDebugModule;
    private final RubyClass edomClass;
    private final RubyClass einvalClass;
    private final RubyClass enoentClass;
    private final RubyClass enotemptyClass;
    private final RubyClass encodingConverterClass;
    private final RubyClass encodingCompatibilityErrorClass;
    private final RubyClass methodClass;
    private final RubyClass unboundMethodClass;
    private final RubyClass byteArrayClass;
    private final RubyClass fiberErrorClass;
    private final RubyClass threadErrorClass;

    private final RubyArray argv;
    private final RubyBasicObject globalVariablesObject;
    private final RubyBasicObject mainObject;
    private final RubyNilClass nilObject;
    private RubyBasicObject rubiniusUndefined;

    private final ArrayNodes.MinBlock arrayMinBlock;
    private final ArrayNodes.MaxBlock arrayMaxBlock;

    @CompilerDirectives.CompilationFinal private RubySymbol eachSymbol;
    @CompilerDirectives.CompilationFinal private RubySymbol mapSymbol;
    @CompilerDirectives.CompilationFinal private RubySymbol mapBangSymbol;
    @CompilerDirectives.CompilationFinal private RubyHash envHash;

    private static enum State {
        INITIALIZING,
        LOADING_RUBY_CORE,
        LOADED
    }

    private State state = State.INITIALIZING;

    public CoreLibrary(RubyContext context) {
        this.context = context;

        // Nothing in this constructor can use RubyContext.getCoreLibrary() as we are building it!
        // Therefore, only initialize the core classes and modules here.

        // Create the cyclic classes and modules

        classClass = RubyClass.createBootClass(context, null, "Class", new RubyClass.ClassAllocator());
        basicObjectClass = RubyClass.createBootClass(context, classClass, "BasicObject", new RubyBasicObject.BasicObjectAllocator());
        objectClass = RubyClass.createBootClass(context, classClass, "Object", basicObjectClass.getAllocator());
        moduleClass = RubyClass.createBootClass(context, classClass, "Module", new RubyModule.ModuleAllocator());

        // Close the cycles
        classClass.unsafeSetLogicalClass(classClass);

        objectClass.unsafeSetSuperclass(basicObjectClass);
        moduleClass.unsafeSetSuperclass(objectClass);
        classClass.unsafeSetSuperclass(moduleClass);

        classClass.getAdoptedByLexicalParent(objectClass, "Class", null);
        basicObjectClass.getAdoptedByLexicalParent(objectClass, "BasicObject", null);
        objectClass.getAdoptedByLexicalParent(objectClass, "Object", null);
        moduleClass.getAdoptedByLexicalParent(objectClass, "Module", null);

        // Create Exception classes 

        // Exception
        exceptionClass = defineClass("Exception", new RubyException.ExceptionAllocator());

        // FiberError
        fiberErrorClass = defineClass(exceptionClass, "FiberError");

        // NoMemoryError
        noMemoryErrorClass = defineClass(exceptionClass, "NoMemoryError");

        // RubyTruffleError
        rubyTruffleErrorClass = defineClass(exceptionClass, "RubyTruffleError");

        // StandardError
        standardErrorClass = defineClass(exceptionClass, "StandardError");
        argumentErrorClass = defineClass(standardErrorClass, "ArgumentError");
        encodingErrorClass = defineClass(standardErrorClass, "EncodingError");
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
        keyErrorClass = defineClass(indexErrorClass, "KeyError");

        // StandardError > IOError
        eofErrorClass = defineClass(ioErrorClass, "EOFError");

        // StandardError > NameError
        nameErrorClass = defineClass(standardErrorClass, "NameError");
        noMethodErrorClass = defineClass(nameErrorClass, "NoMethodError");

        // StandardError > SystemCallError
        systemCallErrorClass = defineClass(standardErrorClass, "SystemCallError");
        errnoModule = defineModule("Errno");
        defineClass(errnoModule, systemCallErrorClass, "EACCES");
        edomClass = defineClass(errnoModule, systemCallErrorClass, "EDOM");
        defineClass(errnoModule, systemCallErrorClass, "EEXIST");
        einvalClass = defineClass(errnoModule, systemCallErrorClass, "EINVAL");
        enoentClass = defineClass(errnoModule, systemCallErrorClass, "ENOENT");
        enotemptyClass = defineClass(errnoModule, systemCallErrorClass, "ENOTEMPTY");
        defineClass(errnoModule, systemCallErrorClass, "ENXIO");
        defineClass(errnoModule, systemCallErrorClass, "EPERM");
        defineClass(errnoModule, systemCallErrorClass, "EXDEV");
        defineClass(errnoModule, systemCallErrorClass, "ECHILD");

        // ScriptError
        RubyClass scriptErrorClass = defineClass(exceptionClass, "ScriptError");
        loadErrorClass = defineClass(scriptErrorClass, "LoadError");
        notImplementedErrorClass = defineClass(scriptErrorClass, "NotImplementedError");
        syntaxErrorClass = defineClass(scriptErrorClass, "SyntaxError");

        // SecurityError
        securityErrorClass = defineClass(exceptionClass, "SecurityError");

        // SignalException
        RubyClass signalExceptionClass = defineClass(exceptionClass, "SignalException");
        defineClass(signalExceptionClass, "Interrupt");

        // SystemExit
        systemExitClass = defineClass(exceptionClass, "SystemExit");

        // SystemStackError
        systemStackErrorClass = defineClass(exceptionClass, "SystemStackError");

        // Create core classes and modules

        numericClass = defineClass("Numeric");
        complexClass = defineClass(numericClass, "Complex");
        floatClass = defineClass(numericClass, "Float");
        integerClass = defineClass(numericClass, "Integer");
        fixnumClass = defineClass(integerClass, "Fixnum");
        bignumClass = defineClass(integerClass, "Bignum", new RubyBignum.BignumAllocator());
        rationalClass = defineClass(numericClass, "Rational");

        ioClass = defineClass("IO");
        fileClass = defineClass(ioClass, "File");

        // Classes defined in Object

        arrayClass = defineClass("Array", new RubyArray.ArrayAllocator());
        bindingClass = defineClass("Binding", new RubyBinding.BindingAllocator());
        dirClass = defineClass("Dir");
        encodingClass = defineClass("Encoding", new RubyEncoding.EncodingAllocator());
        falseClass = defineClass("FalseClass");
        fiberClass = defineClass("Fiber", new RubyFiber.FiberAllocator());
        defineModule("FileTest");
        hashClass = defineClass("Hash", new RubyHash.HashAllocator());
        matchDataClass = defineClass("MatchData");
        methodClass = defineClass("Method");
        defineClass("Mutex", MutexNodes.createMutexAllocator(context.getEmptyShape()));
        nilClass = defineClass("NilClass");
        procClass = defineClass("Proc", new RubyProc.ProcAllocator());
        processModule = defineModule("Process");
        rangeClass = defineClass("Range", new RubyRange.RangeAllocator());
        regexpClass = defineClass("Regexp", new RubyRegexp.RegexpAllocator());
        stringClass = defineClass("String", new RubyString.StringAllocator());
        symbolClass = defineClass("Symbol");
        threadClass = defineClass("Thread", new RubyThread.ThreadAllocator());
        timeClass = defineClass("Time", new RubyTime.TimeAllocator());
        trueClass = defineClass("TrueClass");
        unboundMethodClass = defineClass("UnboundMethod");

        // Modules

        RubyModule comparableModule = defineModule("Comparable");
        configModule = defineModule("Config");
        enumerableModule = defineModule("Enumerable");
        gcModule = defineModule("GC");
        kernelModule = defineModule("Kernel");
        mathModule = defineModule("Math");
        objectSpaceModule = defineModule("ObjectSpace");
        signalModule = defineModule("Signal");

        // The rest

        encodingCompatibilityErrorClass = defineClass(encodingClass, encodingErrorClass, "CompatibilityError");

        encodingConverterClass = defineClass(encodingClass, objectClass, "Converter", new RubyEncodingConverter.EncodingConverterAllocator());

        truffleModule = defineModule("Truffle");
        truffleDebugModule = defineModule(truffleModule, "Debug");
        defineModule(truffleModule, "Primitive");

        rubiniusModule = defineModule("Rubinius");
        rubiniusFFIModule = defineModule(rubiniusModule, "FFI");
        defineModule(defineModule(rubiniusFFIModule, "Platform"), "POSIX");

        byteArrayClass = defineClass(rubiniusModule, objectClass, "ByteArray");
        lookupTableClass = defineClass(rubiniusModule, hashClass, "LookupTable");
        stringDataClass = defineClass(rubiniusModule, objectClass, "StringData");
        transcodingClass = defineClass(encodingClass, objectClass, "Transcoding");
        tupleClass = defineClass(rubiniusModule, arrayClass, "Tuple");

        // Include the core modules

        includeModules(comparableModule);

        // Create some key objects

        mainObject = new RubyBasicObject(objectClass);
        nilObject = new RubyNilClass(nilClass);
        argv = new RubyArray(arrayClass);
        rubiniusUndefined = new RubyBasicObject(objectClass);

        globalVariablesObject = new RubyBasicObject(objectClass);

        arrayMinBlock = new ArrayNodes.MinBlock(context);
        arrayMaxBlock = new ArrayNodes.MaxBlock(context);
    }

    private void includeModules(RubyModule comparableModule) {
        objectClass.include(null, kernelModule);

        numericClass.include(null, comparableModule);
        symbolClass.include(null, comparableModule);

        arrayClass.include(null, enumerableModule);
        dirClass.include(null, enumerableModule);
        hashClass.include(null, enumerableModule);
        ioClass.include(null, enumerableModule);
        rangeClass.include(null, enumerableModule);
    }

    /**
     * Initializations which may access {@link RubyContext#getCoreLibrary()}.
     */
    public void initialize() {
        initializeGlobalVariables();
        initializeConstants();
        initializeEncodingConstants();
        initializeSignalConstants();

        // Common symbols
        eachSymbol = getContext().getSymbolTable().getSymbol("each");
        mapBangSymbol = getContext().getSymbolTable().getSymbol("map!");
        mapSymbol = getContext().getSymbolTable().getSymbol("map");
    }

    private void initializeGlobalVariables() {
        RubyNode.notDesignedForCompilation();

        RubyBasicObject globals = globalVariablesObject;

        globals.getOperations().setInstanceVariable(globals, "$LOAD_PATH", new RubyArray(arrayClass));
        globals.getOperations().setInstanceVariable(globals, "$LOADED_FEATURES", new RubyArray(arrayClass));
        globals.getOperations().setInstanceVariable(globals, "$:", globals.getInstanceVariable("$LOAD_PATH"));
        globals.getOperations().setInstanceVariable(globals, "$\"", globals.getInstanceVariable("$LOADED_FEATURES"));
        globals.getOperations().setInstanceVariable(globals, "$,", nilObject);

        globals.getOperations().setInstanceVariable(globals, "$DEBUG", context.getRuntime().isDebug());

        Object value = context.getRuntime().warningsEnabled() ? context.getRuntime().isVerbose() : nilObject;
        globals.getOperations().setInstanceVariable(globals, "$VERBOSE", value);

        final RubyString defaultRecordSeparator = RubyString.fromJavaString(stringClass, Options.CLI_RECORD_SEPARATOR.load());
        defaultRecordSeparator.freeze();

        // TODO (nirvdrum 05-Feb-15) We need to support the $-0 alias as well.
        globals.getOperations().setInstanceVariable(globals, "$/", defaultRecordSeparator);
    }

    private void initializeConstants() {
        // Set constants

        objectClass.setConstant(null, "RUBY_VERSION", RubyString.fromJavaString(stringClass, Constants.RUBY_VERSION));
        objectClass.setConstant(null, "RUBY_PATCHLEVEL", Constants.RUBY_PATCHLEVEL);
        objectClass.setConstant(null, "RUBY_ENGINE", RubyString.fromJavaString(stringClass, Constants.ENGINE + "+truffle"));
        objectClass.setConstant(null, "RUBY_PLATFORM", RubyString.fromJavaString(stringClass, Constants.PLATFORM));
        objectClass.setConstant(null, "RUBY_RELEASE_DATE", RubyString.fromJavaString(stringClass, Constants.COMPILE_DATE));
        objectClass.setConstant(null, "RUBY_DESCRIPTION", RubyString.fromJavaString(stringClass, OutputStrings.getVersionString()));

        // BasicObject knows itself
        basicObjectClass.setConstant(null, "BasicObject", basicObjectClass);

        // TODO(cs): this should be a separate exception
        mathModule.setConstant(null, "DomainError", edomClass);

        objectClass.setConstant(null, "ARGV", argv);

        rubiniusModule.setConstant(null, "UNDEFINED", rubiniusUndefined);

        final RubyString separator = RubyString.fromJavaString(stringClass, "/");
        separator.freeze();

        fileClass.setConstant(null, "SEPARATOR", separator);
        fileClass.setConstant(null, "Separator", separator);

        if (File.separatorChar == '\\') {
            final RubyString altSeparator = RubyString.fromJavaString(stringClass, "\\");
            altSeparator.freeze();

            fileClass.setConstant(null, "ALT_SEPARATOR", altSeparator);
        } else {
            fileClass.setConstant(null, "ALT_SEPARATOR", nilObject);
        }

        fileClass.setConstant(null, "PATH_SEPARATOR", RubyString.fromJavaString(stringClass, File.pathSeparator));
        fileClass.setConstant(null, "FNM_SYSCASE", 0);

        processModule.setConstant(null, "CLOCK_MONOTONIC", ProcessNodes.CLOCK_MONOTONIC);
        processModule.setConstant(null, "CLOCK_REALTIME", ProcessNodes.CLOCK_REALTIME);

        encodingConverterClass.setConstant(null, "INVALID_MASK", EConvFlags.INVALID_MASK);
        encodingConverterClass.setConstant(null, "INVALID_REPLACE", EConvFlags.INVALID_REPLACE);
        encodingConverterClass.setConstant(null, "UNDEF_MASK", EConvFlags.UNDEF_MASK);
        encodingConverterClass.setConstant(null, "UNDEF_REPLACE", EConvFlags.UNDEF_REPLACE);
        encodingConverterClass.setConstant(null, "UNDEF_HEX_CHARREF", EConvFlags.UNDEF_HEX_CHARREF);
        encodingConverterClass.setConstant(null, "PARTIAL_INPUT", EConvFlags.PARTIAL_INPUT);
        encodingConverterClass.setConstant(null, "AFTER_OUTPUT", EConvFlags.AFTER_OUTPUT);
        encodingConverterClass.setConstant(null, "UNIVERSAL_NEWLINE_DECORATOR", EConvFlags.UNIVERSAL_NEWLINE_DECORATOR);
        encodingConverterClass.setConstant(null, "CRLF_NEWLINE_DECORATOR", EConvFlags.CRLF_NEWLINE_DECORATOR);
        encodingConverterClass.setConstant(null, "CR_NEWLINE_DECORATOR", EConvFlags.CR_NEWLINE_DECORATOR);
        encodingConverterClass.setConstant(null, "XML_TEXT_DECORATOR", EConvFlags.XML_TEXT_DECORATOR);
        encodingConverterClass.setConstant(null, "XML_ATTR_CONTENT_DECORATOR", EConvFlags.XML_ATTR_CONTENT_DECORATOR);
        encodingConverterClass.setConstant(null, "XML_ATTR_QUOTE_DECORATOR", EConvFlags.XML_ATTR_QUOTE_DECORATOR);
    }

    private void initializeSignalConstants() {
        RubyNode.notDesignedForCompilation();

        Object[] signals = new Object[SignalOperations.SIGNALS_LIST.size()];

        int i = 0;
        for (Map.Entry<String, Integer> signal : SignalOperations.SIGNALS_LIST.entrySet()) {
            RubyString signalName = context.makeString(signal.getKey());
            signals[i++] = RubyArray.fromObjects(arrayClass, signalName, signal.getValue());
        }

        signalModule.setConstant(null, "SIGNAL_LIST", new RubyArray(arrayClass, signals, signals.length));
    }

    private RubyClass defineClass(String name) {
        return defineClass(objectClass, name, objectClass.getAllocator());
    }

    private RubyClass defineClass(String name, Allocator allocator) {
        return defineClass(objectClass, name, allocator);
    }

    private RubyClass defineClass(RubyClass superclass, String name) {
        return new RubyClass(context, objectClass, superclass, name, superclass.getAllocator());
    }

    private RubyClass defineClass(RubyClass superclass, String name, Allocator allocator) {
        return new RubyClass(context, objectClass, superclass, name, allocator);
    }

    private RubyClass defineClass(RubyModule lexicalParent, RubyClass superclass, String name) {
        return new RubyClass(context, lexicalParent, superclass, name, superclass.getAllocator());
    }

    private RubyClass defineClass(RubyModule lexicalParent, RubyClass superclass, String name, Allocator allocator) {
        return new RubyClass(context, lexicalParent, superclass, name, allocator);
    }

    private RubyModule defineModule(String name) {
        return defineModule(objectClass, name);
    }

    private RubyModule defineModule(RubyModule lexicalParent, String name) {
        return new RubyModule(context, moduleClass, lexicalParent, name, null);
    }

    public void initializeAfterMethodsAdded() {
        initializeRubiniusFFI();

        // ENV is supposed to be an object that actually updates the environment, and sees any updates

        envHash = getSystemEnv();
        objectClass.setConstant(null, "ENV", envHash);

        // Load Ruby core

        if (Options.TRUFFLE_LOAD_CORE.load()) {
            try {
                state = State.LOADING_RUBY_CORE;
                loadRubyCore("core.rb");
            } catch (RaiseException e) {
                final RubyException rubyException = e.getRubyException();

                for (String line : Backtrace.DISPLAY_FORMATTER.format(getContext(), rubyException, rubyException.getBacktrace())) {
                    System.err.println(line);
                }

                throw new TruffleFatalException("couldn't load the core library", e);
            } finally {
                state = State.LOADED;
            }
        }
    }

    private void initializeRubiniusFFI() {
        rubiniusFFIModule.setConstant(null, "TYPE_CHAR", NativeFunctionPrimitiveNodes.TYPE_CHAR);
        rubiniusFFIModule.setConstant(null, "TYPE_UCHAR", NativeFunctionPrimitiveNodes.TYPE_UCHAR);
        rubiniusFFIModule.setConstant(null, "TYPE_BOOL", NativeFunctionPrimitiveNodes.TYPE_BOOL);
        rubiniusFFIModule.setConstant(null, "TYPE_SHORT", NativeFunctionPrimitiveNodes.TYPE_SHORT);
        rubiniusFFIModule.setConstant(null, "TYPE_USHORT", NativeFunctionPrimitiveNodes.TYPE_USHORT);
        rubiniusFFIModule.setConstant(null, "TYPE_INT", NativeFunctionPrimitiveNodes.TYPE_INT);
        rubiniusFFIModule.setConstant(null, "TYPE_UINT", NativeFunctionPrimitiveNodes.TYPE_UINT);
        rubiniusFFIModule.setConstant(null, "TYPE_LONG", NativeFunctionPrimitiveNodes.TYPE_LONG);
        rubiniusFFIModule.setConstant(null, "TYPE_ULONG", NativeFunctionPrimitiveNodes.TYPE_ULONG);
        rubiniusFFIModule.setConstant(null, "TYPE_LL", NativeFunctionPrimitiveNodes.TYPE_LL);
        rubiniusFFIModule.setConstant(null, "TYPE_ULL", NativeFunctionPrimitiveNodes.TYPE_ULL);
        rubiniusFFIModule.setConstant(null, "TYPE_FLOAT", NativeFunctionPrimitiveNodes.TYPE_FLOAT);
        rubiniusFFIModule.setConstant(null, "TYPE_DOUBLE", NativeFunctionPrimitiveNodes.TYPE_DOUBLE);
        rubiniusFFIModule.setConstant(null, "TYPE_PTR", NativeFunctionPrimitiveNodes.TYPE_PTR);
        rubiniusFFIModule.setConstant(null, "TYPE_VOID", NativeFunctionPrimitiveNodes.TYPE_VOID);
        rubiniusFFIModule.setConstant(null, "TYPE_STRING", NativeFunctionPrimitiveNodes.TYPE_STRING);
        rubiniusFFIModule.setConstant(null, "TYPE_STRPTR", NativeFunctionPrimitiveNodes.TYPE_STRPTR);
        rubiniusFFIModule.setConstant(null, "TYPE_CHARARR", NativeFunctionPrimitiveNodes.TYPE_CHARARR);
        rubiniusFFIModule.setConstant(null, "TYPE_ENUM", NativeFunctionPrimitiveNodes.TYPE_ENUM);
        rubiniusFFIModule.setConstant(null, "TYPE_VARARGS", NativeFunctionPrimitiveNodes.TYPE_VARARGS);
    }

    public void loadRubyCore(String fileName) {
        loadRubyCore(fileName, "core:/");
    }

    public void loadRubyCore(String fileName, String prefix) {
        final Source source;

        try {
            source = Source.fromReader(new InputStreamReader(getRubyCoreInputStream(fileName), StandardCharsets.UTF_8), prefix + fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        context.load(source, null, NodeWrapper.IDENTITY);
    }

    public InputStream getRubyCoreInputStream(String fileName) {
        final LoadServiceResource resource = context.getRuntime().getLoadService().getClassPathResource(getClass().getClassLoader(), fileName);

        if (resource == null) {
            throw new RuntimeException("couldn't load Truffle core library " + fileName);
        }

        try {
            return resource.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeEncodingConstants() {
        getContext().getRuntime().getEncodingService().defineEncodings(new EncodingService.EncodingDefinitionVisitor() {
            @Override
            public void defineEncoding(EncodingDB.Entry encodingEntry, byte[] name, int p, int end) {
                Encoding e = encodingEntry.getEncoding();

                RubyEncoding re = RubyEncoding.newEncoding(encodingClass, e, name, p, end, encodingEntry.isDummy());
                RubyEncoding.storeEncoding(encodingEntry.getIndex(), re);
            }

            @Override
            public void defineConstant(int encodingListIndex, String constName) {
                encodingClass.setConstant(null, constName, RubyEncoding.getEncoding(encodingListIndex));
            }
        });

        getContext().getRuntime().getEncodingService().defineAliases(new EncodingService.EncodingAliasVisitor() {
            @Override
            public void defineAlias(int encodingListIndex, String constName) {
                RubyEncoding re = RubyEncoding.getEncoding(encodingListIndex);
                RubyEncoding.storeAlias(constName, re);
            }

            @Override
            public void defineConstant(int encodingListIndex, String constName) {
                encodingClass.setConstant(null, constName, RubyEncoding.getEncoding(encodingListIndex));
            }
        });
    }

    public RubyClass getMetaClass(Object object) {
        RubyNode.notDesignedForCompilation();

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

    public RubyClass getLogicalClass(Object object) {
        RubyNode.notDesignedForCompilation();

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
    public static double toDouble(Object value) {
        RubyNode.notDesignedForCompilation();

        assert value != null;

        if (value instanceof RubyNilClass) {
            return 0;
        }

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof RubyBignum) {
            return ((RubyBignum) value).bigIntegerValue().doubleValue();
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

    public RubyException runtimeError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(runtimeErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException frozenError(String className, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return runtimeError(String.format("can't modify frozen %s", className), currentNode);
    }

    public RubyException argumentError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(argumentErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException argumentErrorOutOfRange(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError("out of range", currentNode);
    }

    public RubyException argumentErrorInvalidRadix(int radix, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("invalid radix %d", radix), currentNode);
    }

    public RubyException argumentErrorMissingKeyword(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("missing keyword: %s", name), currentNode);
    }

    public RubyException argumentError(int passed, int required, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("wrong number of arguments (%d for %d)", passed, required), currentNode);
    }

    public RubyException argumentError(int passed, int required, int optional, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("wrong number of arguments (%d for %d..%d)", passed, required, required + optional), currentNode);
    }

    public RubyException argumentErrorEmptyVarargs(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError("wrong number of arguments (0 for 1+)", currentNode);
    }

    public RubyException errnoError(int errno, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();

        Errno errnoObj = Errno.valueOf(errno);
        if (errnoObj == null) {
            return systemCallError(String.format("Unknown Error (%s)", errno), currentNode);
        }

        // TODO (nirvdrum 03-Apr-15): This should return the correct errno exception class.
        return systemCallError(errnoObj.description(), currentNode);
    }

    public RubyException indexError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(indexErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException indexTooSmallError(String type, int index, int length, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return indexError(String.format("index %d too small for %s; minimum: -%d", index, type, length), currentNode);
    }

    public RubyException indexNegativeLength(int length, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return indexError(String.format("negative length (%d)", length), currentNode);
    }

    public RubyException localJumpError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(localJumpErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException unexpectedReturn(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return localJumpError("unexpected return", currentNode);
    }

    public RubyException noBlockToYieldTo(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return localJumpError("no block given (yield)", currentNode);
    }

    public RubyException typeError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(typeErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException typeErrorCantDefineSingleton(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError("can't define singleton", currentNode);
    }

    public RubyException typeErrorNoClassToMakeAlias(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError("no class to make alias", currentNode);
    }

    public RubyException typeErrorShouldReturn(String object, String method, String expectedType, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("%s#%s should return %s", object, method, expectedType), currentNode);
    }

    public RubyException typeErrorCantConvertTo(Object from, RubyClass to, String methodUsed, Object result, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String fromClass = getLogicalClass(from).getName();
        return typeError(String.format("can't convert %s to %s (%s#%s gives %s)",
                fromClass, to.getName(), fromClass, methodUsed, getLogicalClass(result).toString()), currentNode);
    }

    public RubyException typeErrorCantConvertInto(Object from, RubyClass to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("can't convert %s into %s", getLogicalClass(from).getName(), to.getName()), currentNode);
    }

    public RubyException typeErrorIsNotA(String value, String expectedType, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("%s is not a %s", value, expectedType), currentNode);
    }

    public RubyException typeErrorNoImplicitConversion(Object from, String to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("no implicit conversion of %s into %s", getLogicalClass(from).getName(), to), currentNode);
    }

    public RubyException typeErrorMustBe(String variable, String type, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("value of %s must be %s", variable, type), currentNode);
    }

    public RubyException typeErrorBadCoercion(Object from, String to, String coercionMethod, Object coercedTo, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String badClassName = getLogicalClass(from).getName();
        return typeError(String.format("can't convert %s to %s (%s#%s gives %s)",
                badClassName,
                to,
                badClassName,
                coercionMethod,
                getLogicalClass(coercedTo).getName()), currentNode);
    }

    public RubyException typeErrorCantCoerce(Object from, String to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("%s can't be coerced into %s", from, to), currentNode);
    }

    public RubyException nameError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(nameErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException nameErrorConstantNotDefined(RubyModule module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("constant %s::%s not defined", module.getName(), name), currentNode);
    }

    public RubyException nameErrorUninitializedConstant(RubyModule module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("uninitialized constant %s::%s", module.getName(), name), currentNode);
    }

    public RubyException nameErrorPrivateConstant(RubyModule module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("private constant %s::%s referenced", module.getName(), name), currentNode);
    }

    public RubyException nameErrorInstanceNameNotAllowable(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("`%s' is not allowable as an instance variable name", name), currentNode);
    }

    public RubyException nameErrorReadOnly(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("%s is a read-only variable", name), currentNode);
    }

    public RubyException nameErrorUndefinedLocalVariableOrMethod(String name, String object, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("undefined local variable or method `%s' for %s", name, object), currentNode);
    }

    public RubyException nameErrorUndefinedMethod(String name, String object, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("undefined method `%s' for %s", name, object), currentNode);
    }

    public RubyException noMethodError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(context.getCoreLibrary().getNoMethodErrorClass(), context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException noMethodErrorOnModule(String name, RubyModule module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return noMethodError(String.format("undefined method `%s' for %s", name, module.getName()), currentNode);
    }

    public RubyException noMethodErrorOnReceiver(String name, Object receiver, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        RubyClass logicalClass = getLogicalClass(receiver);
        String repr = logicalClass.getName();
        if (receiver instanceof RubyModule) {
            repr = ((RubyModule) receiver).getName() + ":" + repr;
        }
        return noMethodError(String.format("undefined method `%s' for %s", name, repr), currentNode);
    }

    public RubyException privateMethodError(String name, RubyModule module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return noMethodError(String.format("private method `%s' called for %s", name, module.toString()), currentNode);
    }

    public RubyException loadError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(context.getCoreLibrary().getLoadErrorClass(), context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException loadErrorCannotLoad(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return loadError(String.format("cannot load such file -- %s", name), currentNode);
    }

    public RubyException zeroDivisionError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(context.getCoreLibrary().getZeroDivisionErrorClass(), context.makeString("divided by 0"), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException syntaxError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(syntaxErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException floatDomainError(String value, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(floatDomainErrorClass, context.makeString(value), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException mathDomainError(String method, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(edomClass, context.makeString(String.format("Numerical argument is out of domain - \"%s\"", method)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException invalidArgumentError(String value, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(einvalClass, context.makeString(String.format("Invalid argument -  %s", value)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException ioError(String fileName, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(ioErrorClass, context.makeString(String.format("Error reading file -  %s", fileName)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException fileNotFoundError(String fileName, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(enoentClass, context.makeString(String.format("No such file or directory -  %s", fileName)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException dirNotEmptyError(String path, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(enotemptyClass, context.makeString(String.format("Directory not empty - %s", path)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException rangeError(int code, RubyEncoding encoding, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return rangeError(String.format("invalid codepoint %x in %s", code, encoding.getEncoding()), currentNode);
    }

    public RubyException rangeError(String type, String value, String range, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return rangeError(String.format("%s %s out of range of %s", type, value, range), currentNode);
    }

    public RubyException rangeError(RubyRange.IntegerFixnumRange range, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return rangeError(String.format("%d..%s%d out of range",
                range.getBegin(),
                range.doesExcludeEnd() ? "." : "",
                range.getEnd()), currentNode);
    }

    public RubyException rangeError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(rangeErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException internalError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(context.getCoreLibrary().getRubyTruffleErrorClass(), context.makeString("internal implementation error - " + message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException regexpError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(regexpErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException encodingCompatibilityErrorIncompatible(String a, String b, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return encodingCompatibilityError(String.format("incompatible character encodings: %s and %s", a, b), currentNode);
    }

    public RubyException encodingCompatibilityError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(encodingCompatibilityErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException fiberError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(fiberErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException deadFiberCalledError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return fiberError("dead fiber called", currentNode);
    }

    public RubyException yieldFromRootFiberError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return fiberError("can't yield from root fiber", currentNode);
    }

    public RubyException threadError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(threadErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException systemCallError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(systemCallErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyContext getContext() {
        return context;
    }

    public RubyClass getArrayClass() {
        return arrayClass;
    }

    public RubyClass getBasicObjectClass() {
        return basicObjectClass;
    }

    public RubyClass getBignumClass() {
        return bignumClass;
    }

    public RubyClass getBindingClass() {
        return bindingClass;
    }

    public RubyClass getClassClass() {
        return classClass;
    }

    public RubyClass getExceptionClass() { return exceptionClass; }

    public RubyClass getFalseClass() {
        return falseClass;
    }

    public RubyClass getFiberClass() {
        return fiberClass;
    }

    public RubyClass getFileClass() {
        return fileClass;
    }

    public RubyClass getFixnumClass() {
        return fixnumClass;
    }

    public RubyClass getFloatClass() {
        return floatClass;
    }

    public RubyClass getHashClass() {
        return hashClass;
    }

    public RubyClass getLoadErrorClass() {
        return loadErrorClass;
    }

    public RubyClass getMatchDataClass() {
        return matchDataClass;
    }

    public RubyClass getModuleClass() {
        return moduleClass;
    }

    public RubyClass getNameErrorClass() {
        return nameErrorClass;
    }

    public RubyClass getNilClass() {
        return nilClass;
    }

    public RubyClass getNoMethodErrorClass() {
        return noMethodErrorClass;
    }

    public RubyClass getObjectClass() {
        return objectClass;
    }

    public RubyClass getProcClass() {
        return procClass;
    }

    public RubyClass getRangeClass() {
        return rangeClass;
    }

    public RubyClass getRationalClass() {
        return rationalClass;
    }

    public RubyClass getRegexpClass() {
        return regexpClass;
    }

    public RubyClass getRubyTruffleErrorClass() {
        return rubyTruffleErrorClass;
    }

    public RubyClass getRuntimeErrorClass() {
        return runtimeErrorClass;
    }

    public RubyClass getStringClass() {
        return stringClass;
    }

    public RubyClass getEncodingClass(){ return encodingClass; }

    public RubyClass getSymbolClass() {
        return symbolClass;
    }

    public RubyClass getSyntaxErrorClass() {
        return syntaxErrorClass;
    }

    public RubyClass getThreadClass() {
        return threadClass;
    }

    public RubyClass getTimeClass() {
        return timeClass;
    }

    public RubyClass getTypeErrorClass() { return typeErrorClass; }

    public RubyClass getTrueClass() {
        return trueClass;
    }

    public RubyClass getZeroDivisionErrorClass() {
        return zeroDivisionErrorClass;
    }

    public RubyModule getKernelModule() {
        return kernelModule;
    }

    public RubyArray getArgv() {
        return argv;
    }

    public RubyBasicObject getGlobalVariablesObject() {
        return globalVariablesObject;
    }

    public RubyArray getLoadPath() {
        return (RubyArray) globalVariablesObject.getInstanceVariable("$LOAD_PATH");
    }

    public RubyArray getLoadedFeatures() {
        return (RubyArray) globalVariablesObject.getInstanceVariable("$LOADED_FEATURES");
    }

    public RubyBasicObject getMainObject() {
        return mainObject;
    }

    public RubyNilClass getNilObject() {
        return nilObject;
    }

    public RubyHash getENV() {
        return envHash;
    }

    public RubyEncoding getDefaultEncoding() { return RubyEncoding.getEncoding("US-ASCII"); }

    private RubyHash getSystemEnv() {
        final List<KeyValue> entries = new ArrayList<>();

        for (Map.Entry<String, String> variable : System.getenv().entrySet()) {
            entries.add(new KeyValue(context.makeString(variable.getKey()), context.makeString(variable.getValue())));
        }

        return HashOperations.verySlowFromEntries(context, entries, false);
    }

    public ArrayNodes.MinBlock getArrayMinBlock() {
        return arrayMinBlock;
    }

    public ArrayNodes.MaxBlock getArrayMaxBlock() {
        return arrayMaxBlock;
    }

    public RubyClass getNumericClass() {
        return numericClass;
    }

    public RubyClass getIntegerClass() {
        return integerClass;
    }

    public RubyClass getArgumentErrorClass() {
        return argumentErrorClass;
    }

    public RubyClass getEncodingConverterClass() {
        return encodingConverterClass;
    }

    public RubyClass getUnboundMethodClass() {
        return unboundMethodClass;
    }

    public RubyClass getMethodClass() {
        return methodClass;
    }

    public RubyClass getComplexClass() {
        return complexClass;
    }

    public RubyClass getByteArrayClass() {
        return byteArrayClass;
    }

    public RubyClass getLookupTableClass() {
        return lookupTableClass;
    }

    public RubyClass getStringDataClass() {
        return stringDataClass;
    }

    public RubyClass getTranscodingClass() {
        return transcodingClass;
    }

    public RubyClass getTupleClass() {
        return tupleClass;
    }

    public RubyBasicObject getRubiniusUndefined() {
        return rubiniusUndefined;
    }

    public RubySymbol getEachSymbol() {
        return eachSymbol;
    }

    public RubySymbol getMapBangSymbol() {
        return mapBangSymbol;
    }

    public RubySymbol getMapSymbol() {
        return mapSymbol;
    }

    public boolean isLoadingRubyCore() {
        return state == State.LOADING_RUBY_CORE;
    }

    public boolean isLoaded() {
        return state == State.LOADED;
    }

    public RubyClass getEnoentClass() {
        return enoentClass;
    }
}
