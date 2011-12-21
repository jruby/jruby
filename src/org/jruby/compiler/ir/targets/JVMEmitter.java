package org.jruby.compiler.ir.targets;

import java.util.EnumMap;
import java.util.HashMap;
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
import org.jruby.compiler.ir.instructions.LineNumberInstr;
import org.jruby.compiler.ir.instructions.PutFieldInstr;
import org.jruby.compiler.ir.instructions.ReceiveArgumentInstruction;
import org.jruby.compiler.ir.instructions.ReceiveClosureInstr;
import org.jruby.compiler.ir.instructions.ReceiveSelfInstruction;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Type;

public abstract class JVMEmitter {
    static class BEQ extends JVMEmitter {
        public void emit(JVM jvm, Instr instr) {
            BEQInstr beq = (BEQInstr)instr;
            Operand[] args = beq.getOperands();
            jvm.method().loadLocal(0);
            jvm.emit(args[0]);
            jvm.emit(args[1]);
            jvm.method().invokeOtherBoolean("==", 1);
        }
    }
    
    static class CALL extends JVMEmitter {
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

            int index = jvm.methodData().local(call.getResult());
            jvm.method().storeLocal(index);
        }
    }
    
    static class COPY extends JVMEmitter {
        public void emit(JVM jvm, Instr instr) {
            CopyInstr copy = (CopyInstr)instr;
            
            int index = jvm.methodData().local(copy.getResult());
            jvm.emit(copy.getSource());
            jvm.method().storeLocal(index);
        }
    }
    
    static class DEF_INST_METH extends JVMEmitter {
        public void emit(JVM jvm, Instr instr) {
            DefineInstanceMethodInstr defInstMethInstr = (DefineInstanceMethodInstr)instr;
            
            // needs rework
//            IRMethod irMethod = instr.getMethod();
//            GeneratorAdapter adapter = new GeneratorAdapter(ACC_PUBLIC, Method.getMethod("void " + irMethod.getName() + " ()"), null, null, cls());
//            adapter.loadThis();
//            adapter.loadArgs();
//            adapter.invokeStatic(OBJECT_TYPE, Method.getMethod("Object __ruby__" + irMethod.getName() + " (Object)"));
//            adapter.returnValue();
//            adapter.endMethod();
        }
    }
    
    static class DEF_CLASS_METH extends JVMEmitter {
        public void emit(JVM jvm, Instr instr) {
            DefineClassMethodInstr defClassMethInstr = (DefineClassMethodInstr)instr;
            
            // needs rework
//            IRMethod irMethod = instr.getMethod();
//            GeneratorAdapter adapter = new GeneratorAdapter(ACC_PUBLIC | ACC_STATIC, Method.getMethod("void " + irMethod.getName() + " ()"), null, null, cls());
//            adapter.returnValue();
//            adapter.endMethod();
        }
    }
    
    static class JUMP extends JVMEmitter {
        public void emit(JVM jvm, Instr instr) {
            JumpInstr jump = (JumpInstr)instr;
            jvm.method().goTo(jvm.getLabel(jump.target));
        }
    }

    static class LABEL extends JVMEmitter {
        public void emit(JVM jvm, Instr instr) {
                LabelInstr lbl = (LabelInstr)instr;
                jvm.method().mark(jvm.getLabel(lbl.label));
        }
    }

    static class LINE_NUM extends JVMEmitter {
        @Override
        public void emit(JVM jvm, Instr instr) {
            LineNumberInstr lineNumber = (LineNumberInstr)instr;
            jvm.method().adapter.line(lineNumber.lineNumber);
        }
    }

    static class PUT_FIELD extends JVMEmitter {
    public void emit(JVM jvm, Instr instr) {
            PutFieldInstr putField = (PutFieldInstr)instr;
            String field = putField.getRef();
            jvm.declareField(field);
            jvm.emit(putField.getValue());
            jvm.emit(putField.getTarget());
            jvm.method().putField(JVM.OBJECT_TYPE, field, JVM.OBJECT_TYPE);
        }
    }

    static class GET_FIELD extends JVMEmitter {
    public void emit(JVM jvm, Instr instr) {
            GetFieldInstr getField = (GetFieldInstr)instr;
            String field = getField.getRef();
            jvm.declareField(field);
            jvm.emit(getField.getSource());
            jvm.method().getField(JVM.OBJECT_TYPE, field, JVM.OBJECT_TYPE);
        }
    }

    static class RETURN extends JVMEmitter {
        public void emit(JVM jvm, Instr instr) {
            ReturnInstr ret = (ReturnInstr)instr;
            jvm.emit(ret.getOperands()[0]);
            jvm.method().returnValue();
        }
    }
    
    static class RECV_SELF extends JVMEmitter {
        public void emit(JVM jvm, Instr instr) {
            ReceiveSelfInstruction recvSelf = (ReceiveSelfInstruction)instr;
            int $selfIndex = jvm.methodData().local(recvSelf.getResult());
            jvm.method().loadLocal(1);
            jvm.method().storeLocal($selfIndex);
        }
    }
    
    static class RECV_CLOSURE extends JVMEmitter {
        public void emit(JVM jvm, Instr instr) {
            ReceiveClosureInstr recvClosure = (ReceiveClosureInstr)instr;
            int closureIndex = jvm.method().newLocal("$block", Type.getType(Block.class));
            // TODO: receive closure
//          method().loadLocal(1);
//          method().storeLocal(closureIndex);
        }
    }

    static class RECV_ARG extends JVMEmitter {
        public void emit(JVM jvm, Instr instr) {
            ReceiveArgumentInstruction recvArg = (ReceiveArgumentInstruction)instr;
            int index = jvm.methodData().local(recvArg.getResult());
            // TODO: ideally, reuse args from signature
        }
    };
    
    public abstract void emit(JVM jvm, Instr instr);

    public static final EnumMap<Operation, JVMEmitter> MAP;
    static {
        HashMap map = new HashMap();
        map.put(Operation.BEQ, new BEQ());
        map.put(Operation.CALL, new CALL());
        map.put(Operation.COPY, new COPY());
        map.put(Operation.DEF_INST_METH, new DEF_INST_METH());
        map.put(Operation.DEF_CLASS_METH, new DEF_CLASS_METH());
        map.put(Operation.JUMP, new JUMP());
        map.put(Operation.LABEL, new LABEL());
        map.put(Operation.LINE_NUM, new LINE_NUM());
        map.put(Operation.PUT_FIELD, new PUT_FIELD());
        map.put(Operation.GET_FIELD, new GET_FIELD());
        map.put(Operation.RETURN, new RETURN());
        map.put(Operation.RECV_SELF, new RECV_SELF());
        map.put(Operation.RECV_CLOSURE, new RECV_CLOSURE());
        map.put(Operation.RECV_ARG, new RECV_ARG());
        
        MAP = new EnumMap<Operation, JVMEmitter>(map);
    }
}
