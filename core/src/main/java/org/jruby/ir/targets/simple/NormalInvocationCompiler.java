package org.jruby.ir.targets.simple;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.EQQInstr;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.InvocationCompiler;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.targets.indy.Bootstrap;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.MonomorphicCallSite;
import org.jruby.runtime.callsite.ProfilingCachingCallSite;
import org.jruby.runtime.callsite.RefinedCachingCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

public class NormalInvocationCompiler implements InvocationCompiler {
    private IRBytecodeAdapter compiler;

    public static final String SUPER_SPLAT_UNRESOLVED = CodegenUtils.sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class));
    public static final String SUPER_NOSPLAT_UNRESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, JVM.OBJECT_ARRAY, Block.class));
    public static final String SUPER_SPLAT_RESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, String.class, RubyModule.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class));
    public static final String SUPER_NOSPLAT_RESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, String.class, RubyModule.class, JVM.OBJECT_ARRAY, Block.class));

    public NormalInvocationCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void invokeOther(String file, int line, String scopeFieldName, CallBase call, int arity) {
        invoke(file, line, scopeFieldName, call, arity);
    }

    @Override
    public void invokeArrayDeref(String file, int line, String scopeFieldName, CallBase call) {
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyString.class));
        String methodName = compiler.getUniqueSiteName(call.getId());
        SkinnyMethodAdapter adapter2 = new SkinnyMethodAdapter(
                compiler.adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.aloadMany(0, 1, 2, 3);
        cacheCallSite(adapter2, compiler.getClassData().clsName, methodName, scopeFieldName, call);
        adapter2.invokestatic(p(IRRuntimeHelpers.class), "callOptimizedAref", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, RubyString.class, CallSite.class));
        adapter2.areturn();
        adapter2.end();

        // now call it
        compiler.adapter.invokestatic(compiler.getClassData().clsName, methodName, incomingSig);
    }

    @Override
    public void invokeAsString(String file, int line, String scopeFieldName, CallBase call) {
        String incomingSig = sig(RubyString.class, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT));
        String methodName = compiler.getUniqueSiteName(call.getId());
        SkinnyMethodAdapter adapter2 = new SkinnyMethodAdapter(
                compiler.adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.aloadMany(0, 1, 2);
        cacheCallSite(adapter2, compiler.getClassData().clsName, methodName, scopeFieldName, call);
        adapter2.invokestatic(p(IRRuntimeHelpers.class), "asString", sig(RubyString.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, CallSite.class));
        adapter2.areturn();
        adapter2.end();

        // now call it
        compiler.adapter.invokestatic(compiler.getClassData().clsName, methodName, incomingSig);
    }

    public void invoke(String file, int lineNumber, String scopeFieldName, CallBase call, int arity) {
        String id = call.getId();
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to `" + id + "' has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        SkinnyMethodAdapter adapter2;
        String incomingSig;
        String outgoingSig;

        IRBytecodeAdapter.BlockPassType blockPassType = IRBytecodeAdapter.BlockPassType.fromIR(call);
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

        String methodName = compiler.getUniqueSiteName(id);

        adapter2 = new SkinnyMethodAdapter(
                compiler.adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.line(lineNumber);

        cacheCallSite(adapter2, compiler.getClassData().clsName, methodName, scopeFieldName, call);

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
                IRBytecodeAdapter.buildArrayFromLocals(adapter2, 3, arity);
                if (blockGiven) adapter2.aload(3 + arity);
                break;
        }

        adapter2.invokevirtual(p(CachingCallSite.class), blockPassType.literal() ? "callIter" : "call", outgoingSig);
        adapter2.areturn();
        adapter2.end();

        // now call it
        compiler.adapter.invokestatic(compiler.getClassData().clsName, methodName, incomingSig);
    }

    @Override
    public void invokeOtherOneFixnum(String file, int line, CallBase call, long fixnum) {
        String id = call.getId();
        if (!MethodIndex.hasFastFixnumOps(id)) {
            compiler.getValueCompiler().pushFixnum(fixnum);
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

        String methodName = "invokeOtherOneFixnum" + compiler.getClassData().cacheFieldCount.getAndIncrement() + ":" + JavaNameMangler.mangleMethodName(id);

        adapter2 = new SkinnyMethodAdapter(
                compiler.adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.line(line);

        // call site object field
        compiler.adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, ci(CallSite.class), null, null).visitEnd();

        // lazily construct it
        adapter2.getstatic(compiler.getClassData().clsName, methodName, ci(CallSite.class));
        adapter2.dup();
        Label doCall = new Label();
        adapter2.ifnonnull(doCall);
        adapter2.pop();
        adapter2.ldc(id);
        adapter2.invokestatic(p(MethodIndex.class), "getFastFixnumOpsCallSite", sig(CallSite.class, String.class));
        adapter2.dup();
        adapter2.putstatic(compiler.getClassData().clsName, methodName, ci(CallSite.class));

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
        compiler.adapter.invokestatic(compiler.getClassData().clsName, methodName, incomingSig);
    }

    @Override
    public void invokeOtherOneFloat(String file, int line, CallBase call, double flote) {
        String id = call.getId();
        if (!MethodIndex.hasFastFloatOps(id)) {
            compiler.getValueCompiler().pushFloat(flote);
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

        String methodName = "invokeOtherOneFloat" + compiler.getClassData().cacheFieldCount.getAndIncrement() + ':' + JavaNameMangler.mangleMethodName(id);

        adapter2 = new SkinnyMethodAdapter(
                compiler.adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        adapter2.line(line);

        // call site object field
        compiler.adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, ci(CallSite.class), null, null).visitEnd();

        // lazily construct it
        adapter2.getstatic(compiler.getClassData().clsName, methodName, ci(CallSite.class));
        adapter2.dup();
        Label doCall = new Label();
        adapter2.ifnonnull(doCall);
        adapter2.pop();
        adapter2.ldc(id);
        adapter2.invokestatic(p(MethodIndex.class), "getFastFloatOpsCallSite", sig(CallSite.class, String.class));
        adapter2.dup();
        adapter2.putstatic(compiler.getClassData().clsName, methodName, ci(CallSite.class));

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
        compiler.adapter.invokestatic(compiler.getClassData().clsName, methodName, incomingSig);
    }

    public void invokeSelf(String file, int line, String scopeFieldName, CallBase call, int arity) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to `" + call.getId() + "' has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        invoke(file, line, scopeFieldName, call, arity);
    }

    public void invokeInstanceSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to instance super has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        performSuper(file, line, name, arity, hasClosure, splatmap, "instanceSuper", "instanceSuperSplatArgs", false);
    }

    public void invokeClassSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to class super has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        performSuper(file, line, name, arity, hasClosure, splatmap, "classSuper", "classSuperSplatArgs", false);
    }

    public void invokeUnresolvedSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to unresolved super has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        performSuper(file, line, name, arity, hasClosure, splatmap, "unresolvedSuper", "unresolvedSuperSplatArgs", true);
    }

    public void invokeZSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to zsuper has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

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

        String methodName = "invokeSuper" + compiler.getClassData().cacheFieldCount.getAndIncrement() + ':' + JavaNameMangler.mangleMethodName(name);
        adapter2 = new SkinnyMethodAdapter(
                compiler.adapter.getClassVisitor(),
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

        IRBytecodeAdapter.buildArrayFromLocals(adapter2, 4, arity);

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
        compiler.adapter.invokestatic(compiler.getClassData().clsName, methodName, incomingSig);
    }

    @Override
    public void invokeEQQ(EQQInstr call) {
        cacheCallSite(compiler.adapter, compiler.getClassData().clsName, compiler.getUniqueSiteName(call.getId()), null, call);
        compiler.adapter.ldc(call.isSplattedValue());
        compiler.invokeIRHelper("isEQQ", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, CallSite.class, boolean.class));
    }

    /**
     * Utility to lazily construct and cache a call site object.
     *
     * @param method the SkinnyMethodAdapter to that's generating the containing method body
     * @param className the name of the class in which the field will reside
     * @param siteName the unique name of the site, used for the field
     * @param call of we are making a callsite for.
     */
    public static void cacheCallSite(SkinnyMethodAdapter method, String className, String siteName, String scopeFieldName, CallBase call) {
        CallType callType = call.getCallType();
        boolean profileCandidate = call.hasLiteralClosure() && scopeFieldName != null && IRManager.IR_INLINER;
        boolean profiled = false;
        boolean refined = call.isPotentiallyRefined();

        boolean specialSite = profiled || refined || profileCandidate;

        if (!specialSite) {
            // use indy to cache the site object
            method.invokedynamic("callSite", sig(CachingCallSite.class), Bootstrap.CALLSITE, call.getId(), callType.ordinal());
            return;
        }

        // site requires special handling (usually refined or profiled that need scope present)
        Class<? extends CachingCallSite> siteClass;
        String signature;

        // call site object field
        method.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, siteName, ci(CachingCallSite.class), null, null).visitEnd();

        // lazily construct it
        method.getstatic(className, siteName, ci(CachingCallSite.class));
        method.dup();
        Label doCall = new Label();
        method.ifnonnull(doCall);
        method.pop();
        method.ldc(call.getId());
        if (refined) {
            siteClass = RefinedCachingCallSite.class;
            signature = sig(siteClass, String.class, StaticScope.class, String.class);
            method.getstatic(className, scopeFieldName, ci(StaticScope.class));
            method.ldc(callType.name());
        } else {
            switch (callType) {
                case NORMAL:
                    if (profileCandidate) {
                        profiled = true;
                        siteClass = ProfilingCachingCallSite.class;
                    } else {
                        siteClass = MonomorphicCallSite.class;
                    }
                    break;
                case FUNCTIONAL:
                    if (profileCandidate) {
                        profiled = true;
                        siteClass = ProfilingCachingCallSite.class;
                    } else {
                        siteClass = FunctionalCachingCallSite.class;
                    }
                    break;
                case VARIABLE:
                    siteClass = VariableCachingCallSite.class;
                    break;
                default:
                    throw new RuntimeException("BUG: Unexpected call type " + callType + " in JVM6 invoke logic");
            }
            if (profiled) {
                method.getstatic(className, scopeFieldName, ci(IRScope.class));
                method.ldc(call.getCallSiteId());
                signature = sig(CallType.class, siteClass, String.class, IRScope.class, long.class);
            } else {
                signature = sig(siteClass, String.class);
            }
        }
        method.invokestatic(p(IRRuntimeHelpers.class), "new" + siteClass.getSimpleName(), signature);
        method.dup();
        method.putstatic(className, siteName, ci(CachingCallSite.class));

        method.label(doCall);
    }
}
