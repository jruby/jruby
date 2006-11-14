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
import org.jruby.ast.RootNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
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
import org.jruby.evaluator.Instruction;

/**
 * Visitor interface to be implemented by visitors of the jRuby AST.
 * each node will call the visit method appropriate to its type.
 * @see org.jruby.ast.Node
 * @see org.jruby.ast.visitor.AbstractVisitor
 * 
 * @author Benoit Cerrina
 **/
public interface NodeVisitor {
    public Instruction visitAliasNode(AliasNode iVisited);
    public Instruction visitAndNode(AndNode iVisited);
    public Instruction visitArgsNode(ArgsNode iVisited);
    public Instruction visitArgsCatNode(ArgsCatNode iVisited);
    public Instruction visitArrayNode(ArrayNode iVisited);
    public Instruction visitBackRefNode(BackRefNode iVisited);
    public Instruction visitBeginNode(BeginNode iVisited);
    public Instruction visitBignumNode(BignumNode iVisited);
    public Instruction visitBlockArgNode(BlockArgNode iVisited);
    public Instruction visitBlockNode(BlockNode iVisited);
    public Instruction visitBlockPassNode(BlockPassNode iVisited);
    public Instruction visitBreakNode(BreakNode iVisited);
    public Instruction visitConstDeclNode(ConstDeclNode iVisited);
    public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited);
    public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited);
    public Instruction visitClassVarNode(ClassVarNode iVisited);
    public Instruction visitCallNode(CallNode iVisited);
    public Instruction visitCaseNode(CaseNode iVisited);
    public Instruction visitClassNode(ClassNode iVisited);
    public Instruction visitColon2Node(Colon2Node iVisited);
    public Instruction visitColon3Node(Colon3Node iVisited);
    public Instruction visitConstNode(ConstNode iVisited);
    public Instruction visitDAsgnNode(DAsgnNode iVisited);
    public Instruction visitDRegxNode(DRegexpNode iVisited);
    public Instruction visitDStrNode(DStrNode iVisited);
    public Instruction visitDSymbolNode(DSymbolNode iVisited);
    public Instruction visitDVarNode(DVarNode iVisited);
    public Instruction visitDXStrNode(DXStrNode iVisited);
    public Instruction visitDefinedNode(DefinedNode iVisited);
    public Instruction visitDefnNode(DefnNode iVisited);
    public Instruction visitDefsNode(DefsNode iVisited);
    public Instruction visitDotNode(DotNode iVisited);
    public Instruction visitEnsureNode(EnsureNode iVisited);
    public Instruction visitEvStrNode(EvStrNode iVisited);
    public Instruction visitFCallNode(FCallNode iVisited);
    public Instruction visitFalseNode(FalseNode iVisited);
    public Instruction visitFixnumNode(FixnumNode iVisited);
    public Instruction visitFlipNode(FlipNode iVisited);
    public Instruction visitFloatNode(FloatNode iVisited);
    public Instruction visitForNode(ForNode iVisited);
    public Instruction visitGlobalAsgnNode(GlobalAsgnNode iVisited);
    public Instruction visitGlobalVarNode(GlobalVarNode iVisited);
    public Instruction visitHashNode(HashNode iVisited);
    public Instruction visitInstAsgnNode(InstAsgnNode iVisited);
    public Instruction visitInstVarNode(InstVarNode iVisited);
    public Instruction visitIfNode(IfNode iVisited);
    public Instruction visitIterNode(IterNode iVisited);
    public Instruction visitLocalAsgnNode(LocalAsgnNode iVisited);
    public Instruction visitLocalVarNode(LocalVarNode iVisited);
    public Instruction visitMultipleAsgnNode(MultipleAsgnNode iVisited);
    public Instruction visitMatch2Node(Match2Node iVisited);
    public Instruction visitMatch3Node(Match3Node iVisited);
    public Instruction visitMatchNode(MatchNode iVisited);
    public Instruction visitModuleNode(ModuleNode iVisited);
    public Instruction visitNewlineNode(NewlineNode iVisited);
    public Instruction visitNextNode(NextNode iVisited);
    public Instruction visitNilNode(NilNode iVisited);
    public Instruction visitNotNode(NotNode iVisited);
    public Instruction visitNthRefNode(NthRefNode iVisited);
    public Instruction visitOpElementAsgnNode(OpElementAsgnNode iVisited);
    public Instruction visitOpAsgnNode(OpAsgnNode iVisited);
    public Instruction visitOpAsgnAndNode(OpAsgnAndNode iVisited);
    public Instruction visitOpAsgnOrNode(OpAsgnOrNode iVisited);
    public Instruction visitOptNNode(OptNNode iVisited);
    public Instruction visitOrNode(OrNode iVisited);
    public Instruction visitPostExeNode(PostExeNode iVisited);
    public Instruction visitRedoNode(RedoNode iVisited);
    public Instruction visitRegexpNode(RegexpNode iVisited);
    public Instruction visitRescueBodyNode(RescueBodyNode iVisited);
    public Instruction visitRescueNode(RescueNode iVisited);
    public Instruction visitRetryNode(RetryNode iVisited);
    public Instruction visitReturnNode(ReturnNode iVisited);
    public Instruction visitRootNode(RootNode iVisited);
    public Instruction visitSClassNode(SClassNode iVisited);
    public Instruction visitSelfNode(SelfNode iVisited);
    public Instruction visitSplatNode(SplatNode iVisited);
    public Instruction visitStrNode(StrNode iVisited);
    public Instruction visitSuperNode(SuperNode iVisited);
    public Instruction visitSValueNode(SValueNode iVisited);
    public Instruction visitSymbolNode(SymbolNode iVisited);
    public Instruction visitToAryNode(ToAryNode iVisited);
    public Instruction visitTrueNode(TrueNode iVisited);
    public Instruction visitUndefNode(UndefNode iVisited);
    public Instruction visitUntilNode(UntilNode iVisited);
    public Instruction visitVAliasNode(VAliasNode iVisited);
    public Instruction visitVCallNode(VCallNode iVisited);
    public Instruction visitWhenNode(WhenNode iVisited);
    public Instruction visitWhileNode(WhileNode iVisited);
    public Instruction visitXStrNode(XStrNode iVisited);
    public Instruction visitYieldNode(YieldNode iVisited);
    public Instruction visitZArrayNode(ZArrayNode iVisited);
    public Instruction visitZSuperNode(ZSuperNode iVisited);
}
