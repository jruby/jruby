/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Abstract implementation of NodeVisitor that simply walks each node's
 * childNodes() in sequence and returns null from all visit methods.
 */
public class AbstractNodeVisitor implements NodeVisitor {    
    private Object defaultVisit(Node iVisited) {
        for (Node node : iVisited.childNodes()) node.accept(this);
        return null;
    }
    
    @Override
    public Object visitAliasNode(AliasNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitAndNode(AndNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitArgsNode(ArgsNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitArgsCatNode(ArgsCatNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitArgsPushNode(ArgsPushNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitArgumentNode(ArgumentNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitArrayNode(ArrayNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitAttrAssignNode(AttrAssignNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitBackRefNode(BackRefNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitBeginNode(BeginNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitBignumNode(BignumNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitBlockArgNode(BlockArgNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitBlockNode(BlockNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitBlockPassNode(BlockPassNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitBreakNode(BreakNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitComplexNode(ComplexNode iVisited) {
        return defaultVisit(iVisited);
    }
    
    @Override
    public Object visitConstDeclNode(ConstDeclNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitClassVarNode(ClassVarNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitCallNode(CallNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitCaseNode(CaseNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitClassNode(ClassNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitColon2Node(Colon2Node iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitColon3Node(Colon3Node iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitConstNode(ConstNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitDAsgnNode(DAsgnNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitDRegxNode(DRegexpNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitDStrNode(DStrNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitDSymbolNode(DSymbolNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitDVarNode(DVarNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitDXStrNode(DXStrNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitDefinedNode(DefinedNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitDefnNode(DefnNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitDefsNode(DefsNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitDotNode(DotNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitEncodingNode(EncodingNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitEnsureNode(EnsureNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitEvStrNode(EvStrNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitFCallNode(FCallNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitFalseNode(FalseNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitFixnumNode(FixnumNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitFlipNode(FlipNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitFloatNode(FloatNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitForNode(ForNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitGlobalVarNode(GlobalVarNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitHashNode(HashNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitInstAsgnNode(InstAsgnNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitInstVarNode(InstVarNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitIfNode(IfNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitIterNode(IterNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitKeywordArgNode(KeywordArgNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitKeywordRestArgNode(KeywordRestArgNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitLambdaNode(LambdaNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitListNode(ListNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitLiteralNode(LiteralNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitLocalAsgnNode(LocalAsgnNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitLocalVarNode(LocalVarNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitMultipleAsgnNode(MultipleAsgn19Node iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitMatch2Node(Match2Node iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitMatch3Node(Match3Node iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitMatchNode(MatchNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitModuleNode(ModuleNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitNewlineNode(NewlineNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitNextNode(NextNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitNilNode(NilNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitNthRefNode(NthRefNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitOpAsgnNode(OpAsgnNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitOptArgNode(OptArgNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitOrNode(OrNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitPreExeNode(PreExeNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitPostExeNode(PostExeNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitRationalNode(RationalNode iVisited) {
        return defaultVisit(iVisited);
    }
    
    @Override
    public Object visitRedoNode(RedoNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitRegexpNode(RegexpNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitRescueBodyNode(RescueBodyNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitRescueNode(RescueNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitRestArgNode(RestArgNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitRetryNode(RetryNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitReturnNode(ReturnNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitRootNode(RootNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitSClassNode(SClassNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitSelfNode(SelfNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitSplatNode(SplatNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitStrNode(StrNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitSuperNode(SuperNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitSValueNode(SValueNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitSymbolNode(SymbolNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitToAryNode(ToAryNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitTrueNode(TrueNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitUndefNode(UndefNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitUntilNode(UntilNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitVAliasNode(VAliasNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitVCallNode(VCallNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitWhenNode(WhenNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitWhileNode(WhileNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitXStrNode(XStrNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitYieldNode(YieldNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitZArrayNode(ZArrayNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitZSuperNode(ZSuperNode iVisited) {
        return defaultVisit(iVisited);
    }
    
}
