package org.jruby.internal.runtime.methods;

import java.util.Iterator;

import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyProc;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.types.IListNode;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.ReturnJump;
import org.jruby.runtime.Namespace;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class DefaultMethod extends AbstractMethod {
    private ScopeNode body;
    private ArgsNode argsNode;
    private Namespace namespace;

    public DefaultMethod(ScopeNode body, ArgsNode argsNode, Namespace namespace, Visibility visibility) {
        super(visibility);
        this.body = body;
        this.argsNode = argsNode;
        this.namespace = namespace;
    }

    /**
     * @see AbstractMethod#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        ThreadContext context = ruby.getCurrentContext();

        RubyProc optionalBlockArg = null;
        if (argsNode.getBlockArgNode() != null && context.isBlockGiven()) {
            optionalBlockArg = RubyProc.newProc(ruby);
        }

        context.getScopeStack().push();

        Namespace savedNamespace = null;

        savedNamespace = ruby.getNamespace();
        ruby.setNamespace(namespace);
        context.getCurrentFrame().setNamespace(namespace);

        if (body.getLocalNames() != null) {
            context.getScopeStack().resetLocalVariables(body.getLocalNames());
        }

        context.pushDynamicVars();

        try {
            if (argsNode != null) {
                prepareArguments(ruby, receiver, args);
            }

            if (optionalBlockArg != null) {
                context.getScopeStack().setValue(argsNode.getBlockArgNode().getCount(), optionalBlockArg);
            }

            traceCall(ruby, receiver, name);

            return receiver.eval(body.getBodyNode());

        } catch (ReturnJump re) {
            return re.getReturnValue();
        } finally {
            context.popDynamicVars();
            context.getScopeStack().pop();
            ruby.setNamespace(savedNamespace);
            traceReturn(ruby, receiver, name);
        }
    }

    private void prepareArguments(Ruby ruby, IRubyObject receiver, IRubyObject[] args) {
        if (args == null) {
            args = IRubyObject.NULL_ARRAY;
        }

        int expectedArgsCount = argsNode.getArgsCount();
        if (expectedArgsCount > args.length) {
            throw new ArgumentError(ruby, "Wrong # of arguments(" + args.length + " for " + expectedArgsCount + ")");
        }
        if (argsNode.getRestArg() == -1 && argsNode.getOptArgs() != null) {
            int opt = expectedArgsCount + argsNode.getOptArgs().size();

            if (opt < args.length) {
                throw new ArgumentError(ruby, "wrong # of arguments(" + args.length + " for " + opt + ")");
            }

            ruby.getCurrentFrame().setArgs(args);
        }

        if (ruby.getScope().hasLocalVariables()) {
            if (expectedArgsCount > 0) {
                for (int i = 0; i < expectedArgsCount; i++) {
                    ruby.getScope().setValue(i + 2, args[i]);
                }
            }

            if (argsNode.getOptArgs() != null) {
                IListNode optArgs = argsNode.getOptArgs();

                Iterator iter = optArgs.iterator();
                for (int i = expectedArgsCount; i < args.length && iter.hasNext(); i++) {
                    new AssignmentVisitor(ruby, receiver).assign((INode)iter.next(), args[i], true);
                    expectedArgsCount++;
                }

                // assign the default values.
                while (iter.hasNext()) {
                    EvaluateVisitor.createVisitor(receiver).eval((INode)iter.next());
                }
            }

            if (argsNode.getRestArg() >= 0) {
                RubyArray array = RubyArray.newArray(ruby, args.length - expectedArgsCount);
                for (int i = expectedArgsCount; i < args.length; i++) {
                    array.append(args[i]);
                }
                ruby.getScope().setValue(argsNode.getRestArg(), array);
            }
        }
    }

    private void traceReturn(Ruby ruby, IRubyObject receiver, String name) {
        if (ruby.getTraceFunction() == null) {
            return;
        }

        ISourcePosition position = ruby.getFrameStack().getPrevious().getPosition();
        if (position == null) {
            position = ruby.getPosition();
        }
        ruby.callTraceFunction("return", position, receiver, name, getImplementationClass()); // XXX
    }

    private void traceCall(Ruby ruby, IRubyObject receiver, String name) {
        if (ruby.getTraceFunction() == null) {
            return;
        }
        //a lot of complication to try to get a line number and a file name
        //without a NullPointerException
        ISourcePosition lPosition = null;
        if (body != null)
            if (body.getBodyNode() != null)
                if(body.getBodyNode().getPosition() != null)
                    lPosition = body.getBodyNode().getPosition();
                else
                    ;
            else
                if (body.getPosition() != null)
                    lPosition = body.getPosition();
                else
                    ;
        else
            if (argsNode != null)
                lPosition = argsNode.getPosition();


        if (lPosition == null) {
           lPosition = ruby.getPosition();
        }
        ruby.callTraceFunction("call", lPosition, receiver, name, getImplementationClass()); // XXX
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
}
