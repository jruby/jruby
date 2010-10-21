package org.jruby.compiler.ir.operands;

public class KeyValuePair {
    private Operand key;
    private Operand value;

    public KeyValuePair(Operand key, Operand value) {
        this.key = key;
        this.value = value;
    }

    public Operand getKey() {
        return key;
    }

    public Operand getValue() {
        return value;
    }

    public void setKey(Operand key) {
        this.key = key;
    }

    public void setValue(Operand value) {
        this.value = value;
    }
}
