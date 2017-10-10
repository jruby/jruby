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
import org.jruby.ir.instructions.ClosureAcceptingInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.NormalCachingCallSite;
import org.jruby.runtime.callsite.RefinedCachingCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;
import org.jruby.util.ByteList;
import org.jruby.util.JavaNameMangler;
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

    /**
     * Utility to lazily construct and cache a call site object.
     *
     * @param method the SkinnyMethodAdapter to that's generating the containing method body
     * @param className the name of the class in which the field will reside
     * @param siteName the unique name of the site, used for the field
     * @param rubyName the Ruby method name being invoked
     * @param callType the type of call
     * @param isPotentiallyRefined whether the call might be refined
     */
    public static void cacheCallSite(SkinnyMethodAdapter method, String className, String siteName, String rubyName, CallType callType, boolean isPotentiallyRefined) {
        // call site object field
        method.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, siteName, ci(CachingCallSite.class), null, null).visitEnd();

        // lazily construct it
        method.getstatic(className, siteName, ci(CachingCallSite.class));
        method.dup();
        Label doCall = new Label();
        method.ifnonnull(doCall);
        method.pop();
        method.ldc(rubyName);
        Class<? extends CachingCallSite> siteClass;
        String signature;
        if (isPotentiallyRefined) {
            siteClass = RefinedCachingCallSite.class;
            signature = sig(siteClass, String.class, String.class);
            method.ldc(callType.name());
        } else {
            switch (callType) {
                case NORMAL:
                    siteClass = NormalCachingCallSite.class;
                    break;
                case FUNCTIONAL:
                    siteClass = FunctionalCachingCallSite.class;
                    break;
                case VARIABLE:
                    siteClass = VariableCachingCallSite.class;
                    break;
                default:
                    throw new RuntimeException("BUG: Unexpected call type " + callType + " in JVM6 invoke logic");
            }
            signature = sig(siteClass, String.class);
        }
        method.invokestatic(p(IRRuntimeHelpers.class), "new" + siteClass.getSimpleName(), signature);
        method.dup();
        method.putstatic(className, siteName, ci(CachingCallSite.class));

        method.label(doCall);
    }

    public String getUniqueSiteName(String name) {
        return "invokeOther" + getClassData().callSiteCount.getAndIncrement() + ":" + JavaNameMangler.mangleMethodName(name);
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

    public void loadSelfBlock() {
        adapter.aload(signature.argOffset(JVMVisitor.SELF_BLOCK_NAME));
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
        adapter.aload(signature.argOffset(JVMVisitor.BLOCK_ARG_NAME));
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

    public void storeSelf() {
        adapter.astore(signature.argOffset("self"));
    }

    public void storeArgs() {
        adapter.astore(signature.argOffset("args"));
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
    public abstract void pushString(ByteList bl, int cr);

    /**
     * Stack required: none
     *
     * @param bl ByteList for the String to push
     */
    public abstract void pushFrozenString(ByteList bl, int cr, String path, int line);

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
     * @param bytes the ByteList for the symbol
     */
    public abstract void pushSymbol(ByteList bytes);

    /**
     * Push a Symbol.to_proc on the stack.
     *
     * Stack required: none
     *
     * @param name the symbol's string identifier
     * @param encoding the symbol's encoding
     */
    public abstract void pushSymbolProc(String name, Encoding encoding);

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
     * @param blockPassType what type of closure is passed
     */
    public abstract void invokeOther(String file, int line, String name, int arity, BlockPassType blockPassType, boolean isPotentiallyRefined);

    /**
     * Invoke the array dereferencing method ([]) on an object other than self.
     *
     * If this invokes against a Hash with a frozen string, it will follow an optimized path.
     *
     * Stack required: context, self, target, arg0
     * @param file
     * @param line
     */
    public abstract void invokeArrayDeref(String file, int line);

    /**
     * Invoke a fixnum-receiving method on an object other than self.
     *
     * Stack required: context, self, receiver (fixnum will be handled separately)
     *
     * @param name name of the method to invoke
     */
    public abstract void invokeOtherOneFixnum(String file, int line, String name, long fixnum, CallType callType);

    /**
     * Invoke a float-receiving method on an object other than self.
     *
     * Stack required: context, self, receiver (float will be handled separately)
     *
     * @param name name of the method to invoke
     */
    public abstract void invokeOtherOneFloat(String file, int line, String name, double flote, CallType callType);

    public enum BlockPassType {
        NONE(false, false),
        GIVEN(true, false),
        LITERAL(true, true);

        private final boolean given;
        private final boolean literal;

        BlockPassType(boolean given, boolean literal) {
            this.given = given;
            this.literal = literal;
        }

        public boolean given() {
            return given;
        }
        public boolean literal() {
            return literal;
        }
        public static BlockPassType fromIR(ClosureAcceptingInstr callInstr) {
            Operand closure = callInstr.getClosureArg();
            return closure != null ? ( callInstr.hasLiteralClosure() ? BlockPassType.LITERAL : BlockPassType.GIVEN) : BlockPassType.NONE;
        }
    }

    /**
     * Invoke a method on self.
     *
     * Stack required: context, caller, self, all arguments, optional block
     *
     * @param file the filename of the script making this call
     * @param line the line number where this call appears
     * @param name name of the method to invoke
     * @param arity arity of the call
     * @param blockPassType what type of closure is passed
     * @param callType
     */
    public abstract void invokeSelf(String file, int line, String name, int arity, BlockPassType blockPassType, CallType callType, boolean isPotentiallyRefined);

    /**
     * Invoke a superclass method from an instance context.
     *
     * Stack required: context, caller, self, start class, arguments[, block]
     *
     * @param file the filename of the script making this call
     * @param line the line number where this call appears
     * @param name name of the method to invoke
     * @param arity arity of the arguments on the stack
     * @param hasClosure whether a block is passed
     * @param splatmap a map of arguments to be splatted back into arg list
     */
    public abstract void invokeInstanceSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap);

    /**
     * Invoke a superclass method from a class context.
     *
     * Stack required: context, caller, self, start class, arguments[, block]
     *
     * @param file the filename of the script making this call
     * @param line the line number where this call appears
     * @param name name of the method to invoke
     * @param arity arity of the arguments on the stack
     * @param hasClosure whether a block is passed
     * @param splatmap a map of arguments to be splatted back into arg list
     */
    public abstract void invokeClassSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap);

    /**
     * Invoke a superclass method from an unresolved context.
     *
     * Stack required: context, caller, self, arguments[, block]
     *
     * @param file the filename of the script making this call
     * @param line the line number where this call appears
     * @param name name of the method to invoke
     * @param arity arity of the arguments on the stack
     * @param hasClosure whether a block is passed
     * @param splatmap a map of arguments to be splatted back into arg list
     */
    public abstract void invokeUnresolvedSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap);

    /**
     * Invoke a superclass method from a zsuper in a block.
     *
     * Stack required: context, caller, self, arguments[, block]
     *
     * @param file the filename of the script making this call
     * @param line the line number where this call appears
     * @param name name of the method to invoke
     * @param arity arity of the arguments on the stack
     * @param hasClosure whether a block is passed
     * @param splatmap a map of arguments to be splatted back into arg list
     */
    public abstract void invokeZSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap);

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
     * Lookup a constant from current module.
     *
     * Stack required: context, static scope
     *
     * @param name name of the constant
     * @param noPrivateConsts whether to ignore private constants
     */
    public abstract void searchModuleForConst(String name, boolean noPrivateConsts);

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

    /**
     * Retrieve a global variable with the given name.
     *
     * Stack required: none
     */
    public abstract void getGlobalVariable(String name, String file, int line);

    /**
     * Set the global variable with the given name to the value on stack.
     *
     * Stack required: the new value
     */
    public abstract void setGlobalVariable(String name, String file, int line);

    /**
     * Yield argument list to a block.
     *
     * Stack required: context, block, argument
     */
    public abstract void yield(boolean unwrap);

    /**
     * Yield to a block.
     *
     * Stack required: context, block
     */
    public abstract void yieldSpecific();

    /**
     * Yield a number of flat arguments to a block.
     *
     * Stack required: context, block
     */
    public abstract void yieldValues(int arity);

    /**
     * Prepare a block for a subsequent call.
     *
     * Stack required: context, self, dynamicScope
     */
    public abstract void prepareBlock(Handle handle, org.jruby.runtime.Signature signature, String className);

    /**
     * Perform a === call appropriate for a case/when statement.
     *
     * Stack required: context, case value, when value
     */
    public abstract void callEqq(boolean isSplattedValue);

    public SkinnyMethodAdapter adapter;
    private int variableCount = 0;
    private Map<Integer, Type> variableTypes = new HashMap<Integer, Type>();
    private Map<Integer, String> variableNames = new HashMap<Integer, String>();
    protected final Signature signature;
    private final ClassData classData;
    public int ipc = 0;  // counter for dumping instr index when in DEBUG
}
