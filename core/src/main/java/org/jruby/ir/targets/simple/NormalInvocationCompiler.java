package org.jruby.ir.targets.simple;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.EQQInstr;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.InvocationCompiler;
import org.jruby.ir.targets.JVM;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodType;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

public class NormalInvocationCompiler implements InvocationCompiler {
    private final IRBytecodeAdapter compiler;

    public static final String SUPER_SPLAT_UNRESOLVED = CodegenUtils.sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class));
    public static final String SUPER_NOSPLAT_UNRESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, JVM.OBJECT_ARRAY, Block.class));
    public static final String SUPER_SPLAT_RESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, String.class, RubyModule.class, JVM.OBJECT_ARRAY, Block.class, boolean[].class));
    public static final String SUPER_NOSPLAT_RESOLVED = sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, String.class, RubyModule.class, JVM.OBJECT_ARRAY, Block.class));

    public NormalInvocationCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void invokeOther(String file, String scopeFieldName, CallBase call, int arity) {
        invoke(file, compiler.getLastLine(), scopeFieldName, call, arity);
    }

    @Override
    public void invokeArrayDeref(String file, String scopeFieldName, CallBase call) {
        MethodType type = MethodType.methodType(JVM.OBJECT, ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyString.class);
        String incomingSig = sig(type);
        String methodName = compiler.getUniqueSiteName(call.getId());
        String clsName = compiler.getClassData().clsName;

        compiler.outline(methodName, type, () -> {
            compiler.adapter.aloadMany(0, 1, 2, 3);
            compiler.getValueCompiler().pushCallSite(clsName, methodName, scopeFieldName, call);
            compiler.adapter.invokestatic(p(IRRuntimeHelpers.class), "callOptimizedAref", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, RubyString.class, CallSite.class));
            compiler.adapter.areturn();
        });

        // now call it
        compiler.adapter.invokestatic(clsName, methodName, incomingSig);
    }

    public void invoke(String file, int lineNumber, String scopeFieldName, CallBase call, int arity) {
        String id = call.getId();
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to `" + id + "' has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        MethodType incoming, outgoing;
        String incomingSig, outgoingSig;

        IRBytecodeAdapter.BlockPassType blockPassType = IRBytecodeAdapter.BlockPassType.fromIR(call);
        boolean blockGiven = blockPassType.given();
        if (blockGiven) {
            switch (arity) {
                case -1:
                    incoming = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class));
                    outgoing = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class));
                    break;
                case 0:
                case 1:
                case 2:
                case 3:
                    incoming = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity, Block.class));
                    outgoing = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity, Block.class));
                    break;
                default:
                    incoming = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity, Block.class));
                    outgoing = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class));
                    break;
            }
        } else {
            switch (arity) {
                case -1:
                    incoming = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY));
                    outgoing = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY));
                    break;
                case 0:
                case 1:
                case 2:
                case 3:
                    incoming = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity));
                    outgoing = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity));
                    break;
                default:
                    incoming = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity));
                    outgoing = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY));
                    break;
            }
        }

        incomingSig = sig(incoming);
        outgoingSig = sig(outgoing);

        String methodName = compiler.getUniqueSiteName(id);

        String clsName = compiler.getClassData().clsName;
        compiler.outline(methodName, incoming, () -> {
            compiler.adapter.line(lineNumber);

            compiler.getValueCompiler().pushCallSite(clsName, methodName, scopeFieldName, call);

            // use call site to invoke
            compiler.adapter.aload(0); // context
            compiler.adapter.aload(1); // caller
            compiler.adapter.aload(2); // self

            switch (arity) {
                case -1:
                case 1:
                    compiler.adapter.aload(3);
                    if (blockGiven) compiler.adapter.aload(4);
                    break;
                case 0:
                    if (blockGiven) compiler.adapter.aload(3);
                    break;
                case 2:
                    compiler.adapter.aload(3);
                    compiler.adapter.aload(4);
                    if (blockGiven) compiler.adapter.aload(5);
                    break;
                case 3:
                    compiler.adapter.aload(3);
                    compiler.adapter.aload(4);
                    compiler.adapter.aload(5);
                    if (blockGiven) compiler.adapter.aload(6);
                    break;
                default:
                    IRBytecodeAdapter.buildArrayFromLocals(compiler.adapter, 3, arity);
                    if (blockGiven) compiler.adapter.aload(3 + arity);
                    break;
            }

            compiler.adapter.invokevirtual(p(CachingCallSite.class), blockPassType.literal() ? "callIter" : "call", outgoingSig);
            compiler.adapter.areturn();
        });

        // now call it
        compiler.adapter.invokestatic(clsName, methodName, incomingSig);
    }

    @Override
    public void invokeOtherOneFixnum(String file, CallBase call, long fixnum) {
        String id = call.getId();
        if (!MethodIndex.hasFastFixnumOps(id)) {
            compiler.getValueCompiler().pushFixnum(fixnum);
            if (call.getCallType() == CallType.NORMAL) {
                invokeOther(file, null, call, 1);
            } else {
                invokeSelf(file, null, call, 1);
            }
            return;
        }

        MethodType incoming = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT));
        MethodType outgoing = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, long.class));
        String incomingSig = sig(incoming);
        String outgoingSig = sig(outgoing);

        String methodName = "invokeOtherOneFixnum" + compiler.getClassData().cacheFieldCount.getAndIncrement() + ":" + JavaNameMangler.mangleMethodName(id);

        compiler.outline(methodName, incoming, () -> {
            SkinnyMethodAdapter adapter = compiler.adapter;
            adapter.line(compiler.getLastLine());

            // call site object field
            adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, ci(CallSite.class), null, null).visitEnd();

            // lazily construct it
            adapter.getstatic(compiler.getClassData().clsName, methodName, ci(CallSite.class));
            adapter.dup();
            Label doCall = new Label();
            adapter.ifnonnull(doCall);
            adapter.pop();
            adapter.ldc(id);
            adapter.invokestatic(p(MethodIndex.class), "getFastFixnumOpsCallSite", sig(CallSite.class, String.class));
            adapter.dup();
            adapter.putstatic(compiler.getClassData().clsName, methodName, ci(CallSite.class));

            // use call site to invoke
            adapter.label(doCall);
            adapter.aload(0); // context
            adapter.aload(1); // caller
            adapter.aload(2); // target
            adapter.ldc(fixnum); // fixnum

            adapter.invokevirtual(p(CallSite.class), "call", outgoingSig);
            adapter.areturn();
        });

        // now call it
        compiler.adapter.invokestatic(compiler.getClassData().clsName, methodName, incomingSig);
    }

    @Override
    public void invokeOtherOneFloat(String file, CallBase call, double flote) {
        String id = call.getId();
        if (!MethodIndex.hasFastFloatOps(id)) {
            compiler.getValueCompiler().pushFloat(flote);
            if (call.getCallType() == CallType.NORMAL) {
                invokeOther(file, null, call, 1);
            } else {
                invokeSelf(file, null, call, 1);
            }
            return;
        }

        MethodType incoming = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT));
        MethodType outgoing = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, double.class));
        String incomingSig = sig(incoming);
        String outgoingSig = sig(outgoing);

        String methodName = "invokeOtherOneFloat" + compiler.getClassData().cacheFieldCount.getAndIncrement() + ':' + JavaNameMangler.mangleMethodName(id);

        compiler.outline(methodName, incoming, () -> {
            SkinnyMethodAdapter adapter = compiler.adapter;
            adapter.line(compiler.getLastLine());

            // call site object field
            adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, ci(CallSite.class), null, null).visitEnd();

            // lazily construct it
            adapter.getstatic(compiler.getClassData().clsName, methodName, ci(CallSite.class));
            adapter.dup();
            Label doCall = new Label();
            adapter.ifnonnull(doCall);
            adapter.pop();
            adapter.ldc(id);
            adapter.invokestatic(p(MethodIndex.class), "getFastFloatOpsCallSite", sig(CallSite.class, String.class));
            adapter.dup();
            adapter.putstatic(compiler.getClassData().clsName, methodName, ci(CallSite.class));

            // use call site to invoke
            adapter.label(doCall);
            adapter.aload(0); // context
            adapter.aload(1); // caller
            adapter.aload(2); // target
            adapter.ldc(flote); // float

            adapter.invokevirtual(p(CallSite.class), "call", outgoingSig);
            adapter.areturn();
        });

        // now call it
        compiler.adapter.invokestatic(compiler.getClassData().clsName, methodName, incomingSig);
    }

    public void invokeSelf(String file, String scopeFieldName, CallBase call, int arity) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to `" + call.getId() + "' has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        invoke(file, compiler.getLastLine(), scopeFieldName, call, arity);
    }

    public void invokeInstanceSuper(String file, String name, int arity, boolean hasClosure, boolean literalClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to instance super has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        String noSplatMethod = literalClosure ? "instanceSuperIter" : "instanceSuper";
        String splatMethod = literalClosure ? "instanceSuperIterSplatArgs" : "instanceSuperSplatArgs";

        performSuper(file, compiler.getLastLine(), name, arity, hasClosure, splatmap, noSplatMethod, splatMethod, false);
    }

    public void invokeClassSuper(String file, String name, int arity, boolean hasClosure, boolean literalClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to class super has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        String noSplatMethod = literalClosure ? "classSuperIter" : "classSuper";
        String splatMethod = literalClosure ? "classSuperIterSplatArgs" : "classSuperSplatArgs";

        performSuper(file, compiler.getLastLine(), name, arity, hasClosure, splatmap, noSplatMethod, splatMethod, false);
    }

    public void invokeUnresolvedSuper(String file, String name, int arity, boolean hasClosure, boolean literalClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to unresolved super has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        String noSplatMethod = literalClosure ? "unresolvedSuperIter" : "unresolvedSuper";
        String splatMethod = literalClosure ? "unresolvedSuperIterSplatArgs" : "unresolvedSuperSplatArgs";

        performSuper(file, compiler.getLastLine(), name, arity, hasClosure, splatmap, noSplatMethod, splatMethod, true);
    }

    public void invokeZSuper(String file, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to zsuper has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        performSuper(file, compiler.getLastLine(), name, arity, hasClosure, splatmap, "zSuper", "zSuperSplatArgs", true);
    }

    private void performSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap, String superHelper, String splatHelper, boolean unresolved) {
        MethodType incoming;
        String incomingSig, outgoingSig;

        boolean needsSplatting = IRRuntimeHelpers.needsSplatting(splatmap);

        if (hasClosure) {
            incoming = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class));
        } else {
            incoming = MethodType.methodType(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity));
        }
        incomingSig = sig(incoming);

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

        compiler.outline(methodName, incoming, () -> {
            SkinnyMethodAdapter adapter = compiler.adapter;
            adapter.line(line);

            // CON FIXME: make these offsets programmatically determined
            adapter.aload(0);
            adapter.aload(2);
            if (!unresolved) adapter.ldc(name);
            if (!unresolved) adapter.aload(3);

            IRBytecodeAdapter.buildArrayFromLocals(adapter, 4, arity);

            if (hasClosure) {
                adapter.aload(4 + arity);
            } else {
                adapter.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
            }

            if (needsSplatting) {
                String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);
                adapter.ldc(splatmapString);
                adapter.invokestatic(p(IRRuntimeHelpers.class), "decodeSplatmap", sig(boolean[].class, String.class));
                adapter.invokestatic(p(IRRuntimeHelpers.class), splatHelper, outgoingSig);
            } else {
                adapter.invokestatic(p(IRRuntimeHelpers.class), superHelper, outgoingSig);
            }

            adapter.areturn();
        });

        // now call it
        compiler.adapter.invokestatic(compiler.getClassData().clsName, methodName, incomingSig);
    }

    @Override
    public void invokeEQQ(EQQInstr call) {
        compiler.getValueCompiler().pushCallSite(compiler.getClassData().clsName, compiler.getUniqueSiteName(call.getId()), null, call);
        compiler.adapter.ldc(call.isSplattedValue());
        compiler.invokeIRHelper("isEQQ", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, CallSite.class, boolean.class));
    }
}
