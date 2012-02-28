package org.jruby.compiler.ir.targets;

import org.jruby.compiler.ir.IRScope;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.compiler.ir.Tuple;
import org.jruby.compiler.ir.CompilerTarget;
import org.jruby.compiler.ir.IRBuilder;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScriptBody;
import org.jruby.compiler.ir.compiler_pass.InlineTest;
import org.jruby.compiler.ir.compiler_pass.opts.DeadCodeElimination;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.CurrentModule;
import org.jruby.compiler.ir.operands.CurrentScope;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import static org.objectweb.asm.Opcodes.*;
import static org.jruby.util.CodegenUtils.*;

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
            
            IRScope scope = new IRBuilder(ruby.getIRManager()).buildRoot((RootNode) ast);
            
            // additional passes not enabled in builder yet
            if (deadCode) scope.runCompilerPass(new DeadCodeElimination());
            if (inlineName != null) {
                scope.runCompilerPass(new InlineTest(inlineName));
            }
            
            // run compiler
            CompilerTarget target = new JDK7();

            target.codegen(scope);
            
            Class compiled = ruby.getJRubyClassLoader().defineClass(scriptToClass(scope.getName()), target.code());
            try {
                compiled.getMethod("__script__", ThreadContext.class, IRubyObject.class).invoke(null, ruby.getCurrentContext(), ruby.getTopSelf());
            } catch (Exception e) {
                e.printStackTrace();
            }

            i++;
            
            if (commandLineScript != null) break;
        }
    }

    public static Class compile(Ruby ruby, Node ast, JRubyClassLoader jrubyClassLoader) {
        IRScope scope = new IRBuilder(ruby.getIRManager()).buildRoot((RootNode) ast);

        // additional passes not enabled in builder yet
        //scope.runCompilerPass(new DeadCodeElimination());

        // run compiler
        CompilerTarget target = new JDK7();

        target.codegen(scope);

        try {
            FileOutputStream fos = new FileOutputStream("tmp.class");
            fos.write(target.code());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        methodData().local("$self");//, JVM.OBJECT_TYPE);
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
    
    public void emitScope(IRScope scope, String name, int arity) {
        pushmethod(scope.getName(), arity);

        Tuple<Instr[], Map<Integer,Label>> t = scope.prepareForCompilation();
        Instr[] instrs = t.a;
        Map<Integer, Label> jumpTable = t.b;

//        System.out.println(method);
        for (int i = 0; i < instrs.length; i++) {
            Instr instr = instrs[i];
//            System.out.println(instr.getClass());
//            System.out.println(instr);
            if (methodData().getLabel(i) != null) {
                method().adapter.label(methodData().getLabel(i));
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
        } else if (operand instanceof CurrentScope) {
            method().adapter.aconst_null();
        } else if (operand instanceof CurrentModule) {
            method().adapter.aconst_null();
        } else {
            throw new RuntimeException("unsupported operand in compiler: " + operand.getClass());
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
    public static final Type OBJECT_TYPE = Type.getType(OBJECT);
    public static final Type THREADCONTEXT_TYPE = Type.getType(THREADCONTEXT);
}
