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
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The executable body portion of a closure.
 */
public abstract class BlockBody {
    public static final String[] EMPTY_PARAMETER_LIST = new String[0];
    
    protected final Signature signature;

    public BlockBody(Signature signature) {
        this.signature = signature;
    }

    public Signature getSignature() {
        return signature;
    }

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
    @Deprecated
    public Arity arity() {
        return signature.arity();
    }

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

    public IRubyObject[] prepareArgumentsForCall(ThreadContext context, IRubyObject[] args, Block.Type type) {
        if (type == Block.Type.LAMBDA) {
            signature.checkArity(context.runtime, args);
        } else {
            // SSS FIXME: How is it even possible to "call" a NORMAL block?
            // I thought only procs & lambdas can be called, and blocks are yielded to.
            if (args.length == 1) {
                // Convert value to arg-array, unwrapping where necessary
                args = IRRuntimeHelpers.convertValueIntoArgArray(context, args[0], signature.arityValue(), type == Block.Type.NORMAL && args[0] instanceof RubyArray);
            } else if (getSignature().arityValue() == 1 && !getSignature().restKwargs()) {
                // discard excess arguments
                args = args.length == 0 ? context.runtime.getSingleNilArray() : new IRubyObject[] { args[0] };
            }
        }

        return args;
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        return ArgumentDescriptor.EMPTY_ARRAY;
    }

    public static final BlockBody NULL_BODY = new NullBlockBody();
}
