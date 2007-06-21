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
 * Copyright (C) 2006 Charles Oliver Nutter <headius@headius.com>
 * Copytight (C) 2006-2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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

import org.jruby.Ruby;
import org.jruby.MetaClass;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyKernel;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.RubyRange;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ast.AliasNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgsPushNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BinaryOperatorNode;
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
import org.jruby.ast.ListNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.RootNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.JumpException.JumpType;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.ReOptions;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.SharedScopeBlock;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.KCode;
import org.jruby.util.collections.SinglyLinkedList;

public class EvaluationState {
    public static IRubyObject eval(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block block) {
        try {
            return evalInternal(runtime, context, node, self, block);
        } catch (StackOverflowError sfe) {
            throw runtime.newSystemStackError("stack level too deep");
        }
    }

    private static IRubyObject evalInternal(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        do {
            if (node == null) return nilNode(runtime, context);

            switch (node.nodeId) {
            case NodeTypes.ALIASNODE:
                return aliasNode(runtime, context, node);
            case NodeTypes.ANDNODE: {
                BinaryOperatorNode iVisited = (BinaryOperatorNode) node;
   
                IRubyObject result = evalInternal(runtime,context, iVisited.getFirstNode(), self, aBlock);
                if (!result.isTrue()) return result;
                node = iVisited.getSecondNode();
                continue;
            }
            case NodeTypes.ARGSCATNODE:
                return argsCatNode(runtime, context, node, self, aBlock);
            case NodeTypes.ARGSPUSHNODE:
                return argsPushNode(runtime, context, node, self, aBlock);
            case NodeTypes.ARRAYNODE:
                return arrayNode(runtime, context, node, self, aBlock);
            case NodeTypes.ATTRASSIGNNODE:
                return attrAssignNode(runtime, context, node, self, aBlock); 
            case NodeTypes.BACKREFNODE:
                return backRefNode(context, node);
            case NodeTypes.BEGINNODE: 
                node = ((BeginNode)node).getBodyNode();
                continue;
            case NodeTypes.BIGNUMNODE:
                return bignumNode(runtime, node);
            case NodeTypes.BLOCKNODE:
                return blockNode(runtime, context, node, self, aBlock);
            case NodeTypes.BLOCKPASSNODE:
            assert false: "Call nodes and friends deal with this";
            case NodeTypes.BREAKNODE:
                return breakNode(runtime, context, node, self, aBlock);
            case NodeTypes.CALLNODE:
                return callNode(runtime, context, node, self, aBlock);
            case NodeTypes.CASENODE:
                return caseNode(runtime, context, node, self, aBlock);
            case NodeTypes.CLASSNODE:
                return classNode(runtime, context, node, self, aBlock);
            case NodeTypes.CLASSVARASGNNODE:
                return classVarAsgnNode(runtime, context, node, self, aBlock);
            case NodeTypes.CLASSVARDECLNODE:
                return classVarDeclNode(runtime, context, node, self, aBlock);
            case NodeTypes.CLASSVARNODE:
                return classVarNode(runtime, context, node, self);
            case NodeTypes.COLON2NODE:
                return colon2Node(runtime, context, node, self, aBlock);
            case NodeTypes.COLON3NODE:
                return colon3Node(runtime, node);
            case NodeTypes.CONSTDECLNODE:
                return constDeclNode(runtime, context, node, self, aBlock);
            case NodeTypes.CONSTNODE:
                return constNode(context, node);
            case NodeTypes.DASGNNODE:
                return dAsgnNode(runtime, context, node, self, aBlock);
            case NodeTypes.DEFINEDNODE:
                return definedNode(runtime, context, node, self, aBlock);
            case NodeTypes.DEFNNODE:
                return defnNode(runtime, context, node);
            case NodeTypes.DEFSNODE:
                return defsNode(runtime, context, node, self, aBlock);
            case NodeTypes.DOTNODE:
                return dotNode(runtime, context, node, self, aBlock);
            case NodeTypes.DREGEXPNODE:
                return dregexpNode(runtime, context, node, self, aBlock);
            case NodeTypes.DSTRNODE:
                return dStrNode(runtime, context, node, self, aBlock);
            case NodeTypes.DSYMBOLNODE:
                return dSymbolNode(runtime, context, node, self, aBlock);
            case NodeTypes.DVARNODE:
                return dVarNode(runtime, context, node);
            case NodeTypes.DXSTRNODE:
                return dXStrNode(runtime, context, node, self, aBlock);
            case NodeTypes.ENSURENODE:
                return ensureNode(runtime, context, node, self, aBlock);
            case NodeTypes.EVSTRNODE:
                return evStrNode(runtime, context, node, self, aBlock);
            case NodeTypes.FALSENODE:
                return falseNode(runtime, context);
            case NodeTypes.FCALLNODE:
                return fCallNode(runtime, context, node, self, aBlock);
            case NodeTypes.FIXNUMNODE:
                return fixnumNode(runtime, node);
            case NodeTypes.FLIPNODE:
                return flipNode(runtime, context, node, self, aBlock);
            case NodeTypes.FLOATNODE:
                return floatNode(runtime, node);
            case NodeTypes.FORNODE:
                return forNode(runtime, context, node, self, aBlock);
            case NodeTypes.GLOBALASGNNODE:
                return globalAsgnNode(runtime, context, node, self, aBlock);
            case NodeTypes.GLOBALVARNODE:
                return globalVarNode(runtime, context, node);
            case NodeTypes.HASHNODE:
                return hashNode(runtime, context, node, self, aBlock);
            case NodeTypes.IFNODE: {
                IfNode iVisited = (IfNode) node;
                IRubyObject result = evalInternal(runtime,context, iVisited.getCondition(), self, aBlock);

                if (result.isTrue()) {
                    node = iVisited.getThenBody();
                } else {
                    node = iVisited.getElseBody();
                }
                continue;
            }
            case NodeTypes.INSTASGNNODE:
                return instAsgnNode(runtime, context, node, self, aBlock);
            case NodeTypes.INSTVARNODE:
                return instVarNode(runtime, node, self);
            case NodeTypes.ITERNODE: 
            assert false: "Call nodes deal with these directly";
            case NodeTypes.LOCALASGNNODE:
                return localAsgnNode(runtime, context, node, self, aBlock);
            case NodeTypes.LOCALVARNODE:
                return localVarNode(runtime, context, node);
            case NodeTypes.MATCH2NODE:
                return match2Node(runtime, context, node, self, aBlock);
            case NodeTypes.MATCH3NODE:
                return match3Node(runtime, context, node, self, aBlock);
            case NodeTypes.MATCHNODE:
                return matchNode(runtime, context, node, self, aBlock);
            case NodeTypes.MODULENODE:
                return moduleNode(runtime, context, node, self, aBlock);
            case NodeTypes.MULTIPLEASGNNODE:
                return multipleAsgnNode(runtime, context, node, self, aBlock);
            case NodeTypes.NEWLINENODE: {
                NewlineNode iVisited = (NewlineNode) node;
        
                // something in here is used to build up ruby stack trace...
                context.setPosition(iVisited.getPosition());

                if (isTrace(runtime)) {
                    callTraceFunction(runtime, context, EventHook.RUBY_EVENT_LINE);
                }

                // TODO: do above but not below for additional newline nodes
                node = iVisited.getNextNode();
                continue;
            }
            case NodeTypes.NEXTNODE:
                return nextNode(runtime, context, node, self, aBlock);
            case NodeTypes.NILNODE:
                return nilNode(runtime, context);
            case NodeTypes.NOTNODE:
                return notNode(runtime, context, node, self, aBlock);
            case NodeTypes.NTHREFNODE:
                return nthRefNode(context, node);
            case NodeTypes.OPASGNANDNODE: {
                BinaryOperatorNode iVisited = (BinaryOperatorNode) node;
        
                // add in reverse order
                IRubyObject result = evalInternal(runtime,context, iVisited.getFirstNode(), self, aBlock);
                if (!result.isTrue()) return pollAndReturn(context, result);
                node = iVisited.getSecondNode();
                continue;
            }
            case NodeTypes.OPASGNNODE:
                return opAsgnNode(runtime, context, node, self, aBlock);
            case NodeTypes.OPASGNORNODE:
                return opAsgnOrNode(runtime, context, node, self, aBlock);
            case NodeTypes.OPELEMENTASGNNODE:
                return opElementAsgnNode(runtime, context, node, self, aBlock);
            case NodeTypes.OPTNNODE:
                return optNNode(runtime, context, node, self, aBlock);
            case NodeTypes.ORNODE:
                return orNode(runtime, context, node, self, aBlock);
            case NodeTypes.POSTEXENODE:
                return postExeNode(runtime, context, node, self, aBlock);
            case NodeTypes.REDONODE: 
                return redoNode(context, node);
            case NodeTypes.REGEXPNODE:
                return regexpNode(runtime, node);
            case NodeTypes.RESCUEBODYNODE:
                node = ((RescueBodyNode)node).getBodyNode();
                continue;
            case NodeTypes.RESCUENODE:
                return rescueNode(runtime, context, node, self, aBlock);
            case NodeTypes.RETRYNODE:
                return retryNode(context);
            case NodeTypes.RETURNNODE: 
                return returnNode(runtime, context, node, self, aBlock);
            case NodeTypes.ROOTNODE:
                return rootNode(runtime, context, node, self, aBlock);
            case NodeTypes.SCLASSNODE:
                return sClassNode(runtime, context, node, self, aBlock);
            case NodeTypes.SELFNODE:
                return pollAndReturn(context, self);
            case NodeTypes.SPLATNODE:
                return splatNode(runtime, context, node, self, aBlock);
            case NodeTypes.STRNODE:
                return strNode(runtime, node);
            case NodeTypes.SUPERNODE:
                return superNode(runtime, context, node, self, aBlock);
            case NodeTypes.SVALUENODE:
                return sValueNode(runtime, context, node, self, aBlock);
            case NodeTypes.SYMBOLNODE:
                return symbolNode(runtime, node);
            case NodeTypes.TOARYNODE:
                return toAryNode(runtime, context, node, self, aBlock);
            case NodeTypes.TRUENODE:
                return trueNode(runtime, context);
            case NodeTypes.UNDEFNODE:
                return undefNode(runtime, context, node);
            case NodeTypes.UNTILNODE:
                return untilNode(runtime, context, node, self, aBlock);
            case NodeTypes.VALIASNODE:
                return valiasNode(runtime, node);
            case NodeTypes.VCALLNODE:
                return vcallNode(runtime, context, node, self);
            case NodeTypes.WHENNODE:
                assert false;
                return null;
            case NodeTypes.WHILENODE:
                return whileNode(runtime, context, node, self, aBlock);
            case NodeTypes.XSTRNODE:
                return xStrNode(runtime, context, node, self);
            case NodeTypes.YIELDNODE:
                return yieldNode(runtime, context, node, self, aBlock);
            case NodeTypes.ZARRAYNODE:
                return zArrayNode(runtime);
            case NodeTypes.ZSUPERNODE:
                return zsuperNode(runtime, context, node, self, aBlock);
            default:
                throw new RuntimeException("Invalid node encountered in interpreter: \"" + node.getClass().getName() + "\", please report this at www.jruby.org");
            }
        } while(true);
    }

    private static IRubyObject aliasNode(Ruby runtime, ThreadContext context, Node node) {
        AliasNode iVisited = (AliasNode) node;
   
        if (context.getRubyClass() == null) {
            throw runtime.newTypeError("no class to make alias");
        }
   
        context.getRubyClass().defineAlias(iVisited.getNewName(), iVisited.getOldName());
        context.getRubyClass().callMethod(context, "method_added", runtime.newSymbol(iVisited.getNewName()));
   
        return runtime.getNil();
    }
    
    private static IRubyObject argsCatNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        ArgsCatNode iVisited = (ArgsCatNode) node;
   
        IRubyObject args = evalInternal(runtime,context, iVisited.getFirstNode(), self, aBlock);
        IRubyObject secondArgs = splatValue(runtime, evalInternal(runtime,context, iVisited.getSecondNode(), self, aBlock));
        RubyArray list = args instanceof RubyArray ? (RubyArray) args : runtime.newArray(args);
   
        return list.concat(secondArgs);
    }

    private static IRubyObject argsPushNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        ArgsPushNode iVisited = (ArgsPushNode) node;
        
        RubyArray args = (RubyArray) evalInternal(runtime,context, iVisited.getFirstNode(), self, aBlock).dup();
        return args.append(evalInternal(runtime,context, iVisited.getSecondNode(), self, aBlock));
    }

    private static IRubyObject arrayNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        ArrayNode iVisited = (ArrayNode) node;
        IRubyObject[] array = new IRubyObject[iVisited.size()];
        
        for (int i = 0; i < iVisited.size(); i++) {
            Node next = iVisited.get(i);
   
            array[i] = evalInternal(runtime,context, next, self, aBlock);
        }
   
        if (iVisited.isLightweight()) {
            return runtime.newArrayNoCopyLight(array);
        }
        
        return runtime.newArrayNoCopy(array);
    }

    public static RubyArray arrayValue(Ruby runtime, IRubyObject value) {
        IRubyObject newValue = value.convertToType(runtime.getArray(), MethodIndex.TO_ARY, "to_ary", false);
        if (newValue.isNil()) {
            // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can 
            // remove this hack too.
            if (value.getMetaClass().searchMethod("to_a").getImplementationClass() != runtime
                    .getKernel()) {
                newValue = value.convertToType(runtime.getArray(), MethodIndex.TO_A, "to_a", false);
                if (newValue.getType() != runtime.getClass("Array")) {
                    throw runtime.newTypeError("`to_a' did not return Array");
                }
            } else {
                newValue = runtime.newArray(value);
            }
        }

        return (RubyArray) newValue;
    }

    private static IRubyObject aryToAry(Ruby runtime, IRubyObject value) {
        if (value instanceof RubyArray) return value;

        if (value.respondsTo("to_ary")) {
            return value.convertToType(runtime.getArray(), MethodIndex.TO_A, "to_ary", false);
        }

        return runtime.newArray(value);
    }

    private static IRubyObject attrAssignNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        AttrAssignNode iVisited = (AttrAssignNode) node;
   
        IRubyObject receiver = evalInternal(runtime,context, iVisited.getReceiverNode(), self, aBlock);
        IRubyObject[] args = setupArgs(runtime, context, iVisited.getArgsNode(), self);
        
        assert receiver.getMetaClass() != null : receiver.getClass().getName();
        
        // If reciever is self then we do the call the same way as vcall
        CallType callType = (receiver == self ? CallType.VARIABLE : CallType.NORMAL);
   
        RubyModule module = receiver.getMetaClass();
        
        String name = iVisited.getName();

        DynamicMethod method = module.searchMethod(name);

        if (method.isUndefined() || (!method.isCallableFrom(self, callType))) {
            return RubyObject.callMethodMissing(context, receiver, method, name, args, self, callType, Block.NULL_BLOCK);
        }

        method.call(context, receiver, module, name, args, false, Block.NULL_BLOCK);

        return args[args.length - 1];
    }

    private static IRubyObject backRefNode(ThreadContext context, Node node) {
        BackRefNode iVisited = (BackRefNode) node;
        IRubyObject backref = context.getBackref();
        switch (iVisited.getType()) {
        case '~':
            return backref;
        case '&':
            return RubyRegexp.last_match(backref);
        case '`':
            return RubyRegexp.match_pre(backref);
        case '\'':
            return RubyRegexp.match_post(backref);
        case '+':
            return RubyRegexp.match_last(backref);
        default:
            assert false: "backref with invalid type";
            return null;
        }
    }

    private static IRubyObject bignumNode(Ruby runtime, Node node) {
        return RubyBignum.newBignum(runtime, ((BignumNode)node).getValue());
    }

    private static IRubyObject blockNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        BlockNode iVisited = (BlockNode) node;
   
        IRubyObject result = runtime.getNil();
        for (int i = 0; i < iVisited.size(); i++) {
            result = evalInternal(runtime,context, (Node) iVisited.get(i), self, aBlock);
        }
   
        return result;
    }

    private static IRubyObject breakNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        BreakNode iVisited = (BreakNode) node;
   
        IRubyObject result = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
   
        throw context.prepareJumpException(JumpException.JumpType.BreakJump, null, result);
    }

    private static IRubyObject callNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        CallNode iVisited = (CallNode) node;

        IRubyObject receiver = evalInternal(runtime,context, iVisited.getReceiverNode(), self, aBlock);
        IRubyObject[] args = setupArgs(runtime, context, iVisited.getArgsNode(), self);
        
        assert receiver.getMetaClass() != null : receiver.getClass().getName();

        Block block = getBlock(runtime, context, self, aBlock, iVisited.getIterNode());
        RubyModule module = receiver.getMetaClass();
        String name = iVisited.getName();
        int index = iVisited.index;

        // No block provided lets look at fast path for STI dispatch.
        if (!block.isGiven()) {
            if (index != 0) {
                return receiver.callMethod(context, module, index, name, args, CallType.NORMAL, Block.NULL_BLOCK);
            } else {
                DynamicMethod method = module.searchMethod(name);
      
                if (method.isUndefined() || (!method.isCallableFrom(self, CallType.NORMAL))) {
                    return RubyObject.callMethodMissing(context, receiver, method, name, args, self, CallType.NORMAL, Block.NULL_BLOCK);
                }

                return method.call(context, receiver, module, name, args, false, Block.NULL_BLOCK);
            }
        }
            
        while (true) {
            try {
                DynamicMethod method = module.searchMethod(name);

                if (method.isUndefined() || (index != MethodIndex.METHOD_MISSING && !method.isCallableFrom(self, CallType.NORMAL))) {
                    return RubyObject.callMethodMissing(context, receiver, method, name, index, args, self, CallType.NORMAL, block);
                }

                return method.call(context, receiver, module, name, args, false, block);
            } catch (JumpException je) {
                switch (je.getJumpType().getTypeId()) {
                case JumpType.RETRY:
                    // allow loop to retry
                case JumpType.BREAK:
                    return (IRubyObject) je.getValue();
                default:
                    throw je;
                }
            }
        }
    }

    private static IRubyObject caseNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        CaseNode iVisited = (CaseNode) node;
        IRubyObject expression = null;
        if (iVisited.getCaseNode() != null) {
            expression = evalInternal(runtime,context, iVisited.getCaseNode(), self, aBlock);
        }

        context.pollThreadEvents();

        IRubyObject result = runtime.getNil();

        Node firstWhenNode = iVisited.getFirstWhenNode();
        while (firstWhenNode != null) {
            if (!(firstWhenNode instanceof WhenNode)) {
                node = firstWhenNode;
                return evalInternal(runtime, context, node, self, aBlock);
            }

            WhenNode whenNode = (WhenNode) firstWhenNode;

            if (whenNode.getExpressionNodes() instanceof ArrayNode) {
                ArrayNode arrayNode = (ArrayNode)whenNode.getExpressionNodes();
                for (int i = 0; i < arrayNode.size(); i++) {
                    Node tag = arrayNode.get(i);

                    context.setPosition(tag.getPosition());
                    if (isTrace(runtime)) {
                        callTraceFunction(runtime, context, EventHook.RUBY_EVENT_LINE);
                    }

                    // Ruby grammar has nested whens in a case body because of
                    // productions case_body and when_args.
                    if (tag instanceof WhenNode) {
                        RubyArray expressions = (RubyArray) evalInternal(runtime,context, ((WhenNode) tag)
                                        .getExpressionNodes(), self, aBlock);

                        for (int j = 0,k = expressions.getLength(); j < k; j++) {
                            IRubyObject condition = expressions.eltInternal(j);

                            if ((expression != null && condition.callMethod(context, MethodIndex.OP_EQQ, "===", expression)
                                    .isTrue())
                                    || (expression == null && condition.isTrue())) {
                                node = ((WhenNode) firstWhenNode).getBodyNode();
                                return evalInternal(runtime, context, node, self, aBlock);
                            }
                        }
                        continue;
                    }

                    result = evalInternal(runtime,context, tag, self, aBlock);

                    if ((expression != null && result.callMethod(context, MethodIndex.OP_EQQ, "===", expression).isTrue())
                            || (expression == null && result.isTrue())) {
                        node = whenNode.getBodyNode();
                        return evalInternal(runtime, context, node, self, aBlock);
                    }
                }
            } else {
                result = evalInternal(runtime,context, whenNode.getExpressionNodes(), self, aBlock);

                if ((expression != null && result.callMethod(context, MethodIndex.OP_EQQ, "===", expression).isTrue())
                        || (expression == null && result.isTrue())) {
                    node = ((WhenNode) firstWhenNode).getBodyNode();
                    return evalInternal(runtime, context, node, self, aBlock);
                }
            }

            context.pollThreadEvents();

            firstWhenNode = whenNode.getNextCase();
        }

        return runtime.getNil();
    }

    private static IRubyObject classNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        ClassNode iVisited = (ClassNode) node;
        Node superNode = iVisited.getSuperNode();
        RubyClass superClass = null;
        if(superNode != null) {
            IRubyObject _super = evalInternal(runtime,context, superNode, self, aBlock);
            if(!(_super instanceof RubyClass)) {
                throw runtime.newTypeError("superclass must be a Class (" + RubyObject.trueFalseNil(_super) + ") given");
            }
            superClass = superNode == null ? null : (RubyClass)_super;
        }
        Node classNameNode = iVisited.getCPath();
        String name = ((INameNode) classNameNode).getName();
        RubyModule enclosingClass = getEnclosingModule(runtime, context, classNameNode, self, aBlock);
        RubyClass rubyClass = enclosingClass.defineOrGetClassUnder(name, superClass);
   
        return evalClassDefinitionBody(runtime, context, iVisited.getScope(), iVisited.getBodyNode(), rubyClass, self, aBlock);
    }

    private static IRubyObject classVarAsgnNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        ClassVarAsgnNode iVisited = (ClassVarAsgnNode) node;
        IRubyObject result = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
        RubyModule rubyClass = getClassVariableBase(context, runtime);
   
        if (rubyClass == null) {
            rubyClass = self.getMetaClass();
        }     
        rubyClass.setClassVar(iVisited.getName(), result);
   
        return result;
    }

    private static IRubyObject classVarDeclNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        ClassVarDeclNode iVisited = (ClassVarDeclNode) node;
   
        RubyModule rubyClass = getClassVariableBase(context, runtime);                
        if (rubyClass == null) {
            throw runtime.newTypeError("no class/module to define class variable");
        }
        IRubyObject result = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
        rubyClass.setClassVar(iVisited.getName(), result);
   
        return result;
    }

    private static IRubyObject classVarNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self) {
        ClassVarNode iVisited = (ClassVarNode) node;
        RubyModule rubyClass = getClassVariableBase(context, runtime);
   
        if (rubyClass == null) {
            rubyClass = self.getMetaClass();
        }

        return rubyClass.getClassVar(iVisited.getName());
    }

    private static IRubyObject colon2Node(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        Colon2Node iVisited = (Colon2Node) node;
        Node leftNode = iVisited.getLeftNode();
        // TODO: Made this more colon3 friendly because of cpath production
        // rule in grammar (it is convenient to think of them as the same thing
        // at a grammar level even though evaluation is).
        if (leftNode == null) {
            return runtime.getObject().getConstantFrom(iVisited.getName());
        } else {
            IRubyObject result = evalInternal(runtime,context, iVisited.getLeftNode(), self, aBlock);
            if (result instanceof RubyModule) {
                return ((RubyModule) result).getConstantFrom(iVisited.getName());
            } else {
                return result.callMethod(context, iVisited.getName(), aBlock);
            }
        }
    }

    private static IRubyObject colon3Node(Ruby runtime, Node node) {
        return runtime.getObject().getConstantFrom(((Colon3Node)node).getName());
    }

    private static IRubyObject constDeclNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        ConstDeclNode iVisited = (ConstDeclNode) node;
        Node constNode = iVisited.getConstNode();
   
        IRubyObject result = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
        
        if (constNode == null) {
            return context.setConstantInCurrent(iVisited.getName(), result);
        } else if (constNode.nodeId == NodeTypes.COLON2NODE) {
            RubyModule module = (RubyModule)evalInternal(runtime,context, ((Colon2Node) iVisited.getConstNode()).getLeftNode(), self, aBlock);
            return context.setConstantInModule(iVisited.getName(), module, result);
        } else { // colon3
            return context.setConstantInObject(iVisited.getName(), result);
        }
    }

    private static IRubyObject constNode(ThreadContext context, Node node) {
        return context.getConstant(((ConstNode)node).getName());
    }

    private static IRubyObject dAsgnNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        DAsgnNode iVisited = (DAsgnNode) node;
   
        IRubyObject result = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);

        // System.out.println("DSetting: " + iVisited.getName() + " at index " + iVisited.getIndex() + " and at depth " + iVisited.getDepth() + " and set " + result);
        context.getCurrentScope().setValue(iVisited.getIndex(), result, iVisited.getDepth());
   
        return result;
    }

    private static IRubyObject definedNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        DefinedNode iVisited = (DefinedNode) node;
        String definition = getDefinition(runtime, context, iVisited.getExpressionNode(), self, aBlock);
        if (definition != null) {
            return runtime.newString(definition);
        } else {
            return runtime.getNil();
        }
    }

    private static IRubyObject defnNode(Ruby runtime, ThreadContext context, Node node) {
        DefnNode iVisited = (DefnNode) node;
        
        RubyModule containingClass = context.getRubyClass();
   
        if (containingClass == null) {
            throw runtime.newTypeError("No class to add method.");
        }
   
        String name = iVisited.getName();

        if (containingClass == runtime.getObject() && name == "initialize") {
            runtime.getWarnings().warn("redefining Object#initialize may cause infinite loop");
        }
   
        Visibility visibility = context.getCurrentVisibility();
        if (name == "initialize" || visibility.isModuleFunction() || context.isTopLevel()) {
            visibility = Visibility.PRIVATE;
        }
        
        DefaultMethod newMethod = new DefaultMethod(containingClass, iVisited.getScope(), 
                iVisited.getBodyNode(), (ArgsNode) iVisited.getArgsNode(), visibility, context.peekCRef());
   
        containingClass.addMethod(name, newMethod);
   
        if (context.getCurrentVisibility().isModuleFunction()) {
            containingClass.getSingletonClass().addMethod(
                    name,
                    new WrapperMethod(containingClass.getSingletonClass(), newMethod,
                            Visibility.PUBLIC));
            containingClass.callMethod(context, "singleton_method_added", runtime.newSymbol(name));
        }
   
        // 'class << state.self' and 'class << obj' uses defn as opposed to defs
        if (containingClass.isSingleton()) {
            ((MetaClass) containingClass).getAttachedObject().callMethod(
                    context, "singleton_method_added", runtime.newSymbol(iVisited.getName()));
        } else {
            containingClass.callMethod(context, "method_added", runtime.newSymbol(name));
        }
   
        return runtime.getNil();
    }
    
    private static IRubyObject defsNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        DefsNode iVisited = (DefsNode) node;
        IRubyObject receiver = evalInternal(runtime,context, iVisited.getReceiverNode(), self, aBlock);
   
        RubyClass rubyClass;
   
        if (receiver.isNil()) {
            rubyClass = runtime.getNilClass();
        } else if (receiver == runtime.getTrue()) {
            rubyClass = runtime.getClass("TrueClass");
        } else if (receiver == runtime.getFalse()) {
            rubyClass = runtime.getClass("FalseClass");
        } else {
            if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                throw runtime.newSecurityError("Insecure; can't define singleton method.");
            }
            if (receiver.isFrozen()) {
                throw runtime.newFrozenError("object");
            }
            if (receiver.getMetaClass() == runtime.getFixnum() || receiver.getMetaClass() == runtime.getClass("Symbol")) {
                throw runtime.newTypeError("can't define singleton method \"" + iVisited.getName()
                                           + "\" for " + receiver.getType());
            }
   
            rubyClass = receiver.getSingletonClass();
        }
   
        if (runtime.getSafeLevel() >= 4) {
            Object method = rubyClass.getMethods().get(iVisited.getName());
            if (method != null) {
                throw runtime.newSecurityError("Redefining method prohibited.");
            }
        }
   
        DefaultMethod newMethod = new DefaultMethod(rubyClass, iVisited.getScope(), 
                iVisited.getBodyNode(), (ArgsNode) iVisited.getArgsNode(), 
                Visibility.PUBLIC, context.peekCRef());
   
        rubyClass.addMethod(iVisited.getName(), newMethod);
        receiver.callMethod(context, "singleton_method_added", runtime.newSymbol(iVisited.getName()));
   
        return runtime.getNil();
    }

    private static IRubyObject dotNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        DotNode iVisited = (DotNode) node;
        return RubyRange.newRange(runtime, 
                evalInternal(runtime,context, iVisited.getBeginNode(), self, aBlock), 
                evalInternal(runtime,context, iVisited.getEndNode(), self, aBlock), 
                iVisited.isExclusive());
    }

    private static IRubyObject dregexpNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        DRegexpNode iVisited = (DRegexpNode) node;
   
        RubyString string = runtime.newString(new ByteList());
        for (int i = 0; i < iVisited.size(); i++) {
            Node iterNode = iVisited.get(i);
            if (iterNode instanceof StrNode) {
                string.getByteList().append(((StrNode) iterNode).getValue());
            } else {
                string.append(evalInternal(runtime,context, iterNode, self, aBlock));
            }
        }
   
        String lang = null;
        int opts = iVisited.getOptions();
        if((opts & 16) != 0) { // param n
            lang = "n";
        } else if((opts & 48) != 0) { // param s
            lang = "s";
        } else if((opts & 64) != 0) { // param s
            lang = "u";
        }
        
        try {
            return RubyRegexp.newRegexp(runtime, string.toString(), iVisited.getOptions(), lang);
        } catch(jregex.PatternSyntaxException e) {
        //                    System.err.println(iVisited.getValue().toString());
        //                    e.printStackTrace();
            throw runtime.newRegexpError(e.getMessage());
        }
    }
    
    private static IRubyObject dStrNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        DStrNode iVisited = (DStrNode) node;
   
        RubyString string = runtime.newString(new ByteList());
        for (int i = 0; i < iVisited.size(); i++) {
            Node iterNode = iVisited.get(i);
            if (iterNode instanceof StrNode) {
                string.getByteList().append(((StrNode) iterNode).getValue());
            } else {
                string.append(evalInternal(runtime,context, iterNode, self, aBlock));
            }
        }
   
        return string;
    }

    private static IRubyObject dSymbolNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        DSymbolNode iVisited = (DSymbolNode) node;
   
        RubyString string = runtime.newString(new ByteList());
        for (int i = 0; i < iVisited.size(); i++) {
            Node iterNode = iVisited.get(i);
            if (iterNode instanceof StrNode) {
                string.getByteList().append(((StrNode) iterNode).getValue());
            } else {
                string.append(evalInternal(runtime,context, iterNode, self, aBlock));
            }
        }
   
        return runtime.newSymbol(string.toString());
    }

    private static IRubyObject dVarNode(Ruby runtime, ThreadContext context, Node node) {
        DVarNode iVisited = (DVarNode) node;

        // System.out.println("DGetting: " + iVisited.getName() + " at index " + iVisited.getIndex() + " and at depth " + iVisited.getDepth());
        IRubyObject obj = context.getCurrentScope().getValue(iVisited.getIndex(), iVisited.getDepth());

        // FIXME: null check is removable once we figure out how to assign to unset named block args
        return obj == null ? runtime.getNil() : obj;
    }

    private static IRubyObject dXStrNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        DXStrNode iVisited = (DXStrNode) node;
   
        RubyString string = runtime.newString(new ByteList());
        for (int i = 0; i < iVisited.size(); i++) {
            Node iterNode = iVisited.get(i);
            if (iterNode instanceof StrNode) {
                string.getByteList().append(((StrNode) iterNode).getValue());
            } else {
                string.append(evalInternal(runtime,context, iterNode, self, aBlock));
            }
        }
   
        return self.callMethod(context, "`", string);
    }

    private static IRubyObject ensureNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        EnsureNode iVisited = (EnsureNode) node;
        
        // save entering the try if there's nothing to ensure
        if (iVisited.getEnsureNode() != null) {
            IRubyObject result = runtime.getNil();

            try {
                result = evalInternal(runtime,context, iVisited.getBodyNode(), self, aBlock);
            } finally {
                evalInternal(runtime,context, iVisited.getEnsureNode(), self, aBlock);
            }

            return result;
        }

        node = iVisited.getBodyNode();
        return evalInternal(runtime, context, node, self, aBlock);
    }

    private static IRubyObject evStrNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        return evalInternal(runtime,context, ((EvStrNode)node).getBody(), self, aBlock).asString();
    }
    
    private static IRubyObject falseNode(Ruby runtime, ThreadContext context) {
        return pollAndReturn(context, runtime.getFalse());
    }

    private static IRubyObject fCallNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        FCallNode iVisited = (FCallNode) node;
        
        IRubyObject[] args = setupArgs(runtime, context, iVisited.getArgsNode(), self);
        Block block = getBlock(runtime, context, self, aBlock, iVisited.getIterNode());
        
        String name = iVisited.getName();
        int index = iVisited.index;

        // No block provided lets look at fast path for STI dispatch.
        if (!block.isGiven()) {
            RubyModule module = self.getMetaClass();
            if (module.index != 0 && index != 0) {
                return self.callMethod(context, module, iVisited.index, name, args, CallType.FUNCTIONAL, Block.NULL_BLOCK);
            } else {
                DynamicMethod method = module.searchMethod(name);
                if (method.isUndefined() || (!method.isCallableFrom(self, CallType.FUNCTIONAL))) {
                    return RubyObject.callMethodMissing(context, self, method, name, args, self, CallType.FUNCTIONAL, Block.NULL_BLOCK);
                }

                return method.call(context, self, module, name, args, false, Block.NULL_BLOCK);
            }
        }

        while (true) {
            try {
                RubyModule module = self.getMetaClass();
                IRubyObject result = self.callMethod(context, module, name, args,
                                                     CallType.FUNCTIONAL, block);
                if (result == null) {
                    result = runtime.getNil();
                }
                    
                return result; 
            } catch (JumpException je) {
                switch (je.getJumpType().getTypeId()) {
                case JumpType.RETRY:
                    // allow loop to retry
                    break;
                case JumpType.BREAK:
                    // JRUBY-530, Kernel#loop case:
                    if (je.isBreakInKernelLoop()) {
                        // consume and rethrow or just keep rethrowing?
                        if (block == je.getTarget()) je.setBreakInKernelLoop(false);
                            
                        throw je;
                    }
                        
                    return (IRubyObject) je.getValue();
                default:
                    throw je;
                }
            }
        }
    }

    private static IRubyObject fixnumNode(Ruby runtime, Node node) {
        return ((FixnumNode)node).getFixnum(runtime);
    }

    private static IRubyObject flipNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        FlipNode iVisited = (FlipNode) node;
        IRubyObject result = runtime.getNil();
   
        if (iVisited.isExclusive()) {
            if (!context.getCurrentScope().getValue(iVisited.getIndex(), iVisited.getDepth()).isTrue()) {
                result = evalInternal(runtime,context, iVisited.getBeginNode(), self, aBlock).isTrue() ? runtime.getFalse()
                        : runtime.getTrue();
                context.getCurrentScope().setValue(iVisited.getIndex(), result, iVisited.getDepth());
                return result;
            } else {
                if (evalInternal(runtime,context, iVisited.getEndNode(), self, aBlock).isTrue()) {
                    context.getCurrentScope().setValue(iVisited.getIndex(), runtime.getFalse(), iVisited.getDepth());
                }
                return runtime.getTrue();
            }
        } else {
            if (!context.getCurrentScope().getValue(iVisited.getIndex(), iVisited.getDepth()).isTrue()) {
                if (evalInternal(runtime,context, iVisited.getBeginNode(), self, aBlock).isTrue()) {
                    context.getCurrentScope().setValue(
                            iVisited.getIndex(),
                            evalInternal(runtime,context, iVisited.getEndNode(), self, aBlock).isTrue() ? runtime.getFalse()
                                    : runtime.getTrue(), iVisited.getDepth());
                    return runtime.getTrue();
                } else {
                    return runtime.getFalse();
                }
            } else {
                if (evalInternal(runtime,context, iVisited.getEndNode(), self, aBlock).isTrue()) {
                    context.getCurrentScope().setValue(iVisited.getIndex(), runtime.getFalse(), iVisited.getDepth());
                }
                return runtime.getTrue();
            }
        }
    }

    private static IRubyObject floatNode(Ruby runtime, Node node) {
        return RubyFloat.newFloat(runtime, ((FloatNode)node).getValue());
    }

    private static IRubyObject forNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        ForNode iVisited = (ForNode) node;
        
        Block block = SharedScopeBlock.createSharedScopeBlock(context, iVisited, 
                context.getCurrentScope(), self);
   
        try {
            while (true) {
                try {
                    ISourcePosition position = context.getPosition();
   
                    IRubyObject recv = null;
                    try {
                        recv = evalInternal(runtime,context, iVisited.getIterNode(), self, aBlock);
                    } finally {
                        context.setPosition(position);
                    }
   
                    return recv.callMethod(context, "each", IRubyObject.NULL_ARRAY, CallType.NORMAL, block);
                } catch (JumpException je) {
                    switch (je.getJumpType().getTypeId()) {
                    case JumpType.RETRY:
                        // do nothing, allow loop to retry
                        break;
                    default:
                        throw je;
                    }
                }
            }
        } catch (JumpException je) {
            switch (je.getJumpType().getTypeId()) {
            case JumpType.BREAK:
                return (IRubyObject) je.getValue();
            default:
                throw je;
            }
        }
    }

    private static IRubyObject globalAsgnNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        GlobalAsgnNode iVisited = (GlobalAsgnNode) node;
   
        IRubyObject result = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
        
        if (iVisited.getName().length() == 2) {
            switch (iVisited.getName().charAt(1)) {
            case '_':
                context.getCurrentScope().setLastLine(result);
                return result;
            case '~':
                context.setBackref(result);
                return result;
            }
        }
   
        runtime.getGlobalVariables().set(iVisited.getName(), result);
   
        // FIXME: this should be encapsulated along with the set above
        if (iVisited.getName() == "$KCODE") {
            runtime.setKCode(KCode.create(runtime, result.toString()));
        }
   
        return result;
    }

    private static IRubyObject globalVarNode(Ruby runtime, ThreadContext context, Node node) {
        GlobalVarNode iVisited = (GlobalVarNode) node;
        
        if (iVisited.getName().length() == 2) {
            IRubyObject value = null;
            switch (iVisited.getName().charAt(1)) {
            case '_':
                value = context.getCurrentScope().getLastLine();
                if (value == null) {
                    return runtime.getNil();
                }
                return value;
            case '~':
                value = context.getCurrentScope().getBackRef();
                if (value == null) {
                    return runtime.getNil();
                }
                return value;
            }
        }
        
        return runtime.getGlobalVariables().get(iVisited.getName());
    }

    private static IRubyObject hashNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        HashNode iVisited = (HashNode) node;
   
        RubyHash hash = null;
        if (iVisited.getListNode() != null) {
            hash = RubyHash.newHash(runtime);
   
        for (int i = 0; i < iVisited.getListNode().size();) {
                // insert all nodes in sequence, hash them in the final instruction
                // KEY
                IRubyObject key = evalInternal(runtime,context, iVisited.getListNode().get(i++), self, aBlock);
                IRubyObject value = evalInternal(runtime,context, iVisited.getListNode().get(i++), self, aBlock);
   
                hash.fastASet(key, value);
            }
        }
   
        if (hash == null) {
            return RubyHash.newHash(runtime);
        }
   
        return hash;
    }

    private static IRubyObject instAsgnNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        InstAsgnNode iVisited = (InstAsgnNode) node;
   
        IRubyObject result = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
        self.setInstanceVariable(iVisited.getName(), result);
   
        return result;
    }

    private static IRubyObject instVarNode(Ruby runtime, Node node, IRubyObject self) {
        InstVarNode iVisited = (InstVarNode) node;
        IRubyObject variable = self.getInstanceVariable(iVisited.getName());
   
        return variable == null ? runtime.getNil() : variable;
    }

    private static IRubyObject localAsgnNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        LocalAsgnNode iVisited = (LocalAsgnNode) node;
        IRubyObject result = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
        
        //System.out.println("LSetting: " + iVisited.getName() + " at index " + iVisited.getIndex() + " and at depth " + iVisited.getDepth() + " and set " + result);
        context.getCurrentScope().setValue(iVisited.getIndex(), result, iVisited.getDepth());

        return result;
    }

    private static IRubyObject localVarNode(Ruby runtime, ThreadContext context, Node node) {
        LocalVarNode iVisited = (LocalVarNode) node;

        //        System.out.println("DGetting: " + iVisited.getName() + " at index " + iVisited.getIndex() + " and at depth " + iVisited.getDepth());
        IRubyObject result = context.getCurrentScope().getValue(iVisited.getIndex(), iVisited.getDepth());

        return result == null ? runtime.getNil() : result;
    }

    private static IRubyObject match2Node(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        Match2Node iVisited = (Match2Node) node;
        IRubyObject recv = evalInternal(runtime,context, iVisited.getReceiverNode(), self, aBlock);
        IRubyObject value = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
   
        return ((RubyRegexp) recv).match(value);
    }
    
    private static IRubyObject match3Node(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        Match3Node iVisited = (Match3Node) node;
        IRubyObject recv = evalInternal(runtime,context, iVisited.getReceiverNode(), self, aBlock);
        IRubyObject value = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
   
        if (value instanceof RubyString) {
            return ((RubyRegexp) recv).match(value);
        } else {
            return value.callMethod(context, "=~", recv);
        }
    }

    private static IRubyObject matchNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        return ((RubyRegexp) evalInternal(runtime,context, ((MatchNode)node).getRegexpNode(), self, aBlock)).match2();
    }

    private static IRubyObject moduleNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        ModuleNode iVisited = (ModuleNode) node;
        Node classNameNode = iVisited.getCPath();
        String name = ((INameNode) classNameNode).getName();
        RubyModule enclosingModule = getEnclosingModule(runtime, context, classNameNode, self, aBlock);
   
        if (enclosingModule == null) {
            throw runtime.newTypeError("no outer class/module");
        }
   
        RubyModule module;
        if (enclosingModule == runtime.getObject()) {
            module = runtime.getOrCreateModule(name);
        } else {
            module = enclosingModule.defineModuleUnder(name);
        }
        return evalClassDefinitionBody(runtime, context, iVisited.getScope(), iVisited.getBodyNode(), module, self, aBlock);
    }

    private static IRubyObject multipleAsgnNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        MultipleAsgnNode iVisited = (MultipleAsgnNode) node;
        
        switch (iVisited.getValueNode().nodeId) {
        case NodeTypes.ARRAYNODE: {
            ArrayNode iVisited2 = (ArrayNode) iVisited.getValueNode();
            IRubyObject[] array = new IRubyObject[iVisited2.size()];

            for (int i = 0; i < iVisited2.size(); i++) {
                Node next = iVisited2.get(i);

                array[i] = evalInternal(runtime,context, next, self, aBlock);
            }
            return AssignmentVisitor.multiAssign(runtime, context, self, iVisited, RubyArray.newArrayNoCopyLight(runtime, array), false);
        }
        case NodeTypes.SPLATNODE: {
            SplatNode splatNode = (SplatNode)iVisited.getValueNode();
            RubyArray rubyArray = splatValue(runtime, evalInternal(runtime, context, ((SplatNode) splatNode).getValue(), self, aBlock));
            return AssignmentVisitor.multiAssign(runtime, context, self, iVisited, rubyArray, false);
        }
        default:
            IRubyObject value = evalInternal(runtime, context, iVisited.getValueNode(), self, aBlock);

            if (!(value instanceof RubyArray)) {
                value = RubyArray.newArray(runtime, value);
            }
            
            return AssignmentVisitor.multiAssign(runtime, context, self, iVisited, (RubyArray)value, false);
        }
    }

    private static IRubyObject nextNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        NextNode iVisited = (NextNode) node;
   
        context.pollThreadEvents();
   
        IRubyObject result = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
   
        // now used as an interpreter event
        throw context.prepareJumpException(JumpException.JumpType.NextJump, iVisited, result);
    }

    private static IRubyObject nilNode(Ruby runtime, ThreadContext context) {
        return pollAndReturn(context, runtime.getNil());
    }

    private static IRubyObject notNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        NotNode iVisited = (NotNode) node;
   
        IRubyObject result = evalInternal(runtime,context, iVisited.getConditionNode(), self, aBlock);
        return result.isTrue() ? runtime.getFalse() : runtime.getTrue();
    }

    private static IRubyObject nthRefNode(ThreadContext context, Node node) {
        return RubyRegexp.nth_match(((NthRefNode)node).getMatchNumber(), context.getBackref());
    }
    
    private static IRubyObject pollAndReturn(ThreadContext context, IRubyObject result) {
        context.pollThreadEvents();
        return result;
    }

    private static IRubyObject opAsgnNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        OpAsgnNode iVisited = (OpAsgnNode) node;
        IRubyObject receiver = evalInternal(runtime,context, iVisited.getReceiverNode(), self, aBlock);
        IRubyObject value = receiver.callMethod(context, iVisited.getVariableName());
   
        if (iVisited.getOperatorName() == "||") {
            if (value.isTrue()) {
                return pollAndReturn(context, value);
            }
            value = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
        } else if (iVisited.getOperatorName() == "&&") {
            if (!value.isTrue()) {
                return pollAndReturn(context, value);
            }
            value = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
        } else {
            value = value.callMethod(context, iVisited.index, iVisited.getOperatorName(), evalInternal(runtime,context,
                    iVisited.getValueNode(), self, aBlock));
        }
   
        receiver.callMethod(context, iVisited.getVariableNameAsgn(), value);
   
        return pollAndReturn(context, value);
    }

    private static IRubyObject opAsgnOrNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        OpAsgnOrNode iVisited = (OpAsgnOrNode) node;
        String def = getDefinition(runtime, context, iVisited.getFirstNode(), self, aBlock);
   
        IRubyObject result = runtime.getNil();
        if (def != null) {
            result = evalInternal(runtime,context, iVisited.getFirstNode(), self, aBlock);
        }
        if (!result.isTrue()) {
            result = evalInternal(runtime,context, iVisited.getSecondNode(), self, aBlock);
        }
   
        return pollAndReturn(context, result);
    }

    private static IRubyObject opElementAsgnNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        OpElementAsgnNode iVisited = (OpElementAsgnNode) node;
        IRubyObject receiver = evalInternal(runtime,context, iVisited.getReceiverNode(), self, aBlock);
   
        IRubyObject[] args = setupArgs(runtime, context, iVisited.getArgsNode(), self);
   
        IRubyObject firstValue = receiver.callMethod(context, MethodIndex.AREF, "[]", args);
   
        if (iVisited.getOperatorName() == "||") {
            if (firstValue.isTrue()) {
                return firstValue;
            }
            firstValue = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
        } else if (iVisited.getOperatorName() == "&&") {
            if (!firstValue.isTrue()) {
                return firstValue;
            }
            firstValue = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
        } else {
            firstValue = firstValue.callMethod(context, iVisited.index, iVisited.getOperatorName(), evalInternal(runtime,context, iVisited
                            .getValueNode(), self, aBlock));
        }
   
        IRubyObject[] expandedArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, expandedArgs, 0, args.length);
        expandedArgs[expandedArgs.length - 1] = firstValue;
        return receiver.callMethod(context, MethodIndex.ASET, "[]=", expandedArgs);
    }

    private static IRubyObject optNNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        OptNNode iVisited = (OptNNode) node;
   
        IRubyObject result = runtime.getNil();
        outerLoop: while (RubyKernel.gets(runtime.getTopSelf(), IRubyObject.NULL_ARRAY).isTrue()) {
            loop: while (true) { // Used for the 'redo' command
                try {
                    result = evalInternal(runtime,context, iVisited.getBodyNode(), self, aBlock);
                    break;
                } catch (JumpException je) {
                    switch (je.getJumpType().getTypeId()) {
                    case JumpType.REDO:
                        // do nothing, this iteration restarts
                        break;
                    case JumpType.NEXT:
                        // recheck condition
                        break loop;
                    case JumpType.BREAK:
                        // end loop
                        result = (IRubyObject) je.getValue();
                        break outerLoop;
                    default:
                        throw je;
                    }
                }
            }
        }
        
        return pollAndReturn(context, result);
    }

    private static IRubyObject orNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        OrNode iVisited = (OrNode) node;
   
        IRubyObject result = evalInternal(runtime,context, iVisited.getFirstNode(), self, aBlock);
   
        if (!result.isTrue()) {
            result = evalInternal(runtime,context, iVisited.getSecondNode(), self, aBlock);
        }
   
        return result;
    }

    private static IRubyObject postExeNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        PostExeNode iVisited = (PostExeNode) node;
        
        // FIXME: I use a for block to implement END node because we need a proc which captures
        // its enclosing scope.   ForBlock now represents these node and should be renamed.
        Block block = SharedScopeBlock.createSharedScopeBlock(context, iVisited, context.getCurrentScope(), self);
        
        runtime.pushExitBlock(runtime.newProc(true, block));
        
        return runtime.getNil();
    }

    private static IRubyObject redoNode(ThreadContext context, Node node) {
        context.pollThreadEvents();
   
        // now used as an interpreter event
        throw context.prepareJumpException(JumpException.JumpType.RedoJump, null, node);
    }

    private static IRubyObject regexpNode(Ruby runtime, Node node) {
        RegexpNode iVisited = (RegexpNode) node;
        String lang = null;
        int opts = iVisited.getOptions();
        if((opts & 16) != 0) { // param n
            lang = "n";
        } else if((opts & 48) != 0) { // param s
            lang = "s";
        } else if((opts & 64) != 0) { // param s
            lang = "u";
        }
        
        IRubyObject noCaseGlobal = runtime.getGlobalVariables().get("$=");
        
        int extraOptions = noCaseGlobal.isTrue() ? ReOptions.RE_OPTION_IGNORECASE : 0;

        try {
            return RubyRegexp.newRegexp(runtime, iVisited.getValue(), 
                    iVisited.getPattern(extraOptions), iVisited.getFlags(extraOptions), lang);
        } catch(jregex.PatternSyntaxException e) {
            throw runtime.newRegexpError(e.getMessage());
        }
    }

    private static IRubyObject rescueNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        RescueNode iVisited = (RescueNode)node;
        RescuedBlock : while (true) {
            IRubyObject globalExceptionState = runtime.getGlobalVariables().get("$!");
            boolean anotherExceptionRaised = false;
            try {
                // Execute rescue block
                IRubyObject result = evalInternal(runtime,context, iVisited.getBodyNode(), self, aBlock);

                // If no exception is thrown execute else block
                if (iVisited.getElseNode() != null) {
                    if (iVisited.getRescueNode() == null) {
                        runtime.getWarnings().warn(iVisited.getElseNode().getPosition(), "else without rescue is useless");
                    }
                    result = evalInternal(runtime,context, iVisited.getElseNode(), self, aBlock);
                }

                return result;
            } catch (RaiseException raiseJump) {
                RubyException raisedException = raiseJump.getException();
                // TODO: Rubicon TestKernel dies without this line.  A cursory glance implies we
                // falsely set $! to nil and this sets it back to something valid.  This should 
                // get fixed at the same time we address bug #1296484.
                runtime.getGlobalVariables().set("$!", raisedException);

                RescueBodyNode rescueNode = iVisited.getRescueNode();

                while (rescueNode != null) {
                    Node  exceptionNodes = rescueNode.getExceptionNodes();
                    ListNode exceptionNodesList;
                    
                    if (exceptionNodes instanceof SplatNode) {                    
                        exceptionNodesList = (ListNode) evalInternal(runtime,context, exceptionNodes, self, aBlock);
                    } else {
                        exceptionNodesList = (ListNode) exceptionNodes;
                    }
                    
                    if (isRescueHandled(runtime, context, raisedException, exceptionNodesList, self)) {
                        try {
                            return evalInternal(runtime,context, rescueNode, self, aBlock);
                        } catch (JumpException je) {
                            if (je.getJumpType() == JumpException.JumpType.RetryJump) {
                                // should be handled in the finally block below
                                //state.runtime.getGlobalVariables().set("$!", state.runtime.getNil());
                                //state.threadContext.setRaisedException(null);
                                continue RescuedBlock;
                                
                            } else {
                                anotherExceptionRaised = true;
                                throw je;
                            }
                        }
                    }
                    
                    rescueNode = rescueNode.getOptRescueNode();
                }

                // no takers; bubble up
                throw raiseJump;
            } finally {
                // clear exception when handled or retried
                if (!anotherExceptionRaised)
                    runtime.getGlobalVariables().set("$!", globalExceptionState);
            }
        }
    }

    private static IRubyObject retryNode(ThreadContext context) {
        context.pollThreadEvents();
   
        throw context.prepareJumpException(JumpException.JumpType.RetryJump, null, null);
    }
    
    private static IRubyObject returnNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        ReturnNode iVisited = (ReturnNode) node;
   
        IRubyObject result = evalInternal(runtime,context, iVisited.getValueNode(), self, aBlock);
   
        throw context.prepareJumpException(JumpException.JumpType.ReturnJump, context.getFrameJumpTarget(), result);
    }

    private static IRubyObject rootNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        RootNode iVisited = (RootNode) node;
        DynamicScope scope = iVisited.getScope();
        
        // Serialization killed our dynamic scope.  We can just create an empty one
        // since serialization cannot serialize an eval (which is the only thing
        // which is capable of having a non-empty dynamic scope).
        if (scope == null) {
            scope = new DynamicScope(iVisited.getStaticScope());
        }
        
        // Each root node has a top-level scope that we need to push
        context.preRootNode(scope);
        
        // FIXME: Wire up BEGIN and END nodes

        try {
            return evalInternal(runtime, context, iVisited.getBodyNode(), self, aBlock);
        } finally {
            context.postRootNode();
        }
    }

    private static IRubyObject sClassNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        SClassNode iVisited = (SClassNode) node;
        IRubyObject receiver = evalInternal(runtime,context, iVisited.getReceiverNode(), self, aBlock);

        RubyClass singletonClass;

        if (receiver.isNil()) {
            singletonClass = runtime.getNilClass();
        } else if (receiver == runtime.getTrue()) {
            singletonClass = runtime.getClass("TrueClass");
        } else if (receiver == runtime.getFalse()) {
            singletonClass = runtime.getClass("FalseClass");
        } else if (receiver.getMetaClass() == runtime.getFixnum() || receiver.getMetaClass() == runtime.getClass("Symbol")) {
            throw runtime.newTypeError("no virtual class for " + receiver.getMetaClass().getBaseName());
        } else {
            if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                throw runtime.newSecurityError("Insecure: can't extend object.");
            }

            singletonClass = receiver.getSingletonClass();
        }

        return evalClassDefinitionBody(runtime, context, iVisited.getScope(), iVisited.getBodyNode(), singletonClass, self, aBlock);
    }

    private static IRubyObject splatNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        return splatValue(runtime, evalInternal(runtime,context, ((SplatNode) node).getValue(), self, aBlock));
    }

    private static IRubyObject strNode(Ruby runtime, Node node) {
        return runtime.newStringShared((ByteList) ((StrNode) node).getValue());
    }
    
    private static IRubyObject superNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        SuperNode iVisited = (SuperNode) node;
   
        RubyModule klazz = context.getFrameKlazz();
        
        if (klazz == null) {
            String name = context.getFrameName();
            throw runtime.newNameError("Superclass method '" + name
                    + "' disabled.", name);
        }
        IRubyObject[] args = setupArgs(runtime, context, iVisited.getArgsNode(), self);
        Block block = getBlock(runtime, context, self, aBlock, iVisited.getIterNode());
        
        // If no explicit block passed to super, then use the one passed in.
        if (!block.isGiven()) block = aBlock;
        
        return self.callSuper(context, args, block);
    }
    
    private static IRubyObject sValueNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        return aValueSplat(runtime, evalInternal(runtime,context, ((SValueNode) node).getValue(), self, aBlock));
    }
    
    private static IRubyObject symbolNode(Ruby runtime, Node node) {
        return runtime.newSymbol(((SymbolNode) node).getName());
    }
    
    private static IRubyObject toAryNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        return aryToAry(runtime, evalInternal(runtime,context, ((ToAryNode) node).getValue(), self, aBlock));
    }

    private static IRubyObject trueNode(Ruby runtime, ThreadContext context) {
        return pollAndReturn(context, runtime.getTrue());
    }
    
    private static IRubyObject undefNode(Ruby runtime, ThreadContext context, Node node) {
        UndefNode iVisited = (UndefNode) node;
        
   
        if (context.getRubyClass() == null) {
            throw runtime
                    .newTypeError("No class to undef method '" + iVisited.getName() + "'.");
        }
        context.getRubyClass().undef(iVisited.getName());
   
        return runtime.getNil();
    }

    private static IRubyObject untilNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        UntilNode iVisited = (UntilNode) node;
   
        IRubyObject result = runtime.getNil();
        boolean firstTest = iVisited.evaluateAtStart();
        
        outerLoop: while (!firstTest || !(result = evalInternal(runtime,context, iVisited.getConditionNode(), self, aBlock)).isTrue()) {
            firstTest = true;
            loop: while (true) { // Used for the 'redo' command
                try {
                    result = evalInternal(runtime,context, iVisited.getBodyNode(), self, aBlock);
                    break loop;
                } catch (JumpException je) {
                    switch (je.getJumpType().getTypeId()) {
                    case JumpType.REDO:
                        continue;
                    case JumpType.NEXT:
                        break loop;
                    case JumpType.BREAK:
                        // JRUBY-530 until case
                        if (je.getTarget() == aBlock) {
                             je.setTarget(null);
                             
                             throw je;
                        }
                        
                        result = (IRubyObject) je.getValue();
                        
                        break outerLoop;
                    default:
                        throw je;
                    }
                }
            }
        }
        
        return pollAndReturn(context, result);
    }

    private static IRubyObject valiasNode(Ruby runtime, Node node) {
        VAliasNode iVisited = (VAliasNode) node;
        runtime.getGlobalVariables().alias(iVisited.getNewName(), iVisited.getOldName());
   
        return runtime.getNil();
    }

    private static IRubyObject vcallNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self) {
        VCallNode iVisited = (VCallNode) node;
        RubyModule module = self.getMetaClass();
        String name = iVisited.getName();
        int index = iVisited.index;

        if (module.index != 0 && index != 0) {
            return self.callMethod(context, module, index, name, 
                    IRubyObject.NULL_ARRAY, CallType.VARIABLE, Block.NULL_BLOCK);
        } else {
            DynamicMethod method = module.searchMethod(name);
            
            if (method.isUndefined() || (index != MethodIndex.METHOD_MISSING  && !method.isCallableFrom(self, CallType.VARIABLE))) {
                return RubyObject.callMethodMissing(context, self, method, name, index, IRubyObject.NULL_ARRAY, self, CallType.VARIABLE, Block.NULL_BLOCK);
            }

            return method.call(context, self, module, name, IRubyObject.NULL_ARRAY, false, Block.NULL_BLOCK);
        }
    }

    private static IRubyObject whileNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        WhileNode iVisited = (WhileNode) node;
   
        IRubyObject result = runtime.getNil();
        boolean firstTest = iVisited.evaluateAtStart();
        
        outerLoop: while (!firstTest || (result = evalInternal(runtime,context, iVisited.getConditionNode(), self, aBlock)).isTrue()) {
            firstTest = true;
            loop: while (true) { // Used for the 'redo' command
                try {
                    evalInternal(runtime,context, iVisited.getBodyNode(), self, aBlock);
                    break loop;
                } catch (RaiseException re) {
                    if (re.getException().isKindOf(runtime.getClass("LocalJumpError"))) {
                        RubyLocalJumpError jumpError = (RubyLocalJumpError)re.getException();
                        
                        IRubyObject reason = jumpError.reason();
                        
                        // admittedly inefficient
                        if (reason.asSymbol().equals("break")) {
                            return jumpError.exitValue();
                        } else if (reason.asSymbol().equals("next")) {
                            break loop;
                        } else if (reason.asSymbol().equals("redo")) {
                            continue;
                        }
                    }
                    
                    throw re;
                } catch (JumpException je) {
                    switch (je.getJumpType().getTypeId()) {
                    case JumpType.REDO:
                        continue;
                    case JumpType.NEXT:
                        break loop;
                    case JumpType.BREAK:
                        // JRUBY-530, while case
                        if (je.getTarget() == aBlock) {
                            je.setTarget(null);
                            
                            throw je;
                        }
                        
                        break outerLoop;
                    default:
                        throw je;
                    }
                }
            }
        }
        
        return pollAndReturn(context, result);
    }

    private static IRubyObject xStrNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self) {
        return self.callMethod(context, "`", runtime.newStringShared((ByteList) ((XStrNode) node).getValue()));
    }

    private static IRubyObject yieldNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        YieldNode iVisited = (YieldNode) node;
   
        IRubyObject result = null;
        if (iVisited.getArgsNode() != null) {
            result = evalInternal(runtime, context, iVisited.getArgsNode(), self, aBlock);
        }

        Block block = context.getCurrentFrame().getBlock();

        return block.yield(context, result, null, null, iVisited.getCheckState());
    }

    private static IRubyObject zArrayNode(Ruby runtime) {
        return runtime.newArray();
    }
    
    private static IRubyObject zsuperNode(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        if (context.getFrameKlazz() == null) {
            String name = context.getFrameName();
            throw runtime.newNameError("superclass method '" + name
                    + "' disabled", name);
        }

        Block block = getBlock(runtime, context, self, aBlock, ((ZSuperNode) node).getIterNode());

        // Has the method that is calling super received a block argument
        if (!block.isGiven()) block = context.getCurrentFrame().getBlock(); 
        
        context.getCurrentScope().getArgValues(context.getFrameArgs(),context.getCurrentFrame().getRequiredArgCount());
        return self.callSuper(context, context.getFrameArgs(), block);
    }

    public static IRubyObject aValueSplat(Ruby runtime, IRubyObject value) {
        if (!(value instanceof RubyArray) || ((RubyArray) value).length().getLongValue() == 0) {
            return runtime.getNil();
        }

        RubyArray array = (RubyArray) value;

        return array.getLength() == 1 ? array.first(IRubyObject.NULL_ARRAY) : array;
    }

    private static void callTraceFunction(Ruby runtime, ThreadContext context, int event) {
        String name = context.getFrameName();
        RubyModule type = context.getFrameKlazz();
        runtime.callEventHooks(context, event, context.getPosition().getFile(), context.getPosition().getStartLine(), name, type);
    }

    /** Evaluates the body in a class or module definition statement.
     *
     */
    private static IRubyObject evalClassDefinitionBody(Ruby runtime, ThreadContext context, StaticScope scope, 
            Node bodyNode, RubyModule type, IRubyObject self, Block block) {
        context.preClassEval(scope, type);

        try {
            if (isTrace(runtime)) {
                callTraceFunction(runtime, context, EventHook.RUBY_EVENT_CLASS);
            }

            return evalInternal(runtime,context, bodyNode, type, block);
        } finally {
            if (isTrace(runtime)) {
                callTraceFunction(runtime, context, EventHook.RUBY_EVENT_END);
            }
            
            context.postClassEval();
        }
    }

    private static String getArgumentDefinition(Ruby runtime, ThreadContext context, Node node, String type, IRubyObject self, Block block) {
        if (node == null) return type;
            
        if (node instanceof ArrayNode) {
            for (int i = 0; i < ((ArrayNode)node).size(); i++) {
                Node iterNode = ((ArrayNode)node).get(i);
                if (getDefinitionInner(runtime, context, iterNode, self, block) == null) return null;
            }
        } else if (getDefinitionInner(runtime, context, node, self, block) == null) {
            return null;
        }

        return type;
    }
    
    public static Block getBlock(Ruby runtime, ThreadContext context, IRubyObject self, Block currentBlock, Node blockNode) {
        if (blockNode == null) return Block.NULL_BLOCK;
        
        if (blockNode instanceof IterNode) {
            IterNode iterNode = (IterNode) blockNode;
            // Create block for this iter node
            // FIXME: We shouldn't use the current scope if it's not actually from the same hierarchy of static scopes
            return Block.createBlock(context, iterNode,
                    new DynamicScope(iterNode.getScope(), context.getCurrentScope()), self);
        } else if (blockNode instanceof BlockPassNode) {
            BlockPassNode blockPassNode = (BlockPassNode) blockNode;
            IRubyObject proc = evalInternal(runtime,context, blockPassNode.getBodyNode(), self, currentBlock);

            // No block from a nil proc
            if (proc.isNil()) return Block.NULL_BLOCK;

            // If not already a proc then we should try and make it one.
            if (!(proc instanceof RubyProc)) {
                proc = proc.convertToType(runtime.getClass("Proc"), 0, "to_proc", false);

                if (!(proc instanceof RubyProc)) {
                    throw runtime.newTypeError("wrong argument type "
                            + proc.getMetaClass().getName() + " (expected Proc)");
                }
            }

            // TODO: Add safety check for taintedness
            
            if (currentBlock.isGiven()) {
                RubyProc procObject = currentBlock.getProcObject();
                // The current block is already associated with proc.  No need to create a new one
                if (procObject != null && procObject == proc) return currentBlock;
            }
            
            return ((RubyProc) proc).getBlock();
        }
         
        assert false: "Trying to get block from something which cannot deliver";
        return null;
    }

    /* Something like cvar_cbase() from eval.c, factored out for the benefit
     * of all the classvar-related node evaluations */
    public static RubyModule getClassVariableBase(ThreadContext context, Ruby runtime) {
        SinglyLinkedList cref = context.peekCRef();
        RubyModule rubyClass = (RubyModule) cref.getValue();
        if (rubyClass.isSingleton()) {
            cref = cref.getNext();
            rubyClass = (RubyModule) cref.getValue();
            if (cref.getNext() == null) {
                runtime.getWarnings().warn("class variable access from toplevel singleton method");
            }            
        }
        return rubyClass;
    }

    private static String getDefinition(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        try {
            context.setWithinDefined(true);
            return getDefinitionInner(runtime, context, node, self, aBlock);
        } finally {
            context.setWithinDefined(false);
        }
    }

    private static String getDefinitionInner(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        if (node == null) return "expression";
        
        switch(node.nodeId) {
        case NodeTypes.ATTRASSIGNNODE: {
            AttrAssignNode iVisited = (AttrAssignNode) node;
            
            if (getDefinitionInner(runtime, context, iVisited.getReceiverNode(), self, aBlock) != null) {
                try {
                    IRubyObject receiver = eval(runtime, context, iVisited.getReceiverNode(), self, aBlock);
                    RubyClass metaClass = receiver.getMetaClass();
                    DynamicMethod method = metaClass.searchMethod(iVisited.getName());
                    Visibility visibility = method.getVisibility();

                    if (!visibility.isPrivate() && 
                            (!visibility.isProtected() || self.isKindOf(metaClass.getRealClass()))) {
                        if (metaClass.isMethodBound(iVisited.getName(), false)) {
                            return getArgumentDefinition(runtime,context, iVisited.getArgsNode(), "assignment", self, aBlock);
                        }
                    }
                } catch (JumpException excptn) {
                }
            }

            return null;
        }
        case NodeTypes.BACKREFNODE:
            return "$" + ((BackRefNode) node).getType();
        case NodeTypes.CALLNODE: {
            CallNode iVisited = (CallNode) node;
            
            if (getDefinitionInner(runtime, context, iVisited.getReceiverNode(), self, aBlock) != null) {
                try {
                    IRubyObject receiver = eval(runtime, context, iVisited.getReceiverNode(), self, aBlock);
                    RubyClass metaClass = receiver.getMetaClass();
                    DynamicMethod method = metaClass.searchMethod(iVisited.getName());
                    Visibility visibility = method.getVisibility();

                    if (!visibility.isPrivate() && 
                            (!visibility.isProtected() || self.isKindOf(metaClass.getRealClass()))) {
                        if (metaClass.isMethodBound(iVisited.getName(), false)) {
                            return getArgumentDefinition(runtime, context, iVisited.getArgsNode(), "method", self, aBlock);
                        }
                    }
                } catch (JumpException excptn) {
                }
            }

            return null;
        }
        case NodeTypes.CLASSVARASGNNODE: case NodeTypes.CLASSVARDECLNODE: case NodeTypes.CONSTDECLNODE:
        case NodeTypes.DASGNNODE: case NodeTypes.GLOBALASGNNODE: case NodeTypes.LOCALASGNNODE:
        case NodeTypes.MULTIPLEASGNNODE: case NodeTypes.OPASGNNODE: case NodeTypes.OPELEMENTASGNNODE:
            return "assignment";
            
        case NodeTypes.CLASSVARNODE: {
            ClassVarNode iVisited = (ClassVarNode) node;
            
            if (context.getRubyClass() == null && self.getMetaClass().isClassVarDefined(iVisited.getName())) {
                return "class_variable";
            } else if (!context.getRubyClass().isSingleton() && context.getRubyClass().isClassVarDefined(iVisited.getName())) {
                return "class_variable";
            } 
            
            IRubyObject attached =  context.getRubyClass().getInstanceVariable("__attached__");
            if (attached instanceof RubyModule) {
                RubyModule module = (RubyModule)attached;
                if (module.isClassVarDefined(iVisited.getName())) return "class_variable"; 
            }

            return null;
        }
        case NodeTypes.COLON3NODE:
        case NodeTypes.COLON2NODE: {
            Colon3Node iVisited = (Colon3Node) node;

            try {
                IRubyObject left = runtime.getObject();
                if (iVisited instanceof Colon2Node) {
                    left = EvaluationState.eval(runtime, context, ((Colon2Node) iVisited).getLeftNode(), self, aBlock);
                }

                if (left instanceof RubyModule &&
                        ((RubyModule) left).getConstantAt(iVisited.getName()) != null) {
                    return "constant";
                } else if (left.getMetaClass().isMethodBound(iVisited.getName(), true)) {
                    return "method";
                }
            } catch (JumpException excptn) {}
            
            return null;
        }
        case NodeTypes.CONSTNODE:
            if (context.getConstantDefined(((ConstNode) node).getName())) {
                return "constant";
            }
            return null;
        case NodeTypes.DVARNODE:
            return "local-variable(in-block)";
        case NodeTypes.FALSENODE:
            return "false";
        case NodeTypes.FCALLNODE: {
            FCallNode iVisited = (FCallNode) node;
            if (self.getMetaClass().isMethodBound(iVisited.getName(), false)) {
                return getArgumentDefinition(runtime, context, iVisited.getArgsNode(), "method", self, aBlock);
            }
            
            return null;
        }
        case NodeTypes.GLOBALVARNODE:
            if (runtime.getGlobalVariables().isDefined(((GlobalVarNode) node).getName())) {
                return "global-variable";
            }
            return null;
        case NodeTypes.INSTVARNODE:
            if (self.getInstanceVariable(((InstVarNode) node).getName()) != null) {
                return "instance-variable";
            }
            return null;
        case NodeTypes.LOCALVARNODE:
            return "local-variable";
        case NodeTypes.MATCH2NODE: case NodeTypes.MATCH3NODE:
            return "method";
        case NodeTypes.NILNODE:
            return "nil";
        case NodeTypes.NTHREFNODE:
            return "$" + ((NthRefNode) node).getMatchNumber();
        case NodeTypes.SELFNODE:
            return "state.getSelf()";
        case NodeTypes.SUPERNODE: {
            SuperNode iVisited = (SuperNode) node;
            String name = context.getFrameName();
            RubyModule klazz = context.getFrameKlazz();
            if (name != null && klazz != null && klazz.getSuperClass().isMethodBound(name, false)) {
                return getArgumentDefinition(runtime, context, iVisited.getArgsNode(), "super", self, aBlock);
            }
            
            return null;
        }
        case NodeTypes.TRUENODE:
            return "true";
        case NodeTypes.VCALLNODE: {
            VCallNode iVisited = (VCallNode) node;
            if (self.getMetaClass().isMethodBound(iVisited.getName(), false)) {
                return "method";
            }
            
            return null;
        }
        case NodeTypes.YIELDNODE:
            return aBlock.isGiven() ? "yield" : null;
        case NodeTypes.ZSUPERNODE: {
            String name = context.getFrameName();
            RubyModule klazz = context.getFrameKlazz();
            if (name != null && klazz != null && klazz.getSuperClass().isMethodBound(name, false)) {
                return "super";
            }
            return null;
        }
        default:
            try {
                EvaluationState.eval(runtime, context, node, self, aBlock);
                return "expression";
            } catch (JumpException jumpExcptn) {}
        }
        
        return null;
    }

    private static RubyModule getEnclosingModule(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block block) {
        RubyModule enclosingModule = null;

        if (node instanceof Colon2Node) {
            IRubyObject result = evalInternal(runtime,context, ((Colon2Node) node).getLeftNode(), self, block);

            if (result != null && !result.isNil()) {
                enclosingModule = (RubyModule) result;
            }
        } else if (node instanceof Colon3Node) {
            enclosingModule = runtime.getObject();
        }

        if (enclosingModule == null) {
            enclosingModule = (RubyModule) context.peekCRef().getValue();
        }

        return enclosingModule;
    }

    private static boolean isRescueHandled(Ruby runtime, ThreadContext context, RubyException currentException, ListNode exceptionNodes,
            IRubyObject self) {
        if (exceptionNodes == null) {
            return currentException.isKindOf(runtime.getClass("StandardError"));
        }

        IRubyObject[] args = setupArgs(runtime, context, exceptionNodes, self);

        for (int i = 0; i < args.length; i++) {
            if (!args[i].isKindOf(runtime.getClass("Module"))) {
                throw runtime.newTypeError("class or module required for rescue clause");
            }
            if (args[i].callMethod(context, "===", currentException).isTrue()) return true;
        }
        return false;
    }

    /**
     * Helper method.
     *
     * test if a trace function is avaiable.
     *
     */
    private static boolean isTrace(Ruby runtime) {
        return runtime.hasEventHooks();
    }

    private static IRubyObject[] setupArgs(Ruby runtime, ThreadContext context, Node node, IRubyObject self) {
        if (node == null) return IRubyObject.NULL_ARRAY;

        if (node instanceof ArrayNode) {
            ArrayNode argsArrayNode = (ArrayNode) node;
            ISourcePosition position = context.getPosition();
            int size = argsArrayNode.size();
            IRubyObject[] argsArray = new IRubyObject[size];

            for (int i = 0; i < size; i++) {
                argsArray[i] = evalInternal(runtime,context, argsArrayNode.get(i), self, Block.NULL_BLOCK);
            }

            context.setPosition(position);

            return argsArray;
        }

        return ArgsUtil.convertToJavaArray(evalInternal(runtime,context, node, self, Block.NULL_BLOCK));
    }

    public static RubyArray splatValue(Ruby runtime, IRubyObject value) {
        if (value.isNil()) {
            return runtime.newArray(value);
        }

        return arrayValue(runtime, value);
    }
}
