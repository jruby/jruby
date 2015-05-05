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
public interface NodeVisitor<T> {
    public T visitAliasNode(AliasNode iVisited);
    public T visitAndNode(AndNode iVisited);
    public T visitArgsNode(ArgsNode iVisited);
    public T visitArgsCatNode(ArgsCatNode iVisited);
    public T visitArgsPushNode(ArgsPushNode iVisited);
    public T visitArgumentNode(ArgumentNode iVisited);
    public T visitArrayNode(ArrayNode iVisited);
    public T visitAttrAssignNode(AttrAssignNode iVisited);
    public T visitBackRefNode(BackRefNode iVisited);
    public T visitBeginNode(BeginNode iVisited);
    public T visitBignumNode(BignumNode iVisited);
    public T visitBlockArgNode(BlockArgNode iVisited);
    public T visitBlockNode(BlockNode iVisited);
    public T visitBlockPassNode(BlockPassNode iVisited);
    public T visitBreakNode(BreakNode iVisited);
    public T visitConstDeclNode(ConstDeclNode iVisited);
    public T visitClassVarAsgnNode(ClassVarAsgnNode iVisited);
    public T visitClassVarDeclNode(ClassVarDeclNode iVisited);
    public T visitClassVarNode(ClassVarNode iVisited);
    public T visitCallNode(CallNode iVisited);
    public T visitCaseNode(CaseNode iVisited);
    public T visitClassNode(ClassNode iVisited);
    public T visitColon2Node(Colon2Node iVisited);
    public T visitColon3Node(Colon3Node iVisited);
    public T visitComplexNode(ComplexNode iVisited);
    public T visitConstNode(ConstNode iVisited);
    public T visitDAsgnNode(DAsgnNode iVisited);
    public T visitDRegxNode(DRegexpNode iVisited);
    public T visitDStrNode(DStrNode iVisited);
    public T visitDSymbolNode(DSymbolNode iVisited);
    public T visitDVarNode(DVarNode iVisited);
    public T visitDXStrNode(DXStrNode iVisited);
    public T visitDefinedNode(DefinedNode iVisited);
    public T visitDefnNode(DefnNode iVisited);
    public T visitDefsNode(DefsNode iVisited);
    public T visitDotNode(DotNode iVisited);
    public T visitEncodingNode(EncodingNode iVisited);
    public T visitEnsureNode(EnsureNode iVisited);
    public T visitEvStrNode(EvStrNode iVisited);
    public T visitFCallNode(FCallNode iVisited);
    public T visitFalseNode(FalseNode iVisited);
    public T visitFixnumNode(FixnumNode iVisited);
    public T visitFlipNode(FlipNode iVisited);
    public T visitFloatNode(FloatNode iVisited);
    public T visitForNode(ForNode iVisited);
    public T visitGlobalAsgnNode(GlobalAsgnNode iVisited);
    public T visitGlobalVarNode(GlobalVarNode iVisited);
    public T visitHashNode(HashNode iVisited);
    public T visitInstAsgnNode(InstAsgnNode iVisited);
    public T visitInstVarNode(InstVarNode iVisited);
    public T visitIfNode(IfNode iVisited);
    public T visitIterNode(IterNode iVisited);
    public T visitKeywordArgNode(KeywordArgNode iVisited);
    public T visitKeywordRestArgNode(KeywordRestArgNode iVisited);
    public T visitLambdaNode(LambdaNode iVisited);
    public T visitListNode(ListNode iVisited);
    public T visitLiteralNode(LiteralNode iVisited);
    public T visitLocalAsgnNode(LocalAsgnNode iVisited);
    public T visitLocalVarNode(LocalVarNode iVisited);
    public T visitMultipleAsgnNode(MultipleAsgnNode iVisited);
    public T visitMatch2Node(Match2Node iVisited);
    public T visitMatch3Node(Match3Node iVisited);
    public T visitMatchNode(MatchNode iVisited);
    public T visitModuleNode(ModuleNode iVisited);
    public T visitNewlineNode(NewlineNode iVisited);
    public T visitNextNode(NextNode iVisited);
    public T visitNilNode(NilNode iVisited);
    public T visitNthRefNode(NthRefNode iVisited);
    public T visitOpElementAsgnNode(OpElementAsgnNode iVisited);
    public T visitOpAsgnNode(OpAsgnNode iVisited);
    public T visitOpAsgnAndNode(OpAsgnAndNode iVisited);
    public T visitOpAsgnOrNode(OpAsgnOrNode iVisited);
    public T visitOptArgNode(OptArgNode iVisited);
    public T visitOrNode(OrNode iVisited);
    public T visitPreExeNode(PreExeNode iVisited);
    public T visitPostExeNode(PostExeNode iVisited);
    public T visitRationalNode(RationalNode iVisited);
    public T visitRedoNode(RedoNode iVisited);
    public T visitRegexpNode(RegexpNode iVisited);
    public T visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueNode iVisited);
    public T visitRescueBodyNode(RescueBodyNode iVisited);
    public T visitRescueNode(RescueNode iVisited);
    public T visitRestArgNode(RestArgNode iVisited);
    public T visitRetryNode(RetryNode iVisited);
    public T visitReturnNode(ReturnNode iVisited);
    public T visitRootNode(RootNode iVisited);
    public T visitSClassNode(SClassNode iVisited);
    public T visitSelfNode(SelfNode iVisited);
    public T visitSplatNode(SplatNode iVisited);
    public T visitStrNode(StrNode iVisited);
    public T visitSuperNode(SuperNode iVisited);
    public T visitSValueNode(SValueNode iVisited);
    public T visitSymbolNode(SymbolNode iVisited);
    public T visitTrueNode(TrueNode iVisited);
    public T visitUndefNode(UndefNode iVisited);
    public T visitUntilNode(UntilNode iVisited);
    public T visitVAliasNode(VAliasNode iVisited);
    public T visitVCallNode(VCallNode iVisited);
    public T visitWhenNode(WhenNode iVisited);
    public T visitWhileNode(WhileNode iVisited);
    public T visitXStrNode(XStrNode iVisited);
    public T visitYieldNode(YieldNode iVisited);
    public T visitZArrayNode(ZArrayNode iVisited);
    public T visitZSuperNode(ZSuperNode iVisited);
    public T visitOther(Node iVisited);
}
