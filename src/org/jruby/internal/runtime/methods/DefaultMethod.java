/*
 * DefaultMethod.java - description
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Thomas E Enebo <enebo@acm.org>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */package org.jruby.internal.runtime.methods;

import java.util.Iterator;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
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
import org.jruby.util.collections.ArrayStack;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class DefaultMethod extends AbstractMethod {
    private ScopeNode body;
    private ArgsNode argsNode;
    private ArrayStack moduleStack;

    public DefaultMethod(ScopeNode body, ArgsNode argsNode, Visibility visibility, ArrayStack moduleStack) {
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
            optionalBlockArg = RubyProc.newProc(runtime);
        }

        context.getScopeStack().push();

        if (body.getLocalNames() != null) {
            context.getScopeStack().resetLocalVariables(body.getLocalNames());
        }

        context.pushDynamicVars();

        ArrayStack oldStack = context.getClassStack();
        // replace class stack with appropriate execution scope
        context.setClassStack(moduleStack);

        try {
            if (argsNode != null) {
                prepareArguments(runtime, receiver, args);
            }

            if (optionalBlockArg != null) {
                context.getScopeStack().setValue(argsNode.getBlockArgNode().getCount(), optionalBlockArg);
            }

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
        if (getArgsNode() == null) {
            return Arity.noArguments();
        }
        ArgsNode args = getArgsNode();
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
