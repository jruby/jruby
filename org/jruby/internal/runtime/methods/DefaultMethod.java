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

    public DefaultMethod(ScopeNode body, ArgsNode argsNode, Namespace namespace) {
        this.body = body;
        this.argsNode = argsNode;
        this.namespace = namespace;
    }

    /**
     * @see IMethod#execute(Ruby, RubyObject, String, RubyObject[], boolean)
     */
    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        RubyProc optionalBlockArg = null;
        if (argsNode.getBlockArgNode() != null && ruby.isBlockGiven()) {
            optionalBlockArg = RubyProc.newProc(ruby, ruby.getClasses().getProcClass());
        }

        ruby.getScope().push();

        Namespace savedNamespace = null;

        savedNamespace = ruby.getNamespace();
        ruby.setNamespace(namespace);
        ruby.getCurrentFrame().setNamespace(namespace);

        if (body.getLocalNames() != null) {
            ruby.getScope().setLocalNames(body.getLocalNames());
        }

        ruby.pushDynamicVars();

        try {
            if (argsNode != null) {
                prepareArguments(ruby, receiver, args);
            }

            if (optionalBlockArg != null) {
                ruby.getScope().setValue(argsNode.getBlockArgNode().getCount(), optionalBlockArg);
            }

            traceCall(ruby, receiver, name);

            return receiver.eval(body.getBodyNode());

        } catch (ReturnJump re) {
            return re.getReturnValue();
        } finally {
            ruby.popDynamicVars();
            ruby.getScope().pop();
            ruby.setNamespace(savedNamespace);
            traceReturn(ruby, receiver, name);
        }
    }

    private void prepareArguments(Ruby ruby, IRubyObject receiver, IRubyObject[] args) {
        if (args == null) {
            args = new IRubyObject[0];
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

        if (ruby.getScope().hasLocalValues()) {
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
        if (ruby.getRuntime().getTraceFunction() == null) {
            return;
        }

        String file = ruby.getFrameStack().getPrevious().getFile();
        int line = ruby.getFrameStack().getPrevious().getLine();

        if (file == null) {
            file = ruby.getSourceFile();
            line = ruby.getSourceLine();
        }

        ruby.getRuntime().callTraceFunction("return", file, line, receiver, name, getImplementationClass()); // XXX
    }

    private void traceCall(Ruby ruby, IRubyObject receiver, String name) {
        if (ruby.getRuntime().getTraceFunction() == null) {
            return;
        }
        //a lot of complication to try to get a line number and a file name
        //without a NullPointerException
        ISourcePosition lPos = null;
        if (body != null)
            if (body.getBodyNode() != null)
                if(body.getBodyNode().getPosition() != null)
                    lPos = body.getBodyNode().getPosition();
                else
                    ;
            else
                if (body.getPosition() != null)
                    lPos = body.getPosition();
                else
                    ;
        else
            if (argsNode != null)
                lPos = argsNode.getPosition();

        String lFile = ruby.getSourceFile();
        int lLine = ruby.getSourceLine();
        if (lPos != null)
        {
            lFile = lPos.getFile();
            lLine = lPos.getLine();
        }
        ruby.getRuntime().callTraceFunction("call", lFile, lLine, receiver, name, getImplementationClass()); // XXX
    }

    /**
     * Gets the argsNode.
     * @return Returns a ArgsNode
     */
    public ArgsNode getArgsNode() {
        return argsNode;
    }
}
