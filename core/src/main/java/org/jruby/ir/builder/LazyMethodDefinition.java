package org.jruby.ir.builder;

import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;

import java.util.List;

public interface LazyMethodDefinition<U, V, W, X, Y, Z> {
    int getEndLine();
    List<String> getMethodData();
    V getMethod();
    U getMethodBody();
    IRBuilder<U, V, W, X, Y, Z> getBuilder(IRManager manager, IRMethod methodScope);
}
