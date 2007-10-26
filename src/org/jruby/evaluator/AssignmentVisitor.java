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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2003-2004 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.evaluator;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeType;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 */
public class AssignmentVisitor {
    public static IRubyObject assign(Ruby runtime, ThreadContext context, IRubyObject self, Node node, IRubyObject value, Block block, boolean checkArity) {
        IRubyObject result = null;
        
        switch (node.nodeId) {
        case ATTRASSIGNNODE:
            attrAssignNode(runtime, context, self, node, value, block);
            break;
        case CALLNODE:
            callNode(runtime, context, self, node, value, block);
            break;
        case CLASSVARASGNNODE:
            classVarAsgnNode(context, node, value);
            break;
        case CLASSVARDECLNODE:
            classVarDeclNode(runtime, context, node, value);
            break;
        case CONSTDECLNODE:
            constDeclNode(runtime, context, self, node, value, block);
            break;
        case DASGNNODE:
            dasgnNode(context, node, value);
            break;
        case GLOBALASGNNODE:
            globalAsgnNode(runtime, node, value);
            break;
        case INSTASGNNODE:
            instAsgnNode(self, node, value);
            break;
        case LOCALASGNNODE:
            localAsgnNode(context, node, value);
            break;
        case MULTIPLEASGNNODE:
            result = multipleAsgnNode(runtime, context, self, node, value, checkArity);
            break;
        default:
            throw new RuntimeException("Invalid node encountered in interpreter: \"" + node.getClass().getName() + "\", please report this at www.jruby.org");
        }

        return result;
    }

    private static void attrAssignNode(Ruby runtime, ThreadContext context, IRubyObject self, Node node, IRubyObject value, Block block) {
        AttrAssignNode iVisited = (AttrAssignNode) node;
        
        IRubyObject receiver = ASTInterpreter.eval(runtime, context, iVisited.getReceiverNode(), self, block);
        
        // If reciever is self then we do the call the same way as vcall
        CallType callType = (receiver == self ? CallType.VARIABLE : CallType.NORMAL);

        if (iVisited.getArgsNode() == null) { // attribute set.
            receiver.callMethod(context, iVisited.getName(), new IRubyObject[] {value}, callType);
        } else { // element set
            RubyArray args = (RubyArray)ASTInterpreter.eval(runtime, context, iVisited.getArgsNode(), self, block);
            args.append(value);
            receiver.callMethod(context, iVisited.getName(), args.toJavaArray(), callType);
        }
    }

    private static void callNode(Ruby runtime, ThreadContext context, IRubyObject self, Node node, IRubyObject value, Block block) {
        CallNode iVisited = (CallNode)node;
        
        IRubyObject receiver = ASTInterpreter.eval(runtime, context, iVisited.getReceiverNode(), self, block);

        if (iVisited.getArgsNode() == null) { // attribute set.
            receiver.callMethod(context, iVisited.getName(), new IRubyObject[] {value}, CallType.NORMAL);
        } else { // element set
            RubyArray args = (RubyArray)ASTInterpreter.eval(runtime, context, iVisited.getArgsNode(), self, block);
            args.append(value);
            receiver.callMethod(context, iVisited.getName(), args.toJavaArray(), CallType.NORMAL);
        }
    }

    private static void classVarAsgnNode(ThreadContext context, Node node, IRubyObject value) {
        ClassVarAsgnNode iVisited = (ClassVarAsgnNode)node;
        RubyModule rubyClass = ASTInterpreter.getClassVariableBase(context, context.getRuntime());
        rubyClass.fastSetClassVar(iVisited.getName(), value);
    }

    private static void classVarDeclNode(Ruby runtime, ThreadContext context, Node node, IRubyObject value) {
        ClassVarDeclNode iVisited = (ClassVarDeclNode)node;
        if (runtime.getVerbose().isTrue()
                && context.getRubyClass().isSingleton()) {
            runtime.getWarnings().warn(iVisited.getPosition(),
                    "Declaring singleton class variable.");
        }
        RubyModule rubyClass = ASTInterpreter.getClassVariableBase(context, context.getRuntime());
        rubyClass.fastSetClassVar(iVisited.getName(), value);
    }

    private static void constDeclNode(Ruby runtime, ThreadContext context, IRubyObject self, Node node, IRubyObject value, Block block) {
        ConstDeclNode iVisited = (ConstDeclNode)node;
        Node constNode = iVisited.getConstNode();

        IRubyObject module;

        if (constNode == null) {
            module = context.getCurrentScope().getStaticScope().getModule();
            
            if (module == null) {
                // TODO: wire into new exception handling mechanism
                throw runtime.newTypeError("no class/module to define constant");
            }
        } else if (constNode instanceof Colon2Node) {
            module = ASTInterpreter.eval(runtime, context, ((Colon2Node) iVisited.getConstNode()).getLeftNode(), self, block);
        } else { // Colon3
            module = runtime.getObject();
        } 

        ((RubyModule) module).fastSetConstant(iVisited.getName(), value);
    }

    private static void dasgnNode(ThreadContext context, Node node, IRubyObject value) {
        DAsgnNode iVisited = (DAsgnNode)node;
        context.getCurrentScope().setValue(iVisited.getIndex(), value, iVisited.getDepth());
    }

    private static void globalAsgnNode(Ruby runtime, Node node, IRubyObject value) {
        GlobalAsgnNode iVisited = (GlobalAsgnNode)node;
        runtime.getGlobalVariables().set(iVisited.getName(), value);
    }

    private static void instAsgnNode(IRubyObject self, Node node, IRubyObject value) {
        InstAsgnNode iVisited = (InstAsgnNode)node;
        self.fastSetInstanceVariable(iVisited.getName(), value);
    }

    private static void localAsgnNode(ThreadContext context, Node node, IRubyObject value) {
        LocalAsgnNode iVisited = (LocalAsgnNode)node;
        
        context.getCurrentScope().setValue(iVisited.getIndex(), value, iVisited.getDepth());
    }

    public static IRubyObject multiAssign(Ruby runtime, ThreadContext context, IRubyObject self, MultipleAsgnNode node, RubyArray value, boolean checkArity) {
        // Assign the values.
        int valueLen = value.getLength();
        int varLen = node.getHeadNode() == null ? 0 : node.getHeadNode().size();
        
        int j = 0;
        for (; j < valueLen && j < varLen; j++) {
            Node lNode = node.getHeadNode().get(j);
            assign(runtime, context, self, lNode, value.eltInternal(j), Block.NULL_BLOCK, checkArity);
        }

        if (checkArity && j < varLen) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        Node argsNode = node.getArgsNode();
        if (argsNode != null) {
            if (argsNode.nodeId == NodeType.STARNODE) {
                // no check for '*'
            } else if (varLen < valueLen) {
                assign(runtime, context, self, argsNode, value.subseqLight(varLen, valueLen), Block.NULL_BLOCK, checkArity);
            } else {
                assign(runtime, context, self, argsNode, RubyArray.newArrayLight(runtime, 0), Block.NULL_BLOCK, checkArity);
            }
        } else if (checkArity && valueLen < varLen) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        while (j < varLen) {
            assign(runtime, context, self, node.getHeadNode().get(j++), runtime.getNil(), Block.NULL_BLOCK, checkArity);
        }
        
        return value;
    }
    
    private static IRubyObject multipleAsgnNode(Ruby runtime, ThreadContext context, IRubyObject self, Node node, IRubyObject value, boolean check) {
        IRubyObject result;
        MultipleAsgnNode iVisited = (MultipleAsgnNode)node;
        if (!(value instanceof RubyArray)) {
            value = ArgsUtil.convertToRubyArray(runtime, value, iVisited.getHeadNode() != null);
        }
        result = multiAssign(runtime, context, self, iVisited, (RubyArray) value, check);
        return result;
    }
}
