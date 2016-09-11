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
package org.jruby.truffle.parser.ast.visitor;

import org.jruby.truffle.parser.ast.AliasParseNode;
import org.jruby.truffle.parser.ast.AndParseNode;
import org.jruby.truffle.parser.ast.ArgsCatParseNode;
import org.jruby.truffle.parser.ast.ArgsParseNode;
import org.jruby.truffle.parser.ast.ArgsPushParseNode;
import org.jruby.truffle.parser.ast.ArgumentParseNode;
import org.jruby.truffle.parser.ast.ArrayParseNode;
import org.jruby.truffle.parser.ast.AttrAssignParseNode;
import org.jruby.truffle.parser.ast.BackRefParseNode;
import org.jruby.truffle.parser.ast.BeginParseNode;
import org.jruby.truffle.parser.ast.BignumParseNode;
import org.jruby.truffle.parser.ast.BlockArgParseNode;
import org.jruby.truffle.parser.ast.BlockParseNode;
import org.jruby.truffle.parser.ast.BlockPassParseNode;
import org.jruby.truffle.parser.ast.BreakParseNode;
import org.jruby.truffle.parser.ast.CallParseNode;
import org.jruby.truffle.parser.ast.CaseParseNode;
import org.jruby.truffle.parser.ast.ClassParseNode;
import org.jruby.truffle.parser.ast.ClassVarAsgnParseNode;
import org.jruby.truffle.parser.ast.ClassVarDeclParseNode;
import org.jruby.truffle.parser.ast.ClassVarParseNode;
import org.jruby.truffle.parser.ast.Colon2ParseNode;
import org.jruby.truffle.parser.ast.Colon3ParseNode;
import org.jruby.truffle.parser.ast.ComplexParseNode;
import org.jruby.truffle.parser.ast.ConstDeclParseNode;
import org.jruby.truffle.parser.ast.ConstParseNode;
import org.jruby.truffle.parser.ast.DAsgnParseNode;
import org.jruby.truffle.parser.ast.DRegexpParseNode;
import org.jruby.truffle.parser.ast.DStrParseNode;
import org.jruby.truffle.parser.ast.DSymbolParseNode;
import org.jruby.truffle.parser.ast.DVarParseNode;
import org.jruby.truffle.parser.ast.DXStrParseNode;
import org.jruby.truffle.parser.ast.DefinedParseNode;
import org.jruby.truffle.parser.ast.DefnParseNode;
import org.jruby.truffle.parser.ast.DefsParseNode;
import org.jruby.truffle.parser.ast.DotParseNode;
import org.jruby.truffle.parser.ast.EncodingParseNode;
import org.jruby.truffle.parser.ast.EnsureParseNode;
import org.jruby.truffle.parser.ast.EvStrParseNode;
import org.jruby.truffle.parser.ast.FCallParseNode;
import org.jruby.truffle.parser.ast.FalseParseNode;
import org.jruby.truffle.parser.ast.FixnumParseNode;
import org.jruby.truffle.parser.ast.FlipParseNode;
import org.jruby.truffle.parser.ast.FloatParseNode;
import org.jruby.truffle.parser.ast.ForParseNode;
import org.jruby.truffle.parser.ast.GlobalAsgnParseNode;
import org.jruby.truffle.parser.ast.GlobalVarParseNode;
import org.jruby.truffle.parser.ast.HashParseNode;
import org.jruby.truffle.parser.ast.IfParseNode;
import org.jruby.truffle.parser.ast.InstAsgnParseNode;
import org.jruby.truffle.parser.ast.InstVarParseNode;
import org.jruby.truffle.parser.ast.IterParseNode;
import org.jruby.truffle.parser.ast.KeywordArgParseNode;
import org.jruby.truffle.parser.ast.KeywordRestArgParseNode;
import org.jruby.truffle.parser.ast.LambdaParseNode;
import org.jruby.truffle.parser.ast.ListParseNode;
import org.jruby.truffle.parser.ast.LiteralParseNode;
import org.jruby.truffle.parser.ast.LocalAsgnParseNode;
import org.jruby.truffle.parser.ast.LocalVarParseNode;
import org.jruby.truffle.parser.ast.Match2ParseNode;
import org.jruby.truffle.parser.ast.Match3ParseNode;
import org.jruby.truffle.parser.ast.MatchParseNode;
import org.jruby.truffle.parser.ast.ModuleParseNode;
import org.jruby.truffle.parser.ast.MultipleAsgnParseNode;
import org.jruby.truffle.parser.ast.NewlineParseNode;
import org.jruby.truffle.parser.ast.NextParseNode;
import org.jruby.truffle.parser.ast.NilParseNode;
import org.jruby.truffle.parser.ast.NthRefParseNode;
import org.jruby.truffle.parser.ast.OpAsgnAndParseNode;
import org.jruby.truffle.parser.ast.OpAsgnConstDeclParseNode;
import org.jruby.truffle.parser.ast.OpAsgnOrParseNode;
import org.jruby.truffle.parser.ast.OpAsgnParseNode;
import org.jruby.truffle.parser.ast.OpElementAsgnParseNode;
import org.jruby.truffle.parser.ast.OptArgParseNode;
import org.jruby.truffle.parser.ast.OrParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.PostExeParseNode;
import org.jruby.truffle.parser.ast.PreExeParseNode;
import org.jruby.truffle.parser.ast.RationalParseNode;
import org.jruby.truffle.parser.ast.RedoParseNode;
import org.jruby.truffle.parser.ast.RegexpParseNode;
import org.jruby.truffle.parser.ast.RequiredKeywordArgumentValueParseNode;
import org.jruby.truffle.parser.ast.RescueBodyParseNode;
import org.jruby.truffle.parser.ast.RescueParseNode;
import org.jruby.truffle.parser.ast.RestArgParseNode;
import org.jruby.truffle.parser.ast.RetryParseNode;
import org.jruby.truffle.parser.ast.ReturnParseNode;
import org.jruby.truffle.parser.ast.RootParseNode;
import org.jruby.truffle.parser.ast.SClassParseNode;
import org.jruby.truffle.parser.ast.SValueParseNode;
import org.jruby.truffle.parser.ast.SelfParseNode;
import org.jruby.truffle.parser.ast.SplatParseNode;
import org.jruby.truffle.parser.ast.StarParseNode;
import org.jruby.truffle.parser.ast.StrParseNode;
import org.jruby.truffle.parser.ast.SuperParseNode;
import org.jruby.truffle.parser.ast.SymbolParseNode;
import org.jruby.truffle.parser.ast.TrueParseNode;
import org.jruby.truffle.parser.ast.TruffleFragmentParseNode;
import org.jruby.truffle.parser.ast.UndefParseNode;
import org.jruby.truffle.parser.ast.UntilParseNode;
import org.jruby.truffle.parser.ast.VAliasParseNode;
import org.jruby.truffle.parser.ast.VCallParseNode;
import org.jruby.truffle.parser.ast.WhenParseNode;
import org.jruby.truffle.parser.ast.WhileParseNode;
import org.jruby.truffle.parser.ast.XStrParseNode;
import org.jruby.truffle.parser.ast.YieldParseNode;
import org.jruby.truffle.parser.ast.ZArrayParseNode;
import org.jruby.truffle.parser.ast.ZSuperParseNode;

/**
 * Visitor interface to be implemented by visitors of the jRuby AST.
 * each node will call the visit method appropriate to its type.
 * @see ParseNode
  *
 * @author Benoit Cerrina
 **/
public interface NodeVisitor<T> {
    T visitAliasNode(AliasParseNode iVisited);
    T visitAndNode(AndParseNode iVisited);
    T visitArgsNode(ArgsParseNode iVisited);
    T visitArgsCatNode(ArgsCatParseNode iVisited);
    T visitArgsPushNode(ArgsPushParseNode iVisited);
    T visitArgumentNode(ArgumentParseNode iVisited);
    T visitArrayNode(ArrayParseNode iVisited);
    T visitAttrAssignNode(AttrAssignParseNode iVisited);
    T visitBackRefNode(BackRefParseNode iVisited);
    T visitBeginNode(BeginParseNode iVisited);
    T visitBignumNode(BignumParseNode iVisited);
    T visitBlockArgNode(BlockArgParseNode iVisited);
    T visitBlockNode(BlockParseNode iVisited);
    T visitBlockPassNode(BlockPassParseNode iVisited);
    T visitBreakNode(BreakParseNode iVisited);
    T visitConstDeclNode(ConstDeclParseNode iVisited);
    T visitClassVarAsgnNode(ClassVarAsgnParseNode iVisited);
    T visitClassVarDeclNode(ClassVarDeclParseNode iVisited);
    T visitClassVarNode(ClassVarParseNode iVisited);
    T visitCallNode(CallParseNode iVisited);
    T visitCaseNode(CaseParseNode iVisited);
    T visitClassNode(ClassParseNode iVisited);
    T visitColon2Node(Colon2ParseNode iVisited);
    T visitColon3Node(Colon3ParseNode iVisited);
    T visitComplexNode(ComplexParseNode iVisited);
    T visitConstNode(ConstParseNode iVisited);
    T visitDAsgnNode(DAsgnParseNode iVisited);
    T visitDRegxNode(DRegexpParseNode iVisited);
    T visitDStrNode(DStrParseNode iVisited);
    T visitDSymbolNode(DSymbolParseNode iVisited);
    T visitDVarNode(DVarParseNode iVisited);
    T visitDXStrNode(DXStrParseNode iVisited);
    T visitDefinedNode(DefinedParseNode iVisited);
    T visitDefnNode(DefnParseNode iVisited);
    T visitDefsNode(DefsParseNode iVisited);
    T visitDotNode(DotParseNode iVisited);
    T visitEncodingNode(EncodingParseNode iVisited);
    T visitEnsureNode(EnsureParseNode iVisited);
    T visitEvStrNode(EvStrParseNode iVisited);
    T visitFCallNode(FCallParseNode iVisited);
    T visitFalseNode(FalseParseNode iVisited);
    T visitFixnumNode(FixnumParseNode iVisited);
    T visitFlipNode(FlipParseNode iVisited);
    T visitFloatNode(FloatParseNode iVisited);
    T visitForNode(ForParseNode iVisited);
    T visitGlobalAsgnNode(GlobalAsgnParseNode iVisited);
    T visitGlobalVarNode(GlobalVarParseNode iVisited);
    T visitHashNode(HashParseNode iVisited);
    T visitInstAsgnNode(InstAsgnParseNode iVisited);
    T visitInstVarNode(InstVarParseNode iVisited);
    T visitIfNode(IfParseNode iVisited);
    T visitIterNode(IterParseNode iVisited);
    T visitKeywordArgNode(KeywordArgParseNode iVisited);
    T visitKeywordRestArgNode(KeywordRestArgParseNode iVisited);
    T visitLambdaNode(LambdaParseNode iVisited);
    T visitListNode(ListParseNode iVisited);
    T visitLiteralNode(LiteralParseNode iVisited);
    T visitLocalAsgnNode(LocalAsgnParseNode iVisited);
    T visitLocalVarNode(LocalVarParseNode iVisited);
    T visitMultipleAsgnNode(MultipleAsgnParseNode iVisited);
    T visitMatch2Node(Match2ParseNode iVisited);
    T visitMatch3Node(Match3ParseNode iVisited);
    T visitMatchNode(MatchParseNode iVisited);
    T visitModuleNode(ModuleParseNode iVisited);
    T visitNewlineNode(NewlineParseNode iVisited);
    T visitNextNode(NextParseNode iVisited);
    T visitNilNode(NilParseNode iVisited);
    T visitNthRefNode(NthRefParseNode iVisited);
    T visitOpElementAsgnNode(OpElementAsgnParseNode iVisited);
    T visitOpAsgnNode(OpAsgnParseNode iVisited);
    T visitOpAsgnAndNode(OpAsgnAndParseNode iVisited);
    T visitOpAsgnConstDeclNode(OpAsgnConstDeclParseNode iVisited);
    T visitOpAsgnOrNode(OpAsgnOrParseNode iVisited);
    T visitOptArgNode(OptArgParseNode iVisited);
    T visitOrNode(OrParseNode iVisited);
    T visitPreExeNode(PreExeParseNode iVisited);
    T visitPostExeNode(PostExeParseNode iVisited);
    T visitRationalNode(RationalParseNode iVisited);
    T visitRedoNode(RedoParseNode iVisited);
    T visitRegexpNode(RegexpParseNode iVisited);
    T visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueParseNode iVisited);
    T visitRescueBodyNode(RescueBodyParseNode iVisited);
    T visitRescueNode(RescueParseNode iVisited);
    T visitRestArgNode(RestArgParseNode iVisited);
    T visitRetryNode(RetryParseNode iVisited);
    T visitReturnNode(ReturnParseNode iVisited);
    T visitRootNode(RootParseNode iVisited);
    T visitSClassNode(SClassParseNode iVisited);
    T visitSelfNode(SelfParseNode iVisited);
    T visitSplatNode(SplatParseNode iVisited);
    T visitStarNode(StarParseNode iVisited);
    T visitStrNode(StrParseNode iVisited);
    T visitSuperNode(SuperParseNode iVisited);
    T visitSValueNode(SValueParseNode iVisited);
    T visitSymbolNode(SymbolParseNode iVisited);
    T visitTrueNode(TrueParseNode iVisited);
    T visitUndefNode(UndefParseNode iVisited);
    T visitUntilNode(UntilParseNode iVisited);
    T visitVAliasNode(VAliasParseNode iVisited);
    T visitVCallNode(VCallParseNode iVisited);
    T visitWhenNode(WhenParseNode iVisited);
    T visitWhileNode(WhileParseNode iVisited);
    T visitXStrNode(XStrParseNode iVisited);
    T visitYieldNode(YieldParseNode iVisited);
    T visitZArrayNode(ZArrayParseNode iVisited);
    T visitZSuperNode(ZSuperParseNode iVisited);
    T visitTruffleFragmentNode(TruffleFragmentParseNode iVisited);
    T visitOther(ParseNode iVisited);
}
