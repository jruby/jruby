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
    @CompilerDirectives.CompilationFinal private RubyClass continuationClass;
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
    @CompilerDirectives.CompilationFinal private RubyModule comparableModule;
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

    @CompilerDirectives.CompilationFinal private RubyArray argv;
    @CompilerDirectives.CompilationFinal private RubyBasicObject globalVariablesObject;
    @CompilerDirectives.CompilationFinal private RubyBasicObject mainObject;
    @CompilerDirectives.CompilationFinal private RubyNilClass nilObject;
    @CompilerDirectives.CompilationFinal private RubyHash envHash;

    private ArrayNodes.MinBlock arrayMinBlock;
    private ArrayNodes.MaxBlock arrayMaxBlock;

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

        // Create all other classes and modules

        numericClass = new RubyClass(context, objectClass, objectClass, "Numeric");
        integerClass = new RubyClass(context, objectClass, numericClass, "Integer");

        exceptionClass = new RubyClass(context, objectClass, objectClass, "Exception");
        exceptionClass.setAllocator(new RubyException.ExceptionAllocator());

        standardErrorClass = new RubyClass(context, objectClass, exceptionClass, "StandardError");
        standardErrorClass.setAllocator(new RubyException.ExceptionAllocator());

        RubyClass signalExceptionClass = new RubyClass(context, objectClass, exceptionClass, "SignalException");
        signalExceptionClass.setAllocator(new RubyException.ExceptionAllocator());

        RubyClass interruptClass = new RubyClass(context, objectClass, signalExceptionClass, "Interrupt");
        interruptClass.setAllocator(new RubyException.ExceptionAllocator());

        ioClass = new RubyClass(context, objectClass, objectClass, "IO");

        argumentErrorClass = new RubyClass(context, objectClass, standardErrorClass, "ArgumentError");
        argumentErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        arrayClass = new RubyClass(context, objectClass, objectClass, "Array");
        arrayClass.setAllocator(new RubyArray.ArrayAllocator());
        bignumClass = new RubyClass(context, objectClass, integerClass, "Bignum");
        bignumClass.setAllocator(new RubyBignum.BignumAllocator());
        bindingClass = new RubyClass(context, objectClass, objectClass, "Binding");
        comparableModule = new RubyModule(context, objectClass, "Comparable");
        complexClass = new RubyClass(context, objectClass, numericClass, "Complex");
        configModule = new RubyModule(context, objectClass, "Config");
        continuationClass = new RubyClass(context, objectClass, objectClass, "Continuation");
        dirClass = new RubyClass(context, objectClass, objectClass, "Dir");
        encodingClass = new RubyClass(context, objectClass, objectClass, "Encoding");
        encodingClass.setAllocator(new RubyEncoding.EncodingAllocator());
        errnoModule = new RubyModule(context, objectClass, "Errno");
        enumerableModule = new RubyModule(context, objectClass, "Enumerable");
        falseClass = new RubyClass(context, objectClass, objectClass, "FalseClass");
        fiberClass = new RubyClass(context, objectClass, objectClass, "Fiber");
        fileClass = new RubyClass(context, objectClass, ioClass, "File");
        fixnumClass = new RubyClass(context, objectClass, integerClass, "Fixnum");
        floatClass = new RubyClass(context, objectClass, numericClass, "Float");
        floatDomainErrorClass = new RubyClass(context, objectClass, rangeErrorClass, "FloatDomainError");
        floatDomainErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        gcModule = new RubyModule(context, objectClass, "GC");
        hashClass = new RubyClass(context, objectClass, objectClass, "Hash");
        hashClass.setAllocator(new RubyHash.HashAllocator());
        indexErrorClass = new RubyClass(context, objectClass, standardErrorClass, "IndexError");
        indexErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        kernelModule = new RubyModule(context, objectClass, "Kernel");
        keyErrorClass = new RubyClass(context, objectClass, indexErrorClass, "KeyError");
        keyErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        loadErrorClass = new RubyClass(context, objectClass, standardErrorClass, "LoadError");
        loadErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        localJumpErrorClass = new RubyClass(context, objectClass, standardErrorClass, "LocalJumpError");
        localJumpErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        matchDataClass = new RubyClass(context, objectClass, objectClass, "MatchData");
        mathModule = new RubyModule(context, objectClass, "Math");
        nameErrorClass = new RubyClass(context, objectClass, standardErrorClass, "NameError");
        nameErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        nilClass = new RubyClass(context, objectClass, objectClass, "NilClass");
        noMethodErrorClass = new RubyClass(context, objectClass, nameErrorClass, "NoMethodError");
        noMethodErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        objectSpaceModule = new RubyModule(context, objectClass, "ObjectSpace");
        procClass = new RubyClass(context, objectClass, objectClass, "Proc");
        procClass.setAllocator(new RubyProc.ProcAllocator());
        processClass = new RubyClass(context, objectClass, objectClass, "Process");
        rangeClass = new RubyClass(context, objectClass, objectClass, "Range");
        rangeErrorClass = new RubyClass(context, objectClass, standardErrorClass, "RangeError");
        rangeErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        rationalClass = new RubyClass(context, objectClass, numericClass, "Rational");
        regexpClass = new RubyClass(context, objectClass, objectClass, "Regexp");
        regexpClass.setAllocator(new RubyRegexp.RegexpAllocator());
        regexpErrorClass = new RubyClass(context, objectClass, standardErrorClass, "RegexpError");
        regexpErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        rubyTruffleErrorClass = new RubyClass(context, objectClass, standardErrorClass, "RubyTruffleError");
        rubyTruffleErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        runtimeErrorClass = new RubyClass(context, objectClass, standardErrorClass, "RuntimeError");
        runtimeErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        signalModule = new RubyModule(context, objectClass, "Signal");
        stringClass = new RubyClass(context, objectClass, objectClass, "String");
        stringClass.setAllocator(new RubyString.StringAllocator());
        symbolClass = new RubyClass(context, objectClass, objectClass, "Symbol");
        syntaxErrorClass = new RubyClass(context, objectClass, standardErrorClass, "SyntaxError");
        syntaxErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        systemCallErrorClass = new RubyClass(context, objectClass, standardErrorClass, "SystemCallError");
        systemCallErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        systemExitClass = new RubyClass(context, objectClass, exceptionClass, "SystemExit");
        systemExitClass.setAllocator(new RubyException.ExceptionAllocator());
        threadClass = new RubyClass(context, objectClass, objectClass, "Thread");
        threadClass.setAllocator(new RubyThread.ThreadAllocator());
        timeClass = new RubyClass(context, objectClass, objectClass, "Time");
        timeClass.setAllocator(new RubyTime.TimeAllocator());
        trueClass = new RubyClass(context, objectClass, objectClass, "TrueClass");
        truffleModule = new RubyModule(context, objectClass, "Truffle");
        truffleDebugModule = new RubyModule(context, truffleModule, "Debug");
        new RubyModule(context, truffleModule, "Primitive");
        typeErrorClass = new RubyClass(context, objectClass, standardErrorClass, "TypeError");
        typeErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        zeroDivisionErrorClass = new RubyClass(context, objectClass, standardErrorClass, "ZeroDivisionError");
        zeroDivisionErrorClass.setAllocator(new RubyException.ExceptionAllocator());
        encodingConverterClass = new RubyClass(context, encodingClass, objectClass, "Converter");
        encodingConverterClass.setAllocator(new RubyEncodingConverter.EncodingConverterAllocator());
        methodClass = new RubyClass(context, objectClass, objectClass, "Method");
        unboundMethodClass = new RubyClass(context, objectClass, objectClass, "UnboundMethod");

        encodingCompatibilityErrorClass = new RubyClass(context, encodingClass, standardErrorClass, "CompatibilityError");
        encodingCompatibilityErrorClass.setAllocator(new RubyException.ExceptionAllocator());

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

        edomClass = new RubyClass(context, errnoModule, systemCallErrorClass, "EDOM");
        edomClass.setAllocator(new RubyException.ExceptionAllocator());

        RubyClass tempClass;

        tempClass = new RubyClass(context, errnoModule, systemCallErrorClass, "ENOENT");
        tempClass.setAllocator(new RubyException.ExceptionAllocator());
        tempClass = new RubyClass(context, errnoModule, systemCallErrorClass, "EPERM");
        tempClass.setAllocator(new RubyException.ExceptionAllocator());
        tempClass = new RubyClass(context, errnoModule, systemCallErrorClass, "ENOTEMPTY");
        tempClass.setAllocator(new RubyException.ExceptionAllocator());
        tempClass = new RubyClass(context, errnoModule, systemCallErrorClass, "EEXIST");
        tempClass.setAllocator(new RubyException.ExceptionAllocator());
        tempClass = new RubyClass(context, errnoModule, systemCallErrorClass, "EXDEV");
        tempClass.setAllocator(new RubyException.ExceptionAllocator());
        tempClass = new RubyClass(context, errnoModule, systemCallErrorClass, "EACCES");
        tempClass.setAllocator(new RubyException.ExceptionAllocator());

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

        fileClass.setConstant(null, "SEPARATOR", RubyString.fromJavaString(stringClass, File.separator));
        fileClass.setConstant(null, "Separator", RubyString.fromJavaString(stringClass, File.separator));
        fileClass.setConstant(null, "ALT_SEPARATOR", nilObject);
        fileClass.setConstant(null, "PATH_SEPARATOR", RubyString.fromJavaString(stringClass, File.pathSeparator));
        fileClass.setConstant(null, "FNM_SYSCASE", 0);

        RubyNode.notDesignedForCompilation();
        globalVariablesObject.getOperations().setInstanceVariable(globalVariablesObject, "$DEBUG", context.getRuntime().isDebug());
        Object value = context.getRuntime().warningsEnabled() ? context.getRuntime().isVerbose() : nilObject;
        RubyNode.notDesignedForCompilation();
        globalVariablesObject.getOperations().setInstanceVariable(globalVariablesObject, "$VERBOSE", value);
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
            final LoadServiceResource resource = context.getRuntime().getLoadService().getClassPathResource(context.getRuntime().getJRubyClassLoader(), fileName);

            if (resource == null) {
                throw new RuntimeException("couldn't load Truffle core library " + fileName);
            }

            source = Source.fromReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8), "core:/" + fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        context.load(source, null, NodeWrapper.IDENTITY);
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

    public RubyException typeErrorCantConvertTo(String from, String to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("can't convert %s to %s", from, to), currentNode);
    }

    public RubyException typeErrorCantConvertTo(String from, String to, String methodUsed, String given, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("can't convert %s to %s (%s#%s gives %s)", from, to, from, methodUsed, given), currentNode);
    }

    public RubyException typeErrorCantConvertInto(String from, String to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("can't convert %s into %s", from, to), currentNode);
    }

    public RubyException typeErrorCantConvertInto(Object from, RubyClass to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeErrorCantConvertInto(getLogicalClass(from).getName(), to.getName(), currentNode);
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

    public RubyException noMethodError(String name, String object, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return noMethodError(String.format("undefined method `%s' for %s", name, object), currentNode);
    }

    public RubyException privateMethodError(String name, String object, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return noMethodError(String.format("private method `%s' called for %s", name, object), currentNode);
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

    public RubyException rangeError(String type, String value, String range, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(rangeErrorClass, context.makeString(String.format("%s %s out of range of %s", type, value, range)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException rangeError(RubyRange.IntegerFixnumRange range, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return new RubyException(rangeErrorClass, context.makeString(String.format("%d..%s%d out of range",
                    range.getBegin(),
                    range.doesExcludeEnd() ? "." : "",
                    range.getEnd())),
                RubyCallStack.getBacktrace(currentNode));
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

    public RubyClass getContinuationClass() {
        return continuationClass;
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
}
