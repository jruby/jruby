/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyHash;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.VariableSite;
import org.jruby.specialized.RubyArraySpecialized;
import org.jruby.util.ByteList;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.RegexpOptions;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.math.BigInteger;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

/**
 *
 * @author headius
 */
public class IRBytecodeAdapter7 extends IRBytecodeAdapter6 {

    public IRBytecodeAdapter7(SkinnyMethodAdapter adapter, Signature signature, ClassData classData) {
        super(adapter, signature, classData);
    }

    @Override
    public void invokeOther(String file, int line, String scopeFieldName, CallBase call, int arity) {
        String id = call.getId();
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + id + "' has more than " + MAX_ARGUMENTS + " arguments");
        if (call.isPotentiallyRefined()) {
            super.invokeOther(file, line, scopeFieldName, call, arity);
            return;
        }

        BlockPassType blockPassType = BlockPassType.fromIR(call);
        if (blockPassType.given()) {
            if (arity == -1) {
                adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class)), NormalInvokeSite.BOOTSTRAP, blockPassType.literal(), file, line);
            } else {
                adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, arity + 2, Block.class)), NormalInvokeSite.BOOTSTRAP, blockPassType.literal(), file, line);
            }
        } else {
            if (arity == -1) {
                adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY)), NormalInvokeSite.BOOTSTRAP, false, file, line);
            } else {
                adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity)), NormalInvokeSite.BOOTSTRAP, false, file, line);
            }
        }
    }

    @Override
    public void invokeArrayDeref(String file, int line, String scopeFieldName, CallBase call) {
        adapter.invokedynamic("aref", sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, 1)), ArrayDerefInvokeSite.BOOTSTRAP, file, line);
    }

    @Override
    public void invokeOtherOneFixnum(String file, int line, CallBase call, long fixnum) {
        String id = call.getId();
        if (!MethodIndex.hasFastFixnumOps(id)) {
            pushFixnum(fixnum);
            invokeOtherOrSelfArity1(file, line, call);
            return;
        }

        String signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));

        adapter.invokedynamic(
                "fixnumOperator:" + JavaNameMangler.mangleMethodName(id),
                signature,
                Bootstrap.getFixnumOperatorHandle(),
                fixnum,
                call.getCallType().ordinal(),
                file,
                line);
    }

    @Override
    public void invokeOtherOneFloat(String file, int line, CallBase call, double flote) {
        String id = call.getId();
        if (!MethodIndex.hasFastFloatOps(id)) {
            pushFloat(flote);
            invokeOtherOrSelfArity1(file, line, call);
            return;
        }

        String signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));
        
        adapter.invokedynamic(
            "floatOperator:" + JavaNameMangler.mangleMethodName(id),
            signature,
            Bootstrap.getFloatOperatorHandle(),
            flote,
                call.getCallType().ordinal(),
            file,
            line);
    }

    private void invokeOtherOrSelfArity1(String file, int line, CallBase call) {
        if (call.getCallType() == CallType.NORMAL) {
            invokeOther(file, line, null, call, 1);
        } else {
            invokeSelf(file, line, null, call, 1);
        }
    }

    @Override
    public void invokeSelf(String file, int line, String scopeFieldName, CallBase call, int arity) {
        String id = call.getId();
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + id + "' has more than " + MAX_ARGUMENTS + " arguments");
        if (call.isPotentiallyRefined()) {
            super.invokeSelf(file, line, scopeFieldName, call, arity);
            return;
        }

        String action = call.getCallType() == CallType.FUNCTIONAL ? "callFunctional" : "callVariable";
        BlockPassType blockPassType = BlockPassType.fromIR(call);
        if (blockPassType != BlockPassType.NONE) {
            if (arity == -1) {
                adapter.invokedynamic(action + ':' + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class)), SelfInvokeSite.BOOTSTRAP, blockPassType.literal(), file, line);
            } else {
                adapter.invokedynamic(action + ':' + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, arity + 2, Block.class)), SelfInvokeSite.BOOTSTRAP, blockPassType.literal(), file, line);
            }
        } else {
            if (arity == -1) {
                adapter.invokedynamic(action + ':' + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY)), SelfInvokeSite.BOOTSTRAP, false, file, line);
            } else {
                adapter.invokedynamic(action + ':' + JavaNameMangler.mangleMethodName(id), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity)), SelfInvokeSite.BOOTSTRAP, false, file, line);
            }
        }
    }

    public void invokeInstanceSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to instance super has more than " + MAX_ARGUMENTS + " arguments");

        String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);
        if (hasClosure) {
            adapter.invokedynamic("invokeInstanceSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class)), Bootstrap.invokeSuper(), splatmapString, file, line);
        } else {
            adapter.invokedynamic("invokeInstanceSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity)), Bootstrap.invokeSuper(), splatmapString, file, line);
        }
    }

    public void invokeClassSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to class super has more than " + MAX_ARGUMENTS + " arguments");

        String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);
        if (hasClosure) {
            adapter.invokedynamic("invokeClassSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class)), Bootstrap.invokeSuper(), splatmapString, file, line);
        } else {
            adapter.invokedynamic("invokeClassSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity)), Bootstrap.invokeSuper(), splatmapString, file, line);
        }
    }

    public void invokeUnresolvedSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to unresolved super has more than " + MAX_ARGUMENTS + " arguments");

        String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);
        if (hasClosure) {
            adapter.invokedynamic("invokeUnresolvedSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class)), Bootstrap.invokeSuper(), splatmapString, file, line);
        } else {
            adapter.invokedynamic("invokeUnresolvedSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity)), Bootstrap.invokeSuper(), splatmapString, file, line);
        }
    }

    public void invokeZSuper(String file, int line, String name, int arity, boolean hasClosure, boolean[] splatmap) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to zsuper has more than " + MAX_ARGUMENTS + " arguments");

        String splatmapString = IRRuntimeHelpers.encodeSplatmap(splatmap);
        if (hasClosure) {
            adapter.invokedynamic("invokeZSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class)), Bootstrap.invokeSuper(), splatmapString, file, line);
        } else {
            adapter.invokedynamic("invokeZSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity)), Bootstrap.invokeSuper(), splatmapString, file, line);
        }
    }

    public void putField(String name) {
        adapter.invokedynamic("ivarSet:" + JavaNameMangler.mangleMethodName(name), sig(void.class, IRubyObject.class, IRubyObject.class), VariableSite.IVAR_ASM_HANDLE);
    }

    public void getField(String name) {
        adapter.invokedynamic("ivarGet:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, IRubyObject.class), VariableSite.IVAR_ASM_HANDLE);
    }

    public void array(int length) {
        if (length > MAX_ARGUMENTS) throw new NotCompilableException("literal array has more than " + MAX_ARGUMENTS + " elements");

        // use utility method for supported sizes
        if (length <= RubyArraySpecialized.MAX_PACKED_SIZE) {
            super.array(length);
            return;
        }

        adapter.invokedynamic("array", sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, length)), Bootstrap.array());
    }

    public void hash(int length) {
        if (length > MAX_ARGUMENTS / 2) throw new NotCompilableException("literal hash has more than " + (MAX_ARGUMENTS / 2) + " pairs");

        adapter.invokedynamic("hash", sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, length * 2)), Bootstrap.hash());
    }

    public void kwargsHash(int length) {
        if (length > MAX_ARGUMENTS / 2) throw new NotCompilableException("kwargs hash has more than " + (MAX_ARGUMENTS / 2) + " pairs");

        adapter.invokedynamic("kwargsHash", sig(JVM.OBJECT, params(ThreadContext.class, RubyHash.class, JVM.OBJECT, length * 2)), Bootstrap.kwargsHash());
    }

    public void checkpoint() {
        loadContext();
        adapter.invokedynamic(
                "checkpoint",
                sig(void.class, ThreadContext.class),
                Bootstrap.checkpointHandle());
    }

    @Override
    public void branchIfNil(Label label) {
        adapter.invokedynamic("isNil", sig(boolean.class, IRubyObject.class), Bootstrap.isNilBoot());
        adapter.iftrue(label);
    }

    @Override
    public void branchIfTruthy(Label label) {
        adapter.invokedynamic("isTrue", sig(boolean.class, IRubyObject.class), Bootstrap.isTrueBoot());
        adapter.iftrue(label);
    }
}
