package org.jruby.ir.builder;

import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;

import java.util.List;

public interface LazyMethodDefinition<U, V, W, X, Y, Z> {
    int getEndLine();

    /**
     * Zero-based byte column where the definition starts. -1 if unknown.
     */
    default int getStartColumn() {
        return -1;
    }

    /**
     * Zero-based byte column just after the definition ends. -1 if unknown.
     */
    default int getEndColumn() {
        return -1;
    }
    List<String> getMethodData();
    V getMethod();
    U getMethodBody();
    IRBuilder<U, V, W, X, Y, Z> getBuilder(IRManager manager, IRMethod methodScope);
}
