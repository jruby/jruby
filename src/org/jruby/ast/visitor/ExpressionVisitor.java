/*
 * ValueExpressionVisitor.java - description
 * Created on 19.02.2002, 14:26:33
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
package org.jruby.ast.visitor;

import org.jruby.ast.BeginNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.Node;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.WhileNode;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ExpressionVisitor extends AbstractVisitor {
    private boolean expression;
    
    public boolean isExpression(Node node) {
        acceptNode(node);
       	return expression;
    }

    protected void visitNode(Node iVisited) {
        expression = true;
    }

    public void visitBeginNode(BeginNode iVisited) {
        acceptNode(iVisited.getBodyNode());
    }

    public void visitBlockNode(BlockNode iVisited) {
		acceptNode(iVisited.getLast());
    }

    public void visitBreakNode(BreakNode iVisited) {
    	acceptNode(iVisited.getValueNode());
    }

    public void visitClassNode(ClassNode iVisited) {
        expression = false;
    }

    public void visitDefnNode(DefnNode iVisited) {
        expression = false;
    }

    public void visitDefsNode(DefsNode iVisited) {
        expression = false;
    }

    public void visitIfNode(IfNode iVisited) {
        expression = isExpression(iVisited.getThenBody()) && isExpression(iVisited.getElseBody());
    }

    public void visitModuleNode(ModuleNode iVisited) {
        expression = false;
    }

    public void visitNewlineNode(NewlineNode iVisited) {
        acceptNode(iVisited.getNextNode());
    }

    public void visitNextNode(NextNode iVisited) {
        expression = false;
    }

    public void visitRedoNode(RedoNode iVisited) {
        expression = false;
    }
    
    public void visitRetryNode(RetryNode iVisited) {
        expression = false;
    }

    public void visitReturnNode(ReturnNode iVisited) {
        expression = false;
    }

    public void visitUntilNode(UntilNode iVisited) {
        expression = false;
    }

    public void visitWhileNode(WhileNode iVisited) {
        expression = false;
    }
}
