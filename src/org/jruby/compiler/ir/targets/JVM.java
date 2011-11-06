package org.jruby.compiler.ir.targets;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyObject;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.compiler.ir.CompilerTarget;
import org.jruby.compiler.ir.IRBuilder;
import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRScript;
import org.jruby.compiler.ir.compiler_pass.InlineTest;
import org.jruby.compiler.ir.compiler_pass.opts.DeadCodeElimination;
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
import org.jruby.compiler.ir.operands.Constant;
import org.jruby.compiler.ir.operands.Fixnum;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.runtime.Block;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.TraceClassVisitor;
import static org.objectweb.asm.Opcodes.*;
import static org.jruby.util.CodegenUtils.*;
import static org.objectweb.asm.commons.GeneratorAdapter.*;

// This class represents JVM as the target of compilation
// and outputs bytecode
public class JVM implements CompilerTarget {
    private static final Logger LOG = LoggerFactory.getLogger("IRBuilder");
    
    Stack<ClassData> clsStack = new Stack();
    List<ClassData> clsAccum = new ArrayList();
    IRScript script;

    public JVM() {
    }

    public static void main(String[] args) {
        int i = 0;
        boolean isDebug = false;
        boolean deadCode = false;
        String commandLineScript = null;
        String inlineName = null;
        
        for (int argIndex = 0; argIndex < args.length; argIndex++) {
            if (args[argIndex].equals("-debug")) {
                isDebug = true;
                i++;
                continue;
            }
            
            if (args[argIndex].equals("-inline")) {
                argIndex++;
                inlineName = args[argIndex];
                i += 2;
                continue;
            }
            
            if (args[argIndex].equals("-dead")) {
                deadCode = true;
                i++;
                continue;
            }
            
            if (args[argIndex].equals("-e")) {
                argIndex++;
                commandLineScript = args[argIndex];
                break;
            }
        }

        LOG.setDebugEnable(isDebug);

        Ruby ruby = Ruby.newInstance(new RubyInstanceConfig() {
            {
                this.setCompileMode(CompileMode.OFFIR);
            }
        });

        while (i < args.length) {
            Node ast;
            if (commandLineScript != null) {
                ast = ruby.parseInline(new ByteArrayInputStream(commandLineScript.getBytes()), "-e", null);
            } else {
                try {
                    ast = ruby.parseFile(new FileInputStream(args[i]), args[i], null);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
            
            IRScope scope = new IRBuilder().buildRoot((RootNode) ast);
            
            // additional passes not enabled in builder yet
            if (deadCode) scope.runCompilerPass(new DeadCodeElimination());
            if (inlineName != null) {
                scope.runCompilerPass(new InlineTest(inlineName));
            }
            
            // run compiler
            CompilerTarget target = new JDK7();
            
            target.codegen(scope);
            
            i++;
            
            if (commandLineScript != null) break;
        }
    }
    
    public static final int CMP_EQ = 0;

    public ClassVisitor cls() {
        return clsData().cls;
    }

    public ClassData clsData() {
        return clsStack.peek();
    }

    public void pushclass() {
        PrintWriter pw = new PrintWriter(System.out);
        clsStack.push(new ClassData(new TraceClassVisitor(new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS), pw)));
        pw.flush();
    }

    public void popclass() {
        clsStack.pop();
    }

    public IRBytecodeAdapter method() {
        return clsData().method();
    }

    public void pushmethod(String name, int arity) {
        clsData().pushmethod(name, arity);
    }

    public void popmethod() {
        clsData().popmethod();
    }

    public void codegen(IRScope scope) {
        if (scope instanceof IRScript) {
            codegen((IRScript)scope);
        }
    }

    public void codegen(IRScript script) {
        this.script = script;
        emit(script.getRootClass());
    }

    public void emit(IRClass cls) {
        pushclass();
        cls().visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, cls.getName(), null, p(RubyObject.class), null);
        cls().visitSource(script.getFileName().toString(), null);

        // root-level logic
        pushmethod("__class__", 0);
        for (Instr instr: cls.getInstrs()) {
            emit(instr);
        }
        popmethod();

        // Root method
        emit(cls.getRootMethod());

        // Additional methods
        for (IRMethod method : cls.getMethods()) {
            emit(method);
        }

        // Nested classes
        for (IRClass cls2 : cls.getClasses()) {
            emit(cls2);
        }

        cls().visitEnd();
        popclass();
    }

    public void emit(IRMethod method) {
        pushmethod(method.getName(), method.getCallArgs().length);

        for (Instr instr: method.getInstrs()) {
            emit(instr);
        }
        
        popmethod();
    }

    public void emit(Instr instr) {
        JVMEmitter emitter = JVMEmitter.MAP.get(instr.getOperation());
        if (emitter == null) {
            System.err.println("unsupported instruction: " + instr.getOperation());
            return;
        }
        emitter.emit(this, instr);
    }

    public void emitConstant(Constant constant) {
        if (constant instanceof Fixnum) {
            method().push(((Fixnum)constant).value);
        }
    }

    public void emit(Operand operand) {
        if (operand.isConstant()) {
            emitConstant((Constant)operand);
        } else if (operand instanceof Variable) {
            emitVariable((Variable)operand);
        }
    }

    public void emitVariable(Variable variable) {
        int index = getVariableIndex(variable);
        method().loadLocal(index);
    }

    public int getVariableIndex(Variable variable) {
        Integer index = clsStack.peek().methodStack.peek().varMap.get(variable);
        if (index == null) {
            index = method().newLocal(Type.getType(Object.class));
            clsStack.peek().methodStack.peek().varMap.put(variable, index);
        }
        return index;
    }

    public org.objectweb.asm.Label getLabel(Label label) {
        org.objectweb.asm.Label asmLabel = clsData().methodData().labelMap.get(label);
        if (asmLabel == null) {
            asmLabel = method().newLabel();
            clsData().methodData().labelMap.put(label, asmLabel);
        }
        return asmLabel;
    }

    public void declareField(String field) {
        if (!clsData().fieldSet.contains(field)) {
            cls().visitField(ACC_PROTECTED, field, ci(Object.class), null, null);
            clsData().fieldSet.add(field);
        }
    }
}
