/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast.visitor;

import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
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
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
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
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
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
import org.jruby.evaluator.SingleNodeVisitor;

/** This visitor calls by default the return visitNode method for each visited Node.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class AbstractVisitor implements NodeVisitor {
    
    /**
     * This method is called by default for each visited Node.
     */
    protected abstract SingleNodeVisitor visitNode(Node iVisited);

    public SingleNodeVisitor visitNullNode() {
        return visitNode(null);
    }
    
    public SingleNodeVisitor acceptNode(Node node) {
        if (node == null) {
            return visitNullNode();
        } else {
        	return node.accept(this);
        }
    }

    public SingleNodeVisitor visitAliasNode(AliasNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitAndNode(AndNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitArgsCatNode(ArgsCatNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitArgsNode(ArgsNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitArrayNode(ArrayNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitBackRefNode(BackRefNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitBeginNode(BeginNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitBlockArgNode(BlockArgNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitBlockNode(BlockNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitBlockPassNode(BlockPassNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitBreakNode(BreakNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitConstDeclNode(ConstDeclNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitClassVarNode(ClassVarNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitCallNode(CallNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitCaseNode(CaseNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitClassNode(ClassNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitColon2Node(Colon2Node iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitColon3Node(Colon3Node iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitConstNode(ConstNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitDAsgnNode(DAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitDRegxNode(DRegexpNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitDStrNode(DStrNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitDSymbolNode(DSymbolNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitDVarNode(DVarNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitDXStrNode(DXStrNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitDefinedNode(DefinedNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitDefnNode(DefnNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitDefsNode(DefsNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitDotNode(DotNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitEnsureNode(EnsureNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitEvStrNode(EvStrNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitFCallNode(FCallNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitFalseNode(FalseNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitFlipNode(FlipNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitForNode(ForNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitGlobalVarNode(GlobalVarNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitHashNode(HashNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitInstAsgnNode(InstAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitInstVarNode(InstVarNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitIfNode(IfNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitIterNode(IterNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitLocalAsgnNode(LocalAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitLocalVarNode(LocalVarNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitMatch2Node(Match2Node iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitMatch3Node(Match3Node iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitMatchNode(MatchNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitModuleNode(ModuleNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitNewlineNode(NewlineNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitNextNode(NextNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitNilNode(NilNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitNotNode(NotNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitNthRefNode(NthRefNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitOpAsgnNode(OpAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitOptNNode(OptNNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitOrNode(OrNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitPostExeNode(PostExeNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitRedoNode(RedoNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitRescueBodyNode(RescueBodyNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitRescueNode(RescueNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitRetryNode(RetryNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitReturnNode(ReturnNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitSClassNode(SClassNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitScopeNode(ScopeNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitSelfNode(SelfNode iVisited) {
        return visitNode(iVisited);
    }
    
    public SingleNodeVisitor visitSplatNode(SplatNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitStrNode(StrNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitSValueNode(SValueNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitSuperNode(SuperNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitToAryNode(ToAryNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitTrueNode(TrueNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitUndefNode(UndefNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitUntilNode(UntilNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitVAliasNode(VAliasNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitVCallNode(VCallNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitWhenNode(WhenNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitWhileNode(WhileNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitXStrNode(XStrNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitYieldNode(YieldNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitZArrayNode(ZArrayNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitZSuperNode(ZSuperNode iVisited) {
        return visitNode(iVisited);
    }
    
    public SingleNodeVisitor visitBignumNode(BignumNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitFixnumNode(FixnumNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitFloatNode(FloatNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitRegexpNode(RegexpNode iVisited) {
        return visitNode(iVisited);
    }

    public SingleNodeVisitor visitSymbolNode(SymbolNode iVisited) {
        return visitNode(iVisited);
    }
}
