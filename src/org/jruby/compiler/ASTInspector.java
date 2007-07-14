/*
 * ASTInspector.java
 * 
 * Created on Jul 11, 2007, 6:43:03 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler;

import java.util.HashSet;
import java.util.Set;
import org.jruby.ast.AndNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BlockAcceptingNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.DotNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IArgumentNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.types.INameNode;

/**
 *
 * @author headius
 */
public class ASTInspector {
    private boolean hasClosure;
    private boolean hasClass;
    private boolean hasDef;
    private boolean hasScopeAwareMethods;
    private boolean hasFrameAwareMethods;
    
    private static Set FRAME_AWARE_METHODS = new HashSet();
    private static Set SCOPE_AWARE_METHODS = new HashSet();
    
    static {
        FRAME_AWARE_METHODS.add("eval");
        FRAME_AWARE_METHODS.add("binding");
        FRAME_AWARE_METHODS.add("public");
        FRAME_AWARE_METHODS.add("private");
        FRAME_AWARE_METHODS.add("protected");
        FRAME_AWARE_METHODS.add("module_function");
        FRAME_AWARE_METHODS.add("block_given?");
        FRAME_AWARE_METHODS.add("iterator?");
        
        SCOPE_AWARE_METHODS.add("eval");
        SCOPE_AWARE_METHODS.add("binding");
        SCOPE_AWARE_METHODS.add("local_variables");
    }
    
    public void inspect(Node node) {
        if (node == null) return;
        
        switch (node.nodeId) {
        case NodeTypes.ALIASNODE:
            break;
        case NodeTypes.ANDNODE:
            AndNode andNode = (AndNode)node;
            inspect(andNode.getFirstNode());
            inspect(andNode.getSecondNode());
            break;
        case NodeTypes.ARRAYNODE:
        case NodeTypes.BLOCKNODE:
        case NodeTypes.DSTRNODE:
            ListNode listNode = (ListNode)node;
            for (int i = 0; i < listNode.size(); i++) {
                inspect(listNode.get(i));
            }
            break;
        case NodeTypes.ATTRASSIGNNODE:
            AttrAssignNode attrAssignNode = (AttrAssignNode)node;
            inspect(attrAssignNode.getArgsNode());
            inspect(attrAssignNode.getReceiverNode());
            break;
        case NodeTypes.BEGINNODE:
            inspect(((BeginNode)node).getBodyNode());
            break;
        case NodeTypes.BIGNUMNODE:
            break;
        case NodeTypes.BREAKNODE:
            inspect(((BreakNode)node).getValueNode());
            break;
        case NodeTypes.CALLNODE:
            CallNode callNode = (CallNode)node;
            inspect(callNode.getReceiverNode());
        case NodeTypes.FCALLNODE:
            inspect(((IArgumentNode)node).getArgsNode());
            inspect(((BlockAcceptingNode)node).getIterNode());
        case NodeTypes.VCALLNODE:
            INameNode nameNode = (INameNode)node;
            if (FRAME_AWARE_METHODS.contains(nameNode)) {
                hasFrameAwareMethods = true;
            }
            if (SCOPE_AWARE_METHODS.contains(nameNode)) {
                hasScopeAwareMethods = true;
            }
            break;
        case NodeTypes.CLASSNODE:
            hasClass = true;
            break;
        case NodeTypes.CLASSVARNODE:
            break;
        case NodeTypes.GLOBALASGNNODE:
            GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode)node;
            if (globalAsgnNode.getName().equals("$_") || globalAsgnNode.getName().equals("$~")) {
                hasScopeAwareMethods = true;
            }
            break;
        case NodeTypes.CONSTDECLNODE:
        case NodeTypes.CLASSVARASGNNODE:
        case NodeTypes.DASGNNODE:
        case NodeTypes.INSTASGNNODE:
        case NodeTypes.LOCALASGNNODE:
            inspect(((AssignableNode)node).getValueNode());
            break;
        case NodeTypes.COLON2NODE:
            inspect(((Colon2Node)node).getLeftNode());
            break;
        case NodeTypes.CONSTNODE:
            break;
        case NodeTypes.DEFNNODE:
            hasDef = true;
            break;
        case NodeTypes.DOTNODE:
            DotNode dotNode = (DotNode)node;
            inspect(dotNode.getBeginNode());
            inspect(dotNode.getEndNode());
            break;
        case NodeTypes.DVARNODE:
            break;
        case NodeTypes.EVSTRNODE:
            inspect(((EvStrNode)node).getBody());
            break;
        case NodeTypes.FALSENODE:
            break;
        case NodeTypes.FIXNUMNODE:
            break;
        case NodeTypes.FLOATNODE:
            break;
        case NodeTypes.GLOBALVARNODE:
            GlobalVarNode globalVarNode = (GlobalVarNode)node;
            if (globalVarNode.getName().equals("$_") || globalVarNode.getName().equals("$~")) {
                hasScopeAwareMethods = true;
            }
            break;
        case NodeTypes.HASHNODE:
            HashNode hashNode = (HashNode)node;
            for (int i = 0; i < hashNode.getListNode().size(); i++) {
                inspect(hashNode.getListNode().get(i));
            }
            break;
        case NodeTypes.IFNODE:
            IfNode ifNode = (IfNode)node;
            inspect(ifNode.getCondition());
            inspect(ifNode.getThenBody());
            inspect(ifNode.getElseBody());
            break;
        case NodeTypes.INSTVARNODE:
            break;
        case NodeTypes.ITERNODE:
            hasClosure = true;
            break;
        case NodeTypes.LOCALVARNODE:
            break;
        case NodeTypes.MATCHNODE:
            inspect(((MatchNode)node).getRegexpNode());
            break;
        case NodeTypes.MATCH2NODE:
            Match2Node match2Node = (Match2Node)node;
            inspect(match2Node.getReceiverNode());
            inspect(match2Node.getValueNode());
            break;
        case NodeTypes.MATCH3NODE:
            Match3Node match3Node = (Match3Node)node;
            inspect(match3Node.getReceiverNode());
            inspect(match3Node.getValueNode());
            break;
        case NodeTypes.MODULENODE:
            hasClass = true;
            break;
        case NodeTypes.NEWLINENODE:
            inspect(((NewlineNode)node).getNextNode());
            break;
        case NodeTypes.NEXTNODE:
            inspect(((NextNode)node).getValueNode());
            break;
        case NodeTypes.NTHREFNODE:
            break;
        case NodeTypes.NILNODE:
            break;
        case NodeTypes.NOTNODE:
            inspect(((NotNode)node).getConditionNode());
            break;
        case NodeTypes.OPASGNNODE:
            OpAsgnNode opAsgnNode = (OpAsgnNode)node;
            inspect(opAsgnNode.getReceiverNode());
            inspect(opAsgnNode.getValueNode());
            break;
        case NodeTypes.ORNODE:
            OrNode orNode = (OrNode)node;
            inspect(orNode.getFirstNode());
            inspect(orNode.getSecondNode());
            break;
        case NodeTypes.REDONODE:
            break;
        case NodeTypes.REGEXPNODE:
            break;
        case NodeTypes.RETURNNODE:
            inspect(((ReturnNode)node).getValueNode());
            break;
        case NodeTypes.SELFNODE:
            break;
        case NodeTypes.SPLATNODE:
            inspect(((SplatNode)node).getValue());
            break;
        case NodeTypes.STRNODE:
            break;
        case NodeTypes.SVALUENODE:
            inspect(((SValueNode)node).getValue());
            break;
        case NodeTypes.SYMBOLNODE:
            break;
        case NodeTypes.TRUENODE:
            break;
        case NodeTypes.WHILENODE:
            WhileNode whileNode = (WhileNode)node;
            inspect(whileNode.getConditionNode());
            inspect(whileNode.getBodyNode());
            break;
        case NodeTypes.YIELDNODE:
            inspect(((YieldNode)node).getArgsNode());
            break;
        case NodeTypes.ZARRAYNODE:
            break;
        default:
        }
    }

    public boolean hasClass() {
        return hasClass;
    }

    public boolean hasClosure() {
        return hasClosure;
    }

    public boolean hasDef() {
        return hasDef;
    }

    public boolean hasFrameAwareMethods() {
        return hasFrameAwareMethods;
    }

    public boolean hasScopeAwareMethods() {
        return hasScopeAwareMethods;
    }
}
