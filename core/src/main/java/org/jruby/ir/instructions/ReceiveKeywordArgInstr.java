package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.Operation;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyHash;
import org.jruby.RubySymbol;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

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
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.RECEIVES_KEYWORD_ARGS);
        return true;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReceiveKeywordArgInstr(ii.getRenamedVariable(result), argName, numUsedArgs);
    }

    @Override
    public IRubyObject receiveArg(ThreadContext context, IRubyObject[] args, boolean keywordArgumentSupplied) {
        if (!keywordArgumentSupplied) return UndefinedValue.UNDEFINED;

        RubyHash keywordArguments = (RubyHash)args[args.length - 1];
        RubySymbol keywordName = context.getRuntime().newSymbol(argName);

        if (keywordArguments.fastARef(keywordName) == null) return UndefinedValue.UNDEFINED;

        // SSS FIXME: Can we use an internal delete here?
        // Enebo FIXME: Delete seems wrong if we are doing this for duplication purposes.
        return keywordArguments.delete(context, keywordName, Block.NULL_BLOCK);
    }
}
