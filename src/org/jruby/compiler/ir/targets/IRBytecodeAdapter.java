/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.targets;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.jruby.RubyEncoding;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
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
public class IRBytecodeAdapter {
    public IRBytecodeAdapter(SkinnyMethodAdapter adapter, int arity, String... params) {
        this.adapter = adapter;
        this.arity = arity;
        this.params = params;
    }

    public void startMethod() {
        adapter.start();

        newLocal("context", JVM.THREADCONTEXT_TYPE);
    }

    public void endMethod() {
        adapter.end();
//        adapter.end(new Runnable() {
//            public void run() {
//                for (Map.Entry<Integer, Type> entry : fields.entrySet()) {
//                    int i = entry.getKey();
//                    String name;
//                    switch (i) {
//                        case 0:
//                            name = "context";
//                            break;
//                        case 1:
//                            name = "self";
//                            break;
//                        default:
//                            name = variables.get(i);
//                    }
//                    adapter.local(i, name, entry.getValue());
//                }
//            }
//        });
    }

    public void push(Long l) {
        adapter.aload(0);
        adapter.invokedynamic("fixnum", sig(JVM.OBJECT, ThreadContext.class), Bootstrap.fixnum(), l);
    }

    public void push(ByteList bl) {
        adapter.aload(0);
        adapter.invokedynamic("string", sig(JVM.OBJECT, ThreadContext.class), Bootstrap.string(), new String(bl.bytes(), RubyEncoding.ISO), bl.getEncoding().getIndex());
    }

    public void loadLocal(int i) {
        adapter.aload(i);
    }

    public void storeLocal(int i) {
        adapter.astore(i);
    }

    public void invokeOther(String name, int arity) {
        adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, arity)), Bootstrap.invoke());
    }

    public void invokeSelf(String name, int arity) {
        adapter.invokedynamic("invokeSelf:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, arity)), Bootstrap.invokeSelf());
    }

    public void invokeSuper(String name, int arity) {
        adapter.invokedynamic("invokeSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, arity)), new Handle(Opcodes.H_INVOKESTATIC, "dummy", "dummy", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/CallSite;"));
    }

    public void invokeOtherBoolean(String name, int arity) {
        adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(name), sig(boolean.class, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, arity)), new Handle(Opcodes.H_INVOKESTATIC, "dummy", "dummy", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/CallSite;"));
    }

    public void invokeSelfBoolean(String name, int arity) {
        adapter.invokedynamic("invokeSelf:" + JavaNameMangler.mangleMethodName(name), sig(boolean.class, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, arity)), new Handle(Opcodes.H_INVOKESTATIC, "dummy", "dummy", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/CallSite;"));
    }

    public void invokeVirtual(Type type, Method method) {
        adapter.invokevirtual(type.getInternalName(), method.getName(), method.getDescriptor());
    }

    public void goTo(org.objectweb.asm.Label label) {
        adapter.go_to(label);
    }

    public void isTrue() {
        adapter.invokeinterface(p(IRubyObject.class), "isTrue", sig(boolean.class));
    }

    public void isNil() {
        adapter.invokeinterface(p(IRubyObject.class), "isNil", sig(boolean.class));
    }

    public void bfalse(org.objectweb.asm.Label label) {
        adapter.iffalse(label);
    }

    public void btrue(org.objectweb.asm.Label label) {
        adapter.iffalse(label);
    }

    public void poll() {
        adapter.aload(0);
        adapter.invokevirtual(p(ThreadContext.class), "pollThreadEvents", sig(void.class));
    }

    public void pushNil() {
        adapter.aload(0);
        adapter.getfield(p(ThreadContext.class), "nil", ci(IRubyObject.class));
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

    public int newLocal(String name, Type type) {
        int index = fieldCount++;
        fields.put(index, type);
        variables.put(index, name);
        return index;
    }

    public org.objectweb.asm.Label newLabel() {
        return new org.objectweb.asm.Label();
    }
    public SkinnyMethodAdapter adapter;
    private int fieldCount = 0;
    private Map<Integer, Type> fields = new HashMap<Integer, Type>();
    private Map<Integer, String> variables = new HashMap<Integer, String>();
    private int arity;
    private String[] params;
}
