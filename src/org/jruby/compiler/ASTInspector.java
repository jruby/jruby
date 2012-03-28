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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.ast.*;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.types.INameNode;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

/**
 *
 * @author headius
 */
public class ASTInspector {

    private static final Logger LOG = LoggerFactory.getLogger("ASTInspector");

    private final boolean dump;
    private final String name;
    public ASTInspector() {
        dump = false;
        name = null;
    }

    public ASTInspector(String name, boolean dump) {
        this.name = name;
        this.dump = dump;
    }

    private static final boolean DEBUG = false;

    public static final int BLOCK_ARG = 0x1; // block argument to the method
    public static final int CLOSURE = 0x2; // closure present
    public static final int CLASS = 0x4; // class present
    public static final int METHOD = 0x8; // method table mutations, def, defs, undef, alias
    public static final int EVAL = 0x10; // likely call to eval
    public static final int FRAME_AWARE = 0x20; // makes calls that are aware of the frame
    public static final int FRAME_SELF = 0x40; // makes calls that are aware of the frame's self
    public static final int FRAME_VISIBILITY = 0x80; // makes calls that are aware of the frame's visibility
    public static final int FRAME_BLOCK = 0x100; // makes calls that are aware of the frame's block
    public static final int FRAME_NAME = 0x200; // makes calls that are aware of the frame's name
    public static final int BACKREF = 0x400; // makes calls that set or get backref
    public static final int LASTLINE = 0x800; // makes calls that set or get lastline
    public static final int FRAME_CLASS = 0x1000; // makes calls that are aware of the frame's class
    public static final int OPT_ARGS = 0x2000; // optional arguments to the method
    public static final int REST_ARG = 0x4000; // rest arg to the method
    public static final int SCOPE_AWARE = 0x8000; // makes calls that are aware of the scope
    public static final int ZSUPER = 0x10000; // makes a zero-argument super call
    public static final int CONSTANT = 0x20000; // accesses or sets constants
    public static final int CLASS_VAR = 0x40000; // accesses or sets class variables
    public static final int SUPER = 0x80000; // makes normal super call
    public static final int RETRY = 0x100000; // contains a retry

    private static final String[] MODIFIER_NAMES = {
        "BLOCK", "CLOSURE", "CLASS", "METHOD", "EVAL", "FRAME_AWARE", "FRAME_SELF",
        "FRAME_VISIBILITY", "FRAME_BLOCK", "FRAME_NAME", "BACKREF", "LASTLINE", "FRAME_CLASS",
        "OPT_ARGS", "REST_ARG", "SCOPE_AWARE", "ZSUPER", "CONSTANT", "CLASS_VAR",
        "SUPER", "RETRY"
    };
    
    private int flags;
    
    // pragmas
    private boolean noFrame;
    
    public static final Set<String> FRAME_AWARE_METHODS = Collections.synchronizedSet(new HashSet<String>());
    public static final Set<String> SCOPE_AWARE_METHODS = Collections.synchronizedSet(new HashSet<String>());

    public static void addFrameAwareMethods(String... methods) {
        if (DEBUG) LOG.debug("Adding frame-aware method names: {}", Arrays.toString(methods));
        FRAME_AWARE_METHODS.addAll(Arrays.asList(methods));
    }
    
    public static void addScopeAwareMethods(String... methods) {
        if (DEBUG) LOG.debug("Adding scope-aware method names: {}", Arrays.toString(methods));
        SCOPE_AWARE_METHODS.addAll(Arrays.asList(methods));
    }

    public static final Set<String> PRAGMAS = Collections.synchronizedSet(new HashSet<String>());
    
    static {
        FRAME_AWARE_METHODS.add("eval");
        FRAME_AWARE_METHODS.add("module_eval");
        FRAME_AWARE_METHODS.add("class_eval");
        FRAME_AWARE_METHODS.add("instance_eval");
        FRAME_AWARE_METHODS.add("binding");
        FRAME_AWARE_METHODS.add("public");
        FRAME_AWARE_METHODS.add("private");
        FRAME_AWARE_METHODS.add("protected");
        FRAME_AWARE_METHODS.add("module_function");
        FRAME_AWARE_METHODS.add("block_given?");
        FRAME_AWARE_METHODS.add("iterator?");
        
        SCOPE_AWARE_METHODS.addAll(RubyModule.SCOPE_CAPTURING_METHODS);
        
        PRAGMAS.add("__NOFRAME__");
    }
    
    public void disable() {
        if (dump) LOG.debug("[ASTInspector] {} DISABLED", name);
        flags = 0xFFFFFFFF;
    }

    public CallConfiguration getCallConfig() {
        if (!noFrame() && (hasFrameAwareMethods() || hasClosure() || RubyInstanceConfig.FULL_TRACE_ENABLED)) {
            // We're doing normal framed compilation or the method needs a frame
            if (hasClosure() || hasScopeAwareMethods()) {
                // The method also needs a scope, do both
                return CallConfiguration.FrameFullScopeFull;
            } else {
                if (hasConstant() || hasMethod() || hasClass() || hasClassVar()) {
                    // The method doesn't need a scope, but has static scope needs; use a dummy scope
                    return CallConfiguration.FrameFullScopeDummy;
                } else {
                    // The method doesn't need a scope or static scope; frame only
                    return CallConfiguration.FrameFullScopeNone;
                }
            }
        } else {
            if (hasClosure() || hasScopeAwareMethods()) {
                return CallConfiguration.FrameNoneScopeFull;
            } else {
                if (hasConstant() || hasMethod() || hasClass() || hasClassVar()) {
                    return CallConfiguration.FrameNoneScopeDummy;
                } else {
                    return CallConfiguration.FrameNoneScopeNone;
                }
            }
        }
    }
    
    /**
     * Perform an inspection of a subtree or set of subtrees separate from the
     * parent inspection, to make independent decisions based on that subtree(s).
     * 
     * @param nodes The child nodes to walk with a new inspector
     * @return The new inspector resulting from the walk
     */
    public ASTInspector subInspect(Node... nodes) {
        ASTInspector newInspector = new ASTInspector(name, dump);
        
        for (Node node : nodes) {
            newInspector.inspect(node);
        }
        
        return newInspector;
    }
    
    public boolean getFlag(int modifier) {
        return (flags & modifier) != 0;
    }

    public void setFlag(int modifier) {
        if (dump) {
            LOG.debug("[ASTInspector] {}\n\tset flag {}", name, Integer.toHexString(modifier));
        }
        flags |= modifier;
    }
    
    public void setFlag(Node node, int modifier) {
        if (dump) {
            LOG.debug("[ASTInspector] {}\n\tset flag {} because of {} at {}", name, Integer.toHexString(modifier), node.getNodeType(), node.getPosition());
        }
        flags |= modifier;
    }
    
    /**
     * Integrate the results of a separate inspection into the state of this
     * inspector.
     * 
     * @param other The other inspector whose state to integrate.
     */
    public void integrate(ASTInspector other) {
        flags |= other.flags;
    }
    
    public void inspect(Node node) {
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            disable();
            return;
        }

        if (node == null) return;
        
        switch (node.getNodeType()) {
        case ALIASNODE:
            setFlag(node, METHOD);
            break;
        case ANDNODE:
            AndNode andNode = (AndNode)node;
            inspect(andNode.getFirstNode());
            inspect(andNode.getSecondNode());
            break;
        case ARGSCATNODE:
            ArgsCatNode argsCatNode = (ArgsCatNode)node;
            inspect(argsCatNode.getFirstNode());
            inspect(argsCatNode.getSecondNode());
            break;
        case ARGSPUSHNODE:
            ArgsPushNode argsPushNode = (ArgsPushNode)node;
            inspect(argsPushNode.getFirstNode());
            inspect(argsPushNode.getSecondNode());
            break;
        case ARGUMENTNODE:
            break;
        case ARRAYNODE:
        case BLOCKNODE:
        case DREGEXPNODE:
        case DSTRNODE:
        case DSYMBOLNODE:
        case DXSTRNODE:
        case LISTNODE:
            ListNode listNode = (ListNode)node;
            for (int i = 0; i < listNode.size(); i++) {
                inspect(listNode.get(i));
            }
            break;
        case ARGSNODE:
            ArgsNode argsNode = (ArgsNode)node;
            if (argsNode.getBlock() != null) setFlag(node, BLOCK_ARG);
            if (argsNode.getOptArgs() != null) {
                setFlag(node, OPT_ARGS);
                inspect(argsNode.getOptArgs());
            }
            if (argsNode.getRestArg() == -2 || argsNode.getRestArg() >= 0) setFlag(node, REST_ARG);
            break;
        case ATTRASSIGNNODE:
            AttrAssignNode attrAssignNode = (AttrAssignNode)node;
            inspect(attrAssignNode.getArgsNode());
            inspect(attrAssignNode.getReceiverNode());
            break;
        case BACKREFNODE:
            setFlag(node, BACKREF);
            break;
        case BEGINNODE:
            inspect(((BeginNode)node).getBodyNode());
            break;
        case BIGNUMNODE:
            break;
        case BINARYOPERATORNODE:
            BinaryOperatorNode binaryOperatorNode = (BinaryOperatorNode)node;
            inspect(binaryOperatorNode.getFirstNode());
            inspect(binaryOperatorNode.getSecondNode());
            break;
        case BLOCKARGNODE:
            break;
        case BLOCKPASSNODE:
            BlockPassNode blockPassNode = (BlockPassNode)node;
            inspect(blockPassNode.getArgsNode());
            inspect(blockPassNode.getBodyNode());
            break;
        case BREAKNODE:
            inspect(((BreakNode)node).getValueNode());
            break;
        case CALLNODE:
            CallNode callNode = (CallNode)node;
            inspect(callNode.getReceiverNode());
            // check for Proc.new, an especially magic method
            if (callNode.getName() == "new" &&
                    callNode.getReceiverNode() instanceof ConstNode &&
                    ((ConstNode)callNode.getReceiverNode()).getName() == "Proc") {
                // Proc.new needs the caller's block to instantiate a proc
                setFlag(node, FRAME_BLOCK);
            }
        case FCALLNODE:
            inspect(((IArgumentNode)node).getArgsNode());
            inspect(((BlockAcceptingNode)node).getIterNode());
        case VCALLNODE:
            INameNode nameNode = (INameNode)node;
            if (FRAME_AWARE_METHODS.contains(nameNode.getName())) {
                setFlag(node, FRAME_AWARE);
                if (nameNode.getName().indexOf("eval") != -1) {
                    setFlag(node, EVAL);
                }
            }
            if (SCOPE_AWARE_METHODS.contains(nameNode.getName())) {
                setFlag(node, SCOPE_AWARE);
            }
            break;
        case CASENODE:
            CaseNode caseNode = (CaseNode)node;
            inspect(caseNode.getCaseNode());
            for (Node when : caseNode.getCases().childNodes()) inspect(when);
            inspect(caseNode.getElseNode());
            break;
        case CLASSNODE:
            setFlag(node, CLASS);
            ClassNode classNode = (ClassNode)node;
            inspect(classNode.getCPath());
            inspect(classNode.getSuperNode());
            break;
        case CLASSVARNODE:
            setFlag(node, CLASS_VAR);
            break;
        case CONSTDECLNODE:
            inspect(((AssignableNode)node).getValueNode());
            setFlag(node, CONSTANT);
            break;
        case CLASSVARASGNNODE:
            inspect(((AssignableNode)node).getValueNode());
            setFlag(node, CLASS_VAR);
            break;
        case CLASSVARDECLNODE:
            inspect(((AssignableNode)node).getValueNode());
            setFlag(node, CLASS_VAR);
            break;
        case COLON2NODE:
            inspect(((Colon2Node)node).getLeftNode());
            break;
        case COLON3NODE:
            break;
        case CONSTNODE:
            setFlag(node, CONSTANT);
            break;
        case DEFNNODE:
        case DEFSNODE:
            setFlag(node, METHOD);
            setFlag(node, FRAME_VISIBILITY);
            break;
        case DEFINEDNODE:
            switch (((DefinedNode)node).getExpressionNode().getNodeType()) {
            case CLASSVARASGNNODE:
            case CLASSVARDECLNODE:
            case CONSTDECLNODE:
            case DASGNNODE:
            case GLOBALASGNNODE:
            case LOCALASGNNODE:
            case MULTIPLEASGNNODE:
            case OPASGNNODE:
            case OPELEMENTASGNNODE:
            case DVARNODE:
            case FALSENODE:
            case TRUENODE:
            case LOCALVARNODE:
            case INSTVARNODE:
            case BACKREFNODE:
            case SELFNODE:
            case VCALLNODE:
            case YIELDNODE:
            case GLOBALVARNODE:
            case CONSTNODE:
            case FCALLNODE:
            case CLASSVARNODE:
                // ok, we have fast paths
                inspect(((DefinedNode)node).getExpressionNode());
                break;
            default:
                // long, slow way causes disabling
                disable();
            }
            break;
        case DOTNODE:
            DotNode dotNode = (DotNode)node;
            inspect(dotNode.getBeginNode());
            inspect(dotNode.getEndNode());
            break;
        case DASGNNODE:
            inspect(((AssignableNode)node).getValueNode());
            break;
        case DVARNODE:
            break;
        case ENSURENODE:
            disable();
            break;
        case ENCODINGNODE:
            break;
        case EVSTRNODE:
            inspect(((EvStrNode)node).getBody());
            break;
        case FALSENODE:
            break;
        case FIXNUMNODE:
            break;
        case FLIPNODE:
            inspect(((FlipNode)node).getBeginNode());
            inspect(((FlipNode)node).getEndNode());
            break;
        case FLOATNODE:
            break;
        case FORNODE:
            setFlag(node, CLOSURE);
            setFlag(node, SCOPE_AWARE);
            inspect(((ForNode)node).getIterNode());
            inspect(((ForNode)node).getBodyNode());
            inspect(((ForNode)node).getVarNode());
            break;
        case GLOBALASGNNODE:
            GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode)node;
            if (globalAsgnNode.getName().equals("$_")) {
                setFlag(node, LASTLINE);
            } else if (globalAsgnNode.getName().equals("$~")) {
                setFlag(node, BACKREF);
            }
            inspect(globalAsgnNode.getValueNode());
            break;
        case GLOBALVARNODE:
        {
            String name = ((GlobalVarNode)node).getName();
            if (name.equals("$_") || name.equals("$LAST_READ_LINE")) {
                setFlag(node, LASTLINE);
            } else if (name.equals("$~") ||
                    name.equals("$`") ||
                    name.equals("$'") ||
                    name.equals("$+") ||
                    name.equals("$LAST_MATCH_INFO") ||
                    name.equals("$PREMATCH") ||
                    name.equals("$POSTMATCH") ||
                    name.equals("$LAST_PAREN_MATCH")) {
                setFlag(node, BACKREF);
            }
            break;
        }
        case HASHNODE:
            HashNode hashNode = (HashNode)node;
            inspect(hashNode.getListNode());
            break;
        case IFNODE:
            IfNode ifNode = (IfNode)node;
            inspect(ifNode.getCondition());
            inspect(ifNode.getThenBody());
            inspect(ifNode.getElseBody());
            break;
        case INSTASGNNODE:
            inspect(((AssignableNode)node).getValueNode());
            break;
        case INSTVARNODE:
            break;
        case ISCOPINGNODE:
            IScopingNode iscopingNode = (IScopingNode)node;
            inspect(iscopingNode.getCPath());
            break;
        case ITERNODE:
            setFlag(node, CLOSURE);
            break;
        case LAMBDANODE:
            setFlag(node, CLOSURE);
            break;
        case LOCALASGNNODE:
            LocalAsgnNode localAsgnNode = (LocalAsgnNode)node;
            if (PRAGMAS.contains(localAsgnNode.getName())) {
                if (localAsgnNode.getName().equals("__NOFRAME__")) {
                    noFrame = localAsgnNode.getValueNode() instanceof TrueNode;
                }
                break;
            }
            inspect(localAsgnNode.getValueNode());
            break;
        case LOCALVARNODE:
            break;
        case MATCHNODE:
            inspect(((MatchNode)node).getRegexpNode());
            setFlag(node, BACKREF);
            break;
        case MATCH2NODE:
            Match2Node match2Node = (Match2Node)node;
            inspect(match2Node.getReceiverNode());
            inspect(match2Node.getValueNode());
            setFlag(node, BACKREF);
            break;
        case MATCH3NODE:
            Match3Node match3Node = (Match3Node)node;
            inspect(match3Node.getReceiverNode());
            inspect(match3Node.getValueNode());
            setFlag(node, BACKREF);
            break;
        case MODULENODE:
            setFlag(node, CLASS);
            inspect(((ModuleNode)node).getCPath());
            break;
        case MULTIPLEASGN19NODE:
            MultipleAsgn19Node multipleAsgn19Node = (MultipleAsgn19Node)node;
            inspect(multipleAsgn19Node.getPre());
            inspect(multipleAsgn19Node.getPost());
            inspect(multipleAsgn19Node.getRest());
            inspect(multipleAsgn19Node.getValueNode());
            break;
        case MULTIPLEASGNNODE:
            MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode)node;
            inspect(multipleAsgnNode.getArgsNode());
            inspect(multipleAsgnNode.getHeadNode());
            inspect(multipleAsgnNode.getValueNode());
            break;
        case NEWLINENODE:
            inspect(((NewlineNode)node).getNextNode());
            break;
        case NEXTNODE:
            inspect(((NextNode)node).getValueNode());
            break;
        case NILNODE:
            break;
        case NOTNODE:
            inspect(((NotNode)node).getConditionNode());
            break;
        case NTHREFNODE:
            break;
        case OPASGNANDNODE:
            OpAsgnAndNode opAsgnAndNode = (OpAsgnAndNode)node;
            inspect(opAsgnAndNode.getFirstNode());
            inspect(opAsgnAndNode.getSecondNode());
            break;
        case OPASGNNODE:
            OpAsgnNode opAsgnNode = (OpAsgnNode)node;
            inspect(opAsgnNode.getReceiverNode());
            inspect(opAsgnNode.getValueNode());
            break;
        case OPASGNORNODE:
            switch (((OpAsgnOrNode)node).getFirstNode().getNodeType()) {
            case CLASSVARASGNNODE:
            case CLASSVARDECLNODE:
            case CONSTDECLNODE:
            case DASGNNODE:
            case GLOBALASGNNODE:
            case LOCALASGNNODE:
            case MULTIPLEASGNNODE:
            case OPASGNNODE:
            case OPELEMENTASGNNODE:
            case DVARNODE:
            case FALSENODE:
            case TRUENODE:
            case LOCALVARNODE:
            case INSTVARNODE:
            case BACKREFNODE:
            case SELFNODE:
            case VCALLNODE:
            case YIELDNODE:
            case GLOBALVARNODE:
            case CONSTNODE:
            case FCALLNODE:
            case CLASSVARNODE:
                // ok, we have fast paths
                inspect(((OpAsgnOrNode)node).getSecondNode());
                break;
            default:
                // long, slow way causes disabling for defined
                disable();
            }
            break;
        case OPELEMENTASGNNODE:
            OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode)node;
            inspect(opElementAsgnNode.getArgsNode());
            inspect(opElementAsgnNode.getReceiverNode());
            inspect(opElementAsgnNode.getValueNode());
            break;
        case OPTARGNODE:
            inspect(((OptArgNode)node).getValue());
            break;
        case ORNODE:
            OrNode orNode = (OrNode)node;
            inspect(orNode.getFirstNode());
            inspect(orNode.getSecondNode());
            break;
        case POSTEXENODE:
            PostExeNode postExeNode = (PostExeNode)node;
            setFlag(node, CLOSURE);
            setFlag(node, SCOPE_AWARE);
            inspect(postExeNode.getBodyNode());
            inspect(postExeNode.getVarNode());
            break;
        case PREEXENODE:
            PreExeNode preExeNode = (PreExeNode)node;
            setFlag(node, CLOSURE);
            setFlag(node, SCOPE_AWARE);
            inspect(preExeNode.getBodyNode());
            inspect(preExeNode.getVarNode());
            break;
        case REDONODE:
            break;
        case REGEXPNODE:
            break;
        case ROOTNODE:
            inspect(((RootNode)node).getBodyNode());
            if (((RootNode)node).getBodyNode() instanceof BlockNode) {
                BlockNode blockNode = (BlockNode)((RootNode)node).getBodyNode();
                if (blockNode.size() > 500) {
                    // method has more than 500 lines; we'll need to split it
                    // and therefore need to use a heap-based scope
                    setFlag(node, SCOPE_AWARE);
                }
            }
            break;
        case RESCUEBODYNODE:
            RescueBodyNode rescueBody = (RescueBodyNode)node;
            inspect(rescueBody.getExceptionNodes());
            inspect(rescueBody.getBodyNode());
            inspect(rescueBody.getOptRescueNode());
            break;
        case RESCUENODE:
            RescueNode rescueNode = (RescueNode)node;
            inspect(rescueNode.getBodyNode());
            inspect(rescueNode.getElseNode());
            inspect(rescueNode.getRescueNode());
            disable();
            break;
        case RETRYNODE:
            setFlag(node, RETRY);
            break;
        case RETURNNODE:
            inspect(((ReturnNode)node).getValueNode());
            break;
        case SCLASSNODE:
            setFlag(node, CLASS);
            setFlag(node, FRAME_AWARE);
            SClassNode sclassNode = (SClassNode)node;
            inspect(sclassNode.getReceiverNode());
            break;
        case SCOPENODE:
            break;
        case SELFNODE:
            break;
        case SPLATNODE:
            inspect(((SplatNode)node).getValue());
            break;
        case STARNODE:
            break;
        case STRNODE:
            break;
        case SUPERNODE:
            SuperNode superNode = (SuperNode)node;
            inspect(superNode.getArgsNode());
            inspect(superNode.getIterNode());
            setFlag(node, SUPER);
            break;
        case SVALUENODE:
            inspect(((SValueNode)node).getValue());
            break;
        case SYMBOLNODE:
            break;
        case TOARYNODE:
            inspect(((ToAryNode)node).getValue());
            break;
        case TRUENODE:
            break;
        case UNDEFNODE:
            setFlag(node, METHOD);
            break;
        case UNTILNODE:
            UntilNode untilNode = (UntilNode)node;
            ASTInspector untilInspector = subInspect(
                    untilNode.getConditionNode(), untilNode.getBodyNode());
            // a while node could receive non-local flow control from any of these:
            // * a closure within the loop
            // * an eval within the loop
            // * a block-arg-based proc called within the loop
            if (untilInspector.getFlag(CLOSURE) || untilInspector.getFlag(EVAL)) {
                untilNode.containsNonlocalFlow = true;
                
                // we set scope-aware to true to force heap-based locals
                setFlag(node, SCOPE_AWARE);
            }
            integrate(untilInspector);
            break;
        case VALIASNODE:
            break;
        case WHENNODE: {
            inspect(((WhenNode)node).getBodyNode());
            inspect(((WhenNode)node).getExpressionNodes());
            inspect(((WhenNode)node).getNextCase());
            // if any elements are not literals or are regexp, set backref
            Node expr = ((WhenNode)node).getExpressionNodes();
            if (!(expr instanceof ILiteralNode) || expr.getNodeType() == NodeType.REGEXPNODE) {
                setFlag(node, BACKREF);
            }
            break;
        }
        case WHILENODE:
            WhileNode whileNode = (WhileNode)node;
            ASTInspector whileInspector = subInspect(
                    whileNode.getConditionNode(), whileNode.getBodyNode());
            // a while node could receive non-local flow control from any of these:
            // * a closure within the loop
            // * an eval within the loop
            // * a block-arg-based proc called within the loop
            if (whileInspector.getFlag(CLOSURE) || whileInspector.getFlag(EVAL) || getFlag(BLOCK_ARG)) {
                whileNode.containsNonlocalFlow = true;
                
                // we set scope-aware to true to force heap-based locals
                setFlag(node, SCOPE_AWARE);
            }
            integrate(whileInspector);
            break;
        case XSTRNODE:
            break;
        case YIELDNODE:
            inspect(((YieldNode)node).getArgsNode());
            break;
        case ZARRAYNODE:
            break;
        case ZEROARGNODE:
            break;
        case ZSUPERNODE:
            setFlag(node, SCOPE_AWARE);
            setFlag(node, ZSUPER);
            inspect(((ZSuperNode)node).getIterNode());
            break;
        default:
            // encountered a node we don't recognize, set everything to true to disable optz
            assert false : "All nodes should be accounted for in AST inspector: " + node;
            disable();
        }
    }

    public boolean hasClass() {
        return getFlag(CLASS);
    }

    public boolean hasClosure() {
        return getFlag(CLOSURE);
    }

    /**
     * Whether the tree under inspection contains any method-table mutations,
     * including def, defs, undef, and alias.
     * 
     * @return True if there are mutations, false otherwise
     */
    public boolean hasMethod() {
        return getFlag(METHOD);
    }

    public boolean hasFrameAwareMethods() {
        return getFlag(
                FRAME_AWARE | FRAME_BLOCK | FRAME_CLASS | FRAME_NAME | FRAME_SELF | FRAME_VISIBILITY |
                CLOSURE | EVAL | ZSUPER | SUPER);
    }

    public boolean hasScopeAwareMethods() {
        return getFlag(SCOPE_AWARE | BACKREF | LASTLINE);
    }

    public boolean hasBlockArg() {
        return getFlag(BLOCK_ARG);
    }

    public boolean hasOptArgs() {
        return getFlag(OPT_ARGS);
    }

    public boolean hasRestArg() {
        return getFlag(REST_ARG);
    }
    
    public boolean hasConstant() {
        return getFlag(CONSTANT);
    }
    
    public boolean hasClassVar() {
        return getFlag(CLASS_VAR);
    }
    
    public boolean noFrame() {
        return noFrame;
    }
}
