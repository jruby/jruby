/*
 * AssignmentVisitor.java - description
 * Created on 13.03.2002, 15:54:53
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
 */
package org.jruby.evaluator;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.visitor.AbstractVisitor;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class AssignmentVisitor extends AbstractVisitor {
    private Ruby runtime;
    private ThreadContext threadContext;
    private IRubyObject self;

    private IRubyObject value;
    private boolean check;
    private IRubyObject result;

    public AssignmentVisitor(Ruby runtime, IRubyObject self) {
        this.runtime = runtime;
        this.self = self;
        this.threadContext = runtime.getCurrentContext();
    }

    public IRubyObject assign(Node node, IRubyObject value, boolean check) {
        this.value = value;
        this.check = check;

        acceptNode(node);

        return result;
    }

    /**
     * @see AbstractVisitor#visitNode(Node)
     */
    protected void visitNode(Node iVisited) {
        assert false;
    }

    /**
     * @see AbstractVisitor#visitCallNode(CallNode)
     */
    public void visitCallNode(CallNode iVisited) {
        EvaluateVisitor evaluator = EvaluateVisitor.createVisitor(self);

        IRubyObject receiver = evaluator.eval(iVisited.getReceiverNode());

        if (iVisited.getArgsNode() == null) { // attribute set.
            receiver.getMetaClass().call(receiver, iVisited.getName(), new IRubyObject[] {value}, CallType.NORMAL);
        } else { // element set
            RubyArray args = (RubyArray) evaluator.eval(iVisited.getArgsNode());
            args.append(value);
            receiver.getMetaClass().call(receiver, iVisited.getName(), args.toJavaArray(), CallType.NORMAL);
        }
    }

    /**
     * @see AbstractVisitor#visitClassVarAsgnNode(ClassVarAsgnNode)
     */
    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        threadContext.getRubyClass().setClassVar(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitClassVarDeclNode(ClassVarDeclNode)
     */
    public void visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        if (runtime.getVerbose().isTrue() && threadContext.getRubyClass().isSingleton()) {
            runtime.getWarnings().warn(iVisited.getPosition(), "Declaring singleton class variable.");
        }
        threadContext.getRubyClass().setClassVar(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitConstDeclNode(ConstDeclNode)
     */
    public void visitConstDeclNode(ConstDeclNode iVisited) {
        threadContext.getRubyClass().defineConstant(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitDAsgnNode(DAsgnNode)
     */
    public void visitDAsgnNode(DAsgnNode iVisited) {
        threadContext.getCurrentDynamicVars().set(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitGlobalAsgnNode(GlobalAsgnNode)
     */
    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        runtime.getGlobalVariables().set(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitInstAsgnNode(InstAsgnNode)
     */
    public void visitInstAsgnNode(InstAsgnNode iVisited) {
        self.setInstanceVariable(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitLocalAsgnNode(LocalAsgnNode)
     */
    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
        threadContext.getScopeStack().setValue(iVisited.getCount(), value);
    }

    /**
     * @see AbstractVisitor#visitMultipleAsgnNode(MultipleAsgnNode)
     */
    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        result = threadContext.mAssign(self, iVisited, (RubyArray)value, check);
    }
}
