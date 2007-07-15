/*
 * StandardInvocationCompiler.java
 * 
 * Created on Jul 14, 2007, 12:31:00 AM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.compiler.ClosureCallback;
import org.jruby.compiler.InvocationCompiler;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallAdapter;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Dispatcher;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.objectweb.asm.Label;

/**
 *
 * @author headius
 */
public class StandardInvocationCompiler implements InvocationCompiler {
    private static final CodegenUtils cg = CodegenUtils.cg;
    private StandardASMCompiler.AbstractMethodCompiler methodCompiler;
    private SkinnyMethodAdapter method;

    public StandardInvocationCompiler(StandardASMCompiler.AbstractMethodCompiler methodCompiler, SkinnyMethodAdapter method) {
        this.methodCompiler = methodCompiler;
        this.method = method;
    }

    public void invokeAttrAssign(String name) {
        // start with [recv, args]
        // get args[length - 1] and stuff it under the receiver
        // dup args * 2
        method.dup(); // [recv, args, args]
        method.dup(); // [recv, args, args, args]
        method.arraylength(); // [recv, args, args, len]
        method.iconst_1(); // [recv, args, args, len, 1]
        method.isub(); // [recv, args, args, len-1]
        // load from array
        method.arrayload(); // [recv, args, val]
        method.dup_x2(); // [val, recv, args, val]
        method.pop(); // [val, recv, args]
        invokeDynamic(name, true, true, CallType.NORMAL, null, true); // [val, result]
        // pop result, use args[length - 1] captured above
        method.pop(); // [val]
    }

    public void invokeDynamic(String name, ClosureCallback receiverCallback, ClosureCallback argsCallback, CallType callType, ClosureCallback closureArg, boolean attrAssign) {
        String classname = methodCompiler.getScriptCompiler().getClassname();
        
        String fieldname = methodCompiler.getScriptCompiler().cacheCallAdapter(name, callType);
        
        if (receiverCallback != null) {
            receiverCallback.compile(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }
        
        // load call adapter
        // FIXME: These swaps suck, but OpAsgn breaks if it can't dup receiver in the middle of making this call :(
        method.getstatic(classname, fieldname, cg.ci(CallAdapter.class));
        method.swap();

        methodCompiler.loadThreadContext(); // [adapter, tc]
        method.swap();
        
        String signature;
        // args
        if (argsCallback == null) {
            // block
            if (closureArg == null) {
                // no args, no block
                signature = cg.sig(IRubyObject.class, cg.params(ThreadContext.class, IRubyObject.class));
            } else {
                // no args, with block
                closureArg.compile(methodCompiler);
                signature = cg.sig(IRubyObject.class, cg.params(ThreadContext.class, IRubyObject.class, Block.class));
            }
        } else {
            argsCallback.compile(methodCompiler);
            // block
            if (closureArg == null) {
                // with args, no block
                signature = cg.sig(IRubyObject.class, cg.params(ThreadContext.class, IRubyObject.class, IRubyObject[].class));
            } else {
                // with args, with block
                closureArg.compile(methodCompiler);
                signature = cg.sig(IRubyObject.class, cg.params(ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class));
            }
        }
        // adapter, tc, recv, args{0,1}, block{0,1}]
        if (closureArg != null) {
            // wrap with try/catch for block flow-control exceptions
            // FIXME: for flow-control from containing blocks, but it's not working right;
            // stack is not as expected for invoke calls below...
            //method.trycatch(tryBegin, tryEnd, tryCatch, cg.p(JumpException.class));
        }

        method.invokevirtual(cg.p(CallAdapter.class), "call", signature);

        if (closureArg != null) {
            // no physical break, terminate loop and skip catch block
            // FIXME: for flow-control from containing blocks, but it's not working right;
            // stack is not as expected for invoke calls below...
            //            Label normalEnd = new Label();
            //            method.go_to(normalEnd);
            //
            //            method.label(tryCatch);
            //            {
            //                loadClosure();
            //                invokeUtilityMethod("handleJumpException", cg.sig(IRubyObject.class, cg.params(JumpException.class, Block.class)));
            //            }
            //
        }
    }

    private void invokeDynamic(String name, boolean hasReceiver, boolean hasArgs, CallType callType, ClosureCallback closureArg, boolean attrAssign) {
        String callSig = cg.sig(IRubyObject.class, cg.params(IRubyObject.class, IRubyObject[].class, ThreadContext.class, String.class, IRubyObject.class, CallType.class, Block.class));
        String callSigIndexed = cg.sig(IRubyObject.class, cg.params(IRubyObject.class, IRubyObject[].class, ThreadContext.class, Byte.TYPE, String.class, IRubyObject.class, CallType.class, Block.class));

        int index = MethodIndex.getIndex(name);

        if (hasArgs) {
            if (hasReceiver) {
                // Call with args
                // receiver already present
            } else {
                // FCall
                // no receiver present, use self
                methodCompiler.loadSelf();
                // put self under args
                method.swap();
            }
        } else {
            if (hasReceiver) {
                // receiver already present
                // empty args list
                method.getstatic(cg.p(IRubyObject.class), "NULL_ARRAY", cg.ci(IRubyObject[].class));
            } else {
                // VCall
                // no receiver present, use self
                methodCompiler.loadSelf();

                // empty args list
                method.getstatic(cg.p(IRubyObject.class), "NULL_ARRAY", cg.ci(IRubyObject[].class));
            }
        }

        methodCompiler.loadThreadContext();

        if (index != 0) {
            // load method index
            method.ldc(new Integer(index));
        }

        method.ldc(name);

        // load self for visibility checks
        methodCompiler.loadSelf();

        method.getstatic(cg.p(CallType.class), callType.toString(), cg.ci(CallType.class));

        if (closureArg == null) {
            method.getstatic(cg.p(Block.class), "NULL_BLOCK", cg.ci(Block.class));
        } else {
            closureArg.compile(methodCompiler);
        }

        Label tryBegin = new Label();
        Label tryEnd = new Label();
        Label tryCatch = new Label();
        if (closureArg != null) {
            // wrap with try/catch for block flow-control exceptions
            // FIXME: for flow-control from containing blocks, but it's not working right;
            // stack is not as expected for invoke calls below...
            //method.trycatch(tryBegin, tryEnd, tryCatch, cg.p(JumpException.class));
            method.label(tryBegin);
        }

        if (attrAssign) {
            if (index != 0) {
                methodCompiler.invokeUtilityMethod("doAttrAssignIndexed", callSigIndexed);
            } else {
                methodCompiler.invokeUtilityMethod("doAttrAssign", callSig);
            }
        } else {
            if (index != 0) {
                methodCompiler.invokeUtilityMethod("doInvokeDynamicIndexed", callSigIndexed);
            } else {
                methodCompiler.invokeUtilityMethod("doInvokeDynamic", callSig);
            }
        }

        if (closureArg != null) {
            method.label(tryEnd);

            // no physical break, terminate loop and skip catch block
            Label normalEnd = new Label();
            method.go_to(normalEnd);

            method.label(tryCatch);
            {
                methodCompiler.loadClosure();
                methodCompiler.invokeUtilityMethod("handleJumpException", cg.sig(IRubyObject.class, cg.params(JumpException.class, Block.class)));
            }

            method.label(normalEnd);
        }
    }

    public void yield(boolean hasArgs, boolean unwrap) {
        methodCompiler.loadClosure();

        if (hasArgs) {
            method.swap();

            methodCompiler.loadThreadContext();
            method.swap();

            // args now here
        } else {
            methodCompiler.loadThreadContext();

            // empty args
            method.aconst_null();
        }

        if (unwrap) {
            method.checkcast(cg.p(RubyArray.class));
            method.invokevirtual(cg.p(RubyArray.class), "toJavaArray", cg.sig(IRubyObject[].class));
        } else {
            methodCompiler.createObjectArray(1);
        }

        method.aconst_null();
        method.aconst_null();
        method.ldc(new Boolean(unwrap));

        method.invokevirtual(cg.p(Block.class), "yield", cg.sig(IRubyObject.class, cg.params(ThreadContext.class, IRubyObject[].class, IRubyObject.class, RubyModule.class, Boolean.TYPE)));
    }
}
