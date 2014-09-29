package org.jruby.ir.targets;

import org.jruby.RubyInstanceConfig;
import org.objectweb.asm.ClassWriter;

import static org.jruby.util.CodegenUtils.p;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;

// This class represents JDK6 as the compiler target
// JDK6 has no support for invokedynamic
public class JVM6 extends JVM {
    public void pushscript(String clsName, String filename) {
        writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        clsStack.push(new ClassData6(clsName, writer));

        cls().visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, clsName, null, p(Object.class), null);
        cls().visitSource(filename, null);
    }
}
