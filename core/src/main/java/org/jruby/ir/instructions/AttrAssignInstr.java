package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneArgOperandAttrAssignInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.ir.IRFlags.*;

// Instruction representing Ruby code of the form: "a[i] = 5"
// which is equivalent to: a.[]=(i,5)
public class AttrAssignInstr extends NoResultCallInstr {
    private transient CallSite functionalSite;

    public static AttrAssignInstr create(Operand obj, String attr, Operand[] args, boolean isPotentiallyRefined) {
        if (!containsArgSplat(args) && args.length == 1) {
            return new OneArgOperandAttrAssignInstr(obj, attr, args, isPotentiallyRefined);
        }

        return new AttrAssignInstr(obj, attr, args, isPotentiallyRefined);
    }

    public AttrAssignInstr(Operand obj, String attr, Operand[] args, boolean isPotentiallyRefined) {
        super(Operation.ATTR_ASSIGN, CallType.NORMAL, attr, obj, args, null, isPotentiallyRefined);
        functionalSite = getCallSiteFor(CallType.FUNCTIONAL, attr, isPotentiallyRefined);
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        // SSS FIXME: For now, forcibly require a frame for scopes
        // having attr-assign instructions. However, we can avoid this
        // by passing in the frame self explicitly to Helpers.invoke(..)
        // rather than try to get it off context.getFrameSelf()
        super.computeScopeFlags(scope);
        if (targetRequiresCallersFrame()) {
            // This can be narrowed further by filtering out cases with literals other than String and Regexp
            scope.getFlags().add(REQUIRES_FRAME);
        }
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new AttrAssignInstr(getReceiver().cloneForInlining(ii), getName(), cloneCallArgs(ii), isPotentiallyRefined());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("Instr(" + getOperation() + "): " + this);
        e.encode(getOperation());
        e.encode(getReceiver());
        e.encode(getName());
        e.encode(getCallArgs());
    }

    public static AttrAssignInstr decode(IRReaderDecoder d) {
        return create(d.decodeOperand(), d.decodeString(), d.decodeOperandArray(), d.getCurrentScope().maybeUsingRefinements());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject[] values = prepareArguments(context, self, currScope, dynamicScope, temp);

        CallSite callSite = self == object ? this.functionalSite : this.callSite;
        callSite.call(context, self, object, values);

        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.AttrAssignInstr(this);
    }
}
