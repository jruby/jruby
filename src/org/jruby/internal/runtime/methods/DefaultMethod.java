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

import java.util.Iterator;
import java.util.Stack;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyProc;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.Node;
import org.jruby.ast.ScopeNode;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.exceptions.ReturnJump;
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class DefaultMethod extends AbstractMethod {
    private ScopeNode body;
    private ArgsNode argsNode;
    private Stack moduleStack;

    public DefaultMethod(ScopeNode body, ArgsNode argsNode, Visibility visibility, Stack moduleStack) {
        super(visibility);
        this.body = body;
        this.argsNode = argsNode;
        this.moduleStack = moduleStack;
    }

    /**
     * @see AbstractMethod#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(Ruby runtime, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        ThreadContext context = runtime.getCurrentContext();

        RubyProc optionalBlockArg = null;
        if (argsNode.getBlockArgNode() != null && context.isBlockGiven()) {
            optionalBlockArg = runtime.newProc();
        }

        context.getScopeStack().push();

        if (body.getLocalNames() != null) {
            context.getScopeStack().resetLocalVariables(body.getLocalNames());
        }

        context.pushDynamicVars();

        Stack oldStack = context.getClassStack();
        // replace class stack with appropriate execution scope
        context.setClassStack(moduleStack);

        try {
            if (argsNode != null) {
                prepareArguments(runtime, receiver, args);
            }

            if (optionalBlockArg != null) {
                context.getScopeStack().setValue(argsNode.getBlockArgNode().getCount(), optionalBlockArg);
            }
            
            getArity().checkArity(runtime, args);

            traceCall(runtime, receiver, name);

            return receiver.eval(body.getBodyNode());

        } catch (ReturnJump rj) {
            if (rj.getTarget() == this) {
                return rj.getReturnValue();
            }
            throw rj;
        } finally {
        	// restore class stack
            context.setClassStack(oldStack);
            context.popDynamicVars();
            context.getScopeStack().pop();
            traceReturn(runtime, receiver, name);
        }
    }

    private void prepareArguments(Ruby runtime, IRubyObject receiver, IRubyObject[] args) {
        if (args == null) {
            args = IRubyObject.NULL_ARRAY;
        }

        int expectedArgsCount = argsNode.getArgsCount();
        if (expectedArgsCount > args.length) {
            throw runtime.newArgumentError("Wrong # of arguments(" + args.length + " for " + expectedArgsCount + ")");
        }
        if (argsNode.getRestArg() == -1 && argsNode.getOptArgs() != null) {
            int opt = expectedArgsCount + argsNode.getOptArgs().size();

            if (opt < args.length) {
                throw runtime.newArgumentError("wrong # of arguments(" + args.length + " for " + opt + ")");
            }

            runtime.getCurrentFrame().setArgs(args);
        }

        if (runtime.getScope().hasLocalVariables()) {
            if (expectedArgsCount > 0) {
                for (int i = 0; i < expectedArgsCount; i++) {
                    runtime.getScope().setValue(i + 2, args[i]);
                }
            }

            if (argsNode.getOptArgs() != null) {
                ListNode optArgs = argsNode.getOptArgs();

                Iterator iter = optArgs.iterator();
                for (int i = expectedArgsCount; i < args.length && iter.hasNext(); i++) {
                    new AssignmentVisitor(runtime, receiver).assign((Node)iter.next(), args[i], true);
                    expectedArgsCount++;
                }

                // assign the default values.
                while (iter.hasNext()) {
                    EvaluateVisitor.createVisitor(receiver).eval((Node)iter.next());
                }
            }

            if (argsNode.getRestArg() >= 0) {
                RubyArray array = runtime.newArray(args.length - expectedArgsCount);
                for (int i = expectedArgsCount; i < args.length; i++) {
                    array.append(args[i]);
                }
                runtime.getScope().setValue(argsNode.getRestArg(), array);
            }
        }
    }

    private void traceReturn(Ruby runtime, IRubyObject receiver, String name) {
        if (runtime.getTraceFunction() == null) {
            return;
        }

        SourcePosition position = runtime.getFrameStack().getPrevious().getPosition();
        if (position == null) {
            position = runtime.getPosition();
        }
        runtime.callTraceFunction("return", position, receiver, name, getImplementationClass()); // XXX
    }

    private void traceCall(Ruby runtime, IRubyObject receiver, String name) {
        if (runtime.getTraceFunction() == null) {
            return;
        }
        //a lot of complication to try to get a line number and a file name
        //without a NullPointerException
        SourcePosition lPosition = null;
        if (body != null) {
            if (body.getBodyNode() != null) {
                if(body.getBodyNode().getPosition() != null) {
                    lPosition = body.getBodyNode().getPosition();
                }
            } else {
                if (body.getPosition() != null) {
                    lPosition = body.getPosition();
                }
            }
        } else {
            if (argsNode != null) {
                lPosition = argsNode.getPosition();
            }
        }
        if (lPosition == null) {
           lPosition = runtime.getPosition();
        }
        runtime.callTraceFunction("call", lPosition, receiver, name, getImplementationClass()); // XXX
    }

    /**
     * Gets the argsNode.
     * @return Returns a ArgsNode
     */
    public ArgsNode getArgsNode() {
        return argsNode;
    }

    public Arity getArity() {
        ArgsNode args = getArgsNode();
        
        if (args == null) {
            return Arity.noArguments();
        }
        
        // TODO: Make special arity-related values use mnemonic
        // -2 means (*) signature to method def
        if (args.getRestArg() == -2) {
        	return Arity.optional();
        } 

        int argsCount = args.getArgsCount();
        if (args.getOptArgs() != null || args.getRestArg() >= 0) {
            return Arity.required(argsCount);
        }
        return Arity.createArity(argsCount);
    }
    
    public ICallable dup() {
        return new DefaultMethod(body, argsNode, getVisibility(), moduleStack);
    }	
}
