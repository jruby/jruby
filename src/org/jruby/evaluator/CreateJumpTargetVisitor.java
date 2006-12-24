/*******************************************************************************
 * BEGIN LICENSE BLOCK *** Version: CPL 1.0/GPL 2.0/LGPL 2.1
 * 
 * The contents of this file are subject to the Common Public License Version
 * 1.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Charles Oliver Nutter <headius@headius.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"), in
 * which case the provisions of the GPL or the LGPL are applicable instead of
 * those above. If you wish to allow use of your version of this file only under
 * the terms of either the GPL or the LGPL, and not to allow others to use your
 * version of this file under the terms of the CPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and other
 * provisions required by the GPL or the LGPL. If you do not delete the
 * provisions above, a recipient may use your version of this file under the
 * terms of any one of the CPL, the GPL or the LGPL. END LICENSE BLOCK ****
 ******************************************************************************/
package org.jruby.evaluator;

import java.util.Iterator;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BinaryOperatorNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;

public class CreateJumpTargetVisitor {
    public static void setJumpTarget(Object target, Node node) {
        bigloop: do {
            if (node == null) return;

            switch (node.nodeId) {
                case NodeTypes.ANDNODE:
                case NodeTypes.OPASGNANDNODE:
                case NodeTypes.OPASGNORNODE:
                case NodeTypes.ORNODE:
                    setJumpTarget(target, ((BinaryOperatorNode)node).getFirstNode());
                    node = ((BinaryOperatorNode)node).getSecondNode();
                    continue bigloop;
                case NodeTypes.ARGSCATNODE:
                    setJumpTarget(target, ((ArgsCatNode)node).getFirstNode());
                    node = ((ArgsCatNode)node).getSecondNode();
                    continue bigloop;
                case NodeTypes.ARRAYNODE:
                case NodeTypes.BLOCKNODE:
                case NodeTypes.DREGEXPNODE:
                case NodeTypes.DSTRNODE:
                case NodeTypes.DSYMBOLNODE:
                case NodeTypes.DXSTRNODE:
                case NodeTypes.HASHNODE:
                    for (Iterator iter = node.childNodes().iterator(); iter.hasNext();) {
                        setJumpTarget(target, (Node)iter.next());
                    }
                    return;
                case NodeTypes.BEGINNODE:
                    node = ((BeginNode)node).getBodyNode();
                    continue bigloop;
                case NodeTypes.BLOCKPASSNODE:
                    setJumpTarget(target, ((BlockPassNode)node).getArgsNode());
                    setJumpTarget(target, ((BlockPassNode)node).getBodyNode());
                    node = ((BlockPassNode)node).getIterNode();
                    continue bigloop;
                case NodeTypes.BREAKNODE:
                    node = ((BreakNode)node).getValueNode();
                    continue bigloop;
                case NodeTypes.CONSTDECLNODE:
                    node = ((ConstDeclNode)node).getValueNode();
                    continue bigloop;
                case NodeTypes.CLASSNODE:
                    setJumpTarget(target, ((ClassNode)node).getSuperNode());
                    node = ((ClassNode)node).getBodyNode();
                    continue bigloop;
                case NodeTypes.CLASSVARASGNNODE:
                    node = ((ClassVarAsgnNode)node).getValueNode();
                    continue bigloop;
                case NodeTypes.CLASSVARDECLNODE:
                    node = ((ClassVarDeclNode)node).getValueNode();
                    continue bigloop;
                case NodeTypes.CALLNODE:
                    node = ((CallNode)node).getReceiverNode();
                    continue bigloop;
                case NodeTypes.CASENODE:
                    setJumpTarget(target, ((CaseNode)node).getCaseNode());
                    node = ((CaseNode)node).getFirstWhenNode();
                    continue bigloop;
                case NodeTypes.COLON2NODE:
                    node = ((Colon2Node)node).getLeftNode();
                    continue bigloop;
                case NodeTypes.DASGNNODE:
                    node = ((DAsgnNode)node).getValueNode();
                    continue bigloop;
                case NodeTypes.DEFINEDNODE:
                    node = ((DefinedNode)node).getExpressionNode();
                    continue bigloop;
                case NodeTypes.DOTNODE:
                    setJumpTarget(target, ((DotNode)node).getBeginNode());
                    node = ((DotNode)node).getEndNode();
                    continue bigloop;
                case NodeTypes.ENSURENODE:
                    setJumpTarget(target, ((EnsureNode)node).getBodyNode());
                    node = ((EnsureNode)node).getEnsureNode();
                    continue bigloop;
                case NodeTypes.EVSTRNODE:
                    node = ((EvStrNode)node).getBody();
                    continue bigloop;
                case NodeTypes.FLIPNODE:
                    setJumpTarget(target, ((FlipNode)node).getBeginNode());
                    node = ((FlipNode)node).getEndNode();
                    continue bigloop;
                case NodeTypes.FORNODE:
                    setJumpTarget(target, ((ForNode)node).getBodyNode());
                    setJumpTarget(target, ((ForNode)node).getIterNode());
                    node = ((ForNode)node).getVarNode();
                    continue bigloop;
                case NodeTypes.GLOBALASGNNODE:
                    node = ((GlobalAsgnNode)node).getValueNode();
                    continue bigloop;
                case NodeTypes.INSTASGNNODE:
                    node = ((InstAsgnNode)node).getValueNode();
                    continue bigloop;
                case NodeTypes.IFNODE:
                    setJumpTarget(target, ((IfNode)node).getCondition());
                    setJumpTarget(target, ((IfNode)node).getThenBody());
                    node = ((IfNode)node).getElseBody();
                    continue bigloop;
                case NodeTypes.ITERNODE:
                    setJumpTarget(target, ((IterNode)node).getBodyNode());
                    setJumpTarget(target, ((IterNode)node).getIterNode());
                    node = ((IterNode)node).getVarNode();
                    continue bigloop;
                case NodeTypes.LOCALASGNNODE:
                    node = ((LocalAsgnNode)node).getValueNode();
                    continue bigloop;
                case NodeTypes.MULTIPLEASGNNODE:
                    node = ((MultipleAsgnNode)node).getValueNode();
                    continue bigloop;
                case NodeTypes.MATCH2NODE:
                    setJumpTarget(target, ((Match2Node)node).getReceiverNode());
                    node = ((Match2Node)node).getValueNode();
                    continue bigloop;
                case NodeTypes.MATCH3NODE:
                    setJumpTarget(target, ((Match3Node)node).getReceiverNode());
                    node = ((Match3Node)node).getValueNode();
                    continue bigloop;
                case NodeTypes.MATCHNODE:
                    node = ((MatchNode)node).getRegexpNode();
                    continue bigloop;
                case NodeTypes.NEWLINENODE:
                    node = ((NewlineNode)node).getNextNode();
                    continue bigloop;
                case NodeTypes.NOTNODE:
                    node = ((NotNode)node).getConditionNode();
                    continue bigloop;
                case NodeTypes.OPELEMENTASGNNODE:
                    setJumpTarget(target, ((OpElementAsgnNode)node).getReceiverNode());
                    node = ((OpElementAsgnNode)node).getValueNode();
                    continue bigloop;
                case NodeTypes.OPASGNNODE:
                    setJumpTarget(target, ((OpAsgnNode)node).getReceiverNode());
                    node = ((OpAsgnNode)node).getValueNode();
                    continue bigloop;
                case NodeTypes.OPTNNODE:
                    node = ((OptNNode)node).getBodyNode();
                    continue bigloop;
                case NodeTypes.RESCUEBODYNODE:
                    setJumpTarget(target, ((RescueBodyNode)node).getBodyNode());
                    setJumpTarget(target, ((RescueBodyNode)node).getExceptionNodes());
                    node = ((RescueBodyNode)node).getOptRescueNode();
                    continue bigloop;
                case NodeTypes.RESCUENODE:
                    setJumpTarget(target, ((RescueNode)node).getBodyNode());
                    setJumpTarget(target, ((RescueNode)node).getElseNode());
                    node = ((RescueNode)node).getRescueNode();
                    continue bigloop;
                case NodeTypes.RETURNNODE:
                    ((ReturnNode)node).setTarget(target);
                    return;
                case NodeTypes.SCLASSNODE:
                    setJumpTarget(target, ((SClassNode)node).getReceiverNode());
                    node = ((SClassNode)node).getBodyNode();
                    continue bigloop;
                case NodeTypes.SPLATNODE:
                    node = ((SplatNode)node).getValue();
                    continue bigloop;
                case NodeTypes.SVALUENODE:
                    node = ((SValueNode)node).getValue();
                    continue bigloop;
                case NodeTypes.TOARYNODE:
                    node = ((ToAryNode)node).getValue();
                    continue bigloop;
                case NodeTypes.UNTILNODE:
                    setJumpTarget(target, ((UntilNode)node).getConditionNode());
                    node = ((UntilNode)node).getBodyNode();
                    continue bigloop;
                case NodeTypes.WHENNODE:
                    setJumpTarget(target, ((WhenNode)node).getBodyNode());
                    setJumpTarget(target, ((WhenNode)node).getExpressionNodes());
                    node = ((WhenNode)node).getNextCase();
                    continue bigloop;
                case NodeTypes.WHILENODE:
                    setJumpTarget(target, ((WhileNode)node).getConditionNode());
                    node = ((WhileNode)node).getBodyNode();
                    continue bigloop;
                default:
                    return;
            }
        } while (node != null);
    }
}