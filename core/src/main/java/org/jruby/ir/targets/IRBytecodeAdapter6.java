/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
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
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JavaNameMangler;
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
        SkinnyMethodAdapter adapter2;
        String incomingSig;
        String outgoingSig;

        if (hasClosure) {
            if (arity == -1) {
                incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class));
                outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, String.class, JVM.OBJECT_ARRAY, Block.class));
            } else {
                incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, arity + 2, Block.class));
                outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, String.class, JVM.OBJECT_ARRAY, Block.class));
            }
        } else {
            if (arity == -1) {
                incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY));
                outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, String.class, JVM.OBJECT_ARRAY));
            } else if (arity == 1) {
                // specialize arity-1 calls through callMethod
                incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, arity + 2));
                outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, String.class, JVM.OBJECT));
            } else {
                incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity));
                outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, String.class, JVM.OBJECT_ARRAY));
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

        adapter2.aload(2);
        adapter2.aload(0);
        adapter2.ldc(name);

        if (hasClosure) {
            if (arity == -1) {
                adapter2.aload(3);
                adapter2.aload(4);
            } else {
                buildArrayFromLocals(adapter2, 3, arity);
                adapter2.aload(3 + arity);
            }
        } else {
            if (arity == -1) {
                adapter2.aload(3);
            } else if (arity == 1) {
                adapter2.aload(3);
            } else {
                buildArrayFromLocals(adapter2, 3, arity);
            }
        }

        adapter2.invokeinterface(p(IRubyObject.class), "callMethod", outgoingSig);
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
            adapter2.pushInt(j);
            i += j;
            adapter2.invokestatic(p(Helpers.class), "aastoreIRubyObjects", sig(IRubyObject[].class, params(IRubyObject[].class, IRubyObject.class, j, int.class)));
        }
    }

    public void invokeSelf(String name, int arity, boolean hasClosure) {
        invokeOther(name, arity, hasClosure);
    }

    public void invokeInstanceSuper(String name, int arity, boolean hasClosure, boolean[] splatmap) {
        performSuper(name, arity, hasClosure, splatmap, "instanceSuperSplatArgs", false);
    }

    public void invokeClassSuper(String name, int arity, boolean hasClosure, boolean[] splatmap) {
        performSuper(name, arity, hasClosure, splatmap, "classSuperSplatArgs", false);
    }

    public void invokeUnresolvedSuper(String name, int arity, boolean hasClosure, boolean[] splatmap) {
        performSuper(name, arity, hasClosure, splatmap, "unresolvedSuperSplatArgs", false);
    }

    public void invokeZSuper(String name, int arity, boolean hasClosure, boolean[] splatmap) {
        performSuper(name, arity, hasClosure, splatmap, "zSuperSplatArgs", false);
    }

    private void performSuper(String name, int arity, boolean hasClosure, boolean[] splatmap, String helperName, boolean unresolved) {
        SkinnyMethodAdapter adapter2;
        String incomingSig;
        String outgoingSig;

        String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);

        if (hasClosure) {
            incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class));
            outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, JVM.OBJECT_ARRAY, Block.class));
        } else {
            incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity));
            outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, JVM.OBJECT_ARRAY, Block.class));
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

        if (hasClosure) {
            buildArrayFromLocals(adapter2, 4, arity);
            adapter2.aload(4 + arity);
        } else {
            buildArrayFromLocals(adapter2, 4, arity);
            adapter2.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
        }

        adapter2.ldc(splatmapString);
        invokeIRHelper("decodeSplatmap", sig(boolean[].class, String.class));

        invokeIRHelper(helperName, outgoingSig);
        adapter2.areturn();
        adapter2.end();

        // now call it
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
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

    public void pushBignum(BigInteger bigint) { throw new RuntimeException("unimplemented for Java 6"); }

    public void putField(String name) {
        adapter.ldc(name);
        invokeIRHelper("setInstanceVariable", sig(IRubyObject.class, IRubyObject.class, IRubyObject.class, String.class));
    }

    public void getField(String name) {
        loadSelf();
        loadRuntime();
        adapter.ldc(name);
        invokeHelper("getInstanceVariable", sig(IRubyObject.class, IRubyObject.class, Ruby.class, String.class));
    }

    public void array(int length) {
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
