/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.instructions;

/**
 * This instruction will be smart enough to potentially provide a specialized
 * version of itself.  Specialized instructions must still conform to the same
 * semantics of an ordinary instruction, but it can elect to provide a more
 * efficient implementation of itself.
 */
public interface Specializeable {
    /**
     * Interpreter can ask the instruction if it knows how to make a more
     * efficient instruction for direct interpretation.
     *
     * @return itself or more efficient but semantically equivalent instr
     */
    public CallBase specializeForInterpretation();
}
