package org.jruby.compiler.ir.compiler_pass.opts;

import java.util.Map;
import java.util.HashMap;
import java.util.ListIterator;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.IR_Class;
import org.jruby.compiler.ir.IR_Module;
import org.jruby.compiler.ir.instructions.ASSERT_METHOD_VERSION_Instr;
import org.jruby.compiler.ir.instructions.CALL_Instr;
import org.jruby.compiler.ir.instructions.COPY_Instr;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.CodeVersion;
import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.Fixnum;
import org.jruby.compiler.ir.operands.Float;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Constant;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;

public class LocalOptimizationPass implements CompilerPass
{
    public LocalOptimizationPass() { }

    // Should we run this pass on the current scope before running it on nested scopes?
    public boolean isPreOrder() { return false; }

    public void run(IR_Scope s)
    {
        if (!(s instanceof IR_Method))
            return;

        // Reset value map if this instruction is the start/end of a basic block
        //
        // Right now, calls are considered hard boundaries for optimization and
        // information cannot be propagated across them!
        //
        // SSS FIXME: Rather than treat all calls with a broad brush, what we need
        // is to capture different attributes about a call :
        //   - uses closures
        //   - known call target
        //   - can modify scope,
        //   - etc.
        //
        // This information is probably already present in the AST Inspector
        IR_Method m = (IR_Method)s;
        Label     deoptLabel = m.getNewLabel();
        Map<Operand,Operand> valueMap   = new HashMap<Operand,Operand>();
        Map<String,CodeVersion> versionMap = new HashMap<String,CodeVersion>();
        ListIterator<IR_Instr> instrs = m.getInstrs().listIterator();
        while (instrs.hasNext()) {
            IR_Instr i = instrs.next();
            Operation iop = i._op;
            if (iop.startsBasicBlock()) {
                valueMap = new HashMap<Operand,Operand>();
                versionMap = new HashMap<String, CodeVersion>();
            }

            // Simplify instruction and record mapping between target variable and simplified value
            //System.out.println("BEFORE: " + i);
            Operand  val = i.simplifyAndGetResult(valueMap);
            Variable res = i.getResult();
            //System.out.println("For " + i + "; dst = " + res + "; val = " + val);
            //System.out.println("AFTER: " + i);
            if (val != null && res != null && res != val) {
                valueMap.put(res, val);
            }
            // Optimize some core class method calls for constant values
            else if (iop.isCall()) {
                val = null;
                CALL_Instr call = ((CALL_Instr)i);
                Operand    r    = call.getReceiver();

                    // If 'r' is not a constant, it could actually be a compound value!
                    // Look in our value map to see if we have a simplified value for the receiver.
                if (!r.isConstant()) {
                    Operand v = valueMap.get(r);
                    if (v != null)
                        r = v;
                }

                // Check if we can optimize this call based on the receiving method and receiver type
                // Use the simplified receiver!
                IR_Method rm = call.getTargetMethodWithReceiver(r);
                if (rm != null) {
                    IR_Module rc = rm.getDefiningModule();
                    if (rc == IR_Class.getCoreClass("Fixnum")) {
                        Operand[] args = call.getCallArgs();
                        if (args[1].isConstant()) {
                            addMethodGuard(rm, deoptLabel, versionMap, instrs);
                            val = ((Fixnum)r).computeValue(rm._name, (Constant)args[1]);
                        }
                    }
                    else if (rc == IR_Class.getCoreClass("Float")) {
                        Operand[] args = call.getCallArgs();
                        if (args[1].isConstant()) {
                            addMethodGuard(rm, deoptLabel, versionMap, instrs);
                            val = ((Float)r).computeValue(rm._name, (Constant)args[1]);
                        }
                    }
                    else if (rc == IR_Class.getCoreClass("Array")) {
                        Operand[] args = call.getCallArgs();
                        if (args[1] instanceof Fixnum && (rm._name == "[]")) {
                            addMethodGuard(rm, deoptLabel, versionMap, instrs);
                            val = ((Array)r).fetchCompileTimeArrayElement(((Fixnum)args[1])._value.intValue(), false);
                        }
                    }

                    // If we got a simplified value, mark the call dead and insert a copy in its place!
                    if (val != null) {
                        i.markDead();
                        instrs.add(new COPY_Instr(res, val));
                        valueMap.put(res, val);
                    }
                }
            } 

            // If the call has been optimized away in the previous step, it is no longer a hard boundary for opts!
            if (iop.endsBasicBlock() || (iop.isCall() && !i.isDead())) {
                valueMap = new HashMap<Operand,Operand>();
                versionMap = new HashMap<String, CodeVersion>();
            }
        }
    }

    private void addMethodGuard(IR_Method m, Label deoptLabel, Map<String, CodeVersion> versionMap, ListIterator instrs)
    {
        String      fullName     = m.getFullyQualifiedName();
        CodeVersion knownVersion = versionMap.get(fullName);
        CodeVersion mVersion     = m.getCodeVersionToken();
        if ((knownVersion == null) || (knownVersion._version != mVersion._version)) {
            instrs.add(new ASSERT_METHOD_VERSION_Instr(m.getDefiningModule(), m._name, m.getCodeVersionToken(), deoptLabel));
            versionMap.put(fullName, mVersion);
        }
    }
}
