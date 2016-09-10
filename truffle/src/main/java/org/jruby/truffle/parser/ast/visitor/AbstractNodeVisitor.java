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
package org.jruby.truffle.parser.ast.visitor;

import org.jruby.truffle.parser.ast.AliasNode;
import org.jruby.truffle.parser.ast.AndNode;
import org.jruby.truffle.parser.ast.ArgsCatNode;
import org.jruby.truffle.parser.ast.ArgsNode;
import org.jruby.truffle.parser.ast.ArgsPushNode;
import org.jruby.truffle.parser.ast.ArgumentNode;
import org.jruby.truffle.parser.ast.ArrayNode;
import org.jruby.truffle.parser.ast.AttrAssignNode;
import org.jruby.truffle.parser.ast.BackRefNode;
import org.jruby.truffle.parser.ast.BeginNode;
import org.jruby.truffle.parser.ast.BignumNode;
import org.jruby.truffle.parser.ast.BlockArgNode;
import org.jruby.truffle.parser.ast.BlockNode;
import org.jruby.truffle.parser.ast.BlockPassNode;
import org.jruby.truffle.parser.ast.BreakNode;
import org.jruby.truffle.parser.ast.CallNode;
import org.jruby.truffle.parser.ast.CaseNode;
import org.jruby.truffle.parser.ast.ClassNode;
import org.jruby.truffle.parser.ast.ClassVarAsgnNode;
import org.jruby.truffle.parser.ast.ClassVarDeclNode;
import org.jruby.truffle.parser.ast.ClassVarNode;
import org.jruby.truffle.parser.ast.Colon2Node;
import org.jruby.truffle.parser.ast.Colon3Node;
import org.jruby.truffle.parser.ast.ComplexNode;
import org.jruby.truffle.parser.ast.ConstDeclNode;
import org.jruby.truffle.parser.ast.ConstNode;
import org.jruby.truffle.parser.ast.DAsgnNode;
import org.jruby.truffle.parser.ast.DRegexpNode;
import org.jruby.truffle.parser.ast.DStrNode;
import org.jruby.truffle.parser.ast.DSymbolNode;
import org.jruby.truffle.parser.ast.DVarNode;
import org.jruby.truffle.parser.ast.DXStrNode;
import org.jruby.truffle.parser.ast.DefinedNode;
import org.jruby.truffle.parser.ast.DefnNode;
import org.jruby.truffle.parser.ast.DefsNode;
import org.jruby.truffle.parser.ast.DotNode;
import org.jruby.truffle.parser.ast.EncodingNode;
import org.jruby.truffle.parser.ast.EnsureNode;
import org.jruby.truffle.parser.ast.EvStrNode;
import org.jruby.truffle.parser.ast.FCallNode;
import org.jruby.truffle.parser.ast.FalseNode;
import org.jruby.truffle.parser.ast.FixnumNode;
import org.jruby.truffle.parser.ast.FlipNode;
import org.jruby.truffle.parser.ast.FloatNode;
import org.jruby.truffle.parser.ast.ForNode;
import org.jruby.truffle.parser.ast.GlobalAsgnNode;
import org.jruby.truffle.parser.ast.GlobalVarNode;
import org.jruby.truffle.parser.ast.HashNode;
import org.jruby.truffle.parser.ast.IfNode;
import org.jruby.truffle.parser.ast.InstAsgnNode;
import org.jruby.truffle.parser.ast.InstVarNode;
import org.jruby.truffle.parser.ast.IterNode;
import org.jruby.truffle.parser.ast.KeywordArgNode;
import org.jruby.truffle.parser.ast.KeywordRestArgNode;
import org.jruby.truffle.parser.ast.LambdaNode;
import org.jruby.truffle.parser.ast.ListNode;
import org.jruby.truffle.parser.ast.LiteralNode;
import org.jruby.truffle.parser.ast.LocalAsgnNode;
import org.jruby.truffle.parser.ast.LocalVarNode;
import org.jruby.truffle.parser.ast.Match2Node;
import org.jruby.truffle.parser.ast.Match3Node;
import org.jruby.truffle.parser.ast.MatchNode;
import org.jruby.truffle.parser.ast.ModuleNode;
import org.jruby.truffle.parser.ast.MultipleAsgnNode;
import org.jruby.truffle.parser.ast.NewlineNode;
import org.jruby.truffle.parser.ast.NextNode;
import org.jruby.truffle.parser.ast.NilNode;
import org.jruby.truffle.parser.ast.Node;
import org.jruby.truffle.parser.ast.NthRefNode;
import org.jruby.truffle.parser.ast.OpAsgnAndNode;
import org.jruby.truffle.parser.ast.OpAsgnConstDeclNode;
import org.jruby.truffle.parser.ast.OpAsgnNode;
import org.jruby.truffle.parser.ast.OpAsgnOrNode;
import org.jruby.truffle.parser.ast.OpElementAsgnNode;
import org.jruby.truffle.parser.ast.OptArgNode;
import org.jruby.truffle.parser.ast.OrNode;
import org.jruby.truffle.parser.ast.PostExeNode;
import org.jruby.truffle.parser.ast.PreExeNode;
import org.jruby.truffle.parser.ast.RationalNode;
import org.jruby.truffle.parser.ast.RedoNode;
import org.jruby.truffle.parser.ast.RegexpNode;
import org.jruby.truffle.parser.ast.RequiredKeywordArgumentValueNode;
import org.jruby.truffle.parser.ast.RescueBodyNode;
import org.jruby.truffle.parser.ast.RescueNode;
import org.jruby.truffle.parser.ast.RestArgNode;
import org.jruby.truffle.parser.ast.RetryNode;
import org.jruby.truffle.parser.ast.ReturnNode;
import org.jruby.truffle.parser.ast.RootNode;
import org.jruby.truffle.parser.ast.SClassNode;
import org.jruby.truffle.parser.ast.SValueNode;
import org.jruby.truffle.parser.ast.SelfNode;
import org.jruby.truffle.parser.ast.SplatNode;
import org.jruby.truffle.parser.ast.StarNode;
import org.jruby.truffle.parser.ast.StrNode;
import org.jruby.truffle.parser.ast.SuperNode;
import org.jruby.truffle.parser.ast.SymbolNode;
import org.jruby.truffle.parser.ast.TrueNode;
import org.jruby.truffle.parser.ast.UndefNode;
import org.jruby.truffle.parser.ast.UntilNode;
import org.jruby.truffle.parser.ast.VAliasNode;
import org.jruby.truffle.parser.ast.VCallNode;
import org.jruby.truffle.parser.ast.WhenNode;
import org.jruby.truffle.parser.ast.WhileNode;
import org.jruby.truffle.parser.ast.XStrNode;
import org.jruby.truffle.parser.ast.YieldNode;
import org.jruby.truffle.parser.ast.ZArrayNode;
import org.jruby.truffle.parser.ast.ZSuperNode;

/**
 * An abstract implementation of {@link NodeVisitor} that has an abstract default visit method, and provides a utility
 * method to visit children.
 */
public abstract class AbstractNodeVisitor<T> implements NodeVisitor<T> {

    abstract protected T defaultVisit(Node node);

    protected void visitChildren(Node node) {
        for (Node child: node.childNodes()) {
            if (child != null) {
                child.accept(this);
            }
        }
    }

    protected T visitFirstChild(Node node) {
        for (Node child: node.childNodes()) {
            if (child != null) {
                final T result = child.accept(this);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    @Override
    public T visitAliasNode(AliasNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitAndNode(AndNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitArgsNode(ArgsNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitArgsCatNode(ArgsCatNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitArgsPushNode(ArgsPushNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitArgumentNode(ArgumentNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitArrayNode(ArrayNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitAttrAssignNode(AttrAssignNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBackRefNode(BackRefNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBeginNode(BeginNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBignumNode(BignumNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBlockArgNode(BlockArgNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBlockNode(BlockNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBlockPassNode(BlockPassNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBreakNode(BreakNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitConstDeclNode(ConstDeclNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitClassVarAsgnNode(ClassVarAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitClassVarDeclNode(ClassVarDeclNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitClassVarNode(ClassVarNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitCallNode(CallNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitCaseNode(CaseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitClassNode(ClassNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitColon2Node(Colon2Node node) {
        return defaultVisit(node);
    }

    @Override
    public T visitColon3Node(Colon3Node node) {
        return defaultVisit(node);
    }

    @Override
    public T visitComplexNode(ComplexNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitConstNode(ConstNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDAsgnNode(DAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDRegxNode(DRegexpNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDStrNode(DStrNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDSymbolNode(DSymbolNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDVarNode(DVarNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDXStrNode(DXStrNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDefinedNode(DefinedNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDefnNode(DefnNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDefsNode(DefsNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDotNode(DotNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitEncodingNode(EncodingNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitEnsureNode(EnsureNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitEvStrNode(EvStrNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitFCallNode(FCallNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitFalseNode(FalseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitFixnumNode(FixnumNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitFlipNode(FlipNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitFloatNode(FloatNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitForNode(ForNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitGlobalAsgnNode(GlobalAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitGlobalVarNode(GlobalVarNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitHashNode(HashNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitInstAsgnNode(InstAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitInstVarNode(InstVarNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitIfNode(IfNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitIterNode(IterNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitKeywordArgNode(KeywordArgNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitKeywordRestArgNode(KeywordRestArgNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitLambdaNode(LambdaNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitListNode(ListNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitLiteralNode(LiteralNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitLocalAsgnNode(LocalAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitLocalVarNode(LocalVarNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitMultipleAsgnNode(MultipleAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitMatch2Node(Match2Node node) {
        return defaultVisit(node);
    }

    @Override
    public T visitMatch3Node(Match3Node node) {
        return defaultVisit(node);
    }

    @Override
    public T visitMatchNode(MatchNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitModuleNode(ModuleNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitNewlineNode(NewlineNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitNextNode(NextNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitNilNode(NilNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitNthRefNode(NthRefNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOpElementAsgnNode(OpElementAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOpAsgnNode(OpAsgnNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOpAsgnAndNode(OpAsgnAndNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOpAsgnConstDeclNode(OpAsgnConstDeclNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOpAsgnOrNode(OpAsgnOrNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOptArgNode(OptArgNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOrNode(OrNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitPreExeNode(PreExeNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitPostExeNode(PostExeNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRationalNode(RationalNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRedoNode(RedoNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRegexpNode(RegexpNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRescueBodyNode(RescueBodyNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRescueNode(RescueNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRestArgNode(RestArgNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRetryNode(RetryNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitReturnNode(ReturnNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRootNode(RootNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSClassNode(SClassNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSelfNode(SelfNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSplatNode(SplatNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitStarNode(StarNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitStrNode(StrNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSuperNode(SuperNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSValueNode(SValueNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSymbolNode(SymbolNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitTrueNode(TrueNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitUndefNode(UndefNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitUntilNode(UntilNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitVAliasNode(VAliasNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitVCallNode(VCallNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitWhenNode(WhenNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitWhileNode(WhileNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitXStrNode(XStrNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitYieldNode(YieldNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitZArrayNode(ZArrayNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitZSuperNode(ZSuperNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOther(Node node) {
        return defaultVisit(node);
    }

}
