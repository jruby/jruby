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
 * An abstract implementation of {@link NodeVisitor} that has an abstract default visit method, and provides a utility
 * method to visit children.
 */
public abstract class AbstractNodeVisitor implements NodeVisitor {

    abstract protected Object defaultVisit(Node node);

    protected void visitChildren(Node node) {
        for (Node child: node.childNodes()) {
            if (child != null) {
                child.accept(this);
            }
        }
    }

    @Override
    public Object visitAliasNode(AliasNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitAndNode(AndNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitArgsNode(ArgsNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitArgsCatNode(ArgsCatNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitArgsPushNode(ArgsPushNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitArgumentNode(ArgumentNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitArrayNode(ArrayNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitAttrAssignNode(AttrAssignNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitBackRefNode(BackRefNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitBeginNode(BeginNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitBignumNode(BignumNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitBlockArgNode(BlockArgNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitBlockNode(BlockNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitBlockPassNode(BlockPassNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitBreakNode(BreakNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitConstDeclNode(ConstDeclNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitClassVarAsgnNode(ClassVarAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitClassVarDeclNode(ClassVarDeclNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitClassVarNode(ClassVarNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitCallNode(CallNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitCaseNode(CaseNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitClassNode(ClassNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitColon2Node(Colon2Node node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitColon3Node(Colon3Node node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitComplexNode(ComplexNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitConstNode(ConstNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitDAsgnNode(DAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitDRegxNode(DRegexpNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitDStrNode(DStrNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitDSymbolNode(DSymbolNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitDVarNode(DVarNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitDXStrNode(DXStrNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitDefinedNode(DefinedNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitDefnNode(DefnNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitDefsNode(DefsNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitDotNode(DotNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitEncodingNode(EncodingNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitEnsureNode(EnsureNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitEvStrNode(EvStrNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitFCallNode(FCallNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitFalseNode(FalseNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitFixnumNode(FixnumNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitFlipNode(FlipNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitFloatNode(FloatNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitForNode(ForNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitGlobalAsgnNode(GlobalAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitGlobalVarNode(GlobalVarNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitHashNode(HashNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitInstAsgnNode(InstAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitInstVarNode(InstVarNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitIfNode(IfNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitIterNode(IterNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitKeywordArgNode(KeywordArgNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitKeywordRestArgNode(KeywordRestArgNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitLambdaNode(LambdaNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitListNode(ListNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitLiteralNode(LiteralNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitLocalAsgnNode(LocalAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitLocalVarNode(LocalVarNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitMultipleAsgnNode(MultipleAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitMultipleAsgnNode(MultipleAsgn19Node node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitMatch2Node(Match2Node node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitMatch3Node(Match3Node node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitMatchNode(MatchNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitModuleNode(ModuleNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitNewlineNode(NewlineNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitNextNode(NextNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitNilNode(NilNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitNthRefNode(NthRefNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitOpElementAsgnNode(OpElementAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitOpAsgnNode(OpAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitOpAsgnAndNode(OpAsgnAndNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitOpAsgnOrNode(OpAsgnOrNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitOptArgNode(OptArgNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitOrNode(OrNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitPreExeNode(PreExeNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitPostExeNode(PostExeNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitRationalNode(RationalNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitRedoNode(RedoNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitRegexpNode(RegexpNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitRescueBodyNode(RescueBodyNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitRescueNode(RescueNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitRestArgNode(RestArgNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitRetryNode(RetryNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitReturnNode(ReturnNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitRootNode(RootNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitSClassNode(SClassNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitSelfNode(SelfNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitSplatNode(SplatNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitStrNode(StrNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitSuperNode(SuperNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitSValueNode(SValueNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitSymbolNode(SymbolNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitToAryNode(ToAryNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitTrueNode(TrueNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitUndefNode(UndefNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitUntilNode(UntilNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitVAliasNode(VAliasNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitVCallNode(VCallNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitWhenNode(WhenNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitWhileNode(WhileNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitXStrNode(XStrNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitYieldNode(YieldNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitZArrayNode(ZArrayNode node) {
        return defaultVisit(node);
    }

    @Override
    public Object visitZSuperNode(ZSuperNode node) {
        return defaultVisit(node);
    }

}
