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

import org.jruby.RubyModule;
import org.jruby.common.IRubyWarnings;
import org.jruby.internal.runtime.methods.ExitableReturn;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.*;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;

import static org.jruby.util.RubyStringBuilder.ids;
import static org.jruby.util.RubyStringBuilder.str;

/**
 * InterpreterEngine capable of exiting part way through execution up to a particular instruction.  Continued
 * execution will assign the result of that instruction which we pass in and continue executing from that point.
 * The mechanism for this interpreter to work is a context object which contains the IPC, the result, and the
 * temporary variables.
 *
 * Extra Notes:
 *   - We may need to make all natural split arities here if a JIT calls this directly.
 *   - I dislike replicating this code but we do not want our default interpreter to have an extra stack frame.
 */
public class ExitableInterpreterEngine extends InterpreterEngine {
    public ExitableReturn interpret(ThreadContext context, Block block, IRubyObject self,
                                 ExitableInterpreterContext interpreterContext, ExitableInterpreterEngineState state,
                                 RubyModule implClass, String name, IRubyObject[] args, Block blockArg) {
        Instr[] instrs = interpreterContext.getInstructions();
        Object[] temp = state.getTemporaryVariables();
        int n = instrs.length;
        int ipc = state.getIPC();
        int exitIPC = interpreterContext.getExitIPC();
        Object exception = null;

        boolean acceptsKeywordArgument = interpreterContext.receivesKeywordArguments();
        if (acceptsKeywordArgument) args = IRRuntimeHelpers.frobnicateKwargsArgument(context, args, interpreterContext.getRequiredArgsCount());

        StaticScope currScope = interpreterContext.getStaticScope();
        DynamicScope currDynScope = context.getCurrentScope();

        int[] rescuePCs = interpreterContext.getRescueIPCs();

        // Init profiling this scope
        boolean debug   = IRRuntimeHelpers.isDebug();
        boolean profile = IRRuntimeHelpers.inProfileMode();
        Integer scopeVersion = profile ? Profiler.initProfiling(interpreterContext.getScope()) : 0;

        // Enter the looooop!
        while (ipc < n) {
            // We want to exit at this instr and return its call arguments to consumer of this interpreter.
            if (ipc == exitIPC) {
                // FIXME: I assume result of super in this case will be nil which means we should not have to explicitly
                // set the temp to nil but we shall see...
                state.setIPC(ipc + 1);  // Mark next instr to execute when we call execute again using this state.
                // FIXME: We are forcing a boxing to a Ruby array we probably do not need but did it anyways so it matched the
                // interface of interpreterengine (re-consider this).
                return new ExitableReturn(
                		context.runtime.newArray(interpreterContext.getArgs(context, self, currScope, currDynScope, temp)),
        				((CallBase)instrs[ipc]).prepareBlock(context, self, currScope, currDynScope, temp));
            }

            Instr instr = instrs[ipc];

            Operation operation = instr.getOperation();
            if (debug) {
                Interpreter.LOG.info("I: " + ipc + ", R: "  + rescuePCs[ipc] + " - " + instr + ">");
                Interpreter.interpInstrsCount++;
            } else if (profile) {
                Profiler.instrTick(operation);
                Interpreter.interpInstrsCount++;
            }

            try {
                switch (operation.opClass) {
                    case ARG_OP:
                        receiveArg(context, instr, operation, args, acceptsKeywordArgument, currDynScope, temp, exception, blockArg);
                        break;
                    case CALL_OP:
                        if (profile) Profiler.updateCallSite(instr, interpreterContext.getScope(), scopeVersion);
                        processCall(context, instr, operation, currDynScope, currScope, temp, self);
                        break;
                    case RET_OP:
                        processReturnOp(context, block, instr, operation, currDynScope, temp, self, currScope);
                        return new ExitableReturn(context.runtime.newArray(), Block.NULL_BLOCK);
                    case BRANCH_OP:
                        switch (operation) {
                            case JUMP:
                                JumpInstr jump = ((JumpInstr)instr);
                                ipc = jump.getJumpTarget().getTargetPC();
                                break;
                            default:
                                ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc + 1);
                                break;

                        }
                        continue;
                    case BOOK_KEEPING_OP:
                        switch (operation) {
                            case PUSH_METHOD_BINDING:
                                // IMPORTANT: Preserve this update of currDynScope.
                                // This affects execution of all instructions in this scope
                                // which will now use the updated value of currDynScope.
                                currDynScope = interpreterContext.newDynamicScope(context);
                                context.pushScope(currDynScope);
                            case EXC_REGION_START:
                            case EXC_REGION_END:
                                break;
                            default:
                                processBookKeepingOp(context, block, instr, operation, name, args, self, blockArg, implClass, currDynScope, temp, currScope);
                        }
                        break;
                    case OTHER_OP:
                        processOtherOp(context, block, instr, operation, currDynScope, currScope, temp, self);
                        break;
                }

                ipc++;
            } catch (Throwable t) {
                if (debug) extractToMethodToAvoidC2Crash(instr, t);

                ipc = rescuePCs == null ? -1 : rescuePCs[ipc];

                if (debug) {
                    Interpreter.LOG.info("in : " + interpreterContext.getScope() + ", caught Java throwable: " + t + "; excepting instr: " + instr);
                    Interpreter.LOG.info("ipc for rescuer: " + ipc);
                }

                if (ipc == -1) {
                    Helpers.throwException(t);
                } else {
                    exception = t;
                }
            }
        }

        // Control should never get here!
        throw context.runtime.newRuntimeError("BUG: interpreter fell through to end unexpectedly");
    }

    protected static void processOtherOp(ThreadContext context, Block block, Instr instr, Operation operation, DynamicScope currDynScope,
                                         StaticScope currScope, Object[] temp, IRubyObject self) {
        switch(operation) {
            case RECV_SELF:
                break;
            case COPY: {
                CopyInstr c = (CopyInstr)instr;
                setResult(temp, currDynScope, c.getResult(), retrieveOp(c.getSource(), context, self, currDynScope, currScope, temp));
                break;
            }
            case GET_FIELD: {
                GetFieldInstr gfi = (GetFieldInstr)instr;
                IRubyObject object = (IRubyObject)gfi.getSource().retrieve(context, self, currScope, currDynScope, temp);
                VariableAccessor a = gfi.getAccessor(object);
                Object result = a == null ? null : (IRubyObject)a.get(object);
                if (result == null) {
                    if (context.runtime.isVerbose()) {
                        context.runtime.getWarnings().warning(IRubyWarnings.ID.IVAR_NOT_INITIALIZED,
                                str(context.runtime, "instance variable ", ids(context.runtime, gfi.getId()), " not initialized"));
                    }
                    result = context.nil;
                }
                setResult(temp, currDynScope, gfi.getResult(), result);
                break;
            }
            case RUNTIME_HELPER: {
                RuntimeHelperCall rhc = (RuntimeHelperCall)instr;
                setResult(temp, currDynScope, rhc.getResult(),
                        rhc.callHelper(context, currScope, currDynScope, self, temp, block));
                break;
            }
            case CHECK_FOR_LJE:
                ((CheckForLJEInstr) instr).check(context, currDynScope, block);
                break;
            case LOAD_FRAME_CLOSURE:
                setResult(temp, currDynScope, instr, context.getFrameBlock());
                return;
            // ---------- All the rest ---------
            default:
                setResult(temp, currDynScope, instr, instr.interpret(context, currScope, currDynScope, self, temp));
                break;
        }
    }
}
