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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
import org.jruby.evaluator.SingleNodeVisitor;

/**
 * 
 * @author jpetersen
 */
public class ExpressionVisitor extends AbstractVisitor {
	private boolean expression;

	public boolean isExpression(Node node) {
		acceptNode(node);
		return expression;
	}

	protected SingleNodeVisitor visitNode(Node iVisited) {
		expression = true;
		return null;
	}

	public SingleNodeVisitor visitBeginNode(BeginNode iVisited) {
		acceptNode(iVisited.getBodyNode());
		return null;
	}

	public SingleNodeVisitor visitBlockNode(BlockNode iVisited) {
		acceptNode(iVisited.getLast());
		return null;
	}

	public SingleNodeVisitor visitBreakNode(BreakNode iVisited) {
		acceptNode(iVisited.getValueNode());
		return null;
	}

	public SingleNodeVisitor visitClassNode(ClassNode iVisited) {
		expression = false;
		return null;
	}

	public SingleNodeVisitor visitDefnNode(DefnNode iVisited) {
		expression = false;
		return null;
	}

	public SingleNodeVisitor visitDefsNode(DefsNode iVisited) {
		expression = false;
		return null;
	}

	public SingleNodeVisitor visitIfNode(IfNode iVisited) {
		expression = isExpression(iVisited.getThenBody())
				&& isExpression(iVisited.getElseBody());
		return null;
	}

	public SingleNodeVisitor visitModuleNode(ModuleNode iVisited) {
		expression = false;
		return null;
	}

	public SingleNodeVisitor visitNewlineNode(NewlineNode iVisited) {
		acceptNode(iVisited.getNextNode());
		return null;
	}

	public SingleNodeVisitor visitNextNode(NextNode iVisited) {
		expression = false;
		return null;
	}

	public SingleNodeVisitor visitRedoNode(RedoNode iVisited) {
		expression = false;
		return null;
	}

	public SingleNodeVisitor visitRetryNode(RetryNode iVisited) {
		expression = false;
		return null;
	}

	public SingleNodeVisitor visitReturnNode(ReturnNode iVisited) {
		expression = false;
		return null;
	}

	public SingleNodeVisitor visitUntilNode(UntilNode iVisited) {
		expression = false;
		return null;
	}

	public SingleNodeVisitor visitWhileNode(WhileNode iVisited) {
		expression = false;
		return null;
	}
}