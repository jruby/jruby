/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.util.CodegenUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

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

    public static final Type[][] PARAMS = new Type[][] {
        new Type[]{JVM.THREADCONTEXT_TYPE, JVM.OBJECT_TYPE, JVM.BLOCK_TYPE},
        new Type[]{JVM.THREADCONTEXT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.BLOCK_TYPE},
        new Type[]{JVM.THREADCONTEXT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.BLOCK_TYPE},
        new Type[]{JVM.THREADCONTEXT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.BLOCK_TYPE}
    };

    public static final Type[][] ARGS = new Type[][] {
        new Type[]{JVM.THREADCONTEXT_TYPE, JVM.STATICSCOPE_TYPE, JVM.OBJECT_TYPE, JVM.BLOCK_TYPE},
        new Type[]{JVM.THREADCONTEXT_TYPE, JVM.STATICSCOPE_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.BLOCK_TYPE},
        new Type[]{JVM.THREADCONTEXT_TYPE, JVM.STATICSCOPE_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.BLOCK_TYPE},
        new Type[]{JVM.THREADCONTEXT_TYPE, JVM.STATICSCOPE_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_TYPE, JVM.BLOCK_TYPE}
    };

    public static final Type[] VARARGS =
            new Type[]{JVM.THREADCONTEXT_TYPE, JVM.STATICSCOPE_TYPE, JVM.OBJECT_TYPE, JVM.OBJECT_ARRAY_TYPE, JVM.BLOCK_TYPE};

    public static final String[] SIGS = new String[] {
        CodegenUtils.sig(JVM.OBJECT, JVM.THREADCONTEXT, JVM.STATICSCOPE, JVM.OBJECT, JVM.BLOCK),
        CodegenUtils.sig(JVM.OBJECT, JVM.THREADCONTEXT, JVM.STATICSCOPE, JVM.OBJECT, JVM.OBJECT, JVM.BLOCK),
        CodegenUtils.sig(JVM.OBJECT, JVM.THREADCONTEXT, JVM.STATICSCOPE, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, JVM.BLOCK),
        CodegenUtils.sig(JVM.OBJECT, JVM.THREADCONTEXT, JVM.STATICSCOPE, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, JVM.BLOCK)
    };

    public static final String VARARGS_SIG =
            CodegenUtils.sig(JVM.OBJECT, JVM.THREADCONTEXT, JVM.STATICSCOPE, JVM.OBJECT, JVM.OBJECT_ARRAY, JVM.BLOCK);

    private static final Type[] typesFromSignature(Signature signature) {
        Type[] types = new Type[signature.argCount()];
        for (int i = 0; i < signature.argCount(); i++) {
            types[i] = Type.getType(signature.argType(i));
        }
        return types;
    }

    public void pushmethod(String name, int arity) {
        Method m;
        switch (arity) {
            case 0:
            case 1:
            case 2:
            case 3:
                m = new Method(name, JVM.OBJECT_TYPE, ARGS[arity]);
                break;
            default:
                throw new RuntimeException("Unsupported arity " + arity + " for " + name);
        }
        methodStack.push(new MethodData(new SkinnyMethodAdapter(cls, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, m.getName(), m.getDescriptor(), null, null), arity));
    }

    public void pushmethodVarargs(String name) {
        Method m = new Method(name, JVM.OBJECT_TYPE, VARARGS);
        methodStack.push(new MethodData(new SkinnyMethodAdapter(cls, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, m.getName(), m.getDescriptor(), null, null), -1));
    }

    public void pushmethod(String name, Signature signature) {
        Method m = new Method(name, Type.getType(signature.type().returnType()), typesFromSignature(signature));
        methodStack.push(new MethodData(new SkinnyMethodAdapter(cls, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, m.getName(), m.getDescriptor(), null, null), -1));
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
