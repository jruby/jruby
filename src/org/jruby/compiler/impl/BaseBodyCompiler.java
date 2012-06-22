/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.impl;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.jcodings.Encoding;
import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyMatchData;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.RubyRange;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ast.NodeType;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.ArgumentsCallback;
import org.jruby.compiler.ArrayCallback;
import org.jruby.compiler.BranchCallback;
import org.jruby.compiler.CompilerCallback;
import org.jruby.compiler.InvocationCompiler;
import org.jruby.compiler.BodyCompiler;
import org.jruby.compiler.FastSwitchType;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.VariableCompiler;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.ReOptions;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InstanceVariables;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.util.ByteList;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.SafePropertyAccessor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import static org.objectweb.asm.Opcodes.*;
import static org.jruby.util.CodegenUtils.*;

/**
 * BaseBodyCompiler encapsulates all common behavior between BodyCompiler
 * implementations.
 */
public abstract class BaseBodyCompiler implements BodyCompiler {
    protected SkinnyMethodAdapter method;
    protected VariableCompiler variableCompiler;
    protected InvocationCompiler invocationCompiler;
    protected int argParamCount;
    protected Label[] currentLoopLabels;
    protected Label scopeStart = new Label();
    protected Label scopeEnd = new Label();
    protected Label redoJump;
    protected boolean inNestedMethod = false;
    private int lastLine = -1;
    private int lastPositionLine = -1;
    protected StaticScope scope;
    protected ASTInspector inspector;
    protected String methodName;
    protected String rubyName;
    protected StandardASMCompiler script;

    public BaseBodyCompiler(StandardASMCompiler scriptCompiler, String methodName, String rubyName, ASTInspector inspector, StaticScope scope) {
        this.script = scriptCompiler;
        this.scope = scope;
        this.inspector = inspector;
        this.methodName = methodName;
        this.rubyName = rubyName;
        this.argParamCount = getActualArgsCount(scope);

        method = new SkinnyMethodAdapter(script.getClassVisitor(), ACC_PUBLIC | ACC_STATIC, methodName, getSignature(), null, null);

        createVariableCompiler();
        invocationCompiler = OptoFactory.newInvocationCompiler(this, method);
    }

    public String getNativeMethodName() {
        return methodName;
    }

    public String getRubyName() {
        return rubyName;
    }

    protected boolean shouldUseBoxedArgs(StaticScope scope) {
        return scope.getRestArg() >= 0 || scope.getRestArg() == -2 || scope.getOptionalArgs() > 0 || scope.getRequiredArgs() > 3;
    }

    protected int getActualArgsCount(StaticScope scope) {
        if (shouldUseBoxedArgs(scope)) {
            return 1; // use IRubyObject[]
        } else {
            return scope.getRequiredArgs(); // specific arity
        }
    }

    protected abstract String getSignature();

    protected abstract void createVariableCompiler();

    public abstract void beginMethod(CompilerCallback args, StaticScope scope);

    public abstract void endBody();

    public BodyCompiler chainToMethod(String methodName) {
        BodyCompiler compiler = outline(methodName);
        endBody();
        return compiler;
    }

    public void beginChainedMethod() {
        method.start();

        method.aload(StandardASMCompiler.THREADCONTEXT_INDEX);
        method.invokevirtual(p(ThreadContext.class), "getCurrentScope", sig(DynamicScope.class));
        method.astore(getDynamicScopeIndex());

        // if more than 4 locals, get the locals array too
        if (scope.getNumberOfVariables() > 4) {
            method.aload(getDynamicScopeIndex());
            method.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
            method.astore(getVarsArrayIndex());
        }

        // visit a label to start scoping for local vars in this method
        method.label(scopeStart);
    }

    public abstract BaseBodyCompiler outline(String methodName);

    public StandardASMCompiler getScriptCompiler() {
        return script;
    }

    public void lineNumber(ISourcePosition position) {
        int thisLine = position.getStartLine();

        // No point in updating number if last number was same value.
        if (thisLine != lastLine) {
            lastLine = thisLine;
        } else {
            return;
        }

        Label line = new Label();
        method.label(line);
        method.visitLineNumber(thisLine + 1, line);
    }

    public int getLastLine() {
        return lastLine;
    }

    public void loadThreadContext() {
        method.aload(StandardASMCompiler.THREADCONTEXT_INDEX);
    }

    public void loadSelf() {
        method.aload(StandardASMCompiler.SELF_INDEX);
    }

    protected int getClosureIndex() {
        return StandardASMCompiler.ARGS_INDEX + argParamCount + StandardASMCompiler.CLOSURE_OFFSET;
    }

    protected int getPreviousExceptionIndex() {
        return StandardASMCompiler.ARGS_INDEX + argParamCount + StandardASMCompiler.PREVIOUS_EXCEPTION_OFFSET;
    }

    protected int getDynamicScopeIndex() {
        return StandardASMCompiler.ARGS_INDEX + argParamCount + StandardASMCompiler.DYNAMIC_SCOPE_OFFSET;
    }

    protected int getVarsArrayIndex() {
        return StandardASMCompiler.ARGS_INDEX + argParamCount + StandardASMCompiler.VARS_ARRAY_OFFSET;
    }

    protected int getFirstTempIndex() {
        return StandardASMCompiler.ARGS_INDEX + argParamCount + StandardASMCompiler.FIRST_TEMP_OFFSET;
    }

    protected int getExceptionIndex() {
        return StandardASMCompiler.ARGS_INDEX + argParamCount + StandardASMCompiler.EXCEPTION_OFFSET;
    }

    public void loadThis() {
        method.aload(StandardASMCompiler.THIS);
    }

    public void loadRuntime() {
        loadThreadContext();
        method.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
    }

    public void loadBlock() {
        method.aload(getClosureIndex());
    }

    public void loadNil() {
        loadThreadContext();
        method.getfield(p(ThreadContext.class), "nil", ci(IRubyObject.class));
    }

    public void loadNull() {
        method.aconst_null();
    }

    public void loadObject() {
        loadRuntime();

        invokeRuby("getObject", sig(RubyClass.class, params()));
    }

    /**
     * This is for utility methods used by the compiler, to reduce the amount of code generation
     * necessary.  All of these live in CompilerHelpers.
     */
    public void invokeUtilityMethod(String methodName, String signature) {
        method.invokestatic(p(RuntimeHelpers.class), methodName, signature);
    }

    public void invokeThreadContext(String methodName, String signature) {
        method.invokevirtual(StandardASMCompiler.THREADCONTEXT, methodName, signature);
    }

    public void invokeRuby(String methodName, String signature) {
        method.invokevirtual(StandardASMCompiler.RUBY, methodName, signature);
    }

    public void invokeIRubyObject(String methodName, String signature) {
        method.invokeinterface(StandardASMCompiler.IRUBYOBJECT, methodName, signature);
    }

    public void consumeCurrentValue() {
        method.pop();
    }

    public void duplicateCurrentValue() {
        method.dup();
    }

    public void swapValues() {
        method.swap();
    }

    public void reverseValues(int count) {
        switch (count) {
        case 2:
            method.swap();
            break;
        case 3:
            method.dup_x2();
            method.pop();
            method.swap();
            break;
        case 4:
            method.swap();
            method.dup2_x2();
            method.pop2();
            method.swap();
            break;
        case 5:
        case 6:
        case 7:
        case 8:
        case 9:
        case 10:
            // up to ten, stuff into tmp locals, load in reverse order, and assign
            // FIXME: There's probably a slightly smarter way, but is it important?
            int[] tmpLocals = new int[count];
            for (int i = 0; i < count; i++) {
                tmpLocals[i] = getVariableCompiler().grabTempLocal();
                getVariableCompiler().setTempLocal(tmpLocals[i]);
            }
            for (int i = 0; i < count; i++) {
                getVariableCompiler().getTempLocal(tmpLocals[i]);
                getVariableCompiler().releaseTempLocal();
            }
            break;
        default:
            throw new NotCompilableException("can't reverse more than ten values on the stack");
        }
    }

    public void retrieveSelf() {
        loadSelf();
    }

    public void retrieveSelfClass() {
        loadSelf();
        metaclass();
    }

    public VariableCompiler getVariableCompiler() {
        return variableCompiler;
    }

    public InvocationCompiler getInvocationCompiler() {
        return invocationCompiler;
    }

    public void assignConstantInCurrent(String name) {
        loadThreadContext();
        method.ldc(name);
        invokeUtilityMethod("setConstantInCurrent", sig(IRubyObject.class, params(IRubyObject.class, ThreadContext.class, String.class)));
    }

    public void assignConstantInModule(String name) {
        method.ldc(name);
        loadThreadContext();
        invokeUtilityMethod("setConstantInModule", sig(IRubyObject.class, IRubyObject.class, IRubyObject.class, String.class, ThreadContext.class));
    }

    public void assignConstantInObject(String name) {
        // load Object under value
        loadObject();

        assignConstantInModule(name);
    }

    public void retrieveConstant(String name) {
        script.getCacheCompiler().cacheConstant(this, name);
    }

    public void retrieveConstantFromModule(String name) {
        invokeUtilityMethod("checkIsModule", sig(RubyModule.class, IRubyObject.class));
        script.getCacheCompiler().cacheConstantFrom(this, name);
    }

    public void retrieveConstantFromObject(String name) {
        loadObject();
        script.getCacheCompiler().cacheConstantFrom(this, name);
    }

    public void retrieveClassVariable(String name) {
        loadThreadContext();
        loadRuntime();
        loadSelf();
        method.ldc(name);

        invokeUtilityMethod("fastFetchClassVariable", sig(IRubyObject.class, params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class)));
    }

    public void assignClassVariable(String name) {
        loadThreadContext();
        method.swap();
        loadRuntime();
        method.swap();
        loadSelf();
        method.swap();
        method.ldc(name);
        method.swap();

        invokeUtilityMethod("fastSetClassVariable", sig(IRubyObject.class, params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class, IRubyObject.class)));
    }

    public void assignClassVariable(String name, CompilerCallback value) {
        loadThreadContext();
        loadRuntime();
        loadSelf();
        method.ldc(name);
        value.call(this);

        invokeUtilityMethod("fastSetClassVariable", sig(IRubyObject.class, params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class, IRubyObject.class)));
    }

    public void declareClassVariable(String name) {
        loadThreadContext();
        method.swap();
        loadRuntime();
        method.swap();
        loadSelf();
        method.swap();
        method.ldc(name);
        method.swap();

        invokeUtilityMethod("fastDeclareClassVariable", sig(IRubyObject.class, params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class, IRubyObject.class)));
    }

    public void declareClassVariable(String name, CompilerCallback value) {
        loadThreadContext();
        loadRuntime();
        loadSelf();
        method.ldc(name);
        value.call(this);

        invokeUtilityMethod("fastDeclareClassVariable", sig(IRubyObject.class, params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class, IRubyObject.class)));
    }

    public void createNewFloat(double value) {
        script.getCacheCompiler().cacheFloat(this, value);
    }

    public void createNewFixnum(long value) {
        script.getCacheCompiler().cacheFixnum(this, value);
    }

    public void createNewBignum(BigInteger value) {
        loadRuntime();
        script.getCacheCompiler().cacheBigInteger(this, value);
        method.invokestatic(p(RubyBignum.class), "newBignum", sig(RubyBignum.class, params(Ruby.class, BigInteger.class)));
    }

    public void createNewString(ArrayCallback callback, int count, Encoding encoding) {
        loadRuntime();
        method.ldc(StandardASMCompiler.STARTING_DSTR_FACTOR * count);
        if (encoding == null) {
            method.invokestatic(p(RubyString.class), "newStringLight", sig(RubyString.class, Ruby.class, int.class));
        } else {
            script.getCacheCompiler().cacheEncoding(this, encoding);
            method.invokestatic(p(RubyString.class), "newStringLight", sig(RubyString.class, Ruby.class, int.class, Encoding.class));
        }

        for (int i = 0; i < count; i++) {
            callback.nextValue(this, null, i);
            if (encoding != null) {
                method.invokevirtual(p(RubyString.class), "append19", sig(RubyString.class, params(IRubyObject.class)));
            } else {
                method.invokevirtual(p(RubyString.class), "append", sig(RubyString.class, params(IRubyObject.class)));
            }
        }
    }

    public void createNewSymbol(ArrayCallback callback, int count, Encoding encoding) {
        loadRuntime();
        createNewString(callback, count, encoding);
        toJavaString();
        invokeRuby("newSymbol", sig(RubySymbol.class, params(String.class)));
    }

    public void createNewString(ByteList value, int codeRange) {
        script.getCacheCompiler().cacheString(this, value, codeRange);
    }

    public void createNewSymbol(String name) {
        script.getCacheCompiler().cacheSymbol(this, name);
    }

    public void createNewArray(boolean lightweight) {
        loadRuntime();
        // put under object array already present
        method.swap();

        if (lightweight) {
            method.invokestatic(p(RubyArray.class), "newArrayNoCopyLight", sig(RubyArray.class, params(Ruby.class, IRubyObject[].class)));
        } else {
            method.invokestatic(p(RubyArray.class), "newArrayNoCopy", sig(RubyArray.class, params(Ruby.class, IRubyObject[].class)));
        }
    }

    public void createNewArray(Object[] sourceArray, ArrayCallback callback, boolean lightweight) {
        loadRuntime();

        buildRubyArray(sourceArray, callback, lightweight);
    }

    public void createNewLiteralArray(Object[] sourceArray, ArrayCallback callback, boolean lightweight) {
        buildRubyLiteralArray(sourceArray, callback, lightweight);
    }

    public void createEmptyArray() {
        loadRuntime();

        invokeRuby("newArray", sig(RubyArray.class));
    }

    public void createObjectArray(Object[] sourceArray, ArrayCallback callback) {
        buildObjectArray(StandardASMCompiler.IRUBYOBJECT, sourceArray, callback);
    }

    private void buildObjectArray(String type, Object[] sourceArray, ArrayCallback callback) {
        if (sourceArray.length == 0) {
            method.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
        } else if (sourceArray.length <= RuntimeHelpers.MAX_SPECIFIC_ARITY_OBJECT_ARRAY) {
            // if we have a specific-arity helper to construct an array for us, use that
            for (int i = 0; i < sourceArray.length; i++) {
                callback.nextValue(this, sourceArray, i);
            }
            invokeUtilityMethod("constructObjectArray", sig(IRubyObject[].class, params(IRubyObject.class, sourceArray.length)));
        } else {
            // brute force construction inline
            method.pushInt(sourceArray.length);
            method.anewarray(type);

            for (int i = 0; i < sourceArray.length; i++) {
                method.dup();
                method.pushInt(i);

                callback.nextValue(this, sourceArray, i);

                method.arraystore();
            }
        }
    }

    private void buildRubyArray(Object[] sourceArray, ArrayCallback callback, boolean light) {
        if (sourceArray.length == 0) {
            method.invokestatic(p(RubyArray.class), "newEmptyArray", sig(RubyArray.class, Ruby.class));
        } else if (sourceArray.length <= RuntimeHelpers.MAX_SPECIFIC_ARITY_OBJECT_ARRAY) {
            // if we have a specific-arity helper to construct an array for us, use that
            for (int i = 0; i < sourceArray.length; i++) {
                callback.nextValue(this, sourceArray, i);
            }
            invokeUtilityMethod("constructRubyArray", sig(RubyArray.class, params(Ruby.class, IRubyObject.class, sourceArray.length)));
        } else {
            // brute force construction
            
            // construct array all at once
            method.pushInt(sourceArray.length);
            invokeUtilityMethod("anewarrayIRubyObjects", sig(IRubyObject[].class, int.class));

            // iterate over elements, stuffing every ten into array in batches
            int i = 0;
            for (; i < sourceArray.length; i++) {
                callback.nextValue(this, sourceArray, i);

                if ((i + 1) % 10 == 0) {
                    method.pushInt(i - 9);
                    invokeUtilityMethod("aastoreIRubyObjects", sig(IRubyObject[].class, params(IRubyObject[].class, IRubyObject.class, 10, int.class)));
                }
            }
            
            // stuff remaining into array
            int remain = i % 10;
            if (remain != 0) {
                method.pushInt(i - remain);
                invokeUtilityMethod("aastoreIRubyObjects", sig(IRubyObject[].class, params(IRubyObject[].class, IRubyObject.class, remain, int.class)));
            }
            
            // construct RubyArray wrapper
            if (light) {
                method.invokestatic(p(RubyArray.class), "newArrayNoCopyLight", sig(RubyArray.class, Ruby.class, IRubyObject[].class));
            } else {
                method.invokestatic(p(RubyArray.class), "newArrayNoCopy", sig(RubyArray.class, Ruby.class, IRubyObject[].class));
            }
        }
    }

    private void buildRubyLiteralArray(Object[] sourceArray, ArrayCallback callback, boolean light) {
        if (sourceArray.length < 100) {
            // don't chunk arrays smaller than 100 elements
            loadRuntime();
            buildRubyArray(sourceArray, callback, light);
        } else {
            // populate the array in a separate series of methods
            SkinnyMethodAdapter oldMethod = method;

            // prepare the first builder in the chain
            String newMethodName = "array_builder_" + script.getAndIncrementMethodIndex() + "";
            method = new SkinnyMethodAdapter(
                    script.getClassVisitor(),
                    ACC_PRIVATE | ACC_SYNTHETIC | ACC_STATIC,
                    newMethodName,
                    sig(IRubyObject[].class, "L" + script.getClassname() + ";", ThreadContext.class, IRubyObject[].class),
                    null,
                    null);
            method.start();

            for (int i = 0; i < sourceArray.length; i++) {
                // for every hundred elements, chain to the next call
                if ((i + 1) % 100 == 0) {
                    String nextName = "array_builder_" + script.getAndIncrementMethodIndex() + "";

                    method.aloadMany(0, 1, 2);
                    method.invokestatic(script.getClassname(), nextName,
                            sig(IRubyObject[].class, "L" + script.getClassname() + ";", ThreadContext.class, IRubyObject[].class));
                    method.areturn();
                    method.end();
                    
                    method = new SkinnyMethodAdapter(
                            script.getClassVisitor(),
                            ACC_PRIVATE | ACC_SYNTHETIC | ACC_STATIC,
                            nextName,
                            sig(IRubyObject[].class, "L" + script.getClassname() + ";", ThreadContext.class, IRubyObject[].class),
                            null,
                            null);
                    method.start();
                }

                method.aload(2);
                method.pushInt(i);

                callback.nextValue(this, sourceArray, i);

                method.arraystore();
            }

            // close out the last method in the chain
            method.aload(2);
            method.areturn();
            method.end();

            // restore original method, prepare runtime and array, and invoke the chain
            method = oldMethod;

            loadRuntime(); // for newArray* call below

            // chain invoke
            method.aload(StandardASMCompiler.THIS);
            method.aload(StandardASMCompiler.THREADCONTEXT_INDEX);
            method.pushInt(sourceArray.length);
            method.anewarray(p(IRubyObject.class));
            method.invokestatic(script.getClassname(), newMethodName,
                    sig(IRubyObject[].class, "L" + script.getClassname() + ";", ThreadContext.class, IRubyObject[].class));

            // array construct
            if (light) {
                method.invokestatic(p(RubyArray.class), "newArrayNoCopyLight", sig(RubyArray.class, Ruby.class, IRubyObject[].class));
            } else {
                method.invokestatic(p(RubyArray.class), "newArrayNoCopy", sig(RubyArray.class, Ruby.class, IRubyObject[].class));
            }
        }
    }

    public void createEmptyHash() {
        loadRuntime();

        method.invokestatic(p(RubyHash.class), "newHash", sig(RubyHash.class, params(Ruby.class)));
    }

    public void createNewHash(Object elements, ArrayCallback callback, int keyCount) {
        createNewHashCommon(elements, callback, keyCount, "constructHash", "fastASetCheckString");
    }

    public void createNewLiteralHash(Object elements, ArrayCallback callback, int keyCount) {
        createNewLiteralHashCommon(elements, callback, keyCount, "constructHash", "fastASetCheckString");
    }
    
    public void createNewHash19(Object elements, ArrayCallback callback, int keyCount) {
        createNewHashCommon(elements, callback, keyCount, "constructHash19", "fastASetCheckString19");
    }
    
    private void createNewHashCommon(Object elements, ArrayCallback callback, int keyCount,
            String constructorName, String methodName) {
        loadRuntime();

        // use specific-arity for as much as possible
        int i = 0;
        for (; i < keyCount && i < RuntimeHelpers.MAX_SPECIFIC_ARITY_HASH; i++) {
            callback.nextValue(this, elements, i);
        }

        invokeUtilityMethod(constructorName, sig(RubyHash.class, params(Ruby.class, IRubyObject.class, i * 2)));

        for (; i < keyCount; i++) {
            method.dup();
            loadRuntime();
            callback.nextValue(this, elements, i);
            method.invokevirtual(p(RubyHash.class), methodName, sig(void.class, params(Ruby.class, IRubyObject.class, IRubyObject.class)));
        }
    }

    private void createNewLiteralHashCommon(Object elements, ArrayCallback callback, int keyCount,
            String constructorName, String methodName) {
        if (keyCount < 50) {
            // small hash, use standard construction
            createNewHashCommon(elements, callback, keyCount, constructorName, methodName);
        } else {
            // populate the hash in a separate series of methods
            SkinnyMethodAdapter oldMethod = method;

            // prepare the first builder in the chain
            String builderMethod = "hash_builder_" + script.getAndIncrementMethodIndex() + "";
            method = new SkinnyMethodAdapter(
                    script.getClassVisitor(),
                    ACC_PRIVATE | ACC_SYNTHETIC | ACC_STATIC,
                    builderMethod,
                    sig(RubyHash.class, "L" + script.getClassname() + ";", ThreadContext.class, RubyHash.class),
                    null,
                    null);
            method.start();

            for (int i = 0; i < keyCount; i++) {
                // for every hundred keys, chain to the next call
                if ((i + 1) % 100 == 0) {
                    String nextName = "hash_builder_" + script.getAndIncrementMethodIndex() + "";

                    method.aloadMany(0, 1, 2);
                    method.invokestatic(script.getClassname(), nextName,
                            sig(RubyHash.class, "L" + script.getClassname() + ";", ThreadContext.class, RubyHash.class));
                    method.areturn();
                    method.end();

                    method = new SkinnyMethodAdapter(
                            script.getClassVisitor(),
                            ACC_PRIVATE | ACC_SYNTHETIC | ACC_STATIC,
                            nextName,
                            sig(RubyHash.class, "L" + script.getClassname() + ";", ThreadContext.class, RubyHash.class),
                            null,
                            null);
                    method.start();
                }

                method.aload(2);
                loadRuntime();
                callback.nextValue(this, elements, i);
                method.invokevirtual(p(RubyHash.class), methodName, sig(void.class, params(Ruby.class, IRubyObject.class, IRubyObject.class)));
            }

            // close out the last method in the chain
            method.aload(2);
            method.areturn();
            method.end();

            // restore original method
            method = oldMethod;

            // chain invoke
            method.aload(StandardASMCompiler.THIS);
            method.aload(StandardASMCompiler.THREADCONTEXT_INDEX);
            loadRuntime();
            method.invokestatic(p(RubyHash.class), "newHash", sig(RubyHash.class, Ruby.class));
            method.invokestatic(script.getClassname(), builderMethod,
                    sig(RubyHash.class, "L" + script.getClassname() + ";", ThreadContext.class, RubyHash.class));
        }
    }

    public void createNewRange(CompilerCallback beginEndCallback, boolean isExclusive) {
        loadRuntime();
        loadThreadContext();
        beginEndCallback.call(this);

        if (isExclusive) {
            method.invokestatic(p(RubyRange.class), "newExclusiveRange", sig(RubyRange.class, params(Ruby.class, ThreadContext.class, IRubyObject.class, IRubyObject.class)));
        } else {
            method.invokestatic(p(RubyRange.class), "newInclusiveRange", sig(RubyRange.class, params(Ruby.class, ThreadContext.class, IRubyObject.class, IRubyObject.class)));
        }
    }

    public void createNewLambda(CompilerCallback closure) {
        loadThreadContext();
        closure.call(this);
        loadSelf();

        invokeUtilityMethod("newLiteralLambda", sig(RubyProc.class, ThreadContext.class, Block.class, IRubyObject.class));
    }

    /**
     * Invoke IRubyObject.isTrue
     */
    public void isTrue() {
        invokeIRubyObject("isTrue", sig(Boolean.TYPE));
    }

    public void performBooleanBranch(BranchCallback trueBranch, BranchCallback falseBranch) {
        // call isTrue on the result
        isTrue();
        
        performBooleanBranch2(trueBranch, falseBranch);
    }

    public void performBooleanBranch2(BranchCallback trueBranch, BranchCallback falseBranch) {
        Label afterJmp = new Label();
        Label falseJmp = new Label();

        method.ifeq(falseJmp); // EQ == 0 (i.e. false)
        trueBranch.branch(this);
        method.go_to(afterJmp);

        // FIXME: optimize for cases where we have no false branch
        method.label(falseJmp);
        falseBranch.branch(this);

        method.label(afterJmp);
    }

    public void performLogicalAnd(BranchCallback longBranch) {
        Label falseJmp = new Label();

        // dup it since we need to return appropriately if it's false
        method.dup();

        // call isTrue on the result
        isTrue();

        method.ifeq(falseJmp); // EQ == 0 (i.e. false)
        // pop the extra result and replace with the send part of the AND
        method.pop();
        longBranch.branch(this);
        method.label(falseJmp);
    }

    public void performLogicalOr(BranchCallback longBranch) {
        // FIXME: after jump is not in here.  Will if ever be?
        //Label afterJmp = new Label();
        Label falseJmp = new Label();

        // dup it since we need to return appropriately if it's false
        method.dup();

        // call isTrue on the result
        isTrue();

        method.ifne(falseJmp); // EQ == 0 (i.e. false)
        // pop the extra result and replace with the send part of the AND
        method.pop();
        longBranch.branch(this);
        method.label(falseJmp);
    }

    public void performBooleanLoopSafe(BranchCallback condition, BranchCallback body, boolean checkFirst) {
        String mname = getNewRescueName();
        BaseBodyCompiler nested = outline(mname);
        nested.performBooleanLoopSafeInner(condition, body, checkFirst);
    }

    private void performBooleanLoopSafeInner(BranchCallback condition, BranchCallback body, boolean checkFirst) {
        performBooleanLoop(condition, body, checkFirst);

        endBody();
    }

    public void performBooleanLoop(BranchCallback condition, final BranchCallback body, boolean checkFirst) {
        Label tryBegin = new Label();
        Label tryEnd = new Label();
        Label catchNext = new Label();
        Label catchBreak = new Label();
        Label endOfBody = new Label();
        Label conditionCheck = new Label();
        final Label topOfBody = new Label();
        Label done = new Label();
        Label normalLoopEnd = new Label();
        method.trycatch(tryBegin, tryEnd, catchNext, p(JumpException.NextJump.class));
        method.trycatch(tryBegin, tryEnd, catchBreak, p(JumpException.BreakJump.class));

        method.label(tryBegin);
        {

            Label[] oldLoopLabels = currentLoopLabels;

            currentLoopLabels = new Label[]{endOfBody, topOfBody, done};

            // FIXME: if we terminate immediately, this appears to break while in method arguments
            // we need to push a nil for the cases where we will never enter the body
            if (checkFirst) {
                method.go_to(conditionCheck);
            }

            method.label(topOfBody);

            Runnable redoBody = new Runnable() { public void run() {
                Runnable raiseBody = new Runnable() { public void run() {
                    body.branch(BaseBodyCompiler.this);
                }};
                Runnable raiseCatch = new Runnable() { public void run() {
                    loadThreadContext();
                    invokeUtilityMethod("unwrapRedoNextBreakOrJustLocalJump", sig(Throwable.class, RaiseException.class, ThreadContext.class));
                    method.athrow();
                }};
                method.trycatch(p(RaiseException.class), raiseBody, raiseCatch);
            }};
            Runnable redoCatch = new Runnable() { public void run() {
                method.pop();
                method.go_to(topOfBody);
            }};
            method.trycatch(p(JumpException.RedoJump.class), redoBody, redoCatch);

            method.label(endOfBody);

            // clear body or next result after each successful loop
            method.pop();

            method.label(conditionCheck);

            // check the condition
            condition.branch(this);
            isTrue();
            method.ifne(topOfBody); // NE == nonzero (i.e. true)

            currentLoopLabels = oldLoopLabels;
        }

        method.label(tryEnd);
        // skip catch block
        method.go_to(normalLoopEnd);

        // catch logic for flow-control: next, break
        {
            // next jump
            {
                method.label(catchNext);
                method.pop();
                // exceptionNext target is for a next that doesn't push a new value, like this one
                method.go_to(conditionCheck);
            }

            // break jump
            {
                method.label(catchBreak);
                loadThreadContext();
                invokeUtilityMethod("breakJumpInWhile", sig(IRubyObject.class, JumpException.BreakJump.class, ThreadContext.class));
                method.go_to(done);
            }
        }

        method.label(normalLoopEnd);
        loadNil();
        method.label(done);
    }

    public void performBooleanLoopLight(BranchCallback condition, BranchCallback body, boolean checkFirst) {
        Label endOfBody = new Label();
        Label conditionCheck = new Label();
        Label topOfBody = new Label();
        Label done = new Label();

        Label[] oldLoopLabels = currentLoopLabels;

        currentLoopLabels = new Label[]{endOfBody, topOfBody, done};

        // FIXME: if we terminate immediately, this appears to break while in method arguments
        // we need to push a nil for the cases where we will never enter the body
        if (checkFirst) {
            method.go_to(conditionCheck);
        }

        method.label(topOfBody);

        body.branch(this);

        method.label(endOfBody);

        // clear body or next result after each successful loop
        method.pop();

        method.label(conditionCheck);

        // check the condition
        condition.branch(this);
        isTrue();
        method.ifne(topOfBody); // NE == nonzero (i.e. true)

        currentLoopLabels = oldLoopLabels;

        loadNil();
        method.label(done);
    }

    public void createNewClosure(
            String file,
            int line,
            StaticScope scope,
            int arity,
            CompilerCallback body,
            CompilerCallback args,
            boolean hasMultipleArgsHead,
            NodeType argsNodeId,
            ASTInspector inspector) {
        String blockInMethod = JavaNameMangler.mangleMethodName(rubyName);
        if (rubyName == null || rubyName.length() == 0) {
            blockInMethod = "__block__";
        }
        String closureMethodName = "block_" + script.getAndIncrementInnerIndex() + "$RUBY$" + blockInMethod;

        ChildScopedBodyCompiler closureCompiler = new ChildScopedBodyCompiler(script, closureMethodName, rubyName, inspector, scope);

        closureCompiler.beginMethod(args, scope);

        body.call(closureCompiler);

        closureCompiler.endBody();

        // Done with closure compilation

        loadThreadContext();
        loadSelf();
        script.getCacheCompiler().cacheClosure(this, closureMethodName, arity, scope, file, line, hasMultipleArgsHead, argsNodeId, inspector);

        script.addBlockCallbackDescriptor(closureMethodName, file, line);

        invokeUtilityMethod("createBlock", sig(Block.class,
                params(ThreadContext.class, IRubyObject.class, BlockBody.class)));
    }

    public void createNewClosure19(
            String file,
            int line,
            StaticScope scope,
            int arity,
            CompilerCallback body,
            CompilerCallback args,
            boolean hasMultipleArgsHead,
            NodeType argsNodeId,
            String parameterList,
            ASTInspector inspector) {
        String blockInMethod = JavaNameMangler.mangleMethodName(rubyName);
        if (rubyName == null || rubyName.length() == 0) {
            blockInMethod = "__block__";
        }
        String closureMethodName = "block_" + script.getAndIncrementInnerIndex() + "$RUBY$" + blockInMethod;

        ChildScopedBodyCompiler19 closureCompiler = new ChildScopedBodyCompiler19(script, closureMethodName, rubyName, inspector, scope);

        closureCompiler.beginMethod(args, scope);

        body.call(closureCompiler);

        closureCompiler.endBody();

        // Done with closure compilation

        loadThreadContext();
        loadSelf();
        script.getCacheCompiler().cacheClosure19(this, closureMethodName, arity, scope, file, line, hasMultipleArgsHead, argsNodeId, parameterList, inspector);

        script.addBlockCallback19Descriptor(closureMethodName, file, line);

        invokeUtilityMethod("createBlock19", sig(Block.class,
                params(ThreadContext.class, IRubyObject.class, BlockBody.class)));
    }

    public void runBeginBlock(StaticScope scope, CompilerCallback body) {
        String closureMethodName = "block_" + script.getAndIncrementInnerIndex() + "$RUBY$__begin__";

        ChildScopedBodyCompiler closureCompiler = new ChildScopedBodyCompiler(script, closureMethodName, rubyName, null, scope);

        closureCompiler.beginMethod(null, scope);

        body.call(closureCompiler);

        closureCompiler.endBody();

        // Done with closure compilation
        loadThreadContext();
        loadSelf();

        String scopeNames = RuntimeHelpers.encodeScope(scope);
        method.ldc(scopeNames);

        script.getCacheCompiler().cacheSpecialClosure(this, closureMethodName);

        invokeUtilityMethod("runBeginBlock", sig(IRubyObject.class,
                params(ThreadContext.class, IRubyObject.class, String.class, CompiledBlockCallback.class)));
    }

    public void createNewForLoop(int arity, CompilerCallback body, CompilerCallback args, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector) {
        String closureMethodName = "block_" + script.getAndIncrementInnerIndex() + "$RUBY$__for__";

        ChildScopedBodyCompiler closureCompiler = new ChildScopedBodyCompiler(script, closureMethodName, rubyName, inspector, scope);

        closureCompiler.beginMethod(args, null);

        body.call(closureCompiler);

        closureCompiler.endBody();

        // Done with closure compilation
        loadThreadContext();
        loadSelf();
        method.pushInt(arity);

        script.getCacheCompiler().cacheSpecialClosure(this, closureMethodName);

        method.ldc(Boolean.valueOf(hasMultipleArgsHead));
        method.ldc(BlockBody.asArgumentType(argsNodeId));

        invokeUtilityMethod("createSharedScopeBlock", sig(Block.class,
                params(ThreadContext.class, IRubyObject.class, Integer.TYPE, CompiledBlockCallback.class, Boolean.TYPE, Integer.TYPE)));
    }

    public void createNewEndBlock(CompilerCallback body) {
        String closureMethodName = "block_" + script.getAndIncrementInnerIndex() + "$RUBY$__end__";

        ChildScopedBodyCompiler closureCompiler = new ChildScopedBodyCompiler(script, closureMethodName, rubyName, null, scope);

        closureCompiler.beginMethod(null, null);

        body.call(closureCompiler);

        closureCompiler.endBody();

        // Done with closure compilation
        loadThreadContext();
        loadSelf();
        method.iconst_0();

        script.getCacheCompiler().cacheSpecialClosure(this, closureMethodName);

        method.iconst_0(); // false
        method.iconst_0(); // zero

        invokeUtilityMethod("createSharedScopeBlock", sig(Block.class,
                params(ThreadContext.class, IRubyObject.class, Integer.TYPE, CompiledBlockCallback.class, Boolean.TYPE, Integer.TYPE)));

        loadRuntime();
        invokeUtilityMethod("registerEndBlock", sig(void.class, Block.class, Ruby.class));
        loadNil();
    }

    public void getCompiledClass() {
        method.ldc(Type.getType(script.getClassname()));
    }

    public void println() {
        method.dup();
        method.getstatic(p(System.class), "out", ci(PrintStream.class));
        method.swap();

        method.invokevirtual(p(PrintStream.class), "println", sig(Void.TYPE, params(Object.class)));
    }

    public void defineAlias(CompilerCallback args) {
        loadThreadContext();
        loadSelf();
        args.call(this);
        invokeUtilityMethod("defineAlias", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, Object.class, Object.class));
    }

    public void literal(String value) {
        method.ldc(value);
    }

    public void loadFalse() {
        // TODO: cache?
        loadRuntime();
        invokeRuby("getFalse", sig(RubyBoolean.class));
    }

    public void loadTrue() {
        // TODO: cache?
        loadRuntime();
        invokeRuby("getTrue", sig(RubyBoolean.class));
    }

    public void loadCurrentModule() {
        loadThreadContext();
        invokeThreadContext("getCurrentScope", sig(DynamicScope.class));
        method.invokevirtual(p(DynamicScope.class), "getStaticScope", sig(StaticScope.class));
        method.invokevirtual(p(StaticScope.class), "getModule", sig(RubyModule.class));
    }

    public void retrieveInstanceVariable(String name) {
        script.getCacheCompiler().cachedGetVariable(this, name);
    }

    public void assignInstanceVariable(String name) {
        final int tmp = getVariableCompiler().grabTempLocal();
        getVariableCompiler().setTempLocal(tmp);
        CompilerCallback callback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                context.getVariableCompiler().getTempLocal(tmp);
            }
        };
        script.getCacheCompiler().cachedSetVariable(this, name, callback);
    }

    public void assignInstanceVariable(String name, CompilerCallback value) {
        script.getCacheCompiler().cachedSetVariable(this, name, value);
    }

    public void retrieveGlobalVariable(String name) {
        loadRuntime();
        method.ldc(name);
        invokeUtilityMethod("getGlobalVariable", sig(IRubyObject.class, Ruby.class, String.class));
    }

    public void assignGlobalVariable(String name) {
        loadRuntime();
        method.ldc(name);
        invokeUtilityMethod("setGlobalVariable", sig(IRubyObject.class, IRubyObject.class, Ruby.class, String.class));
    }

    public void assignGlobalVariable(String name, CompilerCallback value) {
        value.call(this);
        loadRuntime();
        method.ldc(name);
        invokeUtilityMethod("setGlobalVariable", sig(IRubyObject.class, IRubyObject.class, Ruby.class, String.class));
    }

    public void negateCurrentValue() {
        loadRuntime();
        invokeUtilityMethod("negate", sig(IRubyObject.class, IRubyObject.class, Ruby.class));
    }

    public void splatCurrentValue(String methodName) {
        method.invokestatic(p(RuntimeHelpers.class), methodName, sig(RubyArray.class, params(IRubyObject.class)));
    }

    public void singlifySplattedValue() {
        method.invokestatic(p(RuntimeHelpers.class), "aValueSplat", sig(IRubyObject.class, params(IRubyObject.class)));
    }

    public void singlifySplattedValue19() {
        method.invokestatic(p(RuntimeHelpers.class), "aValueSplat19", sig(IRubyObject.class, params(IRubyObject.class)));
    }

    public void aryToAry() {
        method.invokestatic(p(RuntimeHelpers.class), "aryToAry", sig(IRubyObject.class, params(IRubyObject.class)));
    }

    public void ensureRubyArray() {
        invokeUtilityMethod("ensureRubyArray", sig(RubyArray.class, params(IRubyObject.class)));
    }

    public void ensureMultipleAssignableRubyArray(boolean masgnHasHead) {
        loadRuntime();
        method.pushBoolean(masgnHasHead);
        invokeUtilityMethod("ensureMultipleAssignableRubyArray", sig(RubyArray.class, params(IRubyObject.class, Ruby.class, boolean.class)));
    }

    public void forEachInValueArray(int start, int count, Object source, ArrayCallback callback, CompilerCallback argsCallback) {
        if (start < count || argsCallback != null) {
            int tempLocal = getVariableCompiler().grabTempLocal();
            getVariableCompiler().setTempLocal(tempLocal);
            
            for (; start < count; start++) {
                getVariableCompiler().getTempLocal(tempLocal);
                switch (start) {
                case 0:
                    invokeUtilityMethod("arrayEntryOrNilZero", sig(IRubyObject.class, RubyArray.class));
                    break;
                case 1:
                    invokeUtilityMethod("arrayEntryOrNilOne", sig(IRubyObject.class, RubyArray.class));
                    break;
                case 2:
                    invokeUtilityMethod("arrayEntryOrNilTwo", sig(IRubyObject.class, RubyArray.class));
                    break;
                default:
                    method.pushInt(start);
                    invokeUtilityMethod("arrayEntryOrNil", sig(IRubyObject.class, RubyArray.class, int.class));
                    break;
                }
                callback.nextValue(this, source, start);
            }

            if (argsCallback != null) {
                getVariableCompiler().getTempLocal(tempLocal);
                loadRuntime();
                method.pushInt(start);
                invokeUtilityMethod("subarrayOrEmpty", sig(RubyArray.class, RubyArray.class, Ruby.class, int.class));
                argsCallback.call(this);
            }

            getVariableCompiler().getTempLocal(tempLocal);
            getVariableCompiler().releaseTempLocal();
        }
    }

    public void forEachInValueArray(int start, int preCount, Object preSource, int postCount, Object postSource, ArrayCallback callback, CompilerCallback argsCallback) {
        if (start < preCount || argsCallback != null) {
            int tempLocal = getVariableCompiler().grabTempLocal();
            getVariableCompiler().setTempLocal(tempLocal);

            for (; start < preCount; start++) {
                getVariableCompiler().getTempLocal(tempLocal);
                switch (start) {
                case 0:
                    invokeUtilityMethod("arrayEntryOrNilZero", sig(IRubyObject.class, RubyArray.class));
                    break;
                case 1:
                    invokeUtilityMethod("arrayEntryOrNilOne", sig(IRubyObject.class, RubyArray.class));
                    break;
                case 2:
                    invokeUtilityMethod("arrayEntryOrNilTwo", sig(IRubyObject.class, RubyArray.class));
                    break;
                default:
                    method.pushInt(start);
                    invokeUtilityMethod("arrayEntryOrNil", sig(IRubyObject.class, RubyArray.class, int.class));
                    break;
                }
                callback.nextValue(this, preSource, start);
            }

            if (argsCallback != null) {
                getVariableCompiler().getTempLocal(tempLocal);
                loadRuntime();
                method.pushInt(start);
                method.pushInt(postCount);
                invokeUtilityMethod("subarrayOrEmpty", sig(RubyArray.class, RubyArray.class, Ruby.class, int.class, int.class));
                argsCallback.call(this);
            }

            for (int postStart = 0; postStart < postCount; postStart++) {
                getVariableCompiler().getTempLocal(tempLocal);
                method.pushInt(preCount);
                method.pushInt(postCount);
                switch (postStart) {
                case 0:
                    invokeUtilityMethod("arrayPostOrNilZero", sig(IRubyObject.class, RubyArray.class, int.class, int.class));
                    break;
                case 1:
                    invokeUtilityMethod("arrayPostOrNilOne", sig(IRubyObject.class, RubyArray.class, int.class, int.class));
                    break;
                case 2:
                    invokeUtilityMethod("arrayPostOrNilTwo", sig(IRubyObject.class, RubyArray.class, int.class, int.class));
                    break;
                default:
                    method.pushInt(postStart);
                    invokeUtilityMethod("arrayPostOrNil", sig(IRubyObject.class, RubyArray.class, int.class, int.class, int.class));
                    break;
                }
                callback.nextValue(this, postSource, postStart);
            }

            getVariableCompiler().getTempLocal(tempLocal);
            getVariableCompiler().releaseTempLocal();
        }
    }

    public void asString() {
        method.invokeinterface(p(IRubyObject.class), "asString", sig(RubyString.class));
    }

    public void toJavaString() {
        method.invokevirtual(p(Object.class), "toString", sig(String.class));
    }

    public void nthRef(int match) {
        method.pushInt(match);
        backref();
        method.invokestatic(p(RubyRegexp.class), "nth_match", sig(IRubyObject.class, params(Integer.TYPE, IRubyObject.class)));
    }

    public void match(boolean is19) {
        loadThreadContext();
        method.invokevirtual(p(RubyRegexp.class), is19 ? "op_match2_19" : "op_match2", sig(IRubyObject.class, params(ThreadContext.class)));
    }

    public void match2(CompilerCallback value, boolean is19) {
        loadThreadContext();
        value.call(this);
        method.invokevirtual(p(RubyRegexp.class), is19 ? "op_match19" : "op_match", sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class)));
    }

    public void match2Capture(CompilerCallback value, int[] scopeOffsets, boolean is19) {
        loadThreadContext();
        value.call(this);
        method.ldc(RuntimeHelpers.encodeCaptureOffsets(scopeOffsets));
        invokeUtilityMethod(is19 ? "match2AndUpdateScope19" : "match2AndUpdateScope", sig(IRubyObject.class, params(IRubyObject.class, ThreadContext.class, IRubyObject.class, String.class)));
    }

    public void match3(boolean is19) {
        loadThreadContext();
        invokeUtilityMethod(is19 ? "match3_19" : "match3", sig(IRubyObject.class, RubyRegexp.class, IRubyObject.class, ThreadContext.class));
    }

    public void createNewRegexp(final ByteList value, final int options) {
        script.getCacheCompiler().cacheRegexp(this, value, options);
    }

    public void createNewRegexp(CompilerCallback createStringCallback, final int options) {
        boolean onceOnly = (options & ReOptions.RE_OPTION_ONCE) != 0;   // for regular expressions with the /o flag

        if (onceOnly) {
            script.getCacheCompiler().cacheDRegexp(this, createStringCallback, options);
        } else {
            loadRuntime();
            createStringCallback.call(this);
            method.pushInt(options);
            method.invokestatic(p(RubyRegexp.class), "newDRegexpEmbedded", sig(RubyRegexp.class, params(Ruby.class, RubyString.class, int.class))); //[reg]
        }
    }
    
    public void createDRegexp19(ArrayCallback arrayCallback, Object[] sourceArray, int options) {
        boolean onceOnly = (options & ReOptions.RE_OPTION_ONCE) != 0;   // for regular expressions with the /o flag

        if (onceOnly) {
            script.getCacheCompiler().cacheDRegexp19(this, arrayCallback, sourceArray, options);
        } else {
            loadRuntime();
            createObjectArray(sourceArray, arrayCallback);
            method.ldc(options);
            method.invokestatic(p(RubyRegexp.class), "newDRegexpEmbedded19", sig(RubyRegexp.class, params(Ruby.class, IRubyObject[].class, int.class))); //[reg]
        }
    }

    public void pollThreadEvents() {
        if (!RubyInstanceConfig.THREADLESS_COMPILE_ENABLED) {
            loadThreadContext();
            invokeThreadContext("pollThreadEvents", sig(Void.TYPE));
        }
    }

    public void nullToNil() {
        Label notNull = new Label();
        method.dup();
        method.ifnonnull(notNull);
        method.pop();
        loadNil();
        method.label(notNull);
    }

    public void isInstanceOf(Class clazz, BranchCallback trueBranch, BranchCallback falseBranch) {
        method.instance_of(p(clazz));

        Label falseJmp = new Label();
        Label afterJmp = new Label();

        method.ifeq(falseJmp); // EQ == 0 (i.e. false)
        trueBranch.branch(this);

        method.go_to(afterJmp);
        method.label(falseJmp);

        falseBranch.branch(this);

        method.label(afterJmp);
    }

    public void isCaptured(final int number, final BranchCallback trueBranch, final BranchCallback falseBranch) {
        backref();
        method.dup();
        isInstanceOf(RubyMatchData.class, new BranchCallback() {

            public void branch(BodyCompiler context) {
                method.visitTypeInsn(CHECKCAST, p(RubyMatchData.class));
                method.pushInt(number);
                method.invokevirtual(p(RubyMatchData.class), "group", sig(IRubyObject.class, params(int.class)));
                method.invokeinterface(p(IRubyObject.class), "isNil", sig(boolean.class));
                Label isNil = new Label();
                Label after = new Label();

                method.ifne(isNil);
                trueBranch.branch(context);
                method.go_to(after);

                method.label(isNil);
                falseBranch.branch(context);
                method.label(after);
            }
        }, new BranchCallback() {

            public void branch(BodyCompiler context) {
                method.pop();
                falseBranch.branch(context);
            }
        });
    }

    public void backref() {
        loadRuntime();
        loadThreadContext();
        invokeUtilityMethod("getBackref", sig(IRubyObject.class, Ruby.class, ThreadContext.class));
    }

    public void backrefMethod(String methodName) {
        backref();
        method.invokestatic(p(RubyRegexp.class), methodName, sig(IRubyObject.class, params(IRubyObject.class)));
    }

    public void issueLoopBreak() {
        // inside a loop, break out of it
        // go to end of loop, leaving break value on stack
        method.go_to(currentLoopLabels[2]);
    }

    public void issueLoopNext() {
        // inside a loop, jump to conditional
        method.go_to(currentLoopLabels[0]);
    }

    public void issueLoopRedo() {
        // inside a loop, jump to body
        method.go_to(currentLoopLabels[1]);
    }

    protected String getNewEnsureName() {
        return "ensure_" + (script.getAndIncrementEnsureNumber()) + "$RUBY$__ensure__";
    }

    public void protect(BranchCallback regularCode, BranchCallback protectedCode, Class ret) {
        String mname = getNewEnsureName();
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(
                script.getClassVisitor(),
                ACC_PUBLIC | ACC_SYNTHETIC | ACC_STATIC,
                mname,
                sig(ret, "L" + script.getClassname() + ";", ThreadContext.class, IRubyObject.class, Block.class),
                null,
                null);
        SkinnyMethodAdapter old_method = null;
        SkinnyMethodAdapter var_old_method = null;
        SkinnyMethodAdapter inv_old_method = null;
        boolean oldInNestedMethod = inNestedMethod;
        inNestedMethod = true;
        Label[] oldLoopLabels = currentLoopLabels;
        currentLoopLabels = null;
        int oldArgCount = argParamCount;
        argParamCount = 0; // synthetic methods always have zero arg parameters
        try {
            old_method = this.method;
            var_old_method = getVariableCompiler().getMethodAdapter();
            inv_old_method = getInvocationCompiler().getMethodAdapter();
            this.method = mv;
            getVariableCompiler().setMethodAdapter(mv);
            getInvocationCompiler().setMethodAdapter(mv);

            mv.visitCode();

            mv.aload(StandardASMCompiler.THREADCONTEXT_INDEX);
            mv.invokevirtual(p(ThreadContext.class), "getCurrentScope", sig(DynamicScope.class));
            mv.dup();
            mv.astore(getDynamicScopeIndex());
            mv.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
            mv.astore(getVarsArrayIndex());
            Label codeBegin = new Label();
            Label codeEnd = new Label();
            Label ensureBegin = new Label();
            Label ensureEnd = new Label();
            method.label(codeBegin);

            regularCode.branch(this);

            method.label(codeEnd);

            protectedCode.branch(this);
            mv.areturn();

            method.label(ensureBegin);
            method.astore(getExceptionIndex());
            method.label(ensureEnd);
            protectedCode.branch(this);

            method.aload(getExceptionIndex());
            method.athrow();

            method.trycatch(codeBegin, codeEnd, ensureBegin, null);
            method.trycatch(ensureBegin, ensureEnd, ensureBegin, null);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        } finally {
            this.method = old_method;
            getVariableCompiler().setMethodAdapter(var_old_method);
            getInvocationCompiler().setMethodAdapter(inv_old_method);
            inNestedMethod = oldInNestedMethod;
            currentLoopLabels = oldLoopLabels;
            argParamCount = oldArgCount;
        }

        method.aload(StandardASMCompiler.THIS);
        loadThreadContext();
        loadSelf();
        if (this instanceof ChildScopedBodyCompiler) {
            pushNull();
        } else {
            loadBlock();
        }
        method.invokestatic(
                script.getClassname(),
                mname,
                sig(ret, "L" + script.getClassname() + ";", ThreadContext.class, IRubyObject.class, Block.class));
    }

    public void performEnsure(BranchCallback regularCode, BranchCallback protectedCode) {
        String mname = getNewEnsureName();
        BaseBodyCompiler ensure = outline(mname);
        ensure.performEnsureInner(regularCode, protectedCode);
    }

    private void performEnsureInner(BranchCallback regularCode, BranchCallback protectedCode) {
        Label codeBegin = new Label();
        Label codeEnd = new Label();
        Label ensureBegin = new Label();
        Label ensureEnd = new Label();
        method.label(codeBegin);

        regularCode.branch(this);

        method.label(codeEnd);

        protectedCode.branch(this);
        method.areturn();

        method.label(ensureBegin);
        method.astore(getExceptionIndex());
        method.label(ensureEnd);

        protectedCode.branch(this);

        method.aload(getExceptionIndex());
        method.athrow();

        method.trycatch(codeBegin, codeEnd, ensureBegin, null);
        method.trycatch(ensureBegin, ensureEnd, ensureBegin, null);

        loadNil();
        endBody();
    }

    protected String getNewRescueName() {
        return "rescue_" + (script.getAndIncrementRescueNumber()) + "$RUBY$SYNTHETIC" + JavaNameMangler.mangleMethodName(getRubyName());
    }

    public void storeExceptionInErrorInfo() {
        loadException();
        loadThreadContext();
        invokeUtilityMethod("storeExceptionInErrorInfo", sig(void.class, Throwable.class, ThreadContext.class));
    }

    public void storeNativeExceptionInErrorInfo() {
        loadException();
        loadThreadContext();
        invokeUtilityMethod("storeNativeExceptionInErrorInfo", sig(void.class, Throwable.class, ThreadContext.class));
    }

    public void clearErrorInfo() {
        loadThreadContext();
        invokeUtilityMethod("clearErrorInfo", sig(void.class, ThreadContext.class));
    }

    public void rescue(BranchCallback regularCode, Class exception, BranchCallback catchCode, Class ret) {
        String mname = getNewRescueName();
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(
                script.getClassVisitor(),
                    ACC_PUBLIC | ACC_SYNTHETIC | ACC_STATIC,
                    mname,
                    sig(ret, "L" + script.getClassname() + ";", ThreadContext.class, IRubyObject.class, Block.class),
                    null,
                    null);
        SkinnyMethodAdapter old_method = null;
        SkinnyMethodAdapter var_old_method = null;
        SkinnyMethodAdapter inv_old_method = null;
        Label afterMethodBody = new Label();
        Label catchRetry = new Label();
        Label catchRaised = new Label();
        Label catchJumps = new Label();
        Label exitRescue = new Label();
        boolean oldWithinProtection = inNestedMethod;
        inNestedMethod = true;
        Label[] oldLoopLabels = currentLoopLabels;
        currentLoopLabels = null;
        int oldArgCount = argParamCount;
        argParamCount = 0; // synthetic methods always have zero arg parameters
        try {
            old_method = this.method;
            var_old_method = getVariableCompiler().getMethodAdapter();
            inv_old_method = getInvocationCompiler().getMethodAdapter();
            this.method = mv;
            getVariableCompiler().setMethodAdapter(mv);
            getInvocationCompiler().setMethodAdapter(mv);

            mv.start();

            // store previous exception for restoration if we rescue something
            loadThreadContext();
            invokeThreadContext("getErrorInfo", sig(IRubyObject.class));
            mv.astore(getPreviousExceptionIndex());

            mv.aload(StandardASMCompiler.THREADCONTEXT_INDEX);
            mv.invokevirtual(p(ThreadContext.class), "getCurrentScope", sig(DynamicScope.class));
            mv.astore(getDynamicScopeIndex());

            // if more than 4 vars, get values array too
            if (scope.getNumberOfVariables() > 4) {
                mv.aload(getDynamicScopeIndex());
                mv.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
                mv.astore(getVarsArrayIndex());
            }

            Label beforeBody = new Label();
            Label afterBody = new Label();
            Label catchBlock = new Label();
            mv.trycatch(beforeBody, afterBody, catchBlock, p(exception));
            mv.label(beforeBody);

            regularCode.branch(this);

            mv.label(afterBody);
            mv.go_to(exitRescue);
            mv.label(catchBlock);
            mv.astore(getExceptionIndex());

            catchCode.branch(this);

            mv.label(afterMethodBody);
            mv.go_to(exitRescue);

            // retry handling in the rescue block
            mv.trycatch(catchBlock, afterMethodBody, catchRetry, p(JumpException.RetryJump.class));
            mv.label(catchRetry);
            mv.pop();
            mv.go_to(beforeBody);

            // any exceptions raised must continue to be raised, skipping $! restoration
            mv.trycatch(beforeBody, afterMethodBody, catchRaised, p(RaiseException.class));
            mv.label(catchRaised);
            mv.athrow();

            // and remaining jump exceptions should restore $!
            mv.trycatch(beforeBody, afterMethodBody, catchJumps, p(JumpException.class));
            mv.label(catchJumps);
            loadThreadContext();
            method.aload(getPreviousExceptionIndex());
            invokeThreadContext("setErrorInfo", sig(IRubyObject.class, IRubyObject.class));
            method.pop();
            mv.athrow();

            mv.label(exitRescue);

            // restore the original exception
            loadThreadContext();
            method.aload(getPreviousExceptionIndex());
            invokeThreadContext("setErrorInfo", sig(IRubyObject.class, IRubyObject.class));
            method.pop();

            mv.areturn();
            mv.end();
        } finally {
            inNestedMethod = oldWithinProtection;
            this.method = old_method;
            getVariableCompiler().setMethodAdapter(var_old_method);
            getInvocationCompiler().setMethodAdapter(inv_old_method);
            currentLoopLabels = oldLoopLabels;
            argParamCount = oldArgCount;
        }

        method.aload(StandardASMCompiler.THIS);
        loadThreadContext();
        loadSelf();
        if (this instanceof ChildScopedBodyCompiler) {
            pushNull();
        } else {
            loadBlock();
        }
        method.invokestatic(
                script.getClassname(),
                mname,
                sig(ret, "L" + script.getClassname() + ";", ThreadContext.class, IRubyObject.class, Block.class));
    }

    public void performRescue(BranchCallback regularCode, BranchCallback rubyCatchCode, BranchCallback rubyElseCode, boolean needsRetry) {
        String mname = getNewRescueName();
        BaseBodyCompiler rescueMethod = outline(mname);
        rescueMethod.performRescueLight(regularCode, rubyCatchCode, rubyElseCode, needsRetry);
        rescueMethod.endBody();
    }

    public void performRescueLight(BranchCallback regularCode, BranchCallback rubyCatchCode, BranchCallback rubyElseCode, boolean needsRetry) {
        Label afterRubyCatchBody = new Label();
        Label catchRetry = new Label();
        Label catchJumps = new Label();
        Label exitRescue = new Label();

        // store previous exception for restoration if we rescue something
        loadThreadContext();
        invokeThreadContext("getErrorInfo", sig(IRubyObject.class));
        method.astore(getPreviousExceptionIndex());

        Label beforeBody = new Label();
        Label afterBody = new Label();
        Label rubyCatchBlock = new Label();
        Label flowCatchBlock = new Label();
        Label elseLabel = new Label();

        method.visitTryCatchBlock(beforeBody, afterBody, flowCatchBlock, p(JumpException.FlowControlException.class));
        method.visitTryCatchBlock(beforeBody, afterBody, rubyCatchBlock, p(Throwable.class));

        method.visitLabel(beforeBody);
        {
            regularCode.branch(this);
        }
        method.label(afterBody);

        if (rubyElseCode != null) {
            method.go_to(elseLabel);
        } else {
            method.go_to(exitRescue);
        }

        // Handle Flow exceptions, just propagating them
        method.label(flowCatchBlock);
        {
            // rethrow to outer flow catcher
            method.athrow();
        }

        // Handle Ruby exceptions (RaiseException)
        method.label(rubyCatchBlock);
        {
            method.astore(getExceptionIndex());

            rubyCatchCode.branch(this);
            method.label(afterRubyCatchBody);
            method.go_to(exitRescue);
        }

        // retry handling in the rescue blocks
        if (needsRetry) {
            method.trycatch(rubyCatchBlock, afterRubyCatchBody, catchRetry, p(JumpException.RetryJump.class));
            method.label(catchRetry);
            {
                method.pop();
            }
            method.go_to(beforeBody);
        }

        // and remaining jump exceptions should restore $!
        method.trycatch(beforeBody, afterRubyCatchBody, catchJumps, p(JumpException.FlowControlException.class));
        method.label(catchJumps);
        {
            loadThreadContext();
            method.aload(getPreviousExceptionIndex());
            invokeThreadContext("setErrorInfo", sig(IRubyObject.class, IRubyObject.class));
            method.pop();
            method.athrow();
        }

        if (rubyElseCode != null) {
            method.label(elseLabel);
            rubyElseCode.branch(this);
        }

        method.label(exitRescue);

        // restore the original exception
        loadThreadContext();
        method.aload(getPreviousExceptionIndex());
        invokeThreadContext("setErrorInfo", sig(IRubyObject.class, IRubyObject.class));
        method.pop();
    }

    public void wrapJavaException() {
        loadRuntime();
        loadException();
        wrapJavaObject();
    }

    public void wrapJavaObject() {
        method.invokestatic(p(JavaUtil.class), "convertJavaToUsableRubyObject", sig(IRubyObject.class, Ruby.class, Object.class));
    }

    public void stringOrNil() {
        loadThreadContext();
        invokeUtilityMethod("stringOrNil", sig(IRubyObject.class, ByteList.class, ThreadContext.class));
    }

    public void pushNull() {
        method.aconst_null();
    }

    public void pushString(String str) {
        method.ldc(str);
    }
    
    public void pushByteList(ByteList byteList) {
        script.getCacheCompiler().cacheByteList(this, byteList);
    }

    public void isMethodBound(String name, BranchCallback trueBranch, BranchCallback falseBranch) {
        metaclass();
        method.ldc(name);
        method.iconst_0(); // push false
        method.invokevirtual(p(RubyClass.class), "isMethodBound", sig(boolean.class, params(String.class, boolean.class)));
        Label falseLabel = new Label();
        Label exitLabel = new Label();
        method.ifeq(falseLabel); // EQ == 0 (i.e. false)
        trueBranch.branch(this);
        method.go_to(exitLabel);
        method.label(falseLabel);
        falseBranch.branch(this);
        method.label(exitLabel);
    }

    public void hasBlock(BranchCallback trueBranch, BranchCallback falseBranch) {
        loadBlock();
        method.invokevirtual(p(Block.class), "isGiven", sig(boolean.class));
        Label falseLabel = new Label();
        Label exitLabel = new Label();
        method.ifeq(falseLabel); // EQ == 0 (i.e. false)
        trueBranch.branch(this);
        method.go_to(exitLabel);
        method.label(falseLabel);
        falseBranch.branch(this);
        method.label(exitLabel);
    }

    public void isGlobalDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch) {
        loadRuntime();
        invokeRuby("getGlobalVariables", sig(GlobalVariables.class));
        method.ldc(name);
        method.invokevirtual(p(GlobalVariables.class), "isDefined", sig(boolean.class, params(String.class)));
        Label falseLabel = new Label();
        Label exitLabel = new Label();
        method.ifeq(falseLabel); // EQ == 0 (i.e. false)
        trueBranch.branch(this);
        method.go_to(exitLabel);
        method.label(falseLabel);
        falseBranch.branch(this);
        method.label(exitLabel);
    }

    public void isConstantDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch) {
        loadThreadContext();
        method.ldc(name);
        invokeThreadContext("getConstantDefined", sig(boolean.class, params(String.class)));
        Label falseLabel = new Label();
        Label exitLabel = new Label();
        method.ifeq(falseLabel); // EQ == 0 (i.e. false)
        trueBranch.branch(this);
        method.go_to(exitLabel);
        method.label(falseLabel);
        falseBranch.branch(this);
        method.label(exitLabel);
    }

    public void isInstanceVariableDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch) {
        loadSelf();
        invokeIRubyObject("getInstanceVariables", sig(InstanceVariables.class));
        method.ldc(name);
        //method.invokeinterface(p(IRubyObject.class), "getInstanceVariable", sig(IRubyObject.class, params(String.class)));
        method.invokeinterface(p(InstanceVariables.class), "fastHasInstanceVariable", sig(boolean.class, params(String.class)));
        Label trueLabel = new Label();
        Label exitLabel = new Label();
        //method.ifnonnull(trueLabel);
        method.ifne(trueLabel);
        falseBranch.branch(this);
        method.go_to(exitLabel);
        method.label(trueLabel);
        trueBranch.branch(this);
        method.label(exitLabel);
    }

    public void isClassVarDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch) {
        method.ldc(name);
        method.invokevirtual(p(RubyModule.class), "fastIsClassVarDefined", sig(boolean.class, params(String.class)));
        Label trueLabel = new Label();
        Label exitLabel = new Label();
        method.ifne(trueLabel);
        falseBranch.branch(this);
        method.go_to(exitLabel);
        method.label(trueLabel);
        trueBranch.branch(this);
        method.label(exitLabel);
    }

    public Object getNewEnding() {
        return new Label();
    }

    public void isNil(BranchCallback trueBranch, BranchCallback falseBranch) {
        method.invokeinterface(p(IRubyObject.class), "isNil", sig(boolean.class));
        Label falseLabel = new Label();
        Label exitLabel = new Label();
        method.ifeq(falseLabel); // EQ == 0 (i.e. false)
        trueBranch.branch(this);
        method.go_to(exitLabel);
        method.label(falseLabel);
        falseBranch.branch(this);
        method.label(exitLabel);
    }

    public void isNull(BranchCallback trueBranch, BranchCallback falseBranch) {
        Label falseLabel = new Label();
        Label exitLabel = new Label();
        method.ifnonnull(falseLabel);
        trueBranch.branch(this);
        method.go_to(exitLabel);
        method.label(falseLabel);
        falseBranch.branch(this);
        method.label(exitLabel);
    }

    public void ifNull(Object gotoToken) {
        method.ifnull((Label) gotoToken);
    }

    public void ifNotNull(Object gotoToken) {
        method.ifnonnull((Label) gotoToken);
    }

    public void setEnding(Object endingToken) {
        method.label((Label) endingToken);
    }

    public void go(Object gotoToken) {
        method.go_to((Label) gotoToken);
    }

    public void isConstantBranch(final BranchCallback setup, final String name) {
        BranchCallback catchCode = new BranchCallback() {
            public void branch(BodyCompiler context) {
                // restore the original exception
                loadThreadContext();
                method.aload(getPreviousExceptionIndex());
                invokeThreadContext("setErrorInfo", sig(IRubyObject.class, IRubyObject.class));
                method.pop();

                pushNull();
            }
        };
        BranchCallback regularCode = new BranchCallback() {
            public void branch(BodyCompiler context) {
                setup.branch(BaseBodyCompiler.this);
                method.ldc(name); //[C, C, String]
                invokeUtilityMethod("getDefinedConstantOrBoundMethod", sig(ByteList.class, IRubyObject.class, String.class));
            }
        };
        String mname = getNewRescueName();
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(
                script.getClassVisitor(),
                    ACC_PUBLIC | ACC_SYNTHETIC | ACC_STATIC,
                    mname,
                    sig(ByteList.class, "L" + script.getClassname() + ";", ThreadContext.class, IRubyObject.class, Block.class),
                    null,
                    null);
        SkinnyMethodAdapter old_method = null;
        SkinnyMethodAdapter var_old_method = null;
        SkinnyMethodAdapter inv_old_method = null;
        Label exitRescue = new Label();
        boolean oldWithinProtection = inNestedMethod;
        inNestedMethod = true;
        Label[] oldLoopLabels = currentLoopLabels;
        currentLoopLabels = null;
        int oldArgCount = argParamCount;
        argParamCount = 0; // synthetic methods always have zero arg parameters
        try {
            old_method = this.method;
            var_old_method = getVariableCompiler().getMethodAdapter();
            inv_old_method = getInvocationCompiler().getMethodAdapter();
            this.method = mv;
            getVariableCompiler().setMethodAdapter(mv);
            getInvocationCompiler().setMethodAdapter(mv);

            mv.start();

            // store previous exception for restoration if we rescue something
            loadThreadContext();
            invokeThreadContext("getErrorInfo", sig(IRubyObject.class));
            mv.astore(getPreviousExceptionIndex());

            mv.aload(StandardASMCompiler.THREADCONTEXT_INDEX);
            mv.invokevirtual(p(ThreadContext.class), "getCurrentScope", sig(DynamicScope.class));
            mv.astore(getDynamicScopeIndex());

            // if more than 4 vars, get values array too
            if (scope.getNumberOfVariables() > 4) {
                mv.aload(getDynamicScopeIndex());
                mv.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
                mv.astore(getVarsArrayIndex());
            }

            Label beforeBody = new Label();
            Label afterBody = new Label();
            Label catchBlock = new Label();
            mv.trycatch(beforeBody, afterBody, catchBlock, p(JumpException.class));
            mv.label(beforeBody);

            regularCode.branch(this);

            mv.label(afterBody);
            mv.go_to(exitRescue);
            mv.label(catchBlock);
            mv.astore(getExceptionIndex());

            catchCode.branch(this);

            mv.label(exitRescue);

            mv.areturn();
            mv.end();
        } finally {
            inNestedMethod = oldWithinProtection;
            this.method = old_method;
            getVariableCompiler().setMethodAdapter(var_old_method);
            getInvocationCompiler().setMethodAdapter(inv_old_method);
            currentLoopLabels = oldLoopLabels;
            argParamCount = oldArgCount;
        }

        method.aload(StandardASMCompiler.THIS);
        loadThreadContext();
        loadSelf();
        if (this instanceof ChildScopedBodyCompiler) {
            pushNull();
        } else {
            loadBlock();
        }
        method.invokestatic(
                script.getClassname(),
                mname,
                sig(ByteList.class, "L" + script.getClassname() + ";", ThreadContext.class, IRubyObject.class, Block.class));
    }

    public void metaclass() {
        invokeIRubyObject("getMetaClass", sig(RubyClass.class));
    }

    public void aprintln() {
        method.aprintln();
    }

    public void getVisibilityFor(String name) {
        method.ldc(name);
        method.invokevirtual(p(RubyClass.class), "searchMethod", sig(DynamicMethod.class, params(String.class)));
        method.invokevirtual(p(DynamicMethod.class), "getVisibility", sig(Visibility.class));
    }

    public void isPrivate(Object gotoToken, int toConsume) {
        method.getstatic(p(Visibility.class), "PRIVATE", ci(Visibility.class));
        Label temp = new Label();
        method.if_acmpne(temp);
        while ((toConsume--) > 0) {
            method.pop();
        }
        method.go_to((Label) gotoToken);
        method.label(temp);
    }

    public void isNotProtected(Object gotoToken, int toConsume) {
        method.getstatic(p(Visibility.class), "PROTECTED", ci(Visibility.class));
        Label temp = new Label();
        method.if_acmpeq(temp);
        while ((toConsume--) > 0) {
            method.pop();
        }
        method.go_to((Label) gotoToken);
        method.label(temp);
    }

    public void selfIsKindOf(Object gotoToken) {
        method.invokevirtual(p(RubyClass.class), "getRealClass", sig(RubyClass.class));
        loadSelf();
        method.invokevirtual(p(RubyModule.class), "isInstance", sig(boolean.class, params(IRubyObject.class)));
        method.ifne((Label) gotoToken); // EQ != 0 (i.e. true)
    }

    public void notIsModuleAndClassVarDefined(String name, Object gotoToken) {
        method.dup(); //[?, ?]
        method.instance_of(p(RubyModule.class)); //[?, boolean]
        Label falsePopJmp = new Label();
        Label successJmp = new Label();
        method.ifeq(falsePopJmp);

        method.visitTypeInsn(CHECKCAST, p(RubyModule.class)); //[RubyModule]
        method.ldc(name); //[RubyModule, String]

        method.invokevirtual(p(RubyModule.class), "fastIsClassVarDefined", sig(boolean.class, params(String.class))); //[boolean]
        method.ifeq((Label) gotoToken);
        method.go_to(successJmp);
        method.label(falsePopJmp);
        method.pop();
        method.go_to((Label) gotoToken);
        method.label(successJmp);
    }

    public void ifSingleton(Object gotoToken) {
        method.invokevirtual(p(RubyModule.class), "isSingleton", sig(boolean.class));
        method.ifne((Label) gotoToken); // EQ == 0 (i.e. false)
    }

    public void getInstanceVariable(String name) {
        method.ldc(name);
        invokeIRubyObject("getInstanceVariables", sig(InstanceVariables.class));
        method.invokeinterface(p(InstanceVariables.class), "fastGetInstanceVariable", sig(IRubyObject.class, params(String.class)));
    }

    public void getFrameName() {
        loadThreadContext();
        invokeThreadContext("getFrameName", sig(String.class));
    }

    public void getFrameKlazz() {
        loadThreadContext();
        invokeThreadContext("getFrameKlazz", sig(RubyModule.class));
    }

    public void superClass() {
        loadSelf();
        metaclass();
        method.swap();
        invokeUtilityMethod("findImplementerIfNecessary", sig(RubyModule.class, params(RubyModule.class, RubyModule.class)));
        method.invokevirtual(p(RubyModule.class), "getSuperClass", sig(RubyClass.class));
    }

    public void attached() {
        method.visitTypeInsn(CHECKCAST, p(MetaClass.class));
        method.invokevirtual(p(MetaClass.class), "getAttached", sig(IRubyObject.class));
    }

    public void ifNotSuperMethodBound(Object token) {
        method.swap();
        method.iconst_0();
        method.invokevirtual(p(RubyModule.class), "isMethodBound", sig(boolean.class, params(String.class, boolean.class)));
        method.ifeq((Label) token);
    }

    public void concatArrays() {
        method.invokevirtual(p(RubyArray.class), "concat", sig(RubyArray.class, params(IRubyObject.class)));
    }

    public void concatObjectArrays() {
        invokeUtilityMethod("concatObjectArrays", sig(IRubyObject[].class, params(IRubyObject[].class, IRubyObject[].class)));
    }

    public void appendToArray() {
        method.invokevirtual(p(RubyArray.class), "append", sig(RubyArray.class, params(IRubyObject.class)));
    }

    public void appendToObjectArray() {
        invokeUtilityMethod("appendToObjectArray", sig(IRubyObject[].class, params(IRubyObject[].class, IRubyObject.class)));
    }

    public void splatToArguments() {
        invokeUtilityMethod("splatToArguments", sig(IRubyObject[].class, IRubyObject.class));
    }

    public void splatToArguments19() {
        invokeUtilityMethod("splatToArguments19", sig(IRubyObject[].class, IRubyObject.class));
    }
    
    public void argsCatToArguments() {
        invokeUtilityMethod("argsCatToArguments", sig(IRubyObject[].class, IRubyObject[].class, IRubyObject.class));
    }
    
    public void argsCatToArguments19() {
        invokeUtilityMethod("argsCatToArguments19", sig(IRubyObject[].class, IRubyObject[].class, IRubyObject.class));
    }

    public void convertToJavaArray() {
        method.invokestatic(p(ArgsUtil.class), "convertToJavaArray", sig(IRubyObject[].class, params(IRubyObject.class)));
    }

    public void aliasGlobal(String newName, String oldName) {
        loadRuntime();
        invokeRuby("getGlobalVariables", sig(GlobalVariables.class));
        method.ldc(newName);
        method.ldc(oldName);
        method.invokevirtual(p(GlobalVariables.class), "alias", sig(Void.TYPE, params(String.class, String.class)));
        loadNil();
    }
    
    public void raiseTypeError(String msg) {
        loadRuntime();        
        method.ldc(msg);
        invokeRuby("newTypeError", sig(RaiseException.class, params(String.class)));
        method.athrow();                                    
    }    

    public void undefMethod(CompilerCallback nameArg) {
        loadThreadContext();
        nameArg.call(this);
        invokeUtilityMethod("undefMethod", sig(IRubyObject.class, ThreadContext.class, Object.class));
    }

    public void defineClass(
            final String name,
            final StaticScope staticScope,
            final CompilerCallback superCallback,
            final CompilerCallback pathCallback,
            final CompilerCallback bodyCallback,
            final CompilerCallback receiverCallback,
            final ASTInspector inspector) {
        String classMethodName = null;
        String rubyName;
        if (receiverCallback == null) {
            String mangledName = JavaNameMangler.mangleMethodName(name);
            classMethodName = "class_" + script.getAndIncrementMethodIndex() + "$RUBY$" + mangledName;
            rubyName = mangledName;
        } else {
            classMethodName = "sclass_" + script.getAndIncrementMethodIndex() + "$RUBY$__singleton__";
            rubyName = "__singleton__";
        }

        final RootScopedBodyCompiler classBody = new ClassBodyCompiler(script, classMethodName, rubyName, inspector, staticScope);

        // Here starts the logic for the class definition
        Label start = new Label();
        Label end = new Label();
        Label after = new Label();
        Label noException = new Label();
        classBody.method.trycatch(start, end, after, null);

        classBody.beginMethod(new ArgumentsCallback() {
            public int getArity() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public void call(BodyCompiler context) {
                classBody.loadThreadContext();
                classBody.method.aload(StandardASMCompiler.SELF_INDEX); // module to run the class under passed in as self
                classBody.method.checkcast(p(RubyModule.class));

                // static scope
                script.getCacheCompiler().cacheStaticScope(classBody, staticScope);
                if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                    classBody.invokeThreadContext("preCompiledClass", sig(Void.TYPE, params(RubyModule.class, StaticScope.class)));
                } else {
                    classBody.invokeThreadContext("preCompiledClassDummyScope", sig(Void.TYPE, params(RubyModule.class, StaticScope.class)));
                }
            }
        }, staticScope);

        // CLASS BODY
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) classBody.traceClass();

        classBody.method.label(start);

        bodyCallback.call(classBody);

        classBody.method.label(end);
        // finally with no exception
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) classBody.traceEnd();
        classBody.loadThreadContext();
        classBody.invokeThreadContext("postCompiledClass", sig(Void.TYPE, params()));

        classBody.method.go_to(noException);

        classBody.method.label(after);
        // finally with exception
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) classBody.traceEnd();
        classBody.loadThreadContext();
        classBody.invokeThreadContext("postCompiledClass", sig(Void.TYPE, params()));
        classBody.method.athrow();

        classBody.method.label(noException);

        classBody.endBody();

        // prepare to call class definition method
        method.aload(StandardASMCompiler.THIS);
        loadThreadContext();

        // class object
        if (receiverCallback == null) {
            // no receiver for singleton class
            if (superCallback != null) {
                // but there's a superclass provided
                loadRuntime();
                superCallback.call(this);

                invokeUtilityMethod("prepareSuperClass", sig(RubyClass.class, params(Ruby.class, IRubyObject.class)));
            } else {
                method.aconst_null();
            }

            loadThreadContext();

            pathCallback.call(this);

            invokeUtilityMethod("prepareClassNamespace", sig(RubyModule.class, params(ThreadContext.class, IRubyObject.class)));

            method.swap();

            method.ldc(name);

            method.swap();

            method.invokevirtual(p(RubyModule.class), "defineOrGetClassUnder", sig(RubyClass.class, params(String.class, RubyClass.class)));
        } else {
            loadRuntime();
            receiverCallback.call(this);

            invokeUtilityMethod("getSingletonClass", sig(RubyClass.class, params(Ruby.class, IRubyObject.class)));
        }

        method.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));

        method.invokestatic(script.getClassname(), classMethodName, StandardASMCompiler.getStaticMethodSignature(script.getClassname(), 0));
    }

    public void defineModule(final String name, final StaticScope staticScope, final CompilerCallback pathCallback, final CompilerCallback bodyCallback, final ASTInspector inspector) {
        String mangledName = JavaNameMangler.mangleMethodName(name);
        String moduleMethodName = "module__" + script.getAndIncrementMethodIndex() + "$RUBY$" + mangledName;

        final RootScopedBodyCompiler classBody = new ClassBodyCompiler(script, moduleMethodName, mangledName, inspector, staticScope);

        // Here starts the logic for the class definition
        Label start = new Label();
        Label end = new Label();
        Label after = new Label();
        Label noException = new Label();
        classBody.method.trycatch(start, end, after, null);

        classBody.beginMethod(new ArgumentsCallback() {
            public int getArity() {
                return 0;
            }

            public void call(BodyCompiler context) {
                classBody.loadThreadContext();
                classBody.method.aload(StandardASMCompiler.SELF_INDEX); // module to run the module under passed in as self
                classBody.method.checkcast(p(RubyModule.class));

                // static scope
                script.getCacheCompiler().cacheStaticScope(classBody, staticScope);
                if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                    classBody.invokeThreadContext("preCompiledClass", sig(Void.TYPE, params(RubyModule.class, StaticScope.class)));
                } else {
                    classBody.invokeThreadContext("preCompiledClassDummyScope", sig(Void.TYPE, params(RubyModule.class, StaticScope.class)));
                }
            }
        }, staticScope);

        // CLASS BODY

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) classBody.traceClass();

        classBody.method.label(start);

        bodyCallback.call(classBody);
        classBody.method.label(end);

        classBody.method.go_to(noException);

        classBody.method.label(after);
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) classBody.traceEnd();
        classBody.loadThreadContext();
        classBody.invokeThreadContext("postCompiledClass", sig(Void.TYPE, params()));
        classBody.method.athrow();

        classBody.method.label(noException);
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) classBody.traceEnd();
        classBody.loadThreadContext();
        classBody.invokeThreadContext("postCompiledClass", sig(Void.TYPE, params()));

        classBody.endBody();

        // prepare to call class definition method
        method.aload(StandardASMCompiler.THIS);
        loadThreadContext();

        // prepare module object
        loadThreadContext();
        pathCallback.call(this);
        invokeUtilityMethod("prepareClassNamespace", sig(RubyModule.class, params(ThreadContext.class, IRubyObject.class)));
        method.ldc(name);
        method.invokevirtual(p(RubyModule.class), "defineOrGetModuleUnder", sig(RubyModule.class, params(String.class)));

        method.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));

        method.invokestatic(script.getClassname(), moduleMethodName, StandardASMCompiler.getStaticMethodSignature(script.getClassname(), 0));
    }

    public void unwrapPassedBlock() {
        loadBlock();
        invokeUtilityMethod("getBlockFromBlockPassBody", sig(Block.class, params(IRubyObject.class, Block.class)));
    }

    public void performBackref(char type) {
        loadThreadContext();
        switch (type) {
        case '~':
            invokeUtilityMethod("backref", sig(IRubyObject.class, params(ThreadContext.class)));
            break;
        case '&':
            invokeUtilityMethod("backrefLastMatch", sig(IRubyObject.class, params(ThreadContext.class)));
            break;
        case '`':
            invokeUtilityMethod("backrefMatchPre", sig(IRubyObject.class, params(ThreadContext.class)));
            break;
        case '\'':
            invokeUtilityMethod("backrefMatchPost", sig(IRubyObject.class, params(ThreadContext.class)));
            break;
        case '+':
            invokeUtilityMethod("backrefMatchLast", sig(IRubyObject.class, params(ThreadContext.class)));
            break;
        default:
            throw new NotCompilableException("ERROR: backref with invalid type");
        }
    }

    public void callZSuper(CompilerCallback closure) {
        ArgumentsCallback argsCallback = new ArgumentsCallback() {
            public int getArity() {
                return -1;
            }

            public void call(BodyCompiler context) {
                loadThreadContext();
                invokeUtilityMethod("getArgValues", sig(IRubyObject[].class, ThreadContext.class));
            }
        };
        getInvocationCompiler().invokeDynamicVarargs(null, null, argsCallback, CallType.SUPER, closure, false);
    }

    public void checkIsExceptionHandled(ArgumentsCallback rescueArgs) {
        // original exception is on stack
        rescueArgs.call(this);
        loadThreadContext();

        switch (rescueArgs.getArity()) {
        case 1:
            invokeUtilityMethod("isJavaExceptionHandled", sig(IRubyObject.class, Throwable.class, IRubyObject.class, ThreadContext.class));
            break;
        case 2:
            invokeUtilityMethod("isJavaExceptionHandled", sig(IRubyObject.class, Throwable.class, IRubyObject.class, IRubyObject.class, ThreadContext.class));
            break;
        case 3:
            invokeUtilityMethod("isJavaExceptionHandled", sig(IRubyObject.class, Throwable.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, ThreadContext.class));
            break;
        default:
            invokeUtilityMethod("isJavaExceptionHandled", sig(IRubyObject.class, Throwable.class, IRubyObject[].class, ThreadContext.class));
        }
    }

    public void rethrowException() {
        loadException();
        method.athrow();
    }

    public void loadClass(String name) {
        loadRuntime();
        method.ldc(name);
        invokeRuby("getClass", sig(RubyClass.class, String.class));
    }

    public void loadStandardError() {
        loadRuntime();
        invokeRuby("getStandardError", sig(RubyClass.class));
    }

    public void unwrapRaiseException() {
        // RaiseException is on stack, get RubyException out
        method.invokevirtual(p(RaiseException.class), "getException", sig(RubyException.class));
    }

    public void loadException() {
        method.aload(getExceptionIndex());
    }

    public void setFilePosition(ISourcePosition position) {
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            loadThreadContext();
            method.ldc(position.getFile());
            invokeThreadContext("setFile", sig(void.class, params(String.class)));
        }
    }

    public void setLinePosition(ISourcePosition position) {
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            if (lastPositionLine == position.getStartLine()) {
                // updating position for same line; skip
                return;
            } else {
                lastPositionLine = position.getStartLine();
                loadThreadContext();
                method.pushInt(position.getStartLine());
                method.invokestatic(script.getClassname(), "setPosition", sig(void.class, params(ThreadContext.class, int.class)));
            }
        }
    }

    public void checkWhenWithSplat() {
        loadThreadContext();
        invokeUtilityMethod("isWhenTriggered", sig(RubyBoolean.class, IRubyObject.class, IRubyObject.class, ThreadContext.class));
    }

    public void issueRetryEvent() {
        invokeUtilityMethod("retryJump", sig(IRubyObject.class));
    }

    public void defineNewMethod(String name, int methodArity, StaticScope scope,
            CompilerCallback body, CompilerCallback args,
            CompilerCallback receiver, ASTInspector inspector, boolean root,
            String filename, int line, String parameterDesc) {
        String mangledName = JavaNameMangler.mangleMethodName(name);
        String newMethodName = "method__" + script.getAndIncrementMethodIndex() + "$RUBY$" + mangledName;

        BodyCompiler methodCompiler = script.startMethod(name, newMethodName, args, scope, inspector);

        // callbacks to fill in method body
        body.call(methodCompiler);

        methodCompiler.endBody();

        // prepare to call "def" utility method to handle def logic
        loadThreadContext();

        loadSelf();

        if (receiver != null) {
            receiver.call(this);        // script object
        }
        method.aload(StandardASMCompiler.THIS);

        method.ldc(name);

        method.ldc(newMethodName);

        script.getCacheCompiler().cacheStaticScope(this, scope);

        method.pushInt(methodArity);

        // arities
        method.ldc(filename);
        method.ldc(line);
        method.getstatic(p(CallConfiguration.class), inspector.getCallConfig().name(), ci(CallConfiguration.class));
        method.ldc(parameterDesc);

        if (receiver != null) {
            invokeUtilityMethod("defs", sig(IRubyObject.class,
                    params(ThreadContext.class, IRubyObject.class, IRubyObject.class, Object.class, String.class, String.class, StaticScope.class, int.class, String.class, int.class, CallConfiguration.class, String.class)));
        } else {
            invokeUtilityMethod("def", sig(IRubyObject.class,
                    params(ThreadContext.class, IRubyObject.class, Object.class, String.class, String.class, StaticScope.class, int.class, String.class, int.class, CallConfiguration.class, String.class)));
        }

        script.addInvokerDescriptor(newMethodName, methodArity, scope, inspector.getCallConfig(), filename, line);
    }

    public void rethrowIfSystemExit() {
        loadRuntime();
        method.ldc("SystemExit");
        method.invokevirtual(p(Ruby.class), "fastGetClass", sig(RubyClass.class, String.class));
        method.swap();
        method.invokevirtual(p(RubyModule.class), "isInstance", sig(boolean.class, params(IRubyObject.class)));
        method.iconst_0();
        Label ifEnd = new Label();
        method.if_icmpeq(ifEnd);
        loadException();
        method.athrow();
        method.label(ifEnd);
    }

    public void literalSwitch(int[] cases, Object[] bodies, ArrayCallback arrayCallback, CompilerCallback defaultCallback) {
        // TODO assuming case is a fixnum
        method.checkcast(p(RubyFixnum.class));
        method.invokevirtual(p(RubyFixnum.class), "getLongValue", sig(long.class));
        method.l2i();

        Map<Object, Label> labelMap = new HashMap<Object, Label>();
        Label[] labels = new Label[cases.length];
        for (int i = 0; i < labels.length; i++) {
            Object body = bodies[i];
            Label label = labelMap.get(body);
            if (label == null) {
                label = new Label();
                labelMap.put(body, label);
            }
            labels[i] = label;
        }
        Label defaultLabel = new Label();
        Label endLabel = new Label();

        method.lookupswitch(defaultLabel, cases, labels);
        Set<Label> labelDone = new HashSet<Label>();
        for (int i = 0; i < cases.length; i++) {
            if (labelDone.contains(labels[i])) continue;
            labelDone.add(labels[i]);
            method.label(labels[i]);
            arrayCallback.nextValue(this, bodies, i);
            method.go_to(endLabel);
        }

        method.label(defaultLabel);
        defaultCallback.call(this);
        method.label(endLabel);
    }

    public void typeCheckBranch(Class type, BranchCallback trueCallback, BranchCallback falseCallback) {
        Label elseLabel = new Label();
        Label done = new Label();

        method.dup();
        method.instance_of(p(type));
        method.ifeq(elseLabel);

        trueCallback.branch(this);
        method.go_to(done);

        method.label(elseLabel);
        falseCallback.branch(this);

        method.label(done);
    }
    
    public void loadFilename() {
        loadRuntime();
        loadThis();
        method.getfield(getScriptCompiler().getClassname(), "filename", ci(String.class));
        method.invokestatic(p(RubyString.class), "newString", sig(RubyString.class, Ruby.class, CharSequence.class));
    }

    public void compileSequencedConditional(
            CompilerCallback inputValue,
            FastSwitchType fastSwitchType,
            Map<CompilerCallback, int[]> switchCases,
            List<ArgumentsCallback> conditionals,
            List<CompilerCallback> bodies,
            CompilerCallback fallback) {
        Map<CompilerCallback, Label> bodyLabels = new HashMap<CompilerCallback, Label>();
        Label defaultCase = new Label();
        Label slowPath = new Label();
        CompilerCallback getCaseValue = null;
        final int tmp = getVariableCompiler().grabTempLocal();
        
        if (inputValue != null) {
            // we have an input case, prepare branching logic
            inputValue.call(this);
            getVariableCompiler().setTempLocal(tmp);
            getCaseValue = new CompilerCallback() {
                public void call(BodyCompiler context) {
                    getVariableCompiler().getTempLocal(tmp);
                }
            };

            if (switchCases != null) {
                // we have optimized switch cases, build a lookupswitch

                SortedMap<Integer, Label> optimizedLabels = new TreeMap<Integer, Label>();
                for (Map.Entry<CompilerCallback, int[]> entry : switchCases.entrySet()) {
                    Label lbl = new Label();

                    bodyLabels.put(entry.getKey(), lbl);
                    
                    for (int i : entry.getValue()) {
                        optimizedLabels.put(i, lbl);
                    }
                }

                int[] caseValues = new int[optimizedLabels.size()];
                Label[] caseLabels = new Label[optimizedLabels.size()];
                Set<Map.Entry<Integer, Label>> entrySet = optimizedLabels.entrySet();
                Iterator<Map.Entry<Integer, Label>> iterator = entrySet.iterator();
                for (int i = 0; i < entrySet.size(); i++) {
                    Map.Entry<Integer, Label> entry = iterator.next();
                    caseValues[i] = entry.getKey();
                    caseLabels[i] = entry.getValue();
                }

                // checkcast the value; if match, fast path; otherwise proceed to slow logic
                getCaseValue.call(this);
                method.instance_of(p(fastSwitchType.getAssociatedClass()));
                method.ifeq(slowPath);

                switch (fastSwitchType) {
                case FIXNUM:
                    getCaseValue.call(this);
                    method.checkcast(p(RubyFixnum.class));
                    method.invokevirtual(p(RubyFixnum.class), "getLongValue", sig(long.class));
                    method.l2i();
                    break;
                case SINGLE_CHAR_STRING:
                    getCaseValue.call(this);
                    invokeUtilityMethod("isFastSwitchableSingleCharString", sig(boolean.class, IRubyObject.class));
                    method.ifeq(slowPath);
                    getCaseValue.call(this);
                    invokeUtilityMethod("getFastSwitchSingleCharString", sig(int.class, IRubyObject.class));
                    break;
                case SINGLE_CHAR_SYMBOL:
                    getCaseValue.call(this);
                    invokeUtilityMethod("isFastSwitchableSingleCharSymbol", sig(boolean.class, IRubyObject.class));
                    method.ifeq(slowPath);
                    getCaseValue.call(this);
                    invokeUtilityMethod("getFastSwitchSingleCharSymbol", sig(int.class, IRubyObject.class));
                    break;
                }

                method.lookupswitch(defaultCase, caseValues, caseLabels);
            }
        }

        Label done = new Label();

        // expression-based tests + bodies
        Label currentLabel = slowPath;
        for (int i = 0; i < conditionals.size(); i++) {
            ArgumentsCallback conditional = conditionals.get(i);
            CompilerCallback body = bodies.get(i);

            method.label(currentLabel);

            getInvocationCompiler().invokeEqq(conditional, getCaseValue);
            if (i + 1 < conditionals.size()) {
                // normal case, create a new label
                currentLabel = new Label();
            } else {
                // last conditional case, use defaultCase
                currentLabel = defaultCase;
            }
            method.ifeq(currentLabel);

            Label bodyLabel = bodyLabels.get(body);
            if (bodyLabel != null) method.label(bodyLabel);

            body.call(this);

            method.go_to(done);
        }

        // "else" body
        method.label(currentLabel);
        fallback.call(this);

        method.label(done);

        getVariableCompiler().releaseTempLocal();
    }

    public void traceLine() {
        loadThreadContext();
        invokeUtilityMethod("traceLine", sig(void.class, ThreadContext.class));
    }

    public void traceClass() {
        loadThreadContext();
        invokeUtilityMethod("traceClass", sig(void.class, ThreadContext.class));
    }

    public void traceEnd() {
        loadThreadContext();
        invokeUtilityMethod("traceEnd", sig(void.class, ThreadContext.class));
    }

    public void preMultiAssign(int head, boolean args) {
        // arrayish object is on stack, call utility and unpack
        if (head == 1 && args) {
            invokeUtilityMethod("arraySlice1N", sig(IRubyObject[].class, IRubyObject.class));
            method.dup();
            method.pushInt(1);
            method.aaload();
            method.swap();
            method.pushInt(0);
            method.aaload();
        } else if (head == 1 && !args) {
            invokeUtilityMethod("arraySlice1", sig(IRubyObject.class, IRubyObject.class));
        } else {
            throw new RuntimeException("invalid preMultiAssign args: " + head + ", " + args);
        }
    }

    public void argsPush() {
        invokeUtilityMethod("argsPush", sig(RubyArray.class, RubyArray.class, IRubyObject.class));
    }

    public void argsCat() {
        invokeUtilityMethod("argsCat", sig(RubyArray.class, IRubyObject.class, IRubyObject.class));
    }

    public void loadEncoding(Encoding encoding) {
        script.getCacheCompiler().cacheRubyEncoding(this, encoding);
    }

    public void definedCall(String name) {
        loadThreadContext();
        loadSelf();
        method.dup2_x1();
        method.pop2();
        method.ldc(name);
        invokeUtilityMethod("getDefinedCall", sig(ByteList.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));
    }

    public void definedNot() {
        loadRuntime();
        method.swap();
        invokeUtilityMethod("getDefinedNot", sig(ByteList.class, Ruby.class, ByteList.class));
    }
}
