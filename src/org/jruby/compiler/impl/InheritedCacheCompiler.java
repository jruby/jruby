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
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ast.NodeType;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CacheCompiler;
import org.jruby.compiler.CompilerCallback;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingService;
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
    Map<String, Integer> encodingIndices = new HashMap<String, Integer>();
    Map<String, Integer> stringEncodings = new HashMap<String, Integer>();
    Map<String, Integer> symbolIndices = new HashMap<String, Integer>();
    Map<Long, Integer> fixnumIndices = new HashMap<Long, Integer>();
    Map<Double, Integer> floatIndices = new HashMap<Double, Integer>();
    int inheritedSymbolCount = 0;
    int inheritedStringCount = 0;
    int inheritedEncodingCount = 0;
    int inheritedRegexpCount = 0;
    int inheritedBigIntegerCount = 0;
    int inheritedVariableReaderCount = 0;
    int inheritedVariableWriterCount = 0;
    int inheritedFixnumCount = 0;
    int inheritedFloatCount = 0;
    int inheritedConstantCount = 0;
    int inheritedBlockBodyCount = 0;
    int inheritedBlockCallbackCount = 0;
    int inheritedMethodCount = 0;

    boolean runtimeCacheInited = false;
    
    public InheritedCacheCompiler(StandardASMCompiler scriptCompiler) {
        this.scriptCompiler = scriptCompiler;
    }

    public void cacheStaticScope(BaseBodyCompiler method, StaticScope scope) {
        String scopeString = RuntimeHelpers.encodeScope(scope);

        // retrieve scope from scopes array
        method.loadThis();
        method.loadThreadContext();
        method.method.ldc(scopeString);
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
            index = Integer.valueOf(inheritedSymbolCount++);
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

    public void cacheRegexp(BaseBodyCompiler method, ByteList pattern, int options) {

        method.loadThis();
        method.loadRuntime();
        int index = inheritedRegexpCount++;
        if (index < AbstractScript.NUMBERED_REGEXP_COUNT) {
            cacheByteList(method, pattern);
            method.method.ldc(options);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getRegexp" + index, sig(RubyRegexp.class, Ruby.class, ByteList.class, int.class));
        } else {
            method.method.pushInt(index);
            cacheByteList(method, pattern);
            method.method.ldc(options);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getRegexp", sig(RubyRegexp.class, Ruby.class, int.class, ByteList.class, int.class));
        }
    }

    public void cacheDRegexp(BaseBodyCompiler method, CompilerCallback createStringCallback, int options) {
        int index = inheritedRegexpCount++;
        Label alreadyCompiled = new Label();

        method.loadThis();
        method.method.getfield(scriptCompiler.getClassname(), "runtimeCache", ci(RuntimeCache.class));
        method.method.pushInt(index);
        method.method.invokevirtual(p(RuntimeCache.class), "getRegexp", sig(RubyRegexp.class, int.class));
        method.method.dup();

        method.ifNotNull(alreadyCompiled);

        method.method.pop();
        method.loadThis();
        method.method.getfield(scriptCompiler.getClassname(), "runtimeCache", ci(RuntimeCache.class));
        method.method.pushInt(index);
        createStringCallback.call(method);
        method.method.ldc(options);
        method.method.invokevirtual(p(RuntimeCache.class), "cacheRegexp", sig(RubyRegexp.class, int.class, RubyString.class, int.class));

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
                index =  Integer.valueOf(inheritedFixnumCount++);
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
    
    public void cacheFloat(BaseBodyCompiler method, double value) {
        Integer index = Integer.valueOf(inheritedFloatCount++);
        floatIndices.put(value, index);

        method.loadThis();
        method.loadRuntime();
        if (index < AbstractScript.NUMBERED_FLOAT_COUNT) {
            method.method.ldc(value);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getFloat" + index, sig(RubyFloat.class, Ruby.class, double.class));
        } else {
            method.method.pushInt(index.intValue());
            method.method.ldc(value);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getFloat", sig(RubyFloat.class, Ruby.class, int.class, double.class));
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

    public void cacheString(BaseBodyCompiler method, ByteList contents, int codeRange) {
        String asString = RuntimeHelpers.rawBytesToString(contents.bytes());
        
        Integer index = stringIndices.get(asString);
        if (index == null) {
            index = Integer.valueOf(inheritedStringCount++);
            stringIndices.put(asString, index);
            stringEncodings.put(asString, cacheEncoding(contents.getEncoding()));
        }

        method.loadThis();
        method.loadRuntime();
        if (index < AbstractScript.NUMBERED_STRING_COUNT) {
            method.method.pushInt(codeRange);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getString" + index, sig(RubyString.class, Ruby.class, int.class));
        } else {
            method.method.pushInt(index.intValue());
            method.method.pushInt(codeRange);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getString", sig(RubyString.class, Ruby.class, int.class, int.class));
        }
    }

    public void cacheByteList(BaseBodyCompiler method, ByteList contents) {
        String asString = RuntimeHelpers.rawBytesToString(contents.bytes());

        Integer index = stringIndices.get(asString);
        if (index == null) {
            index = Integer.valueOf(inheritedStringCount++);
            stringIndices.put(asString, index);
            stringEncodings.put(asString, cacheEncoding(contents.getEncoding()));
        }

        method.loadThis();
        if (index < AbstractScript.NUMBERED_STRING_COUNT) {
            method.method.invokevirtual(scriptCompiler.getClassname(), "getByteList" + index, sig(ByteList.class));
        } else {
            method.method.pushInt(index.intValue());
            method.method.invokevirtual(scriptCompiler.getClassname(), "getByteList", sig(ByteList.class, int.class));
        }
    }

    public void cacheEncoding(BaseBodyCompiler method, Encoding encoding) {
        // split into three methods since ByteList depends on two parts in different places
        int encodingIndex = cacheEncoding(encoding);
        loadEncoding(method.method, encodingIndex);
        createRubyEncoding(method);
    }

    private int cacheEncoding(Encoding encoding) {
        String encodingName = new String(encoding.getName());

        Integer index = encodingIndices.get(encodingName);
        if (index == null) {
            index = Integer.valueOf(inheritedEncodingCount++);
            encodingIndices.put(encodingName, index);
        }
        return index;
    }

    private void loadEncoding(SkinnyMethodAdapter method, int encodingIndex) {
        method.aload(0);
        if (encodingIndex < AbstractScript.NUMBERED_ENCODING_COUNT) {
            method.invokevirtual(scriptCompiler.getClassname(), "getEncoding" + encodingIndex, sig(Encoding.class));
        } else {
            method.pushInt(encodingIndex);
            method.invokevirtual(scriptCompiler.getClassname(), "getEncoding", sig(Encoding.class, int.class));
        }
    }

    private void createRubyEncoding(BaseBodyCompiler method) {
        method.loadRuntime();
        method.invokeRuby("getEncodingService", sig(EncodingService.class));
        method.method.swap();
        method.method.invokevirtual(p(EncodingService.class), "getEncoding", sig(RubyEncoding.class, Encoding.class));
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

    public void cacheClosure(BaseBodyCompiler method, String closureMethod, int arity, StaticScope scope, String file, int line, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector) {
        String descriptor = RuntimeHelpers.buildBlockDescriptor(
                closureMethod,
                arity,
                scope,
                file,
                line,
                hasMultipleArgsHead,
                argsNodeId,
                inspector);

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

    public void cacheClosure19(BaseBodyCompiler method, String closureMethod, int arity, StaticScope scope, String file, int line, boolean hasMultipleArgsHead, NodeType argsNodeId, String parameterList, ASTInspector inspector) {
        String descriptor = RuntimeHelpers.buildBlockDescriptor19(
                closureMethod,
                arity,
                scope,
                file,
                line,
                hasMultipleArgsHead,
                argsNodeId,
                parameterList,
                inspector);

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

        // generate call sites portion of descriptor
        int callSiteListSize = callSiteList.size();
        int otherCount = scopeCount
                + inheritedSymbolCount
                + inheritedFixnumCount
                + inheritedFloatCount
                + inheritedConstantCount
                + inheritedRegexpCount
                + inheritedBigIntegerCount
                + inheritedVariableReaderCount
                + inheritedVariableWriterCount
                + inheritedBlockBodyCount
                + inheritedBlockCallbackCount
                + inheritedMethodCount
                + inheritedStringCount
                + inheritedEncodingCount;
        if (callSiteListSize + otherCount != 0) {
            ensureRuntimeCacheInited(initMethod);

            StringBuffer descriptor = new StringBuffer(callSiteListSize * 5 + 12); // rough guess of total size

            for (int i = 0; i < callSiteListSize; i++) {
                String name = callSiteList.get(i);
                CallType callType = callTypeList.get(i);
                
                if (i > 0) descriptor.append('\uFFFF');
                
                if (callType.equals(CallType.NORMAL)) {
                    descriptor.append(name).append("\uFFFFN");
                } else if (callType.equals(CallType.FUNCTIONAL)) {
                    descriptor.append(name).append("\uFFFFF");
                } else if (callType.equals(CallType.VARIABLE)) {
                    descriptor.append(name).append("\uFFFFV");
                } else if (callType.equals(CallType.SUPER)) {
                    descriptor.append("super").append("\uFFFFS");
                }
            }

            // generate "others" part of descriptor
            descriptor.append('\uFFFF');
            descriptor.append((char)scopeCount);
            descriptor.append((char)inheritedSymbolCount);
            descriptor.append((char)inheritedFixnumCount);
            descriptor.append((char)inheritedFloatCount);
            descriptor.append((char)inheritedConstantCount);
            descriptor.append((char)inheritedRegexpCount);
            descriptor.append((char)inheritedBigIntegerCount);
            descriptor.append((char)inheritedVariableReaderCount);
            descriptor.append((char)inheritedVariableWriterCount);
            descriptor.append((char)inheritedBlockBodyCount);
            descriptor.append((char)inheritedBlockCallbackCount);
            descriptor.append((char)inheritedMethodCount);
            descriptor.append((char)inheritedStringCount);
            descriptor.append((char)inheritedEncodingCount);

            // init from descriptor
            initMethod.aload(0);
            initMethod.ldc(descriptor.toString());
            initMethod.invokevirtual(p(AbstractScript.class), "initFromDescriptor", sig(void.class, String.class));
            
            if (inheritedEncodingCount > 0) {
                // init all encodings
                for (Map.Entry<String, Integer> entry : encodingIndices.entrySet()) {
                    initMethod.aload(0);
                    initMethod.ldc(entry.getValue());
                    initMethod.ldc(entry.getKey());
                    initMethod.invokevirtual(p(AbstractScript.class), "setEncoding", sig(void.class, int.class, String.class));
                }
            }

            if (inheritedStringCount > 0) {
                // init all strings
                for (Map.Entry<String, Integer> entry : stringIndices.entrySet()) {
                    initMethod.aload(0);
                    initMethod.ldc(entry.getValue());
                    initMethod.ldc(entry.getKey());
                    loadEncoding(initMethod, stringEncodings.get(entry.getKey()));
                    initMethod.invokevirtual(p(AbstractScript.class), "setByteList", sig(void.class, int.class, String.class, Encoding.class));
                }
            }
        }
    }

    private void ensureRuntimeCacheInited(SkinnyMethodAdapter initMethod) {
        if (!runtimeCacheInited) {
            initMethod.aload(0);
            initMethod.newobj(p(RuntimeCache.class));
            initMethod.dup();
            initMethod.invokespecial(p(RuntimeCache.class), "<init>", sig(void.class));
            initMethod.putfield(p(AbstractScript.class), "runtimeCache", ci(RuntimeCache.class));
            runtimeCacheInited = true;
        }
    }
}
