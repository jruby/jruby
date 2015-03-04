/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.jruby.util.CodegenUtils.*;

/**
 *
 * @author headius
 */
public abstract class IRBytecodeAdapter {
    public static final int MAX_ARGUMENTS = 250;

    public IRBytecodeAdapter(SkinnyMethodAdapter adapter, Signature signature, ClassData classData) {
        this.adapter = adapter;
        this.signature = signature;
        this.classData = classData;
    }

    public ClassData getClassData() {
        return classData;
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

    public void loadLocal(int i) {
        adapter.aload(i);
    }

    public void loadContext() {
        adapter.aload(signature.argOffset("context"));
    }

    public void loadStaticScope() {
        adapter.aload(signature.argOffset("scope"));
    }

    public void loadSelf() {
        adapter.aload(signature.argOffset("self"));
    }

    public void loadArgs() {
        adapter.aload(signature.argOffset("args"));
    }

    public void loadBlock() {
        adapter.aload(signature.argOffset("block"));
    }

    public void loadFrameClass() {
        // when present, should be second-to-last element in signature
        adapter.aload(signature.argCount() - 2);
    }

    public void loadFrameName() {
        // when present, should be second-to-last element in signature
        adapter.aload(signature.argCount() - 1);
    }

    public void loadSuperName() {
        adapter.aload(5);
    }

    public void loadBlockType() {
        if (signature.argOffset("type") == -1) {
            adapter.aconst_null();
        } else {
            adapter.aload(signature.argOffset("type"));
        }
    }

    public void storeLocal(int i) {
        adapter.astore(i);
    }

    public void invokeVirtual(Type type, Method method) {
        adapter.invokevirtual(type.getInternalName(), method.getName(), method.getDescriptor());
    }

    public void invokeStatic(Type type, Method method) {
        adapter.invokestatic(type.getInternalName(), method.getName(), method.getDescriptor());
    }

    public void invokeHelper(String name, String sig) {
        adapter.invokestatic(p(Helpers.class), name, sig);
    }

    public void invokeHelper(String name, Class... x) {
        adapter.invokestatic(p(Helpers.class), name, sig(x));
    }

    public void invokeIRHelper(String name, String sig) {
        adapter.invokestatic(p(IRRuntimeHelpers.class), name, sig);
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
        loadContext();
        adapter.invokevirtual(p(ThreadContext.class), "pollThreadEvents", sig(void.class));
    }

    public void pushObjectClass() {
        loadRuntime();
        adapter.invokevirtual(p(Ruby.class), "getObject", sig(RubyClass.class));
    }

    public void pushUndefined() {
        adapter.getstatic(p(UndefinedValue.class), "UNDEFINED", ci(UndefinedValue.class));
    }

    public void pushHandle(Handle handle) {
        adapter.getMethodVisitor().visitLdcInsn(handle);
    }

    public void mark(org.objectweb.asm.Label label) {
        adapter.label(label);
    }

    public void returnValue() {
        adapter.areturn();
    }

    public int newLocal(String name, Type type) {
        int index = variableCount++;
        if (type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE) {
            variableCount++;
        }
        variableTypes.put(index, type);
        variableNames.put(index, name);
        return index;
    }

    public org.objectweb.asm.Label newLabel() {
        return new org.objectweb.asm.Label();
    }

    public void pushBlockBody(Handle handle, org.jruby.runtime.Signature signature, String className) {
        // FIXME: too much bytecode
        String cacheField = "blockBody" + getClassData().callSiteCount.getAndIncrement();
        Label done = new Label();
        adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, cacheField, ci(CompiledIRBlockBody.class), null, null).visitEnd();
        adapter.getstatic(getClassData().clsName, cacheField, ci(CompiledIRBlockBody.class));
        adapter.dup();
        adapter.ifnonnull(done);
        {
            adapter.pop();
            adapter.newobj(p(CompiledIRBlockBody.class));
            adapter.dup();

            adapter.ldc(handle);
            adapter.getstatic(className, handle.getName() + "_IRScope", ci(IRScope.class));
            adapter.ldc(signature.encode());

            adapter.invokespecial(p(CompiledIRBlockBody.class), "<init>", sig(void.class, java.lang.invoke.MethodHandle.class, IRScope.class, long.class));
            adapter.dup();
            adapter.putstatic(getClassData().clsName, cacheField, ci(CompiledIRBlockBody.class));
        }
        adapter.label(done);
    }

    /**
     * Stack required: none
     *
     * @param l long value to push as a Fixnum
     */
    public abstract void pushFixnum(long l);

    /**
     * Stack required: none
     *
     * @param d double value to push as a Float
     */
    public abstract void pushFloat(double d);

    /**
     * Stack required: none
     *
     * @param bl ByteList for the String to push
     */
    public abstract void pushString(ByteList bl);

    /**
     * Stack required: none
     *
     * @param bl ByteList for the String to push
     */
    public abstract void pushFrozenString(ByteList bl);

    /**
     * Stack required: none
     *
     * @param bl ByteList to push
     */
    public abstract void pushByteList(ByteList bl);

    /**
     * Build and save a literal regular expression.
     *
     * Stack required: none
     *
     * @param options options for the regexp
     */
    public abstract void pushRegexp(ByteList source, int options);

    /**
     * Build a dynamic regexp.
     *
     * No stack requirement. The callback must push onto this method's stack the ThreadContext and all arguments for
     * building the dregexp, matching the given arity.
     *
     * @param options options for the regexp
     * @param arity number of Strings passed in
     */
    public abstract void pushDRegexp(Runnable callback, RegexpOptions options, int arity);

    /**
     * Push a symbol on the stack.
     *
     * Stack required: none
     *
     * @param sym the symbol's string identifier
     */
    public abstract void pushSymbol(String sym, Encoding encoding);

    /**
     * Push the JRuby runtime on the stack.
     *
     * Stack required: none
     */
    public abstract void loadRuntime();

    /**
     * Push an encoding on the stack.
     *
     * Stack required: none
     *
     * @param encoding the encoding to push
     */
    public abstract void pushEncoding(Encoding encoding);

    /**
     * Invoke a method on an object other than self.
     *
     * Stack required: context, self, all arguments, optional block
     *
     * @param name name of the method to invoke
     * @param arity arity of the call
     * @param hasClosure whether a closure will be on the stack for passing
     */
    public abstract void invokeOther(String name, int arity, boolean hasClosure);

    /**
     * Invoke a fixnum-receiving method on an object other than self.
     *
     * Stack required: context, self, receiver (fixnum will be handled separately)
     *
     * @param name name of the method to invoke
     */
    public abstract void invokeOtherOneFixnum(String name, long fixnum);

    /**
     * Invoke a float-receiving method on an object other than self.
     *
     * Stack required: context, self, receiver (float will be handled separately)
     *
     * @param name name of the method to invoke
     */
    public abstract void invokeOtherOneFloat(String name, double flote);


    /**
     * Invoke a method on self.
     *
     * Stack required: context, caller, self, all arguments, optional block
     *
     * @param name name of the method to invoke
     * @param arity arity of the call
     * @param hasClosure whether a closure will be on the stack for passing
     * @param callType
     */
    public abstract void invokeSelf(String name, int arity, boolean hasClosure, CallType callType);

    /**
     * Invoke a superclass method from an instance context.
     *
     * Stack required: context, caller, self, start class, arguments[, block]
     *
     * @param name name of the method to invoke
     * @param arity arity of the arguments on the stack
     * @param hasClosure whether a block is passed
     * @param splatmap a map of arguments to be splatted back into arg list
     */
    public abstract void invokeInstanceSuper(String name, int arity, boolean hasClosure, boolean[] splatmap);

    /**
     * Invoke a superclass method from a class context.
     *
     * Stack required: context, caller, self, start class, arguments[, block]
     *
     * @param name name of the method to invoke
     * @param arity arity of the arguments on the stack
     * @param hasClosure whether a block is passed
     * @param splatmap a map of arguments to be splatted back into arg list
     */
    public abstract void invokeClassSuper(String name, int arity, boolean hasClosure, boolean[] splatmap);

    /**
     * Invoke a superclass method from an unresolved context.
     *
     * Stack required: context, caller, self, arguments[, block]
     *
     * @param name name of the method to invoke
     * @param arity arity of the arguments on the stack
     * @param hasClosure whether a block is passed
     * @param splatmap a map of arguments to be splatted back into arg list
     */
    public abstract void invokeUnresolvedSuper(String name, int arity, boolean hasClosure, boolean[] splatmap);

    /**
     * Invoke a superclass method from a zsuper in a block.
     *
     * Stack required: context, caller, self, arguments[, block]
     *
     * @param name name of the method to invoke
     * @param arity arity of the arguments on the stack
     * @param hasClosure whether a block is passed
     * @param splatmap a map of arguments to be splatted back into arg list
     */
    public abstract void invokeZSuper(String name, int arity, boolean hasClosure, boolean[] splatmap);

    /**
     * Lookup a constant from current context.
     *
     * Stack required: context, static scope
     *
     * @param name name of the constant
     * @param noPrivateConsts whether to ignore private constants
     */
    public abstract void searchConst(String name, boolean noPrivateConsts);

    /**
     * Lookup a constant from a given class or module.
     *
     * Stack required: context, module
     *
     * @param name name of the constant
     * @param noPrivateConsts whether to ignore private constants
     */
    public abstract void inheritanceSearchConst(String name, boolean noPrivateConsts);

    /**
     * Lookup a constant from a lexical scope.
     *
     * Stack required: context, static scope
     *
     * @param name name of the constant
     */
    public abstract void lexicalSearchConst(String name);

    /**
     * Load nil onto the stack.
     *
     * Stack required: none
     */
    public abstract void pushNil();

    /**
     * Load a boolean onto the stack.
     *
     * Stack required: none
     *
     * @param b the boolean to push
     */
    public abstract void pushBoolean(boolean b);

    /**
     * Load a Bignum onto the stack.
     *
     * Stack required: none
     *
     * @param bigint the value of the Bignum to push
     */
    public abstract void pushBignum(BigInteger bigint);

    /**
     * Store instance variable into self.
     *
     * Stack required: self, value
     * Stack result: empty
     *
     * @param name name of variable to store
     */
    public abstract void putField(String name);

    /**
     * Load instance variable from self.
     *
     * Stack required: self
     * Stack result: value from self
     *
     * @param name name of variable to load
     */
    public abstract void getField(String name);

    /**
     * Construct an Array from elements on stack.
     *
     * Stack required: all elements of array
     *
     * @param length number of elements
     */
    public abstract void array(int length);

    /**
     * Construct a Hash from elements on stack.
     *
     * Stack required: context, all elements of hash
     *
     * @param length number of element pairs
     */
    public abstract void hash(int length);

    /**
     * Construct a Hash based on keyword arguments pasesd to this method, for use in zsuper
     *
     * Stack required: context, kwargs hash to dup, remaining elements of hash
     *
     * @param length number of element pairs
     */
    public abstract void kwargsHash(int length);

    /**
     * Perform a thread event checkpoint.
     *
     * Stack required: none
     */
    public abstract void checkpoint();

    public SkinnyMethodAdapter adapter;
    private int variableCount = 0;
    private Map<Integer, Type> variableTypes = new HashMap<Integer, Type>();
    private Map<Integer, String> variableNames = new HashMap<Integer, String>();
    private final Signature signature;
    private final ClassData classData;
}
