package org.jruby.internal.runtime.methods;

import java.util.*;

import org.ablaf.ast.*;
import org.jruby.*;
import org.jruby.ast.*;
import org.jruby.ast.types.*;
import org.jruby.evaluator.*;
import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.ablaf.common.ISourcePosition;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DefaultMethod extends AbstractMethod {
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
    public RubyObject execute(Ruby ruby, RubyObject receiver, String name, RubyObject[] args, boolean noSuper) {
        if (args == null) {
            args = new RubyObject[0];
        }

        RubyProc optionalBlockArg = null;

        if (argsNode.getBlockArgNode() != null && ruby.isBlockGiven()) {
            optionalBlockArg = RubyProc.newProc(ruby, ruby.getClasses().getProcClass());
        }

        ruby.getScope().push();

        Namespace savedNamespace = null;

        if (namespace != null) {
            savedNamespace = ruby.getNamespace();
            ruby.setNamespace(namespace);
            ruby.getActFrame().setNamespace(namespace);
        }

        if (body.getLocalNames() != null) {
            ruby.getScope().setLocalValues(new ArrayList(Collections.nCopies(body.getLocalNames().size(), ruby.getNil())));
            ruby.getScope().setLocalNames(body.getLocalNames());
        }

        RubyVarmap.push(ruby);

        try {
            if (argsNode != null) {
                int i = argsNode.getArgsCount();
                if (i > args.length) {
                    throw new ArgumentError(ruby, "Wrong # of arguments(" + args.length + " for " + i + ")");
                }
                if (argsNode.getRestArg() == -1 && argsNode.getOptArgs() != null) {
                    int opt = i;

                    IListNode optNode = argsNode.getOptArgs();

                    Iterator iter = optNode.iterator();
                    while (iter.hasNext()) {
                        iter.next();
                        opt++;
                    }

                    if (opt < args.length) {
                        throw new ArgumentError(ruby, "wrong # of arguments(" + args.length + " for " + opt + ")");
                    }

                    ruby.getActFrame().setArgs(args);
                }

                if (ruby.getScope().getLocalValues() != null) {
                    if (i > 0) {
                        for (int j = 0; j < i; j++) {
                            ruby.getScope().setValue(j + 2, args[j]);
                        }
                    }

                    if (argsNode.getOptArgs() != null) {
                        IListNode optArgs = argsNode.getOptArgs();

                        Iterator iter = optArgs.iterator();
                        for (int j = i; j < args.length && iter.hasNext(); j++) {
                            new AssignmentVisitor(ruby, receiver).assign((INode)iter.next(), args[j], true);
                            i++;
                        }

                        // assign the default values.
                        while (iter.hasNext()) {
                            EvaluateVisitor.createVisitor(receiver).eval((INode)iter.next());
                        }
                    }

                    if (argsNode.getRestArg() >= 0) {
                        RubyArray array = null;
                        if (args.length > i) {
                            array = RubyArray.newArray(ruby, Arrays.asList(args).subList(i, args.length));
                        } else {
                            array = RubyArray.newArray(ruby, 0);
                        }
                        ruby.getScope().setValue(argsNode.getRestArg(), array);
                    }
                }
            }

            if (optionalBlockArg != null) {
                ruby.getScope().setValue(argsNode.getBlockArgNode().getCount(), optionalBlockArg);
            }

            if (ruby.getRuntime().getTraceFunction() != null) {
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

            return receiver.eval(body.getBodyNode()); // skip scope assignment
        } catch (ReturnException rExcptn) {
            return rExcptn.getReturnValue();
        } finally {
            RubyVarmap.pop(ruby);

            ruby.getScope().pop();

            if (savedNamespace != null) {
                ruby.setNamespace(savedNamespace);
            }

            if (ruby.getRuntime().getTraceFunction() != null) {
                String file = ruby.getFrameStack().getPrevious().getFile();
                int line = ruby.getFrameStack().getPrevious().getLine();

                if (file == null) {
                    file = ruby.getSourceFile();
                    line = ruby.getSourceLine();
                }

                ruby.getRuntime().callTraceFunction("return", file, line, receiver, name, getImplementationClass()); // XXX
            }
        }
    }

    /**
     * Gets the argsNode.
     * @return Returns a ArgsNode
     */
    public ArgsNode getArgsNode() {
        return argsNode;
    }
}
