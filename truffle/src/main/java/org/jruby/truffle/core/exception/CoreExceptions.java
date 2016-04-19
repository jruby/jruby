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
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.encoding.EncodingOperations;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.CoreStrings;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;

public class CoreExceptions {

    private final RubyContext context;

    public CoreExceptions(RubyContext context) {
        this.context = context;
    }

    // ArgumentError

    public DynamicObject argumentErrorOneHashRequired(Node currentNode) {
        return argumentError(coreStrings().ONE_HASH_REQUIRED.getRope(), currentNode, null);
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

    @TruffleBoundary
    public DynamicObject argumentErrorUnknownKeyword(Object name, Node currentNode) {
        return argumentError("unknown keyword: " + name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentErrorInvalidRadix(int radix, Node currentNode) {
        return argumentError(String.format("invalid radix %d", radix), currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentErrorMissingKeyword(String name, Node currentNode) {
        return argumentError(String.format("missing keyword: %s", name), currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentError(int passed, int required, Node currentNode) {
        return argumentError(String.format("wrong number of arguments (%d for %d)", passed, required), currentNode);
    }

    @TruffleBoundary
    public DynamicObject argumentError(int passed, int required, int optional, Node currentNode) {
        return argumentError(String.format("wrong number of arguments (%d for %d..%d)", passed, required, required + optional), currentNode);
    }

    public DynamicObject argumentErrorEmptyVarargs(Node currentNode) {
        return argumentError(coreStrings().WRONG_ARGS_ZERO_PLUS_ONE.getRope(), currentNode, null);
    }

    @TruffleBoundary
    public DynamicObject argumentErrorWrongArgumentType(Object object, String expectedType, Node currentNode) {
        String badClassName = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(object)).getName();
        return argumentError(String.format("wrong argument type %s (expected %s)", badClassName, expectedType), currentNode);
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
    public DynamicObject frozenError(String className, Node currentNode) {
        return runtimeError(String.format("can't modify frozen %s", className), currentNode);
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
    public DynamicObject mathDomainError(String method, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getErrnoClass(Errno.EDOM),
                StringOperations.createString(context, StringOperations.encodeRope(String.format("Numerical argument is out of domain - \"%s\"", method), UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    @TruffleBoundary
    public DynamicObject errnoError(int errno, Node currentNode) {
        Errno errnoObj = Errno.valueOf(errno);
        if (errnoObj == null) {
            return systemCallError(String.format("Unknown Error (%s)", errno), currentNode);
        }

        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getErrnoClass(errnoObj),
                StringOperations.createString(context, StringOperations.encodeRope(errnoObj.description(), UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    @TruffleBoundary
    public DynamicObject errnoError(int errno, String message, Node currentNode) {
        Errno errnoObj = Errno.valueOf(errno);
        if (errnoObj == null) {
            return systemCallError(String.format("Unknown Error (%s) - %s", errno, message), currentNode);
        }

        final DynamicObject errorMessage = StringOperations.createString(context, StringOperations.encodeRope(String.format("%s - %s", errnoObj.description(), message), UTF8Encoding.INSTANCE));

        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getErrnoClass(errnoObj),
                errorMessage,
                context.getCallStack().getBacktrace(currentNode));
    }

    // IndexError

    @TruffleBoundary
    public DynamicObject indexError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getIndexErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    @TruffleBoundary
    public DynamicObject indexTooSmallError(String type, int index, int length, Node currentNode) {
        return indexError(String.format("index %d too small for %s; minimum: -%d", index, type, length), currentNode);
    }

    @TruffleBoundary
    public DynamicObject negativeLengthError(int length, Node currentNode) {
        return indexError(String.format("negative length (%d)", length), currentNode);
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
        return typeError(String.format("allocator undefined for %s", className), currentNode);
    }

    public DynamicObject typeErrorCantDefineSingleton(Node currentNode) {
        return typeError("can't define singleton", currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorMustHaveWriteMethod(Object object, Node currentNode) {
        return typeError(String.format("$stdout must have write method, %s given", Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(object)).getName()), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorCantConvertTo(Object from, String toClass, String methodUsed, Object result, Node currentNode) {
        String fromClass = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(from)).getName();
        return typeError(String.format("can't convert %s to %s (%s#%s gives %s)",
                fromClass, toClass, fromClass, methodUsed, context.getCoreLibrary().getLogicalClass(result).toString()), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorCantConvertInto(Object from, String toClass, Node currentNode) {
        return typeError(String.format("can't convert %s into %s", Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(from)).getName(), toClass), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorIsNotA(Object value, String expectedType, Node currentNode) {
        return typeErrorIsNotA(value.toString(), expectedType, currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorIsNotA(String value, String expectedType, Node currentNode) {
        return typeError(String.format("%s is not a %s", value, expectedType), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorNoImplicitConversion(Object from, String to, Node currentNode) {
        return typeError(String.format("no implicit conversion of %s into %s", Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(from)).getName(), to), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorMustBe(String variable, String type, Node currentNode) {
        return typeError(String.format("value of %s must be %s", variable, type), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorBadCoercion(Object from, String to, String coercionMethod, Object coercedTo, Node currentNode) {
        String badClassName = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(from)).getName();
        return typeError(String.format("can't convert %s to %s (%s#%s gives %s)",
                badClassName,
                to,
                badClassName,
                coercionMethod,
                Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(coercedTo)).getName()), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorCantDump(Object object, Node currentNode) {
        String logicalClass = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(object)).getName();
        return typeError(String.format("can't dump %s", logicalClass), currentNode);
    }

    @TruffleBoundary
    public DynamicObject typeErrorWrongArgumentType(Object object, String expectedType, Node currentNode) {
        String badClassName = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(object)).getName();
        return typeError(String.format("wrong argument type %s (expected %s)", badClassName, expectedType), currentNode);
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
        return nameError(String.format("constant %s::%s not defined", Layouts.MODULE.getFields(module).getName(), name), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorUninitializedConstant(DynamicObject module, String name, Node currentNode) {
        assert RubyGuards.isRubyModule(module);
        final String message;
        if (module == context.getCoreLibrary().getObjectClass()) {
            message = String.format("uninitialized constant %s", name);
        } else {
            message = String.format("uninitialized constant %s::%s", Layouts.MODULE.getFields(module).getName(), name);
        }
        return nameError(message, name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorUninitializedClassVariable(DynamicObject module, String name, Node currentNode) {
        assert RubyGuards.isRubyModule(module);
        return nameError(String.format("uninitialized class variable %s in %s", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorPrivateConstant(DynamicObject module, String name, Node currentNode) {
        return nameError(String.format("private constant %s::%s referenced", Layouts.MODULE.getFields(module).getName(), name), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorInstanceNameNotAllowable(String name, Node currentNode) {
        return nameError(String.format("`%s' is not allowable as an instance variable name", name), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorInstanceVariableNotDefined(String name, Node currentNode) {
        return nameError(String.format("instance variable %s not defined", name), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorReadOnly(String name, Node currentNode) {
        return nameError(String.format("%s is a read-only variable", name), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorUndefinedLocalVariableOrMethod(String name, Object receiver, Node currentNode) {
        // TODO: should not be just the class, but rather sth like name_err_mesg_to_str() in MRI error.c
        String className = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(receiver)).getName();
        return nameError(String.format("undefined local variable or method `%s' for %s", name, className), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorUndefinedMethod(String name, DynamicObject module, Node currentNode) {
        assert RubyGuards.isRubyModule(module);
        return nameError(String.format("undefined method `%s' for %s", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorMethodNotDefinedIn(DynamicObject module, String name, Node currentNode) {
        return nameError(String.format("method `%s' not defined in %s", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorPrivateMethod(String name, DynamicObject module, Node currentNode) {
        return nameError(String.format("method `%s' for %s is private", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorLocalVariableNotDefined(String name, DynamicObject binding, Node currentNode) {
        assert RubyGuards.isRubyBinding(binding);
        return nameError(String.format("local variable `%s' not defined for %s", name, binding.toString()), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameErrorClassVariableNotDefined(String name, DynamicObject module, Node currentNode) {
        assert RubyGuards.isRubyModule(module);
        return nameError(String.format("class variable `%s' not defined for %s", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject nameError(String message, String name, Node currentNode) {
        final DynamicObject nameString = StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE));
        DynamicObject nameError = ExceptionOperations.createRubyException(context.getCoreLibrary().getNameErrorClass(), nameString, context.getCallStack().getBacktrace(currentNode));
        nameError.define("@name", context.getSymbolTable().getSymbol(name), 0);
        return nameError;
    }

    // NoMethodError

    @TruffleBoundary
    public DynamicObject noMethodError(String message, String name, Node currentNode) {
        final DynamicObject messageString = StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE));
        DynamicObject noMethodError = ExceptionOperations.createRubyException(context.getCoreLibrary().getNoMethodErrorClass(), messageString, context.getCallStack().getBacktrace(currentNode));
        noMethodError.define("@name", context.getSymbolTable().getSymbol(name), 0);
        return noMethodError;
    }

    @TruffleBoundary
    public DynamicObject noSuperMethodOutsideMethodError(Node currentNode) {
        DynamicObject noMethodError = noMethodError("super called outside of method", "<unknown>", currentNode);
        noMethodError.define("@name", context.getCoreLibrary().getNilObject(), 0); // FIXME: the name of the method is not known in this case currently
        return noMethodError;
    }

    @TruffleBoundary
    public DynamicObject noSuperMethodError(String name, Node currentNode) {
        return noMethodError(String.format("super: no superclass method `%s'", name), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject noMethodErrorOnReceiver(String name, Object receiver, Node currentNode) {
        final DynamicObject logicalClass = context.getCoreLibrary().getLogicalClass(receiver);
        final String moduleName = Layouts.MODULE.getFields(logicalClass).getName();

        // e.g. BasicObject does not have to_s
        final boolean hasToS = ModuleOperations.lookupMethod(logicalClass, "to_s", Visibility.PUBLIC) != null;
        final Object stringRepresentation = hasToS ? context.send(receiver, "to_s", null) : context.getCoreLibrary().getNilObject();

        return noMethodError(String.format("undefined method `%s' for %s:%s", name, stringRepresentation, moduleName), name, currentNode);
    }

    @TruffleBoundary
    public DynamicObject privateMethodError(String name, Object self, Node currentNode) {
        String className = Layouts.MODULE.getFields(context.getCoreLibrary().getLogicalClass(self)).getName();
        return noMethodError(String.format("private method `%s' called for %s", name, className), name, currentNode);
    }

    // LoadError

    @TruffleBoundary
    public DynamicObject loadError(String message, String path, Node currentNode) {
        DynamicObject messageString = StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE));
        DynamicObject loadError = ExceptionOperations.createRubyException(context.getCoreLibrary().getLoadErrorClass(), messageString, context.getCallStack().getBacktrace(currentNode));
        loadError.define("@path", StringOperations.createString(context, StringOperations.encodeRope(path, UTF8Encoding.INSTANCE)), 0);
        return loadError;
    }

    @TruffleBoundary
    public DynamicObject loadErrorCannotLoad(String name, Node currentNode) {
        return loadError(String.format("cannot load such file -- %s", name), name, currentNode);
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
                StringOperations.createString(context, StringOperations.encodeRope(String.format("Method %s not implemented", message),
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

    // IOError

    @TruffleBoundary
    public DynamicObject ioError(String fileName, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getIOErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(String.format("Error reading file -  %s", fileName), UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    // RangeError

    @TruffleBoundary
    public DynamicObject rangeError(int code, DynamicObject encoding, Node currentNode) {
        assert RubyGuards.isRubyEncoding(encoding);
        return rangeError(String.format("invalid codepoint %x in %s", code, EncodingOperations.getEncoding(encoding)), currentNode);
    }

    @TruffleBoundary
    public DynamicObject rangeError(String type, String value, String range, Node currentNode) {
        return rangeError(String.format("%s %s out of range of %s", type, value, range), currentNode);
    }

    @TruffleBoundary
    public DynamicObject rangeError(DynamicObject range, Node currentNode) {
        assert RubyGuards.isIntegerFixnumRange(range);
        return rangeError(String.format("%d..%s%d out of range",
                Layouts.INTEGER_FIXNUM_RANGE.getBegin(range),
                Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range) ? "." : "",
                Layouts.INTEGER_FIXNUM_RANGE.getEnd(range)), currentNode);
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
        return internalError("Value in Truffle::Primitive.assert_constant was not constant", currentNode);
    }

    public DynamicObject internalErrorAssertNotCompiledCompiled(Node currentNode) {
        return internalError("Call to Truffle::Primitive.assert_not_compiled was compiled", currentNode);
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
    public DynamicObject encodingCompatibilityErrorIncompatible(String a, String b, Node currentNode) {
        return encodingCompatibilityError(String.format("incompatible character encodings: %s and %s", a, b), currentNode);
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
    public DynamicObject systemCallError(String message, Node currentNode) {
        return ExceptionOperations.createRubyException(
                context.getCoreLibrary().getSystemCallErrorClass(),
                StringOperations.createString(context, StringOperations.encodeRope(message, UTF8Encoding.INSTANCE)),
                context.getCallStack().getBacktrace(currentNode));
    }

    // SystemExit

    @TruffleBoundary
    public DynamicObject systemExit(int exitStatus, Node currentNode) {
        final DynamicObject message = StringOperations.createString(context, StringOperations.encodeRope("exit", UTF8Encoding.INSTANCE));
        final DynamicObject systemExit = ExceptionOperations.createRubyException(context.getCoreLibrary().getSystemExitClass(), message, context.getCallStack().getBacktrace(currentNode));
        systemExit.define("@status", exitStatus, 0);
        return systemExit;
    }

    // Helpers

    private CoreStrings coreStrings() {
        return context.getCoreStrings();
    }

}
