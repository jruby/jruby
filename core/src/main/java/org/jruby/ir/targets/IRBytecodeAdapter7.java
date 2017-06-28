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
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
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
        adapter.invokedynamic("string", sig(RubyString.class, ThreadContext.class), Bootstrap.string(), new String(bl.bytes(), RubyEncoding.ISO), bl.getEncoding().toString(), cr);
    }

    public void pushFrozenString(ByteList bl, int cr, String file, int line) {
        loadContext();
        adapter.invokedynamic("frozen", sig(RubyString.class, ThreadContext.class), Bootstrap.fstring(), new String(bl.bytes(), RubyEncoding.ISO), bl.getEncoding().toString(), cr, file, line);
    }

    public void pushByteList(ByteList bl) {
        adapter.invokedynamic("bytelist", sig(ByteList.class), Bootstrap.bytelist(), new String(bl.bytes(), RubyEncoding.ISO), bl.getEncoding().toString());
    }

    public void pushRegexp(ByteList source, int options) {
        loadContext();
        pushByteList(source);
        adapter.invokedynamic("regexp", sig(RubyRegexp.class, ThreadContext.class, ByteList.class), RegexpObjectSite.BOOTSTRAP, options);
    }

    public void pushDRegexp(Runnable callback, RegexpOptions options, int arity) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("dynamic regexp has more than " + MAX_ARGUMENTS + " elements");

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

    /**
     * Push a symbol on the stack
     * @param sym the symbol's string identifier
     */
    public void pushSymbol(String sym, Encoding encoding) {
        loadContext();
        adapter.invokedynamic("symbol", sig(JVM.OBJECT, ThreadContext.class), SymbolObjectSite.BOOTSTRAP, sym, new String(encoding.getName()));
    }

    public void loadRuntime() {
        loadContext();
        adapter.invokedynamic("runtime", sig(Ruby.class, ThreadContext.class), Bootstrap.contextValue());
    }

    public void pushEncoding(Encoding encoding) {
        loadContext();
        adapter.invokedynamic("encoding", sig(RubyEncoding.class, ThreadContext.class), Bootstrap.contextValueString(), new String(encoding.getName()));
    }

    public void invokeOther(String file, int line, String name, int arity, BlockPassType blockPassType, boolean isPotentiallyRefined) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + name + "' has more than " + MAX_ARGUMENTS + " arguments");
        if (isPotentiallyRefined) {
            super.invokeOther(file, line, name, arity, blockPassType, isPotentiallyRefined);
            return;
        }

        if (blockPassType.given()) {
            if (arity == -1) {
                adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class)), NormalInvokeSite.BOOTSTRAP, blockPassType.literal(), file, line);
            } else {
                adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, arity + 2, Block.class)), NormalInvokeSite.BOOTSTRAP, blockPassType.literal(), file, line);
            }
        } else {
            if (arity == -1) {
                adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY)), NormalInvokeSite.BOOTSTRAP, false, file, line);
            } else {
                adapter.invokedynamic("invoke:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity)), NormalInvokeSite.BOOTSTRAP, false, file, line);
            }
        }
    }

    @Override
    public void invokeArrayDeref(String file, int line) {
        adapter.invokedynamic("aref", sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, 1)), ArrayDerefInvokeSite.BOOTSTRAP, file, line);
    }

    public void invokeOtherOneFixnum(String file, int line, String name, long fixnum, CallType callType) {
        if (!MethodIndex.hasFastFixnumOps(name)) {
            pushFixnum(fixnum);
            if (callType == CallType.NORMAL) {
                invokeOther(file, line, name, 1, BlockPassType.NONE,false);
            } else {
                invokeSelf(file, line, name, 1, BlockPassType.NONE, callType, false);
            }
            return;
        }

        String signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));

        adapter.invokedynamic(
                "fixnumOperator:" + JavaNameMangler.mangleMethodName(name),
                signature,
                Bootstrap.getFixnumOperatorHandle(),
                fixnum,
                callType.ordinal(),
                "",
                0);
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

        String signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));
        
        adapter.invokedynamic(
            "floatOperator:" + JavaNameMangler.mangleMethodName(name),
            signature,
            Bootstrap.getFloatOperatorHandle(),
            flote,
                callType.ordinal(),
            "",
            0);
    }

    public void invokeSelf(String file, int line, String name, int arity, BlockPassType blockPassType, CallType callType, boolean isPotentiallyRefined) {
        if (arity > MAX_ARGUMENTS) throw new NotCompilableException("call to `" + name + "' has more than " + MAX_ARGUMENTS + " arguments");
        if (isPotentiallyRefined) {
            super.invokeSelf(file, line, name, arity, blockPassType, callType, isPotentiallyRefined);
            return;
        }

        String action = callType == CallType.FUNCTIONAL ? "callFunctional" : "callVariable";
        if (blockPassType != BlockPassType.NONE) {
            if (arity == -1) {
                adapter.invokedynamic(action + ":" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY, Block.class)), SelfInvokeSite.BOOTSTRAP, blockPassType.literal(), file, line);
            } else {
                adapter.invokedynamic(action + ":" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, arity + 2, Block.class)), SelfInvokeSite.BOOTSTRAP, blockPassType.literal(), file, line);
            }
        } else {
            if (arity == -1) {
                adapter.invokedynamic(action + ":" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT_ARRAY)), SelfInvokeSite.BOOTSTRAP, false, file, line);
            } else {
                adapter.invokedynamic(action + ":" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, JVM.OBJECT, arity)), SelfInvokeSite.BOOTSTRAP, false, file, line);
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
            adapter.invokedynamic("invokeUnresolvedSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity, Block.class)), Bootstrap.invokeSuper(), splatmapString, file, line);
        } else {
            adapter.invokedynamic("invokeUnresolvedSuper:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, JVM.OBJECT, RubyClass.class, JVM.OBJECT, arity)), Bootstrap.invokeSuper(), splatmapString, file, line);
        }
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
        String bigintStr = bigint.toString();

        loadContext();

        adapter.invokedynamic("bignum", sig(RubyBignum.class, ThreadContext.class), BignumObjectSite.BOOTSTRAP, bigintStr);
    }

    public void putField(String name) {
        adapter.invokedynamic("ivarSet:" + JavaNameMangler.mangleMethodName(name), sig(void.class, IRubyObject.class, IRubyObject.class), Bootstrap.ivar());
    }

    public void getField(String name) {
        adapter.invokedynamic("ivarGet:" + JavaNameMangler.mangleMethodName(name), sig(JVM.OBJECT, IRubyObject.class), Bootstrap.ivar());
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
    public void prepareBlock(Handle handle, org.jruby.runtime.Signature signature, String className) {
        Handle scopeHandle = new Handle(Opcodes.H_GETSTATIC, getClassData().clsName, handle.getName() + "_IRScope", ci(IRScope.class));
        long encodedSignature = signature.encode();
        adapter.invokedynamic(handle.getName(), sig(Block.class, ThreadContext.class, IRubyObject.class, DynamicScope.class), Bootstrap.prepareBlock(), handle, scopeHandle, encodedSignature);
    }
}
