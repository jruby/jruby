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
import org.jruby.evaluator.SingleNodeVisitor;

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
    public SingleNodeVisitor visitAliasNode(AliasNode iVisited);
    public SingleNodeVisitor visitAndNode(AndNode iVisited);
    public SingleNodeVisitor visitArgsNode(ArgsNode iVisited);
    public SingleNodeVisitor visitArgsCatNode(ArgsCatNode iVisited);
    public SingleNodeVisitor visitArrayNode(ArrayNode iVisited);
    public SingleNodeVisitor visitBackRefNode(BackRefNode iVisited);
    public SingleNodeVisitor visitBeginNode(BeginNode iVisited);
    public SingleNodeVisitor visitBignumNode(BignumNode iVisited);
    public SingleNodeVisitor visitBlockArgNode(BlockArgNode iVisited);
    public SingleNodeVisitor visitBlockNode(BlockNode iVisited);
    public SingleNodeVisitor visitBlockPassNode(BlockPassNode iVisited);
    public SingleNodeVisitor visitBreakNode(BreakNode iVisited);
    public SingleNodeVisitor visitConstDeclNode(ConstDeclNode iVisited);
    public SingleNodeVisitor visitClassVarAsgnNode(ClassVarAsgnNode iVisited);
    public SingleNodeVisitor visitClassVarDeclNode(ClassVarDeclNode iVisited);
    public SingleNodeVisitor visitClassVarNode(ClassVarNode iVisited);
    public SingleNodeVisitor visitCallNode(CallNode iVisited);
    public SingleNodeVisitor visitCaseNode(CaseNode iVisited);
    public SingleNodeVisitor visitClassNode(ClassNode iVisited);
    public SingleNodeVisitor visitColon2Node(Colon2Node iVisited);
    public SingleNodeVisitor visitColon3Node(Colon3Node iVisited);
    public SingleNodeVisitor visitConstNode(ConstNode iVisited);
    public SingleNodeVisitor visitDAsgnNode(DAsgnNode iVisited);
    public SingleNodeVisitor visitDRegxNode(DRegexpNode iVisited);
    public SingleNodeVisitor visitDStrNode(DStrNode iVisited);
    public SingleNodeVisitor visitDSymbolNode(DSymbolNode iVisited);
    public SingleNodeVisitor visitDVarNode(DVarNode iVisited);
    public SingleNodeVisitor visitDXStrNode(DXStrNode iVisited);
    public SingleNodeVisitor visitDefinedNode(DefinedNode iVisited);
    public SingleNodeVisitor visitDefnNode(DefnNode iVisited);
    public SingleNodeVisitor visitDefsNode(DefsNode iVisited);
    public SingleNodeVisitor visitDotNode(DotNode iVisited);
    public SingleNodeVisitor visitEnsureNode(EnsureNode iVisited);
    public SingleNodeVisitor visitEvStrNode(EvStrNode iVisited);
    public SingleNodeVisitor visitFCallNode(FCallNode iVisited);
    public SingleNodeVisitor visitFalseNode(FalseNode iVisited);
    public SingleNodeVisitor visitFixnumNode(FixnumNode iVisited);
    public SingleNodeVisitor visitFlipNode(FlipNode iVisited);
    public SingleNodeVisitor visitFloatNode(FloatNode iVisited);
    public SingleNodeVisitor visitForNode(ForNode iVisited);
    public SingleNodeVisitor visitGlobalAsgnNode(GlobalAsgnNode iVisited);
    public SingleNodeVisitor visitGlobalVarNode(GlobalVarNode iVisited);
    public SingleNodeVisitor visitHashNode(HashNode iVisited);
    public SingleNodeVisitor visitInstAsgnNode(InstAsgnNode iVisited);
    public SingleNodeVisitor visitInstVarNode(InstVarNode iVisited);
    public SingleNodeVisitor visitIfNode(IfNode iVisited);
    public SingleNodeVisitor visitIterNode(IterNode iVisited);
    public SingleNodeVisitor visitLocalAsgnNode(LocalAsgnNode iVisited);
    public SingleNodeVisitor visitLocalVarNode(LocalVarNode iVisited);
    public SingleNodeVisitor visitMultipleAsgnNode(MultipleAsgnNode iVisited);
    public SingleNodeVisitor visitMatch2Node(Match2Node iVisited);
    public SingleNodeVisitor visitMatch3Node(Match3Node iVisited);
    public SingleNodeVisitor visitMatchNode(MatchNode iVisited);
    public SingleNodeVisitor visitModuleNode(ModuleNode iVisited);
    public SingleNodeVisitor visitNewlineNode(NewlineNode iVisited);
    public SingleNodeVisitor visitNextNode(NextNode iVisited);
    public SingleNodeVisitor visitNilNode(NilNode iVisited);
    public SingleNodeVisitor visitNotNode(NotNode iVisited);
    public SingleNodeVisitor visitNthRefNode(NthRefNode iVisited);
    public SingleNodeVisitor visitOpElementAsgnNode(OpElementAsgnNode iVisited);
    public SingleNodeVisitor visitOpAsgnNode(OpAsgnNode iVisited);
    public SingleNodeVisitor visitOpAsgnAndNode(OpAsgnAndNode iVisited);
    public SingleNodeVisitor visitOpAsgnOrNode(OpAsgnOrNode iVisited);
    public SingleNodeVisitor visitOptNNode(OptNNode iVisited);
    public SingleNodeVisitor visitOrNode(OrNode iVisited);
    public SingleNodeVisitor visitPostExeNode(PostExeNode iVisited);
    public SingleNodeVisitor visitRedoNode(RedoNode iVisited);
    public SingleNodeVisitor visitRegexpNode(RegexpNode iVisited);
    public SingleNodeVisitor visitRescueBodyNode(RescueBodyNode iVisited);
    public SingleNodeVisitor visitRescueNode(RescueNode iVisited);
    public SingleNodeVisitor visitRetryNode(RetryNode iVisited);
    public SingleNodeVisitor visitReturnNode(ReturnNode iVisited);
    public SingleNodeVisitor visitSClassNode(SClassNode iVisited);
    public SingleNodeVisitor visitScopeNode(ScopeNode iVisited);
    public SingleNodeVisitor visitSelfNode(SelfNode iVisited);
    public SingleNodeVisitor visitSplatNode(SplatNode iVisited);
    public SingleNodeVisitor visitStrNode(StrNode iVisited);
    public SingleNodeVisitor visitSuperNode(SuperNode iVisited);
    public SingleNodeVisitor visitSValueNode(SValueNode iVisited);
    public SingleNodeVisitor visitSymbolNode(SymbolNode iVisited);
    public SingleNodeVisitor visitToAryNode(ToAryNode iVisited);
    public SingleNodeVisitor visitTrueNode(TrueNode iVisited);
    public SingleNodeVisitor visitUndefNode(UndefNode iVisited);
    public SingleNodeVisitor visitUntilNode(UntilNode iVisited);
    public SingleNodeVisitor visitVAliasNode(VAliasNode iVisited);
    public SingleNodeVisitor visitVCallNode(VCallNode iVisited);
    public SingleNodeVisitor visitWhenNode(WhenNode iVisited);
    public SingleNodeVisitor visitWhileNode(WhileNode iVisited);
    public SingleNodeVisitor visitXStrNode(XStrNode iVisited);
    public SingleNodeVisitor visitYieldNode(YieldNode iVisited);
    public SingleNodeVisitor visitZArrayNode(ZArrayNode iVisited);
    public SingleNodeVisitor visitZSuperNode(ZSuperNode iVisited);
}
