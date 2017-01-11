/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.exception;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import jnr.constants.platform.Errno;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.encoding.EncodingOperations;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.CoreStrings;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.Visibility;

import static org.jruby.truffle.core.array.ArrayHelpers.createArray;

public class CoreExceptions {

    private final RubyContext context;

    public CoreExceptions(RubyContext context) {
        this.context = context;
    }

    // ArgumentError

    public DynamicObject argumentErrorOneHashRequired(Node currentNode) {
        return argumentError(coreStrings().ONE_HASH_REQUIRED.getRope(), currentNode, null);
    }

    public DynamicObject argumentError(Rope message, Node currentNode) {
        return argumentError(message, currentNode, null);
    }

    public DynamicObject argumentError(String message, Node currentNode) {
        return argumentError(message, currentNode, null);
    }

    public DynamicObject argumentErrorProcWithoutBlock(Node currentNode) {
        return argumentError(coreStrings().PROC_WITHOUT_BLOCK.getRope(), currentNode, null);
    }

    public DynamicObject argumentErrorTooFewArguments(Node currentNode) {
        return argumentError(coreStrings().TOO_FEW_ARGUMENTS.getRope(), currentNode, null);
    }

    public DynamicObject argumentErrorTimeItervalPositive(Node currentNode) {
        return argumentError(coreStrings().TIME_INTERVAL_MUST_BE_POS.getRope(), currentNode, null);
    }

    public DynamicObject argumentErrorXOutsideOfString(Node currentNode) {
        return argumentError(coreStrings().X_OUTSIDE_OF_STRING.getRope(), currentNode, null);
    }

    public DynamicObject argumentErrorCantCompressNegativeNumbers(Node currentNode) {
        return argumentError(coreStrings().CANT_COMPRESS_NEGATIVE.getRope(), currentNode, null);
    }

    public DynamicObject argumentErrorOutOfRange(Node currentNode) {
        return argumentError(coreStrings().OUT_OF_RANGE.getRope(), currentNode, null);
    }

    public DynamicObject argumentErrorNegativeArraySize(Node currentNode) {
        return argumentError(coreStrings().NEGATIVE_ARRAY_SIZE.getRope(), currentNode, null);
    }

    public DynamicObject argumentErrorCharacterRequired(Node currentNode) {
        return argumentError("%c requires a character", currentNode);
    }

    public DynamicObject argumentErrorCantOmitPrecision(Node currentNode) {
        return argumentError("can't omit precision for a Float.", currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentErrorUnknownKeyword(Object name, Node currentNode) {
        return argumentError("unknown keyword: " + name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentErrorInvalidRadix(int radix, Node currentNode) {
        return argumentError(StringUtils.format("invalid radix %d", radix), currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentErrorMissingKeyword(String name, Node currentNode) {
        return argumentError(StringUtils.format("missing keyword: %s", name), currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentError(int passed, int required, Node currentNode) {
        return argumentError(StringUtils.format("wrong number of arguments (%d for %d)", passed, required), currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentError(int passed, int required, int optional, Node currentNode) {
        return argumentError(StringUtils.format("wrong number of arguments (%d for %d..%d)", passed, required, required + optional), currentNode);
    }

    public DynamicObject argumentErrorEmptyVarargs(Node currentNode) {
        return argumentError(coreStrings().WRONG_ARGS_ZERO_PLUS_ONE.getRope(), currentNode, null);
    }

    @TruffleBoundary
    public DynamicObject argumentErrorWrongArgumentType(Object object, String expectedType, Node currentNode) {
        String badClassName = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(object)).getName();
        return argumentError(StringUtils.format("wrong argument type %s (expected %s)", badClassName, expectedType), currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentErrorInvalidValue(Object object, String expectedType, Node currentNode) {
        String badClassName = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(object)).getName();
        return argumentError(StringUtils.format("invalid value for %s(): %s", badClassName, expectedType), currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentErrorNoReceiver(Node currentNode) {
        return argumentError("no receiver is available", currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentErrorEncodingAlreadyRegistered(String nameString, Node currentNode) {
        return argumentError(StringUtils.format("encoding %s is already registered", nameString), currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentError(String message, Node currentNode, Throwable javaThrowable) {
        return argumentError(StringOperations.encodeRope(message, UTF8Encoding.INSTANCE), currentNode, javaThrowable);
    }

    public DynamicObject argumentError(Rope message, Node currentNode, Throwable javaThrowable) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getArgumentErrorClass(),
                StringOperations.createString(context, message),
                context.getCallStack().getBacktrace(currentNode, javaThrowable));
    }

    // RuntimeError

    @TruffleBoundary
    public DynamicObject frozenError(Object object, Node currentNode) {
        String className = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(object)).getName();
        return runtimeError(StringUtils.format("can't modify frozen %s", className), currentNode);
    }

    public DynamicObject runtimeErrorNotConstant(Node currentNode) {
        return runtimeError("Truffle::Graal.assert_constant can only be called lexically", currentNode);
    }

    public DynamicObject runtimeErrorCompiled(Node currentNode) {
        return runtimeError("Truffle::Graal.assert_not_compiled can only be called lexically", currentNode);
    }

    public DynamicObject runtimeErrorCoverageNotEnabled(Node currentNode) {
        return runtimeError("coverage measurement is not enabled", currentNode);
    }

    @TruffleBoundary
    public DynamicObject runtimeError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getRuntimeErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    // SystemStackError

    @TruffleBoundary
    public DynamicObject systemStackErrorStackLevelTooDeep(Node currentNode, Throwable javaThrowable) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getSystemStackErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope("stack level too deep", UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode, javaThrowable));
    }

    // Errno

    @TruffleBoundary
    public DynamicObject mathDomainErrorAcos(Node currentNode) {
        return mathDomainError("acos", currentNode);
    }

    @TruffleBoundary
    public DynamicObject mathDomainErrorAcosh(Node currentNode) {
        return mathDomainError("acosh", currentNode);
    }

    @TruffleBoundary
    public DynamicObject mathDomainErrorAsin(Node currentNode) {
        return mathDomainError("asin", currentNode);
    }

    @TruffleBoundary
    public DynamicObject mathDomainErrorAtanh(Node currentNode) {
        return mathDomainError("atanh", currentNode);
    }

    @TruffleBoundary
    public DynamicObject mathDomainErrorGamma(Node currentNode) {
        return mathDomainError("gamma", currentNode);
    }

    @TruffleBoundary
    public DynamicObject mathDomainErrorLog2(Node currentNode) {
        return mathDomainError("log2", currentNode);
    }

    @TruffleBoundary
    public DynamicObject mathDomainErrorLog10(Node currentNode) {
        return mathDomainError("log10", currentNode);
    }

    @TruffleBoundary
    public DynamicObject mathDomainErrorLog(Node currentNode) {
        return mathDomainError("log", currentNode);
    }

    @TruffleBoundary
    public DynamicObject mathDomainError(String method, Node currentNode) {
        return ExceptionOperations.createSystemCallError(
                context.getCoreLibrary().getErrnoClass(Errno.EDOM),
                StringOperations.createString(context, StringOperations.encodeRope(StringUtils.format("Numerical argument is out of domain - \"%s\"", method), UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode), Errno.EDOM.intValue());
    }

    @TruffleBoundary
    public DynamicObject errnoError(int errno, Node currentNode) {
        return errnoError(errno, "", currentNode);
    }

    @TruffleBoundary
    public DynamicObject errnoError(int errno, String extraMessage, Node currentNode) {
        Errno errnoObj = Errno.valueOf(errno);
        DynamicObject errnoClass = context.getCoreLibrary().getErrnoClass(errnoObj);
        if (errnoObj == null || errnoClass == null) {
            return systemCallError(StringUtils.format("Unknown Error (%s)%s", errno, extraMessage), errno, currentNode);
        }

        String fullMessage = StringUtils.format("%s%s", errnoObj.description(), extraMessage);
        DynamicObject errorMessage = StringOperations.createString(context, StringOperations.encodeRope(fullMessage, UTF8Encoding.INSTANCE));

        return ExceptionOperations.createSystemCallError(
            errnoClass,
            errorMessage,
            context.getCallStack().getBacktrace(currentNode), errno);
    }

    // IndexError

    @TruffleBoundary
    public DynamicObject indexErrorOutOfString(int index, Node currentNode) {
        return indexError(StringUtils.format("index %d out of string", index), currentNode);
    }

    @TruffleBoundary
    public DynamicObject indexError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getIndexErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    @TruffleBoundary
    public DynamicObject indexTooSmallError(String type, int index, int length, Node currentNode) {
        return indexError(StringUtils.format("index %d too small for %s; minimum: -%d", index, type, length), currentNode);
    }

    @TruffleBoundary
    public DynamicObject negativeLengthError(int length, Node currentNode) {
        return indexError(StringUtils.format("negative length (%d)", length), currentNode);
    }

    @TruffleBoundary
    public DynamicObject indexErrorInvalidIndex(Node currentNode) {
        return indexError("invalid index", currentNode);
    }

    // LocalJumpError

    @TruffleBoundary
    public DynamicObject localJumpError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getLocalJumpErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    public DynamicObject noBlockGiven(Node currentNode) {
        return localJumpError("no block given", currentNode);
    }

    public DynamicObject breakFromProcClosure(Node currentNode) {
        return localJumpError("break from proc-closure", currentNode);
    }

    public DynamicObject unexpectedReturn(Node currentNode) {
        return localJumpError("unexpected return", currentNode);
    }

    public DynamicObject noBlockToYieldTo(Node currentNode) {
        return localJumpError("no block given (yield)", currentNode);
    }

    // TypeError

    public DynamicObject typeErrorCantCreateInstanceOfSingletonClass(Node currentNode) {
        return typeError("can't create instance of singleton class", currentNode, null);
    }

    @TruffleBoundary
    public DynamicObject superclassMismatch(String name, Node currentNode) {
        return typeError("superclass mismatch for class " + name, currentNode);
    }

    public DynamicObject typeError(String message, Node currentNode) {
        return typeError(message, currentNode, null);
    }

    @TruffleBoundary
    public DynamicObject typeErrorAllocatorUndefinedFor(DynamicObject rubyClass, Node currentNode) {
        String className = Layouts.MODULE.getFields(rubyClass).getName();
        return typeError(StringUtils.format("allocator undefined for %s", className), currentNode);
    }

    public DynamicObject typeErrorCantDefineSingleton(Node currentNode) {
        return typeError("can't define singleton", currentNode);
    }

    public DynamicObject typeErrorCantBeCastedToBigDecimal(Node currentNode) {
        return typeError("could not be casted to BigDecimal", currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorMustHaveWriteMethod(Object object, Node currentNode) {
        return typeError(StringUtils.format("$stdout must have write method, %s given", Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(object)).getName()), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorCantConvertTo(Object from, String toClass, String methodUsed, Object result, Node currentNode) {
        String fromClass = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(from)).getName();
        return typeError(StringUtils.format("can't convert %s to %s (%s#%s gives %s)",
                fromClass, toClass, fromClass, methodUsed, context.getCoreLibrary().getLogicalClass(result).toString()), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorCantConvertInto(Object from, String toClass, Node currentNode) {
        return typeError(StringUtils.format("can't convert %s into %s", Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(from)).getName(), toClass), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorIsNotA(Object value, String expectedType, Node currentNode) {
        return typeErrorIsNotA(value.toString(), expectedType, currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorIsNotA(String value, String expectedType, Node currentNode) {
        return typeError(StringUtils.format("%s is not a %s", value, expectedType), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorIsNotAClassModule(Object value, Node currentNode) {
        return typeError(StringUtils.format("%s is not a class/module", value), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorNoImplicitConversion(Object from, String to, Node currentNode) {
        return typeError(StringUtils.format("no implicit conversion of %s into %s", Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(from)).getName(), to), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorMustBe(String variable, String type, Node currentNode) {
        return typeError(StringUtils.format("value of %s must be %s", variable, type), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorBadCoercion(Object from, String to, String coercionMethod, Object coercedTo, Node currentNode) {
        String badClassName = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(from)).getName();
        return typeError(StringUtils.format("can't convert %s to %s (%s#%s gives %s)",
                badClassName,
                to,
                badClassName,
                coercionMethod,
                Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(coercedTo)).getName()), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorCantDump(Object object, Node currentNode) {
        String logicalClass = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(object)).getName();
        return typeError(StringUtils.format("can't dump %s", logicalClass), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorWrongArgumentType(Object object, String expectedType, Node currentNode) {
        String badClassName = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(object)).getName();
        return typeError(StringUtils.format("wrong argument type %s (expected %s)", badClassName, expectedType), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorAlreadyInitializedClass(Node currentNode) {
        return typeError("already initialized class", currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorSubclassSingletonClass(Node currentNode) {
        return typeError("can't make subclass of singleton class", currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorSubclassClass(Node currentNode) {
        return typeError("can't make subclass of Class", currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorSuperclassMustBeClass(Node currentNode) {
        return typeError("superclass must be a Class", currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorInheritUninitializedClass(Node currentNode) {
        return typeError("can't inherit uninitialized class", currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorRescueInvalidClause(Node currentNode) {
        return typeError("class or module required for rescue clause", currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeError(String message, Node currentNode, Throwable javaThrowable) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getTypeErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode, javaThrowable));
    }

    // NameError

    @TruffleBoundary
    public DynamicObject nameErrorConstantNotDefined(DynamicObject module, String name, Node currentNode) {
        return nameError(StringUtils.format("constant %s::%s not defined", Layouts.MODULE.getFields(module).getName(), name), null, name,  currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorUninitializedConstant(DynamicObject module, String name, Node currentNode) {
        assert RubyGuards.isRubyModule(module);
        final String message;
        if (module == context.getCoreLibrary().getObjectClass()) {
            message = StringUtils.format("uninitialized constant %s", name);
        } else {
            message = StringUtils.format("uninitialized constant %s::%s", Layouts.MODULE.getFields(module).getName(), name);
        }
        return nameError(message, module, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorUninitializedClassVariable(DynamicObject module, String name, Node currentNode) {
        assert RubyGuards.isRubyModule(module);
        return nameError(StringUtils.format("uninitialized class variable %s in %s", name, Layouts.MODULE.getFields(module).getName()), module, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorPrivateConstant(DynamicObject module, String name, Node currentNode) {
        return nameError(StringUtils.format("private constant %s::%s referenced", Layouts.MODULE.getFields(module).getName(), name), module, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorInstanceNameNotAllowable(String name, Object receiver, Node currentNode) {
        return nameError(StringUtils.format("`%s' is not allowable as an instance variable name", name), receiver, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorInstanceVariableNotDefined(String name, Object receiver, Node currentNode) {
        return nameError(StringUtils.format("instance variable %s not defined", name), receiver, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorReadOnly(String name, Node currentNode) {
        return nameError(StringUtils.format("%s is a read-only variable", name), null, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorUndefinedLocalVariableOrMethod(String name, Object receiver, Node currentNode) {
        // TODO: should not be just the class, but rather sth like name_err_mesg_to_str() in MRI error.c
        String className = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(receiver)).getName();
        return nameError(StringUtils.format("undefined local variable or method `%s' for %s", name, className), receiver,  name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorUndefinedMethod(String name, DynamicObject module, Node currentNode) {
        assert RubyGuards.isRubyModule(module);
        return nameError(StringUtils.format("undefined method `%s' for %s", name, Layouts.MODULE.getFields(module).getName()), module, name,  currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorMethodNotDefinedIn(DynamicObject module, String name, Node currentNode) {
        return nameError(StringUtils.format("method `%s' not defined in %s", name, Layouts.MODULE.getFields(module).getName()), module, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorPrivateMethod(String name, DynamicObject module, Node currentNode) {
        return nameError(StringUtils.format("method `%s' for %s is private", name, Layouts.MODULE.getFields(module).getName()), module, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorLocalVariableNotDefined(String name, DynamicObject binding, Node currentNode) {
        assert RubyGuards.isRubyBinding(binding);
        return nameError(StringUtils.format("local variable `%s' not defined for %s", name, binding.toString()), binding, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorClassVariableNotDefined(String name, DynamicObject module, Node currentNode) {
        assert RubyGuards.isRubyModule(module);
        return nameError(StringUtils.format("class variable `%s' not defined for %s", name, Layouts.MODULE.getFields(module).getName()), module, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorImportNotFound(String name, Node currentNode) {
        return nameError(StringUtils.format("import '%s' not found", name), null, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameError(String message, Object receiver, String name, Node currentNode) {
        final DynamicObject nameString = StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE));
        return Layouts.NAME_ERROR.createNameError(
                context.getCoreLibrary().getNameErrorFactory(),
                nameString,
                context.getCallStack().getBacktrace(currentNode),
                receiver,
                context.getSymbolTable().getSymbol(name));
    }

    // NoMethodError

    @TruffleBoundary
    public DynamicObject noMethodError(String message, Object receiver, String name, Object[] args, Node currentNode) {
        final DynamicObject messageString = StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE));
        final DynamicObject argsArray =  createArray(context, args, args.length);
        return Layouts.NO_METHOD_ERROR.createNoMethodError(
                context.getCoreLibrary().getNoMethodErrorFactory(),
                messageString,
                context.getCallStack().getBacktrace(currentNode),
                receiver,
                context.getSymbolTable().getSymbol(name),
                argsArray);
    }

    @TruffleBoundary
    public DynamicObject noSuperMethodOutsideMethodError(Node currentNode) {
        final DynamicObject messageString = StringOperations.createString(context, StringOperations.encodeRope("super called outside of method", UTF8Encoding.INSTANCE));
        // TODO BJF Jul 21, 2016 Review to add receiver
        DynamicObject noMethodError = Layouts.NAME_ERROR.createNameError(
                context.getCoreLibrary().getNoMethodErrorFactory(),
                messageString,
                context.getCallStack().getBacktrace(currentNode),
                null,
                context.getSymbolTable().getSymbol("<unknown>"));
        // FIXME: the name of the method is not known in this case currently
        return noMethodError;
    }

    @TruffleBoundary
    public DynamicObject noSuperMethodError(String name, Object self, Object[] args,  Node currentNode) {
        return noMethodError(StringUtils.format("super: no superclass method `%s'", name), self, name, args, currentNode);
    }

    @TruffleBoundary
    public DynamicObject noMethodErrorOnReceiver(String name, Object receiver, Object[] args, Node currentNode) {
        final DynamicObject logicalClass = context.getCoreLibrary().getLogicalClass(receiver);
        final String moduleName = Layouts.MODULE.getFields(logicalClass).getName();

        // e.g. BasicObject does not have inspect
        final boolean hasInspect = ModuleOperations.lookupMethod(logicalClass, "inspect", Visibility.PUBLIC) != null;
        final Object stringRepresentation = hasInspect ? context.send(receiver, "inspect", null) : context.getCoreLibrary().getNilObject();

        return noMethodError(StringUtils.format("undefined method `%s' for %s:%s", name, stringRepresentation, moduleName), receiver, name, args, currentNode);
    }

    @TruffleBoundary
    public DynamicObject privateMethodError(String name, Object self, Object[] args, Node currentNode) {
        String className = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(self)).getName();
        return noMethodError(StringUtils.format("private method `%s' called for %s", name, className), self, name, args, currentNode);
    }

    @TruffleBoundary
    public DynamicObject protectedMethodError(String name, Object self, Object[] args, Node currentNode) {
        String className = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(self)).getName();
        return noMethodError(StringUtils.format("protected method `%s' called for %s", name, className), self, name, args, currentNode);
    }

    // LoadError

    @TruffleBoundary
    public DynamicObject loadError(String message, String path, Node currentNode) {
        DynamicObject messageString = StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE));
        DynamicObject loadError = ExceptionOperations.createRubyException(context.getCoreLibrary().getLoadErrorClass(), messageString, context.getCallStack().getBacktrace(currentNode));
        if("openssl.so".equals(path)){
            // This is a workaround for the rubygems/security.rb file expecting the error path to be openssl
            path = "openssl";
        }
        loadError.define("@path", StringOperations.createString(context, StringOperations.encodeRope(path, UTF8Encoding.INSTANCE)));
        return loadError;
    }

    @TruffleBoundary
    public DynamicObject loadErrorCannotLoad(String name, Node currentNode) {
        return loadError(StringUtils.format("cannot load such file -- %s", name), name, currentNode);
    }

    // ZeroDivisionError

    public DynamicObject zeroDivisionError(Node currentNode) {
        return zeroDivisionError(currentNode, null);
    }

    @TruffleBoundary
    public DynamicObject zeroDivisionError(Node currentNode, ArithmeticException exception) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getZeroDivisionErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope("divided by 0", UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode, exception));
    }

    // NotImplementedError

    @TruffleBoundary
    public DynamicObject notImplementedError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getNotImplementedErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(StringUtils.format("Method %s not implemented", message),
                        UTF8Encoding.INSTANCE)), context.getCallStack().getBacktrace(currentNode));
    }

    // SyntaxError

    @TruffleBoundary
    public DynamicObject syntaxErrorInvalidRetry(Node currentNode) {
        return syntaxError("Invalid retry", currentNode);
    }

    @TruffleBoundary
    public DynamicObject syntaxError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getSyntaxErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    // FloatDomainError

    @TruffleBoundary
    public DynamicObject floatDomainError(String value, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getFloatDomainErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(value, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    public DynamicObject floatDomainErrorResultsToNaN(Node currentNode) {
        return floatDomainError("Computation results to 'NaN'(Not a Number)", currentNode);
    }

    public DynamicObject floatDomainErrorResultsToInfinity(Node currentNode) {
        return floatDomainError("Computation results to 'Infinity'", currentNode);
    }

    public DynamicObject floatDomainErrorResultsToNegInfinity(Node currentNode) {
        return floatDomainError("Computation results to '-Infinity'", currentNode);
    }

    public DynamicObject floatDomainErrorSqrtNegative(Node currentNode) {
        return floatDomainError("(VpSqrt) SQRT(negative value)", currentNode);
    }

    // IOError

    @TruffleBoundary
    public DynamicObject ioError(String fileName, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getIOErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(StringUtils.format("Error reading file -  %s", fileName), UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    // RangeError

    @TruffleBoundary
    public DynamicObject rangeError(int code, DynamicObject encoding, Node currentNode) {
        assert RubyGuards.isRubyEncoding(encoding);
        return rangeError(StringUtils.format("invalid codepoint %x in %s", code, EncodingOperations.getEncoding(encoding)), currentNode);
    }

    @TruffleBoundary
    public DynamicObject rangeError(long code, DynamicObject encoding, Node currentNode) {
        assert RubyGuards.isRubyEncoding(encoding);
        return rangeError(StringUtils.format("invalid codepoint %x in %s", code, EncodingOperations.getEncoding(encoding)), currentNode);
    }

    @TruffleBoundary
    public DynamicObject rangeError(String type, String value, String range, Node currentNode) {
        return rangeError(StringUtils.format("%s %s out of range of %s", type, value, range), currentNode);
    }

    @TruffleBoundary
    public DynamicObject rangeError(DynamicObject range, Node currentNode) {
        assert RubyGuards.isIntRange(range);
        return rangeError(StringUtils.format("%d..%s%d out of range",
                Layouts.INT_RANGE.getBegin(range),
                Layouts.INT_RANGE.getExcludedEnd(range) ? "." : "",
                Layouts.INT_RANGE.getEnd(range)), currentNode);
    }

    @TruffleBoundary
    public DynamicObject rangeErrorConvertToInt(long value, Node currentNode) {
        final String direction;

        if (value < Integer.MIN_VALUE) {
            direction = "small";
        } else if (value > Integer.MAX_VALUE) {
            direction = "big";
        } else {
            throw new IllegalArgumentException();
        }

        return rangeError(StringUtils.format("integer %d too %s to convert to `int'", value, direction), currentNode);
    }

    @TruffleBoundary
    public DynamicObject rangeError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getRangeErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    // InternalError

    public DynamicObject internalErrorUnsafe(Node currentNode) {
        return internalError("unsafe operation", currentNode, null);
    }

    public DynamicObject internalError(String message, Node currentNode) {
        return internalError(message, currentNode, null);
    }

    public DynamicObject internalErrorAssertConstantNotConstant(Node currentNode) {
        return internalError("Value in Truffle::Graal.assert_constant was not constant", currentNode);
    }

    public DynamicObject internalErrorAssertNotCompiledCompiled(Node currentNode) {
        return internalError("Call to Truffle::Graal.assert_not_compiled was compiled", currentNode);
    }

    @TruffleBoundary
    public DynamicObject internalError(String message, Node currentNode, Throwable javaThrowable) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getRubyTruffleErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope("internal implementation error - " + message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode, javaThrowable));
    }

    // RegexpError

    @TruffleBoundary
    public DynamicObject regexpError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getRegexpErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    // EncodingCompatibilityError

    @TruffleBoundary
    public DynamicObject encodingCompatibilityErrorIncompatible(Encoding a, Encoding b, Node currentNode) {
        return encodingCompatibilityError(StringUtils.format("incompatible character encodings: %s and %s", a, b), currentNode);
    }

    @TruffleBoundary
    public DynamicObject encodingCompatibilityErrorIncompatibleWithOperation(Encoding encoding, Node currentNode) {
        return encodingCompatibilityError(StringUtils.format("incompatible encoding with this operation: %s", encoding), currentNode);
    }

    @TruffleBoundary
    public DynamicObject encodingCompatibilityError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getEncodingCompatibilityErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    // FiberError

    @TruffleBoundary
    public DynamicObject fiberError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getFiberErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    public DynamicObject deadFiberCalledError(Node currentNode) {
        return fiberError("dead fiber called", currentNode);
    }

    public DynamicObject yieldFromRootFiberError(Node currentNode) {
        return fiberError("can't yield from root fiber", currentNode);
    }

    // ThreadError

    @TruffleBoundary
    public DynamicObject threadError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getThreadErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    public DynamicObject threadErrorKilledThread(Node currentNode) {
        return threadError("killed thread", currentNode);
    }

    public DynamicObject threadErrorRecursiveLocking(Node currentNode) {
        return threadError("deadlock; recursive locking", currentNode);
    }

    public DynamicObject threadErrorUnlockNotLocked(Node currentNode) {
        return threadError("Attempt to unlock a mutex which is not locked", currentNode);
    }

    public DynamicObject threadErrorAlreadyLocked(Node currentNode) {
        return threadError("Attempt to unlock a mutex which is locked by another thread", currentNode);
    }

    // SecurityError

    @TruffleBoundary
    public DynamicObject securityError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getSecurityErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    // SystemCallError

    @TruffleBoundary
    public DynamicObject systemCallError(String message, int errno, Node currentNode) {
        return ExceptionOperations.createSystemCallError(
                context.getCoreLibrary().getSystemCallErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode), errno);
    }

    // IO::EAGAINWaitReadable, IO::EAGAINWaitWritable

    @TruffleBoundary
    public DynamicObject eAGAINWaitReadable(Node currentNode) {
        return ExceptionOperations.createSystemCallError(
                context.getCoreLibrary().getEagainWaitReadable(),
                coreStrings().RESOURCE_TEMP_UNAVAIL.createInstance(),
                context.getCallStack().getBacktrace(currentNode), Errno.EAGAIN.intValue());
    }

    @TruffleBoundary
    public DynamicObject eAGAINWaitWritable(Node currentNode) {
        return ExceptionOperations.createSystemCallError(
                context.getCoreLibrary().getEagainWaitWritable(),
                coreStrings().RESOURCE_TEMP_UNAVAIL.createInstance(),
                context.getCallStack().getBacktrace(currentNode), Errno.EAGAIN.intValue());
    }

    // SystemExit

    @TruffleBoundary
    public DynamicObject systemExit(int exitStatus, Node currentNode) {
        final DynamicObject message = StringOperations.createString(context, StringOperations.encodeRope("exit", UTF8Encoding.INSTANCE));
        final DynamicObject systemExit = ExceptionOperations.createRubyException(context.getCoreLibrary().getSystemExitClass(), message, context.getCallStack().getBacktrace(currentNode));
        systemExit.define("@status", exitStatus);
        return systemExit;
    }

    // Helpers

    private CoreStrings coreStrings() {
        return context.getCoreStrings();
    }

}
