package org.jruby.ir.targets;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.ir.CompilerTarget;
import org.jruby.ir.IRBuilder;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.Tuple;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.Variable;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.objectweb.asm.Opcodes.*;

// This class represents JVM as the target of compilation
// and outputs bytecode
public class JVM implements CompilerTarget {
    private static final Logger LOG = LoggerFactory.getLogger("IRBuilder");
    
    Stack<ClassData> clsStack = new Stack();
    List<ClassData> clsAccum = new ArrayList();
    IRScriptBody script;
    ClassWriter writer;

    public JVM() {
    }

    public static Class compile(Ruby ruby, IRScope scope, JRubyClassLoader jrubyClassLoader) {
        // run compiler
        CompilerTarget target = new JDK7();

        target.codegen(scope);

//        try {
//            FileOutputStream fos = new FileOutputStream("tmp.class");
//            fos.write(target.code());
//            fos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return jrubyClassLoader.defineClass(scriptToClass(scope.getName()), target.code());
    }
    
    public static final int CMP_EQ = 0;

    public byte[] code() {
        return writer.toByteArray();
    }

    public ClassVisitor cls() {
        return clsData().cls;
    }

    public ClassData clsData() {
        return clsStack.peek();
    }

    public MethodData methodData() {
        return clsData().methodData();
    }

    public void pushclass(String clsName) {
        PrintWriter pw = new PrintWriter(System.out);
        clsStack.push(new ClassData(clsName, new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)));
        pw.flush();
    }

    public void pushscript(String clsName) {
        PrintWriter pw = new PrintWriter(System.out);
        writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        clsStack.push(new ClassData(clsName, writer));

        cls().visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, clsName, null, p(Object.class), null);
        cls().visitSource(script.getFileName(), null);
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
        method().startMethod();

        // locals for ThreadContext and self
        methodData().local("$context", JVM.THREADCONTEXT_TYPE);
        methodData().local("$scope", JVM.STATICSCOPE_TYPE);
        methodData().local("$self");//, JVM.OBJECT_TYPE);
        for (int i = 0; i < arity; i++) {
            // incoming arguments
            methodData().local("$argument" + i);
        }
    }

    public void popmethod() {
        clsData().popmethod();
    }

    public void codegen(IRScope scope) {
        if (scope instanceof IRScriptBody) {
            codegen((IRScriptBody)scope);
        }
    }

    public void codegen(IRScriptBody script) {
        this.script = script;
        emit(script);
    }
    
    private static String scriptToClass(String name) {
        if (name.equals("-e")) {
            return "DashE";
        } else {
            return JavaNameMangler.mangledFilenameForStartupClasspath(name);
        }
    }
    
    public void emit(IRScriptBody script) {
        String clsName = scriptToClass(script.getName());
        pushscript(clsName);

        emitScope(script, "__script__", 0);

        cls().visitEnd();
        popclass();
    }

    public void emit(IRMethod method) {
        emitScope(method, method.getName(), method.getCallArgs().length);

        // push a method handle for binding purposes
        method().pushHandle(clsData().clsName, method.getName(), method.getStaticScope().getRequiredArgs());
    }

    public void emit(IRModuleBody method) {
        String name = method.getName();
        if (name.indexOf("DUMMY_MC") != -1) {
            name = "METACLASS:" + Math.abs(method.hashCode());
        }
        emitScope(method, name, 0);

        // push a method handle for binding purposes
        method().pushHandle(clsData().clsName, name, method.getStaticScope().getRequiredArgs());
    }
    
    public void emitScope(IRScope scope, String name, int arity) {
        pushmethod(name, arity);

        Tuple<Instr[], Map<Integer,Label[]>> t = scope.prepareForCompilation();
        Instr[] instrs = t.a;
        Map<Integer, Label[]> jumpTable = t.b;
//        System.out.println("table: " + jumpTable);

//        System.out.println(method);
        for (int i = 0; i < instrs.length; i++) {
            Instr instr = instrs[i];
//            System.out.println(instr.getClass());
//            System.out.println(instr);
            if (jumpTable.get(i) != null) {
//                System.out.println("pc: " + i + " label: " + jumpTable.get(i));
                for (Label label : jumpTable.get(i)) method().mark(methodData().getLabel(label));
            }
            emit(instr);
        }

        popmethod();
    }

    public void emit(Instr instr) {
        instr.compile(this);
    }

    public void emit(Operand operand) {
        if (operand.hasKnownValue()) {
            operand.compile(this);
        } else if (operand instanceof Variable) {
            emitVariable((Variable)operand);
        } else {
            operand.compile(this);
        }
    }

    public void emitVariable(Variable variable) {
//        System.out.println("variable: " + variable);
        int index = methodData().local(variable);
//        System.out.println("index: " + index);
        method().loadLocal(index);
    }

    public void declareField(String field) {
        if (!clsData().fieldSet.contains(field)) {
            cls().visitField(ACC_PROTECTED, field, ci(Object.class), null, null);
            clsData().fieldSet.add(field);
        }
    }

    public static final Class OBJECT = IRubyObject.class;
    public static final Class THREADCONTEXT = ThreadContext.class;
    public static final Class STATICSCOPE = StaticScope.class;
    public static final Type OBJECT_TYPE = Type.getType(OBJECT);
    public static final Type THREADCONTEXT_TYPE = Type.getType(THREADCONTEXT);
    public static final Type STATICSCOPE_TYPE = Type.getType(STATICSCOPE);
}
