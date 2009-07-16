package org.jruby.compiler.ir;

// This instruction receives the last argument from the argument array and removes it from the argument array.
// It is important to remove the block from the argument array so that a splat (*x) can receive the "rest" of the args
// minus the block itself.
//
// (Most likely, this will be implemented by decrementing the length counter of the argument array.)
public class RECV_BLOCK_ARG_Instr extends IR_Instr
{
    public RECV_BLOCK_ARG_Instr(Variable dest)
    {
        super(Operation.RECV_BLOCK, dest);
    }
}
