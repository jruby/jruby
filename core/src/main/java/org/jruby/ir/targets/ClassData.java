/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.IRScope;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.util.CodegenUtils;
import org.jruby.util.collections.IntHashMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassData {

    public ClassData(String clsName, ClassVisitor cls, JVMVisitor visitor) {
        this.clsName = clsName;
        this.cls = cls;
        this.visitor = visitor;
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

    public void pushmethod(String name, IRScope scope, String scopeField, Signature signature, boolean specificArity) {
        Method m = new Method(name, Type.getType(signature.type().returnType()), IRRuntimeHelpers.typesFromSignature(signature));
        SkinnyMethodAdapter adapter = new SkinnyMethodAdapter(cls, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, m.getName(), m.getDescriptor(), null, null);
        IRBytecodeAdapter method = new IRBytecodeAdapter(visitor.getBytecodeMode(), adapter, signature, this);
        methodStack.push(
                new MethodData(
                        method,
                        scope,
                        scopeField,
                        signature,
                        specificArity ? scope.getStaticScope().getSignature().required() : -1)
        );
    }

    public void popmethod() {
        method().endMethod();
        methodStack.pop();
    }

    public final ClassVisitor cls;
    public final JVMVisitor visitor;
    public final String clsName;
    private final Deque<MethodData> methodStack = new ArrayDeque<>(8);
    public final AtomicInteger cacheFieldCount = new AtomicInteger(0);
    public final IntHashMap<Void> arrayMethodsDefined = new IntHashMap<>(4, 1); // Set<int>
    public final IntHashMap<Void> hashMethodsDefined = new IntHashMap<>(4, 1); // Set<int>
    public final IntHashMap<Void> kwargsHashMethodsDefined = new IntHashMap<>(4, 1); // Set<int>
}
