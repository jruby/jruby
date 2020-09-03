package org.jruby.ir.targets;

import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.io.PrintWriter;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;

// This class represents JDK7 as the compiler target
// JDK7 supports invokedynamic for example
public class JVM7 extends JVM {
    public void pushscript(String clsName, String filename) {
        writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        clsStack.push(new ClassData7(clsName, writer));

        cls().visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, clsName, null, p(Object.class), null);
        cls().visitSource(filename, null);

        // ensure all classes used in jitted method signatures are forced to load (jruby/jruby#6280)
        SkinnyMethodAdapter adapter = new SkinnyMethodAdapter(cls(), ACC_PUBLIC | ACC_STATIC, "<clinit>", sig(void.class), null, null);
        adapter.start();
        adapter.ldc(Type.getObjectType(p(String.class)));
        adapter.ldc(Type.getObjectType(p(RubyModule.class)));
        adapter.pop2();
        adapter.ldc(Type.getObjectType(p(Block.class)));
        adapter.ldc(Type.getObjectType(p(IRubyObject.class)));
        adapter.pop2();
        adapter.ldc(Type.getObjectType(p(StaticScope.class)));
        adapter.ldc(Type.getObjectType(p(ThreadContext.class)));
        adapter.pop2();
        adapter.ldc(Type.getObjectType(p(IRubyObject[].class)));
        adapter.ldc(Type.getObjectType(p(DynamicScope.class)));
        adapter.pop2();
        adapter.voidreturn();
        adapter.end();
    }
}
