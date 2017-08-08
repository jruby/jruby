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

import com.headius.invokebinder.Binder;
import org.jruby.EvalType;
import org.jruby.RubyArray;
import org.jruby.RubyProc;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * The executable body portion of a closure.
 */
public abstract class BlockBody {

    protected final Signature signature;

    public BlockBody(Signature signature) {
        this.signature = signature;
    }

    public Signature getSignature() {
        return signature;
    }

    public EvalType getEvalType()  {
        return null; // method should be abstract - isn't due compatibility
    }

    public void setEvalType(EvalType evalType) {
        // NOOP - but "real" block bodies should track their eval-type
    }

    public boolean canCallDirect() {
        return false;
    }

    protected IRubyObject callDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        throw new RuntimeException("callDirect not implemented in base class. We should never get here.");
    }

    protected IRubyObject yieldDirect(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        throw new RuntimeException("yieldDirect not implemented in base class. We should never get here.");
    }

    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args) {
        if (canCallDirect()) {
            return callDirect(context, block, args, Block.NULL_BLOCK);
        } else {
            return yield(context, block, prepareArgumentsForCall(context, args, block.type), null);
        }
    }

    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        if (canCallDirect()) {
            return callDirect(context, block, args, blockArg);
        } else {
            return yield(context, block, prepareArgumentsForCall(context, args, block.type), null, blockArg);
        }
    }

    public final IRubyObject yield(ThreadContext context, Block block, IRubyObject value) {
        if (canCallDirect()) {
            return yieldDirect(context, block, new IRubyObject[] { value }, null);
        } else {
            return doYield(context, block, value);
        }
    }

    public final IRubyObject yield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        if (canCallDirect()) {
            return yieldDirect(context, block, args, self);
        } else {
            IRubyObject[] preppedValue = RubyProc.prepareArgs(context, block.type, this, args);
            return doYield(context, block, preppedValue, self);
        }
    }

    /**
     * Subclass specific yield implementation.
     * <p>
     * Should not be called directly. Gets called by {@link #yield(ThreadContext, Block, org.jruby.runtime.builtin.IRubyObject)}
     * after ensuring that any common yield logic is taken care of.
     */
    protected abstract IRubyObject doYield(ThreadContext context, Block block, IRubyObject value);

    /**
     * Subclass specific yield implementation.
     * <p>
     * Should not be called directly. Gets called by {@link #yield(ThreadContext, Block, org.jruby.runtime.builtin.IRubyObject[], org.jruby.runtime.builtin.IRubyObject)}
     * after ensuring that all common yield logic is taken care of.
     */
    protected abstract IRubyObject doYield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self);

    // FIXME: This should be unified with the final versions above
    // Here to allow incremental replacement. Overriden by subclasses which support it.
    public IRubyObject yield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self, Block blockArg) {
        return yield(context, block, args, self);
    }

    // FIXME: This should be unified with the final versions above
    // Here to allow incremental replacement. Overriden by subclasses which support it.
    public IRubyObject yield(ThreadContext context, Block block, IRubyObject value, Block blockArg) {
        return yield(context, block, value);
    }

    public IRubyObject call(ThreadContext context, Block block) {
        IRubyObject[] args = IRubyObject.NULL_ARRAY;
        if (canCallDirect()) {
            return callDirect(context, block, args, Block.NULL_BLOCK);
        } else {
            return yield(context, block, prepareArgumentsForCall(context, args, block.type), null);
        }
    }

    public IRubyObject call(ThreadContext context, Block block, Block unusedBlock) {
        return call(context, block);
    }

    public IRubyObject yieldSpecific(ThreadContext context, Block block) {
        if (canCallDirect()) {
            return yieldDirect(context, block, null, null);
        } else {
            return yield(context, block, null);
        }
    }
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0) {
        IRubyObject[] args = new IRubyObject[] {arg0};
        if (canCallDirect()) {
            return callDirect(context, block, args, Block.NULL_BLOCK);
        } else {
            return yield(context, block, prepareArgumentsForCall(context, args, block.type), null);
        }
    }
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, Block unusedBlock) {
        return call(context, block, arg0);
    }

    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0) {
        if (canCallDirect()) {
            return yieldDirect(context, block, new IRubyObject[] { arg0 }, null);
        } else {
            return yield(context, block, arg0);
        }
    }
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject[] args = new IRubyObject[] {arg0, arg1};
        if (canCallDirect()) {
            return callDirect(context, block, args, Block.NULL_BLOCK);
        } else {
            return yield(context, block, prepareArgumentsForCall(context, args, block.type), null);
        }
    }
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, Block unusedBlock) {
        return call(context, block, arg0, arg1);
    }

    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        if (canCallDirect()) {
            return yieldDirect(context, block, new IRubyObject[] { arg0, arg1 }, null);
        } else {
            return yield(context, block, new IRubyObject[] { arg0, arg1 }, null);
        }
    }
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        IRubyObject[] args = new IRubyObject[] {arg0, arg1, arg2};
        if (canCallDirect()) {
            return callDirect(context, block, args, Block.NULL_BLOCK);
        } else {
            return yield(context, block, prepareArgumentsForCall(context, args, block.type), null);
        }
    }
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block unusedBlock) {
        return call(context, block, arg0, arg1, arg2);
    }

    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (canCallDirect()) {
            return yieldDirect(context, block, new IRubyObject[] { arg0, arg1, arg2 }, null);
        } else {
            return yield(context, block, new IRubyObject[] { arg0, arg1, arg2 }, null);
        }
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
                args = IRRuntimeHelpers.convertValueIntoArgArray(context, args[0], signature, type == Block.Type.NORMAL && args[0] instanceof RubyArray);
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
