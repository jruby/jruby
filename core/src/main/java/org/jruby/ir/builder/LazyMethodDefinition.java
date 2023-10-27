package org.jruby.ir.builder;

import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;

import java.util.List;

public interface LazyMethodDefinition<U, V, W, X, Y> {
    int getEndLine();
    List<String> getMethodData();
    V getMethod();
    U getMethodArgs();
    U getMethodBody();
    IRBuilder<U, V, W, X, Y> getBuilder(IRManager manager, IRMethod methodScope);
}
