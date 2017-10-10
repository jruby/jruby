/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
import org.jruby.RubyProc;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.IRScope;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.specialized.RubyArraySpecialized;
import org.jruby.util.ByteList;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.RegexpOptions;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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

    public static final String SUPER_SPLAT_UNRESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class));
    public static final String SUPER_NOSPLAT_UNRESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, JVM.OBJECT_ARRAY, Block.class));
    public static final String SUPER_SPLAT_RESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, String.class, RubyModule.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class));
    public static final String SUPER_NOSPLAT_RESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, String.class, RubyModule.class, JVM.OBJECT_ARRAY, Block.class));

    public IRBytecodeAdapter6(SkinnyMethodAdapter adapter, Signature signature, ClassData classData) {
        super(adapter, signature, classData);
    }

    public void pushFixnum(final long l) {
        cacheValuePermanentlyLoadContext("fixnum", RubyFixnum.class, keyFor("fixnum", l), new Runnable() {
            @Override
            public void run() {
                loadRuntime();
                adapter.ldc(l);
                adapter.invokevirtual(p(Ruby.class), "newFixnum", sig(RubyFixnum.class, long.class));
            }
        });
    }

    public void pushFloat(final double d) {
        cacheValuePermanentlyLoadContext("float", RubyFloat.class, keyFor("float", Double.doubleToLongBits(d)), new Runnable() {
            @Override
            public void run() {
                loadRuntime();
                adapter.ldc(d);
                adapter.invokevirtual(p(Ruby.class), "newFloat", sig(RubyFloat.class, double.class));
            }
        });
    }

    public void pushString(ByteList bl, int cr) {
        loadRuntime();
        pushByteList(bl);
        adapter.ldc(cr);
        adapter.invokestatic(p(RubyString.class), "newStringShared", sig(RubyString.class, Ruby.class, ByteList.class, int.class));
    }

    private String newFieldName(String baseName) {
        return baseName + getClassData().callSiteCount.getAndIncrement();
    }

    /**
     * Stack required: none
     *
     * @param bl ByteList for the String to push
     */
    public void pushFrozenString(final ByteList bl, final int cr, final String file, final int line) {
        cacheValuePermanentlyLoadContext("fstring", RubyString.class, keyFor("fstring", bl), new Runnable() {
            @Override
            public void run() {
                loadContext();
                adapter.ldc(bl.toString());
                adapter.ldc(bl.getEncoding().toString());
                adapter.ldc(cr);
                adapter.ldc(file);
                adapter.ldc(line);
                invokeIRHelper("newFrozenStringFromRaw", sig(RubyString.class, ThreadContext.class, String.class, String.class, int.class, String.class, int.class));
            }
        });
    }

    public void pushByteList(final ByteList bl) {
        cacheValuePermanentlyLoadContext("bytelist", ByteList.class, keyFor("bytelist", bl), new Runnable() {
            @Override
            public void run() {
                loadRuntime();
                adapter.ldc(bl.toString());
                adapter.ldc(bl.getEncoding().toString());
                invokeIRHelper("newByteListFromRaw", sig(ByteList.class, Ruby.class, String.class, String.class));
            }
        });
    }

    private final Runnable LOAD_CONTEXT = new Runnable() {
        @Override
        public void run() {
            loadContext();
        }
    };

    public String cacheValuePermanentlyLoadContext(String what, Class type, Object key, Runnable construction) {
        return cacheValuePermanently(what, type, key, false, sig(type, ThreadContext.class), LOAD_CONTEXT, construction);
    }

    public String cacheValuePermanently(String what, Class type, Object key, boolean sync, Runnable construction) {
        return cacheValuePermanently(what, type, key, sync, sig(type), null, construction);
    }

    public String cacheValuePermanently(String what, Class type, Object key, boolean sync, String signature, Runnable loadState, Runnable construction) {
        String cacheField = key == null ? null : cacheFieldNames.get(key);
        String clsName = getClassData().clsName;
        if (cacheField == null) {
            cacheField = newFieldName(what);
            cacheFieldNames.put(key, cacheField);

            SkinnyMethodAdapter tmp = adapter;
            adapter = new SkinnyMethodAdapter(
                    adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    cacheField,
                    signature,
                    null,
                    null);
            Label done = new Label();
            Label before = sync ? new Label() : null;
            Label after = sync ? new Label() : null;
            Label catchbody = sync ? new Label() : null;
            Label done2 = sync ? new Label() : null;
            adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, cacheField, ci(type), null, null).visitEnd();
            adapter.getstatic(clsName, cacheField, ci(type));
            adapter.dup();
            adapter.ifnonnull(done);
            adapter.pop();

            // lock class and check static field again
            Type classType = Type.getType("L" + clsName.replace('.', '/') + ';');
            int tempIndex = Type.getMethodType(signature).getArgumentsAndReturnSizes() >> 2 + 1;
            if (sync) {
                adapter.ldc(classType);
                adapter.dup();
                adapter.astore(tempIndex);
                adapter.monitorenter();

                adapter.trycatch(before, after, catchbody, null);

                adapter.label(before);
                adapter.getstatic(clsName, cacheField, ci(type));
                adapter.dup();
                adapter.ifnonnull(done2);
                adapter.pop();
            }

            construction.run();
            adapter.dup();
            adapter.putstatic(clsName, cacheField, ci(type));

            // unlock class along normal and exceptional exits
            if (sync) {
                adapter.label(done2);
                adapter.aload(tempIndex);
                adapter.monitorexit();
                adapter.go_to(done);
                adapter.label(after);

                adapter.label(catchbody);
                adapter.aload(tempIndex);
                adapter.monitorexit();
                adapter.athrow();
            }

            adapter.label(done);
            adapter.areturn();
            adapter.end();
            adapter = tmp;
        }

        if (loadState != null) loadState.run();
        adapter.invokestatic(clsName, cacheField, signature);

        return cacheField;
    }

    public void pushRegexp(final ByteList source, final int options) {
        cacheValuePermanentlyLoadContext("regexp", RubyRegexp.class, keyFor("regexp", source, options), new Runnable() {
            @Override
            public void run() {
                loadContext();
                pushByteList(source);
                adapter.pushInt(options);
                invokeIRHelper("newLiteralRegexp", sig(RubyRegexp.class, ThreadContext.class, ByteList.class, int.class));
            }
        });
    }

    private static String keyFor(Object obj1, Object obj2) {
        StringBuilder sb = new StringBuilder(16);
        keyFor(sb, obj1);
        keyFor(sb, obj2);
        return sb.toString();
    }

    private static String keyFor(Object obj1, Object obj2, Object obj3) {
        StringBuilder sb = new StringBuilder(24);
        keyFor(sb, obj1);
        keyFor(sb, obj2);
        keyFor(sb, obj3);
        return sb.toString();
    }

    private static void keyFor(StringBuilder builder, Object obj) {
        builder.append(obj.toString());
        if (obj instanceof ByteList) builder.append('_').append(((ByteList) obj).getEncoding());
        builder.append('_');
    }

    public void pushDRegexp(final Runnable callback, final RegexpOptions options, final int arity) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("dynamic regexp has more than " + MAX_ARGUMENTS + " elements");

        String incomingSig = sig(RubyRegexp.class, params(ThreadContext.class, IRubyObject.class, arity, int.class));
        ClassData classData = getClassData();
        String className = classData.clsName;

        String cacheField = "dregexp" + classData.callSiteCount.getAndIncrement();
        String atomicRefField = null;
        Label done = new Label();

        if (options.isOnce()) {
            // need to cache result forever, but do it atomically so first one wins

            // TODO: this might be better in a static initializer than lazy + sync to construct?
            atomicRefField = cacheValuePermanently("atomicref", AtomicReference.class, cacheField, true, new Runnable() {
                @Override
                public void run() {
                    adapter.newobj(p(AtomicReference.class));
                    adapter.dup();
                    adapter.invokespecial(p(AtomicReference.class), "<init>", sig(void.class));
                }
            });

            adapter.invokevirtual(p(AtomicReference.class), "get", sig(Object.class));
            adapter.dup();
            adapter.ifnonnull(done);
            adapter.pop();

            // load ref again plus null expected value for CAS, below regexp we are about to construct
            adapter.getstatic(className.replace('.', '/'), atomicRefField, ci(AtomicReference.class));
            adapter.aconst_null();
        }

        // We may evaluate these operands multiple times or the upstream instrs that created them, which is a bug (jruby/jruby#2798).
        // However, the atomic reference will ensure we only cache the first dregexp to win.
        callback.run();
        adapter.ldc(options.toEmbeddedOptions());

        if (arity >= 1 && arity <= 5) {
            // use pre-made version from IR helpers
            invokeIRHelper("newDynamicRegexp", incomingSig);
        } else {
            String methodName = "dregexp" + arity;

            if (!classData.dregexpMethodsDefined.contains(arity)) {
                // generate a new one
                SkinnyMethodAdapter adapter2 = new SkinnyMethodAdapter(
                        adapter.getClassVisitor(),
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                        methodName,
                        incomingSig,
                        null,
                        null);

                adapter2.aload(0);
                buildArrayFromLocals(adapter2, 1, arity);
                adapter2.iload(1 + arity);

                adapter2.invokestatic(p(IRRuntimeHelpers.class), "newDynamicRegexp", sig(RubyRegexp.class, ThreadContext.class, IRubyObject[].class, int.class));
                adapter2.areturn();
                adapter2.end();

                classData.dregexpMethodsDefined.add(arity);
            }

            adapter.invokestatic(className, methodName, incomingSig);
        }

        if (options.isOnce()) {
            // do the CAS
            adapter.invokevirtual(p(AtomicReference.class), "compareAndSet", sig(boolean.class, Object.class, Object.class));
            adapter.pop();

            // get the value again
            adapter.getstatic(className.replace('.', '/'), atomicRefField, ci(AtomicReference.class));
            adapter.invokevirtual(p(AtomicReference.class), "get", sig(Object.class));

            adapter.label(done);

            adapter.checkcast(p(RubyRegexp.class));
        }
    }

    public void pushSymbol(final ByteList bytes) {
        cacheValuePermanentlyLoadContext("symbol", RubySymbol.class, keyFor("symbol", bytes, bytes.getEncoding()), new Runnable() {
            @Override
            public void run() {
                loadRuntime();
                pushByteList(bytes);
                adapter.invokestatic(p(RubySymbol.class), "newSymbol", sig(RubySymbol.class, Ruby.class, ByteList.class));
            }
        });
    }

    public void pushSymbolProc(final String name, final Encoding encoding) {
        cacheValuePermanentlyLoadContext("symbolProc", RubyProc.class, null, new Runnable() {
            @Override
            public void run() {
                loadContext();
                adapter.ldc(name);
                adapter.ldc(encoding.toString());
                invokeIRHelper("newSymbolProc", sig(RubyProc.class, ThreadContext.class, String.class, String.class));
            }
        });
    }

    public void loadRuntime() {
        loadContext();
        adapter.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
    }

    public void pushEncoding(final Encoding encoding) {
        cacheValuePermanentlyLoadContext("encoding", RubySymbol.class, keyFor("encoding", encoding), new Runnable() {
            @Override
            public void run() {
                loadContext();
                adapter.ldc(encoding.toString());
                invokeIRHelper("retrieveEncoding", sig(RubyEncoding.class, ThreadContext.class, String.class));
            }
        });
    }

    @Override
    public void invokeOther(String file, int line, String name, int arity, BlockPassType blockPassType, boolean isPotentiallyRefined) {
        invoke(file, line, name, arity, blockPassType, CallType.NORMAL, isPotentiallyRefined);
    }

    public void invokeArrayDeref(String file, int line) {
        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyString.class));

        String methodName = getUniqueSiteName("[]");

        adapter2 = new SkinnyMethodAdapter(
                adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.aloadMany(0, 1, 2, 3);
        cacheCallSite(adapter2, getClassData().clsName, methodName, "[]", CallType.FUNCTIONAL, false);
        adapter2.invokestatic(p(IRRuntimeHelpers.class), "callOptimizedAref", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, RubyString.class, CallSite.class));
        adapter2.areturn();
        adapter2.end();

        // now call it
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
    }

    public void invoke(String file, int lineNumber, String name, int arity, BlockPassType blockPassType, CallType callType, boolean isPotentiallyRefined) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + name + "' has more than " + MAX_ARGUMENTS + " arguments");

        SkinnyMethodAdapter adapter2;
        String incomingSig;
        String outgoingSig;

        boolean blockGiven = blockPassType.given();
        if (blockGiven) {
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

        String methodName = getUniqueSiteName(name);

        adapter2 = new SkinnyMethodAdapter(
                adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.line(lineNumber);

        cacheCallSite(adapter2, getClassData().clsName, methodName, name, callType, isPotentiallyRefined);

        // use call site to invoke
        adapter2.aload(0); // context
        adapter2.aload(1); // caller
        adapter2.aload(2); // self

        switch (arity) {
            case -1:
            case 1:
                adapter2.aload(3);
                if (blockGiven) adapter2.aload(4);
                break;
            case 0:
                if (blockGiven) adapter2.aload(3);
                break;
            case 2:
                adapter2.aload(3);
                adapter2.aload(4);
                if (blockGiven) adapter2.aload(5);
                break;
            case 3:
                adapter2.aload(3);
                adapter2.aload(4);
                adapter2.aload(5);
                if (blockGiven) adapter2.aload(6);
                break;
            default:
                buildArrayFromLocals(adapter2, 3, arity);
                if (blockGiven) adapter2.aload(3 + arity);
                break;
        }

        adapter2.invokevirtual(p(CachingCallSite.class), blockPassType.literal() ? "callIter" : "call", outgoingSig);
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

    public void invokeOtherOneFixnum(String file, int line, String name, long fixnum, CallType callType) {
        if (!MethodIndex.hasFastFixnumOps(name)) {
            pushFixnum(fixnum);
            if (callType == CallType.NORMAL) {
                invokeOther(file, line, name, 1, BlockPassType.NONE, false);
            } else {
                invokeSelf(file, line, name, 1, BlockPassType.NONE, callType, false);
            }
            return;
        }

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

        adapter2.line(line);

        // call site object field
        adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, ci(CallSite.class), null, null).visitEnd();

        // lazily construct it
        adapter2.getstatic(getClassData().clsName, methodName, ci(CallSite.class));
        adapter2.dup();
        Label doCall = new Label();
        adapter2.ifnonnull(doCall);
        adapter2.pop();
        adapter2.ldc(name);
        adapter2.invokestatic(p(MethodIndex.class), "getFastFixnumOpsCallSite", sig(CallSite.class, String.class));
        adapter2.dup();
        adapter2.putstatic(getClassData().clsName, methodName, ci(CallSite.class));

        // use call site to invoke
        adapter2.label(doCall);
        adapter2.aload(0); // context
        adapter2.aload(1); // caller
        adapter2.aload(2); // target
        adapter2.ldc(fixnum); // fixnum

        adapter2.invokevirtual(p(CallSite.class), "call", outgoingSig);
        adapter2.areturn();
        adapter2.end();

        // now call it
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
    }

    public void invokeOtherOneFloat(String file, int line, String name, double flote, CallType callType) {
        if (!MethodIndex.hasFastFloatOps(name)) {
            pushFloat(flote);
            if (callType == CallType.NORMAL) {
                invokeOther(file, line, name, 1, BlockPassType.NONE, false);
            } else {
                invokeSelf(file, line, name, 1, BlockPassType.NONE, callType, false);
            }
            return;
        }

        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT));
        String outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, double.class));

        String methodName = "invokeOtherOneFloat" + getClassData().callSiteCount.getAndIncrement() + ':' + JavaNameMangler.mangleMethodName(name);

        adapter2 = new SkinnyMethodAdapter(
                adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.line(line);

        // call site object field
        adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, ci(CallSite.class), null, null).visitEnd();

        // lazily construct it
        adapter2.getstatic(getClassData().clsName, methodName, ci(CallSite.class));
        adapter2.dup();
        Label doCall = new Label();
        adapter2.ifnonnull(doCall);
        adapter2.pop();
        adapter2.ldc(name);
        adapter2.invokestatic(p(MethodIndex.class), "getFastFloatOpsCallSite", sig(CallSite.class, String.class));
        adapter2.dup();
        adapter2.putstatic(getClassData().clsName, methodName, ci(CallSite.class));

        // use call site to invoke
        adapter2.label(doCall);
        adapter2.aload(0); // context
        adapter2.aload(1); // caller
        adapter2.aload(2); // target
        adapter2.ldc(flote); // float

        adapter2.invokevirtual(p(CallSite.class), "call", outgoingSig);
        adapter2.areturn();
        adapter2.end();

        // now call it
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
    }

    public void invokeSelf(String file, int line, String name, int arity, BlockPassType blockPassType, CallType callType, boolean isPotentiallyRefined) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + name + "' has more than " + MAX_ARGUMENTS + " arguments");

        invoke(file, line, name, arity, blockPassType, callType, isPotentiallyRefined);
    }

    public void invokeInstanceSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to instance super has more than " + MAX_ARGUMENTS + " arguments");

        performSuper(file, line, name, arity, hasClosure, splatmap, "instanceSuper", "instanceSuperSplatArgs", false);
    }

    public void invokeClassSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to class super has more than " + MAX_ARGUMENTS + " arguments");

        performSuper(file, line, name, arity, hasClosure, splatmap, "classSuper", "classSuperSplatArgs", false);
    }

    public void invokeUnresolvedSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to unresolved super has more than " + MAX_ARGUMENTS + " arguments");

        performSuper(file, line, name, arity, hasClosure, splatmap, "unresolvedSuper", "unresolvedSuperSplatArgs", true);
    }

    public void invokeZSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to zsuper has more than " + MAX_ARGUMENTS + " arguments");

        performSuper(file, line, name, arity, hasClosure, splatmap, "zSuper", "zSuperSplatArgs", true);
    }

    private void performSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap, String superHelper, String splatHelper, boolean unresolved) {
        SkinnyMethodAdapter adapter2;
        String incomingSig;
        String outgoingSig;

        boolean needsSplatting = IRRuntimeHelpers.needsSplatting(splatmap);

        if (hasClosure) {
            incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class));
        } else {
            incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity));
        }

        if (unresolved) {
            if (needsSplatting) {
                outgoingSig = SUPER_SPLAT_UNRESOLVED;
            } else {
                outgoingSig = SUPER_NOSPLAT_UNRESOLVED;
            }
        } else {
            if (needsSplatting) {
                outgoingSig = SUPER_SPLAT_RESOLVED;
            } else {
                outgoingSig = SUPER_NOSPLAT_RESOLVED;
            }
        }

        String methodName = "invokeSuper" + getClassData().callSiteCount.getAndIncrement() + ':' + JavaNameMangler.mangleMethodName(name);
        adapter2 = new SkinnyMethodAdapter(
                adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.line(line);

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

        if (needsSplatting) {
            String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);
            adapter2.ldc(splatmapString);
            adapter2.invokestatic(p(IRRuntimeHelpers.class), "decodeSplatmap", sig(boolean[].class, String.class));
            adapter2.invokestatic(p(IRRuntimeHelpers.class), splatHelper, outgoingSig);
        } else {
            adapter2.invokestatic(p(IRRuntimeHelpers.class), superHelper, outgoingSig);
        }

        adapter2.areturn();
        adapter2.end();

        // now call it
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
    }

    public void searchConst(String name, boolean noPrivateConsts) {
        adapter.invokedynamic("searchConst", sig(JVM.OBJECT, params(ThreadContext.class, StaticScope.class)), ConstantLookupSite.BOOTSTRAP, name, noPrivateConsts ? 1 : 0);
    }

    public void searchModuleForConst(String name, boolean noPrivateConsts) {
        adapter.invokedynamic("searchModuleForConst", sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class)), ConstantLookupSite.BOOTSTRAP, name, noPrivateConsts ? 1 : 0);
    }

    public void inheritanceSearchConst(String name, boolean noPrivateConsts) {
        adapter.invokedynamic("inheritanceSearchConst", sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class)), ConstantLookupSite.BOOTSTRAP, name, noPrivateConsts ? 1 : 0);
    }

    public void lexicalSearchConst(String name) {
        adapter.invokedynamic("lexicalSearchConst", sig(JVM.OBJECT, params(ThreadContext.class, StaticScope.class)), ConstantLookupSite.BOOTSTRAP, name, 0);
    }

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

        String methodName = (write ? "ivarSet" : "ivarGet") + getClassData().callSiteCount.getAndIncrement() + ':' + JavaNameMangler.mangleMethodName(name);

        adapter2 = new SkinnyMethodAdapter(
                adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        // call site object field
        adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, ci(VariableAccessor.class), null, null).visitEnd();

        final String className = getClassData().clsName;

        // retrieve accessor, verifying if non-null
        adapter2.getstatic(className, methodName, ci(VariableAccessor.class));
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
        adapter2.putstatic(className, methodName, ci(VariableAccessor.class));
        adapter2.areturn();

        adapter2.end();

        // call it from original method to get accessor
        adapter.invokestatic(className, methodName, incomingSig);
    }

    public void array(int length) {
        if (length > MAX_ARGUMENTS) throw new NotCompilableException("literal array has more than " + MAX_ARGUMENTS + " elements");

        // use utility method for supported sizes
        if (length <= RubyArraySpecialized.MAX_PACKED_SIZE) {
            invokeIRHelper("newArray", sig(RubyArray.class, params(ThreadContext.class, IRubyObject.class, length)));
            return;
        }

        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, length));

        final String methodName = "array:" + length;
        final ClassData classData = getClassData();

        if (!classData.arrayMethodsDefined.contains(length)) {
            adapter2 = new SkinnyMethodAdapter(
                    adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    methodName,
                    incomingSig,
                    null,
                    null);

            adapter2.aload(0);
            adapter2.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
            buildArrayFromLocals(adapter2, 1, length);

            adapter2.invokevirtual(p(Ruby.class), "newArrayNoCopy", sig(RubyArray.class, IRubyObject[].class));
            adapter2.areturn();
            adapter2.end();

            classData.arrayMethodsDefined.add(length);
        }

        // now call it
        adapter.invokestatic(classData.clsName, methodName, incomingSig);
    }

    public void hash(int length) {
        if (length > MAX_ARGUMENTS / 2) throw new NotCompilableException("literal hash has more than " + (MAX_ARGUMENTS / 2) + " pairs");

        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, length * 2));

        final String methodName = "hash:" + length;
        final ClassData classData = getClassData();

        if (!classData.hashMethodsDefined.contains(length)) {
            adapter2 = new SkinnyMethodAdapter(
                    adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    methodName,
                    incomingSig,
                    null,
                    null);

            adapter2.aload(0);
            adapter2.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
            buildArrayFromLocals(adapter2, 1, length * 2);

            adapter2.invokestatic(p(IRRuntimeHelpers.class), "constructHashFromArray", sig(RubyHash.class, Ruby.class, IRubyObject[].class));
            adapter2.areturn();
            adapter2.end();

            classData.hashMethodsDefined.add(length);
        }

        // now call it
        adapter.invokestatic(classData.clsName, methodName, incomingSig);
    }

    public void kwargsHash(int length) {
        if (length > MAX_ARGUMENTS / 2) throw new NotCompilableException("kwargs hash has more than " + (MAX_ARGUMENTS / 2) + " pairs");

        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, RubyHash.class, IRubyObject.class, length * 2));

        final String methodName = "kwargsHash:" + length;
        final ClassData classData = getClassData();

        if (!classData.kwargsHashMethodsDefined.contains(length)) {
            adapter2 = new SkinnyMethodAdapter(
                    adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    methodName,
                    incomingSig,
                    null,
                    null);

            adapter2.aload(0);
            adapter2.aload(1);
            buildArrayFromLocals(adapter2, 2, length * 2);

            adapter2.invokestatic(p(IRRuntimeHelpers.class), "dupKwargsHashAndPopulateFromArray", sig(RubyHash.class, ThreadContext.class, RubyHash.class, IRubyObject[].class));
            adapter2.areturn();
            adapter2.end();

            classData.kwargsHashMethodsDefined.add(length);
        }

        // now call it
        adapter.invokestatic(classData.clsName, methodName, incomingSig);
    }

    public void checkpoint() {
        loadContext();
        adapter.invokevirtual(
                p(ThreadContext.class),
                "callThreadPoll",
                sig(void.class));
    }

    @Override
    public void getGlobalVariable(String name, String file, int line) {
        loadContext();
        adapter.invokedynamic(
                "get:" + JavaNameMangler.mangleMethodName(name),
                sig(IRubyObject.class, ThreadContext.class),
                Bootstrap.global(),
                file, line);
    }

    @Override
    public void setGlobalVariable(String name, String file, int line) {
        loadContext();
        adapter.invokedynamic(
                "set:" + JavaNameMangler.mangleMethodName(name),
                sig(void.class, IRubyObject.class, ThreadContext.class),
                Bootstrap.global(),
                file, line);
    }

    @Override
    public void yield(boolean unwrap) {
        adapter.invokedynamic("yield", sig(JVM.OBJECT, params(ThreadContext.class, Block.class, JVM.OBJECT)), YieldSite.BOOTSTRAP, unwrap ? 1 : 0);
    }

    @Override
    public void yieldSpecific() {
        adapter.invokedynamic("yieldSpecific", sig(JVM.OBJECT, params(ThreadContext.class, Block.class)), YieldSite.BOOTSTRAP, 0);
    }

    @Override
    public void yieldValues(int arity) {
        adapter.invokedynamic("yieldValues", sig(JVM.OBJECT, params(ThreadContext.class, Block.class, JVM.OBJECT, arity)), YieldSite.BOOTSTRAP, 0);
    }

    @Override
    public void prepareBlock(Handle handle, org.jruby.runtime.Signature signature, String className) {
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

        invokeIRHelper("prepareBlock", sig(Block.class, ThreadContext.class, IRubyObject.class, DynamicScope.class, BlockBody.class));
    }

    @Override
    public void callEqq(boolean isSplattedValue) {
        String siteName = getUniqueSiteName("===");
        IRBytecodeAdapter.cacheCallSite(adapter, getClassData().clsName, siteName, "===", CallType.FUNCTIONAL, false);
        adapter.ldc(isSplattedValue);
        invokeIRHelper("isEQQ", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, CallSite.class, boolean.class));
    }

    private final Map<Object, String> cacheFieldNames = new HashMap<>();
}
