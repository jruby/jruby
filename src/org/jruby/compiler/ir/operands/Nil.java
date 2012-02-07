package org.jruby.compiler.ir.operands;

// Records the nil object

import org.jruby.compiler.ir.targets.JVM;
import org.jruby.runtime.ThreadContext;

public class Nil extends ImmutableLiteral {
    public static final Nil NIL = new Nil();

    protected Nil() {
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return context.nil;
    }
    
    @Override
    public String toString() { 
        return "nil";
    }

    public void compile(JVM jvm) {
        jvm.method().pushNil();
    }
}
