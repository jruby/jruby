package org.jruby.compiler.ir;

// This interface represents the compiler target
// Example JDK6, JDK7
public interface CompilerTarget
{
    public void codegen(IR_Scope scope);
}
