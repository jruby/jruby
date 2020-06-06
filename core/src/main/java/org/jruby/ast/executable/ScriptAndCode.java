package org.jruby.ast.executable;

import java.util.Arrays;

/**
 * Represents an executable Script object and the bytecode that goes with it.
 */
public class ScriptAndCode {
    private final byte[] bytecode;
    private final Script script;

    public ScriptAndCode(byte[] bytecode, Script script) {
        this.bytecode = bytecode;
        this.script = script;
    }

    public byte[] bytecode() {
        return Arrays.copyOf(bytecode, bytecode.length);
    }

    public Script script() {
        return script;
    }
}
