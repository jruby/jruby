/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ast.NodeType;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CacheCompiler;
import org.jruby.compiler.CompilerCallback;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.objectweb.asm.Label;
import static org.jruby.util.CodegenUtils.*;

/**
 *
 * @author headius
 */
public class InheritedCacheCompiler implements CacheCompiler {
    protected StandardASMCompiler scriptCompiler;
    int scopeCount = 0;
    int callSiteCount = 0;
    List<String> callSiteList = new ArrayList<String>();
    List<CallType> callTypeList = new ArrayList<CallType>();
    Map<String, Integer> stringIndices = new HashMap<String, Integer>();
    Map<BigInteger, String> bigIntegers = new HashMap<BigInteger, String>();
    Map<String, Integer> symbolIndices = new HashMap<String, Integer>();
    Map<Long, Integer> fixnumIndices = new HashMap<Long, Integer>();
    int inheritedSymbolCount = 0;
    int inheritedStringCount = 0;
    int inheritedRegexpCount = 0;
    int inheritedBigIntegerCount = 0;
    int inheritedVariableReaderCount = 0;
    int inheritedVariableWriterCount = 0;
    int inheritedFixnumCount = 0;
    int inheritedConstantCount = 0;
    int inheritedBlockBodyCount = 0;
    int inheritedBlockCallbackCount = 0;
    int inheritedMethodCount = 0;

    boolean runtimeCacheInited = false;
    
    public InheritedCacheCompiler(StandardASMCompiler scriptCompiler) {
        this.scriptCompiler = scriptCompiler;
    }

    public void cacheStaticScope(BaseBodyCompiler method, StaticScope scope) {
        StringBuffer scopeNames = new StringBuffer();
        for (int i = 0; i < scope.getVariables().length; i++) {
            if (i != 0) scopeNames.append(';');
            scopeNames.append(scope.getVariables()[i]);
        }

        // retrieve scope from scopes array
        method.loadThis();
        method.loadThreadContext();
        method.method.ldc(scopeNames.toString());
        if (scopeCount < AbstractScript.NUMBERED_SCOPE_COUNT) {
            // use numbered access method
            method.method.invokevirtual(scriptCompiler.getClassname(), "getScope" + scopeCount, sig(StaticScope.class, ThreadContext.class, String.class));
        } else {
            method.method.pushInt(scopeCount);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getScope", sig(StaticScope.class, ThreadContext.class, String.class, int.class));
        }

        scopeCount++;
    }
    
    public void cacheCallSite(BaseBodyCompiler method, String name, CallType callType) {
        // retrieve call site from sites array
        method.loadThis();
        if (callSiteCount < AbstractScript.NUMBERED_CALLSITE_COUNT) {
            // use numbered access method
            method.method.invokevirtual(scriptCompiler.getClassname(), "getCallSite" + callSiteCount, sig(CallSite.class));
        } else {
            method.method.pushInt(callSiteCount);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getCallSite", sig(CallSite.class, int.class));
        }

        // add name to call site list
        callSiteList.add(name);
        callTypeList.add(callType);
        
        callSiteCount++;
    }
    
    public void cacheSymbol(BaseBodyCompiler method, String symbol) {
        Integer index = symbolIndices.get(symbol);
        if (index == null) {
            index = new Integer(inheritedSymbolCount++);
            symbolIndices.put(symbol, index);
        }

        method.loadThis();
        method.loadRuntime();
        if (index < AbstractScript.NUMBERED_SYMBOL_COUNT) {
            method.method.ldc(symbol);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getSymbol" + index, sig(RubySymbol.class, Ruby.class, String.class));
        } else {
            method.method.ldc(index.intValue());
            method.method.ldc(symbol);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getSymbol", sig(RubySymbol.class, Ruby.class, int.class, String.class));
        }
    }

    public void cacheRegexp(BaseBodyCompiler method, String pattern, int options) {
        method.loadThis();
        method.loadRuntime();
        int index = inheritedRegexpCount++;
        if (index < AbstractScript.NUMBERED_REGEXP_COUNT) {
            method.method.ldc(pattern);
            method.method.ldc(options);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getRegexp" + index, sig(RubyRegexp.class, Ruby.class, String.class, int.class));
        } else {
            method.method.pushInt(index);
            method.method.ldc(pattern);
            method.method.ldc(options);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getRegexp", sig(RubyRegexp.class, Ruby.class, int.class, String.class, int.class));
        }
    }

    public void cacheDRegexp(BaseBodyCompiler method, CompilerCallback createStringCallback, int options) {
        int index = inheritedRegexpCount++;
        Label alreadyCompiled = new Label();

        method.loadThis();
        method.method.getfield(scriptCompiler.getClassname(), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
        method.method.pushInt(index);
        method.method.invokevirtual(p(AbstractScript.RuntimeCache.class), "getRegexp", sig(RubyRegexp.class, int.class));
        method.method.dup();

        method.ifNotNull(alreadyCompiled);

        method.method.pop();
        method.loadThis();
        method.method.getfield(scriptCompiler.getClassname(), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
        method.method.pushInt(index);
        createStringCallback.call(method);
        method.method.ldc(options);
        method.method.invokevirtual(p(AbstractScript.RuntimeCache.class), "cacheRegexp", sig(RubyRegexp.class, int.class, RubyString.class, int.class));

        method.method.label(alreadyCompiled);
    }
    
    public void cacheFixnum(BaseBodyCompiler method, long value) {
        if (value <= 5 && value >= -1) {
            method.loadRuntime();
            switch ((int)value) {
            case -1:
                method.method.invokestatic(p(RubyFixnum.class), "minus_one", sig(RubyFixnum.class, Ruby.class));
                break;
            case 0:
                method.method.invokestatic(p(RubyFixnum.class), "zero", sig(RubyFixnum.class, Ruby.class));
                break;
            case 1:
                method.method.invokestatic(p(RubyFixnum.class), "one", sig(RubyFixnum.class, Ruby.class));
                break;
            case 2:
                method.method.invokestatic(p(RubyFixnum.class), "two", sig(RubyFixnum.class, Ruby.class));
                break;
            case 3:
                method.method.invokestatic(p(RubyFixnum.class), "three", sig(RubyFixnum.class, Ruby.class));
                break;
            case 4:
                method.method.invokestatic(p(RubyFixnum.class), "four", sig(RubyFixnum.class, Ruby.class));
                break;
            case 5:
                method.method.invokestatic(p(RubyFixnum.class), "five", sig(RubyFixnum.class, Ruby.class));
                break;
            default:
                throw new RuntimeException("wtf?");
            }
        } else {
            Integer index = fixnumIndices.get(value);
            if (index == null) {
                index = new Integer(inheritedFixnumCount++);
                fixnumIndices.put(value, index);
            }
            
            method.loadThis();
            method.loadRuntime();
            if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                if (index < AbstractScript.NUMBERED_FIXNUM_COUNT) {
                    method.method.pushInt((int)value);
                    method.method.invokevirtual(scriptCompiler.getClassname(), "getFixnum" + index, sig(RubyFixnum.class, Ruby.class, int.class));
                } else {
                    method.method.pushInt(index.intValue());
                    method.method.pushInt((int)value);
                    method.method.invokevirtual(scriptCompiler.getClassname(), "getFixnum", sig(RubyFixnum.class, Ruby.class, int.class, int.class));
                }
            } else {
                method.method.pushInt(index.intValue());
                method.method.ldc(value);
                method.method.invokevirtual(scriptCompiler.getClassname(), "getFixnum", sig(RubyFixnum.class, Ruby.class, int.class, long.class));
            }
        }
    }

    public void cacheConstant(BaseBodyCompiler method, String constantName) {
        method.loadThis();
        method.loadThreadContext();
        method.method.ldc(constantName);
        if (inheritedConstantCount < AbstractScript.NUMBERED_CONSTANT_COUNT) {
            method.method.invokevirtual(scriptCompiler.getClassname(), "getConstant" + inheritedConstantCount, sig(IRubyObject.class, ThreadContext.class, String.class));
        } else {
            method.method.pushInt(inheritedConstantCount);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getConstant", sig(IRubyObject.class, ThreadContext.class, String.class, int.class));
        }

        inheritedConstantCount++;
    }

    public void cacheConstantFrom(BaseBodyCompiler method, String constantName) {
        // module is on top of stack
        method.loadThis();
        method.method.swap();
        method.loadThreadContext();
        method.method.ldc(constantName);
        if (inheritedConstantCount < AbstractScript.NUMBERED_CONSTANT_COUNT) {
            method.method.invokevirtual(scriptCompiler.getClassname(), "getConstantFrom" + inheritedConstantCount, sig(IRubyObject.class, RubyModule.class, ThreadContext.class, String.class));
        } else {
            method.method.pushInt(inheritedConstantCount);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getConstantFrom", sig(IRubyObject.class, RubyModule.class, ThreadContext.class, String.class, int.class));
        }

        inheritedConstantCount++;
    }

    public void cacheString(BaseBodyCompiler method, ByteList contents) {
        String asString = contents.toString();
        Integer index = stringIndices.get(asString);
        if (index == null) {
            index = new Integer(inheritedStringCount++);
            stringIndices.put(asString, index);
        }

        method.loadThis();
        method.loadRuntime();
        if (index < AbstractScript.NUMBERED_STRING_COUNT) {
            method.method.invokevirtual(scriptCompiler.getClassname(), "getString" + index, sig(RubyString.class, Ruby.class));
        } else {
            method.method.ldc(index.intValue());
            method.method.invokevirtual(scriptCompiler.getClassname(), "getString", sig(RubyString.class, Ruby.class, int.class));
        }
    }

    public void cacheBigInteger(BaseBodyCompiler method, BigInteger bigint) {
        method.loadThis();
        method.loadRuntime();
        int index = inheritedBigIntegerCount++;
        if (index < AbstractScript.NUMBERED_BIGINTEGER_COUNT) {
            method.method.ldc(bigint.toString(16));
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBigInteger" + index, sig(BigInteger.class, Ruby.class, String.class));
        } else {
            method.method.pushInt(index);
            method.method.ldc(bigint.toString(16));
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBigInteger", sig(BigInteger.class, Ruby.class, int.class, String.class));
        }
    }

    public void cachedGetVariable(BaseBodyCompiler method, String name) {
        method.loadThis();
        method.loadRuntime();
        int index = inheritedVariableReaderCount++;
        if (index < AbstractScript.NUMBERED_VARIABLEREADER_COUNT) {
            method.method.ldc(name);
            method.loadSelf();
            method.method.invokevirtual(scriptCompiler.getClassname(), "getVariable" + index, sig(IRubyObject.class, Ruby.class, String.class, IRubyObject.class));
        } else {
            method.method.pushInt(index);
            method.method.ldc(name);
            method.loadSelf();
            method.method.invokevirtual(scriptCompiler.getClassname(), "getVariable", sig(IRubyObject.class, Ruby.class, int.class, String.class, IRubyObject.class));
        }
    }

    public void cachedSetVariable(BaseBodyCompiler method, String name, CompilerCallback valueCallback) {
        method.loadThis();
        method.loadRuntime();
        int index = inheritedVariableWriterCount++;
        if (index < AbstractScript.NUMBERED_VARIABLEWRITER_COUNT) {
            method.method.ldc(name);
            method.loadSelf();
            valueCallback.call(method);
            method.method.invokevirtual(scriptCompiler.getClassname(), "setVariable" + index, sig(IRubyObject.class, Ruby.class, String.class, IRubyObject.class, IRubyObject.class));
        } else {
            method.method.pushInt(index);
            method.method.ldc(name);
            method.loadSelf();
            valueCallback.call(method);
            method.method.invokevirtual(scriptCompiler.getClassname(), "setVariable", sig(IRubyObject.class, Ruby.class, int.class, String.class, IRubyObject.class, IRubyObject.class));
        }
    }

    public void cacheClosure(BaseBodyCompiler method, String closureMethod, int arity, StaticScope scope, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector) {
        // build scope names string
        StringBuffer scopeNames = new StringBuffer();
        for (int i = 0; i < scope.getVariables().length; i++) {
            if (i != 0) scopeNames.append(';');
            scopeNames.append(scope.getVariables()[i]);
        }

        // build descriptor string
        String descriptor =
                closureMethod + ',' +
                arity + ',' +
                scopeNames + ',' +
                hasMultipleArgsHead + ',' +
                BlockBody.asArgumentType(argsNodeId) + ',' +
                !(inspector.hasClosure() || inspector.hasScopeAwareMethods());

        method.loadThis();
        method.loadThreadContext();

        if (inheritedBlockBodyCount < AbstractScript.NUMBERED_BLOCKBODY_COUNT) {
            method.method.ldc(descriptor);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockBody" + inheritedBlockBodyCount, sig(BlockBody.class, ThreadContext.class, String.class));
        } else {
            method.method.pushInt(inheritedBlockBodyCount);
            method.method.ldc(descriptor);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockBody", sig(BlockBody.class, ThreadContext.class, int.class, String.class));
        }

        inheritedBlockBodyCount++;
    }

    public void cacheClosure19(BaseBodyCompiler method, String closureMethod, int arity, StaticScope scope, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector) {
        // build scope names string
        StringBuffer scopeNames = new StringBuffer();
        for (int i = 0; i < scope.getVariables().length; i++) {
            if (i != 0) scopeNames.append(';');
            scopeNames.append(scope.getVariables()[i]);
        }

        // build descriptor string
        String descriptor =
                closureMethod + ',' +
                arity + ',' +
                scopeNames + ',' +
                hasMultipleArgsHead + ',' +
                BlockBody.asArgumentType(argsNodeId) + ',' +
                !(inspector.hasClosure() || inspector.hasScopeAwareMethods());

        method.loadThis();
        method.loadThreadContext();

        if (inheritedBlockBodyCount < AbstractScript.NUMBERED_BLOCKBODY_COUNT) {
            method.method.ldc(descriptor);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockBody19" + inheritedBlockBodyCount, sig(BlockBody.class, ThreadContext.class, String.class));
        } else {
            method.method.pushInt(inheritedBlockBodyCount);
            method.method.ldc(descriptor);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockBody19", sig(BlockBody.class, ThreadContext.class, int.class, String.class));
        }

        inheritedBlockBodyCount++;
    }

    public void cacheSpecialClosure(BaseBodyCompiler method, String closureMethod) {
        method.loadThis();
        method.loadRuntime();

        if (inheritedBlockCallbackCount < AbstractScript.NUMBERED_BLOCKCALLBACK_COUNT) {
            method.method.ldc(closureMethod);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockCallback" + inheritedBlockCallbackCount, sig(CompiledBlockCallback.class, Ruby.class, String.class));
        } else {
            method.method.pushInt(inheritedBlockCallbackCount);
            method.method.ldc(closureMethod);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockCallback", sig(CompiledBlockCallback.class, Ruby.class, int.class, String.class));
        }

        inheritedBlockCallbackCount++;
    }

    public void cacheMethod(BaseBodyCompiler method, String methodName) {
        method.loadThis();
        method.loadThreadContext();
        method.loadSelf();

        if (inheritedMethodCount < AbstractScript.NUMBERED_METHOD_COUNT) {
            method.method.ldc(methodName);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getMethod" + inheritedMethodCount, sig(DynamicMethod.class, ThreadContext.class, IRubyObject.class, String.class));
        } else {
            method.method.pushInt(inheritedMethodCount);
            method.method.ldc(methodName);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getMethod", sig(DynamicMethod.class, ThreadContext.class, IRubyObject.class, int.class, String.class));
        }

        inheritedMethodCount++;
    }

    public void cacheMethod(BaseBodyCompiler method, String methodName, int receiverLocal) {
        method.loadThis();
        method.loadThreadContext();
        method.method.aload(receiverLocal);

        if (inheritedMethodCount < AbstractScript.NUMBERED_METHOD_COUNT) {
            method.method.ldc(methodName);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getMethod" + inheritedMethodCount, sig(DynamicMethod.class, ThreadContext.class, IRubyObject.class, String.class));
        } else {
            method.method.pushInt(inheritedMethodCount);
            method.method.ldc(methodName);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getMethod", sig(DynamicMethod.class, ThreadContext.class, IRubyObject.class, int.class, String.class));
        }

        inheritedMethodCount++;
    }

    public void finish() {
        SkinnyMethodAdapter initMethod = scriptCompiler.getInitMethod();

        // generate call sites initialization code
        int size = callSiteList.size();
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);
            
            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.dup();
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initCallSites", sig(void.class, int.class));
            initMethod.getfield(p(AbstractScript.RuntimeCache.class), "callSites", ci(CallSite[].class));
            
            for (int i = size - 1; i >= 0; i--) {
                String name = callSiteList.get(i);
                CallType callType = callTypeList.get(i);

                initMethod.pushInt(i);
                if (callType.equals(CallType.NORMAL)) {
                    initMethod.ldc(name);
                    initMethod.invokestatic(scriptCompiler.getClassname(), "setCallSite", sig(CallSite[].class, params(CallSite[].class, int.class, String.class)));
                } else if (callType.equals(CallType.FUNCTIONAL)) {
                    initMethod.ldc(name);
                    initMethod.invokestatic(scriptCompiler.getClassname(), "setFunctionalCallSite", sig(CallSite[].class, params(CallSite[].class, int.class, String.class)));
                } else if (callType.equals(CallType.VARIABLE)) {
                    initMethod.ldc(name);
                    initMethod.invokestatic(scriptCompiler.getClassname(), "setVariableCallSite", sig(CallSite[].class, params(CallSite[].class, int.class, String.class)));
                } else if (callType.equals(CallType.SUPER)) {
                    initMethod.invokestatic(scriptCompiler.getClassname(), "setSuperCallSite", sig(CallSite[].class, params(CallSite[].class, int.class)));
                }
            }

            initMethod.pop();
        }

        size = scopeCount;
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);

            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initScopes", sig(void.class, params(int.class)));
        }

        // generate symbols initialization code
        size = inheritedSymbolCount;
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);

            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initSymbols", sig(void.class, params(int.class)));
        }

        // generate fixnums initialization code
        size = inheritedFixnumCount;
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);

            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initFixnums", sig(void.class, params(int.class)));
        }

        // generate constants initialization code
        size = inheritedConstantCount;
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);

            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initConstants", sig(void.class, params(int.class)));
        }

        // generate regexps initialization code
        size = inheritedRegexpCount;
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);

            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initRegexps", sig(void.class, params(int.class)));
        }

        // generate regexps initialization code
        size = inheritedBigIntegerCount;
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);

            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initBigIntegers", sig(void.class, params(int.class)));
        }

        // generate variable readers initialization code
        size = inheritedVariableReaderCount;
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);

            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initVariableReaders", sig(void.class, params(int.class)));
        }

        // generate variable writers initialization code
        size = inheritedVariableWriterCount;
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);

            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initVariableWriters", sig(void.class, params(int.class)));
        }

        // generate block bodies initialization code
        size = inheritedBlockBodyCount;
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);

            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initBlockBodies", sig(void.class, params(int.class)));
        }

        // generate block bodies initialization code
        size = inheritedBlockCallbackCount;
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);

            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initBlockCallbacks", sig(void.class, params(int.class)));
        }

        // generate bytelists initialization code
        size = inheritedStringCount;
        if (inheritedStringCount > 0) {
            ensureRuntimeCacheInited(initMethod);

            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initStrings", sig(ByteList[].class, params(int.class)));

            for (Map.Entry<String, Integer> entry : stringIndices.entrySet()) {
                initMethod.ldc(entry.getValue());
                initMethod.ldc(entry.getKey());
                initMethod.invokestatic(p(AbstractScript.class), "createByteList", sig(ByteList[].class, ByteList[].class, int.class, String.class));
            }
            
            initMethod.pop();
        }

        // generate method cache initialization code
        size = inheritedMethodCount;
        if (size != 0) {
            ensureRuntimeCacheInited(initMethod);
            
            initMethod.aload(0);
            initMethod.getfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            initMethod.pushInt(size);
            initMethod.invokevirtual(p(AbstractScript.RuntimeCache.class), "initMethodCache", sig(void.class, params(int.class)));
        }
    }

    private void ensureRuntimeCacheInited(SkinnyMethodAdapter initMethod) {
        if (!runtimeCacheInited) {
            initMethod.aload(0);
            initMethod.newobj(p(AbstractScript.RuntimeCache.class));
            initMethod.dup();
            initMethod.invokespecial(p(AbstractScript.RuntimeCache.class), "<init>", sig(void.class));
            initMethod.putfield(p(AbstractScript.class), "runtimeCache", ci(AbstractScript.RuntimeCache.class));
            runtimeCacheInited = true;
        }
    }
}
