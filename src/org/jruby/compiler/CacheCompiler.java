/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler;

import java.math.BigInteger;
import org.jcodings.Encoding;
import org.jruby.ast.NodeType;
import org.jruby.compiler.impl.BaseBodyCompiler;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.util.ByteList;

/**
 *
 * @author headius
 */
public interface CacheCompiler {
    public void cacheCallSite(BaseBodyCompiler method, String name, CallType callType);
    
    public void cacheString(BaseBodyCompiler method, ByteList contents, int codeRange);
    
    public void cacheByteList(BaseBodyCompiler method, ByteList contents);

    public void cacheRubyEncoding(BaseBodyCompiler method, Encoding encoding);

    public int cacheEncoding(BaseBodyCompiler method, Encoding encoding);
    
    public void cacheSymbol(BaseBodyCompiler method, String symbol);
    
    public void cacheFixnum(BaseBodyCompiler method, long value);
    
    public void cacheFloat(BaseBodyCompiler method, double value);
    
    public void cacheBigInteger(BaseBodyCompiler method, BigInteger bigint);

    public void cachedGetVariable(BaseBodyCompiler method, String name);

    public void cachedGetVariableDefined(BaseBodyCompiler method, String name);

    public void cachedSetVariable(BaseBodyCompiler method, String name, CompilerCallback value);

    public void cacheRegexp(BaseBodyCompiler method, ByteList pattern, int options);

    public void cacheDRegexp(BaseBodyCompiler method, CompilerCallback createStringCallback, int options);

    public void cacheDRegexp19(BaseBodyCompiler method, ArrayCallback arrayCallback, Object[] sourceArray, int options);

    public int cacheClosure(BaseBodyCompiler method, String closureMethod, int arity, StaticScope scope, String file, int line, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector);

    public int cacheClosure19(BaseBodyCompiler method, String closureMethod, int arity, StaticScope scope, String file, int line, boolean hasMultipleArgsHead, NodeType argsNodeId, String parameterList, ASTInspector inspector);
    
    public void cacheSpecialClosure(BaseBodyCompiler method, String closureMethod);

    public void cacheConstant(BaseBodyCompiler method, String constantName);

    public void cacheConstantDefined(BaseBodyCompiler method, String constantName);

    public void cacheConstantFrom(BaseBodyCompiler method, String constantName);

    public int reserveStaticScope();

    public int cacheStaticScope(BaseBodyCompiler method, StaticScope scope);
    
    public void loadStaticScope(BaseBodyCompiler method, int index);

    public void cacheMethod(BaseBodyCompiler method, String methodName);

    public void cacheMethod(BaseBodyCompiler method, String methodName, int receiverLocal);
    
    public void cacheGlobal(BaseBodyCompiler method, String globalName);
    
    public void cacheGlobalBoolean(BaseBodyCompiler method, String globalName);
    
    public void cacheConstantBoolean(BaseBodyCompiler method, String globalName);
    
    public void cacheBoolean(BaseBodyCompiler method, boolean tru);

    public void finish();
}
