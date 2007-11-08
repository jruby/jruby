/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
package org.jruby.internal.runtime.methods;

import java.util.ArrayList;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.ClosureCallback;
import org.jruby.compiler.MethodCompiler;
import org.jruby.compiler.ASTCompiler;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyClassLoader;

/**
 *
 */
public final class DefaultMethod extends DynamicMethod implements JumpTarget {
    
    private StaticScope staticScope;
    private Node body;
    private ArgsNode argsNode;
    private int callCount = 0;
    private Script jitCompiledScript;
    private int requiredArgsCount;
    private int restArg;
    private boolean hasOptArgs;
    private CallConfiguration jitCallConfig;
    private ISourcePosition position;

    public DefaultMethod(RubyModule implementationClass, StaticScope staticScope, Node body, 
            ArgsNode argsNode, Visibility visibility, ISourcePosition position) {
        super(implementationClass, visibility, CallConfiguration.RUBY_FULL);
        this.body = body;
        this.staticScope = staticScope;
        this.argsNode = argsNode;
        this.requiredArgsCount = argsNode.getRequiredArgsCount();
        this.restArg = argsNode.getRestArg();
        this.hasOptArgs = argsNode.getOptArgs() != null;
        this.position = position;
		
        assert argsNode != null;
    }

    /**
     * @see AbstractCallable#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        assert args != null;

        RubyModule implementer = getImplementationClass();
        
        Ruby runtime = context.getRuntime();

        if (runtime.getInstanceConfig().getCompileMode().shouldJIT()) {
            runJIT(runtime, context, name);
        }
        
        if (jitCompiledScript != null && !runtime.hasEventHooks()) {
            try {
                // FIXME: For some reason this wants (and works with) clazz instead of implementer,
                // and needed it for compiled module method_function's called from outside the module. Why?
                jitCallConfig.pre(context, self, implementer, getArity(), name, args, block, staticScope, this);

                return jitCompiledScript.run(context, self, args, block);
            } catch (JumpException.ReturnJump rj) {
                if (rj.getTarget() == this) {
                    return (IRubyObject) rj.getValue();
                }
                throw rj;
            } catch (JumpException.RedoJump rj) {
                    throw runtime.newLocalJumpError("redo", runtime.getNil(), "unexpected redo");
            } finally {
                if (runtime.hasEventHooks()) {
                    traceReturn(context, runtime, name);
                }
                jitCallConfig.post(context);
            }
        } else {
            try {
                callConfig.pre(context, self, implementer, getArity(), name, args, block, staticScope, this);
                if (argsNode.getBlockArgNode() != null) {
                    context.getCurrentScope().setValue(
                            argsNode.getBlockArgNode().getCount(),
                            RuntimeHelpers.processBlockArgument(runtime, block),
                            0);
                }

                getArity().checkArity(runtime, args);

                prepareArguments(context, runtime, args);

                if (runtime.hasEventHooks()) {
                    traceCall(context, runtime, name);
                }

                return ASTInterpreter.eval(runtime, context, body, self, block);
            } catch (JumpException.ReturnJump rj) {
                if (rj.getTarget() == this) {
                    return (IRubyObject) rj.getValue();
                }
                throw rj;
            } catch (JumpException.RedoJump rj) {
                throw runtime.newLocalJumpError("redo", runtime.getNil(), "unexpected redo");
            } finally {
                if (runtime.hasEventHooks()) {
                    traceReturn(context, runtime, name);
                }
                callConfig.post(context);
            }
        }
    }

    private void runJIT(Ruby runtime, ThreadContext context, String name) {
        if (callCount >= 0) {
            try {
                callCount++;

                if (callCount >= runtime.getInstanceConfig().getJitThreshold()) {
                    String cleanName = CodegenUtils.cg.cleanJavaIdentifier(name);
                    String filename = "__eval__";
                    if (body != null) {
                        filename = body.getPosition().getFile();
                    } else if (argsNode != null) {
                        filename = argsNode.getPosition().getFile();
                    }
                    StandardASMCompiler compiler = new StandardASMCompiler(cleanName + hashCode() + "_" + context.hashCode(), filename);
                    compiler.startScript(staticScope);
        
                    ClosureCallback args = new ClosureCallback() {
                        public void compile(MethodCompiler context) {
                            ASTCompiler.compileArgs(argsNode, context);
                        }
                    };
        
                    ASTInspector inspector = new ASTInspector();
                    inspector.inspect(body);
                    inspector.inspect(argsNode);
                    
                    MethodCompiler methodCompiler;
                    if (body != null) {
                        // we have a body, do a full-on method
                        methodCompiler = compiler.startMethod("__file__", args, staticScope, inspector);
                        ASTCompiler.compile(body, methodCompiler);
                    } else {
                        // If we don't have a body, check for required or opt args
                        // if opt args, they could have side effects
                        // if required args, need to raise errors if too few args passed
                        // otherwise, method does nothing, make it a nop
                        if (argsNode != null && (argsNode.getRequiredArgsCount() > 0 || argsNode.getOptionalArgsCount() > 0)) {
                            methodCompiler = compiler.startMethod("__file__", args, staticScope, inspector);
                            methodCompiler.loadNil();
                        } else {
                            methodCompiler = compiler.startMethod("__file__", null, staticScope, inspector);
                            methodCompiler.loadNil();
                            jitCallConfig = CallConfiguration.JAVA_FAST;
                        }
                    }
                    methodCompiler.endMethod();
                    compiler.endScript();
                    Class sourceClass = compiler.loadClass(new JRubyClassLoader(runtime.getJRubyClassLoader()));
                    
                    // if we haven't already decided on a do-nothing call
                    if (jitCallConfig == null) {
                        // if we're not doing any of the operations that still need a scope, use the scopeless config
                        if (!(inspector.hasClosure() || inspector.hasScopeAwareMethods())) {
                            // switch to a slightly faster call config
                            jitCallConfig = CallConfiguration.JAVA_FULL;
                        } else {
                            jitCallConfig = CallConfiguration.RUBY_FULL;
                        }
                    }
                    
                    // finally, grab the script
                    jitCompiledScript = (Script)sourceClass.newInstance();
                    
                    if (runtime.getInstanceConfig().isJitLogging()) {
                        String className = getImplementationClass().getBaseName();
                        if (className == null) {
                            className = "<anon class>";
                        }
                        System.err.println("compiled: " + className + "." + name);
                    }
                    callCount = -1;
                }
            } catch (Exception e) {
                if (runtime.getInstanceConfig().isJitLoggingVerbose()) {
                    String className = getImplementationClass().getBaseName();
                    if (className == null) {
                        className = "<anon class>";
                    }
                    System.err.println("could not compile: " + className + "." + name + " because of: \"" + e.getMessage() + '"');
                }
                callCount = -1;
             }
        }
    }

    private void prepareArguments(ThreadContext context, Ruby runtime, IRubyObject[] args) {
        if (requiredArgsCount > args.length) {
            throw runtime.newArgumentError("Wrong # of arguments(" + args.length + " for " + requiredArgsCount + ")");
        }

        // Bind 'normal' parameter values to the local scope for this method.
        if (requiredArgsCount > 0) {
            context.getCurrentScope().setArgValues(args, requiredArgsCount);
        }

        // optArgs and restArgs require more work, so isolate them and ArrayList creation here
        if (hasOptArgs || restArg != -1) {
            prepareOptOrRestArgs(context, runtime, args);
        }
    }

    private void prepareOptOrRestArgs(ThreadContext context, Ruby runtime, IRubyObject[] args) {
        // we know we've at least got the required count at this point, so start with that
        int givenArgsCount = requiredArgsCount;
        
        // determine the maximum number of arguments
        int maximumArgs = requiredArgsCount;
        if (argsNode.getOptArgs() != null) {
            maximumArgs += argsNode.getOptArgs().size();
            
            if (restArg == -1 && maximumArgs < args.length) {
                throw runtime.newArgumentError("wrong # of arguments(" + args.length + " for " + maximumArgs + ")");
            }
        }
        
        if (hasOptArgs) {
            ListNode optArgs = argsNode.getOptArgs();
   
            // assign given optional arguments to their variables
            int j = 0;
            for (int i = requiredArgsCount; i < args.length && j < optArgs.size(); i++, j++) {
                // in-frame EvalState should already have receiver set as self, continue to use it
                AssignmentVisitor.assign(runtime, context, context.getFrameSelf(), optArgs.get(j), args[i], Block.NULL_BLOCK, true);
                givenArgsCount++;
            }
   
            // assign the default values, adding to the end of allArgs
            for (int i = 0; j < optArgs.size(); i++, j++) {
                ASTInterpreter.eval(runtime, context, optArgs.get(j), context.getFrameSelf(), Block.NULL_BLOCK);
            }
        }
        
        // build an array from *rest type args, also adding to allArgs
        
        // ENEBO: Does this next comment still need to be done since I killed hasLocalVars:
        // move this out of the scope.hasLocalVariables() condition to deal
        // with anonymous restargs (* versus *rest)
        
        
        // none present ==> -1
        // named restarg ==> >=0
        // anonymous restarg ==> -2
        if (restArg != -1) {
            // only set in scope if named
            if (restArg >= 0) {
                RubyArray array = runtime.newArray(args.length - givenArgsCount);
                for (int i = givenArgsCount; i < args.length; i++) {
                    array.append(args[i]);
                }

                context.getCurrentScope().setValue(restArg, array, 0);
            }
        }
    }

    private void traceReturn(ThreadContext context, Ruby runtime, String name) {
        ISourcePosition position = context.getPreviousFramePosition();
        runtime.callEventHooks(context, EventHook.RUBY_EVENT_RETURN, position.getFile(), position.getStartLine(), name, getImplementationClass());
    }
    
    private void traceCall(ThreadContext context, Ruby runtime, String name) {
        runtime.callEventHooks(context, EventHook.RUBY_EVENT_CALL, position.getFile(), position.getStartLine(), name, getImplementationClass());
    }

    public Arity getArity() {
        return argsNode.getArity();
    }
    
    public DynamicMethod dup() {
        return new DefaultMethod(getImplementationClass(), staticScope, body, argsNode, getVisibility(), position);
    }
}
