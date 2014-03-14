package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.Operation;
import org.jruby.ir.runtime.IRRuntimeHelpers;
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
    public final int required;

    public ReceiveKeywordArgInstr(Variable result, String argName, int required) {
        super(Operation.RECV_KW_ARG, result, -1);
        this.argName = argName;
        this.required = required;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new Fixnum(required), new StringLiteral(argName) };
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + required + ", " + argName + ")";
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.RECEIVES_KEYWORD_ARGS);
        return true;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReceiveKeywordArgInstr(ii.getRenamedVariable(result), argName, required);
    }

    @Override
    public IRubyObject receiveArg(ThreadContext context, IRubyObject[] args, boolean acceptsKeywordArgument) {
        RubyHash keywordArguments = IRRuntimeHelpers.extractKwargsHash(args, required, acceptsKeywordArgument);

        if (keywordArguments == null) return UndefinedValue.UNDEFINED;

        RubySymbol keywordName = context.getRuntime().newSymbol(argName);

        if (keywordArguments.fastARef(keywordName) == null) return UndefinedValue.UNDEFINED;

        // SSS FIXME: Can we use an internal delete here?
        // Enebo FIXME: Delete seems wrong if we are doing this for duplication purposes.
        return keywordArguments.delete(context, keywordName, Block.NULL_BLOCK);
    }
}
