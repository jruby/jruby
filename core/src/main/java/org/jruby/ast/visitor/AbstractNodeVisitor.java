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

import java.util.HashSet;
import java.util.Set;
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
import org.jruby.ast.Node;
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
 * Abstract implementation of NodeVisitor that simply walks each node's
 * childNodes() in sequence and returns null from all visit methods.
 */
public class AbstractNodeVisitor implements NodeVisitor {
    Set<String> foundVariables = new HashSet<String>();
    
    public Set<String> getFoundVariables() {
        return foundVariables;
    }
    
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
    public Object visitBlockArg18Node(BlockArg18Node iVisited) {
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
    public Object visitNotNode(NotNode iVisited) {
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
    public Object visitRedoNode(RedoNode iVisited) {
        return defaultVisit(iVisited);
    }

    @Override
    public Object visitRegexpNode(RegexpNode iVisited) {
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
