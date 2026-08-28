package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.RubyString;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.FrozenString;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Instruction representing Ruby code of the form: "a['str']"
 * which is equivalent to: a.[]('str'). Because a Hash receiver
 * would immediately freeze the string, we can freeze and dedup
 * the string ahead of time and call [] directly.
 */
public class ArrayDerefInstr extends OneOperandArgNoBlockCallInstr {
    private final FrozenString key;
    public static final ByteList AREF = new ByteList(new byte[] {'[', ']'});
    public static final ByteList ASET = new ByteList(new byte[] {'[', ']', '='});

    public static ArrayDerefInstr create(IRScope scope, Variable result, Operand obj, FrozenString arg0, int flags) {
        return new ArrayDerefInstr(scope, result, obj, arg0, flags);
    }

    // normal constructor
    public ArrayDerefInstr(IRScope scope, Variable result, Operand obj, FrozenString arg0, int flags) {
        super(scope, Operation.ARRAY_DEREF, CallType.FUNCTIONAL, result,
                scope.getManager().getRuntime().newSymbol(AREF), obj, new Operand[] {arg0}, flags, false);

        key = arg0;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ArrayDerefInstr(ii.getScope(), (Variable) getResult().cloneForInlining(ii),
                getReceiver().cloneForInlining(ii), key, getFlags());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("Instr(" + getOperation() + "): " + this);
        e.encode(getOperation());
        e.encode(getResult());
        e.encode(getReceiver());
        e.encode(getArg1());
        e.encode(getFlags());
    }

    public static ArrayDerefInstr decode(IRReaderDecoder d) {
        return create(d.getCurrentScope(), d.decodeVariable(), d.decodeOperand(), (FrozenString) d.decodeOperand(), d.decodeInt());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        RubyString keyStr = key.retrieve(context, self, currScope, dynamicScope, temp);

        return IRRuntimeHelpers.callOptimizedAref(context, self, object, keyStr, getCallSite());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ArrayDerefInstr(this);
    }

    public FrozenString getKey() {
        return key;
    }
}
