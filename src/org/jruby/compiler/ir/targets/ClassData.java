/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.targets;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

/**
 *
 * @author headius
 */
class ClassData {

    public ClassData(ClassVisitor cls) {
        this.cls = cls;
    }

    public IRBytecodeAdapter method() {
        return methodData().method;
    }

    public MethodData methodData() {
        return methodStack.peek();
    }

    public void pushmethod(String name, int arity) {
        Method m;
        switch (arity) {
            case 0:
                m = Method.getMethod("java.lang.Object " + name + " (org.jruby.runtime.ThreadContext, java.lang.Object)");
                break;
            case 1:
                m = Method.getMethod("java.lang.Object " + name + " (org.jruby.runtime.ThreadContext, java.lang.Object, java.lang.Object)");
                break;
            case 2:
                m = Method.getMethod("java.lang.Object " + name + " (org.jruby.runtime.ThreadContext, java.lang.Object, java.lang.Object, java.lang.Object)");
                break;
            case 3:
                m = Method.getMethod("java.lang.Object " + name + " (org.jruby.runtime.ThreadContext, java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)");
                break;
            default:
                throw new RuntimeException("Unsupported arity " + arity + " for " + name);
        }
        methodStack.push(new MethodData(new SkinnyMethodAdapter(cls, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, m.getName(), m.getDescriptor(), null, null), arity));
    }

    public void popmethod() {
        method().endMethod();
        methodStack.pop();
    }
    public ClassVisitor cls;
    Stack<MethodData> methodStack = new Stack();
    public Set<String> fieldSet = new HashSet<String>();
    
}
