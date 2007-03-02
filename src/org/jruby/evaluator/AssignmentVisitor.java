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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
import org.jruby.ast.NodeTypes;
import org.jruby.ast.StarNode;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 */
public class AssignmentVisitor {
    public static IRubyObject assign(Ruby runtime, ThreadContext context, IRubyObject self, Node node, IRubyObject value, Block block, boolean check) {
        IRubyObject result = null;
        
        switch (node.nodeId) {
        case NodeTypes.ATTRASSIGNNODE: {
            AttrAssignNode iVisited = (AttrAssignNode) node;
            
            IRubyObject receiver = EvaluationState.eval(runtime, context, iVisited.getReceiverNode(), self, block);
            
            // If reciever is self then we do the call the same way as vcall
            CallType callType = (receiver == self ? CallType.VARIABLE : CallType.NORMAL);

            if (iVisited.getArgsNode() == null) { // attribute set.
                receiver.callMethod(context, iVisited.getName(), new IRubyObject[] {value}, callType);
            } else { // element set
                RubyArray args = (RubyArray)EvaluationState.eval(runtime, context, iVisited.getArgsNode(), self, block);
                args.append(value);
                receiver.callMethod(context, iVisited.getName(), args.toJavaArray(), callType);
            }
            break;
        }
        case NodeTypes.CALLNODE: {
            CallNode iVisited = (CallNode)node;
            
            IRubyObject receiver = EvaluationState.eval(runtime, context, iVisited.getReceiverNode(), self, block);

            if (iVisited.getArgsNode() == null) { // attribute set.
                receiver.callMethod(context, iVisited.getName(), new IRubyObject[] {value}, CallType.NORMAL);
            } else { // element set
                RubyArray args = (RubyArray)EvaluationState.eval(runtime, context, iVisited.getArgsNode(), self, block);
                args.append(value);
                receiver.callMethod(context, iVisited.getName(), args.toJavaArray(), CallType.NORMAL);
            }
            break;
        }
        case NodeTypes.CLASSVARASGNNODE: {
            ClassVarAsgnNode iVisited = (ClassVarAsgnNode)node;
            context.getRubyClass().setClassVar(iVisited.getName(), value);
            break;
        }
        case NodeTypes.CLASSVARDECLNODE: {
            ClassVarDeclNode iVisited = (ClassVarDeclNode)node;
            if (runtime.getVerbose().isTrue()
                    && context.getRubyClass().isSingleton()) {
                runtime.getWarnings().warn(iVisited.getPosition(),
                        "Declaring singleton class variable.");
            }
            context.getRubyClass().setClassVar(iVisited.getName(), value);
            break;
        }
        case NodeTypes.CONSTDECLNODE: {
            ConstDeclNode iVisited = (ConstDeclNode)node;
            Node constNode = iVisited.getConstNode();

            IRubyObject module;

            if (constNode == null) {
                // FIXME: why do we check RubyClass and then use CRef?
                if (context.getRubyClass() == null) {
                    // TODO: wire into new exception handling mechanism
                    throw runtime.newTypeError("no class/module to define constant");
                }
                module = (RubyModule) context.peekCRef().getValue();
            } else if (constNode instanceof Colon2Node) {
                module = EvaluationState.eval(runtime, context, ((Colon2Node) iVisited.getConstNode()).getLeftNode(), self, block);
            } else { // Colon3
                module = runtime.getObject();
            } 

            ((RubyModule) module).setConstant(iVisited.getName(), value);
            break;
        }
        case NodeTypes.DASGNNODE: {
            DAsgnNode iVisited = (DAsgnNode)node;
            context.getCurrentScope().setValue(iVisited.getIndex(), value, iVisited.getDepth());
            break;
        }
        case NodeTypes.GLOBALASGNNODE: {
            GlobalAsgnNode iVisited = (GlobalAsgnNode)node;
            runtime.getGlobalVariables().set(iVisited.getName(), value);
            break;
        }
        case NodeTypes.INSTASGNNODE: {
            InstAsgnNode iVisited = (InstAsgnNode)node;
            self.setInstanceVariable(iVisited.getName(), value);
            break;
        }
        case NodeTypes.LOCALASGNNODE: {
            LocalAsgnNode iVisited = (LocalAsgnNode)node;
            
            //System.out.println("Assigning to " + iVisited.getName() + "@"+ iVisited.getPosition());
            //context.printScope();
            context.getCurrentScope().setValue(iVisited.getIndex(), value, iVisited.getDepth());
            break;
        }
        case NodeTypes.MULTIPLEASGNNODE: {
            MultipleAsgnNode iVisited = (MultipleAsgnNode)node;
            if (!(value instanceof RubyArray)) {
                value = RubyArray.newArray(runtime, value);
            }
            result = multiAssign(runtime, context, self, iVisited, (RubyArray) value, check);
            break;
        }
        default:
            throw new RuntimeException("Invalid node encountered in interpreter: \"" + node.getClass().getName() + "\", please report this at www.jruby.org");
        }

        return result;
    }
    
    public static IRubyObject multiAssign(Ruby runtime, ThreadContext context, IRubyObject self, MultipleAsgnNode node, RubyArray value, boolean callAsProc) {
        // Assign the values.
        int valueLen = value.getLength();
        int varLen = node.getHeadNode() == null ? 0 : node.getHeadNode().size();
        
        Iterator iter = node.getHeadNode() != null ? node.getHeadNode().iterator() : Collections.EMPTY_LIST.iterator();
        for (int i = 0; i < valueLen && iter.hasNext(); i++) {
            Node lNode = (Node) iter.next();
            assign(runtime, context, self, lNode, value.entry(i), Block.NULL_BLOCK, callAsProc);
        }

        if (callAsProc && iter.hasNext()) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        Node argsNode = node.getArgsNode();
        if (argsNode != null) {
            if (argsNode instanceof StarNode) {
                // no check for '*'
            } else if (varLen < valueLen) {
                ArrayList newList = new ArrayList(value.getList().subList(varLen, valueLen));
                assign(runtime, context, self, argsNode, runtime.newArray(newList), Block.NULL_BLOCK, callAsProc);
            } else {
                assign(runtime, context, self, argsNode, runtime.newArray(0), Block.NULL_BLOCK, callAsProc);
            }
        } else if (callAsProc && valueLen < varLen) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        while (iter.hasNext()) {
            assign(runtime, context, self, (Node)iter.next(), runtime.getNil(), Block.NULL_BLOCK, callAsProc);
        }
        
        return value;
    }
}
