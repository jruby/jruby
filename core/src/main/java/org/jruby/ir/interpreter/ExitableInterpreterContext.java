/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ir.interpreter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveRestArgInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.ImmutableLiteral;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.runtime.ThreadContext.CALL_SPLATS;

public class ExitableInterpreterContext extends InterpreterContext {

    public static final ExitableInterpreterContext NULL = new ExitableInterpreterContext(null, null, 0, null, null, 0);

    private final static ExitableInterpreterEngine EXITABLE_INTERPRETER = new ExitableInterpreterEngine();
	
    private final CallBase superCall;
    private final int exitIPC;
    private final boolean exitsAtReturn;
    private final boolean directSuperNoArgs;
    private final boolean directSuperAllArgs;
    private final int directSuperRequiredArgs;
    private final Operand[] terminalLiteralSuperArgs;

    public ExitableInterpreterContext(InterpreterContext originalIC, CallBase superCall, int exitIPC) {
        this(originalIC.getScope(), Arrays.asList(originalIC.getInstructions()), originalIC.getTemporaryVariableCount(), originalIC.getFlags(), superCall, exitIPC);
    }

    private ExitableInterpreterContext(IRScope scope, List<Instr> instructions, int temporaryVariableCount, EnumSet<IRFlags> flags, CallBase superCall, int exitIPC) {
        super(scope, instructions, temporaryVariableCount, flags);

        this.superCall = superCall;
        this.exitIPC = exitIPC;
        this.exitsAtReturn = exitsAtReturn(instructions, superCall, exitIPC);
        this.directSuperNoArgs = directSuperNoArgs(instructions, superCall, exitIPC) && getStaticScope().getSignature().isNoArguments();
        this.directSuperAllArgs = (directSuperAllArgs(instructions, superCall, exitIPC) && isRestOnlySignature()) ||
                directSuperAllZSuperArgs(instructions, superCall, exitIPC);
        this.directSuperRequiredArgs = directSuperRequiredArgs(instructions, superCall, exitIPC);
        this.terminalLiteralSuperArgs = terminalLiteralSuperArgs(instructions, superCall, exitIPC);
    }

    public ExitableInterpreterEngineState getEngineState() {
        return new ExitableInterpreterEngineState(this);
    }

    public int getExitIPC() {
        return exitIPC;
    }

    public boolean exitsAtReturn() {
        return exitsAtReturn;
    }

    public boolean canEscapeAtSuper() {
        int[] rescuePCs = getRescueIPCs();

        return exitsAtReturn && (rescuePCs == null || rescuePCs[exitIPC] == -1);
    }

    public boolean directSuperNoArgs() {
        return directSuperNoArgs;
    }

    public boolean directSuperAllArgs() {
        return directSuperAllArgs;
    }

    public int directSuperRequiredArgs() {
        return directSuperRequiredArgs;
    }

    public boolean terminalLiteralSuper() {
        return terminalLiteralSuperArgs != null;
    }

    public IRubyObject[] getTerminalLiteralSuperArgs(ThreadContext context) {
        Operand[] operands = terminalLiteralSuperArgs;
        IRubyObject[] args = new IRubyObject[operands.length];

        for (int i = 0; i < operands.length; i++) {
            args[i] = (IRubyObject) ((ImmutableLiteral<?>) operands[i]).cachedObject(context);
        }

        return args;
    }
    
    @Override
    public ExitableInterpreterEngine getEngine() {
    	return EXITABLE_INTERPRETER;
    }

    /**
     * @return the live ruby values for the operand to the original super call.
      */
    public IRubyObject[] getArgs(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temps) {
    	IRubyObject[] args = superCall.prepareArguments(context, self, currScope, currDynScope, temps);

        IRRuntimeHelpers.setCallInfo(context, superCall.getFlags());

        return args;
    }

    private static boolean exitsAtReturn(List<Instr> instructions, CallBase superCall, int exitIPC) {
        if (instructions == null || exitIPC + 1 >= instructions.size()) return false;

        Instr instr = instructions.get(exitIPC + 1);

        return instr instanceof ReturnInstr && ((ReturnInstr) instr).getReturnValue().equals(superCall.getResult());
    }

    private static boolean directSuperNoArgs(List<Instr> instructions, CallBase superCall, int exitIPC) {
        if (instructions == null || !exitsAtReturn(instructions, superCall, exitIPC) || superCall.getArgsCount() != 0) return false;

        for (int i = 0; i < exitIPC; i++) {
            Operation operation = instructions.get(i).getOperation();

            switch (operation) {
                case CHECK_ARITY:
                case COPY:
                case LINE_NUM:
                case LOAD_IMPLICIT_CLOSURE:
                case LOAD_FRAME_CLOSURE:
                case RECV_KW:
                case RECV_SELF:
                    break;
                default:
                    return false;
            }
        }

        return true;
    }

    private static boolean directSuperAllArgs(List<Instr> instructions, CallBase superCall, int exitIPC) {
        if (instructions == null || !exitsAtReturn(instructions, superCall, exitIPC) ||
                superCall.getArgsCount() != 1 || superCall.getFlags() != CALL_SPLATS) return false;

        for (int i = 0; i < exitIPC; i++) {
            Operation operation = instructions.get(i).getOperation();

            switch (operation) {
                case BUILD_SPLAT:
                case CHECK_ARITY:
                case COPY:
                case LINE_NUM:
                case LOAD_IMPLICIT_CLOSURE:
                case LOAD_FRAME_CLOSURE:
                case RECV_KW:
                case RECV_REST_ARG:
                case RECV_SELF:
                    break;
                default:
                    return false;
            }
        }

        return true;
    }

    private boolean directSuperAllZSuperArgs(List<Instr> instructions, CallBase superCall, int exitIPC) {
        if (instructions == null || !exitsAtReturn(instructions, superCall, exitIPC) || superCall.getFlags() != 0) return false;

        var signature = getStaticScope().getSignature();
        int required = signature.required();
        if (superCall.getArgsCount() != required + 1 || signature.opt() != 0 || !signature.hasRest() ||
                signature.post() != 0 || signature.hasKwargs()) return false;

        Map<Variable, Integer> receivedArgs = new HashMap<>();
        Variable restArg = null;

        for (int i = 0; i < exitIPC; i++) {
            Instr instr = instructions.get(i);
            Operation operation = instr.getOperation();

            if (instr instanceof ResultInstr resultInstr) {
                Variable result = resultInstr.getResult();
                if (receivedArgs.containsKey(result) || result.equals(restArg)) return false;
            }

            switch (operation) {
                case CHECK_ARITY:
                case COPY:
                case LINE_NUM:
                case LOAD_IMPLICIT_CLOSURE:
                case LOAD_FRAME_CLOSURE:
                case RECV_KW:
                case RECV_SELF:
                    break;
                case RECV_PRE_REQD_ARG:
                    ReceivePreReqdArgInstr receive = (ReceivePreReqdArgInstr) instr;
                    receivedArgs.put(receive.getResult(), receive.getArgIndex());
                    break;
                case RECV_REST_ARG:
                    ReceiveRestArgInstr rest = (ReceiveRestArgInstr) instr;
                    if (rest.getArgIndex() != required || rest.required != required) return false;
                    restArg = rest.getResult();
                    break;
                default:
                    return false;
            }
        }

        Operand[] superArgs = superCall.getCallArgs();
        for (int i = 0; i < required; i++) {
            if (!(superArgs[i] instanceof Variable variable) || receivedArgs.get(variable) == null || receivedArgs.get(variable) != i) {
                return false;
            }
        }

        return superArgs[required] instanceof Splat splat && splat.getArray().equals(restArg);
    }

    private static Operand[] terminalLiteralSuperArgs(List<Instr> instructions, CallBase superCall, int exitIPC) {
        if (instructions == null || !exitsAtReturn(instructions, superCall, exitIPC) || superCall.getFlags() != 0 ||
                superCall.getClosureArg() instanceof WrappedIRClosure) return null;

        for (int i = 0; i < exitIPC; i++) {
            Operation operation = instructions.get(i).getOperation();

            switch (operation) {
                case CHECK_ARITY:
                case COPY:
                case LINE_NUM:
                case LOAD_IMPLICIT_CLOSURE:
                case LOAD_FRAME_CLOSURE:
                case RECV_KW:
                case RECV_SELF:
                    break;
                default:
                    return null;
            }
        }

        Operand[] args = superCall.getCallArgs();
        for (Operand arg : args) {
            if (!(arg instanceof ImmutableLiteral<?>)) return null;
        }

        return args;
    }

    private int directSuperRequiredArgs(List<Instr> instructions, CallBase superCall, int exitIPC) {
        if (instructions == null || !exitsAtReturn(instructions, superCall, exitIPC) || superCall.getFlags() != 0) return -1;

        var signature = getStaticScope().getSignature();
        int required = signature.required();
        if (required == 0 || superCall.getArgsCount() != required || signature.opt() != 0 || signature.hasRest() ||
                signature.post() != 0 || signature.hasKwargs()) return -1;

        Map<Variable, Integer> receivedArgs = new HashMap<>();

        for (int i = 0; i < exitIPC; i++) {
            Instr instr = instructions.get(i);
            Operation operation = instr.getOperation();

            if (instr instanceof ResultInstr resultInstr && receivedArgs.containsKey(resultInstr.getResult())) return -1;

            switch (operation) {
                case CHECK_ARITY:
                case COPY:
                case LINE_NUM:
                case LOAD_IMPLICIT_CLOSURE:
                case LOAD_FRAME_CLOSURE:
                case RECV_KW:
                case RECV_SELF:
                    break;
                case RECV_PRE_REQD_ARG:
                    ReceivePreReqdArgInstr receive = (ReceivePreReqdArgInstr) instr;
                    receivedArgs.put(receive.getResult(), receive.getArgIndex());
                    break;
                default:
                    return -1;
            }
        }

        Operand[] superArgs = superCall.getCallArgs();
        for (int i = 0; i < required; i++) {
            if (!(superArgs[i] instanceof Variable variable) || receivedArgs.get(variable) == null || receivedArgs.get(variable) != i) {
                return -1;
            }
        }

        return required;
    }

    private boolean isRestOnlySignature() {
        var signature = getStaticScope().getSignature();

        return signature.pre() == 0 && signature.opt() == 0 && signature.post() == 0 &&
                signature.hasRest() && !signature.hasKwargs();
    }
}
