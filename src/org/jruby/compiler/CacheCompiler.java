/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler;

import java.math.BigInteger;
import org.jruby.ast.NodeType;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;

/**
 *
 * @author headius
 */
public interface CacheCompiler {
    public void cacheCallSite(StandardASMCompiler.AbstractMethodCompiler method, String name, CallType callType);
    
    public void cacheByteList(StandardASMCompiler.AbstractMethodCompiler method, String contents);
    
    public void cacheSymbol(StandardASMCompiler.AbstractMethodCompiler method, String symbol);
    
    public void cacheFixnum(StandardASMCompiler.AbstractMethodCompiler method, long value);
    
    public void cacheBigInteger(StandardASMCompiler.AbstractMethodCompiler method, BigInteger bigint);
    
    public void cacheClosure(StandardASMCompiler.AbstractMethodCompiler method, String closureMethod, int arity, StaticScope scope, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector);
    
    public void cacheClosureOld(StandardASMCompiler.AbstractMethodCompiler method, String closureMethod);

    public void finish();
}
