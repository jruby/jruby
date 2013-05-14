/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast.visitor;

import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgsPushNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArg18Node;
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
import org.jruby.ast.EncodingNode;
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
import org.jruby.ast.KeywordArgNode;
import org.jruby.ast.KeywordRestArgNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LiteralNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgn19Node;
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
import org.jruby.ast.OptArgNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RestArgNode;
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

/**
 * Visitor interface to be implemented by visitors of the jRuby AST.
 * each node will call the visit method appropriate to its type.
 * @see org.jruby.ast.Node
 * @see org.jruby.ast.visitor.AbstractVisitor
 * 
 * @author Benoit Cerrina
 **/
public interface NodeVisitor {
    public Object visitAliasNode(AliasNode iVisited);
    public Object visitAndNode(AndNode iVisited);
    public Object visitArgsNode(ArgsNode iVisited);
    public Object visitArgsCatNode(ArgsCatNode iVisited);
    public Object visitArgsPushNode(ArgsPushNode iVisited);
    public Object visitArrayNode(ArrayNode iVisited);
    public Object visitAttrAssignNode(AttrAssignNode iVisited);
    public Object visitBackRefNode(BackRefNode iVisited);
    public Object visitBeginNode(BeginNode iVisited);
    public Object visitBignumNode(BignumNode iVisited);
    public Object visitBlockArgNode(BlockArgNode iVisited);
    public Object visitBlockArg18Node(BlockArg18Node iVisited);
    public Object visitBlockNode(BlockNode iVisited);
    public Object visitBlockPassNode(BlockPassNode iVisited);
    public Object visitBreakNode(BreakNode iVisited);
    public Object visitConstDeclNode(ConstDeclNode iVisited);
    public Object visitClassVarAsgnNode(ClassVarAsgnNode iVisited);
    public Object visitClassVarDeclNode(ClassVarDeclNode iVisited);
    public Object visitClassVarNode(ClassVarNode iVisited);
    public Object visitCallNode(CallNode iVisited);
    public Object visitCaseNode(CaseNode iVisited);
    public Object visitClassNode(ClassNode iVisited);
    public Object visitColon2Node(Colon2Node iVisited);
    public Object visitColon3Node(Colon3Node iVisited);
    public Object visitConstNode(ConstNode iVisited);
    public Object visitDAsgnNode(DAsgnNode iVisited);
    public Object visitDRegxNode(DRegexpNode iVisited);
    public Object visitDStrNode(DStrNode iVisited);
    public Object visitDSymbolNode(DSymbolNode iVisited);
    public Object visitDVarNode(DVarNode iVisited);
    public Object visitDXStrNode(DXStrNode iVisited);
    public Object visitDefinedNode(DefinedNode iVisited);
    public Object visitDefnNode(DefnNode iVisited);
    public Object visitDefsNode(DefsNode iVisited);
    public Object visitDotNode(DotNode iVisited);
    public Object visitEncodingNode(EncodingNode iVisited);
    public Object visitEnsureNode(EnsureNode iVisited);
    public Object visitEvStrNode(EvStrNode iVisited);
    public Object visitFCallNode(FCallNode iVisited);
    public Object visitFalseNode(FalseNode iVisited);
    public Object visitFixnumNode(FixnumNode iVisited);
    public Object visitFlipNode(FlipNode iVisited);
    public Object visitFloatNode(FloatNode iVisited);
    public Object visitForNode(ForNode iVisited);
    public Object visitGlobalAsgnNode(GlobalAsgnNode iVisited);
    public Object visitGlobalVarNode(GlobalVarNode iVisited);
    public Object visitHashNode(HashNode iVisited);
    public Object visitInstAsgnNode(InstAsgnNode iVisited);
    public Object visitInstVarNode(InstVarNode iVisited);
    public Object visitIfNode(IfNode iVisited);
    public Object visitIterNode(IterNode iVisited);
    public Object visitKeywordArgNode(KeywordArgNode iVisited);
    public Object visitKeywordRestArgNode(KeywordRestArgNode iVisited);
    public Object visitLambdaNode(LambdaNode iVisited);
    public Object visitListNode(ListNode iVisited);
    public Object visitLiteralNode(LiteralNode iVisited);
    public Object visitLocalAsgnNode(LocalAsgnNode iVisited);
    public Object visitLocalVarNode(LocalVarNode iVisited);
    public Object visitMultipleAsgnNode(MultipleAsgnNode iVisited);
    public Object visitMultipleAsgnNode(MultipleAsgn19Node iVisited);
    public Object visitMatch2Node(Match2Node iVisited);
    public Object visitMatch3Node(Match3Node iVisited);
    public Object visitMatchNode(MatchNode iVisited);
    public Object visitModuleNode(ModuleNode iVisited);
    public Object visitNewlineNode(NewlineNode iVisited);
    public Object visitNextNode(NextNode iVisited);
    public Object visitNilNode(NilNode iVisited);
    public Object visitNotNode(NotNode iVisited);
    public Object visitNthRefNode(NthRefNode iVisited);
    public Object visitOpElementAsgnNode(OpElementAsgnNode iVisited);
    public Object visitOpAsgnNode(OpAsgnNode iVisited);
    public Object visitOpAsgnAndNode(OpAsgnAndNode iVisited);
    public Object visitOpAsgnOrNode(OpAsgnOrNode iVisited);
    public Object visitOptArgNode(OptArgNode iVisited);
    public Object visitOrNode(OrNode iVisited);
    public Object visitPreExeNode(PreExeNode iVisited);
    public Object visitPostExeNode(PostExeNode iVisited);
    public Object visitRedoNode(RedoNode iVisited);
    public Object visitRegexpNode(RegexpNode iVisited);
    public Object visitRescueBodyNode(RescueBodyNode iVisited);
    public Object visitRescueNode(RescueNode iVisited);
    public Object visitRestArgNode(RestArgNode iVisited);
    public Object visitRetryNode(RetryNode iVisited);
    public Object visitReturnNode(ReturnNode iVisited);
    public Object visitRootNode(RootNode iVisited);
    public Object visitSClassNode(SClassNode iVisited);
    public Object visitSelfNode(SelfNode iVisited);
    public Object visitSplatNode(SplatNode iVisited);
    public Object visitStrNode(StrNode iVisited);
    public Object visitSuperNode(SuperNode iVisited);
    public Object visitSValueNode(SValueNode iVisited);
    public Object visitSymbolNode(SymbolNode iVisited);
    public Object visitToAryNode(ToAryNode iVisited);
    public Object visitTrueNode(TrueNode iVisited);
    public Object visitUndefNode(UndefNode iVisited);
    public Object visitUntilNode(UntilNode iVisited);
    public Object visitVAliasNode(VAliasNode iVisited);
    public Object visitVCallNode(VCallNode iVisited);
    public Object visitWhenNode(WhenNode iVisited);
    public Object visitWhileNode(WhileNode iVisited);
    public Object visitXStrNode(XStrNode iVisited);
    public Object visitYieldNode(YieldNode iVisited);
    public Object visitZArrayNode(ZArrayNode iVisited);
    public Object visitZSuperNode(ZSuperNode iVisited);
}
