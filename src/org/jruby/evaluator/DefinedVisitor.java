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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.evaluator;

import java.util.Iterator;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.visitor.AbstractVisitor;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/** This visitor is used to evaluate a defined? statement.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DefinedVisitor extends AbstractVisitor {
    private Ruby runtime;
    private IRubyObject self;
    private ThreadContext threadContext;

    private String definition;

    public DefinedVisitor(Ruby runtime, IRubyObject self) {
        this.runtime = runtime;
        this.self = self;
        this.threadContext = runtime.getCurrentContext();
    }

    public String getDefinition(Node expression) {
        definition = null;

        acceptNode(expression);

        return definition;
    }

    public String getArgumentDefinition(Node node, String type) {
        if (node == null) {
            return type;
        } else if (node instanceof ArrayNode) {
            Iterator iter = ((ArrayNode)node).iterator();
            while (iter.hasNext()) {
                if (getDefinition((Node)iter.next()) == null) {
                    return null;
                }
            }
        } else if (getDefinition(node) == null) {
            return null;
        }
        return type;
    }

    /**
     * @see AbstractVisitor#visitNode(Node)
     */
    protected void visitNode(Node iVisited) {
        try {
            EvaluateVisitor.createVisitor(self).eval(iVisited);
            definition = "expression";
        } catch (JumpException jumpExcptn) {
        }
    }

	/**
     * @see AbstractVisitor#visitSuperNode(SuperNode)
     */
    public void visitSuperNode(SuperNode iVisited) {
        String lastMethod = threadContext.getCurrentFrame().getLastFunc();
        RubyModule lastClass = threadContext.getCurrentFrame().getLastClass();
        if (lastMethod != null && lastClass != null && lastClass.getSuperClass().isMethodBound(lastMethod, false)) {
            definition = getArgumentDefinition(iVisited.getArgsNode(), "super");
        }
    }

    /**
     * @see AbstractVisitor#visitZSuperNode(ZSuperNode)
     */
    public void visitZSuperNode(ZSuperNode iVisited) {
        String lastMethod = threadContext.getCurrentFrame().getLastFunc();
        RubyModule lastClass = threadContext.getCurrentFrame().getLastClass();
        if (lastMethod != null && lastClass != null && lastClass.getSuperClass().isMethodBound(lastMethod, false)) {
            definition = "super";
        }
    }
    /**
     * @see AbstractVisitor#visitCallNode(CallNode)
     */
    public void visitCallNode(CallNode iVisited) {
        if (getDefinition(iVisited.getReceiverNode()) != null) {
            try {
                IRubyObject receiver = EvaluateVisitor.createVisitor(self).eval(iVisited.getReceiverNode());
                RubyClass metaClass = receiver.getMetaClass();
                ICallable method = metaClass.searchMethod(iVisited.getName());
                Visibility visibility = method.getVisibility();

                if (!visibility.isPrivate() && (!visibility.isProtected() || self.isKindOf(metaClass.getRealClass()))) {
                    if (metaClass.isMethodBound(iVisited.getName(), false)) {
                        definition = getArgumentDefinition(iVisited.getArgsNode(), "method");
                        return;
                    }
                }
            } catch (JumpException excptn) {
            }
        }
        definition = null;
    }

    /**
     * @see AbstractVisitor#visitFCallNode(FCallNode)
     */
    public void visitFCallNode(FCallNode iVisited) {
        if (self.getMetaClass().isMethodBound(iVisited.getName(), false)) {
            definition = getArgumentDefinition(iVisited.getArgsNode(), "method");
        }
    }

    /**
     * @see AbstractVisitor#visitVCallNode(VCallNode)
     */
    public void visitVCallNode(VCallNode iVisited) {
        if (self.getMetaClass().isMethodBound(iVisited.getMethodName(), false)) {
            definition = "method";
        }
    }

    /**
     * @see AbstractVisitor#visitMatch2Node(Match2Node)
     */
    public void visitMatch2Node(Match2Node iVisited) {
        definition = "method";
    }

    /**
     * @see AbstractVisitor#visitMatch3Node(Match3Node)
     */
    public void visitMatch3Node(Match3Node iVisited) {
        definition = "method";
    }

    /**
     * @see AbstractVisitor#visitFalseNode(FalseNode)
     */
    public void visitFalseNode(FalseNode iVisited) {
        definition = "false";
    }

    /**
     * @see AbstractVisitor#visitNilNode(NilNode)
     */
    public void visitNilNode(NilNode iVisited) {
        definition = "nil";
    }

    /**
     * @see AbstractVisitor#visitNullNode()
     */
    public void visitNullNode() {
        definition = "expression";
    }

    /**
     * @see AbstractVisitor#visitSelfNode(SelfNode)
     */
    public void visitSelfNode(SelfNode iVisited) {
        definition = "self";
    }

    /**
     * @see AbstractVisitor#visitTrueNode(TrueNode)
     */
    public void visitTrueNode(TrueNode iVisited) {
        definition = "true";
    }

    /**
     * @see AbstractVisitor#visitYieldNode(YieldNode)
     */
    public void visitYieldNode(YieldNode iVisited) {
        if (threadContext.isBlockGiven()) {
            definition = "yield";
        }
    }

    /**
     * @see AbstractVisitor#visitClassVarAsgnNode(ClassVarAsgnNode)
     */
    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        definition = "assignment";
    }

    /**
     * @see AbstractVisitor#visitClassVarDeclNode(ClassVarDeclNode)
     */
    public void visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        definition = "assignment";
    }

    /**
     * @see AbstractVisitor#visitConstDeclNode(ConstDeclNode)
     */
    public void visitConstDeclNode(ConstDeclNode iVisited) {
        definition = "assignment";
    }

    /**
     * @see AbstractVisitor#visitDAsgnNode(DAsgnNode)
     */
    public void visitDAsgnNode(DAsgnNode iVisited) {
        definition = "assignment";
    }

    /**
     * @see AbstractVisitor#visitGlobalAsgnNode(GlobalAsgnNode)
     */
    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        definition = "assignment";
    }

    /**
     * @see AbstractVisitor#visitLocalAsgnNode(LocalAsgnNode)
     */
    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
        definition = "assignment";
    }

    /**
     * @see AbstractVisitor#visitMultipleAsgnNode(MultipleAsgnNode)
     */
    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        definition = "assignment";
    }

    /**
     * @see AbstractVisitor#visitOpAsgnNode(OpAsgnNode)
     */
    public void visitOpAsgnNode(OpAsgnNode iVisited) {
        definition = "assignment";
    }

    /**
     * @see AbstractVisitor#visitOpElementAsgnNode(OpElementAsgnNode)
     */
    public void visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        definition = "assignment";
    }

    /**
     * @see AbstractVisitor#visitDVarNode(DVarNode)
     */
    public void visitDVarNode(DVarNode iVisited) {
        definition = "local-variable(in-block)";
    }

    /**
     * @see AbstractVisitor#visitLocalVarNode(LocalVarNode)
     */
    public void visitLocalVarNode(LocalVarNode iVisited) {
        definition = "local-variable";
    }

    /**
     * @see AbstractVisitor#visitClassVarNode(ClassVarNode)
     */
    public void visitClassVarNode(ClassVarNode iVisited) {
        if (threadContext.getRubyClass() == null && self.getMetaClass().isClassVarDefined(iVisited.getName())) {
            definition = "class_variable";
        } else if (!threadContext.getRubyClass().isSingleton() && threadContext.getRubyClass().isClassVarDefined(iVisited.getName())) {
            definition = "class_variable";
        } else {
        	RubyModule module = (RubyModule) threadContext.getRubyClass().getInstanceVariable("__attached__");
        	
        	if (module != null && module.isClassVarDefined(iVisited.getName())) {
        		definition = "class_variable";
            }
        }
    }

    /**
     * @see AbstractVisitor#visitConstNode(ConstNode)
     */
    public void visitConstNode(ConstNode iVisited) {
        if (runtime.getClass("Module").getConstant(iVisited.getName(), false) != null) {
            definition = "constant";
        }
    }

    /**
     * @see AbstractVisitor#visitGlobalVarNode(GlobalVarNode)
     */
    public void visitGlobalVarNode(GlobalVarNode iVisited) {
        if (runtime.getGlobalVariables().isDefined(iVisited.getName())) {
            definition = "global-variable";
        }
    }

    /**
     * @see AbstractVisitor#visitInstVarNode(InstVarNode)
     */
    public void visitInstVarNode(InstVarNode iVisited) {
        if (self.getInstanceVariable(iVisited.getName()) != null) {
            definition = "instance-variable";
        }
    }

    /**
     * @see AbstractVisitor#visitColon2Node(Colon2Node)
     */
    public void visitColon2Node(Colon2Node iVisited) {
        try {
            IRubyObject left = EvaluateVisitor.createVisitor(self).eval(iVisited.getLeftNode());
            if (left instanceof RubyModule) {
                if (((RubyModule)left).getConstantAt(iVisited.getName()) != null) {
                    definition = "constant";
                }
            } else if (left.getMetaClass().isMethodBound(iVisited.getName(), true)) {
                definition = "method";
            }
        } catch (JumpException excptn) {
        }
    }

    /**
     * @see AbstractVisitor#visitBackRefNode(BackRefNode)
     *
     * @fixme Add test if back ref exists.
     */
    public void visitBackRefNode(BackRefNode iVisited) {
        // if () {
        definition = "$" + iVisited.getType();
        // }
    }

    /**
     * @see AbstractVisitor#visitNthRefNode(NthRefNode)
     *
     * @fixme Add test if nth ref exists.
     */
    public void visitNthRefNode(NthRefNode iVisited) {
        // if () {
        definition = "$" + iVisited.getMatchNumber();
        // }
    }
}
