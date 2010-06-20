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

import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.compiler.ArgumentsCallback;
import org.jruby.compiler.BodyCompiler;
import org.jruby.compiler.CompilerCallback;
import org.jruby.compiler.InvocationCompiler;
import org.jruby.compiler.NotCompilableException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.*;
import org.objectweb.asm.Label;

/**
 *
 * @author headius
 */
public class StandardInvocationCompiler implements InvocationCompiler {
    protected BaseBodyCompiler methodCompiler;
    protected SkinnyMethodAdapter method;

    public StandardInvocationCompiler(BaseBodyCompiler methodCompiler, SkinnyMethodAdapter method) {
        this.methodCompiler = methodCompiler;
        this.method = method;
    }

    public SkinnyMethodAdapter getMethodAdapter() {
        return this.method;
    }

    public void setMethodAdapter(SkinnyMethodAdapter sma) {
        this.method = sma;
    }

    public void invokeAttrAssignMasgn(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback) {
        // value is already on stack, save it for later
        int temp = methodCompiler.getVariableCompiler().grabTempLocal();
        methodCompiler.getVariableCompiler().setTempLocal(temp);
        
        // receiver first, so we know which call site to use
        receiverCallback.call(methodCompiler);

        // select appropriate call site
        method.dup(); // dup receiver
        methodCompiler.loadSelf(); // load self
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, name, CallType.NORMAL);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, name, CallType.VARIABLE);
        methodCompiler.invokeUtilityMethod("selectAttrAsgnCallSite", sig(CallSite.class, IRubyObject.class, IRubyObject.class, CallSite.class, CallSite.class));

        String signature = null;
        if (argsCallback == null) {
            signature = sig(IRubyObject.class,
                    IRubyObject.class /*receiver*/,
                    CallSite.class,
                    IRubyObject.class /*value*/,
                    ThreadContext.class,
                    IRubyObject.class /*self*/);
        } else {
            switch (argsCallback.getArity()) {
            case 1:
                argsCallback.call(methodCompiler);
                signature = sig(IRubyObject.class,
                        IRubyObject.class, /*receiver*/
                        CallSite.class,
                        IRubyObject.class, /*arg0*/
                        IRubyObject.class, /*value*/
                        ThreadContext.class,
                        IRubyObject.class /*self*/);
                break;
            case 2:
                argsCallback.call(methodCompiler);
                signature = sig(IRubyObject.class,
                        IRubyObject.class, /*receiver*/
                        CallSite.class,
                        IRubyObject.class, /*arg0*/
                        IRubyObject.class, /*arg1*/
                        IRubyObject.class, /*value*/
                        ThreadContext.class,
                        IRubyObject.class /*self*/);
                break;
            case 3:
                argsCallback.call(methodCompiler);
                signature = sig(IRubyObject.class,
                        IRubyObject.class, /*receiver*/
                        CallSite.class,
                        IRubyObject.class, /*arg0*/
                        IRubyObject.class, /*arg1*/
                        IRubyObject.class, /*arg2*/
                        IRubyObject.class, /*value*/
                        ThreadContext.class,
                        IRubyObject.class /*self*/);
                break;
            default:
                argsCallback.call(methodCompiler);
                signature = sig(IRubyObject.class,
                        IRubyObject.class, /*receiver*/
                        CallSite.class,
                        IRubyObject[].class, /*args*/
                        IRubyObject.class, /*value*/
                        ThreadContext.class,
                        IRubyObject.class /*self*/);
            }
        }

        methodCompiler.getVariableCompiler().getTempLocal(temp);
        methodCompiler.getVariableCompiler().releaseTempLocal();
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();

        methodCompiler.invokeUtilityMethod("doAttrAsgn", signature);
    }

    public void invokeAttrAssign(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback) {
        // receiver first, so we know which call site to use
        receiverCallback.call(methodCompiler);
        
        // select appropriate call site
        method.dup(); // dup receiver
        methodCompiler.loadSelf(); // load self
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, name, CallType.NORMAL);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, name, CallType.VARIABLE);
        methodCompiler.invokeUtilityMethod("selectAttrAsgnCallSite", sig(CallSite.class, IRubyObject.class, IRubyObject.class, CallSite.class, CallSite.class));
        
        String signature = null;
        switch (argsCallback.getArity()) {
        case 1:
            signature = sig(IRubyObject.class,
                    IRubyObject.class, /*receiver*/
                    CallSite.class,
                    IRubyObject.class, /*value*/
                    ThreadContext.class,
                    IRubyObject.class /*self*/);
            break;
        case 2:
            signature = sig(IRubyObject.class,
                    IRubyObject.class, /*receiver*/
                    CallSite.class,
                    IRubyObject.class, /*arg0*/
                    IRubyObject.class, /*value*/
                    ThreadContext.class,
                    IRubyObject.class /*self*/);
            break;
        case 3:
            signature = sig(IRubyObject.class,
                    IRubyObject.class, /*receiver*/
                    CallSite.class,
                    IRubyObject.class, /*arg0*/
                    IRubyObject.class, /*arg1*/
                    IRubyObject.class, /*value*/
                    ThreadContext.class,
                    IRubyObject.class /*self*/);
            break;
        default:
            signature = sig(IRubyObject.class,
                    IRubyObject.class, /*receiver*/
                    CallSite.class,
                    IRubyObject[].class, /*args*/
                    ThreadContext.class,
                    IRubyObject.class /*self*/);
        }
        
        argsCallback.call(methodCompiler);
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        
        methodCompiler.invokeUtilityMethod("doAttrAsgn", signature);
    }
    
    public void opElementAsgnWithOr(CompilerCallback receiver, ArgumentsCallback args, CompilerCallback valueCallback) {
        // get call site and thread context
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, "[]", CallType.FUNCTIONAL);
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        
        // evaluate and save receiver and args
        receiver.call(methodCompiler);
        args.call(methodCompiler);
        method.dup2();
        int argsLocal = methodCompiler.getVariableCompiler().grabTempLocal();
        methodCompiler.getVariableCompiler().setTempLocal(argsLocal);
        int receiverLocal = methodCompiler.getVariableCompiler().grabTempLocal();
        methodCompiler.getVariableCompiler().setTempLocal(receiverLocal);
        
        // invoke
        switch (args.getArity()) {
        case 1:
            method.invokevirtual(p(CallSite.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
            break;
        default:
            method.invokevirtual(p(CallSite.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class));
        }
        
        // check if it's true, ending if so
        method.dup();
        methodCompiler.invokeIRubyObject("isTrue", sig(boolean.class));
        Label done = new Label();
        method.ifne(done);
        
        // not true, eval value and assign
        method.pop();
        // thread context, receiver and original args
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        methodCompiler.getVariableCompiler().getTempLocal(receiverLocal);
        methodCompiler.getVariableCompiler().getTempLocal(argsLocal);
        
        // eval value for assignment
        valueCallback.call(methodCompiler);
        
        // call site
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, "[]=", CallType.FUNCTIONAL);
        
        // depending on size of original args, call appropriate utility method
        switch (args.getArity()) {
        case 0:
            throw new NotCompilableException("Op Element Asgn with zero-arity args");
        case 1:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithOrPartTwoOneArg", 
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, CallSite.class));
            break;
        case 2:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithOrPartTwoTwoArgs", 
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class, IRubyObject.class, CallSite.class));
            break;
        case 3:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithOrPartTwoThreeArgs", 
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class, IRubyObject.class, CallSite.class));
            break;
        default:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithOrPartTwoNArgs", 
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class, IRubyObject.class, CallSite.class));
            break;
        }
        
        method.label(done);
        
        methodCompiler.getVariableCompiler().releaseTempLocal();
        methodCompiler.getVariableCompiler().releaseTempLocal();
    }
    
    public void opElementAsgnWithAnd(CompilerCallback receiver, ArgumentsCallback args, CompilerCallback valueCallback) {
        // get call site and thread context
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, "[]", CallType.FUNCTIONAL);
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        
        // evaluate and save receiver and args
        receiver.call(methodCompiler);
        args.call(methodCompiler);
        method.dup2();
        int argsLocal = methodCompiler.getVariableCompiler().grabTempLocal();
        methodCompiler.getVariableCompiler().setTempLocal(argsLocal);
        int receiverLocal = methodCompiler.getVariableCompiler().grabTempLocal();
        methodCompiler.getVariableCompiler().setTempLocal(receiverLocal);
        
        // invoke
        switch (args.getArity()) {
        case 1:
            method.invokevirtual(p(CallSite.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
            break;
        default:
            method.invokevirtual(p(CallSite.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class));
        }
        
        // check if it's true, ending if not
        method.dup();
        methodCompiler.invokeIRubyObject("isTrue", sig(boolean.class));
        Label done = new Label();
        method.ifeq(done);
        
        // not true, eval value and assign
        method.pop();
        // thread context, receiver and original args
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        methodCompiler.getVariableCompiler().getTempLocal(receiverLocal);
        methodCompiler.getVariableCompiler().getTempLocal(argsLocal);
        
        // eval value and save it
        valueCallback.call(methodCompiler);
        
        // call site
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, "[]=", CallType.FUNCTIONAL);
        
        // depending on size of original args, call appropriate utility method
        switch (args.getArity()) {
        case 0:
            throw new NotCompilableException("Op Element Asgn with zero-arity args");
        case 1:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithOrPartTwoOneArg", 
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, CallSite.class));
            break;
        case 2:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithOrPartTwoTwoArgs", 
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class, IRubyObject.class, CallSite.class));
            break;
        case 3:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithOrPartTwoThreeArgs", 
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class, IRubyObject.class, CallSite.class));
            break;
        default:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithOrPartTwoNArgs", 
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class, IRubyObject.class, CallSite.class));
            break;
        }
        
        method.label(done);
        
        methodCompiler.getVariableCompiler().releaseTempLocal();
        methodCompiler.getVariableCompiler().releaseTempLocal();
    }
    
    public void opElementAsgnWithMethod(CompilerCallback receiver, ArgumentsCallback args, CompilerCallback valueCallback, String operator) {
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        receiver.call(methodCompiler);
        args.call(methodCompiler);
        valueCallback.call(methodCompiler); // receiver, args, result, value
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, "[]", CallType.FUNCTIONAL);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, operator, CallType.NORMAL);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, "[]=", CallType.FUNCTIONAL);
        
        switch (args.getArity()) {
        case 0:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithMethod",
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, CallSite.class, CallSite.class, CallSite.class));
            break;
        case 1:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithMethod",
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, CallSite.class, CallSite.class, CallSite.class));
            break;
        case 2:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithMethod",
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, CallSite.class, CallSite.class, CallSite.class));
            break;
        case 3:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithMethod",
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, CallSite.class, CallSite.class, CallSite.class));
            break;
        default:
            methodCompiler.invokeUtilityMethod("opElementAsgnWithMethod",
                    sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class, IRubyObject.class, CallSite.class, CallSite.class, CallSite.class));
            break;
        }
    }

    public void invokeBinaryFixnumRHS(String name, CompilerCallback receiverCallback, long fixnum) {
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, name, CallType.NORMAL);
        methodCompiler.loadThreadContext(); // [adapter, tc]

        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        methodCompiler.loadSelf();

        if (receiverCallback != null) {
            receiverCallback.call(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }

        method.ldc(fixnum);

        String signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, long.class));
        String callSiteMethod = "call";

        method.invokevirtual(p(CallSite.class), callSiteMethod, signature);
    }

    public void invokeBinaryFloatRHS(String name, CompilerCallback receiverCallback, double flote) {
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, name, CallType.NORMAL);
        methodCompiler.loadThreadContext(); // [adapter, tc]

        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        methodCompiler.loadSelf();

        if (receiverCallback != null) {
            receiverCallback.call(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }

        method.ldc(flote);

        String signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, double.class));
        String callSiteMethod = "call";

        method.invokevirtual(p(CallSite.class), callSiteMethod, signature);
    }

    public void invokeFixnumLong(String rubyName, int moduleGeneration, CompilerCallback receiverCallback, String methodName, long fixnum) {
        receiverCallback.call(methodCompiler);
        final int tmp = methodCompiler.getVariableCompiler().grabTempLocal();
        method.astore(tmp);

        Label slow = new Label();
        Label after = new Label();
        if (!RubyInstanceConfig.NOGUARDS_COMPILE_ENABLED) {
            method.aload(tmp);
            method.ldc(moduleGeneration);
            methodCompiler.invokeUtilityMethod("isGenerationEqual", sig(boolean.class, IRubyObject.class, int.class));

            method.ifne(slow);
        }

        method.aload(tmp);
        method.checkcast(p(RubyFixnum.class));
        methodCompiler.loadThreadContext();
        method.ldc(fixnum);

        method.invokevirtual(p(RubyFixnum.class), methodName, sig(IRubyObject.class, ThreadContext.class, long.class));

        if (!RubyInstanceConfig.NOGUARDS_COMPILE_ENABLED) {
            method.go_to(after);
            method.label(slow);

            invokeBinaryFixnumRHS(rubyName, new CompilerCallback() {
                public void call(BodyCompiler context) {
                    method.aload(tmp);
                }
            }, fixnum);

            method.label(after);
        }
        methodCompiler.getVariableCompiler().releaseTempLocal();
    }

    public void invokeFloatDouble(String rubyName, int moduleGeneration, CompilerCallback receiverCallback, String methodName, double flote) {
        receiverCallback.call(methodCompiler);
        final int tmp = methodCompiler.getVariableCompiler().grabTempLocal();
        method.astore(tmp);
        
        method.aload(tmp);
        method.ldc(moduleGeneration);
        methodCompiler.invokeUtilityMethod("isGenerationEqual", sig(boolean.class, IRubyObject.class, int.class));

        Label slow = new Label();
        Label after = new Label();
        method.ifne(slow);

        method.aload(tmp);
        method.checkcast(p(RubyFloat.class));
        methodCompiler.loadThreadContext();
        method.ldc(flote);

        method.invokevirtual(p(RubyFloat.class), methodName, sig(IRubyObject.class, ThreadContext.class, double.class));

        method.go_to(after);
        method.label(slow);

        invokeBinaryFloatRHS(rubyName, new CompilerCallback() {
            public void call(BodyCompiler context) {
                method.aload(tmp);
            }
        }, flote);

        method.label(after);
    }

    public void invokeRecursive(String name, int moduleGeneration, ArgumentsCallback argsCallback, CompilerCallback closure, CallType callType, boolean iterator) {
        Label slow = new Label();
        Label after = new Label();
        if (!RubyInstanceConfig.NOGUARDS_COMPILE_ENABLED) {
            methodCompiler.loadSelf();
            method.ldc(moduleGeneration);
            methodCompiler.invokeUtilityMethod("isGenerationEqual", sig(boolean.class, IRubyObject.class, int.class));

            method.ifne(slow);
        }

        method.aload(0);
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        if (argsCallback != null) {
            argsCallback.call(methodCompiler);
        }
        method.aconst_null();

        method.invokestatic(methodCompiler.getScriptCompiler().getClassname(), methodCompiler.getNativeMethodName(), methodCompiler.getSignature());

        if (!RubyInstanceConfig.NOGUARDS_COMPILE_ENABLED) {
            method.go_to(after);
            method.label(slow);

            invokeDynamic(name, null, argsCallback, callType, closure, iterator);

            method.label(after);
        }
    }

    public void invokeNative(String name, DynamicMethod.NativeCall nativeCall,
            int moduleGeneration, CompilerCallback receiver, ArgumentsCallback args,
            CompilerCallback closure, CallType callType, boolean iterator) {
        Class[] nativeSignature = nativeCall.getNativeSignature();

        int leadingArgs = 0;
        receiver.call(methodCompiler);
        final int tmp = methodCompiler.getVariableCompiler().grabTempLocal();
        method.astore(tmp);

        // validate generation
        Label slow = new Label();
        Label after = new Label();
        if (!RubyInstanceConfig.NOGUARDS_COMPILE_ENABLED) {
            method.aload(tmp);
            method.ldc(moduleGeneration);
            methodCompiler.invokeUtilityMethod("isGenerationEqual", sig(boolean.class, IRubyObject.class, int.class));

            method.ifne(slow);
        }

        if (nativeCall.isStatic()) {
            if (nativeSignature.length > 0 && nativeSignature[0] == ThreadContext.class) {
                methodCompiler.loadThreadContext();
                leadingArgs++;
            }
            method.aload(tmp);
            leadingArgs++;
        } else {
            method.aload(tmp);
            method.checkcast(p(nativeCall.getNativeTarget()));
            if (nativeSignature.length > 0 && nativeSignature[0] == ThreadContext.class) {
                methodCompiler.loadThreadContext();
                leadingArgs++;
            }
        }

        if (args != null) {
            args.call(methodCompiler);
            leadingArgs += args.getArity();
        }

        if (closure != null) {
            closure.call(methodCompiler);
            if (nativeSignature.length == leadingArgs + 1 && nativeSignature[leadingArgs] == Block.class) {
                // ok, pass the block
            } else {
                // doesn't receive block, drop it
                // note: have to evaluate it because it could be a & block arg
                // with side effects
                method.pop();
            }
        } else {
            if (nativeSignature.length == leadingArgs + 1 && nativeSignature[leadingArgs] == Block.class) {
                // needs a block
                method.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
            } else {
                // ok with no block
            }
        }

        if (nativeCall.isStatic()) {
            method.invokestatic(p(nativeCall.getNativeTarget()), nativeCall.getNativeName(), sig(nativeCall.getNativeReturn(), nativeSignature));
        } else {
            method.invokevirtual(p(nativeCall.getNativeTarget()), nativeCall.getNativeName(), sig(nativeCall.getNativeReturn(), nativeSignature));
        }

        if (!RubyInstanceConfig.NOGUARDS_COMPILE_ENABLED) {
            method.go_to(after);
            method.label(slow);

            invokeDynamic(name, new CompilerCallback() {
                public void call(BodyCompiler context) {
                    method.aload(tmp);
                }
            }, args, callType, closure, iterator);

            method.label(after);
        }

        methodCompiler.getVariableCompiler().releaseTempLocal();
    }
    
    public void invokeDynamic(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback, CallType callType, CompilerCallback closureArg, boolean iterator) {
        if (RubyInstanceConfig.INLINE_DYNCALL_ENABLED && closureArg == null) {
            if (callType == CallType.FUNCTIONAL || callType == CallType.VARIABLE) {
                if (argsCallback == null) {
                    invokeDynamicSelfNoBlockZero(name);
                    return;
                } else if (argsCallback.getArity() >= 1 && argsCallback.getArity() <= 3) {
                    invokeDynamicSelfNoBlockSpecificArity(name, argsCallback);
                    return;
                }
//            } else if (callType == CallType.NORMAL) {
//                if (argsCallback == null) {
//                    invokeDynamicNoBlockZero(name, receiverCallback);
//                    return;
//                } else if (argsCallback.getArity() >= 1 && argsCallback.getArity() <= 3) {
//                    invokeDynamicNoBlockSpecificArity(name, receiverCallback, argsCallback);
//                    return;
//                }
            }
        }
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, name, callType);

        methodCompiler.loadThreadContext(); // [adapter, tc]

        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        methodCompiler.loadSelf();
        
        if (receiverCallback != null) {
            receiverCallback.call(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }

        // super uses current block if none given
        if (callType == CallType.SUPER && closureArg == null) {
            closureArg = new CompilerCallback() {
                public void call(BodyCompiler context) {
                    methodCompiler.loadBlock();
                }
            };
        }
        
        String signature;
        String callSiteMethod = "call";
        // args
        if (argsCallback == null) {
            // block
            if (closureArg == null) {
                // no args, no block
                signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));
            } else {
                // no args, with block
                if (iterator) callSiteMethod = "callIter";
                closureArg.call(methodCompiler);
                signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class));
            }
        } else {
            argsCallback.call(methodCompiler);
            // block
            if (closureArg == null) {
                // with args, no block
                switch (argsCallback.getArity()) {
                case 1:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
                    break;
                case 2:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
                    break;
                case 3:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
                    break;
                default:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class));
                }
            } else {
                // with args, with block
                if (iterator) callSiteMethod = "callIter";
                closureArg.call(methodCompiler);
                
                switch (argsCallback.getArity()) {
                case 1:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
                    break;
                case 2:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
                    break;
                case 3:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
                    break;
                default:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class, Block.class));
                }
            }
        }
        
        // adapter, tc, recv, args{0,1}, block{0,1}]

        method.invokevirtual(p(CallSite.class), callSiteMethod, signature);
    }

    public void invokeDynamicSelfNoBlockZero(String name) {
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheMethod(methodCompiler, name);
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        method.dup();
        method.invokeinterface(p(IRubyObject.class), "getMetaClass", sig(RubyClass.class));
        method.ldc(name);
        method.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));
    }

    public void invokeDynamicSelfNoBlockSpecificArity(String name, ArgumentsCallback argsCallback) {
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheMethod(methodCompiler, name);
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        method.dup();
        method.invokeinterface(p(IRubyObject.class), "getMetaClass", sig(RubyClass.class));
        method.ldc(name);
        argsCallback.call(methodCompiler);
        switch (argsCallback.getArity()) {
        case 1:
            method.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));
            break;
        case 2:
            method.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class));
            break;
        case 3:
            method.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
            break;
        }
    }

    public void invokeDynamicNoBlockZero(String name, CompilerCallback receiverCallback) {
        receiverCallback.call(methodCompiler);
        int recv = methodCompiler.getVariableCompiler().grabTempLocal();
        method.astore(recv);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheMethod(methodCompiler, name, recv);
        methodCompiler.loadThreadContext();
        method.aload(recv);
        methodCompiler.getVariableCompiler().releaseTempLocal();
        method.dup();
        method.invokeinterface(p(IRubyObject.class), "getMetaClass", sig(RubyClass.class));
        method.ldc(name);
        method.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));
    }

    public void invokeDynamicNoBlockSpecificArity(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback) {
        receiverCallback.call(methodCompiler);
        int recv = methodCompiler.getVariableCompiler().grabTempLocal();
        method.astore(recv);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheMethod(methodCompiler, name, recv);
        methodCompiler.loadThreadContext();
        method.aload(recv);
        methodCompiler.getVariableCompiler().releaseTempLocal();
        method.dup();
        method.invokeinterface(p(IRubyObject.class), "getMetaClass", sig(RubyClass.class));
        method.ldc(name);
        argsCallback.call(methodCompiler);
        switch (argsCallback.getArity()) {
        case 1:
            method.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));
            break;
        case 2:
            method.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class));
            break;
        case 3:
            method.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
            break;
        }
    }

    public void invokeOpAsgnWithOr(String attrName, String attrAsgnName, CompilerCallback receiverCallback, ArgumentsCallback argsCallback) {
        receiverCallback.call(methodCompiler);
        method.dup();
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, attrName, CallType.FUNCTIONAL);
        
        methodCompiler.invokeUtilityMethod("preOpAsgnWithOrAnd", sig(IRubyObject.class, IRubyObject.class, ThreadContext.class, IRubyObject.class, CallSite.class));
        
        Label done = new Label();
        Label isTrue = new Label();
        
        method.dup();
        methodCompiler.invokeIRubyObject("isTrue", sig(boolean.class));
        method.ifne(isTrue);
        
        method.pop(); // pop extra attr value
        argsCallback.call(methodCompiler);
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, attrAsgnName, CallType.NORMAL);
        
        methodCompiler.invokeUtilityMethod("postOpAsgnWithOrAnd",
                sig(IRubyObject.class, IRubyObject.class, IRubyObject.class, ThreadContext.class, IRubyObject.class, CallSite.class));
        method.go_to(done);
        
        method.label(isTrue);
        method.swap();
        method.pop();
        
        method.label(done);
    }

    public void invokeOpAsgnWithAnd(String attrName, String attrAsgnName, CompilerCallback receiverCallback, ArgumentsCallback argsCallback) {
        receiverCallback.call(methodCompiler);
        method.dup();
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, attrName, CallType.FUNCTIONAL);
        
        methodCompiler.invokeUtilityMethod("preOpAsgnWithOrAnd", sig(IRubyObject.class, IRubyObject.class, ThreadContext.class, IRubyObject.class, CallSite.class));
        
        Label done = new Label();
        Label isFalse = new Label();
        
        method.dup();
        methodCompiler.invokeIRubyObject("isTrue", sig(boolean.class));
        method.ifeq(isFalse);
        
        method.pop(); // pop extra attr value
        argsCallback.call(methodCompiler);
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, attrAsgnName, CallType.NORMAL);
        
        methodCompiler.invokeUtilityMethod("postOpAsgnWithOrAnd",
                sig(IRubyObject.class, IRubyObject.class, IRubyObject.class, ThreadContext.class, IRubyObject.class, CallSite.class));
        method.go_to(done);
        
        method.label(isFalse);
        method.swap();
        method.pop();
        
        method.label(done);
    }

    public void invokeOpAsgnWithMethod(String operatorName, String attrName, String attrAsgnName, CompilerCallback receiverCallback, ArgumentsCallback argsCallback) {
        methodCompiler.loadThreadContext();
        methodCompiler.loadSelf();
        receiverCallback.call(methodCompiler);
        argsCallback.call(methodCompiler);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, attrName, CallType.FUNCTIONAL);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, operatorName, CallType.FUNCTIONAL);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, attrAsgnName, CallType.NORMAL);
        
        methodCompiler.invokeUtilityMethod("opAsgnWithMethod",
                sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, CallSite.class, CallSite.class, CallSite.class));
    }

    public void invokeOpElementAsgnWithMethod(String operatorName, CompilerCallback receiverCallback, ArgumentsCallback argsCallback) {
        methodCompiler.loadThreadContext(); // [adapter, tc]
        methodCompiler.loadSelf();
        receiverCallback.call(methodCompiler);
        argsCallback.call(methodCompiler);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, "[]", CallType.FUNCTIONAL);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, operatorName, CallType.FUNCTIONAL);
        methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, "[]=", CallType.NORMAL);
        
        methodCompiler.invokeUtilityMethod("opElementAsgnWithMethod",
                sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, CallSite.class, CallSite.class, CallSite.class));
    }

    public void yield(CompilerCallback argsCallback, boolean unwrap) {
        methodCompiler.loadBlock();
        methodCompiler.loadThreadContext();

        if (argsCallback != null) {
            argsCallback.call(methodCompiler);
        } else {
            method.aconst_null();
        }

        if (unwrap) {
            method.aconst_null();
            method.aconst_null();
            method.invokevirtual(p(Block.class), "yieldArray", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, RubyModule.class));
        } else {
            method.invokevirtual(p(Block.class), "yield", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
        }
    }

    public void yieldSpecific(ArgumentsCallback argsCallback) {
        methodCompiler.loadBlock();
        methodCompiler.loadThreadContext();

        String signature;
        if (argsCallback == null) {
            signature = sig(IRubyObject.class, ThreadContext.class);
        } else {
            argsCallback.call(methodCompiler);
            switch (argsCallback.getArity()) {
            case 1:
                signature = sig(IRubyObject.class, ThreadContext.class, IRubyObject.class);
                break;
            case 2:
                signature = sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
                break;
            case 3:
                signature = sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class);
                break;
            default:
                throw new NotCompilableException("Can't do specific-arity call for > 3 args yet");
            }
        }

        method.invokevirtual(p(Block.class), "yieldSpecific", signature);
    }

    public void invokeEqq(ArgumentsCallback receivers, CompilerCallback argument) {
        if (argument == null) {
            receivers.call(methodCompiler);

            switch (receivers.getArity()) {
            case 1:
                methodCompiler.invokeUtilityMethod("invokeEqqForCaselessWhen", sig(boolean.class,
                        IRubyObject.class /*receiver*/
                        ));
                break;
            case 2:
                methodCompiler.invokeUtilityMethod("invokeEqqForCaselessWhen", sig(boolean.class,
                        IRubyObject.class, /*receiver*/
                        IRubyObject.class
                        ));
                break;
            case 3:
                methodCompiler.invokeUtilityMethod("invokeEqqForCaselessWhen", sig(boolean.class,
                        IRubyObject.class, /*receiver*/
                        IRubyObject.class,
                        IRubyObject.class
                        ));
                break;
            default:
                methodCompiler.invokeUtilityMethod("invokeEqqForCaselessWhen", sig(boolean.class,
                        IRubyObject[].class /*receiver*/
                        ));
            }
        } else {
            // arg and receiver already present on the stack
            methodCompiler.getScriptCompiler().getCacheCompiler().cacheCallSite(methodCompiler, "===", CallType.NORMAL);
            methodCompiler.loadThreadContext();
            methodCompiler.loadSelf();
            argument.call(methodCompiler);
            receivers.call(methodCompiler);

            switch (receivers.getArity()) {
            case 1:
                methodCompiler.invokeUtilityMethod("invokeEqqForCaseWhen", sig(boolean.class,
                        CallSite.class,
                        ThreadContext.class,
                        IRubyObject.class /*self*/,
                        IRubyObject.class, /*arg*/
                        IRubyObject.class /*receiver*/
                        ));
                break;
            case 2:
                methodCompiler.invokeUtilityMethod("invokeEqqForCaseWhen", sig(boolean.class,
                        CallSite.class,
                        ThreadContext.class,
                        IRubyObject.class /*self*/,
                        IRubyObject.class, /*arg*/
                        IRubyObject.class, /*receiver*/
                        IRubyObject.class
                        ));
                break;
            case 3:
                methodCompiler.invokeUtilityMethod("invokeEqqForCaseWhen", sig(boolean.class,
                        CallSite.class,
                        ThreadContext.class,
                        IRubyObject.class /*self*/,
                        IRubyObject.class, /*arg*/
                        IRubyObject.class, /*receiver*/
                        IRubyObject.class,
                        IRubyObject.class
                        ));
                break;
            default:
                methodCompiler.invokeUtilityMethod("invokeEqqForCaseWhen", sig(boolean.class,
                        CallSite.class,
                        ThreadContext.class,
                        IRubyObject.class /*self*/,
                        IRubyObject.class, /*arg*/
                        IRubyObject[].class /*receiver*/
                        ));
            }
        }
    }
}
