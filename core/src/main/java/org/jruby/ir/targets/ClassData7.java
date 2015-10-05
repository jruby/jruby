/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.IRScope;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 *
 * @author headius
 */
class ClassData7 extends ClassData {

    public ClassData7(String clsName, ClassVisitor cls) {
        super(clsName, cls);
    }

    public void pushmethod(String name, IRScope scope, Signature signature, boolean specificArity) {
        Method m = new Method(name, Type.getType(signature.type().returnType()), IRRuntimeHelpers.typesFromSignature(signature));
        SkinnyMethodAdapter adapter = new SkinnyMethodAdapter(cls, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, m.getName(), m.getDescriptor(), null, null);
        methodStack.push(
                new MethodData(
                        new IRBytecodeAdapter7(adapter, signature, this),
                        scope,
                        signature,
                        specificArity ? scope.getStaticScope().getSignature().required() : -1)
        );
    }

}
