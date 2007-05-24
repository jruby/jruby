/*
 * DefnNodeCompiler.java
 *
 * Created on January 4, 2007, 2:07 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.ArgsNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.Node;
import org.jruby.runtime.Arity;

/**
 *
 * @author headius
 */
public class DefnNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of DefnNodeCompiler */
    public DefnNodeCompiler() {
        
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        final DefnNode defnNode = (DefnNode)node;
        final ArgsNode argsNode = defnNode.getArgsNode();
        
        // FIXME: simple arity check assumes simple arg list; expand to support weirder arg lists
        if (NodeCompilerFactory.SAFE) {
            // FIXME: We can't compile cases like def(a=(b=1)) because the variables
            // in the arg list get ordered differently than you might expect (b comes first)
            // So the code below searches through all opt args, ensuring none of them skip
            // indicies. A skipped index means there's a hidden local var/arg like b above
            // and so we shouldn't try to compile.
            if (argsNode.getOptArgs() != null && argsNode.getOptArgs().size() > 0) {
                int index = argsNode.getArgsCount() - 1;
                
                for (int i = 0; i < argsNode.getOptArgs().size(); i++) {
                    int newIndex = ((LocalAsgnNode)argsNode.getOptArgs().get(i)).getIndex() - 2;
                    
                    if (newIndex - index != 1) {
                        throw new NotCompilableException("Can't compile def with optional args that assign other variables at: " + node.getPosition());
                    }
                    index = newIndex;
                }
            }
            
            // Also do not compile anything with a block argument or "rest" argument
            if (argsNode.getBlockArgNode() != null) throw new NotCompilableException("Can't compile def with block arg at: " + node.getPosition());
            if (argsNode.getRestArg() != -1) throw new NotCompilableException("Can't compile def with rest arg at: " + node.getPosition());
        }
        
        ClosureCallback body = new ClosureCallback() {
            public void compile(Compiler context) {
                if (defnNode.getBodyNode() != null) {
                    NodeCompilerFactory.getCompiler(defnNode.getBodyNode()).compile(defnNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        
        final ArrayCallback evalOptionalValue = new ArrayCallback() {
            public void nextValue(Compiler context, Object object, int index) {
                ListNode optArgs = (ListNode)object;
                
                Node node = optArgs.get(index);

                NodeCompilerFactory.getCompiler(node).compile(node, context);
            }
        };
        
        ClosureCallback args = new ClosureCallback() {
            public void compile(Compiler context) {
                int expectedArgsCount = argsNode.getArgsCount();
                int restArg = argsNode.getRestArg();
                boolean hasOptArgs = argsNode.getOptArgs() != null;
                Arity arity = argsNode.getArity();
                
                if (hasOptArgs) {
                    if (restArg > -1) {
                        throw new NotCompilableException("Can't compile def with rest arg at: " + defnNode.getPosition());
                    } else {
                        int opt = expectedArgsCount + argsNode.getOptArgs().size();
                        context.processRequiredArgs(arity, opt);
                        
                        ListNode optArgs = argsNode.getOptArgs();
                        context.assignOptionalArgs(optArgs, expectedArgsCount, optArgs.size(), evalOptionalValue);
                    }
                } else {
                    if (restArg > -1) {
                        throw new NotCompilableException("Can't compile def with rest arg at: " + defnNode.getPosition());
                    } else {
                        context.processRequiredArgs(arity, expectedArgsCount);
                    }
                }
            }
        };
        
        context.defineNewMethod(defnNode.getName(), defnNode.getScope(), body, args);
    }
    
}
