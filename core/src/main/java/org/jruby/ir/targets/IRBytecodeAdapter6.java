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
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.EQQInstr;
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
 * Baseline JIT backend for literals, calls, etc that uses indy sparingly to reduce warmup/startup hit.
 */
public class IRBytecodeAdapter6 extends IRBytecodeAdapter{

    public static final String SUPER_SPLAT_UNRESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class));
    public static final String SUPER_NOSPLAT_UNRESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, JVM.OBJECT_ARRAY, Block.class));
    public static final String SUPER_SPLAT_RESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, String.class, RubyModule.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class));
    public static final String SUPER_NOSPLAT_RESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, String.class, RubyModule.class, JVM.OBJECT_ARRAY, Block.class));

    public IRBytecodeAdapter6(SkinnyMethodAdapter adapter, Signature signature, ClassData classData) {
        super(adapter, signature, classData);
    }

    public void pushFixnum(long l) {
        loadContext();
        adapter.invokedynamic("fixnum", sig(JVM.OBJECT, ThreadContext.class), FixnumObjectSite.BOOTSTRAP, l);
    }

    public void pushFloat(double d) {
        loadContext();
        adapter.invokedynamic("flote", sig(JVM.OBJECT, ThreadContext.class), FloatObjectSite.BOOTSTRAP, d);
    }

    public void pushString(ByteList bl, int cr) {
        loadContext();
        adapter.invokedynamic("string", sig(RubyString.class, ThreadContext.class), Bootstrap.string(), RubyEncoding.decodeISO(bl), bl.getEncoding().toString(), cr);
    }

    public void pushFrozenString(ByteList bl, int cr, String file, int line) {
        loadContext();
        adapter.invokedynamic("frozen", sig(RubyString.class, ThreadContext.class), Bootstrap.fstring(), RubyEncoding.decodeISO(bl), bl.getEncoding().toString(), cr, file, line);
    }

    public void pushByteList(ByteList bl) {
        adapter.invokedynamic("bytelist", sig(ByteList.class), Bootstrap.bytelist(), RubyEncoding.decodeISO(bl), bl.getEncoding().toString());
    }

    public void pushRegexp(ByteList source, int options) {
        loadContext();
        adapter.invokedynamic("regexp", sig(RubyRegexp.class, ThreadContext.class), RegexpObjectSite.BOOTSTRAP, RubyEncoding.decodeISO(source), source.getEncoding().toString(), options);
    }

    public void pushDRegexp(Runnable callback, RegexpOptions options, int arity) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("dynamic regexp has more than " + MAX_ARGUMENTS + " elements");

        String cacheField = null;
        Label done = null;

        if (options.isOnce()) {
            // need to cache result forever
            cacheField = "dregexp" + getClassData().cacheFieldCount.getAndIncrement();
            done = new Label();
            adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, cacheField, ci(RubyRegexp.class), null, null).visitEnd();
            adapter.getstatic(getClassData().clsName, cacheField, ci(RubyRegexp.class));
            adapter.dup();
            adapter.ifnonnull(done);
            adapter.pop();
        }

        // We may evaluate these operands multiple times or the upstream instrs that created them, which is a bug (jruby/jruby#2798).
        // However, only one dregexp will ever come out of the indy call.
        callback.run();
        adapter.invokedynamic("dregexp", sig(RubyRegexp.class, params(ThreadContext.class, RubyString.class, arity)), DRegexpObjectSite.BOOTSTRAP, options.toEmbeddedOptions());

        if (done != null) {
            adapter.dup();
            adapter.putstatic(getClassData().clsName, cacheField, ci(RubyRegexp.class));
            adapter.label(done);
        }
    }

    public void pushSymbol(final ByteList bytes) {
        loadContext();
        adapter.invokedynamic("symbol", sig(JVM.OBJECT, ThreadContext.class), SymbolObjectSite.BOOTSTRAP, RubyEncoding.decodeISO(bytes), bytes.getEncoding().toString());
    }

    public void pushSymbolProc(final ByteList bytes) {
        loadContext();
        adapter.invokedynamic("symbolProc", sig(JVM.OBJECT, ThreadContext.class), SymbolProcObjectSite.BOOTSTRAP, RubyEncoding.decodeISO(bytes), bytes.getEncoding().toString());
    }

    public void loadRuntime() {
        loadContext();
        adapter.invokedynamic("runtime", sig(Ruby.class, ThreadContext.class), Bootstrap.contextValue());
    }

    public void pushEncoding(Encoding encoding) {
        loadContext();
        adapter.invokedynamic("encoding", sig(RubyEncoding.class, ThreadContext.class), Bootstrap.contextValueString(), new String(encoding.getName()));
    }

    @Override
    public void invokeOther(String file, int line, String scopeFieldName, CallBase call, int arity) {
        invoke(file, line, scopeFieldName, call, arity);
    }

    @Override
    public void invokeArrayDeref(String file, int line, String scopeFieldName, CallBase call) {
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyString.class));
        String methodName = getUniqueSiteName(call.getId());
        SkinnyMethodAdapter adapter2 = new SkinnyMethodAdapter(
                adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.aloadMany(0, 1, 2, 3);
        cacheCallSite(adapter2, getClassData().clsName, methodName, scopeFieldName, call);
        adapter2.invokestatic(p(IRRuntimeHelpers.class), "callOptimizedAref", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, RubyString.class, CallSite.class));
        adapter2.areturn();
        adapter2.end();

        // now call it
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
    }

    @Override
    public void invokeAsString(String file, int line, String scopeFieldName, CallBase call) {
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT));
        String methodName = getUniqueSiteName(call.getId());
        SkinnyMethodAdapter adapter2 = new SkinnyMethodAdapter(
                adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.aloadMany(0, 1, 2);
        cacheCallSite(adapter2, getClassData().clsName, methodName, scopeFieldName, call);
        adapter2.invokestatic(p(IRRuntimeHelpers.class), "asString", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, CallSite.class));
        adapter2.areturn();
        adapter2.end();

        // now call it
        adapter.invokestatic(getClassData().clsName, methodName, incomingSig);
    }

    public void invoke(String file, int lineNumber, String scopeFieldName, CallBase call, int arity) {
        String id = call.getId();
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + id + "' has more than " + MAX_ARGUMENTS + " arguments");

        SkinnyMethodAdapter adapter2;
        String incomingSig;
        String outgoingSig;

        BlockPassType blockPassType = BlockPassType.fromIR(call);
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

        String methodName = getUniqueSiteName(id);

        adapter2 = new SkinnyMethodAdapter(
                adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.line(lineNumber);

        cacheCallSite(adapter2, getClassData().clsName, methodName, scopeFieldName, call);

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

    @Override
    public void invokeOtherOneFixnum(String file, int line, CallBase call, long fixnum) {
        String id = call.getId();
        if (!MethodIndex.hasFastFixnumOps(id)) {
            pushFixnum(fixnum);
            if (call.getCallType() == CallType.NORMAL) {
                invokeOther(file, line, null, call, 1);
            } else {
                invokeSelf(file, line, null, call, 1);
            }
            return;
        }

        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT));
        String outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, long.class));

        String methodName = "invokeOtherOneFixnum" + getClassData().cacheFieldCount.getAndIncrement() + ":" + JavaNameMangler.mangleMethodName(id);

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
        adapter2.ldc(id);
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

    @Override
    public void invokeOtherOneFloat(String file, int line, CallBase call, double flote) {
        String id = call.getId();
        if (!MethodIndex.hasFastFloatOps(id)) {
            pushFloat(flote);
            if (call.getCallType() == CallType.NORMAL) {
                invokeOther(file, line, null, call, 1);
            } else {
                invokeSelf(file, line, null, call, 1);
            }
            return;
        }

        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT));
        String outgoingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, double.class));

        String methodName = "invokeOtherOneFloat" + getClassData().cacheFieldCount.getAndIncrement() + ':' + JavaNameMangler.mangleMethodName(id);

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
        adapter2.ldc(id);
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

    public void invokeSelf(String file, int line, String scopeFieldName, CallBase call, int arity) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + call.getId() + "' has more than " + MAX_ARGUMENTS + " arguments");

        invoke(file, line, scopeFieldName, call, arity);
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

        String methodName = "invokeSuper" + getClassData().cacheFieldCount.getAndIncrement() + ':' + JavaNameMangler.mangleMethodName(name);
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
        adapter.invokedynamic("searchConst", sig(JVM.OBJECT, params(ThreadContext.class, StaticScope.class)), ConstantLookupSite.BOOTSTRAP, name, noPrivateConsts ? 1 : 0, 1);
    }

    public void searchModuleForConst(String name, boolean noPrivateConsts, boolean callConstMissing) {
        adapter.invokedynamic("searchModuleForConst", sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class)), ConstantLookupSite.BOOTSTRAP, name, noPrivateConsts ? 1 : 0, callConstMissing ? 1 : 0);
    }

    public void inheritanceSearchConst(String name, boolean noPrivateConsts) {
        adapter.invokedynamic("inheritanceSearchConst", sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class)), ConstantLookupSite.BOOTSTRAP, name, noPrivateConsts ? 1 : 0, 1);
    }

    public void lexicalSearchConst(String name) {
        adapter.invokedynamic("lexicalSearchConst", sig(JVM.OBJECT, params(ThreadContext.class, StaticScope.class)), ConstantLookupSite.BOOTSTRAP, name, 0, 1);
    }

    public void pushNil() {
        loadContext();
        adapter.invokedynamic("nil", sig(IRubyObject.class, ThreadContext.class), Bootstrap.contextValue());
    }

    public void pushBoolean(boolean b) {
        loadContext();
        adapter.invokedynamic(b ? "True" : "False", sig(IRubyObject.class, ThreadContext.class), Bootstrap.contextValue());
    }

    public void pushBignum(BigInteger bigint) {
        loadContext();
        adapter.invokedynamic("bignum", sig(RubyBignum.class, ThreadContext.class), BignumObjectSite.BOOTSTRAP, bigint.toString());
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

        String methodName = (write ? "ivarSet" : "ivarGet") + getClassData().cacheFieldCount.getAndIncrement() + ':' + JavaNameMangler.mangleMethodName(name);

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
        Handle scopeHandle = new Handle(
                Opcodes.H_GETSTATIC,
                getClassData().clsName,
                handle.getName() + "_IRScope",
                ci(IRScope.class),
                false);
        long encodedSignature = signature.encode();
        adapter.invokedynamic(handle.getName(), sig(Block.class, ThreadContext.class, IRubyObject.class, DynamicScope.class), Bootstrap.prepareBlock(), handle, scopeHandle, encodedSignature);
    }

    @Override
    public void callEqq(EQQInstr call) {
        IRBytecodeAdapter.cacheCallSite(adapter, getClassData().clsName, getUniqueSiteName(call.getId()), null, call);
        adapter.ldc(call.isSplattedValue());
        invokeIRHelper("isEQQ", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, CallSite.class, boolean.class));
    }
}
