/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.compiler.impl;

import org.jruby.RubyModule;
import org.jruby.compiler.ArgumentsCallback;
import org.jruby.compiler.CompilerCallback;
import org.jruby.compiler.InvocationCompiler;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.*;
import org.objectweb.asm.Label;

/**
 *
 * @author headius
 */
public class StandardInvocationCompiler implements InvocationCompiler {
    private StandardASMCompiler.AbstractMethodCompiler methodCompiler;
    private SkinnyMethodAdapter method;
    
    private static final int THIS = 0;

    public StandardInvocationCompiler(StandardASMCompiler.AbstractMethodCompiler methodCompiler, SkinnyMethodAdapter method) {
        this.methodCompiler = methodCompiler;
        this.method = method;
    }

    public SkinnyMethodAdapter getMethodAdapter() {
        return this.method;
    }

    public void setMethodAdapter(SkinnyMethodAdapter sma) {
        this.method = sma;
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

    public void invokeAttrAssign(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback) {
        Label variableCallType = new Label();
        Label readyForCall = new Label();
        
        // receiver first, so we know which call site to use
        receiverCallback.call(methodCompiler);
        
        // select appropriate call site
        method.dup(); // dup receiver
        methodCompiler.loadSelf(); // load self
        method.if_acmpeq(variableCallType); // compare
        
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(method, name, CallType.NORMAL);
        method.go_to(readyForCall);
        method.label(variableCallType);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(method, name, CallType.VARIABLE);
        method.label(readyForCall);
        
        // call site under receiver
        method.swap();
        
        // load thread context under receiver
        methodCompiler.loadThreadContext();
        method.swap();
        
        String signature = null;
        switch (argsCallback.getArity()) {
        case 1:
            signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));
            break;
        case 2:
            signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
            break;
        case 3:
            signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
            break;
        default:
            signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject[].class));
        }
        
        argsCallback.call(methodCompiler);
        
        // store in temp variable
        int tempLocal = methodCompiler.variableCompiler.grabTempLocal();
        switch (argsCallback.getArity()) {
        case 1:
        case 2:
        case 3:
            // specific-arity args, just save off top of stack
            method.dup();
            break;
        default:
            // variable-arity args, peel off and save the last argument in the array
            method.dup(); // [args, args]
            method.dup(); // [args, args, args]
            method.arraylength(); // [args, args, len]
            method.iconst_1(); // [args, args, len, 1]
            method.isub(); // [args, args, len-1]
            // load from array
            method.arrayload(); // [args, val]
        }
        methodCompiler.variableCompiler.setTempLocal(tempLocal);
        
        // invoke call site
        method.invokevirtual(p(CallSite.class), "call", signature);
        
        // pop the return value and restore the dup'ed arg on the stack
        method.pop();
        
        methodCompiler.variableCompiler.getTempLocal(tempLocal);
        methodCompiler.variableCompiler.releaseTempLocal();
    }
    
    public void opElementAsgn(CompilerCallback valueCallback, String operator) {
        // FIXME: op element asgn is not yet using CallAdapter. Boo hoo.
        
        // receiver and args are already on the stack
        methodCompiler.method.dup2();
        
        // invoke the [] operator and dup the result
        invokeDynamic("[]", true, true, CallType.FUNCTIONAL, null, false);
        methodCompiler.method.dup();
        
        // stack is now: .. receiver, args, result, result
        Label end = new Label();
        if (operator == "||") {
            Label falseResult = new Label();
            methodCompiler.invokeIRubyObject("isTrue", sig(boolean.class)); // receiver, args, result, istrue
            methodCompiler.method.ifeq(falseResult); // receiver, args, result
            
            // it's true, clear everything but the result
            methodCompiler.method.dup_x2(); // result, receiver, args, result
            methodCompiler.method.pop(); // result, receiver, args
            methodCompiler.method.pop2(); // result
            methodCompiler.method.go_to(end);
            
            // it's false, stuff the element in
            methodCompiler.method.label(falseResult);
            // START: .. receiver, args, result
            methodCompiler.method.pop(); // receiver, args
            valueCallback.call(methodCompiler); // receiver, args, value
            // save the value rather than using the result of the []= call
            methodCompiler.method.dup_x2(); // value, receiver, args, value
            methodCompiler.appendToObjectArray(); // value, receiver, combinedArgs
            invokeDynamic("[]=", true, true, CallType.FUNCTIONAL, null, false); // value, assignmentResult
            // pop result
            methodCompiler.method.pop();
            
            methodCompiler.method.label(end);
        } else if (operator == "&&") {
            // TODO: This is the reverse of the above logic, and could probably be abstracted better
            Label falseResult = new Label();
            methodCompiler.invokeIRubyObject("isTrue", sig(boolean.class));
            methodCompiler.method.ifeq(falseResult);
            
            // it's true, stuff the element in
            // START: .. receiver, args, result
            methodCompiler.method.pop(); // receiver, args
            valueCallback.call(methodCompiler); // receiver, args, value
            methodCompiler.appendToObjectArray(); // receiver, combinedArgs
            invokeDynamic("[]=", true, true, CallType.FUNCTIONAL, null, false); // assignmentResult
            methodCompiler.method.go_to(end);
            
            // it's false, clear everything but the result
            methodCompiler.method.label(falseResult);
            methodCompiler.method.dup_x2();
            methodCompiler.method.pop();
            methodCompiler.method.pop2();
            
            methodCompiler.method.label(end);
        } else {
            // remove extra result, operate on it, and reassign with original args
            methodCompiler.method.pop();
            // START: .. receiver, args, result
            valueCallback.call(methodCompiler); // receiver, args, result, value
            methodCompiler.createObjectArray(1);
            invokeDynamic(operator, true, true, CallType.FUNCTIONAL, null, false); // receiver, args, newresult
            methodCompiler.appendToObjectArray(); // receiver, newargs
            invokeDynamic("[]=", true, true, CallType.FUNCTIONAL, null, false); // assignmentResult
        }
    }

    public void invokeSuper(CompilerCallback argsCallback, CompilerCallback closureArg) {
        methodCompiler.loadThreadContext();
        methodCompiler.invokeUtilityMethod("checkSuperDisabled", sig(void.class, ThreadContext.class));
        
        methodCompiler.loadSelf();

        methodCompiler.loadThreadContext(); // [self, tc]
        
        // args
        if (argsCallback == null) {
            method.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
            // block
            if (closureArg == null) {
                // no args, no block
                methodCompiler.loadBlock();
            } else {
                // no args, with block
                closureArg.call(methodCompiler);
            }
        } else {
            argsCallback.call(methodCompiler);
            // block
            if (closureArg == null) {
                // with args, no block
                methodCompiler.loadBlock();
            } else {
                // with args, with block
                closureArg.call(methodCompiler);
            }
        }
        
        method.invokeinterface(p(IRubyObject.class), "callSuper", sig(IRubyObject.class, ThreadContext.class, IRubyObject[].class, Block.class));
    }

    public void invokeDynamic(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback, CallType callType, CompilerCallback closureArg) {
        if (receiverCallback != null) {
            receiverCallback.call(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }
        
        // load call adapter
        // FIXME: These swaps suck, but OpAsgn breaks if it can't dup receiver in the middle of making this call :(
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(method, name, callType);
        method.swap();

        methodCompiler.loadThreadContext(); // [adapter, tc]
        method.swap();
        
        String signature;
        // args
        if (argsCallback == null) {
            // block
            if (closureArg == null) {
                // no args, no block
                signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class));
            } else {
                // no args, with block
                closureArg.call(methodCompiler);
                signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, Block.class));
            }
        } else {
            argsCallback.call(methodCompiler);
            // block
            if (closureArg == null) {
                // with args, no block
                switch (argsCallback.getArity()) {
                case 1:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));
                    break;
                case 2:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
                    break;
                case 3:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
                    break;
                default:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject[].class));
                }
            } else {
                // with args, with block
                closureArg.call(methodCompiler);
                
                switch (argsCallback.getArity()) {
                case 1:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class));
                    break;
                case 2:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
                    break;
                case 3:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
                    break;
                default:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class));
                }
            }
        }
        
        // adapter, tc, recv, args{0,1}, block{0,1}]

        method.invokevirtual(p(CallSite.class), "call", signature);
    }

    public void invokeOpAsgnWithMethod(String operatorName, String attrName, String attrAsgnName, CompilerCallback receiverCallback, ArgumentsCallback argsCallback) {
        methodCompiler.loadThreadContext(); // [adapter, tc]
        receiverCallback.call(methodCompiler);
        argsCallback.call(methodCompiler);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(method, attrName, CallType.FUNCTIONAL);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(method, operatorName, CallType.FUNCTIONAL);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(method, attrAsgnName, CallType.NORMAL);
        
        methodCompiler.invokeUtilityMethod("opAsgnWithMethod",
                sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, CallSite.class, CallSite.class, CallSite.class));
    }

    private void invokeDynamic(String name, boolean hasReceiver, boolean hasArgs, CallType callType, CompilerCallback closureArg, boolean attrAssign) {
        String callSig = sig(IRubyObject.class, params(IRubyObject.class, IRubyObject[].class, ThreadContext.class, String.class, IRubyObject.class, CallType.class, Block.class));
        String callSigIndexed = sig(IRubyObject.class, params(IRubyObject.class, IRubyObject[].class, ThreadContext.class, Byte.TYPE, String.class, IRubyObject.class, CallType.class, Block.class));

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
                method.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
            } else {
                // VCall
                // no receiver present, use self
                methodCompiler.loadSelf();

                // empty args list
                method.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
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

        method.getstatic(p(CallType.class), callType.toString(), ci(CallType.class));

        if (closureArg == null) {
            method.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
        } else {
            closureArg.call(methodCompiler);
        }

        Label tryBegin = new Label();
        Label tryEnd = new Label();
        Label tryCatch = new Label();
        if (closureArg != null) {
            // wrap with try/catch for block flow-control exceptions
            // FIXME: for flow-control from containing blocks, but it's not working right;
            // stack is not as expected for invoke calls below...
            //method.trycatch(tryBegin, tryEnd, tryCatch, p(JumpException.class));
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
                methodCompiler.loadBlock();
                methodCompiler.invokeUtilityMethod("handleJumpException", sig(IRubyObject.class, params(JumpException.class, Block.class)));
            }

            method.label(normalEnd);
        }
    }

    public void yield(boolean hasArgs, boolean unwrap) {
        methodCompiler.loadBlock();

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

        method.aconst_null();
        method.aconst_null();
        method.ldc(new Boolean(unwrap));

        method.invokevirtual(p(Block.class), "yield", sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, RubyModule.class, Boolean.TYPE)));
    }

    public void invokeEqq() {
        // receiver and args already present on the stack
        
        // load call adapter under receiver
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(method, "===", CallType.NORMAL);
        method.dup_x2();
        method.pop();

        methodCompiler.loadThreadContext(); // [adapter, tc]
        method.dup_x2();
        method.pop();
        
        String signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));
        
        method.invokevirtual(p(CallSite.class), "call", signature);
    }
}
