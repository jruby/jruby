package org.jruby.ir.instructions;

import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.Operation;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyHash;
import org.jruby.RubySymbol;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;

public class ReceiveKeywordArgInstr extends ReceiveArgBase implements FixedArityInstr {
    public final String argName;
    public final int numUsedArgs;

    public ReceiveKeywordArgInstr(Variable result, String argName, int numUsedArgs) {
        super(Operation.RECV_KW_ARG, result, -1);
        this.argName = argName;
        this.numUsedArgs = numUsedArgs;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new Fixnum(numUsedArgs), new StringLiteral(argName) };
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + numUsedArgs + ", " + argName + ")";
    }

    @Override
    public IRubyObject receiveArg(ThreadContext context, int kwArgHashCount, IRubyObject[] args) {
        if (kwArgHashCount == 0 || numUsedArgs == args.length) {
            return UndefinedValue.UNDEFINED;
        } else {
            RubyHash lastArg = (RubyHash)args[args.length - 1];
            // If the key exists in the hash, delete and return it.
            RubySymbol argSym = context.getRuntime().newSymbol(argName);
            if (lastArg.fastARef(argSym) != null) {
                // SSS FIXME: Can we use an internal delete here?
                return lastArg.delete(context, argSym, Block.NULL_BLOCK);
            } else {
                return UndefinedValue.UNDEFINED;
            }
        }
    }
}
