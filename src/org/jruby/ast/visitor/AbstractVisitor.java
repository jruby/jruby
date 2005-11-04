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
import org.jruby.evaluator.Instruction;

/** This visitor calls by default the return visitNode method for each visited Node.
 *
 * @author  jpetersen
 */
public abstract class AbstractVisitor implements NodeVisitor {
    
    /**
     * This method is called by default for each visited Node.
     */
    protected abstract Instruction visitNode(Node iVisited);

    public Instruction visitNullNode() {
        return visitNode(null);
    }
    
    public Instruction acceptNode(Node node) {
        if (node == null) {
            return visitNullNode();
        } else {
        	return node.accept(this);
        }
    }

    public Instruction visitAliasNode(AliasNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitAndNode(AndNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitArgsCatNode(ArgsCatNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitArgsNode(ArgsNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitArrayNode(ArrayNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitBackRefNode(BackRefNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitBeginNode(BeginNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitBlockArgNode(BlockArgNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitBlockNode(BlockNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitBlockPassNode(BlockPassNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitBreakNode(BreakNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitConstDeclNode(ConstDeclNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitClassVarNode(ClassVarNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitCallNode(CallNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitCaseNode(CaseNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitClassNode(ClassNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitColon2Node(Colon2Node iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitColon3Node(Colon3Node iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitConstNode(ConstNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitDAsgnNode(DAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitDRegxNode(DRegexpNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitDStrNode(DStrNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitDSymbolNode(DSymbolNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitDVarNode(DVarNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitDXStrNode(DXStrNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitDefinedNode(DefinedNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitDefnNode(DefnNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitDefsNode(DefsNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitDotNode(DotNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitEnsureNode(EnsureNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitEvStrNode(EvStrNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitFCallNode(FCallNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitFalseNode(FalseNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitFlipNode(FlipNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitForNode(ForNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitGlobalVarNode(GlobalVarNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitHashNode(HashNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitInstAsgnNode(InstAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitInstVarNode(InstVarNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitIfNode(IfNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitIterNode(IterNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitLocalAsgnNode(LocalAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitLocalVarNode(LocalVarNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitMatch2Node(Match2Node iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitMatch3Node(Match3Node iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitMatchNode(MatchNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitModuleNode(ModuleNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitNewlineNode(NewlineNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitNextNode(NextNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitNilNode(NilNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitNotNode(NotNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitNthRefNode(NthRefNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitOpAsgnNode(OpAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitOptNNode(OptNNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitOrNode(OrNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitPostExeNode(PostExeNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitRedoNode(RedoNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitRescueBodyNode(RescueBodyNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitRescueNode(RescueNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitRetryNode(RetryNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitReturnNode(ReturnNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitSClassNode(SClassNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitScopeNode(ScopeNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitSelfNode(SelfNode iVisited) {
        return visitNode(iVisited);
    }
    
    public Instruction visitSplatNode(SplatNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitStrNode(StrNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitSValueNode(SValueNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitSuperNode(SuperNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitToAryNode(ToAryNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitTrueNode(TrueNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitUndefNode(UndefNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitUntilNode(UntilNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitVAliasNode(VAliasNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitVCallNode(VCallNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitWhenNode(WhenNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitWhileNode(WhileNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitXStrNode(XStrNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitYieldNode(YieldNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitZArrayNode(ZArrayNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitZSuperNode(ZSuperNode iVisited) {
        return visitNode(iVisited);
    }
    
    public Instruction visitBignumNode(BignumNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitFixnumNode(FixnumNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitFloatNode(FloatNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitRegexpNode(RegexpNode iVisited) {
        return visitNode(iVisited);
    }

    public Instruction visitSymbolNode(SymbolNode iVisited) {
        return visitNode(iVisited);
    }
}
