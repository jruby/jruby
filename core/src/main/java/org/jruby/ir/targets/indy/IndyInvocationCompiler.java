package org.jruby.ir.targets.indy;

import org.jruby.RubyClass;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.EQQInstr;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.InvocationCompiler;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.targets.simple.NormalInvocationCompiler;
import org.jruby.ir.targets.simple.NormalInvokeSite;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JavaNameMangler;

import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

public class IndyInvocationCompiler implements InvocationCompiler {
    private final IRBytecodeAdapter compiler;
    private final InvocationCompiler normalCompiler;

    public IndyInvocationCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
        this.normalCompiler = new NormalInvocationCompiler(compiler);
    }

    @Override
    public void invokeOther(String file, String scopeFieldName, CallBase call, int arity) {
        String id = call.getId();
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to `" + id + "' has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");
        if (call.isPotentiallyRefined()) {
            normalCompiler.invokeOther(file, scopeFieldName, call, arity);
            return;
        }

        IRBytecodeAdapter.BlockPassType blockPassType = IRBytecodeAdapter.BlockPassType.fromIR(call);
        if (blockPassType.given()) {
            if (arity == -1) {
                compiler.adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(id), CodegenUtils.sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class)), NormalInvokeSite.BOOTSTRAP, blockPassType.literal(), file, compiler.getLastLine());
            } else {
                compiler.adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, arity + 2, Block.class)), NormalInvokeSite.BOOTSTRAP, blockPassType.literal(), file, compiler.getLastLine());
            }
        } else {
            if (arity == -1) {
                compiler.adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY)), NormalInvokeSite.BOOTSTRAP, false, file, compiler.getLastLine());
            } else {
                compiler.adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity)), NormalInvokeSite.BOOTSTRAP, false, file, compiler.getLastLine());
            }
        }
    }

    @Override
    public void invokeArrayDeref(String file, String scopeFieldName, CallBase call) {
        compiler.adapter.invokedynamic("aref", sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, 1)), ArrayDerefInvokeSite.BOOTSTRAP, file, compiler.getLastLine());
    }

    @Override
    public void invokeOtherOneFixnum(String file, CallBase call, long fixnum) {
        String id = call.getId();
        if (!MethodIndex.hasFastFixnumOps(id)) {
            compiler.getValueCompiler().pushFixnum(fixnum);
            invokeOtherOrSelfArity1(file, compiler.getLastLine(), call);
            return;
        }

        String signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));

        compiler.adapter.invokedynamic(
                "fixnumOperator:" + JavaNameMangler.mangleMethodName(id),
                signature,
                Bootstrap.getFixnumOperatorHandle(),
                fixnum,
                call.getCallType().ordinal(),
                file,
                compiler.getLastLine());
    }

    @Override
    public void invokeOtherOneFloat(String file, CallBase call, double flote) {
        String id = call.getId();
        if (!MethodIndex.hasFastFloatOps(id)) {
            compiler.getValueCompiler().pushFloat(flote);
            invokeOtherOrSelfArity1(file, compiler.getLastLine(), call);
            return;
        }

        String signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));

        compiler.adapter.invokedynamic(
                "floatOperator:" + JavaNameMangler.mangleMethodName(id),
                signature,
                Bootstrap.getFloatOperatorHandle(),
                flote,
                call.getCallType().ordinal(),
                file,
                compiler.getLastLine());
    }

    private void invokeOtherOrSelfArity1(String file, int line, CallBase call) {
        if (call.getCallType() == CallType.NORMAL) {
            invokeOther(file, null, call, 1);
        } else {
            invokeSelf(file, null, call, 1);
        }
    }

    @Override
    public void invokeSelf(String file, String scopeFieldName, CallBase call, int arity) {
        String id = call.getId();
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to `" + id + "' has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");
        if (call.isPotentiallyRefined()) {
            normalCompiler.invokeSelf(file, scopeFieldName, call, arity);
            return;
        }

        String action = call.getCallType() == CallType.FUNCTIONAL ? "callFunctional" : "callVariable";
        IRBytecodeAdapter.BlockPassType blockPassType = IRBytecodeAdapter.BlockPassType.fromIR(call);
        if (blockPassType != IRBytecodeAdapter.BlockPassType.NONE) {
            if (arity == -1) {
                compiler.adapter.invokedynamic(action + ':' + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class)), SelfInvokeSite.BOOTSTRAP, blockPassType.literal(), file, compiler.getLastLine());
            } else {
                compiler.adapter.invokedynamic(action + ':' + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, arity + 2, Block.class)), SelfInvokeSite.BOOTSTRAP, blockPassType.literal(), file, compiler.getLastLine());
            }
        } else {
            if (arity == -1) {
                compiler.adapter.invokedynamic(action + ':' + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY)), SelfInvokeSite.BOOTSTRAP, false, file, compiler.getLastLine());
            } else {
                compiler.adapter.invokedynamic(action + ':' + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity)), SelfInvokeSite.BOOTSTRAP, false, file, compiler.getLastLine());
            }
        }
    }

    public void invokeInstanceSuper(String file, String name, int arity, boolean hasClosure, boolean literalClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to instance super has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);
        if (hasClosure) {
            String operation = literalClosure ? "invokeInstanceSuperIter" : "invokeInstanceSuper";
            compiler.adapter.invokedynamic(operation + ":" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class)), Bootstrap.invokeSuper(), splatmapString, file, compiler.getLastLine());
        } else {
            compiler.adapter.invokedynamic("invokeInstanceSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity)), Bootstrap.invokeSuper(), splatmapString, file, compiler.getLastLine());
        }
    }

    public void invokeClassSuper(String file, String name, int arity, boolean hasClosure, boolean literalClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to class super has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);
        if (hasClosure) {
            String operation = literalClosure ? "invokeClassSuperIter" : "invokeClassSuper";
            compiler.adapter.invokedynamic(operation + ":" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class)), Bootstrap.invokeSuper(), splatmapString, file, compiler.getLastLine());
        } else {
            compiler.adapter.invokedynamic("invokeClassSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity)), Bootstrap.invokeSuper(), splatmapString, file, compiler.getLastLine());
        }
    }

    public void invokeUnresolvedSuper(String file, String name, int arity, boolean hasClosure, boolean literalClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to unresolved super has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);
        if (hasClosure) {
            String operation = literalClosure ? "invokeUnresolvedSuperIter" : "invokeUnresolvedSuper";
            compiler.adapter.invokedynamic(operation + ":" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class)), Bootstrap.invokeSuper(), splatmapString, file, compiler.getLastLine());
        } else {
            compiler.adapter.invokedynamic("invokeUnresolvedSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity)), Bootstrap.invokeSuper(), splatmapString, file, compiler.getLastLine());
        }
    }

    public void invokeZSuper(String file, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("call to zsuper has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);
        if (hasClosure) {
            compiler.adapter.invokedynamic("invokeZSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class)), Bootstrap.invokeSuper(), splatmapString, file, compiler.getLastLine());
        } else {
            compiler.adapter.invokedynamic("invokeZSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity)), Bootstrap.invokeSuper(), splatmapString, file, compiler.getLastLine());
        }
    }

    @Override
    public void invokeEQQ(EQQInstr call) {
        normalCompiler.invokeEQQ(call);
    }
}
