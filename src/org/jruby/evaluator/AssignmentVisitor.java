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

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 */
public class AssignmentVisitor {
    public static IRubyObject assign(ThreadContext context, IRubyObject self, Node node, IRubyObject value, boolean check) {
        IRubyObject result = null;
        IRuby runtime = context.getRuntime();
        
        switch (node.nodeId) {
        case NodeTypes.CALLNODE: {
            CallNode iVisited = (CallNode)node;
            
            IRubyObject receiver = EvaluationState.eval(context, iVisited.getReceiverNode(), self);

            if (iVisited.getArgsNode() == null) { // attribute set.
                receiver.callMethod(context, iVisited.getName(), new IRubyObject[] {value}, CallType.NORMAL);
            } else { // element set
                RubyArray args = (RubyArray)EvaluationState.eval(context, iVisited.getArgsNode(), self);
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
            if (iVisited.getPathNode() == null) {
                context.getRubyClass().defineConstant(iVisited.getName(), value);
            } else {
                ((RubyModule) EvaluationState.eval(context, iVisited.getPathNode(), self)).defineConstant(iVisited.getName(), value);
            }
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
            result = context.mAssign(self, iVisited, (RubyArray) value, check);
            break;
        }
        }

        return result;
    }
}