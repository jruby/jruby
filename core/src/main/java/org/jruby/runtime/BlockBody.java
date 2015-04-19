/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.RubyArray;
import org.jruby.RubyProc;
import org.jruby.ast.IterNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NodeType;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The executable body portion of a closure.
 */
public abstract class BlockBody {
    // FIXME: Maybe not best place, but move it to a good home
    public static final int ZERO_ARGS = 0;
    public static final int MULTIPLE_ASSIGNMENT = 1;
    public static final int ARRAY = 2;
    public static final int SINGLE_RESTARG = 3;

    public static final String[] EMPTY_PARAMETER_LIST = new String[0];
    
    protected final int argumentType;

    public BlockBody(int argumentType) {
        this.argumentType = argumentType;
    }

    // FIXME: Push all impls down so this is single concrete impl
    public abstract Signature getSignature();

    public void setEvalType(EvalType evalType) {
        System.err.println("setEvalType unimplemented in " + this.getClass().getName());
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        args = prepareArgumentsForCall(context, args, type);

        return yield(context, args, null, binding, type);
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding,
            Block.Type type, Block block) {
        args = prepareArgumentsForCall(context, args, type);

        return yield(context, args, null, binding, type, block);
    }

    public final IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type) {
        return doYield(context, value, binding, type);
    }

    public final IRubyObject yield(ThreadContext context, IRubyObject[] args, IRubyObject self,
                                   Binding binding, Block.Type type) {
        IRubyObject[] preppedValue = RubyProc.prepareArgs(context, type, this, args);
        return doYield(context, preppedValue, self, binding, type);
    }

    /**
     * Subclass specific yield implementation.
     * <p>
     * Should not be called directly. Gets called by {@link #yield(ThreadContext, IRubyObject, Binding, Block.Type)}
     * after ensuring that any common yield logic is taken care of.
     */
    protected abstract IRubyObject doYield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type);

    /**
     * Subclass specific yield implementation.
     * <p>
     * Should not be called directly. Gets called by {@link #yield(ThreadContext, org.jruby.runtime.builtin.IRubyObject[], org.jruby.runtime.builtin.IRubyObject, Binding, org.jruby.runtime.Block.Type)}
     * after ensuring that all common yield logic is taken care of.
     */
    protected abstract IRubyObject doYield(ThreadContext context, IRubyObject[] args, IRubyObject self,
                                           Binding binding, Block.Type type);

    // FIXME: This should be unified with the final versions above
    // Here to allow incremental replacement. Overriden by subclasses which support it.
    public IRubyObject yield(ThreadContext context, IRubyObject[] args, IRubyObject self,
                             Binding binding, Block.Type type, Block block) {
        return yield(context, args, self, binding, type);
    }

    // FIXME: This should be unified with the final versions above
    // Here to allow incremental replacement. Overriden by subclasses which support it.
    public IRubyObject yield(ThreadContext context, IRubyObject value,
            Binding binding, Block.Type type, Block block) {
        return yield(context, value, binding, type);
    }

    public int getArgumentType() {
        return argumentType;
    }

    public IRubyObject call(ThreadContext context, Binding binding, Block.Type type) {
        IRubyObject[] args = IRubyObject.NULL_ARRAY;
        args = prepareArgumentsForCall(context, args, type);

        return yield(context, args, null, binding, type);
    }
    public IRubyObject call(ThreadContext context, Binding binding,
            Block.Type type, Block unusedBlock) {
        return call(context, binding, type);
    }

    public IRubyObject yieldSpecific(ThreadContext context, Binding binding, Block.Type type) {
        return yield(context, null, binding, type);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        IRubyObject[] args = new IRubyObject[] {arg0};
        args = prepareArgumentsForCall(context, args, type);

        return yield(context, args, null, binding, type);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0, Binding binding,
            Block.Type type, Block unusedBlock) {
        return call(context, arg0, binding, type);
    }

    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        return yield(context, arg0, binding, type);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        IRubyObject[] args = new IRubyObject[] {arg0, arg1};
        args = prepareArgumentsForCall(context, args, type);

        return yield(context, args, null, binding, type);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding,
            Block.Type type, Block unusedBlock) {
        return call(context, arg0, arg1, binding, type);
    }

    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        return yield(context, new IRubyObject[] { arg0, arg1 }, null, binding, type);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        IRubyObject[] args = new IRubyObject[] {arg0, arg1, arg2};
        args = prepareArgumentsForCall(context, args, type);

        return yield(context, args, null, binding, type);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding,
            Block.Type type, Block unusedBlock) {
        return call(context, arg0, arg1, arg2, binding, type);
    }

    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return yield(context, new IRubyObject[] { arg0, arg1, arg2 }, null, binding, type);
    }


    public abstract StaticScope getStaticScope();
    public abstract void setStaticScope(StaticScope newScope);

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
     * Get the filename for this block
     */
    public abstract String getFile();

    /**
     * get The line number for this block
     */
    public abstract int getLine();

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
                context.runtime.getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" + args.length + " for " + arity().getValue() + ")");
                if (args.length == 0) {
                    args = context.runtime.getSingleNilArray();
                } else {
                    args = new IRubyObject[] {context.runtime.newArrayNoCopy(args)};
                }
            } else {
                arity().checkArity(context.runtime, args);
            }
            break;
        }

        return args;
    }

    public String[] getParameterList() {
        return EMPTY_PARAMETER_LIST;
    }

    /**
     * This is only for 'for' iters since 1.9+ blocks are only ever ArgsNode instances for their vars.
     */
    // FIXME: Change this to only be ForNode or encapsulate into ForNode altogether once non-9k runtimes removed.
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
