/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler;

import java.math.BigInteger;
import org.jruby.ast.NodeType;
import org.jruby.compiler.impl.AbstractMethodCompiler;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;

/**
 *
 * @author headius
 */
public interface CacheCompiler {
    public void cacheCallSite(AbstractMethodCompiler method, String name, CallType callType);
    
    public void cacheByteList(AbstractMethodCompiler method, String contents);
    
    public void cacheSymbol(AbstractMethodCompiler method, String symbol);
    
    public void cacheFixnum(AbstractMethodCompiler method, long value);
    
    public void cacheBigInteger(AbstractMethodCompiler method, BigInteger bigint);
    
    public void cacheClosure(AbstractMethodCompiler method, String closureMethod, int arity, StaticScope scope, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector);
    
    public void cacheClosureOld(AbstractMethodCompiler method, String closureMethod);

    public void finish();
}
