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

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.Constants;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.runtime.load.LoadServiceResource;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ArrayNodes;
import org.jruby.truffle.nodes.methods.SetMethodDeclarationContext;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.truffle.translator.TranslatorDriver;
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

    @CompilerDirectives.CompilationFinal private RubyClass argumentErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass arrayClass;
    @CompilerDirectives.CompilationFinal private RubyClass basicObjectClass;
    @CompilerDirectives.CompilationFinal private RubyClass bignumClass;
    @CompilerDirectives.CompilationFinal private RubyClass bindingClass;
    @CompilerDirectives.CompilationFinal private RubyClass classClass;
    @CompilerDirectives.CompilationFinal private RubyClass complexClass;
    @CompilerDirectives.CompilationFinal private RubyClass dirClass;
    @CompilerDirectives.CompilationFinal private RubyClass encodingClass;
    @CompilerDirectives.CompilationFinal private RubyClass exceptionClass;
    @CompilerDirectives.CompilationFinal private RubyClass falseClass;
    @CompilerDirectives.CompilationFinal private RubyClass fiberClass;
    @CompilerDirectives.CompilationFinal private RubyClass fileClass;
    @CompilerDirectives.CompilationFinal private RubyClass fixnumClass;
    @CompilerDirectives.CompilationFinal private RubyClass floatClass;
    @CompilerDirectives.CompilationFinal private RubyClass floatDomainErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass hashClass;
    @CompilerDirectives.CompilationFinal private RubyClass integerClass;
    @CompilerDirectives.CompilationFinal private RubyClass indexErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass ioClass;
    @CompilerDirectives.CompilationFinal private RubyClass keyErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass loadErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass localJumpErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass matchDataClass;
    @CompilerDirectives.CompilationFinal private RubyClass moduleClass;
    @CompilerDirectives.CompilationFinal private RubyClass nameErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass nilClass;
    @CompilerDirectives.CompilationFinal private RubyClass noMethodErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass numericClass;
    @CompilerDirectives.CompilationFinal private RubyClass objectClass;
    @CompilerDirectives.CompilationFinal private RubyClass procClass;
    @CompilerDirectives.CompilationFinal private RubyClass processClass;
    @CompilerDirectives.CompilationFinal private RubyClass rangeClass;
    @CompilerDirectives.CompilationFinal private RubyClass rangeErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass rationalClass;
    @CompilerDirectives.CompilationFinal private RubyClass regexpClass;
    @CompilerDirectives.CompilationFinal private RubyClass regexpErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass rubyTruffleErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass runtimeErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass standardErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass stringClass;
    @CompilerDirectives.CompilationFinal private RubyClass symbolClass;
    @CompilerDirectives.CompilationFinal private RubyClass syntaxErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass systemCallErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass systemExitClass;
    @CompilerDirectives.CompilationFinal private RubyClass threadClass;
    @CompilerDirectives.CompilationFinal private RubyClass timeClass;
    @CompilerDirectives.CompilationFinal private RubyClass trueClass;
    @CompilerDirectives.CompilationFinal private RubyClass typeErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass zeroDivisionErrorClass;
    @CompilerDirectives.CompilationFinal private RubyModule configModule;
    @CompilerDirectives.CompilationFinal private RubyModule enumerableModule;
    @CompilerDirectives.CompilationFinal private RubyModule errnoModule;
    @CompilerDirectives.CompilationFinal private RubyModule gcModule;
    @CompilerDirectives.CompilationFinal private RubyModule kernelModule;
    @CompilerDirectives.CompilationFinal private RubyModule mathModule;
    @CompilerDirectives.CompilationFinal private RubyModule objectSpaceModule;
    @CompilerDirectives.CompilationFinal private RubyModule signalModule;
    @CompilerDirectives.CompilationFinal private RubyModule truffleModule;
    @CompilerDirectives.CompilationFinal private RubyModule truffleDebugModule;
    @CompilerDirectives.CompilationFinal private RubyClass edomClass;
    @CompilerDirectives.CompilationFinal private RubyClass encodingConverterClass;
    @CompilerDirectives.CompilationFinal private RubyClass encodingCompatibilityErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass methodClass;
    @CompilerDirectives.CompilationFinal private RubyClass unboundMethodClass;
    @CompilerDirectives.CompilationFinal private RubyClass byteArrayClass;
    @CompilerDirectives.CompilationFinal private RubyClass fiberErrorClass;
    @CompilerDirectives.CompilationFinal private RubyClass threadErrorClass;

    @CompilerDirectives.CompilationFinal private RubyArray argv;
    @CompilerDirectives.CompilationFinal private RubyBasicObject globalVariablesObject;
    @CompilerDirectives.CompilationFinal private RubyBasicObject mainObject;
    @CompilerDirectives.CompilationFinal private RubyNilClass nilObject;
    @CompilerDirectives.CompilationFinal private RubyHash envHash;
    @CompilerDirectives.CompilationFinal private RubyBasicObject rubiniusUndefined;

    private ArrayNodes.MinBlock arrayMinBlock;
    private ArrayNodes.MaxBlock arrayMaxBlock;

    @CompilerDirectives.CompilationFinal private RubySymbol eachSymbol;

    public CoreLibrary(RubyContext context) {
        this.context = context;
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
            return ((RubyBignum) value).doubleValue();
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

    public void initialize() {
        // Create the cyclic classes and modules

        classClass = new RubyClass(context, null, null, "Class", false);
        classClass.setAllocator(new RubyClass.ClassAllocator());

        basicObjectClass = RubyClass.createBootClass(context, "BasicObject");
        objectClass = RubyClass.createBootClass(context, "Object");

        moduleClass = new RubyClass(context, null, null, "Module", false);
        moduleClass.setAllocator(new RubyModule.ModuleAllocator());

        // Close the cycles
        classClass.unsafeSetLogicalClass(classClass);

        objectClass.unsafeSetSuperclass(basicObjectClass);
        moduleClass.unsafeSetSuperclass(objectClass);
        classClass.unsafeSetSuperclass(moduleClass);

        classClass.getAdoptedByLexicalParent(objectClass, null);
        basicObjectClass.getAdoptedByLexicalParent(objectClass, null);
        objectClass.getAdoptedByLexicalParent(objectClass, null);
        moduleClass.getAdoptedByLexicalParent(objectClass, null);

        // BasicObject knows itself

        basicObjectClass.setConstant(null, "BasicObject", basicObjectClass);

        // Create classes and modules

        createExceptionClasses();

        numericClass = defineClass("Numeric");
        complexClass = defineClass(numericClass, "Complex");
        floatClass = defineClass(numericClass, "Float");
        integerClass = defineClass(numericClass, "Integer");
        fixnumClass = defineClass(integerClass, "Fixnum");
        bignumClass = defineClass(integerClass, "Bignum", new RubyBignum.BignumAllocator());
        rationalClass = defineClass(numericClass, "Rational");

        ioClass = defineClass("IO", new RubyBasicObject.BasicObjectAllocator());
        fileClass = defineClass(ioClass, "File");

        // Classes defined in Object

        arrayClass = defineClass("Array", new RubyArray.ArrayAllocator());
        bindingClass = defineClass("Binding");
        dirClass = defineClass("Dir");
        encodingClass = defineClass("Encoding", new RubyEncoding.EncodingAllocator());
        falseClass = defineClass("FalseClass");
        fiberClass = defineClass("Fiber", new RubyFiber.FiberAllocator());
        hashClass = defineClass("Hash", new RubyHash.HashAllocator());
        matchDataClass = defineClass("MatchData");
        methodClass = defineClass("Method");
        defineClass("Mutex", new RubyMutex.MutexAllocator());
        nilClass = defineClass("NilClass");
        procClass = defineClass("Proc", new RubyProc.ProcAllocator());
        processClass = defineClass("Process");
        rangeClass = defineClass("Range", new RubyRange.RangeAllocator());
        regexpClass = defineClass("Regexp", new RubyRegexp.RegexpAllocator());
        stringClass = defineClass("String", new RubyString.StringAllocator());
        symbolClass = defineClass("Symbol");
        threadClass = defineClass("Thread", new RubyThread.ThreadAllocator());
        timeClass = defineClass("Time", new RubyTime.TimeAllocator());
        trueClass = defineClass("TrueClass");
        unboundMethodClass = defineClass("UnboundMethod");

        // Modules

        RubyModule comparableModule = new RubyModule(context, objectClass, "Comparable");
        configModule = new RubyModule(context, objectClass, "Config");
        enumerableModule = new RubyModule(context, objectClass, "Enumerable");
        gcModule = new RubyModule(context, objectClass, "GC");
        kernelModule = new RubyModule(context, objectClass, "Kernel");
        mathModule = new RubyModule(context, objectClass, "Math");
        objectSpaceModule = new RubyModule(context, objectClass, "ObjectSpace");
        signalModule = new RubyModule(context, objectClass, "Signal");

        // The rest

        encodingCompatibilityErrorClass = new RubyClass(context, encodingClass, standardErrorClass, "CompatibilityError");

        encodingConverterClass = new RubyClass(context, encodingClass, objectClass, "Converter");
        encodingConverterClass.setAllocator(new RubyEncodingConverter.EncodingConverterAllocator());

        truffleModule = new RubyModule(context, objectClass, "Truffle");
        truffleDebugModule = new RubyModule(context, truffleModule, "Debug");
        new RubyModule(context, truffleModule, "Primitive");

        final RubyModule rubiniusModule = new RubyModule(context, objectClass, "Rubinius");
        rubiniusUndefined = new RubyBasicObject(objectClass);
        rubiniusModule.setConstant(null, "UNDEFINED", rubiniusUndefined);
        byteArrayClass = new RubyClass(context, rubiniusModule, objectClass, "ByteArray");

        // Includes

        objectClass.include(null, kernelModule);

        numericClass.include(null, comparableModule);
        stringClass.include(null, comparableModule);
        symbolClass.include(null, comparableModule);

        arrayClass.include(null, enumerableModule);
        dirClass.include(null, enumerableModule);
        hashClass.include(null, enumerableModule);
        ioClass.include(null, enumerableModule);
        rangeClass.include(null, enumerableModule);

        // Set constants

        objectClass.setConstant(null, "RUBY_VERSION", RubyString.fromJavaString(stringClass, Constants.RUBY_VERSION));
        objectClass.setConstant(null, "RUBY_PATCHLEVEL", Constants.RUBY_PATCHLEVEL);
        objectClass.setConstant(null, "RUBY_ENGINE", RubyString.fromJavaString(stringClass, Constants.ENGINE + "+truffle"));
        objectClass.setConstant(null, "RUBY_PLATFORM", RubyString.fromJavaString(stringClass, Constants.PLATFORM));


        // TODO(cs): this should be a separate exception
        mathModule.setConstant(null, "DomainError", edomClass);

        // Create some key objects

        mainObject = new RubyBasicObject(objectClass);
        nilObject = new RubyNilClass(nilClass);

        // Create the globals object

        globalVariablesObject = new RubyBasicObject(objectClass);
        globalVariablesObject.getOperations().setInstanceVariable(globalVariablesObject, "$LOAD_PATH", new RubyArray(arrayClass));
        globalVariablesObject.getOperations().setInstanceVariable(globalVariablesObject, "$LOADED_FEATURES", new RubyArray(arrayClass));
        globalVariablesObject.getOperations().setInstanceVariable(globalVariablesObject, "$:", globalVariablesObject.getInstanceVariable("$LOAD_PATH"));
        globalVariablesObject.getOperations().setInstanceVariable(globalVariablesObject, "$\"", globalVariablesObject.getInstanceVariable("$LOADED_FEATURES"));
        globalVariablesObject.getOperations().setInstanceVariable(globalVariablesObject, "$,", nilObject);

        initializeEncodingConstants();

        arrayMinBlock = new ArrayNodes.MinBlock(context);
        arrayMaxBlock = new ArrayNodes.MaxBlock(context);

        argv = new RubyArray(arrayClass);
        objectClass.setConstant(null, "ARGV", argv);

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

        RubyNode.notDesignedForCompilation();
        globalVariablesObject.getOperations().setInstanceVariable(globalVariablesObject, "$DEBUG", context.getRuntime().isDebug());
        Object value = context.getRuntime().warningsEnabled() ? context.getRuntime().isVerbose() : nilObject;
        RubyNode.notDesignedForCompilation();
        globalVariablesObject.getOperations().setInstanceVariable(globalVariablesObject, "$VERBOSE", value);

        // Common symbols

        eachSymbol = getContext().getSymbolTable().getSymbol("each");
    }

    private RubyClass defineClass(String name) {
        return defineClass(objectClass, name, objectClass.getAllocator());
    }

    private RubyClass defineClass(String name, Allocator allocator) {
        return defineClass(objectClass, name, allocator);
    }

    private RubyClass defineClass(RubyClass superclass, String name) {
        return new RubyClass(context, objectClass, superclass, name);
    }

    private RubyClass defineClass(RubyClass superclass, String name, Allocator allocator) {
        RubyClass rubyClass = new RubyClass(context, objectClass, superclass, name);
        rubyClass.setAllocator(allocator);
        return rubyClass;
    }

    private void createExceptionClasses() {
        // Exception
        exceptionClass = defineClass("Exception");
        exceptionClass.setAllocator(new RubyException.ExceptionAllocator());

        // FiberError
        fiberErrorClass = defineClass(exceptionClass, "FiberError");

        // StandardError
        standardErrorClass = defineClass(exceptionClass, "StandardError");
        argumentErrorClass = defineClass(standardErrorClass, "ArgumentError");
        loadErrorClass = defineClass(standardErrorClass, "LoadError");
        localJumpErrorClass = defineClass(standardErrorClass, "LocalJumpError");
        regexpErrorClass = defineClass(standardErrorClass, "RegexpError");
        rubyTruffleErrorClass = defineClass(standardErrorClass, "RubyTruffleError");
        runtimeErrorClass = defineClass(standardErrorClass, "RuntimeError");
        typeErrorClass = defineClass(standardErrorClass, "TypeError");
        zeroDivisionErrorClass = defineClass(standardErrorClass, "ZeroDivisionError");

        // StandardError > RangeError
        rangeErrorClass = defineClass(standardErrorClass, "RangeError");
        floatDomainErrorClass = defineClass(rangeErrorClass, "FloatDomainError");

        // StandardError > IndexError
        indexErrorClass = defineClass(standardErrorClass, "IndexError");
        keyErrorClass = defineClass(indexErrorClass, "KeyError");

        // StandardError > NameError
        nameErrorClass = defineClass(standardErrorClass, "NameError");
        noMethodErrorClass = defineClass(nameErrorClass, "NoMethodError");

        // StandardError > SystemCallError
        systemCallErrorClass = defineClass(standardErrorClass, "SystemCallError");
        errnoModule = new RubyModule(context, objectClass, "Errno");
        new RubyClass(context, errnoModule, systemCallErrorClass, "EACCES");
        edomClass = new RubyClass(context, errnoModule, systemCallErrorClass, "EDOM");
        new RubyClass(context, errnoModule, systemCallErrorClass, "EEXIST");
        new RubyClass(context, errnoModule, systemCallErrorClass, "ENOENT");
        new RubyClass(context, errnoModule, systemCallErrorClass, "ENOTEMPTY");
        new RubyClass(context, errnoModule, systemCallErrorClass, "EPERM");
        new RubyClass(context, errnoModule, systemCallErrorClass, "EXDEV");

        // ScriptError
        RubyClass scriptErrorClass = defineClass(exceptionClass, "ScriptError");
        syntaxErrorClass = defineClass(scriptErrorClass, "SyntaxError");

        // SignalException
        RubyClass signalExceptionClass = defineClass(exceptionClass, "SignalException");
        defineClass(signalExceptionClass, "Interrupt");

        // SystemExit
        systemExitClass = defineClass(exceptionClass, "SystemExit");

        // ThreadError
        threadErrorClass = defineClass(exceptionClass, "ThreadError");
    }

    public void initializeAfterMethodsAdded() {
        objectClass.setConstant(null, "RUBY_RELEASE_DATE", context.makeString(Constants.COMPILE_DATE));
        objectClass.setConstant(null, "RUBY_DESCRIPTION", context.makeString(OutputStrings.getVersionString()));

        if (Options.TRUFFLE_LOAD_CORE.load()) {
            try {
                loadRubyCore("jruby/truffle/core.rb");
            } catch (RaiseException e) {
                final RubyException rubyException = e.getRubyException();

                for (String line : Backtrace.DISPLAY_FORMATTER.format(getContext(), rubyException, rubyException.getBacktrace())) {
                    System.err.println(line);
                }

                throw new TruffleFatalException("couldn't load the core library", e);
            }
        }

        // ENV is supposed to be an object that actually updates the environment, and sees any updates

        envHash = getSystemEnv();
        objectClass.setConstant(null, "ENV", envHash);
    }

    public void loadRubyCore(String fileName) {
        final Source source;

        try {
            source = Source.fromReader(new InputStreamReader(getRubyCoreInputStream(fileName), StandardCharsets.UTF_8), "core:/" + fileName);
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

                RubyEncoding re = RubyEncoding.newEncoding(getContext(), e, name, p, end, encodingEntry.isDummy());
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
            throw new RuntimeException();
        } else {
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
            throw new UnsupportedOperationException(String.format("Don't know how to get the logical class for %s", object.getClass()));
        }
    }

    public RubyException runtimeError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(runtimeErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException frozenError(String className, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return runtimeError(String.format("FrozenError: can't modify frozen %s", className), currentNode);
    }

    public RubyException argumentError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(argumentErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
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
        return nameError(String.format("undefined local variable or method `%s' for %s", name, object), currentNode);
    }

    public RubyException noMethodError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(context.getCoreLibrary().getNoMethodErrorClass(), context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException noMethodError(String name, RubyModule module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return noMethodError(String.format("undefined method `%s' for %s", name, module.getName()), currentNode);
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

        return HashOperations.verySlowFromEntries(context, entries);
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

    public RubyBasicObject getRubiniusUndefined() {
        return rubiniusUndefined;
    }

    public RubySymbol getEachSymbol() {
        return eachSymbol;
    }
}
