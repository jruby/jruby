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
import org.jcodings.Encoding;
import org.jruby.RubyBoolean;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ast.NodeType;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CompilerCallback;
import org.jruby.runtime.Helpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import org.jruby.util.ByteList;
import static org.jruby.util.CodegenUtils.*;
import org.jruby.util.JavaNameMangler;

/**
 * A CacheCompiler that uses invokedynamic as a lazy thunk for literals and other
 * invokedynamic features like SwitchPoint to produce fast (nearly free)
 * invalidatable caches for things like constant lookup.
 */
public class InvokeDynamicCacheCompiler extends InheritedCacheCompiler {
    public InvokeDynamicCacheCompiler(StandardASMCompiler scriptCompiler) {
        super(scriptCompiler);
    }

    /**
     * Cache a constant reference using invokedynamic.
     * 
     * This cache uses a java.lang.invoke.SwitchPoint as the invalidation
     * mechanism in order to avoid the cost of constantly pinging a constant
     * generation in org.jruby.Ruby. This allows a nearly free constant cache.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param constantName the name of the constant to look up
     */
    @Override
    public void cacheConstant(BaseBodyCompiler method, String constantName) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_CONSTANTS) {
            super.cacheConstant(method, constantName);
            return;
        }

        method.loadThis();
        method.loadThreadContext();
        method.method.invokedynamic(
                constantName,
                sig(IRubyObject.class, AbstractScript.class, ThreadContext.class),
                InvokeDynamicSupport.getConstantHandle(),
                method.getScopeIndex());
    }

    /**
     * Cache a constant boolean using invokedynamic.
     * 
     * This cache uses a java.lang.invoke.SwitchPoint as the invalidation
     * mechanism in order to avoid the cost of constantly pinging a constant
     * generation in org.jruby.Ruby. This allows a nearly free constant cache.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param constantName the name of the constant to look up
     */
    @Override
    public void cacheConstantBoolean(BaseBodyCompiler method, String constantName) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_CONSTANTS) {
            super.cacheConstantBoolean(method, constantName);
            return;
        }

        method.loadThis();
        method.loadThreadContext();
        method.method.invokedynamic(
                constantName,
                sig(boolean.class, AbstractScript.class, ThreadContext.class),
                InvokeDynamicSupport.getConstantBooleanHandle(),
                method.getScopeIndex());
    }

    /**
     * This doesn't get used, since it's only used from cacheRegexp in superclass,
     * and that is completely bound via invokedynamic now. Implemented here and in
     * InvokeDynamicSupport for consistency.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param contents the contents of the bytelist to cache
     */
    @Override
    public void cacheByteList(BaseBodyCompiler method, ByteList contents) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_LITERALS) {
            super.cacheByteList(method, contents);
            return;
        }
        
        String asString = Helpers.rawBytesToString(contents.bytes());
        String encodingName = new String(contents.getEncoding().getName());
        
        method.method.invokedynamic(
                "getByteList",
                sig(ByteList.class),
                InvokeDynamicSupport.getByteListHandle(),
                asString,
                encodingName);
    }

    /**
     * Cache the __ENCODING__ keyword using invokedynamic.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param encoding the encoding for this script
     */
    @Override
    public void cacheRubyEncoding(BaseBodyCompiler method, Encoding encoding) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_LITERALS) {
            super.cacheRubyEncoding(method, encoding);
            return;
        }
        
        String encodingName = new String(encoding.getName());
        
        method.loadThreadContext();
        
        method.method.invokedynamic(
                "getEncoding",
                sig(RubyEncoding.class, ThreadContext.class),
                InvokeDynamicSupport.getEncodingHandle(),
                encodingName);
    }

    /**
     * Cache a closure body (BlockBody) using invokedynamic.
     */
    @Override
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
        
        method.method.invokedynamic(
                "getBlockBody",
                sig(BlockBody.class, Object.class, ThreadContext.class, StaticScope.class),
                InvokeDynamicSupport.getBlockBodyHandle(),
                descriptor);

        return scopeIndex;
    }

    /**
     * Cache a closure body (BlockBody) for 1.9 mode using invokedynamic.
     */
    @Override
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
        
        method.method.invokedynamic(
                "getBlockBody19",
                sig(BlockBody.class, Object.class, ThreadContext.class, StaticScope.class),
                InvokeDynamicSupport.getBlockBody19Handle(),
                descriptor);

        return scopeIndex;
    }

    /**
     * Cache a Regexp literal using invokedynamic.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param pattern the contents of the bytelist for the regexp pattern
     * @param options the regexp options
     */
    @Override
    public void cacheRegexp(BaseBodyCompiler method, ByteList pattern, int options) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_LITERALS) {
            super.cacheRegexp(method, pattern, options);
            return;
        }
        
        String asString = Helpers.rawBytesToString(pattern.bytes());
        String encodingName = new String(pattern.getEncoding().getName());
        
        method.loadThreadContext();
        
        method.method.invokedynamic(
                "getRegexp",
                sig(RubyRegexp.class, ThreadContext.class),
                InvokeDynamicSupport.getRegexpHandle(),
                asString,
                encodingName,
                options);
    }

    /**
     * Cache a Fixnum literal using invokedynamic.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param value the value of the Fixnum
     */
    @Override
    public void cacheFixnum(BaseBodyCompiler method, long value) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_LITERALS) {
            super.cacheFixnum(method, value);
            return;
        }
        
        method.loadThreadContext();
        
        method.method.invokedynamic(
                "getFixnum",
                sig(RubyFixnum.class, ThreadContext.class),
                InvokeDynamicSupport.getFixnumHandle(),
                value);
    }

    /**
     * Cache a Float literal using invokedynamic.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param value the value of the Float
     */
    @Override
    public void cacheFloat(BaseBodyCompiler method, double value) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_LITERALS) {
            super.cacheFloat(method, value);
            return;
        }
        
        method.loadThreadContext();
        
        method.method.invokedynamic(
                "getFloat",
                sig(RubyFloat.class, ThreadContext.class),
                InvokeDynamicSupport.getFloatHandle(),
                value);
    }

    /**
     * Cache a StaticScope using invokedynamic.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param scope the original scope to base the new one on
     */
    @Override
    public int cacheStaticScope(BaseBodyCompiler method, StaticScope scope) {
        String scopeString = Helpers.encodeScope(scope);
        
        int index = scopeCount;
        scopeCount++;
        
        method.loadThis();
        method.loadThreadContext();
        method.loadStaticScope();
        
        method.method.invokedynamic(
                "getStaticScope",
                sig(StaticScope.class, AbstractScript.class, ThreadContext.class, StaticScope.class),
                InvokeDynamicSupport.getStaticScopeHandle(),
                scopeString,
                index);
        
        return index;
    }
    
    public void loadStaticScope(BaseBodyCompiler method, int index) {
        method.loadThis();
        
        method.method.invokedynamic(
                "getStaticScope",
                sig(StaticScope.class, AbstractScript.class),
                InvokeDynamicSupport.getLoadStaticScopeHandle(),
                index);
    }
    
    /**
     * Cache a CallSite object using invokedynamic.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param name the method name the call site invokes, or null for "super"
     * @param callType the type of call
     */
    @Override
    public void cacheCallSite(BaseBodyCompiler method, String name, CallType callType) {
        char callTypeChar = 0;
        
        switch (callType) {
            case NORMAL:
                callTypeChar = 'N';
                break;
            case FUNCTIONAL:
                callTypeChar = 'F';
                break;
            case VARIABLE:
                callTypeChar = 'V';
                break;
            case SUPER:
                callTypeChar = 'S';
                break;
        }
        
        if (name == null) name = "super";
        
        method.method.invokedynamic(
                "getCallSite",
                sig(CallSite.class),
                InvokeDynamicSupport.getCallSiteHandle(),
                name,
                callTypeChar);
    }

    /**
     * Cache a String literal using invokedynamic.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param contents the contents of the bytelist for the String
     * @param codeRange the code range for the String
     */
    @Override
    public void cacheString(BaseBodyCompiler method, ByteList contents, int codeRange) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_LITERALS) {
            super.cacheString(method, contents, codeRange);
            return;
        }
        
        String asString = Helpers.rawBytesToString(contents.bytes());
        String encodingName = new String(contents.getEncoding().getName());
        
        method.loadThreadContext();
        
        method.method.invokedynamic(
                "getString",
                sig(RubyString.class, ThreadContext.class),
                InvokeDynamicSupport.getStringHandle(),
                asString,
                encodingName,
                codeRange);
    }

    /**
     * Cache a BigInteger using invokedynamic. Used for Bignum construction
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param bigint the BigInteger to cache
     */
    @Override
    public void cacheBigInteger(BaseBodyCompiler method, BigInteger bigint) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_LITERALS) {
            super.cacheBigInteger(method, bigint);
            return;
        }
        
        String asString = bigint.toString(16);
        
        method.method.invokedynamic(
                "getBigInteger",
                sig(BigInteger.class),
                InvokeDynamicSupport.getBigIntegerHandle(),
                asString);
    }
    
    /**
     * Cache the symbol for the given string using invokedynamic.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param symbol the string of the Symbol to cache
     */
    public void cacheSymbol(BaseBodyCompiler method, String symbol) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_LITERALS) {
            super.cacheSymbol(method, symbol);
            return;
        }
        
        method.loadThreadContext();
        
        method.method.invokedynamic(
                "getSymbol",
                sig(RubySymbol.class, ThreadContext.class),
                InvokeDynamicSupport.getSymbolHandle(),
                symbol);
    }

    public void cachedGetVariable(BaseBodyCompiler method, String name) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_IVARS) {
            super.cachedGetVariable(method, name);
            return;
        }
        
        method.loadSelf();
        
        method.method.invokedynamic(
                "get:" + name,
                sig(IRubyObject.class, IRubyObject.class),
                InvokeDynamicSupport.getVariableHandle(),
                method.getScriptCompiler().getSourcename(),
                method.getLastLine() + 1
                );
    }

    public void cachedSetVariable(BaseBodyCompiler method, String name, CompilerCallback valueCallback) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_IVARS) {
            super.cachedSetVariable(method, name, valueCallback);
            return;
        }
        
        method.loadSelf();
        valueCallback.call(method);
        
        method.method.invokedynamic(
                "set:" + name,
                sig(IRubyObject.class, IRubyObject.class, IRubyObject.class),
                InvokeDynamicSupport.getVariableHandle(),
                method.getScriptCompiler().getSourcename(),
                method.getLastLine() + 1
        );
    }
    
    public void cacheGlobal(BaseBodyCompiler method, String globalName) {
        method.loadThreadContext();
        method.method.invokedynamic(
                "get:" + JavaNameMangler.mangleMethodName(globalName),
                sig(IRubyObject.class, ThreadContext.class),
                InvokeDynamicSupport.getGlobalHandle(),
                method.getScriptCompiler().getSourcename(), 
                method.getLastLine() + 1);
    }
    
    public void cacheGlobalBoolean(BaseBodyCompiler method, String globalName) {
        method.loadThreadContext();
        method.method.invokedynamic(
                "getBoolean:" + JavaNameMangler.mangleMethodName(globalName),
                sig(boolean.class, ThreadContext.class),
                InvokeDynamicSupport.getGlobalBooleanHandle(),
                method.getScriptCompiler().getSourcename(), 
                method.getLastLine() + 1);
    }
    
    public void cacheBoolean(BaseBodyCompiler method, boolean tru) {
        method.loadThreadContext();
        method.method.invokedynamic(
                "loadBoolean:" + tru,
                sig(RubyBoolean.class, ThreadContext.class),
                InvokeDynamicSupport.getLoadBooleanHandle());
    }
}
