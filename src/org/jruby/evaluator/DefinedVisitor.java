/*
 * DefinedVisitor.java - description
 * Created on 19.02.2002, 16:31:56
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
 */
package org.jruby.evaluator;

import java.util.Iterator;

import org.ablaf.ast.INode;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrSetNode;
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
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/** This visitor is used to evaluate a defined? statement.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DefinedVisitor extends AbstractVisitor {
    private Ruby ruby;
    private IRubyObject self;

    private String definition;

    public DefinedVisitor(Ruby ruby, IRubyObject self) {
        this.ruby = ruby;
        this.self = self;
    }

    public String getDefinition(INode expression) {
        definition = null;

        acceptNode(expression);

        return definition;
    }

    public String getArgumentDefinition(INode node, String type) {
        if (node == null) {
            return type;
        } else if (node instanceof ArrayNode) {
            Iterator iter = ((ArrayNode)node).iterator();
            while (iter.hasNext()) {
                if (getDefinition((INode)iter.next()) == null) {
                    return null;
                }
            }
        } else if (getDefinition(node) == null) {
            return null;
        }
        return type;
    }

    /**
     * @see AbstractVisitor#visitNode(INode)
     */
    protected void visitNode(INode iVisited) {
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
        String lastMethod = ruby.getCurrentFrame().getLastFunc();
        RubyModule lastClass = ruby.getCurrentFrame().getLastClass();
        if (lastMethod != null && lastClass != null && lastClass.getSuperClass().isMethodBound(lastMethod, false)) {
            definition = getArgumentDefinition(iVisited.getArgsNode(), "super");
        }
    }

    /**
     * @see AbstractVisitor#visitZSuperNode(ZSuperNode)
     */
    public void visitZSuperNode(ZSuperNode iVisited) {
        String lastMethod = ruby.getCurrentFrame().getLastFunc();
        RubyModule lastClass = ruby.getCurrentFrame().getLastClass();
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
                
                Visibility visibility = receiver.getInternalClass().getMethodVisibility(iVisited.getName());

                if (!visibility.isPrivate() && (!visibility.isProtected() || self.isKindOf(receiver.getInternalClass().getRealClass()))) {
                    if (receiver.getInternalClass().isMethodBound(iVisited.getName(), false)) {
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
        if (self.getInternalClass().isMethodBound(iVisited.getName(), false)) {
            definition = getArgumentDefinition(iVisited.getArgsNode(), "method");
        }
    }

    /**
     * @see AbstractVisitor#visitVCallNode(VCallNode)
     */
    public void visitVCallNode(VCallNode iVisited) {
        if (self.getInternalClass().isMethodBound(iVisited.getMethodName(), false)) {
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
        if (ruby.isBlockGiven()) {
            definition = "yield";
        }
    }
    /**
     * @see AbstractVisitor#visitAttrSetNode(AttrSetNode)
     */
    public void visitAttrSetNode(AttrSetNode iVisited) {
        definition = "assignment";
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
        if (ruby.getCBase() == null && self.getInternalClass().isClassVarDefined(iVisited.getName())) {
            definition = "class_variable";
        } else if (!ruby.getCBase().isSingleton() && ruby.getCBase().isClassVarDefined(iVisited.getName())) {
            definition = "class_variable";
        } else if (((RubyModule)ruby.getCBase().getInstanceVariable("__attached__")).isClassVarDefined(iVisited.getName())) {
            definition = "class_variable";
        }
    }

    /**
     * @see AbstractVisitor#visitConstNode(ConstNode)
     * 
     * @fixme Implement this method.
     */
    public void visitConstNode(ConstNode iVisited) {
        // if (isConstant()) {
        definition = "constant";
        // }
    }

    /**
     * @see AbstractVisitor#visitGlobalVarNode(GlobalVarNode)
     */
    public void visitGlobalVarNode(GlobalVarNode iVisited) {
        if (ruby.getGlobalVariables().isDefined(iVisited.getName())) {
            definition = "global-variable";
        }
    }

    /**
     * @see AbstractVisitor#visitInstVarNode(InstVarNode)
     */
    public void visitInstVarNode(InstVarNode iVisited) {
        if (self.hasInstanceVariable(iVisited.getName())) {
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
                if (((RubyModule)left).isConstantDefinedAt(iVisited.getName())) {
                    definition = "constant";
                }
            } else if (left.getInternalClass().isMethodBound(iVisited.getName(), true)) {
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
