/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.IRScope;
import org.jruby.util.CodegenUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author headius
 */
abstract class ClassData {

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

    public abstract void pushmethod(String name, IRScope scope, Signature signature, boolean specificArity);

    public void popmethod() {
        method().endMethod();
        methodStack.pop();
    }

    public ClassVisitor cls;
    public final String clsName;
    Stack<MethodData> methodStack = new Stack();
    public AtomicInteger callSiteCount = new AtomicInteger(0);
    public Set<Integer> arrayMethodsDefined = new HashSet();
    public Set<Integer> hashMethodsDefined = new HashSet();
    public Set<Integer> kwargsHashMethodsDefined = new HashSet();
    public Set<Integer> dregexpMethodsDefined = new HashSet();
}
