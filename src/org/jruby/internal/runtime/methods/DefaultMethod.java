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
import org.jruby.RubyBinding;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.compiler.NodeCompilerFactory;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.evaluator.EvaluationState;
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.jruby.util.collections.SinglyLinkedList;

/**
 *
 */
public final class DefaultMethod extends DynamicMethod {
    
    private StaticScope staticScope;
    private Node body;
    private ArgsNode argsNode;
    private SinglyLinkedList cref;
    private int callCount = 0;
    private Script jitCompiledScript;

    private static final boolean JIT_ENABLED;
    private static final boolean JIT_LOGGING;
    private static final boolean JIT_LOGGING_VERBOSE;
    private static final int JIT_THRESHOLD;
      
    static {
        if (Ruby.isSecurityRestricted()) {
            JIT_ENABLED = false;
            JIT_LOGGING = false;
            JIT_LOGGING_VERBOSE = false;
            JIT_THRESHOLD = -1;
        } else {
            String enabledValue = System.getProperty("jruby.jit.enabled");
            String threshold = System.getProperty("jruby.jit.threshold");
           
            JIT_ENABLED = enabledValue == null ? true : Boolean.getBoolean(enabledValue);
            JIT_LOGGING = Boolean.getBoolean("jruby.jit.logging");
            JIT_LOGGING_VERBOSE = Boolean.getBoolean("jruby.jit.logging.verbose");
            JIT_THRESHOLD = threshold == null ? 0 : Integer.parseInt(threshold); 
        }
    }
    public DefaultMethod(RubyModule implementationClass, StaticScope staticScope, Node body, 
            ArgsNode argsNode, Visibility visibility, SinglyLinkedList cref) {
        super(implementationClass, visibility);
        this.body = body;
        this.staticScope = staticScope;
        this.argsNode = argsNode;
		this.cref = cref;
		
		assert argsNode != null;
    }
    
    public void preMethod(ThreadContext context, RubyModule clazz, IRubyObject self, String name, 
            IRubyObject[] args, boolean noSuper, Block block) {
        context.preDefMethodInternalCall(clazz, name, self, args, getArity().required(), block, noSuper, cref, staticScope, this);
    }
    
    public void postMethod(ThreadContext context) {
        context.postDefMethodInternalCall();
    }

    /**
     * @see AbstractCallable#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(ThreadContext context, IRubyObject self, 
            RubyModule clazz, String name, IRubyObject[] args, boolean noSuper, Block block) {
        if (jitCompiledScript != null) {
            try {
                context.preDefMethodInternalCall(clazz, name, self, args, getArity().required(), block, noSuper, cref, staticScope, this);
                //context.preCompiledMethod(implementationClass, cref);
                // FIXME: pass block when available
                return jitCompiledScript.run(context, self, args, block);
            } finally {
                context.postDefMethodInternalCall();
            }
        } 
          
        return super.call(context, self, clazz, name, args, noSuper, block);
    }

    /**
     * @see AbstractCallable#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject internalCall(ThreadContext context, RubyModule clazz, 
            IRubyObject self, String name, IRubyObject[] args, boolean noSuper, Block block) {
        assert args != null;
        
        Ruby runtime = context.getRuntime();
        
        if (JIT_ENABLED) {
            runJIT(runtime, context, name);
        
            if (jitCompiledScript != null) {
                try {
                    return call(context, self, clazz, name, args, noSuper, block);
                } catch (JumpException je) {
                    if (je.getJumpType() == JumpException.JumpType.ReturnJump && je.getTarget() == this) {
                            return (IRubyObject) je.getValue();
                    }
                    
                    throw je;
                }
            }
        }

        if (argsNode.getBlockArgNode() != null && block.isGiven()) {
            RubyProc blockArg;
            
            if (block.getProcObject() != null) {
                blockArg = (RubyProc) block.getProcObject();
            } else {
                blockArg = runtime.newProc(false, block);
                blockArg.getBlock().isLambda = block.isLambda;
            }
            // We pass depth zero since we know this only applies to newly created local scope
            context.getCurrentScope().setValue(argsNode.getBlockArgNode().getCount(), blockArg, 0);
        }

        RubyBinding binding = null;
        try {
            prepareArguments(context, runtime, args);
            
            getArity().checkArity(runtime, args);

            if (runtime.getTraceFunction() != null) {
                binding = RubyBinding.newBinding(runtime);
                traceCall(context, runtime, binding, name);
            }
                    
            return EvaluationState.eval(runtime, context, body, self, block);
        } catch (JumpException je) {
        	if (je.getJumpType() == JumpException.JumpType.ReturnJump && je.getTarget() == this) {
	                return (IRubyObject) je.getValue();
        	}
            
       		throw je;
        } finally {
            if (binding != null) {
                traceReturn(context, runtime, binding, name);
            }
        }
    }

    private void runJIT(Ruby runtime, ThreadContext context, String name) {
        if (callCount >= 0) {
            // we can only compile normal args, not block, opt, or rest
            if (!(getArity().isFixed() && // perhaps redundant to check the others
                    argsNode.getBlockArgNode() == null &&
                    argsNode.getOptArgs() == null &&
                    argsNode.getRestArg() == -1) ||
                    // for some reason anonymous classes don't mix well with the compiler yet (JRUBY-831 and JRUBY-857)
                    implementationClass.getName() == null) {
                callCount = -1;
                return;
            }
            
            callCount++;
            
            if (callCount >= JIT_THRESHOLD) {
                
                String className = null;
                if (JIT_LOGGING) {
                    className = getImplementationClass().getBaseName();
                    if (className == null) {
                        className = "<anon class>";
                    }
                }
                
                try {
                    String cleanName = CodegenUtils.cleanJavaIdentifier(name);
                    StandardASMCompiler compiler = new StandardASMCompiler(cleanName + hashCode() + "_" + context.hashCode(), body.getPosition().getFile());
                    compiler.startScript();
                    Object methodToken = compiler.beginMethod("__file__", getArity().getValue());
                    NodeCompilerFactory.getCompiler(body).compile(body, compiler);
                    compiler.endMethod(methodToken);
                    compiler.endScript();
                    Class sourceClass = compiler.loadClass(runtime);
                    jitCompiledScript = (Script)sourceClass.newInstance();
                    
                    if (JIT_LOGGING) System.err.println("compiled: " + className + "." + name);
                } catch (Exception e) {
                    if (JIT_LOGGING_VERBOSE) System.err.println("could not compile: " + className + "." + name + " because of: \"" + e.getMessage() + '"');
                } finally {
                    callCount = -1;
                }
            }
        }
    }

    private void prepareArguments(ThreadContext context, Ruby runtime, IRubyObject[] args) {
        int expectedArgsCount = argsNode.getArgsCount();

        int restArg = argsNode.getRestArg();
        boolean hasOptArgs = argsNode.getOptArgs() != null;

        // FIXME: This seems redundant with the arity check in internalCall...is it actually different?
        if (expectedArgsCount > args.length) {
            throw runtime.newArgumentError("Wrong # of arguments(" + args.length + " for " + expectedArgsCount + ")");
        }

        // Bind 'normal' parameter values to the local scope for this method.
        if (expectedArgsCount > 0) {
            context.getCurrentScope().setArgValues(args, expectedArgsCount);
        }

        // optArgs and restArgs require more work, so isolate them and ArrayList creation here
        if (hasOptArgs || restArg != -1) {
            args = prepareOptOrRestArgs(context, runtime, args, expectedArgsCount, restArg, hasOptArgs);
        }
        
        context.setFrameArgs(args);
    }

    private IRubyObject[] prepareOptOrRestArgs(ThreadContext context, Ruby runtime, IRubyObject[] args, int expectedArgsCount, int restArg, boolean hasOptArgs) {
        if (restArg == -1 && hasOptArgs) {
            int opt = expectedArgsCount + argsNode.getOptArgs().size();

            if (opt < args.length) {
                throw runtime.newArgumentError("wrong # of arguments(" + args.length + " for " + opt + ")");
            }
        }
        
        int count = expectedArgsCount;
        if (argsNode.getOptArgs() != null) {
            count += argsNode.getOptArgs().size();
        }

        ArrayList allArgs = new ArrayList();
        
        // Combine static and optional args into a single list allArgs
        for (int i = 0; i < count && i < args.length; i++) {
            allArgs.add(args[i]);
        }
        
        if (hasOptArgs) {
            ListNode optArgs = argsNode.getOptArgs();
   
            int j = 0;
            for (int i = expectedArgsCount; i < args.length && j < optArgs.size(); i++, j++) {
                // in-frame EvalState should already have receiver set as self, continue to use it
                AssignmentVisitor.assign(runtime, context, context.getFrameSelf(), optArgs.get(j), args[i], Block.NULL_BLOCK, true);
                expectedArgsCount++;
            }
   
            // assign the default values, adding to the end of allArgs
            while (j < optArgs.size()) {
                // in-frame EvalState should already have receiver set as self, continue to use it
                allArgs.add(EvaluationState.eval(runtime, context, optArgs.get(j++), context.getFrameSelf(), Block.NULL_BLOCK));
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
            for (int i = expectedArgsCount; i < args.length; i++) {
                allArgs.add(args[i]);
            }

            // only set in scope if named
            if (restArg >= 0) {
                RubyArray array = runtime.newArray(args.length - expectedArgsCount);
                for (int i = expectedArgsCount; i < args.length; i++) {
                    array.append(args[i]);
                }

                context.getCurrentScope().setValue(restArg, array, 0);
            }
        }
        
        args = (IRubyObject[])allArgs.toArray(new IRubyObject[allArgs.size()]);
        return args;
    }

    private void traceReturn(ThreadContext context, Ruby runtime, RubyBinding binding, String name) {
        if (runtime.getTraceFunction() == null) {
            return;
        }

        ISourcePosition position = context.getPreviousFramePosition();
        runtime.callTraceFunction(context, "return", position, binding, name, getImplementationClass());
    }
    
    private void traceCall(ThreadContext context, Ruby runtime, RubyBinding binding, String name) {
        if (runtime.getTraceFunction() == null) return;
        
        ISourcePosition position = body != null ? body.getPosition() : context.getPosition();
        
        runtime.callTraceFunction(context, "call", position, binding, name, getImplementationClass());
    }

    public Arity getArity() {
        return argsNode.getArity();
    }
    
    public DynamicMethod dup() {
        return new DefaultMethod(getImplementationClass(), staticScope, body, argsNode, getVisibility(), cref);
    }
}
