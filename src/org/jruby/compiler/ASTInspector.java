/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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

package org.jruby.compiler;

import java.util.HashSet;
import java.util.Set;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsNode;
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
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.RootNode;
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
    private boolean hasBlockArg;
    private boolean hasOptArgs;
    private boolean hasRestArg;
    
    private static Set<String> FRAME_AWARE_METHODS = new HashSet<String>();
    private static Set<String> SCOPE_AWARE_METHODS = new HashSet<String>();
    
    static {
        FRAME_AWARE_METHODS.add("eval");
        FRAME_AWARE_METHODS.add("module_eval");
        FRAME_AWARE_METHODS.add("class_eval");
        FRAME_AWARE_METHODS.add("binding");
        FRAME_AWARE_METHODS.add("public");
        FRAME_AWARE_METHODS.add("private");
        FRAME_AWARE_METHODS.add("protected");
        FRAME_AWARE_METHODS.add("module_function");
        FRAME_AWARE_METHODS.add("block_given?");
        FRAME_AWARE_METHODS.add("iterator?");
        
        SCOPE_AWARE_METHODS.add("eval");
        SCOPE_AWARE_METHODS.add("module_eval");
        SCOPE_AWARE_METHODS.add("class_eval");
        SCOPE_AWARE_METHODS.add("binding");
        SCOPE_AWARE_METHODS.add("local_variables");
    }
    
    public void disable() {
        hasClosure = true;
        hasClass = true;
        hasDef = true;
        hasScopeAwareMethods = true;
        hasFrameAwareMethods = true;
        hasBlockArg = true;
        hasOptArgs = true;
        hasRestArg = true;
    }
    
    public static final boolean ENABLED = System.getProperty("jruby.astInspector.enabled", "true").equals("true");
    
    public void inspect(Node node) {
        // TODO: This code effectively disables all inspection-based optimizations; none of them are 100% safe yet
        if (!ENABLED) disable();

        if (node == null) return;
        
        switch (node.nodeId) {
        case ALIASNODE:
            break;
        case ANDNODE:
            AndNode andNode = (AndNode)node;
            inspect(andNode.getFirstNode());
            inspect(andNode.getSecondNode());
            break;
        case ARRAYNODE:
        case BLOCKNODE:
        case DSTRNODE:
            ListNode listNode = (ListNode)node;
            for (int i = 0; i < listNode.size(); i++) {
                inspect(listNode.get(i));
            }
            break;
        case ARGSNODE:
            ArgsNode argsNode = (ArgsNode)node;
            if (argsNode.getBlockArgNode() != null) hasBlockArg = true;
            if (argsNode.getOptArgs() != null) hasOptArgs = true;
            if (argsNode.getRestArg() == -2 || argsNode.getRestArg() >= 0) hasRestArg = true;
            break;
        case ATTRASSIGNNODE:
            AttrAssignNode attrAssignNode = (AttrAssignNode)node;
            inspect(attrAssignNode.getArgsNode());
            inspect(attrAssignNode.getReceiverNode());
            break;
        case BACKREFNODE:
            hasFrameAwareMethods = true;
            break;
        case BEGINNODE:
            inspect(((BeginNode)node).getBodyNode());
            break;
        case BIGNUMNODE:
            break;
        case BREAKNODE:
            inspect(((BreakNode)node).getValueNode());
            break;
        case CALLNODE:
            CallNode callNode = (CallNode)node;
            inspect(callNode.getReceiverNode());
        case FCALLNODE:
            inspect(((IArgumentNode)node).getArgsNode());
            inspect(((BlockAcceptingNode)node).getIterNode());
        case VCALLNODE:
            INameNode nameNode = (INameNode)node;
            if (FRAME_AWARE_METHODS.contains(nameNode.getName())) {
                hasFrameAwareMethods = true;
            }
            if (SCOPE_AWARE_METHODS.contains(nameNode.getName())) {
                hasScopeAwareMethods = true;
            }
            break;
        case CLASSNODE:
            hasScopeAwareMethods = true;
            hasClass = true;
            break;
        case CLASSVARNODE:
            hasScopeAwareMethods = true;
            break;
        case GLOBALASGNNODE:
            GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode)node;
            if (globalAsgnNode.getName().equals("$_") || globalAsgnNode.getName().equals("$~")) {
                hasScopeAwareMethods = true;
            }
            break;
        case CONSTDECLNODE:
        case CLASSVARASGNNODE:
            hasScopeAwareMethods = true;
        case DASGNNODE:
        case INSTASGNNODE:
        case LOCALASGNNODE:
            inspect(((AssignableNode)node).getValueNode());
            break;
        case COLON2NODE:
            inspect(((Colon2Node)node).getLeftNode());
            break;
        case CONSTNODE:
            hasScopeAwareMethods = true;
            break;
        case DEFNNODE:
            hasDef = true;
            break;
        case DEFINEDNODE:
            disable();
            break;
        case DOTNODE:
            DotNode dotNode = (DotNode)node;
            inspect(dotNode.getBeginNode());
            inspect(dotNode.getEndNode());
            break;
        case DVARNODE:
            break;
        case ENSURENODE:
            disable();
            break;
        case EVSTRNODE:
            inspect(((EvStrNode)node).getBody());
            break;
        case FALSENODE:
            break;
        case FIXNUMNODE:
            break;
        case FLOATNODE:
            break;
        case GLOBALVARNODE:
            break;
        case HASHNODE:
            HashNode hashNode = (HashNode)node;
            for (int i = 0; i < hashNode.getListNode().size(); i++) {
                inspect(hashNode.getListNode().get(i));
            }
            break;
        case IFNODE:
            IfNode ifNode = (IfNode)node;
            inspect(ifNode.getCondition());
            inspect(ifNode.getThenBody());
            inspect(ifNode.getElseBody());
            break;
        case INSTVARNODE:
            break;
        case ITERNODE:
            hasClosure = true;
            break;
        case LOCALVARNODE:
            break;
        case MATCHNODE:
            inspect(((MatchNode)node).getRegexpNode());
            break;
        case MATCH2NODE:
            Match2Node match2Node = (Match2Node)node;
            inspect(match2Node.getReceiverNode());
            inspect(match2Node.getValueNode());
            break;
        case MATCH3NODE:
            Match3Node match3Node = (Match3Node)node;
            inspect(match3Node.getReceiverNode());
            inspect(match3Node.getValueNode());
            break;
        case MODULENODE:
            hasClass = true;
            hasScopeAwareMethods = true;
            break;
        case NEWLINENODE:
            inspect(((NewlineNode)node).getNextNode());
            break;
        case NEXTNODE:
            inspect(((NextNode)node).getValueNode());
            break;
        case NTHREFNODE:
            break;
        case NILNODE:
            break;
        case NOTNODE:
            inspect(((NotNode)node).getConditionNode());
            break;
        case OPASGNNODE:
            OpAsgnNode opAsgnNode = (OpAsgnNode)node;
            inspect(opAsgnNode.getReceiverNode());
            inspect(opAsgnNode.getValueNode());
            break;
        case OPASGNORNODE:
            disable(); // Depends on defined
            break;
        case ORNODE:
            OrNode orNode = (OrNode)node;
            inspect(orNode.getFirstNode());
            inspect(orNode.getSecondNode());
            break;
        case REDONODE:
            break;
        case REGEXPNODE:
            break;
        case ROOTNODE:
            inspect(((RootNode)node).getBodyNode());
            break;
        case RETURNNODE:
            inspect(((ReturnNode)node).getValueNode());
            break;
        case SELFNODE:
            break;
        case SPLATNODE:
            inspect(((SplatNode)node).getValue());
            break;
        case STRNODE:
            break;
        case SVALUENODE:
            inspect(((SValueNode)node).getValue());
            break;
        case SYMBOLNODE:
            break;
        case TRUENODE:
            break;
        case WHILENODE:
            WhileNode whileNode = (WhileNode)node;
            inspect(whileNode.getConditionNode());
            inspect(whileNode.getBodyNode());
            break;
        case YIELDNODE:
            inspect(((YieldNode)node).getArgsNode());
            break;
        case ZARRAYNODE:
            break;
        default:
            // encountered a node we don't recognize, set everything to true to disable optz
            disable();
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

    public boolean hasBlockArg() {
        return hasBlockArg;
    }

    public boolean hasOptArgs() {
        return hasOptArgs;
    }

    public boolean hasRestArg() {
        return hasRestArg;
    }
}
