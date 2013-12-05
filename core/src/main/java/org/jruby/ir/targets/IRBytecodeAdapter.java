/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.runtime.Helpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.HashMap;
import java.util.Map;

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
    }

    public void endMethod() {
        adapter.end(new Runnable() {
            public void run() {
                for (Map.Entry<Integer, Type> entry : variableTypes.entrySet()) {
                    int i = entry.getKey();
                    String name = variableNames.get(i);
                    adapter.local(i, name, entry.getValue());
                }
            }
        });
    }

    public void pushFixnum(Long l) {
        adapter.aload(0);
        adapter.invokedynamic("fixnum", sig(JVM.OBJECT, ThreadContext.class), Bootstrap.fixnum(), l);
    }

    public void pushFloat(Double d) {
        adapter.aload(0);
        adapter.invokedynamic("flote", sig(JVM.OBJECT, ThreadContext.class), Bootstrap.flote(), d);
    }

    public void pushString(ByteList bl) {
        adapter.aload(0);
        adapter.invokedynamic("string", sig(JVM.OBJECT, ThreadContext.class), Bootstrap.string(), new String(bl.bytes(), RubyEncoding.ISO), bl.getEncoding().getIndex());
    }

    /**
     * Push a symbol on the stack
     * @param sym the symbol's string identifier
     */
    public void pushSymbol(String sym) {
        adapter.aload(0);
        adapter.invokedynamic("symbol", sig(JVM.OBJECT, ThreadContext.class), Bootstrap.symbol(), sym);
    }

    public void loadRuntime() {
        adapter.aload(0);
        adapter.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
    }

    public void loadLocal(int i) {
        adapter.aload(i);
    }

    public void loadContext() {
        adapter.aload(0);
    }

    public void loadStaticScope() {
        adapter.aload(1);
    }

    public void loadSelf() {
        adapter.aload(2);
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

    public void invokeFixnumOp(String name, long value) {
        adapter.invokedynamic("fixnumOperator:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT)), Bootstrap.invokeFixnumOp(), value, "--dummy--", -1);
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

    public void attrAssign(String name) {
        adapter.invokedynamic("attrAssign:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, ThreadContext.class, JVM.OBJECT, JVM.OBJECT), Bootstrap.attrAssign());
    }

    public void invokeVirtual(Type type, Method method) {
        adapter.invokevirtual(type.getInternalName(), method.getName(), method.getDescriptor());
    }

    public void invokeStatic(Type type, Method method) {
        adapter.invokestatic(type.getInternalName(), method.getName(), method.getDescriptor());
    }

    public void invokeHelper(String name, Class... sig) {
        adapter.invokestatic(p(Helpers.class), name, sig(sig));
    }

    public void invokeHelper(String name, String sig) {
        adapter.invokestatic(p(Helpers.class), name, sig);
    }

    public void searchConst(String name) {
        adapter.invokedynamic("searchConst:" + name, sig(JVM.OBJECT, params(ThreadContext.class, StaticScope.class)), Bootstrap.searchConst());
    }

    public void inheritanceSearchConst(String name) {
        adapter.invokedynamic("inheritanceSearchConst:" + name, sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class)), Bootstrap.inheritanceSearchConst());
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
        adapter.iftrue(label);
    }

    public void poll() {
        adapter.aload(0);
        adapter.invokevirtual(p(ThreadContext.class), "pollThreadEvents", sig(void.class));
    }

    public void pushNil() {
        adapter.aload(0);
        adapter.getfield(p(ThreadContext.class), "nil", ci(IRubyObject.class));
    }

    public void pushBoolean(boolean b) {
        adapter.aload(0);
        adapter.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
        if (b) {
            adapter.invokevirtual(p(Ruby.class), "getTrue", sig(RubyBoolean.class));
        } else {
            adapter.invokevirtual(p(Ruby.class), "getFalse", sig(RubyBoolean.class));
        }
    }

    public void pushObjectClass() {
        loadRuntime();
        adapter.invokevirtual(p(Ruby.class), "getObject", sig(RubyClass.class));
    }

    public void pushUndefined() {
        adapter.getstatic(p(UndefinedValue.class), "UNDEFINED", ci(UndefinedValue.class));
    }

    public void pushHandle(String className, String methodName, int arity) {
        adapter.getMethodVisitor().visitLdcInsn(new Handle(Opcodes.H_INVOKESTATIC, className, methodName, ClassData.SIGS[arity]));
    }

    public void mark(org.objectweb.asm.Label label) {
        adapter.label(label);
    }

    public void putField(String name) {
        adapter.invokedynamic("ivarSet:" + JavaNameMangler.mangleMethodName(name), sig(void.class, IRubyObject.class, IRubyObject.class), Bootstrap.ivar());
    }

    public void getField(String name) {
        adapter.invokedynamic("ivarGet:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, IRubyObject.class), Bootstrap.ivar());
    }

    public void returnValue() {
        adapter.areturn();
    }

    public void array(int length) {
        adapter.invokedynamic("array", sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, length)), Bootstrap.array());
    }

    public int newLocal(String name, Type type) {
        int index = variableCount++;
        variableTypes.put(index, type);
        variableNames.put(index, name);
        return index;
    }

    public org.objectweb.asm.Label newLabel() {
        return new org.objectweb.asm.Label();
    }
    public SkinnyMethodAdapter adapter;
    private int variableCount = 0;
    private Map<Integer, Type> variableTypes = new HashMap<Integer, Type>();
    private Map<Integer, String> variableNames = new HashMap<Integer, String>();
    private int arity;
    private String[] params;
}
