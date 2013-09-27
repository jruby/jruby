package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

// This instruction is similar to EQQInstr, except it also verifies that
// the type to EQQ with is actually a class or a module since rescue clauses
// have this requirement unlike case statements.
//
// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class RescueEQQInstr extends Instr implements ResultInstr {
    private Operand arg1;
    private Operand arg2;
    private Variable result;

    public RescueEQQInstr(Variable result, Operand v1, Operand v2) {
        super(Operation.RESCUE_EQQ);

        assert result != null: "RescueEQQInstr result is null";

        this.arg1 = v1;
        this.arg2 = v2;
        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[]{arg1, arg2};
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        arg1 = arg1.getSimplifiedOperand(valueMap, force);
        arg2 = arg2.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + arg1 + ", " + arg2 + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new RescueEQQInstr(ii.getRenamedVariable(result),
                arg1.cloneForInlining(ii), arg2.cloneForInlining(ii));
    }

    // SSS FIXME: Is this code effectively equivalent to Helpers.isJavaExceptionHandled?
    private boolean exceptionHandled(ThreadContext context, IRubyObject excType, Object excObj) {
        Ruby runtime = context.runtime;
        if (excObj instanceof IRubyObject) {
            // regular ruby exception
            if (!(excType instanceof RubyModule)) throw runtime.newTypeError("class or module required for rescue clause. Found: " + excType);
            return excType.callMethod(context, "===", (IRubyObject)excObj).isTrue();
        } else if (runtime.getException().op_ge(excType).isTrue() || runtime.getObject() == excType) {
            // convert java obj to a ruby object and try again
            return excType.callMethod(context, "===", JavaUtil.convertJavaToUsableRubyObject(runtime, excObj)).isTrue();
        } else if (excType instanceof RubyClass && excType.getInstanceVariables().hasInstanceVariable("@java_class")) {
            // java exception where the rescue clause has an embedded java class that could catch it
            RubyClass rubyClass = (RubyClass)excType;
            JavaClass javaClass = (JavaClass)rubyClass.getInstanceVariable("@java_class");
            if (javaClass != null) {
                Class cls = javaClass.javaClass();
                if (cls.isInstance(excObj)) return true;
            }
        }

        return false;
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject excType = (IRubyObject) arg1.retrieve(context, self, currDynScope, temp);
        Object excObj = arg2.retrieve(context, self, currDynScope, temp);

        boolean isUndefExc = excObj == UndefinedValue.UNDEFINED;
        if (excType instanceof RubyArray) {
            RubyArray testTypes = (RubyArray)excType;
            for (int i = 0, n = testTypes.getLength(); i < n; i++) {
                IRubyObject testType = (IRubyObject)testTypes.eltInternal(i);
                boolean handled = isUndefExc ? testType.isTrue() : exceptionHandled(context, testType, excObj);
                if (handled) return runtime.newBoolean(true);
            }
            return runtime.newBoolean(false);
        } else {
            return isUndefExc ? excType : runtime.newBoolean(exceptionHandled(context, excType, excObj));
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RescueEQQInstr(this);
    }
}
