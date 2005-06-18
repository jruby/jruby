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
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;

/**
 * Visitor interface to be implemented by visitors of the jRuby AST.
 * each node will call the visit method appropriate to its type.
 * @see org.jruby.ast.Node
 * @see org.jruby.ast.visitor.AbstractVisitor
 * 
 * @author Benoit Cerrina
 * @version $Revision$
 **/
public interface NodeVisitor {
    public void visitAliasNode(AliasNode iVisited);
    public void visitAndNode(AndNode iVisited);
    public void visitArgsNode(ArgsNode iVisited);
    public void visitArgsCatNode(ArgsCatNode iVisited);
    public void visitArrayNode(ArrayNode iVisited);
    public void visitBackRefNode(BackRefNode iVisited);
    public void visitBeginNode(BeginNode iVisited);
    public void visitBignumNode(BignumNode iVisited);
    public void visitBlockArgNode(BlockArgNode iVisited);
    public void visitBlockNode(BlockNode iVisited);
    public void visitBlockPassNode(BlockPassNode iVisited);
    public void visitBreakNode(BreakNode iVisited);
    public void visitConstDeclNode(ConstDeclNode iVisited);
    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited);
    public void visitClassVarDeclNode(ClassVarDeclNode iVisited);
    public void visitClassVarNode(ClassVarNode iVisited);
    public void visitCallNode(CallNode iVisited);
    public void visitCaseNode(CaseNode iVisited);
    public void visitClassNode(ClassNode iVisited);
    public void visitColon2Node(Colon2Node iVisited);
    public void visitColon3Node(Colon3Node iVisited);
    public void visitConstNode(ConstNode iVisited);
    public void visitDAsgnNode(DAsgnNode iVisited);
    public void visitDRegxNode(DRegexpNode iVisited);
    public void visitDStrNode(DStrNode iVisited);
    public void visitDSymbolNode(DSymbolNode iVisited);
    public void visitDVarNode(DVarNode iVisited);
    public void visitDXStrNode(DXStrNode iVisited);
    public void visitDefinedNode(DefinedNode iVisited);
    public void visitDefnNode(DefnNode iVisited);
    public void visitDefsNode(DefsNode iVisited);
    public void visitDotNode(DotNode iVisited);
    public void visitEnsureNode(EnsureNode iVisited);
    public void visitEvStrNode(EvStrNode iVisited);
    public void visitFCallNode(FCallNode iVisited);
    public void visitFalseNode(FalseNode iVisited);
    public void visitFixnumNode(FixnumNode iVisited);
    public void visitFlipNode(FlipNode iVisited);
    public void visitFloatNode(FloatNode iVisited);
    public void visitForNode(ForNode iVisited);
    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited);
    public void visitGlobalVarNode(GlobalVarNode iVisited);
    public void visitHashNode(HashNode iVisited);
    public void visitInstAsgnNode(InstAsgnNode iVisited);
    public void visitInstVarNode(InstVarNode iVisited);
    public void visitIfNode(IfNode iVisited);
    public void visitIterNode(IterNode iVisited);
    public void visitLocalAsgnNode(LocalAsgnNode iVisited);
    public void visitLocalVarNode(LocalVarNode iVisited);
    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited);
    public void visitMatch2Node(Match2Node iVisited);
    public void visitMatch3Node(Match3Node iVisited);
    public void visitMatchNode(MatchNode iVisited);
    public void visitModuleNode(ModuleNode iVisited);
    public void visitNewlineNode(NewlineNode iVisited);
    public void visitNextNode(NextNode iVisited);
    public void visitNilNode(NilNode iVisited);
    public void visitNotNode(NotNode iVisited);
    public void visitNthRefNode(NthRefNode iVisited);
    public void visitOpElementAsgnNode(OpElementAsgnNode iVisited);
    public void visitOpAsgnNode(OpAsgnNode iVisited);
    public void visitOpAsgnAndNode(OpAsgnAndNode iVisited);
    public void visitOpAsgnOrNode(OpAsgnOrNode iVisited);
    public void visitOptNNode(OptNNode iVisited);
    public void visitOrNode(OrNode iVisited);
    public void visitPostExeNode(PostExeNode iVisited);
    public void visitRedoNode(RedoNode iVisited);
    public void visitRegexpNode(RegexpNode iVisited);
    public void visitRescueBodyNode(RescueBodyNode iVisited);
    public void visitRescueNode(RescueNode iVisited);
    public void visitRetryNode(RetryNode iVisited);
    public void visitReturnNode(ReturnNode iVisited);
    public void visitSClassNode(SClassNode iVisited);
    public void visitScopeNode(ScopeNode iVisited);
    public void visitSelfNode(SelfNode iVisited);
    public void visitSplatNode(SplatNode iVisited);
    public void visitStrNode(StrNode iVisited);
    public void visitSuperNode(SuperNode iVisited);
    public void visitSValueNode(SValueNode iVisited);
    public void visitSymbolNode(SymbolNode iVisited);
    public void visitToAryNode(ToAryNode iVisited);
    public void visitTrueNode(TrueNode iVisited);
    public void visitUndefNode(UndefNode iVisited);
    public void visitUntilNode(UntilNode iVisited);
    public void visitVAliasNode(VAliasNode iVisited);
    public void visitVCallNode(VCallNode iVisited);
    public void visitWhenNode(WhenNode iVisited);
    public void visitWhileNode(WhileNode iVisited);
    public void visitXStrNode(XStrNode iVisited);
    public void visitYieldNode(YieldNode iVisited);
    public void visitZArrayNode(ZArrayNode iVisited);
    public void visitZSuperNode(ZSuperNode iVisited);
}
