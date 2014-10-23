/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.math.BigInteger;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

/**
 * Java 6 and lower-compatible version of bytecode adapter for IR JIT.
 *
 * CON FIXME: These are all dirt-stupid impls that will not be as efficient.
 */
public class IRBytecodeAdapter6 extends IRBytecodeAdapter{
    public IRBytecodeAdapter6(SkinnyMethodAdapter adapter, Signature signature, ClassData classData) {
        super(adapter, signature, classData);
    }

    public void pushFixnum(Long l) {
        loadRuntime();
        adapter.ldc(l);
        adapter.invokevirtual(p(Ruby.class), "newFixnum", sig(RubyFixnum.class, long.class));
    }

    public void pushFloat(Double d) {
        loadRuntime();
        adapter.ldc(d);
        adapter.invokevirtual(p(Ruby.class), "newFloat", sig(RubyFloat.class, double.class));
    }

    public void pushString(ByteList bl) {
        loadRuntime();
        adapter.ldc(bl.toString());
        adapter.ldc(bl.getEncoding().toString());
        invokeIRHelper("newStringFromRaw", sig(RubyString.class, Ruby.class, String.class, String.class));
    }

    public void pushByteList(ByteList bl) {
        loadRuntime();
        adapter.ldc(bl.toString());
        adapter.ldc(bl.getEncoding().toString());
        invokeIRHelper("newByteListFromRaw", sig(ByteList.class, Ruby.class, String.class, String.class));
    }

    public void pushRegexp(int options) {
        adapter.pushInt(options);
        invokeIRHelper("constructRubyRegexp", sig(RubyRegexp.class, ThreadContext.class, RubyString.class, int.class));
    }

    public void pushSymbol(String sym) {
        loadRuntime();
        adapter.ldc(sym);
        adapter.invokevirtual(p(Ruby.class), "newSymbol", sig(RubySymbol.class, String.class));
    }

    public void loadRuntime() {
        loadContext();
        adapter.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
    }

    public void pushEncoding(Encoding encoding) {
        loadContext();
        adapter.ldc(encoding.toString());
        invokeIRHelper("retrieveEncoding", sig(RubyEncoding.class, ThreadContext.class, String.class));
    }

    public void invokeOther(String name, int arity, boolean hasClosure) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + name + "' has more than " + MAX_ARGUMENTS + " arguments");

        SkinnyMethodAdapter adapter2;
        String incomingSig;
        String outgoingSig;

        if (hasClosure) {
            switch (arity) {
                default:
                case -1:
                    incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class));
                    outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class));
                    break;
                case 0:
                case 1:
                case 2:
                case 3:
                    incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity, Block.class));
                    outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity, Block.class));
                    break;
            }
        } else {
            switch (arity) {
                default:
                case -1:
                    incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY));
                    outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY));
                    break;
                case 0:
                case 1:
                case 2:
                case 3:
                    incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity));
                    outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity));
                    break;
            }
        }

        String methodName = "invokeOther" + getClassData().callSiteCount.getAndIncrement() + ":" + JavaNameMangler.mangleMethodName(name);

        adapter2 = new SkinnyMethodAdapter(
                adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        // call site object field
        adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, ci(CachingCallSite.class), null, null).visitEnd();

        // lazily construct it
        adapter2.getstatic(getClassData().clsName, methodName, ci(CachingCallSite.class));
        adapter2.dup();
        Label doCall = new Label();
        adapter2.ifnonnull(doCall);
        adapter2.pop();
        adapter2.newobj(p(FunctionalCachingCallSite.class));
        adapter2.dup();
        adapter2.ldc(name);
        adapter2.invokespecial(p(FunctionalCachingCallSite.class), "<init>", sig(void.class, String.class));
        adapter2.dup();
        adapter2.putstatic(getClassData().clsName, methodName, ci(CachingCallSite.class));

        // use call site to invoke
        adapter2.label(doCall);
        adapter2.aload(0); // context
        adapter2.aload(1); // caller
        adapter2.aload(2); // self

        switch (arity) {
            case -1:
            case 1:
                adapter2.aload(3);
                if (hasClosure) adapter2.aload(4);
                break;
            case 0:
                if (hasClosure) adapter2.aload(3);
                break;
            case 2:
                adapter2.aload(3);
                adapter2.aload(4);
                if (hasClosure) adapter2.aload(5);
                break;
            case 3:
                adapter2.aload(3);
                adapter2.aload(4);
                adapter2.aload(5);
                if (hasClosure) adapter2.aload(6);
                break;
            default:
                buildArrayFromLocals(adapter2, 3, arity);
                if (hasClosure) adapter2.aload(3 + arity);
                break;
        }

        adapter2.invokevirtual(p(CachingCallSite.class), "call", outgoingSig);
        adapter2.areturn();
        adapter2.end();

        // now call it
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
    }

    public static void buildArrayFromLocals(SkinnyMethodAdapter adapter2, int base, int arity) {
        if (arity == 0) {
            adapter2.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
            return;
        }

        adapter2.pushInt(arity);
        adapter2.invokestatic(p(Helpers.class), "anewarrayIRubyObjects", sig(IRubyObject[].class, int.class));
        for (int i = 0; i < arity;) {
            int j = 0;
            while (i + j < arity && j < Helpers.MAX_SPECIFIC_ARITY_OBJECT_ARRAY) {
                adapter2.aload(base + i + j);
                j++;
            }
            adapter2.pushInt(i);
            i += j;
            adapter2.invokestatic(p(Helpers.class), "aastoreIRubyObjects", sig(IRubyObject[].class, params(IRubyObject[].class, IRubyObject.class, j, int.class)));
        }
    }

    public void invokeSelf(String name, int arity, boolean hasClosure) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + name + "' has more than " + MAX_ARGUMENTS + " arguments");

        invokeOther(name, arity, hasClosure);
    }

    public void invokeInstanceSuper(String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to instance super has more than " + MAX_ARGUMENTS + " arguments");

        performSuper(name, arity, hasClosure, splatmap, "instanceSuperSplatArgs", false);
    }

    public void invokeClassSuper(String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to class super has more than " + MAX_ARGUMENTS + " arguments");

        performSuper(name, arity, hasClosure, splatmap, "classSuperSplatArgs", false);
    }

    public void invokeUnresolvedSuper(String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to unresolved super has more than " + MAX_ARGUMENTS + " arguments");

        performSuper(name, arity, hasClosure, splatmap, "unresolvedSuperSplatArgs", true);
    }

    public void invokeZSuper(String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to zsuper has more than " + MAX_ARGUMENTS + " arguments");

        performSuper(name, arity, hasClosure, splatmap, "zSuperSplatArgs", true);
    }

    private void performSuper(String name, int arity, boolean hasClosure, boolean[] splatmap, String helperName, boolean unresolved) {
        SkinnyMethodAdapter adapter2;
        String incomingSig;
        String outgoingSig;

        if (hasClosure) {
            incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class));
            outgoingSig = unresolved ?
                    sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class)) :
                    sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, String.class, RubyModule.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class));
        } else {
            incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity));
            outgoingSig = unresolved ?
                    sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class)) :
                    sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, String.class, RubyModule.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class));
        }

        String methodName = "invokeSuper" + getClassData().callSiteCount.getAndIncrement() + ":" + JavaNameMangler.mangleMethodName(name);
        adapter2 = new SkinnyMethodAdapter(
                adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        // CON FIXME: make these offsets programmatically determined
        adapter2.aload(0);
        adapter2.aload(2);
        if (!unresolved) adapter2.ldc(name);
        if (!unresolved) adapter2.aload(3);

        buildArrayFromLocals(adapter2, 4, arity);

        if (hasClosure) {
            adapter2.aload(4 + arity);
        } else {
            adapter2.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
        }

        if (splatmap != null || splatmap.length > 0 || anyTrue(splatmap)) {
            String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);
            adapter2.ldc(splatmapString);
            adapter2.invokestatic(p(IRRuntimeHelpers.class), "decodeSplatmap", sig(boolean[].class, String.class));
        } else {
            adapter2.getstatic(p(IRRuntimeHelpers.class), "EMPTY_BOOLEAN_ARRAY", ci(boolean[].class));
        }

        adapter2.invokestatic(p(IRRuntimeHelpers.class), helperName, outgoingSig);
        adapter2.areturn();
        adapter2.end();

        // now call it
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
    }

    private static boolean anyTrue(boolean[] booleans) {
        for (boolean b : booleans) if (b) return true;
        return false;
    }

    public void searchConst(String name, boolean noPrivateConsts) {
        adapter.ldc(name);
        adapter.ldc(noPrivateConsts);
        invokeIRHelper("searchConst", sig(IRubyObject.class, ThreadContext.class, StaticScope.class, String.class, boolean.class));
    }

    public void inheritanceSearchConst(String name, boolean noPrivateConsts) {
        adapter.ldc(name);
        adapter.ldc(noPrivateConsts);
        invokeIRHelper("inheritedSearchConst", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, String.class, boolean.class));
    }

    public void lexicalSearchConst(String name) {
        adapter.ldc(name);
        invokeIRHelper("lexicalSearchConst", sig(IRubyObject.class, ThreadContext.class, StaticScope.class, String.class));}

    public void pushNil() {
        loadContext();
        adapter.getfield(p(ThreadContext.class), "nil", ci(IRubyObject.class));
    }

    public void pushBoolean(boolean b) {
        loadRuntime();
        adapter.invokevirtual(p(Ruby.class), b ? "getTrue" : "getFalse", sig(RubyBoolean.class));
    }

    public void pushBignum(BigInteger bigint) {
        String bigintStr = bigint.toString();

        loadRuntime();
        adapter.ldc(bigintStr);
        adapter.invokestatic(p(RubyBignum.class), "newBignum", sig(RubyBignum.class, Ruby.class, String.class));
    }

    public void putField(String name) {
        adapter.ldc(name);
        invokeIRHelper("setInstanceVariable", sig(IRubyObject.class, IRubyObject.class, IRubyObject.class, String.class));
        adapter.pop();
    }

    public void getField(String name) {
        loadRuntime();
        adapter.ldc(name);
        invokeHelper("getInstanceVariable", sig(IRubyObject.class, IRubyObject.class, Ruby.class, String.class));
    }

    public void array(int length) {
        if (length > MAX_ARGUMENTS) throw new NotCompilableException("literal array has more than " + MAX_ARGUMENTS + " elements");

        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, length));

        if (!getClassData().arrayMethodsDefined.contains(length)) {
            adapter2 = new SkinnyMethodAdapter(
                    adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "array:" + length,
                    incomingSig,
                    null,
                    null);

            adapter2.aload(0);
            adapter2.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
            buildArrayFromLocals(adapter2, 1, length);

            adapter2.invokevirtual(p(Ruby.class), "newArrayNoCopy", sig(RubyArray.class, IRubyObject[].class));
            adapter2.areturn();
            adapter2.end();

            getClassData().arrayMethodsDefined.add(length);
        }

        // now call it
        adapter.invokestatic(getClassData().clsName, "array:" + length, incomingSig);
    }

    public void hash(int length) {
        if (length > MAX_ARGUMENTS) throw new NotCompilableException("literal hash has more than " + (MAX_ARGUMENTS / 2) + " pairs");

        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, length * 2));

        if (!getClassData().hashMethodsDefined.contains(length)) {
            adapter2 = new SkinnyMethodAdapter(
                    adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "hash:" + length,
                    incomingSig,
                    null,
                    null);

            adapter2.aload(0);
            adapter2.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
            buildArrayFromLocals(adapter2, 1, length * 2);

            adapter2.invokestatic(p(IRRuntimeHelpers.class), "constructHashFromArray", sig(RubyHash.class, Ruby.class, IRubyObject[].class));
            adapter2.areturn();
            adapter2.end();

            getClassData().hashMethodsDefined.add(length);
        }

        // now call it
        adapter.invokestatic(getClassData().clsName, "hash:" + length, incomingSig);
    }

    public void checkpoint() {
        loadContext();
        adapter.invokevirtual(
                p(ThreadContext.class),
                "callThreadPoll",
                sig(void.class));
    }
}
