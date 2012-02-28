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
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 *
 * @author headius
 */
class ClassData {

    public ClassData(String clsName, ClassVisitor cls) {
        this.clsName = clsName;
        this.cls = cls;
    }

    public IRBytecodeAdapter method() {
        return methodData().method;
    }

    public MethodData methodData() {
        return methodStack.peek();
    }

    private static final Type[][] PARAMS = new Type[][] {
            new Type[]{JVM.THREADCONTEXT_TYPE, JVM.OBJECT_TYPE},
            new Type[]{JVM.THREADCONTEXT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE},
            new Type[]{JVM.THREADCONTEXT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE},
            new Type[]{JVM.THREADCONTEXT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE}
    };

    public void pushmethod(String name, int arity) {
        Method m;
        switch (arity) {
            case 0:
            case 1:
            case 2:
            case 3:
                m = new Method(name, JVM.OBJECT_TYPE, PARAMS[arity]);
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
    public String clsName;
    Stack<MethodData> methodStack = new Stack();
    public Set<String> fieldSet = new HashSet<String>();
    
}
