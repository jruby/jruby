/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.targets;

import java.util.HashMap;
import java.util.Map;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import static org.jruby.util.CodegenUtils.*;

/**
 *
 * @author headius
 */
class IRBytecodeAdapter {

    public IRBytecodeAdapter(SkinnyMethodAdapter adapter, int arity) {
        this.adapter = adapter;
        newLocal(Type.getType(ThreadContext.class));
        newLocal(Type.getType(Object.class));
        for (int i = 0; i < arity; i++) {
            newLocal(Type.getType(Object.class));
        }
    }

    public void startMethod() {
        adapter.start();
    }

    public void endMethod() {
        //            adapter.end();
        adapter.visitEnd();
    }

    public void push(Long l) {
        adapter.invokedynamic("fixnum", "()Ljava/lang/Object;", new Handle(Opcodes.H_INVOKESTATIC, "dummy", "dummy", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/CallSite;"), l);
    }

    public void loadLocal(int i) {
        adapter.aload(i);
    }

    public void storeLocal(int i) {
        adapter.astore(i);
    }

    public void invokeOther(String name, int arity) {
        adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(name), sig(Object.class, params(ThreadContext.class, Object.class, Object.class, arity)), new Handle(Opcodes.H_INVOKESTATIC, "dummy", "dummy", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/CallSite;"));
    }

    public void invokeSelf(String name, int arity) {
        adapter.invokedynamic("invokeSelf:" + JavaNameMangler.mangleMethodName(name), sig(Object.class, params(ThreadContext.class, Object.class, Object.class, arity)), new Handle(Opcodes.H_INVOKESTATIC, "dummy", "dummy", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/CallSite;"));
    }

    public void invokeSuper(String name, int arity) {
        adapter.invokedynamic("invokeSuper:" + JavaNameMangler.mangleMethodName(name), sig(Object.class, params(ThreadContext.class, Object.class, Object.class, arity)), new Handle(Opcodes.H_INVOKESTATIC, "dummy", "dummy", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/CallSite;"));
    }

    public void invokeOtherBoolean(String name, int arity) {
        adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(name), sig(boolean.class, params(ThreadContext.class, Object.class, Object.class, arity)), new Handle(Opcodes.H_INVOKESTATIC, "dummy", "dummy", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/CallSite;"));
    }

    public void invokeSelfBoolean(String name, int arity) {
        adapter.invokedynamic("invokeSelf:" + JavaNameMangler.mangleMethodName(name), sig(boolean.class, params(ThreadContext.class, Object.class, Object.class, arity)), new Handle(Opcodes.H_INVOKESTATIC, "dummy", "dummy", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/CallSite;"));
    }

    public void invokeVirtual(Type type, Method method) {
        adapter.invokevirtual(type.getInternalName(), method.getName(), method.getDescriptor());
    }

    public void goTo(org.objectweb.asm.Label label) {
        adapter.go_to(label);
    }

    public void mark(org.objectweb.asm.Label label) {
        adapter.label(label);
    }

    public void putField(Type type, String name, Type fieldType) {
        adapter.putfield(type.getInternalName(), name, fieldType.getDescriptor());
    }

    public void getField(Type type, String name, Type fieldType) {
        adapter.getfield(type.getInternalName(), name, fieldType.getDescriptor());
    }

    public void returnValue() {
        adapter.areturn();
    }

    public int newLocal(Type type) {
        int index = fieldCount++;
        fields.put(index, type);
        // TODO: declare local variable
        return index;
    }

    public org.objectweb.asm.Label newLabel() {
        return new org.objectweb.asm.Label();
    }
    public SkinnyMethodAdapter adapter;
    private int fieldCount = 0;
    private Map<Integer, Type> fields = new HashMap<Integer, Type>();
    
}
