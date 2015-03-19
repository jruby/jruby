/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
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
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.NormalCachingCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.ByteList;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.RegexpOptions;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

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

    public void pushFixnum(final long l) {
        cacheValuePermanently("fixnum", RubyFixnum.class, keyFor("fixnum", l), new Runnable() {
            @Override
            public void run() {
                loadRuntime();
                adapter.ldc(l);
                adapter.invokevirtual(p(Ruby.class), "newFixnum", sig(RubyFixnum.class, long.class));
            }
        });
    }

    public void pushFloat(final double d) {
        cacheValuePermanently("float", RubyFloat.class, keyFor("float", Double.doubleToLongBits(d)), new Runnable() {
            @Override
            public void run() {
                loadRuntime();
                adapter.ldc(d);
                adapter.invokevirtual(p(Ruby.class), "newFloat", sig(RubyFloat.class, double.class));
            }
        });
    }

    public void pushString(ByteList bl) {
        loadRuntime();
        pushByteList(bl);
        adapter.invokestatic(p(RubyString.class), "newStringShared", sig(RubyString.class, Ruby.class, ByteList.class));
    }

    private String newFieldName(String baseName) {
        return baseName + getClassData().callSiteCount.getAndIncrement();
    }

    /**
     * Stack required: none
     *
     * @param bl ByteList for the String to push
     */
    public void pushFrozenString(final ByteList bl) {
        cacheValuePermanently("fstring", RubyString.class, keyFor("fstring", bl), new Runnable() {
            @Override
            public void run() {
                loadRuntime();
                adapter.ldc(bl.toString());
                adapter.ldc(bl.getEncoding().toString());
                invokeIRHelper("newFrozenStringFromRaw", sig(RubyString.class, Ruby.class, String.class, String.class));
            }
        });
    }

    public void pushByteList(final ByteList bl) {
        cacheValuePermanently("bytelist", ByteList.class, keyFor("bytelist", bl), new Runnable() {
            @Override
            public void run() {
                loadRuntime();
                adapter.ldc(bl.toString());
                adapter.ldc(bl.getEncoding().toString());
                invokeIRHelper("newByteListFromRaw", sig(ByteList.class, Ruby.class, String.class, String.class));
            }
        });
    }

    public void cacheValuePermanently(String what, Class type, Object key, Runnable construction) {
        String cacheField = key == null ? null : cacheFieldNames.get(key);
        if (cacheField == null) {
            cacheField = newFieldName(what);
            cacheFieldNames.put(key, cacheField);

            SkinnyMethodAdapter tmp = adapter;
            adapter = new SkinnyMethodAdapter(
                    adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    cacheField,
                    sig(type, ThreadContext.class),
                    null,
                    null);
            Label done = new Label();
            adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, cacheField, ci(type), null, null).visitEnd();
            adapter.getstatic(getClassData().clsName, cacheField, ci(type));
            adapter.dup();
            adapter.ifnonnull(done);
            adapter.pop();
            construction.run();
            adapter.dup();
            adapter.putstatic(getClassData().clsName, cacheField, ci(type));
            adapter.label(done);
            adapter.areturn();
            adapter.end();
            adapter = tmp;
        }

        loadContext();
        adapter.invokestatic(getClassData().clsName, cacheField, sig(type, ThreadContext.class));
    }

    public void pushRegexp(final ByteList source, final int options) {
        cacheValuePermanently("regexp", RubyRegexp.class, keyFor("regexp", source, options), new Runnable() {
            @Override
            public void run() {
                loadContext();
                pushByteList(source);
                adapter.pushInt(options);
                invokeIRHelper("newLiteralRegexp", sig(RubyRegexp.class, ThreadContext.class, ByteList.class, int.class));
            }
        });
    }

    private static String keyFor(Object... objs) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : objs) {
            sb.append(obj.toString());
            if (obj instanceof ByteList) sb.append('_').append(((ByteList) obj).getEncoding());
            sb.append("_");
        }
        return sb.toString();
    }

    public void pushDRegexp(Runnable callback, RegexpOptions options, int arity) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("dynamic regexp has more than " + MAX_ARGUMENTS + " elements");

        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(RubyRegexp.class, params(ThreadContext.class, RubyString.class, arity, int.class));

        if (!getClassData().dregexpMethodsDefined.contains(arity)) {
            adapter2 = new SkinnyMethodAdapter(
                    adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "dregexp:" + arity,
                    incomingSig,
                    null,
                    null);

            adapter2.aload(0);
            buildArrayFromLocals(adapter2, 1, arity);
            adapter2.iload(1 + arity);

            adapter2.invokestatic(p(IRRuntimeHelpers.class), "newDynamicRegexp", sig(RubyRegexp.class, ThreadContext.class, IRubyObject[].class, int.class));
            adapter2.areturn();
            adapter2.end();

            getClassData().dregexpMethodsDefined.add(arity);
        }

        String cacheField = null;
        Label done = null;

        if (options.isOnce()) {
            // need to cache result forever
            cacheField = "dregexp" + getClassData().callSiteCount.getAndIncrement();
            done = new Label();
            adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, cacheField, ci(RubyRegexp.class), null, null).visitEnd();
            adapter.getstatic(getClassData().clsName, cacheField, ci(RubyRegexp.class));
            adapter.dup();
            adapter.ifnonnull(done);
            adapter.pop();
        }

        // call synthetic method if we still need to build dregexp
        callback.run();
        adapter.ldc(options.toEmbeddedOptions());
        adapter.invokestatic(getClassData().clsName, "dregexp:" + arity, incomingSig);

        if (done != null) {
            adapter.dup();
            adapter.putstatic(getClassData().clsName, cacheField, ci(RubyRegexp.class));
            adapter.label(done);
        }
    }

    public void pushSymbol(final String sym, final Encoding encoding) {
        cacheValuePermanently("symbol", RubySymbol.class, keyFor("symbol", sym, encoding), new Runnable() {
            @Override
            public void run() {
                loadRuntime();
                adapter.ldc(sym);
                loadContext();
                adapter.ldc(encoding.toString());
                invokeIRHelper("retrieveJCodingsEncoding", sig(Encoding.class, ThreadContext.class, String.class));

                adapter.invokestatic(p(RubySymbol.class), "newSymbol", sig(RubySymbol.class, Ruby.class, String.class, Encoding.class));
            }
        });
    }

    public void loadRuntime() {
        loadContext();
        adapter.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
    }

    public void pushEncoding(final Encoding encoding) {
        cacheValuePermanently("encoding", RubySymbol.class, keyFor("encoding", encoding), new Runnable() {
            @Override
            public void run() {
                loadContext();
                adapter.ldc(encoding.toString());
                invokeIRHelper("retrieveEncoding", sig(RubyEncoding.class, ThreadContext.class, String.class));
            }
        });
    }

    public void invokeOther(String name, int arity, boolean hasClosure) {
        invoke(name, arity, hasClosure, CallType.NORMAL);
    }

    public void invoke(String name, int arity, boolean hasClosure, CallType callType) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + name + "' has more than " + MAX_ARGUMENTS + " arguments");

        SkinnyMethodAdapter adapter2;
        String incomingSig;
        String outgoingSig;

        if (hasClosure) {
            switch (arity) {
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
                default:
                    incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity, Block.class));
                    outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class));
                    break;
            }
        } else {
            switch (arity) {
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
                default:
                    incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity));
                    outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY));
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
        adapter2.ldc(name);
        Class<? extends CachingCallSite> siteClass;
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
        adapter2.invokestatic(p(IRRuntimeHelpers.class), "new" + siteClass.getSimpleName(), sig(siteClass, String.class));
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
            adapter2.invokestatic(p(Helpers.class), "aastoreIRubyObjects", sig(IRubyObject[].class, params(IRubyObject[].class, IRubyObject.class, j, int.class)));
            i += j;
        }
    }

    public void invokeOtherOneFixnum(String name, long fixnum) {
        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT));
        String outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, long.class));

        String methodName = "invokeOtherOneFixnum" + getClassData().callSiteCount.getAndIncrement() + ":" + JavaNameMangler.mangleMethodName(name);

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
        adapter2.ldc(name);
        adapter2.invokestatic(p(IRRuntimeHelpers.class), "newNormalCachingCallSite", sig(NormalCachingCallSite.class, String.class));
        adapter2.dup();
        adapter2.putstatic(getClassData().clsName, methodName, ci(CachingCallSite.class));

        // use call site to invoke
        adapter2.label(doCall);
        adapter2.aload(0); // context
        adapter2.aload(1); // caller
        adapter2.aload(2); // target
        adapter2.ldc(fixnum); // fixnum

        adapter2.invokevirtual(p(CachingCallSite.class), "call", outgoingSig);
        adapter2.areturn();
        adapter2.end();

        // now call it
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
    }

    public void invokeOtherOneFloat(String name, double flote) {
        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT));
        String outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, double.class));

        String methodName = "invokeOtherOneFloat" + getClassData().callSiteCount.getAndIncrement() + ":" + JavaNameMangler.mangleMethodName(name);

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
        adapter2.ldc(name);
        adapter2.invokestatic(p(IRRuntimeHelpers.class), "newNormalCachingCallSite", sig(NormalCachingCallSite.class, String.class));
        adapter2.dup();
        adapter2.putstatic(getClassData().clsName, methodName, ci(CachingCallSite.class));

        // use call site to invoke
        adapter2.label(doCall);
        adapter2.aload(0); // context
        adapter2.aload(1); // caller
        adapter2.aload(2); // target
        adapter2.ldc(flote); // float

        adapter2.invokevirtual(p(CachingCallSite.class), "call", outgoingSig);
        adapter2.areturn();
        adapter2.end();

        // now call it
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
    }

    public void invokeSelf(String name, int arity, boolean hasClosure, CallType callType) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + name + "' has more than " + MAX_ARGUMENTS + " arguments");

        invoke(name, arity, hasClosure, callType);
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
        adapter.dup2(); // self, value, self, value
        adapter.pop(); // self, value, self
        cacheVariableAccessor(name, true); // self, value, accessor
        invokeIRHelper("setVariableWithAccessor", sig(void.class, IRubyObject.class, IRubyObject.class, VariableAccessor.class));
    }

    public void getField(String name) {
        adapter.dup(); // self, self
        cacheVariableAccessor(name, false); // self, accessor
        loadContext(); // self, accessor, context
        adapter.ldc(name);
        invokeIRHelper("getVariableWithAccessor", sig(IRubyObject.class, IRubyObject.class, VariableAccessor.class, ThreadContext.class, String.class));
    }

    /**
     * Retrieve the proper variable accessor for the given arguments. The source object is expected to be on stack.
     * @param name name of the variable
     * @param write whether the accessor will be used for a write operation
     */
    private void cacheVariableAccessor(String name, boolean write) {
        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(VariableAccessor.class, params(JVM.OBJECT));

        String methodName = (write ? "ivarSet" : "ivarGet") + getClassData().callSiteCount.getAndIncrement() + ":" + JavaNameMangler.mangleMethodName(name);

        adapter2 = new SkinnyMethodAdapter(
                adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        // call site object field
        adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, ci(VariableAccessor.class), null, null).visitEnd();

        // retrieve accessor, verifying if non-null
        adapter2.getstatic(getClassData().clsName, methodName, ci(VariableAccessor.class));
        adapter2.dup();
        Label get = new Label();
        adapter2.ifnull(get);

        // this might be a little faster if we cached the last class ID seen and used that rather than getMetaClass().getRealClass() in VariableAccessor
        adapter2.dup();
        adapter2.aload(0);
        adapter2.invokevirtual(p(VariableAccessor.class), "verify", sig(boolean.class, Object.class));
        adapter2.iffalse(get);
        adapter2.areturn();

        adapter2.label(get);
        adapter2.pop();
        adapter2.aload(0);
        adapter2.ldc(name);
        adapter2.invokestatic(p(IRRuntimeHelpers.class), write ? "getVariableAccessorForWrite" : "getVariableAccessorForRead", sig(VariableAccessor.class, IRubyObject.class, String.class));
        adapter2.dup();
        adapter2.putstatic(getClassData().clsName, methodName, ci(VariableAccessor.class));
        adapter2.areturn();

        adapter2.end();

        // call it from original method to get accessor
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
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
        if (length > MAX_ARGUMENTS / 2) throw new NotCompilableException("literal hash has more than " + (MAX_ARGUMENTS / 2) + " pairs");

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

    public void kwargsHash(int length) {
        if (length > MAX_ARGUMENTS / 2) throw new NotCompilableException("kwargs hash has more than " + (MAX_ARGUMENTS / 2) + " pairs");

        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, RubyHash.class, IRubyObject.class, length * 2));

        if (!getClassData().kwargsHashMethodsDefined.contains(length)) {
            adapter2 = new SkinnyMethodAdapter(
                    adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "kwargsHash:" + length,
                    incomingSig,
                    null,
                    null);

            adapter2.aload(0);
            adapter2.aload(1);
            buildArrayFromLocals(adapter2, 2, length * 2);

            adapter2.invokestatic(p(IRRuntimeHelpers.class), "dupKwargsHashAndPopulateFromArray", sig(RubyHash.class, ThreadContext.class, RubyHash.class, IRubyObject[].class));
            adapter2.areturn();
            adapter2.end();

            getClassData().hashMethodsDefined.add(length);
        }

        // now call it
        adapter.invokestatic(getClassData().clsName, "kwargsHash:" + length, incomingSig);
    }

    public void checkpoint() {
        loadContext();
        adapter.invokevirtual(
                p(ThreadContext.class),
                "callThreadPoll",
                sig(void.class));
    }

    private final Map<Object, String> cacheFieldNames = new HashMap<>();
}
