/*
 * BreakStatementVisitor.java - description
 * Created on 27.02.2002, 12:59:29
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
package org.jruby.ast.visitor;

import org.jruby.ast.BreakNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.Node;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class BreakStatementVisitor extends AbstractVisitor {
    private boolean breakStatement;
    
    public boolean isBreakStatement(Node node) {
        breakStatement = false;

        acceptNode(node);
        
        return breakStatement;
    }
    
    protected void visitNode(Node iVisited) {
    }

    public void visitBreakNode(BreakNode iVisited) {
        breakStatement = true;
    }

    public void visitNewlineNode(NewlineNode iVisited) {
        acceptNode(iVisited.getNextNode());
    }

    public void visitNextNode(NextNode iVisited) {
        breakStatement = true;
    }

    public void visitRedoNode(RedoNode iVisited) {
        breakStatement = true;
    }

    public void visitRetryNode(RetryNode iVisited) {
        breakStatement = true;
    }

    public void visitReturnNode(ReturnNode iVisited) {
        breakStatement = true;
    }
}