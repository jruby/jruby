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

import org.jruby.ast.*;

/**
 * Visitor interface to be implemented by visitors of the jRuby AST.
 * each node will call the visit method appropriate to its type.
 * @see org.jruby.ast.Node
  *
 * @author Benoit Cerrina
 **/
public interface NodeVisitor {
    public Object visitAliasNode(AliasNode iVisited);
    public Object visitAndNode(AndNode iVisited);
    public Object visitArgsNode(ArgsNode iVisited);
    public Object visitArgsCatNode(ArgsCatNode iVisited);
    public Object visitArgsPushNode(ArgsPushNode iVisited);
    public Object visitArgumentNode(ArgumentNode iVisited);
    public Object visitArrayNode(ArrayNode iVisited);
    public Object visitAttrAssignNode(AttrAssignNode iVisited);
    public Object visitBackRefNode(BackRefNode iVisited);
    public Object visitBeginNode(BeginNode iVisited);
    public Object visitBignumNode(BignumNode iVisited);
    public Object visitBlockArgNode(BlockArgNode iVisited);
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
    public Object visitComplexNode(ComplexNode iVisited);
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
    public Object visitNthRefNode(NthRefNode iVisited);
    public Object visitOpElementAsgnNode(OpElementAsgnNode iVisited);
    public Object visitOpAsgnNode(OpAsgnNode iVisited);
    public Object visitOpAsgnAndNode(OpAsgnAndNode iVisited);
    public Object visitOpAsgnOrNode(OpAsgnOrNode iVisited);
    public Object visitOptArgNode(OptArgNode iVisited);
    public Object visitOrNode(OrNode iVisited);
    public Object visitPreExeNode(PreExeNode iVisited);
    public Object visitPostExeNode(PostExeNode iVisited);
    public Object visitRationalNode(RationalNode iVisited);
    public Object visitRedoNode(RedoNode iVisited);
    public Object visitRegexpNode(RegexpNode iVisited);
    public Object visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueNode iVisited);
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
