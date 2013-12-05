package org.jruby.ir.targets;

import org.jruby.RubyInstanceConfig;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.objectweb.asm.Opcodes.*;

// This class represents JVM as the target of compilation
// and outputs bytecode
public class JVM {
    private static final Logger LOG = LoggerFactory.getLogger("IRBuilder");

    Stack<ClassData> clsStack = new Stack();
    ClassWriter writer;

    public JVM() {
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

    public void pushscript(String clsName, String filename) {
        PrintWriter pw = new PrintWriter(System.out);
        writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        clsStack.push(new ClassData(clsName, writer));

        cls().visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, clsName, null, p(Object.class), null);
        cls().visitSource(filename, null);
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
        methodData().local("$block", Type.getType(Block.class));

        // TODO: this should go into the PushBinding instruction
        methodData().local("$dynamicScope");
    }

    public void popmethod() {
        clsData().popmethod();
    }

    public static String scriptToClass(String name) {
        if (name.equals("-e")) {
            return "DashE";
        } else {
            return JavaNameMangler.mangledFilenameForStartupClasspath(name);
        }
    }

    public void declareField(String field) {
        if (!clsData().fieldSet.contains(field)) {
            cls().visitField(ACC_PROTECTED, field, ci(Object.class), null, null);
            clsData().fieldSet.add(field);
        }
    }

    public static final Class OBJECT = IRubyObject.class;
    public static final Class BLOCK = Block.class;
    public static final Class THREADCONTEXT = ThreadContext.class;
    public static final Class STATICSCOPE = StaticScope.class;
    public static final Type OBJECT_TYPE = Type.getType(OBJECT);
    public static final Type BLOCK_TYPE = Type.getType(BLOCK);
    public static final Type THREADCONTEXT_TYPE = Type.getType(THREADCONTEXT);
    public static final Type STATICSCOPE_TYPE = Type.getType(STATICSCOPE);
}
