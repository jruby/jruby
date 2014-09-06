/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import org.jcodings.specific.EUCJPEncoding;
import org.jcodings.specific.SJISEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.runtime.Constants;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ArrayNodes;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.translator.TranslatorDriver;
import org.jruby.util.cli.Options;
import org.jruby.util.cli.OutputStrings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.LinkedHashMap;
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
    @CompilerDirectives.CompilationFinal private RubyClass dirClass;
    @CompilerDirectives.CompilationFinal private RubyClass encodingClass;
    @CompilerDirectives.CompilationFinal private RubyClass exceptionClass;
    @CompilerDirectives.CompilationFinal private RubyClass falseClass;
    @CompilerDirectives.CompilationFinal private RubyClass fiberClass;
    @CompilerDirectives.CompilationFinal private RubyClass fileClass;
    @CompilerDirectives.CompilationFinal private RubyClass fixnumClass;
    @CompilerDirectives.CompilationFinal private RubyClass floatClass;
    @CompilerDirectives.CompilationFinal private RubyClass hashClass;
    @CompilerDirectives.CompilationFinal private RubyClass integerClass;
    @CompilerDirectives.CompilationFinal private RubyClass ioClass;
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
    @CompilerDirectives.CompilationFinal private RubyClass regexpClass;
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
    @CompilerDirectives.CompilationFinal private RubyModule kernelModule;
    @CompilerDirectives.CompilationFinal private RubyModule mathModule;
    @CompilerDirectives.CompilationFinal private RubyModule objectSpaceModule;
    @CompilerDirectives.CompilationFinal private RubyModule signalModule;
    @CompilerDirectives.CompilationFinal private RubyModule truffleDebugModule;

    @CompilerDirectives.CompilationFinal private RubyArray argv;
    @CompilerDirectives.CompilationFinal private RubyBasicObject globalVariablesObject;
    @CompilerDirectives.CompilationFinal private RubyBasicObject mainObject;
    @CompilerDirectives.CompilationFinal private RubyFalseClass falseObject;
    @CompilerDirectives.CompilationFinal private RubyNilClass nilObject;
    @CompilerDirectives.CompilationFinal private RubyTrueClass trueObject;

    private ArrayNodes.MinBlock arrayMinBlock;
    private ArrayNodes.MaxBlock arrayMaxBlock;

    public CoreLibrary(RubyContext context) {
        this.context = context;
    }

    public void initialize() {
        // Create the cyclic classes and modules

        classClass = new RubyClass.RubyClassClass(context);
        basicObjectClass = new RubyClass(null, context, classClass, null, null, "BasicObject");
        objectClass = new RubyClass(null, null, basicObjectClass, "Object");
        moduleClass = new RubyModule.RubyModuleClass(context);

        // Close the cycles

        moduleClass.unsafeSetRubyClass(classClass);
        classClass.unsafeSetSuperclass(null, moduleClass);
        moduleClass.unsafeSetSuperclass(null, objectClass);
        classClass.unsafeSetRubyClass(classClass);

        // Create all other classes and modules

        numericClass = new RubyClass(null, null, objectClass, "Numeric");
        integerClass = new RubyClass(null, null, numericClass, "Integer");

        exceptionClass = new RubyException.RubyExceptionClass(objectClass, "Exception");
        standardErrorClass = new RubyException.RubyExceptionClass(exceptionClass, "StandardError");

        ioClass = new RubyClass(null, null, objectClass, "IO");

        argumentErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "ArgumentError");
        arrayClass = new RubyArray.RubyArrayClass(objectClass);
        bignumClass = new RubyClass(null, null, integerClass, "Bignum");
        bindingClass = new RubyClass(null, null, objectClass, "Binding");
        comparableModule = new RubyModule(moduleClass, null, "Comparable");
        configModule = new RubyModule(moduleClass, null, "Config");
        continuationClass = new RubyClass(null, null, objectClass, "Continuation");
        dirClass = new RubyClass(null, null, objectClass, "Dir");
        encodingClass = new RubyEncoding.RubyEncodingClass(objectClass);
        errnoModule = new RubyModule(moduleClass, null, "Errno");
        enumerableModule = new RubyModule(moduleClass, null, "Enumerable");
        falseClass = new RubyClass(null, null, objectClass, "FalseClass");
        fiberClass = new RubyFiber.RubyFiberClass(objectClass);
        fileClass = new RubyClass(null, null, ioClass, "File");
        fixnumClass = new RubyClass(null, null, integerClass, "Fixnum");
        floatClass = new RubyClass(null, null, objectClass, "Float");
        hashClass = new RubyHash.RubyHashClass(objectClass);
        kernelModule = new RubyModule(moduleClass, null, "Kernel");
        loadErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "LoadError");
        localJumpErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "LocalJumpError");
        matchDataClass = new RubyClass(null, null, objectClass, "MatchData");
        mathModule = new RubyModule(moduleClass, null, "Math");
        nameErrorClass = new RubyClass(null, null, standardErrorClass, "NameError");
        nilClass = new RubyClass(null, null, objectClass, "NilClass");
        noMethodErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "NoMethodError");
        objectSpaceModule = new RubyModule(moduleClass, null, "ObjectSpace");
        procClass = new RubyProc.RubyProcClass(objectClass);
        processClass = new RubyClass(null, null, objectClass, "Process");
        rangeClass = new RubyClass(null, null, objectClass, "Range");
        rangeErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "RangeError");
        regexpClass = new RubyRegexp.RubyRegexpClass(objectClass);
        rubyTruffleErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "RubyTruffleError");
        runtimeErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "RuntimeError");
        signalModule = new RubyModule(moduleClass, null, "Signal");
        stringClass = new RubyString.RubyStringClass(objectClass);
        symbolClass = new RubyClass(null, null, objectClass, "Symbol");
        syntaxErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "SyntaxError");
        systemCallErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "SystemCallError");
        systemExitClass = new RubyException.RubyExceptionClass(exceptionClass, "SystemExit");
        threadClass = new RubyThread.RubyThreadClass(objectClass);
        timeClass = new RubyTime.RubyTimeClass(objectClass);
        trueClass = new RubyClass(null, null, objectClass, "TrueClass");
        truffleDebugModule = new RubyModule(moduleClass, null, "TruffleDebug");
        typeErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "TypeError");
        zeroDivisionErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "ZeroDivisionError");

        // Includes

        objectClass.include(null, kernelModule);

        // Set constants

        objectClass.setConstant(null, "RUBY_VERSION", RubyString.fromJavaString(stringClass, "2.1.0"));
        objectClass.setConstant(null, "RUBY_PATCHLEVEL", 0);
        objectClass.setConstant(null, "RUBY_ENGINE", RubyString.fromJavaString(stringClass, "rubytruffle"));
        objectClass.setConstant(null, "RUBY_PLATFORM", RubyString.fromJavaString(stringClass, "jvm"));

        argv = new RubyArray(arrayClass);
        objectClass.setConstant(null, "ARGV", argv);
        objectClass.setConstant(null, "ENV", getEnv());
        objectClass.setConstant(null, "TRUE", true);
        objectClass.setConstant(null, "FALSE", false);
        objectClass.setConstant(null, "NIL", NilPlaceholder.INSTANCE);

        final LinkedHashMap<Object, Object> configHashMap = new LinkedHashMap<>();
        configHashMap.put(RubyString.fromJavaString(stringClass, "ruby_install_name"), RubyString.fromJavaString(stringClass, "rubytruffle"));
        configHashMap.put(RubyString.fromJavaString(stringClass, "RUBY_INSTALL_NAME"), RubyString.fromJavaString(stringClass, "rubytruffle"));
        configHashMap.put(RubyString.fromJavaString(stringClass, "host_os"), RubyString.fromJavaString(stringClass, "unknown"));
        configHashMap.put(RubyString.fromJavaString(stringClass, "exeext"), RubyString.fromJavaString(stringClass, ""));
        configHashMap.put(RubyString.fromJavaString(stringClass, "EXEEXT"), RubyString.fromJavaString(stringClass, "rubytruffle"));
        final RubyHash configHash = new RubyHash(hashClass, null, configHashMap, 0);
        configModule.setConstant(null, "CONFIG", configHash);

        objectClass.setConstant(null, "RbConfig", configModule);

        mathModule.setConstant(null, "PI", Math.PI);

        fileClass.setConstant(null, "SEPARATOR", RubyString.fromJavaString(stringClass, File.separator));
        fileClass.setConstant(null, "Separator", RubyString.fromJavaString(stringClass, File.separator));
        fileClass.setConstant(null, "ALT_SEPARATOR", NilPlaceholder.INSTANCE);
        fileClass.setConstant(null, "PATH_SEPARATOR", RubyString.fromJavaString(stringClass, File.pathSeparator));
        fileClass.setConstant(null, "FNM_SYSCASE", 0);

        errnoModule.setConstant(null, "ENOENT", new RubyClass(null, null, systemCallErrorClass, "ENOENT"));
        errnoModule.setConstant(null, "EPERM", new RubyClass(null, null, systemCallErrorClass, "EPERM"));
        errnoModule.setConstant(null, "ENOTEMPTY", new RubyClass(null, null, systemCallErrorClass, "ENOTEMPTY"));
        errnoModule.setConstant(null, "EEXIST", new RubyClass(null, null, systemCallErrorClass, "EEXIST"));
        errnoModule.setConstant(null, "EXDEV", new RubyClass(null, null, systemCallErrorClass, "EXDEV"));
        errnoModule.setConstant(null, "EACCES", new RubyClass(null, null, systemCallErrorClass, "EACCES"));
        errnoModule.setConstant(null, "EDOM", new RubyClass(null, null, systemCallErrorClass, "EDOM"));

        // Add all classes and modules as constants in Object

        final RubyModule[] modules = {argumentErrorClass, //
                        arrayClass, //
                        basicObjectClass, //
                        bignumClass, //
                        bindingClass, //
                        classClass, //
                        continuationClass, //
                        comparableModule, //
                        configModule, //
                        dirClass, //
                        enumerableModule, //
                        errnoModule, //
                        exceptionClass, //
                        falseClass, //
                        fiberClass, //
                        fileClass, //
                        fixnumClass, //
                        floatClass, //
                        hashClass, //
                        integerClass, //
                        ioClass, //
                        kernelModule, //
                        loadErrorClass, //
                        localJumpErrorClass, //
                        matchDataClass, //
                        mathModule, //
                        moduleClass, //
                        nameErrorClass, //
                        nilClass, //
                        noMethodErrorClass, //
                        numericClass, //
                        objectClass, //
                        objectSpaceModule, //
                        procClass, //
                        processClass, //
                        rangeClass, //
                        rangeErrorClass, //
                        regexpClass, //
                        rubyTruffleErrorClass, //
                        runtimeErrorClass, //
                        signalModule, //
                        standardErrorClass, //
                        stringClass, //
                        encodingClass, //
                        symbolClass, //
                        syntaxErrorClass, //
                        systemCallErrorClass, //
                        systemExitClass, //
                        threadClass, //
                        timeClass, //
                        trueClass, //
                        truffleDebugModule, //
                        typeErrorClass, //
                        zeroDivisionErrorClass};

        for (RubyModule module : modules) {
            objectClass.setConstant(null, module.getName(), module);
        }

        // Create some key objects

        mainObject = new RubyObject(objectClass);
        nilObject = new RubyNilClass(nilClass);
        trueObject = new RubyTrueClass(trueClass);
        falseObject = new RubyFalseClass(falseClass);

        // Create the globals object

        globalVariablesObject = new RubyBasicObject(objectClass);
        globalVariablesObject.switchToPrivateLayout();
        globalVariablesObject.setInstanceVariable("$LOAD_PATH", new RubyArray(arrayClass));
        globalVariablesObject.setInstanceVariable("$LOADED_FEATURES", new RubyArray(arrayClass));
        globalVariablesObject.setInstanceVariable("$:", globalVariablesObject.getInstanceVariable("$LOAD_PATH"));
        globalVariablesObject.setInstanceVariable("$\"", globalVariablesObject.getInstanceVariable("$LOADED_FEATURES"));

        initializeEncodingConstants();

        arrayMinBlock = new ArrayNodes.MinBlock(context);
        arrayMaxBlock = new ArrayNodes.MaxBlock(context);
    }

    public void initializeAfterMethodsAdded() {
        // Just create a dummy object for $stdout - we can use Kernel#print

        final RubyBasicObject stdout = new RubyBasicObject(objectClass);
        stdout.getSingletonClass(null).addMethod(null, stdout.getLookupNode().lookupMethod("print").withNewVisibility(Visibility.PUBLIC));
        globalVariablesObject.setInstanceVariable("$stdout", stdout);

        objectClass.setConstant(null, "STDIN", new RubyBasicObject(objectClass));
        objectClass.setConstant(null, "STDOUT", globalVariablesObject.getInstanceVariable("$stdout"));
        objectClass.setConstant(null, "STDERR", globalVariablesObject.getInstanceVariable("$stdout"));
        objectClass.setConstant(null, "RUBY_RELEASE_DATE", context.makeString(Constants.COMPILE_DATE));
        objectClass.setConstant(null, "RUBY_DESCRIPTION", context.makeString(OutputStrings.getVersionString()));

        bignumClass.getSingletonClass(null).undefMethod(null, "new");
        falseClass.getSingletonClass(null).undefMethod(null, "new");
        fixnumClass.getSingletonClass(null).undefMethod(null, "new");
        floatClass.getSingletonClass(null).undefMethod(null, "new");
        integerClass.getSingletonClass(null).undefMethod(null, "new");
        nilClass.getSingletonClass(null).undefMethod(null, "new");
        numericClass.getSingletonClass(null).undefMethod(null, "new");
        trueClass.getSingletonClass(null).undefMethod(null, "new");
        encodingClass.getSingletonClass(null).undefMethod(null, "new");

        if (Options.TRUFFLE_LOAD_CORE.load()) {
            final String[] files = new String[]{
                    "jruby/truffle/core/kernel.rb"
            };

            for (String file : files) {
                loadRubyCore(file);
            }
        }
    }

    public void loadRubyCore(String fileName) {
        final Source source;

        try {
            source = Source.fromReader(new InputStreamReader(context.getRuntime().getLoadService().getClassPathResource(context.getRuntime().getJRubyClassLoader(), fileName).getInputStream()), fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        context.execute(context, source, TranslatorDriver.ParserContext.TOP_LEVEL, mainObject, null, null);
    }

    public void initializeEncodingConstants() {
        encodingClass.setConstant(null, "US_ASCII", new RubyEncoding(encodingClass, USASCIIEncoding.INSTANCE));
        encodingClass.setConstant(null, "ASCII_8BIT", new RubyEncoding(encodingClass, USASCIIEncoding.INSTANCE));
        encodingClass.setConstant(null, "UTF_8", new RubyEncoding(encodingClass, USASCIIEncoding.INSTANCE));
        encodingClass.setConstant(null, "EUC_JP", new RubyEncoding(encodingClass, EUCJPEncoding.INSTANCE));
        encodingClass.setConstant(null, "Windows_31J", new RubyEncoding(encodingClass, SJISEncoding.INSTANCE));

    }

    public RubyBasicObject box(Object object) {
        RubyNode.notDesignedForCompilation();

        assert RubyContext.shouldObjectBeVisible(object) : object.getClass();

        // TODO(cs): pool common object instances like small Fixnums?

        if (object instanceof RubyBasicObject) {
            return (RubyBasicObject) object;
        }

        if (object instanceof Boolean) {
            if ((boolean) object) {
                return trueObject;
            } else {
                return falseObject;
            }
        }

        if (object instanceof Integer) {
            return new RubyFixnum.IntegerFixnum(fixnumClass, (int) object);
        }

        if (object instanceof Long) {
            return new RubyFixnum.LongFixnum(fixnumClass, (long) object);
        }

        if (object instanceof BigInteger) {
            return new RubyBignum(bignumClass, (BigInteger) object);
        }

        if (object instanceof Double) {
            return new RubyFloat(floatClass, (double) object);
        }

        if (object instanceof NilPlaceholder) {
            return nilObject;
        }

        if (object instanceof String) {
            return context.makeString((String) object);
        }

        CompilerDirectives.transferToInterpreter();

        throw new UnsupportedOperationException("Don't know how to box " + object.getClass().getName());
    }

    public RubyException runtimeError(String message, Node currentNode) {
        return new RubyException(runtimeErrorClass, context.makeString(String.format("RuntimeError: %s", message)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException frozenError(String className, Node currentNode) {
        return runtimeError(String.format("FrozenError: can't modify frozen %s \n %s", className), currentNode);
    }

    public RubyException argumentError(String message, Node currentNode) {
        return new RubyException(argumentErrorClass, context.makeString(String.format("ArgumentError: %s", message)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException argumentError(int passed, int required, Node currentNode) {
        return argumentError(String.format("ArgumentError: wrong number of arguments (%d for %d)", passed, required), currentNode);
    }

    public RubyException argumentErrorUncaughtThrow(Object tag, Node currentNode) {
        return argumentError(String.format("ArgumentError: uncaught throw `%s'", tag), currentNode);
    }

    public RubyException localJumpError(String message, Node currentNode) {
        return new RubyException(localJumpErrorClass, context.makeString(String.format("LocalJumpError: %s", message)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException unexpectedReturn(Node currentNode) {
        return localJumpError("unexpected return", currentNode);
    }

    public RubyException noBlockToYieldTo(Node currentNode) {
        return localJumpError("no block given (yield)", currentNode);
    }

    public RubyException typeError(String message, Node currentNode) {
        return new RubyException(typeErrorClass, context.makeString(String.format("%s ", message)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException typeErrorShouldReturn(String object, String method, String expectedType, Node currentNode) {
        return typeError(String.format("TypeError: %s#%s should return %s", object, method, expectedType), currentNode);
    }

    public RubyException typeError(String from, String to, Node currentNode) {
        return typeError(String.format("TypeError: can't convert %s to %s", from, to), currentNode);
    }

    public RubyException typeErrorIsNotA(String value, String expectedType, Node currentNode) {
        return typeError(String.format("TypeError: %s is not a %s", value, expectedType), currentNode);
    }

    public RubyException typeErrorNeedsToBe(String name, String expectedType, Node currentNode) {
        return typeError(String.format("TypeError: %s needs to be %s", name, expectedType), currentNode);
    }

    public RubyException rangeError(String message, Node currentNode) {
        return new RubyException(rangeErrorClass, context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException nameError(String message, Node currentNode) {
        return new RubyException(nameErrorClass, context.makeString(String.format("%s ", message)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException nameErrorUninitializedConstant(String name, Node currentNode) {
        return nameError(String.format("NameError: uninitialized constant %s", name), currentNode);
    }

    public RubyException nameErrorNoMethod(String name, String object, Node currentNode) {
        return nameError(String.format("NameError: undefined local variable or method `%s' for %s", name, object), currentNode);
    }

    public RubyException nameErrorInstanceNameNotAllowable(String name, Node currentNode) {
        return nameError(String.format("`%s' is not allowable as an instance variable name", name), currentNode);
    }

    public RubyException nameErrorUncaughtThrow(Object tag, Node currentNode) {
        return nameError(String.format("NameError: uncaught throw `%s'", tag), currentNode);
    }

    public RubyException noMethodError(String message, Node currentNode) {
        return new RubyException(context.getCoreLibrary().getNoMethodErrorClass(), context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException noMethodError(String name, String object, Node currentNode) {
        return noMethodError(String.format("NameError: undefined method `%s' for %s", name, object), currentNode);
    }

    public RubyException privateNoMethodError(String name, String object, Node currentNode) {
        return noMethodError(String.format("NoMethodError: private Method method `%s' called for %s", name, object), currentNode);
    }

    public RubyException loadError(String message, Node currentNode) {
        return new RubyException(context.getCoreLibrary().getLoadErrorClass(), context.makeString(message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException loadErrorCannotLoad(String name, Node currentNode) {
        return loadError(String.format("LoadError: cannot load such file -- %s", name), currentNode);
    }

    public RubyException zeroDivisionError(Node currentNode) {
        return new RubyException(context.getCoreLibrary().getZeroDivisionErrorClass(), context.makeString("divided by 0"), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyException syntaxError(String message, Node currentNode) {
        return new RubyException(syntaxErrorClass, context.makeString(String.format("SyntaxError: %s ", message)), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyContext getContext() {
        return context;
    }

    public RubyClass getArrayClass() {
        return arrayClass;
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

    public RubyTrueClass getTrueObject() {
        return trueObject;
    }

    public RubyFalseClass getFalseObject() {
        return falseObject;
    }

    public RubyNilClass getNilObject() {
        return nilObject;
    }

    public RubyEncoding getDefaultEncoding() { return RubyEncoding.findEncodingByName(context.makeString("US-ASCII")); }

    public RubyHash getEnv() {
        final LinkedHashMap<Object, Object> storage = new LinkedHashMap<>();

        for (Map.Entry<String, String> variable : System.getenv().entrySet()) {
            storage.put(context.makeString(variable.getKey()), context.makeString(variable.getValue()));
        }

        return new RubyHash(context.getCoreLibrary().getHashClass(), null, storage, 0);
    }

    public ArrayNodes.MinBlock getArrayMinBlock() {
        return arrayMinBlock;
    }

    public ArrayNodes.MaxBlock getArrayMaxBlock() {
        return arrayMaxBlock;
    }

}
