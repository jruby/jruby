/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler;

import java.math.BigInteger;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.runtime.CallType;

/**
 *
 * @author headius
 */
public interface CacheCompiler {
    public void cacheCallSite(SkinnyMethodAdapter method, String name, CallType callType);
    
    public void cacheByteList(SkinnyMethodAdapter method, String contents);
    
    public void cacheSymbol(SkinnyMethodAdapter method, String symbol);
    
    public void cacheBigInteger(SkinnyMethodAdapter method, BigInteger bigint);
    
    public void cacheClosure(SkinnyMethodAdapter method, String closureMethod);
}
