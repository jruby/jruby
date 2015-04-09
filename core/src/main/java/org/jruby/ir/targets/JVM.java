package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.ir.IRScope;
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
import java.util.Stack;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.objectweb.asm.Opcodes.*;

// This class represents JVM as the target of compilation
// and outputs bytecode
public abstract class JVM {
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

    public abstract void pushscript(String clsName, String filename);

    public void popclass() {
        clsStack.pop();
    }

    public IRBytecodeAdapter method() {
        return clsData().method();
    }

    public void pushmethod(String name, IRScope scope, Signature signature, boolean specificArity) {
        clsData().pushmethod(name, scope, signature, specificArity);
        method().startMethod();
    }

    public void popmethod() {
        clsData().popmethod();
    }

    public static String scriptToClass(String name) {
        if (name == null || name.length() == 0) {
            return "NullFilename";
        } else if (name.equals("-e")) {
            return "DashE";
        } else {
            return JavaNameMangler.mangledFilenameForStartupClasspath(name);
        }
    }

    public static final Class OBJECT = IRubyObject.class;
    public static final Class OBJECT_ARRAY = IRubyObject[].class;
    public static final Class BLOCK = Block.class;
    public static final Class THREADCONTEXT = ThreadContext.class;
    public static final Class STATICSCOPE = StaticScope.class;
    public static final Class RUBY_MODULE = RubyModule.class;
    public static final Type OBJECT_TYPE = Type.getType(OBJECT);
    public static final Type OBJECT_ARRAY_TYPE = Type.getType(OBJECT_ARRAY);
    public static final Type BOOLEAN_TYPE = Type.BOOLEAN_TYPE;
    public static final Type DOUBLE_TYPE = Type.DOUBLE_TYPE;
    public static final Type LONG_TYPE = Type.LONG_TYPE;
    public static final Type BLOCK_TYPE = Type.getType(BLOCK);
    public static final Type THREADCONTEXT_TYPE = Type.getType(THREADCONTEXT);
    public static final Type STATICSCOPE_TYPE = Type.getType(STATICSCOPE);
    public static final Type RUBY_MODULE_TYPE = Type.getType(RUBY_MODULE);
}
