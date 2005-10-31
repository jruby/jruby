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

import org.jruby.ast.BackRefNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.TrueNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.evaluator.SingleNodeVisitor;

/**
 * 
 * @author jpetersen
 */
public class UselessStatementVisitor extends AbstractVisitor {
	private IRubyWarnings warnings;

	public UselessStatementVisitor(IRubyWarnings warnings) {
		this.warnings = warnings;
	}

	/**
	 * @see AbstractVisitor#visitNode(Node)
	 */
	protected SingleNodeVisitor visitNode(Node iVisited) {
		return null;
	}

	private void handleUselessWarn(Node node, String useless) {
		warnings.warn(node.getPosition(), "Useless use of " + useless
				+ " in void context.");
	}

	/**
	 * @see NodeVisitor#visitCallNode(CallNode)
	 */
	public SingleNodeVisitor visitCallNode(CallNode iVisited) {
		String name = iVisited.getName().intern();

		if (name == "+" || name == "-" || name == "*" || name == "/"
				|| name == "%" || name == "**" || name == "+@" || name == "-@"
				|| name == "|" || name == "^" || name == "&" || name == "<=>"
				|| name == ">" || name == ">=" || name == "<" || name == "<="
				|| name == "==" || name == "!=") {

			handleUselessWarn(iVisited, name);
		}
		return null;
	}

	/**
	 * @see NodeVisitor#visitBackRefNode(BackRefNode)
	 */
	public SingleNodeVisitor visitBackRefNode(BackRefNode iVisited) {
		handleUselessWarn(iVisited, "a variable");
		return null;
	}

	/**
	 * @see NodeVisitor#visitDVarNode(DVarNode)
	 */
	public SingleNodeVisitor visitDVarNode(DVarNode iVisited) {
		handleUselessWarn(iVisited, "a variable");
		return null;
	}

	/**
	 * @see NodeVisitor#visitGlobalVarNode(GlobalVarNode)
	 */
	public SingleNodeVisitor visitGlobalVarNode(GlobalVarNode iVisited) {
		handleUselessWarn(iVisited, "a variable");
		return null;
	}

	/**
	 * @see NodeVisitor#visitLocalVarNode(LocalVarNode)
	 */
	public SingleNodeVisitor visitLocalVarNode(LocalVarNode iVisited) {
		handleUselessWarn(iVisited, "a variable");
		return null;
	}

	/**
	 * @see NodeVisitor#visitNthRefNode(NthRefNode)
	 */
	public SingleNodeVisitor visitNthRefNode(NthRefNode iVisited) {
		handleUselessWarn(iVisited, "a variable");
		return null;
	}

	/**
	 * @see NodeVisitor#visitClassVarNode(ClassVarNode)
	 */
	public SingleNodeVisitor visitClassVarNode(ClassVarNode iVisited) {
		handleUselessWarn(iVisited, "a variable");
		return null;
	}

	/**
	 * @see NodeVisitor#visitInstVarNode(InstVarNode)
	 */
	public SingleNodeVisitor visitInstVarNode(InstVarNode iVisited) {
		handleUselessWarn(iVisited, "a variable");
		return null;
	}

	/**
	 * @see NodeVisitor#visitConstNode(ConstNode)
	 */
	public SingleNodeVisitor visitConstNode(ConstNode iVisited) {
		handleUselessWarn(iVisited, "a constant");
		return null;
	}

	/**
	 * @see NodeVisitor#visitBignumNode(BignumNode)
	 */
	public SingleNodeVisitor visitBignumNode(BignumNode iVisited) {
		handleUselessWarn(iVisited, "a literal");
		return null;
	}

	/**
	 * @see NodeVisitor#visitDRegxNode(DRegexpNode)
	 */
	public SingleNodeVisitor visitDRegxNode(DRegexpNode iVisited) {
		handleUselessWarn(iVisited, "a literal");
		return null;
	}

	/**
	 * @see NodeVisitor#visitDStrNode(DStrNode)
	 */
	public SingleNodeVisitor visitDStrNode(DStrNode iVisited) {
		handleUselessWarn(iVisited, "a literal");
		return null;
	}

	/**
	 * @see NodeVisitor#visitFixnumNode(FixnumNode)
	 */
	public SingleNodeVisitor visitFixnumNode(FixnumNode iVisited) {
		handleUselessWarn(iVisited, "a literal");
		return null;
	}

	/**
	 * @see NodeVisitor#visitFloatNode(FloatNode)
	 */
	public SingleNodeVisitor visitFloatNode(FloatNode iVisited) {
		handleUselessWarn(iVisited, "a literal");
		return null;
	}

	/**
	 * @see NodeVisitor#visitRegexpNode(RegexpNode)
	 */
	public SingleNodeVisitor visitRegexpNode(RegexpNode iVisited) {
		handleUselessWarn(iVisited, "a literal");
		return null;
	}

	/**
	 * @see NodeVisitor#visitStrNode(StrNode)
	 */
	public SingleNodeVisitor visitStrNode(StrNode iVisited) {
		handleUselessWarn(iVisited, "a literal");
		return null;
	}

	/**
	 * @see NodeVisitor#visitSymbolNode(SymbolNode)
	 */
	public SingleNodeVisitor visitSymbolNode(SymbolNode iVisited) {
		handleUselessWarn(iVisited, "a literal");
		return null;
	}

	/**
	 * @see NodeVisitor#visitClassNode(ClassNode)
	 */
	public SingleNodeVisitor visitClassNode(ClassNode iVisited) {
		handleUselessWarn(iVisited, "::");
		return null;
	}

	/**
	 * @see NodeVisitor#visitColon2Node(Colon2Node)
	 */
	public SingleNodeVisitor visitColon2Node(Colon2Node iVisited) {
		handleUselessWarn(iVisited, "::");
		return null;
	}

	/**
	 * @see NodeVisitor#visitDotNode(DotNode)
	 */
	public SingleNodeVisitor visitDotNode(DotNode iVisited) {
		handleUselessWarn(iVisited, iVisited.isExclusive() ? "..." : "..");
		return null;
	}

	/**
	 * @see NodeVisitor#visitDefinedNode(DefinedNode)
	 */
	public SingleNodeVisitor visitDefinedNode(DefinedNode iVisited) {
		handleUselessWarn(iVisited, "defined?");
		return null;
	}

	/**
	 * @see NodeVisitor#visitFalseNode(FalseNode)
	 */
	public SingleNodeVisitor visitFalseNode(FalseNode iVisited) {
		handleUselessWarn(iVisited, "false");
		return null;
	}

	/**
	 * @see NodeVisitor#visitNilNode(NilNode)
	 */
	public SingleNodeVisitor visitNilNode(NilNode iVisited) {
		handleUselessWarn(iVisited, "nil");
		return null;
	}

	/**
	 * @see NodeVisitor#visitSelfNode(SelfNode)
	 */
	public SingleNodeVisitor visitSelfNode(SelfNode iVisited) {
		handleUselessWarn(iVisited, "self");
		return null;
	}

	/**
	 * @see NodeVisitor#visitTrueNode(TrueNode)
	 */
	public SingleNodeVisitor visitTrueNode(TrueNode iVisited) {
		handleUselessWarn(iVisited, "true");
		return null;
	}

	/**
	 * @see NodeVisitor#visitNewlineNode(NewlineNode)
	 */
	public SingleNodeVisitor visitNewlineNode(NewlineNode iVisited) {
		acceptNode(iVisited.getNextNode());
		return null;
	}
}