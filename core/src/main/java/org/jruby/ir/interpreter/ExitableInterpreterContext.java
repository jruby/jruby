package org.jruby.ir.interpreter;

import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.operands.Operand;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Arrays;

public class ExitableInterpreterContext extends InterpreterContext {

    private final static ExitableInterpreterEngine EXITABLE_INTERPRETER = new ExitableInterpreterEngine();
	
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
    
    @Override
    public ExitableInterpreterEngine getEngine()
    {
    	return EXITABLE_INTERPRETER;
    }

    /**
     * @returns the live ruby values for the operand to the original super call.
      */
    public IRubyObject[] getArgs(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temps) {
    	return superCall.prepareArguments(context, self, currScope, currDynScope, temps);
    }
}
