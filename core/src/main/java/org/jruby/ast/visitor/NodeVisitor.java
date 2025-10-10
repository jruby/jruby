/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import org.jruby.ast.*;

/**
 * Visitor interface to be implemented by visitors of the jRuby AST.
 * each node will call the visit method appropriate to its type.
 * @see org.jruby.ast.Node
 *
 * @param <T> the return value of visit
 * @author Benoit Cerrina
 **/
public interface NodeVisitor<T> {
    T visitAliasNode(AliasNode iVisited);
    T visitAndNode(AndNode iVisited);
    T visitArgsNode(ArgsNode iVisited);
    T visitArgsCatNode(ArgsCatNode iVisited);
    T visitArgsPushNode(ArgsPushNode iVisited);
    T visitArgumentNode(ArgumentNode iVisited);
    T visitArrayNode(ArrayNode iVisited);
    T visitArrayPatternNode(ArrayPatternNode iVisited);
    T visitAttrAssignNode(AttrAssignNode iVisited);
    T visitBackRefNode(BackRefNode iVisited);
    T visitBeginNode(BeginNode iVisited);
    T visitBignumNode(BignumNode iVisited);
    T visitBlockArgNode(BlockArgNode iVisited);
    T visitBlockNode(BlockNode iVisited);
    T visitBlockPassNode(BlockPassNode iVisited);
    T visitBreakNode(BreakNode iVisited);
    T visitConstDeclNode(ConstDeclNode iVisited);
    T visitClassVarAsgnNode(ClassVarAsgnNode iVisited);
    T visitClassVarNode(ClassVarNode iVisited);
    T visitCallNode(CallNode iVisited);
    T visitCaseNode(CaseNode iVisited);
    T visitClassNode(ClassNode iVisited);
    T visitColon2Node(Colon2Node iVisited);
    T visitColon3Node(Colon3Node iVisited);
    T visitComplexNode(ComplexNode iVisited);
    T visitConstNode(ConstNode iVisited);
    T visitDAsgnNode(DAsgnNode iVisited);
    T visitDRegxNode(DRegexpNode iVisited);
    T visitDStrNode(DStrNode iVisited);
    T visitDSymbolNode(DSymbolNode iVisited);
    T visitDVarNode(DVarNode iVisited);
    T visitDXStrNode(DXStrNode iVisited);
    T visitDefinedNode(DefinedNode iVisited);
    T visitDefnNode(DefnNode iVisited);
    T visitDefsNode(DefsNode iVisited);
    T visitDotNode(DotNode iVisited);
    T visitEncodingNode(EncodingNode iVisited);
    T visitEnsureNode(EnsureNode iVisited);
    T visitErrorNode(ErrorNode iVisited);
    T visitEvStrNode(EvStrNode iVisited);
    T visitFCallNode(FCallNode iVisited);
    T visitFalseNode(FalseNode iVisited);
    T visitFindPatternNode(FindPatternNode iVisited);
    T visitFixnumNode(FixnumNode iVisited);
    T visitFlipNode(FlipNode iVisited);
    T visitFloatNode(FloatNode iVisited);
    T visitForNode(ForNode iVisited);
    T visitGlobalAsgnNode(GlobalAsgnNode iVisited);
    T visitGlobalVarNode(GlobalVarNode iVisited);
    T visitHashNode(HashNode iVisited);
    T visitHashPatternNode(HashPatternNode iVisited);
    T visitInNode(InNode iVisited);
    T visitInstAsgnNode(InstAsgnNode iVisited);
    T visitInstVarNode(InstVarNode iVisited);
    T visitIfNode(IfNode iVisited);
    T visitIterNode(IterNode iVisited);
    T visitKeywordArgNode(KeywordArgNode iVisited);
    T visitKeywordRestArgNode(KeywordRestArgNode iVisited);
    T visitLambdaNode(LambdaNode iVisited);
    T visitListNode(ListNode iVisited);
    T visitLiteralNode(LiteralNode iVisited);
    T visitLocalAsgnNode(LocalAsgnNode iVisited);
    T visitLocalVarNode(LocalVarNode iVisited);
    T visitMultipleAsgnNode(MultipleAsgnNode iVisited);
    T visitMatch2Node(Match2Node iVisited);
    T visitMatch3Node(Match3Node iVisited);
    T visitMatchNode(MatchNode iVisited);
    T visitModuleNode(ModuleNode iVisited);
    T visitNewlineNode(NewlineNode iVisited);
    T visitNextNode(NextNode iVisited);
    T visitNilNode(NilNode iVisited);
    T visitNilRestArgNode(NilRestArgNode iVisited);
    T visitNthRefNode(NthRefNode iVisited);
    T visitOperatorCallNode(OperatorCallNode iVisited);
    T visitOpElementAsgnNode(OpElementAsgnNode iVisited);
    T visitOpAsgnNode(OpAsgnNode iVisited);
    T visitOpAsgnAndNode(OpAsgnAndNode iVisited);
    T visitOpAsgnConstDeclNode(OpAsgnConstDeclNode iVisited);
    T visitOpAsgnOrNode(OpAsgnOrNode iVisited);
    T visitOptArgNode(OptArgNode iVisited);
    T visitOrNode(OrNode iVisited);
    T visitPatternCaseNode(PatternCaseNode iVisited);
    T visitPreExeNode(PreExeNode iVisited);
    T visitPostExeNode(PostExeNode iVisited);
    T visitRationalNode(RationalNode iVisited);
    T visitRedoNode(RedoNode iVisited);
    T visitRegexpNode(RegexpNode iVisited);
    T visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueNode iVisited);
    T visitRescueBodyNode(RescueBodyNode iVisited);
    T visitRescueNode(RescueNode iVisited);
    T visitRestArgNode(RestArgNode iVisited);
    T visitRetryNode(RetryNode iVisited);
    T visitReturnNode(ReturnNode iVisited);
    T visitRootNode(RootNode iVisited);
    T visitSClassNode(SClassNode iVisited);
    T visitSelfNode(SelfNode iVisited);
    T visitSplatNode(SplatNode iVisited);
    T visitStarNode(StarNode iVisited);
    T visitStrNode(StrNode iVisited);
    T visitSuperNode(SuperNode iVisited);
    T visitSValueNode(SValueNode iVisited);
    T visitSymbolNode(SymbolNode iVisited);
    T visitTrueNode(TrueNode iVisited);
    T visitUndefNode(UndefNode iVisited);
    T visitUntilNode(UntilNode iVisited);
    T visitVAliasNode(VAliasNode iVisited);
    T visitVCallNode(VCallNode iVisited);
    T visitWhenNode(WhenNode iVisited);
    T visitWhileNode(WhileNode iVisited);
    T visitXStrNode(XStrNode iVisited);
    T visitYieldNode(YieldNode iVisited);
    T visitZArrayNode(ZArrayNode iVisited);
    T visitZSuperNode(ZSuperNode iVisited);
    T visitOther(Node iVisited);

    @Deprecated(since = "9.2.0.0")
    default T visitClassVarDeclNode(ClassVarDeclNode iVisited) { return null; }
}
