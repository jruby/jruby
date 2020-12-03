package org.jruby.ir.interpreter;

import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.operands.Operand;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Arrays;

public class ExitableInterpreterContext extends InterpreterContext {
    private CallBase superCall;
    private int exitIPC;

    public ExitableInterpreterContext(InterpreterContext originalIC, CallBase superCall, int exitIPC) {
        super(originalIC.getScope(), Arrays.asList(originalIC.getInstructions()),
                originalIC.getTemporaryVariableCount(), originalIC.getFlags());

        this.superCall = superCall;
        this.exitIPC = exitIPC;
    }

    public ExitableInterpreterEngineState getEngineState() {
        return new ExitableInterpreterEngineState(this);
    }

    public int getExitIPC() {
        return exitIPC;
    }

    /**
     * @returns the live ruby values for the operand to the original super call.
      */
    public IRubyObject[] getArgs(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temps) {
        Operand[] args = superCall.getCallArgs();
        int length = args.length;
        IRubyObject[] values = new IRubyObject[length];

        for(int i = 0; i < length; i++) {
            values[i] = (IRubyObject) args[i].retrieve(context, self, currScope, currDynScope, temps);
        }

        return values;
    }
}
