package org.jruby.compiler.ir;

// Placeholder class for method address
public class MethAddr extends Operand {
    final public String _name;

    public MethAddr(String name) {
        _name = name;
    }
}
