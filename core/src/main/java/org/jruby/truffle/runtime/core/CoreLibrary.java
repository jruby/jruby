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
import org.jcodings.specific.EUCJPEncoding;
import org.jcodings.specific.SJISEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.runtime.Constants;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.cli.OutputStrings;

import java.io.File;
import java.math.BigInteger;
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
    @CompilerDirectives.CompilationFinal private RubyClass structClass;
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
    @CompilerDirectives.CompilationFinal private RubyModule debugModule;
    @CompilerDirectives.CompilationFinal private RubyModule enumerableModule;
    @CompilerDirectives.CompilationFinal private RubyModule errnoModule;
    @CompilerDirectives.CompilationFinal private RubyModule kernelModule;
    @CompilerDirectives.CompilationFinal private RubyModule mathModule;
    @CompilerDirectives.CompilationFinal private RubyModule objectSpaceModule;
    @CompilerDirectives.CompilationFinal private RubyModule signalModule;

    @CompilerDirectives.CompilationFinal private RubyArray argv;
    @CompilerDirectives.CompilationFinal private RubyBasicObject globalVariablesObject;
    @CompilerDirectives.CompilationFinal private RubyBasicObject mainObject;
    @CompilerDirectives.CompilationFinal private RubyFalseClass falseObject;
    @CompilerDirectives.CompilationFinal private RubyNilClass nilObject;
    @CompilerDirectives.CompilationFinal private RubyTrueClass trueObject;

    public CoreLibrary(RubyContext context) {
        this.context = context;
    }

    public void initialize() {
        // Create the cyclic classes and modules

        classClass = new RubyClass.RubyClassClass(context);
        basicObjectClass = new RubyClass(context, classClass, null, null, "BasicObject");
        objectClass = new RubyClass(null, basicObjectClass, "Object");
        moduleClass = new RubyModule.RubyModuleClass(context);

        // Close the cycles

        moduleClass.unsafeSetRubyClass(classClass);
        classClass.unsafeSetSuperclass(moduleClass);
        moduleClass.unsafeSetSuperclass(objectClass);
        classClass.unsafeSetRubyClass(classClass);

        // Create all other classes and modules

        numericClass = new RubyClass(null, objectClass, "Numeric");
        integerClass = new RubyClass(null, numericClass, "Integer");

        exceptionClass = new RubyException.RubyExceptionClass(objectClass, "Exception");
        standardErrorClass = new RubyException.RubyExceptionClass(exceptionClass, "StandardError");

        ioClass = new RubyClass(null, objectClass, "IO");

        argumentErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "ArgumentError");
        arrayClass = new RubyArray.RubyArrayClass(objectClass);
        bignumClass = new RubyClass(null, integerClass, "Bignum");
        bindingClass = new RubyClass(null, objectClass, "Binding");
        comparableModule = new RubyModule(moduleClass, null, "Comparable");
        configModule = new RubyModule(moduleClass, null, "Config");
        continuationClass = new RubyClass(null, objectClass, "Continuation");
        debugModule = new RubyModule(moduleClass, null, "Debug");
        dirClass = new RubyClass(null, objectClass, "Dir");
        encodingClass = new RubyEncoding.RubyEncodingClass(objectClass);
        errnoModule = new RubyModule(moduleClass, null, "Errno");
        enumerableModule = new RubyModule(moduleClass, null, "Enumerable");
        falseClass = new RubyClass(null, objectClass, "FalseClass");
        fiberClass = new RubyFiber.RubyFiberClass(objectClass);
        fileClass = new RubyClass(null, ioClass, "File");
        fixnumClass = new RubyClass(null, integerClass, "Fixnum");
        floatClass = new RubyClass(null, objectClass, "Float");
        hashClass = new RubyHash.RubyHashClass(objectClass);
        kernelModule = new RubyModule(moduleClass, null, "Kernel");
        loadErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "LoadError");
        localJumpErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "LocalJumpError");
        matchDataClass = new RubyClass(null, objectClass, "MatchData");
        mathModule = new RubyModule(moduleClass, null, "Math");
        nameErrorClass = new RubyClass(null, standardErrorClass, "NameError");
        nilClass = new RubyClass(null, objectClass, "NilClass");
        noMethodErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "NoMethodError");
        objectSpaceModule = new RubyModule(moduleClass, null, "ObjectSpace");
        procClass = new RubyProc.RubyProcClass(objectClass);
        processClass = new RubyClass(null, objectClass, "Process");
        rangeClass = new RubyClass(null, objectClass, "Range");
        rangeErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "RangeError");
        regexpClass = new RubyRegexp.RubyRegexpClass(objectClass);
        rubyTruffleErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "RubyTruffleError");
        runtimeErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "RuntimeError");
        signalModule = new RubyModule(moduleClass, null, "Signal");
        stringClass = new RubyString.RubyStringClass(objectClass);
        structClass = new RubyClass(null, ioClass, "Struct");
        symbolClass = new RubyClass(null, objectClass, "Symbol");
        syntaxErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "SyntaxError");
        systemCallErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "SystemCallError");
        systemExitClass = new RubyException.RubyExceptionClass(exceptionClass, "SystemExit");
        threadClass = new RubyThread.RubyThreadClass(objectClass);
        timeClass = new RubyTime.RubyTimeClass(objectClass);
        trueClass = new RubyClass(null, objectClass, "TrueClass");
        typeErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "TypeError");
        zeroDivisionErrorClass = new RubyException.RubyExceptionClass(standardErrorClass, "ZeroDivisionError");

        // Includes

        objectClass.include(kernelModule);

        // Set constants

        objectClass.setConstant("RUBY_VERSION", RubyString.fromJavaString(stringClass, "2.1.0"));
        objectClass.setConstant("RUBY_PATCHLEVEL", 0);
        objectClass.setConstant("RUBY_ENGINE", RubyString.fromJavaString(stringClass, "rubytruffle"));
        objectClass.setConstant("RUBY_PLATFORM", RubyString.fromJavaString(stringClass, "jvm"));

        argv = new RubyArray(arrayClass);
        objectClass.setConstant("ARGV", argv);
        objectClass.setConstant("ENV", getEnv());
        objectClass.setConstant("TRUE", true);
        objectClass.setConstant("FALSE", false);
        objectClass.setConstant("NIL", NilPlaceholder.INSTANCE);

        final RubyHash configHash = new RubyHash(hashClass);
        configHash.put(RubyString.fromJavaString(stringClass, "ruby_install_name"), RubyString.fromJavaString(stringClass, "rubytruffle"));
        configHash.put(RubyString.fromJavaString(stringClass, "RUBY_INSTALL_NAME"), RubyString.fromJavaString(stringClass, "rubytruffle"));
        configHash.put(RubyString.fromJavaString(stringClass, "host_os"), RubyString.fromJavaString(stringClass, "unknown"));
        configHash.put(RubyString.fromJavaString(stringClass, "exeext"), RubyString.fromJavaString(stringClass, ""));
        configHash.put(RubyString.fromJavaString(stringClass, "EXEEXT"), RubyString.fromJavaString(stringClass, "rubytruffle"));
        configModule.setConstant("CONFIG", configHash);
        objectClass.setConstant("RbConfig", configModule);

        mathModule.setConstant("PI", Math.PI);

        fileClass.setConstant("SEPARATOR", RubyString.fromJavaString(stringClass, File.separator));
        fileClass.setConstant("Separator", RubyString.fromJavaString(stringClass, File.separator));
        fileClass.setConstant("ALT_SEPARATOR", NilPlaceholder.INSTANCE);
        fileClass.setConstant("PATH_SEPARATOR", RubyString.fromJavaString(stringClass, File.pathSeparator));
        fileClass.setConstant("FNM_SYSCASE", 0);

        errnoModule.setConstant("ENOENT", new RubyClass(null, systemCallErrorClass, "ENOENT"));
        errnoModule.setConstant("EPERM", new RubyClass(null, systemCallErrorClass, "EPERM"));
        errnoModule.setConstant("ENOTEMPTY", new RubyClass(null, systemCallErrorClass, "ENOTEMPTY"));
        errnoModule.setConstant("EEXIST", new RubyClass(null, systemCallErrorClass, "EEXIST"));
        errnoModule.setConstant("EXDEV", new RubyClass(null, systemCallErrorClass, "EXDEV"));
        errnoModule.setConstant("EACCES", new RubyClass(null, systemCallErrorClass, "EACCES"));

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
                        debugModule, //
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
                        structClass, //
                        symbolClass, //
                        syntaxErrorClass, //
                        systemCallErrorClass, //
                        systemExitClass, //
                        threadClass, //
                        timeClass, //
                        trueClass, //
                        typeErrorClass, //
                        zeroDivisionErrorClass};

        for (RubyModule module : modules) {
            objectClass.setConstant(module.getName(), module);
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
    }

    public void initializeAfterMethodsAdded() {
        // Just create a dummy object for $stdout - we can use Kernel#print

        final RubyBasicObject stdout = new RubyBasicObject(objectClass);
        stdout.getSingletonClass().addMethod(stdout.getLookupNode().lookupMethod("print").withNewVisibility(Visibility.PUBLIC));
        globalVariablesObject.setInstanceVariable("$stdout", stdout);

        objectClass.setConstant("STDIN", new RubyBasicObject(objectClass));
        objectClass.setConstant("STDOUT", globalVariablesObject.getInstanceVariable("$stdout"));
        objectClass.setConstant("STDERR", globalVariablesObject.getInstanceVariable("$stdout"));
        objectClass.setConstant("RUBY_RELEASE_DATE", context.makeString(Constants.COMPILE_DATE));
        objectClass.setConstant("RUBY_DESCRIPTION", context.makeString(OutputStrings.getVersionString()));

        bignumClass.getSingletonClass().undefMethod("new");
        falseClass.getSingletonClass().undefMethod("new");
        fixnumClass.getSingletonClass().undefMethod("new");
        floatClass.getSingletonClass().undefMethod("new");
        integerClass.getSingletonClass().undefMethod("new");
        nilClass.getSingletonClass().undefMethod("new");
        numericClass.getSingletonClass().undefMethod("new");
        trueClass.getSingletonClass().undefMethod("new");
        encodingClass.getSingletonClass().undefMethod("new");
    }

    public void initializeEncodingConstants() {
        encodingClass.setConstant("US_ASCII", new RubyEncoding(encodingClass, USASCIIEncoding.INSTANCE));
        encodingClass.setConstant("ASCII_8BIT", new RubyEncoding(encodingClass, USASCIIEncoding.INSTANCE));
        encodingClass.setConstant("UTF_8", new RubyEncoding(encodingClass, USASCIIEncoding.INSTANCE));
        encodingClass.setConstant("EUC_JP", new RubyEncoding(encodingClass, EUCJPEncoding.INSTANCE));
        encodingClass.setConstant("Windows_31J", new RubyEncoding(encodingClass, SJISEncoding.INSTANCE));

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

    public RubyException runtimeError(String message) {
        return new RubyException(runtimeErrorClass, String.format("RuntimeError: %s \n %s", message, RubyCallStack.getRubyStacktrace()));
    }

    public RubyException frozenError(String className) {
        return runtimeError(String.format("FrozenError: can't modify frozen %s \n %s", className, RubyCallStack.getRubyStacktrace()));
    }

    public RubyException argumentError(String message) {
        return new RubyException(argumentErrorClass, String.format("ArgumentError: %s \n %s", message, RubyCallStack.getRubyStacktrace()));
    }

    public RubyException argumentError(int passed, int required) {
        return argumentError(String.format("ArgumentError: wrong number of arguments (%d for %d)", passed, required));
    }

    public RubyException argumentErrorUncaughtThrow(Object tag) {
        return argumentError(String.format("ArgumentError: uncaught throw `%s'", tag));
    }

    public RubyException localJumpError(String message) {
        return new RubyException(localJumpErrorClass, String.format("LocalJumpError: %s \n %s", message, RubyCallStack.getRubyStacktrace()));
    }

    public RubyException unexpectedReturn() {
        return localJumpError("unexpected return");
    }

    public RubyException noBlockToYieldTo() {
        return localJumpError("no block given (yield)");
    }

    public RubyException typeError(String message) {
        return new RubyException(typeErrorClass, String.format("%s \n %s", message, RubyCallStack.getRubyStacktrace()));
    }

    public RubyException typeErrorShouldReturn(String object, String method, String expectedType) {
        return typeError(String.format("TypeError: %s#%s should return %s", object, method, expectedType));
    }

    public RubyException typeError(String from, String to) {
        return typeError(String.format("TypeError: can't convert %s to %s", from, to));
    }

    public RubyException typeErrorIsNotA(String value, String expectedType) {
        return typeError(String.format("TypeError: %s is not a %s", value, expectedType));
    }

    public RubyException typeErrorNeedsToBe(String name, String expectedType) {
        return typeError(String.format("TypeError: %s needs to be %s", name, expectedType));
    }

    public RubyException rangeError(String message) {
        return new RubyException(rangeErrorClass, message);
    }

    public RubyException nameError(String message) {
        return new RubyException(nameErrorClass, String.format("%s \n %s", message, RubyCallStack.getRubyStacktrace()));
    }

    public RubyException nameErrorUninitializedConstant(String name) {
        return nameError(String.format("NameError: uninitialized constant %s", name));
    }

    public RubyException nameErrorNoMethod(String name, String object) {
        return nameError(String.format("NameError: undefined local variable or method `%s' for %s", name, object));
    }

    public RubyException nameErrorInstanceNameNotAllowable(String name) {
        return nameError(String.format("`%s' is not allowable as an instance variable name", name));
    }

    public RubyException nameErrorUncaughtThrow(Object tag) {
        return nameError(String.format("NameError: uncaught throw `%s'", tag));
    }

    public RubyException noMethodError(String message) {
        return new RubyException(context.getCoreLibrary().getNoMethodErrorClass(), String.format("%s \n %s", message, RubyCallStack.getRubyStacktrace()));
    }

    public RubyException noMethodError(String name, String object) {
        return noMethodError(String.format("NameError: undefined method `%s' for %s", name, object));
    }

    public RubyException loadError(String message) {
        return new RubyException(context.getCoreLibrary().getLoadErrorClass(), String.format("%s \n %s", message, RubyCallStack.getRubyStacktrace()));
    }

    public RubyException loadErrorCannotLoad(String name) {
        return loadError(String.format("LoadError: cannot load such file -- %s", name));
    }

    public RubyException zeroDivisionError() {
        return new RubyException(context.getCoreLibrary().getZeroDivisionErrorClass(), String.format("divided by 0 \n %s", RubyCallStack.getRubyStacktrace()));
    }

    public RubyException syntaxError(String message) {
        return new RubyException(syntaxErrorClass, String.format("SyntaxError: %s \n %s", message, RubyCallStack.getRubyStacktrace()));
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

    public RubyClass getStructClass() {
        return structClass;
    }

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
        final RubyHash hash = new RubyHash(context.getCoreLibrary().getHashClass());

        for (Map.Entry<String, String> variable : System.getenv().entrySet()) {
            hash.put(context.makeString(variable.getKey()), context.makeString(variable.getValue()));
        }

        return hash;
    }

}
