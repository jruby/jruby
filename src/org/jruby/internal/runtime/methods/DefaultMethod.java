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
import java.util.Iterator;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.Node;
import org.jruby.ast.ScopeNode;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.evaluator.EvaluationState;
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Scope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

/**
 *
 */
public final class DefaultMethod extends AbstractMethod {
    private ScopeNode body;
    private ArgsNode argsNode;
    private SinglyLinkedList cref;

    public DefaultMethod(RubyModule implementationClass, ScopeNode body, ArgsNode argsNode, 
        Visibility visibility, SinglyLinkedList cref) {
        super(implementationClass, visibility);
        this.body = body;
        this.argsNode = argsNode;
		this.cref = cref;
		
		assert body != null;
		assert argsNode != null;
    }
    
    public void preMethod(IRuby runtime, RubyModule lastClass, IRubyObject recv, String name, IRubyObject[] args, boolean noSuper) {
        ThreadContext context = runtime.getCurrentContext();
        
        context.preDefMethodInternalCall(lastClass, recv, name, args, noSuper, cref);
    }
    
    public void postMethod(IRuby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        
        context.postDefMethodInternalCall();
    }

    /**
     * @see AbstractCallable#call(IRuby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject internalCall(IRuby runtime, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper) {
    	assert args != null;

        ThreadContext context = runtime.getCurrentContext();
        
        Scope scope = context.getFrameScope();
        if (body.getLocalNames() != null) {
            scope.resetLocalVariables(body.getLocalNames());
        }
        
        if (argsNode.getBlockArgNode() != null && context.isBlockGiven()) {
            scope.setValue(argsNode.getBlockArgNode().getCount(), runtime.newProc());
        }

        try {
            prepareArguments(context, runtime, scope, receiver, args);
            
            getArity().checkArity(runtime, args);

            traceCall(runtime, receiver, name);

            return receiver.eval(body.getBodyNode());
        } catch (JumpException je) {
        	if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
	            if (je.getPrimaryData() == this) {
	                return (IRubyObject)je.getSecondaryData();
	            }
        	}
       		throw je;
        } finally {
            traceReturn(runtime, receiver, name);
        }
    }

    private void prepareArguments(ThreadContext context, IRuby runtime, Scope scope, IRubyObject receiver, IRubyObject[] args) {
        int expectedArgsCount = argsNode.getArgsCount();

        int restArg = argsNode.getRestArg();
        boolean hasOptArgs = argsNode.getOptArgs() != null;

        if (expectedArgsCount > args.length) {
            throw runtime.newArgumentError("Wrong # of arguments(" + args.length + " for " + expectedArgsCount + ")");
        }

        if (scope.hasLocalVariables() && expectedArgsCount > 0) {
            for (int i = 0; i < expectedArgsCount; i++) {
                scope.setValue(i + 2, args[i]);
            }
        }

        // optArgs and restArgs require more work, so isolate them and ArrayList creation here
        if (hasOptArgs || restArg != -1) {
            args = prepareOptOrRestArgs(context, runtime, scope, args, expectedArgsCount, restArg, hasOptArgs);
        }
        
        context.setFrameArgs(args);
    }

    private IRubyObject[] prepareOptOrRestArgs(ThreadContext context, IRuby runtime, Scope scope, IRubyObject[] args, int expectedArgsCount, int restArg, boolean hasOptArgs) {
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
        
        if (scope.hasLocalVariables()) {
            if (hasOptArgs) {
                ListNode optArgs = argsNode.getOptArgs();
   
                Iterator iter = optArgs.iterator();
                for (int i = expectedArgsCount; i < args.length && iter.hasNext(); i++) {
                    //new AssignmentVisitor(new EvaluationState(runtime, receiver)).assign((Node)iter.next(), args[i], true);
   //                  in-frame EvalState should already have receiver set as self, continue to use it
                    new AssignmentVisitor(context.getFrameSelf()).assign((Node)iter.next(), args[i], true);
                    expectedArgsCount++;
                }
   
                // assign the default values, adding to the end of allArgs
                while (iter.hasNext()) {
                    //new EvaluationState(runtime, receiver).begin((Node)iter.next());
                    //EvaluateVisitor.getInstance().eval(receiver.getRuntime(), receiver, (Node)iter.next());
                    // in-frame EvalState should already have receiver set as self, continue to use it
                    allArgs.add(EvaluationState.eval(runtime.getCurrentContext(), ((Node)iter.next()), runtime.getCurrentContext().getFrameSelf()));
                }
            }
        }
        
        // build an array from *rest type args, also adding to allArgs
        
        // move this out of the scope.hasLocalVariables() condition to deal
        // with anonymous restargs (* versus *rest)
        // none present ==> -1
        // named restarg ==> >=0
        // anonymous restarg ==> -2
        if (restArg != -1) {
            RubyArray array = runtime.newArray(args.length - expectedArgsCount);
            for (int i = expectedArgsCount; i < args.length; i++) {
                array.append(args[i]);
                allArgs.add(args[i]);
            }
            // only set in scope if named
            if (restArg >= 0) {
                scope.setValue(restArg, array);
            }
        }
        
        args = (IRubyObject[])allArgs.toArray(new IRubyObject[allArgs.size()]);
        return args;
    }

    private void traceReturn(IRuby runtime, IRubyObject receiver, String name) {
        if (runtime.getTraceFunction() == null) {
            return;
        }

        ISourcePosition position = runtime.getCurrentContext().getPreviousFramePosition();
        runtime.callTraceFunction("return", position, receiver, name, getImplementationClass()); // XXX
    }

    private void traceCall(IRuby runtime, IRubyObject receiver, String name) {
        if (runtime.getTraceFunction() == null) {
            return;
        }

		ISourcePosition position = body.getBodyNode() != null ? 
            body.getBodyNode().getPosition() : body.getPosition();  

		runtime.callTraceFunction("call", position, receiver, name, getImplementationClass()); // XXX
    }

    public Arity getArity() {
        return argsNode.getArity();
    }
    
    public ICallable dup() {
        return new DefaultMethod(getImplementationClass(), body, argsNode, getVisibility(), cref);
    }	
}
