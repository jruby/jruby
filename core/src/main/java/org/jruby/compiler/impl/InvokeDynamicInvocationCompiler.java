/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.compiler.impl;

import org.jruby.RubyInstanceConfig;
import org.jruby.compiler.ArgumentsCallback;
import org.jruby.compiler.BodyCompiler;
import org.jruby.compiler.CompilerCallback;
import org.jruby.compiler.NotCompilableException;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Label;

import static org.jruby.util.CodegenUtils.*;

/**
 *
 * @author headius
 */
public class InvokeDynamicInvocationCompiler extends StandardInvocationCompiler {
    public InvokeDynamicInvocationCompiler(BaseBodyCompiler methodCompiler, SkinnyMethodAdapter method) {
        super(methodCompiler, method);
    }

    @Override
    public void invokeAttrAssign(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback, boolean selfCall, boolean expr) {
        methodCompiler.loadThreadContext(); // [adapter, tc]
        
        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        methodCompiler.loadSelf();

        if (receiverCallback != null) {
            receiverCallback.call(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }

        String signature;

        argsCallback.call(methodCompiler);
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
        
        // adapter, tc, recv, args{0,1}, block{0,1}]
        method.invokedynamic(
                "attrAssign" + (selfCall ? "Self" : "") + (expr ? "Expr" : "") + ":" + JavaNameMangler.mangleMethodName(name),
                signature,
                InvokeDynamicSupport.getInvocationHandle(),
                methodCompiler.getScriptCompiler().getSourcename(),
                methodCompiler.getLastLine() + 1);
        
        // TODO: void invokedynamic to avoid pop
        if (!expr) method.pop();
    }

    @Override
    public void invokeDynamic(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback, CallType callType, CompilerCallback closureArg, boolean iterator) {
        if (callType == CallType.SUPER) {
            super.invokeDynamic(name, receiverCallback, argsCallback, callType, closureArg, iterator);
            return;
        }
        
        methodCompiler.loadThreadContext(); // [adapter, tc]
        
        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        methodCompiler.loadSelf();

        if (receiverCallback != null) {
            receiverCallback.call(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }

        String invokeName;
        if (iterator) {
            switch (callType) {
                case NORMAL:        invokeName = "callIter"; break;
                case FUNCTIONAL:    invokeName = "fcallIter"; break;
                case VARIABLE:      invokeName = "vcallIter"; break;
                default:            throw new NotCompilableException("unknown call type " + callType);
            }
        } else {
            switch (callType) {
                case NORMAL:        invokeName = "call"; break;
                case FUNCTIONAL:    invokeName = "fcall"; break;
                case VARIABLE:      invokeName = "vcall"; break;
                default:            throw new NotCompilableException("unknown call type " + callType);
            }
        }
        invokeName += ":" + JavaNameMangler.mangleMethodName(name);
        String signature;

        // args
        if (argsCallback == null) {
            // block
            if (closureArg == null) {
                // no args, no block
                signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));
            } else {
                // no args, with block
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
        method.invokedynamic(invokeName,
                signature,
                InvokeDynamicSupport.getInvocationHandle(),
                methodCompiler.getScriptCompiler().getSourcename(),
                methodCompiler.getLastLine() + 1);
    }

    public void invokeDynamicVarargs(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback, CallType callType, CompilerCallback closureArg, boolean iterator) {
        if (callType == CallType.SUPER) {
            super.invokeDynamic(name, receiverCallback, argsCallback, callType, closureArg, iterator);
            return;
        }

        assert argsCallback.getArity() == -1;

        methodCompiler.loadThreadContext(); // [adapter, tc]

        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        methodCompiler.loadSelf();

        if (receiverCallback != null) {
            receiverCallback.call(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }

        String invokeName;
        if (iterator) {
            switch (callType) {
                case NORMAL:        invokeName = "callIter"; break;
                case FUNCTIONAL:    invokeName = "fcallIter"; break;
                default:            throw new NotCompilableException("unknown call type " + callType);
            }
        } else {
            switch (callType) {
                case NORMAL:        invokeName = "call"; break;
                case FUNCTIONAL:    invokeName = "fcall"; break;
                default:            throw new NotCompilableException("unknown call type " + callType);
            }
        }
        invokeName += ":" + JavaNameMangler.mangleMethodName(name);
        String signature;

        argsCallback.call(methodCompiler);

        // block
        if (closureArg == null) {
            signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class));
        } else {
            closureArg.call(methodCompiler);

            signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class, Block.class));
        }

        // adapter, tc, recv, args{0,1}, block{0,1}]
        method.invokedynamic(invokeName,
                signature,
                InvokeDynamicSupport.getInvocationHandle(),
                methodCompiler.getScriptCompiler().getSourcename(),
                methodCompiler.getLastLine() + 1);
    }

    @Override
    public void invokeEqq(ArgumentsCallback receivers, final CompilerCallback argument) {
        if (argument == null) {
            super.invokeEqq(receivers, argument);
        } else {
            if (receivers.getArity() == 1) {
                invokeDynamic("===", receivers, new ArgumentsCallback() {
                    public int getArity() {
                        return 1;
                    }

                    public void call(BodyCompiler context) {
                        argument.call(context);
                    }
                }, CallType.FUNCTIONAL, null, false);
                methodCompiler.isTrue();
            } else {
                super.invokeEqq(receivers, argument);
            }
        }
    }

    @Override
    public void yieldSpecific(ArgumentsCallback argsCallback) {
        methodCompiler.loadBlock();
        methodCompiler.loadThreadContext();

        String signature;
        if (argsCallback == null) {
            signature = sig(IRubyObject.class, Block.class, ThreadContext.class);
        } else {
            argsCallback.call(methodCompiler);
            switch (argsCallback.getArity()) {
            case 1:
                signature = sig(IRubyObject.class, Block.class, ThreadContext.class, IRubyObject.class);
                break;
            case 2:
                signature = sig(IRubyObject.class, Block.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
                break;
            case 3:
                signature = sig(IRubyObject.class, Block.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class);
                break;
            default:
                throw new NotCompilableException("Can't do specific-arity call for > 3 args yet");
            }
        }

        method.invokedynamic(
                "yieldSpecific",
                signature,
                InvokeDynamicSupport.getInvocationHandle(),
                methodCompiler.getScriptCompiler().getSourcename(),
                methodCompiler.getLastLine() + 1);
    }

    @Override
    public void invokeBinaryFixnumRHS(String name, CompilerCallback receiverCallback, final long fixnum) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_FASTOPS) {
            ArgumentsCallback argumentsCallback = new ArgumentsCallback() {
                @Override
                public int getArity() {
                    return 1;
                }

                @Override
                public void call(BodyCompiler context) {
                    methodCompiler.getScriptCompiler().getCacheCompiler().cacheFixnum(methodCompiler, fixnum);
                }
            };
            invokeDynamic(name, receiverCallback, argumentsCallback, CallType.NORMAL, null, false);
            return;
        }
        
        methodCompiler.loadThreadContext(); // [adapter, tc]

        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        methodCompiler.loadSelf();

        if (receiverCallback != null) {
            receiverCallback.call(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }

        String signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));

        method.invokedynamic(
                "fixnumOperator:" + JavaNameMangler.mangleMethodName(name),
                signature,
                InvokeDynamicSupport.getFixnumOperatorHandle(),
                fixnum,
                methodCompiler.getScriptCompiler().getSourcename(),
                methodCompiler.getLastLine() + 1);
    }

    @Override
    public void invokeBinaryBooleanFixnumRHS(String name, CompilerCallback receiverCallback, final long fixnum) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_FASTOPS) {
            ArgumentsCallback argumentsCallback = new ArgumentsCallback() {
                @Override
                public int getArity() {
                    return 1;
                }

                @Override
                public void call(BodyCompiler context) {
                    methodCompiler.getScriptCompiler().getCacheCompiler().cacheFixnum(methodCompiler, fixnum);
                }
            };
            invokeDynamic(name, receiverCallback, argumentsCallback, CallType.NORMAL, null, false);
            methodCompiler.isTrue();
            return;
        }
        
        methodCompiler.loadThreadContext(); // [adapter, tc]

        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        methodCompiler.loadSelf();

        if (receiverCallback != null) {
            receiverCallback.call(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }

        String signature = sig(boolean.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));

        method.invokedynamic(
                "fixnumBoolean:" + JavaNameMangler.mangleMethodName(name),
                signature,
                InvokeDynamicSupport.getFixnumBooleanHandle(),
                fixnum,
                methodCompiler.getScriptCompiler().getSourcename(),
                methodCompiler.getLastLine() + 1);
    }
    
    public void invokeBinaryFloatRHS(String name, CompilerCallback receiverCallback, final double flote) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_FASTOPS) {
            ArgumentsCallback argumentsCallback = new ArgumentsCallback() {
                @Override
                public int getArity() {
                    return 1;
                }

                @Override
                public void call(BodyCompiler context) {
                    methodCompiler.getScriptCompiler().getCacheCompiler().cacheFloat(methodCompiler, flote);
                }
            };
            invokeDynamic(name, receiverCallback, argumentsCallback, CallType.NORMAL, null, false);
            return;
        }
        
        methodCompiler.loadThreadContext(); // [adapter, tc]

        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        methodCompiler.loadSelf();

        if (receiverCallback != null) {
            receiverCallback.call(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }

        String signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class));

        method.invokedynamic(
                "floatOperator:" + JavaNameMangler.mangleMethodName(name),
                signature,
                InvokeDynamicSupport.getFloatOperatorHandle(),
                flote,
                methodCompiler.getScriptCompiler().getSourcename(),
                methodCompiler.getLastLine() + 1);
    }

    @Override
    public void invokeOpAsgnWithMethod(final String operatorName, final String attrName, final String attrAsgnName, final CompilerCallback receiverCallback, final ArgumentsCallback argsCallback) {
        final int temp = methodCompiler.variableCompiler.grabTempLocal();
        receiverCallback.call(methodCompiler);
        methodCompiler.method.astore(temp);
        final CompilerCallback receiver = new CompilerCallback() {
            public void call(BodyCompiler context) {
                methodCompiler.method.aload(temp);
            }
        };

        ArgumentsCallback result = new ArgumentsCallback() {
            public void call(BodyCompiler context) {
                CompilerCallback value = new CompilerCallback() {
                    public void call(BodyCompiler context) {
                        invokeDynamic(attrName, receiver, null, CallType.FUNCTIONAL, null, false);
                    }
                };

                invokeDynamic(operatorName, value, argsCallback, CallType.FUNCTIONAL, null, false);
            }

            public int getArity() {
                return 1;
            }
        };
        invokeAttrAssign(attrAsgnName, receiver, result, false, true);
        methodCompiler.variableCompiler.releaseTempLocal();
    }

    public void invokeOpAsgnWithOr(String attrName, String attrAsgnName, CompilerCallback receiverCallback, ArgumentsCallback argsCallback) {
        final int temp = methodCompiler.variableCompiler.grabTempLocal();
        receiverCallback.call(methodCompiler);
        methodCompiler.method.astore(temp);
        final CompilerCallback receiver = new CompilerCallback() {
            public void call(BodyCompiler context) {
                methodCompiler.method.aload(temp);
            }
        };

        invokeDynamic(attrName, receiver, null, CallType.FUNCTIONAL, null, false);

        Label done = new Label();
        Label isTrue = new Label();

        method.dup();
        methodCompiler.invokeIRubyObject("isTrue", sig(boolean.class));
        method.iftrue(done);

        method.pop(); // pop extra attr value
        invokeAttrAssign(attrAsgnName, receiver, argsCallback, false, true);

        method.label(done);
        methodCompiler.variableCompiler.releaseTempLocal();
    }

    public void invokeOpAsgnWithAnd(String attrName, String attrAsgnName, CompilerCallback receiverCallback, ArgumentsCallback argsCallback) {
        final int temp = methodCompiler.variableCompiler.grabTempLocal();
        receiverCallback.call(methodCompiler);
        methodCompiler.method.astore(temp);
        final CompilerCallback receiver = new CompilerCallback() {
            public void call(BodyCompiler context) {
                methodCompiler.method.aload(temp);
            }
        };

        invokeDynamic(attrName, receiver, null, CallType.FUNCTIONAL, null, false);

        Label done = new Label();

        method.dup();
        methodCompiler.invokeIRubyObject("isTrue", sig(boolean.class));
        method.iffalse(done);

        method.pop(); // pop extra attr value
        invokeAttrAssign(attrAsgnName, receiver, argsCallback, false, true);

        method.label(done);
    }
}
