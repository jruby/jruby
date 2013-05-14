/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.compiler.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
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
import org.jruby.compiler.ArrayCallback;
import org.jruby.compiler.CacheCompiler;
import org.jruby.compiler.CompilerCallback;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Helpers;
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
    Map<String, String> stringEncToString = new HashMap<String, String>();
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

    public int reserveStaticScope() {
        int index = scopeCount;
        scopeCount++;

        return index;
    }

    public int cacheStaticScope(BaseBodyCompiler method, StaticScope scope) {
        String scopeString = Helpers.encodeScope(scope);
        
        int index = scopeCount;
        scopeCount++;

        // retrieve scope from scopes array
        method.loadThis();
        method.loadThreadContext();
        method.loadStaticScope();
        method.method.ldc(scopeString);
        if (index < AbstractScript.NUMBERED_SCOPE_COUNT) {
            // use numbered access method
            method.method.invokevirtual(scriptCompiler.getClassname(), "getScope" + index, sig(StaticScope.class, ThreadContext.class, StaticScope.class, String.class));
        } else {
            method.method.pushInt(index);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getScope", sig(StaticScope.class, ThreadContext.class, StaticScope.class, String.class, int.class));
        }

        return index;
    }
    
    public void loadStaticScope(BaseBodyCompiler method, int index) {
        method.loadThis();

        if (scopeCount < AbstractScript.NUMBERED_SCOPE_COUNT) {
            // use numbered access method
            method.method.invokevirtual(scriptCompiler.getClassname(), "getScope" + index, sig(StaticScope.class));
        } else {
            method.method.pushInt(index);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getScope", sig(StaticScope.class, int.class));
        }
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
        method.loadThreadContext();
        if (index < AbstractScript.NUMBERED_SYMBOL_COUNT) {
            method.method.ldc(symbol);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getSymbol" + index, sig(RubySymbol.class, ThreadContext.class, String.class));
        } else {
            method.method.ldc(index.intValue());
            method.method.ldc(symbol);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getSymbol", sig(RubySymbol.class, ThreadContext.class, int.class, String.class));
        }
    }

    public void cacheRegexp(BaseBodyCompiler method, ByteList pattern, int options) {

        method.loadThis();
        method.loadThreadContext();
        int index = inheritedRegexpCount++;
        if (index < AbstractScript.NUMBERED_REGEXP_COUNT) {
            cacheByteList(method, pattern);
            method.method.ldc(options);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getRegexp" + index, sig(RubyRegexp.class, ThreadContext.class, ByteList.class, int.class));
        } else {
            method.method.pushInt(index);
            cacheByteList(method, pattern);
            method.method.ldc(options);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getRegexp", sig(RubyRegexp.class, ThreadContext.class, int.class, ByteList.class, int.class));
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

    public void cacheDRegexp19(BaseBodyCompiler method, ArrayCallback arrayCallback, Object[] sourceArray, int options) {
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
        method.loadRuntime();
        method.createObjectArray(sourceArray, arrayCallback);
        method.method.ldc(options);
        method.method.invokestatic(p(RubyRegexp.class), "newDRegexpEmbedded19", sig(RubyRegexp.class, params(Ruby.class, IRubyObject[].class, int.class))); //[reg]
        method.method.invokevirtual(p(RuntimeCache.class), "cacheRegexp", sig(RubyRegexp.class, int.class, RubyRegexp.class));

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
            method.loadThreadContext();
            if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                if (index < AbstractScript.NUMBERED_FIXNUM_COUNT) {
                    method.method.pushInt((int)value);
                    method.method.invokevirtual(scriptCompiler.getClassname(), "getFixnum" + index, sig(RubyFixnum.class, ThreadContext.class, int.class));
                } else {
                    method.method.pushInt(index.intValue());
                    method.method.pushInt((int)value);
                    method.method.invokevirtual(scriptCompiler.getClassname(), "getFixnum", sig(RubyFixnum.class, ThreadContext.class, int.class, int.class));
                }
            } else {
                method.method.pushInt(index.intValue());
                method.method.ldc(value);
                method.method.invokevirtual(scriptCompiler.getClassname(), "getFixnum", sig(RubyFixnum.class, ThreadContext.class, int.class, long.class));
            }
        }
    }
    
    public void cacheFloat(BaseBodyCompiler method, double value) {
        Integer index = Integer.valueOf(inheritedFloatCount++);
        floatIndices.put(value, index);

        method.loadThis();
        method.loadThreadContext();
        if (index < AbstractScript.NUMBERED_FLOAT_COUNT) {
            method.method.ldc(value);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getFloat" + index, sig(RubyFloat.class, ThreadContext.class, double.class));
        } else {
            method.method.pushInt(index.intValue());
            method.method.ldc(value);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getFloat", sig(RubyFloat.class, ThreadContext.class, int.class, double.class));
        }
    }

    public void cacheConstant(BaseBodyCompiler method, String constantName) {
        method.loadThis();
        method.loadThreadContext();
        method.loadStaticScope();
        method.method.ldc(constantName);
        if (inheritedConstantCount < AbstractScript.NUMBERED_CONSTANT_COUNT) {
            method.method.invokevirtual(scriptCompiler.getClassname(), "getConstant" + inheritedConstantCount, sig(IRubyObject.class, ThreadContext.class, StaticScope.class, String.class));
        } else {
            method.method.pushInt(inheritedConstantCount);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getConstant", sig(IRubyObject.class, ThreadContext.class, StaticScope.class, String.class, int.class));
        }

        inheritedConstantCount++;
    }

    public void cacheConstantBoolean(BaseBodyCompiler method, String constantName) {
        cacheConstant(method, constantName);
        method.method.invokeinterface(p(IRubyObject.class), "isTrue", sig(boolean.class));
    }

    public void cacheConstantDefined(BaseBodyCompiler method, String constantName) {
        method.loadThis();
        method.loadThreadContext();
        method.loadStaticScope();
        method.method.ldc(constantName);
        if (inheritedConstantCount < AbstractScript.NUMBERED_CONSTANT_COUNT) {
            method.method.invokevirtual(scriptCompiler.getClassname(), "getConstantDefined" + inheritedConstantCount, sig(IRubyObject.class, ThreadContext.class, StaticScope.class, String.class));
        } else {
            method.method.pushInt(inheritedConstantCount);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getConstantDefined", sig(IRubyObject.class, ThreadContext.class, StaticScope.class, String.class, int.class));
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
        String asString = Helpers.rawBytesToString(contents.bytes());
        String key = asString + contents.getEncoding();
        
        Integer index = stringIndices.get(key);
        if (index == null) {
            index = Integer.valueOf(inheritedStringCount++);
            stringEncToString.put(key, asString);
            stringIndices.put(key, index);
            stringEncodings.put(key, cacheEncodingInternal(contents.getEncoding()));
        }

        method.loadThis();
        method.loadThreadContext();
        if (index < AbstractScript.NUMBERED_STRING_COUNT) {
            method.method.pushInt(codeRange);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getString" + index, sig(RubyString.class, ThreadContext.class, int.class));
        } else {
            method.method.pushInt(index.intValue());
            method.method.pushInt(codeRange);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getString", sig(RubyString.class, ThreadContext.class, int.class, int.class));
        }
    }

    public void cacheByteList(BaseBodyCompiler method, ByteList contents) {
        String asString = Helpers.rawBytesToString(contents.bytes());
        String key = asString + contents.getEncoding();

        Integer index = stringIndices.get(key);
        if (index == null) {
            index = Integer.valueOf(inheritedStringCount++);
            stringEncToString.put(key, asString);
            stringIndices.put(key, index);
            stringEncodings.put(key, cacheEncodingInternal(contents.getEncoding()));
        }

        method.loadThis();
        if (index < AbstractScript.NUMBERED_STRING_COUNT) {
            method.method.invokevirtual(scriptCompiler.getClassname(), "getByteList" + index, sig(ByteList.class));
        } else {
            method.method.pushInt(index.intValue());
            method.method.invokevirtual(scriptCompiler.getClassname(), "getByteList", sig(ByteList.class, int.class));
        }
    }

    public void cacheRubyEncoding(BaseBodyCompiler method, Encoding encoding) {
        // split into three methods since ByteList depends on two parts in different places
        cacheEncoding(method, encoding);
        createRubyEncoding(method);
    }
    
    public int cacheEncoding(BaseBodyCompiler method, Encoding encoding) {
        int index = cacheEncodingInternal(encoding);
        loadEncoding(method.method, index);
        return index;
    }

    private int cacheEncodingInternal(Encoding encoding) {
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
        int index = inheritedBigIntegerCount++;
        if (index < AbstractScript.NUMBERED_BIGINTEGER_COUNT) {
            method.method.ldc(bigint.toString(16));
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBigInteger" + index, sig(BigInteger.class, String.class));
        } else {
            method.method.pushInt(index);
            method.method.ldc(bigint.toString(16));
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBigInteger", sig(BigInteger.class, int.class, String.class));
        }
    }

    public void cachedGetVariable(BaseBodyCompiler method, String name) {
        method.loadThis();
        method.loadThreadContext();
        int index = inheritedVariableReaderCount++;
        if (index < AbstractScript.NUMBERED_VARIABLEREADER_COUNT) {
            method.method.ldc(name);
            method.loadSelf();
            method.method.invokevirtual(scriptCompiler.getClassname(), "getVariable" + index, sig(IRubyObject.class, ThreadContext.class, String.class, IRubyObject.class));
        } else {
            method.method.pushInt(index);
            method.method.ldc(name);
            method.loadSelf();
            method.method.invokevirtual(scriptCompiler.getClassname(), "getVariable", sig(IRubyObject.class, ThreadContext.class, int.class, String.class, IRubyObject.class));
        }
    }

    public void cachedGetVariableDefined(BaseBodyCompiler method, String name) {
        method.loadThis();
        method.loadThreadContext();
        int index = inheritedVariableReaderCount++;
        if (index < AbstractScript.NUMBERED_VARIABLEREADER_COUNT) {
            method.method.ldc(name);
            method.loadSelf();
            method.method.invokevirtual(scriptCompiler.getClassname(), "getVariableDefined" + index, sig(IRubyObject.class, ThreadContext.class, String.class, IRubyObject.class));
        } else {
            method.method.pushInt(index);
            method.method.ldc(name);
            method.loadSelf();
            method.method.invokevirtual(scriptCompiler.getClassname(), "getVariableDefined", sig(IRubyObject.class, ThreadContext.class, int.class, String.class, IRubyObject.class));
        }
    }

    public void cachedSetVariable(BaseBodyCompiler method, String name, CompilerCallback valueCallback) {
        method.loadThis();
        int index = inheritedVariableWriterCount++;
        if (index < AbstractScript.NUMBERED_VARIABLEWRITER_COUNT) {
            method.method.ldc(name);
            method.loadSelf();
            valueCallback.call(method);
            method.method.invokevirtual(scriptCompiler.getClassname(), "setVariable" + index, sig(IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));
        } else {
            method.method.pushInt(index);
            method.method.ldc(name);
            method.loadSelf();
            valueCallback.call(method);
            method.method.invokevirtual(scriptCompiler.getClassname(), "setVariable", sig(IRubyObject.class, int.class, String.class, IRubyObject.class, IRubyObject.class));
        }
    }

    public int cacheClosure(BaseBodyCompiler method, String closureMethod, int arity, StaticScope scope, String file, int line, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector) {
        String descriptor = Helpers.buildBlockDescriptor(
                closureMethod,
                arity,
                file,
                line,
                hasMultipleArgsHead,
                argsNodeId,
                inspector);

        method.loadThis();
        method.loadThreadContext();
        int scopeIndex = cacheStaticScope(method, scope);

        if (inheritedBlockBodyCount < AbstractScript.NUMBERED_BLOCKBODY_COUNT) {
            method.method.ldc(descriptor);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockBody" + inheritedBlockBodyCount, sig(BlockBody.class, ThreadContext.class, StaticScope.class, String.class));
        } else {
            method.method.pushInt(inheritedBlockBodyCount);
            method.method.ldc(descriptor);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockBody", sig(BlockBody.class, ThreadContext.class, StaticScope.class, int.class, String.class));
        }

        inheritedBlockBodyCount++;

        return scopeIndex;
    }

    public int cacheClosure19(BaseBodyCompiler method, String closureMethod, int arity, StaticScope scope, String file, int line, boolean hasMultipleArgsHead, NodeType argsNodeId, String parameterList, ASTInspector inspector) {
        String descriptor = Helpers.buildBlockDescriptor19(
                closureMethod,
                arity,
                file,
                line,
                hasMultipleArgsHead,
                argsNodeId,
                parameterList,
                inspector);

        method.loadThis();
        method.loadThreadContext();
        int scopeIndex = cacheStaticScope(method, scope);

        if (inheritedBlockBodyCount < AbstractScript.NUMBERED_BLOCKBODY_COUNT) {
            method.method.ldc(descriptor);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockBody19" + inheritedBlockBodyCount, sig(BlockBody.class, ThreadContext.class, StaticScope.class, String.class));
        } else {
            method.method.pushInt(inheritedBlockBodyCount);
            method.method.ldc(descriptor);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockBody19", sig(BlockBody.class, ThreadContext.class, StaticScope.class, int.class, String.class));
        }

        inheritedBlockBodyCount++;

        return scopeIndex;
    }

    public void cacheSpecialClosure(BaseBodyCompiler method, String closureMethod) {
        method.loadThis();
        
        if (inheritedBlockCallbackCount < AbstractScript.NUMBERED_BLOCKCALLBACK_COUNT) {
            method.method.ldc(closureMethod);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockCallback" + inheritedBlockCallbackCount, sig(CompiledBlockCallback.class, String.class));
        } else {
            method.method.pushInt(inheritedBlockCallbackCount);
            method.method.ldc(closureMethod);
            method.method.invokevirtual(scriptCompiler.getClassname(), "getBlockCallback", sig(CompiledBlockCallback.class, int.class, String.class));
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
    
    public void cacheGlobal(BaseBodyCompiler method, String globalName) {
        method.loadRuntime();
        method.method.ldc(globalName);
        method.invokeUtilityMethod("getGlobalVariable", sig(IRubyObject.class, Ruby.class, String.class));
    }
    
    public void cacheGlobalBoolean(BaseBodyCompiler method, String globalName) {
        cacheGlobal(method, globalName);
        method.method.invokeinterface(p(IRubyObject.class), "isTrue", sig(boolean.class));
    }
    
    public void cacheBoolean(BaseBodyCompiler method, boolean tru) {
        // no savings to cache under non-indy
        method.loadRuntime();
        method.invokeRuby(tru ? "getTrue" : "getFalse", sig(RubyBoolean.class));
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
                    String key = entry.getKey();
                    initMethod.ldc(stringEncToString.get(key));
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
