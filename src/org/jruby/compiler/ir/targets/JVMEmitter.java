package org.jruby.compiler.ir.targets;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.BEQInstr;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.DefineClassMethodInstr;
import org.jruby.compiler.ir.instructions.DefineInstanceMethodInstr;
import org.jruby.compiler.ir.instructions.GetFieldInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.JumpInstr;
import org.jruby.compiler.ir.instructions.LabelInstr;
import org.jruby.compiler.ir.instructions.PutFieldInstr;
import org.jruby.compiler.ir.instructions.ReceiveArgumentInstruction;
import org.jruby.compiler.ir.instructions.ReceiveClosureInstr;
import org.jruby.compiler.ir.instructions.ReceiveSelfInstruction;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.runtime.Block;
import org.objectweb.asm.Type;

public enum JVMEmitter {
    BEQ {
        public void emit(JVM jvm, Instr instr) {
            BEQInstr beq = (BEQInstr)instr;
            Operand[] args = beq.getOperands();
            jvm.method().loadLocal(0);
            jvm.emit(args[0]);
            jvm.emit(args[1]);
            jvm.method().invokeOtherBoolean("==", 1);
        }
    },
    
    CALL {
        public void emit(JVM jvm, Instr instr) {
            CallInstr call = (CallInstr)instr;

            jvm.method().loadLocal(0);
            jvm.emit(call.getReceiver());
            for (Operand operand : call.getCallArgs()) {
                jvm.emit(operand);
            }

            switch (call.getCallType()) {
                case FUNCTIONAL:
                case VARIABLE:
                    jvm.method().invokeSelf(call.getMethodAddr().getName(), call.getCallArgs().length);
                    break;
                case NORMAL:
                    jvm.method().invokeOther(call.getMethodAddr().getName(), call.getCallArgs().length);
                    break;
                case SUPER:
                    jvm.method().invokeSuper(call.getMethodAddr().getName(), call.getCallArgs().length);
                    break;
            }
        }
    },
    
    COPY {
        public void emit(JVM jvm, Instr instr) {
            CopyInstr copy = (CopyInstr)instr;
            
            int index = jvm.getVariableIndex(copy.getResult());
            jvm.emit(copy.getOperands()[0]);
            jvm.method().storeLocal(index);
        }
    },
    
    DEF_INST_METH {
        public void emit(JVM jvm, Instr instr) {
            DefineInstanceMethodInstr defInstMethInstr = (DefineInstanceMethodInstr)instr;
            
            // needs rework
//            IRMethod irMethod = instr.getMethod();
//            GeneratorAdapter adapter = new GeneratorAdapter(ACC_PUBLIC, Method.getMethod("void " + irMethod.getName() + " ()"), null, null, cls());
//            adapter.loadThis();
//            adapter.loadArgs();
//            adapter.invokeStatic(Type.getType(Object.class), Method.getMethod("Object __ruby__" + irMethod.getName() + " (Object)"));
//            adapter.returnValue();
//            adapter.endMethod();
        }
    },
    
    DEF_CLASS_METH {
        public void emit(JVM jvm, Instr instr) {
            DefineClassMethodInstr defClassMethInstr = (DefineClassMethodInstr)instr;
            
            // needs rework
//            IRMethod irMethod = instr.getMethod();
//            GeneratorAdapter adapter = new GeneratorAdapter(ACC_PUBLIC | ACC_STATIC, Method.getMethod("void " + irMethod.getName() + " ()"), null, null, cls());
//            adapter.returnValue();
//            adapter.endMethod();
        }
    },
    
    JUMP {
        public void emit(JVM jvm, Instr instr) {
            JumpInstr jump = (JumpInstr)instr;
            jvm.method().goTo(jvm.getLabel(jump.target));
        }
    },

    LABEL {
        public void emit(JVM jvm, Instr instr) {
                LabelInstr lbl = (LabelInstr)instr;
                jvm.method().mark(jvm.getLabel(lbl.label));
        }
    },

    PUT_FIELD {
    public void emit(JVM jvm, Instr instr) {
            PutFieldInstr putField = (PutFieldInstr)instr;
            String field = putField.getRef();
            jvm.declareField(field);
            jvm.emit(putField.getValue());
            jvm.emit(putField.getTarget());
            jvm.method().putField(Type.getType(Object.class), field, Type.getType(Object.class));
        }
    },

    GET_FIELD {
    public void emit(JVM jvm, Instr instr) {
            GetFieldInstr getField = (GetFieldInstr)instr;
            String field = getField.getRef();
            jvm.declareField(field);
            jvm.emit(getField.getSource());
            jvm.method().getField(Type.getType(Object.class), field, Type.getType(Object.class));
        }
    },

    RETURN {
        public void emit(JVM jvm, Instr instr) {
            ReturnInstr ret = (ReturnInstr)instr;
            jvm.emit(ret.getOperands()[0]);
            jvm.method().returnValue();
        }
    },
    
    RECV_SELF {
        public void emit(JVM jvm, Instr instr) {
            ReceiveSelfInstruction recvSelf = (ReceiveSelfInstruction)instr;
            int selfIndex = jvm.method().newLocal(Type.getType(Object.class));
            jvm.method().loadLocal(1);
            jvm.method().storeLocal(selfIndex);
        }
    },
    
    RECV_CLOSURE {
        public void emit(JVM jvm, Instr instr) {
            ReceiveClosureInstr recvClosure = (ReceiveClosureInstr)instr;
            int closureIndex = jvm.method().newLocal(Type.getType(Block.class));
            // TODO: receive closure
//          method().loadLocal(1);
//          method().storeLocal(closureIndex);
        }
    },

    RECV_ARG {
        public void emit(JVM jvm, Instr instr) {
            ReceiveArgumentInstruction recvArg = (ReceiveArgumentInstruction)instr;
            int index = jvm.getVariableIndex(recvArg.getResult());
            // TODO: ideally, reuse args from signature
        }
    };
    
    public abstract void emit(JVM jvm, Instr instr);

    public static final EnumMap<Operation, JVMEmitter> MAP;
    static {
        HashMap map = new HashMap();
        map.put(Operation.BEQ, BEQ);
        map.put(Operation.CALL, CALL);
        map.put(Operation.COPY, COPY);
        map.put(Operation.DEF_INST_METH, DEF_INST_METH);
        map.put(Operation.DEF_CLASS_METH, DEF_CLASS_METH);
        map.put(Operation.JUMP, JUMP);
        map.put(Operation.LABEL, LABEL);
        map.put(Operation.PUT_FIELD, PUT_FIELD);
        map.put(Operation.GET_FIELD, GET_FIELD);
        map.put(Operation.RETURN, RETURN);
        map.put(Operation.RECV_SELF, RECV_SELF);
        map.put(Operation.RECV_CLOSURE, RECV_CLOSURE);
        map.put(Operation.RECV_ARG, RECV_ARG);
        
        MAP = new EnumMap<Operation, JVMEmitter>(map);
    }
}
