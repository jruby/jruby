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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004-2007 Thomas E Enebo <enebo@acm.org>
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

<%= generated_warning %>

package org.jruby.runtime;

import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ast.IterNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NodeType;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The executable body portion of a closure.
 */
public abstract class BlockBody implements JumpTarget {
    // FIXME: Maybe not best place, but move it to a good home
    public static final int ZERO_ARGS = 0;
    public static final int MULTIPLE_ASSIGNMENT = 1;
    public static final int ARRAY = 2;
    public static final int SINGLE_RESTARG = 3;
    protected final int argumentType;
    
    public BlockBody(int argumentType) {
        this.argumentType = argumentType;
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        args = prepareArgumentsForCall(context, args, type);

        return yield(context, RubyArray.newArrayNoCopy(context.getRuntime(), args), null, null, true, binding, type);
    }

    // This should only be called by 1.8 (1.9 subclasses this to handle unusedBlock).
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, 
            Block.Type type, Block unusedBlock) {
        return call(context, args, binding, type);
    }

    public abstract IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type);

    public abstract IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self,
            RubyModule klass, boolean aValue, Binding binding, Block.Type type);
    
    public int getArgumentType() {
        return argumentType;
    }

<%= generated_arities %>
    
    public abstract StaticScope getStaticScope();

    public abstract Block cloneBlock(Binding binding);

    /**
     * What is the arity of this block?
     * 
     * @return the arity
     */
    public abstract Arity arity();
    
    /**
     * Is the current block a real yield'able block instead a null one
     * 
     * @return true if this is a valid block or false otherwise
     */
    public boolean isGiven() {
        return true;
    }
    
    /**
     * Compiled codes way of examining arguments
     * 
     * @param nodeId to be considered
     * @return something not linked to AST and a constant to make compiler happy
     */
    public static int asArgumentType(NodeType nodeId) {
        if (nodeId == null) return ZERO_ARGS;
        
        switch (nodeId) {
        case ZEROARGNODE: return ZERO_ARGS;
        case MULTIPLEASGNNODE: return MULTIPLE_ASSIGNMENT;
        case SVALUENODE: return SINGLE_RESTARG;
        }
        return ARRAY;
    }
    
    public IRubyObject[] prepareArgumentsForCall(ThreadContext context, IRubyObject[] args, Block.Type type) {
        switch (type) {
        case NORMAL: {
//            assert false : "can this happen?";
            if (args.length == 1 && args[0] instanceof RubyArray) {
                if (argumentType == MULTIPLE_ASSIGNMENT || argumentType == SINGLE_RESTARG) {
                    args = ((RubyArray) args[0]).toJavaArray();
                }
                break;
            }
        }
        case PROC: {
            if (args.length == 1 && args[0] instanceof RubyArray) {
                if (argumentType == MULTIPLE_ASSIGNMENT && argumentType != SINGLE_RESTARG) {
                    args = ((RubyArray) args[0]).toJavaArray();
                }
            }
            break;
        }
        case LAMBDA:
            if (argumentType == ARRAY && args.length != 1) {
                context.getRuntime().getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" + args.length + " for " + arity().getValue() + ")");
                if (args.length == 0) {
                    args = context.getRuntime().getSingleNilArray();
                } else {
                    args = new IRubyObject[] {context.getRuntime().newArrayNoCopy(args)};
                }
            } else {
                arity().checkArity(context.getRuntime(), args);
            }
            break;
        }
        
        return args;
    }
    
    public static NodeType getArgumentTypeWackyHack(IterNode iterNode) {
        NodeType argsNodeId = null;
        if (iterNode.getVarNode() != null && iterNode.getVarNode().getNodeType() != NodeType.ZEROARGNODE) {
            // if we have multiple asgn with just *args, need a special type for that
            argsNodeId = iterNode.getVarNode().getNodeType();
            if (argsNodeId == NodeType.MULTIPLEASGNNODE) {
                MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode)iterNode.getVarNode();
                if (multipleAsgnNode.getHeadNode() == null && multipleAsgnNode.getArgsNode() != null) {
                    // FIXME: This is gross. Don't do this.
                    argsNodeId = NodeType.SVALUENODE;
                }
            }
        }
        
        return argsNodeId;
    }

    public static final BlockBody NULL_BODY = new NullBlockBody();
}
