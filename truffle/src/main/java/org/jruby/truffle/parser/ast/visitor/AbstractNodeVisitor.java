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
 * An abstract implementation of {@link NodeVisitor} that has an abstract default visit method, and provides a utility
 * method to visit children.
 */
public abstract class AbstractNodeVisitor<T> implements NodeVisitor<T> {

    abstract protected T defaultVisit(ParseNode node);

    protected void visitChildren(ParseNode node) {
        for (ParseNode child: node.childNodes()) {
            if (child != null) {
                child.accept(this);
            }
        }
    }

    protected T visitFirstChild(ParseNode node) {
        for (ParseNode child: node.childNodes()) {
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
    public T visitAliasNode(AliasParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitAndNode(AndParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitArgsNode(ArgsParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitArgsCatNode(ArgsCatParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitArgsPushNode(ArgsPushParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitArgumentNode(ArgumentParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitArrayNode(ArrayParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitAttrAssignNode(AttrAssignParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBackRefNode(BackRefParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBeginNode(BeginParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBignumNode(BignumParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBlockArgNode(BlockArgParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBlockNode(BlockParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBlockPassNode(BlockPassParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitBreakNode(BreakParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitConstDeclNode(ConstDeclParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitClassVarAsgnNode(ClassVarAsgnParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitClassVarDeclNode(ClassVarDeclParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitClassVarNode(ClassVarParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitCallNode(CallParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitCaseNode(CaseParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitClassNode(ClassParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitColon2Node(Colon2ParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitColon3Node(Colon3ParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitComplexNode(ComplexParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitConstNode(ConstParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDAsgnNode(DAsgnParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDRegxNode(DRegexpParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDStrNode(DStrParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDSymbolNode(DSymbolParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDVarNode(DVarParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDXStrNode(DXStrParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDefinedNode(DefinedParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDefnNode(DefnParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDefsNode(DefsParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitDotNode(DotParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitEncodingNode(EncodingParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitEnsureNode(EnsureParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitEvStrNode(EvStrParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitFCallNode(FCallParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitFalseNode(FalseParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitFixnumNode(FixnumParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitFlipNode(FlipParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitFloatNode(FloatParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitForNode(ForParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitGlobalAsgnNode(GlobalAsgnParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitGlobalVarNode(GlobalVarParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitHashNode(HashParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitInstAsgnNode(InstAsgnParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitInstVarNode(InstVarParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitIfNode(IfParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitIterNode(IterParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitKeywordArgNode(KeywordArgParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitKeywordRestArgNode(KeywordRestArgParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitLambdaNode(LambdaParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitListNode(ListParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitLiteralNode(LiteralParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitLocalAsgnNode(LocalAsgnParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitLocalVarNode(LocalVarParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitMultipleAsgnNode(MultipleAsgnParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitMatch2Node(Match2ParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitMatch3Node(Match3ParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitMatchNode(MatchParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitModuleNode(ModuleParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitNewlineNode(NewlineParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitNextNode(NextParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitNilNode(NilParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitNthRefNode(NthRefParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOpElementAsgnNode(OpElementAsgnParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOpAsgnNode(OpAsgnParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOpAsgnAndNode(OpAsgnAndParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOpAsgnConstDeclNode(OpAsgnConstDeclParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOpAsgnOrNode(OpAsgnOrParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOptArgNode(OptArgParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOrNode(OrParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitPreExeNode(PreExeParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitPostExeNode(PostExeParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRationalNode(RationalParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRedoNode(RedoParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRegexpNode(RegexpParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRescueBodyNode(RescueBodyParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRescueNode(RescueParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRestArgNode(RestArgParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRetryNode(RetryParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitReturnNode(ReturnParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitRootNode(RootParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSClassNode(SClassParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSelfNode(SelfParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSplatNode(SplatParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitStarNode(StarParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitStrNode(StrParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSuperNode(SuperParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSValueNode(SValueParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitSymbolNode(SymbolParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitTrueNode(TrueParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitUndefNode(UndefParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitUntilNode(UntilParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitVAliasNode(VAliasParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitVCallNode(VCallParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitWhenNode(WhenParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitWhileNode(WhileParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitXStrNode(XStrParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitYieldNode(YieldParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitZArrayNode(ZArrayParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitZSuperNode(ZSuperParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitTruffleFragmentNode(TruffleFragmentParseNode node) {
        return defaultVisit(node);
    }

    @Override
    public T visitOther(ParseNode node) {
        return defaultVisit(node);
    }

}
