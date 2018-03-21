/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import java.util.Objects;
import org.jruby.EvalType;
import org.jruby.RubyProc;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.block.LambdaBlock;
import org.jruby.runtime.block.NormalBlock;
import org.jruby.runtime.block.ProcBlock;
import org.jruby.runtime.block.ThreadBlock;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.runtime.Helpers.arrayOf;

/**
 *  Internal live representation of a block ({...} or do ... end).
 */
public class Block {
    public enum Type {
        NORMAL(false), PROC(false), LAMBDA(true), THREAD(false);

        Type(boolean checkArity) {
            this.checkArity = checkArity;
        }

        public final boolean checkArity;
    }

    public final Type type;

    protected final Binding binding;

    protected final BlockBody body;

    /**
     * The Proc that this block is associated with.  When we reference blocks via variable
     * reference they are converted to Proc objects.  We store a reference of the associated
     * Proc object for easy conversion.
     */
    protected RubyProc proc = null;

    /** Whether this block and any clones of it should be considered "escaped" */
    protected boolean escaped;

    /** What block to use for determining escape; defaults to this */
    protected Block escapeBlock;

    /**
     * All Block variables should either refer to a real block or this NULL_BLOCK.
     */
    public static final Block NULL_BLOCK = Block.newBlock(BlockBody.NULL_BODY);

    protected Block(BlockBody body, Binding binding, Type type) {
        assert binding != null;
        this.body = body;
        this.binding = binding;
        this.type = type;
        this.escapeBlock = this;
    }

    protected Block(BlockBody body, Binding binding, Type type, Block escapeBlock) {
        assert binding != null;
        this.body = body;
        this.binding = binding;
        this.type = type;
        this.escapeBlock = escapeBlock;
    }

    public static Block newBlock(BlockBody body) {
        return NormalBlock.newBlock(body, Binding.DUMMY);
    }

    public static Block newBlock(BlockBody body, Binding binding) {
        return NormalBlock.newBlock(body, binding);
    }

    public static Block newProc(BlockBody body, Binding binding) {
        return ProcBlock.newBlock(body, binding);
    }

    public static Block newLambda(BlockBody body, Binding binding) {
        return LambdaBlock.newBlock(body, binding);
    }

    public Block toLambda() {
        if (this == NULL_BLOCK) return this;

        return LambdaBlock.newBlock(body, binding, escapeBlock);
    }

    public Block toProc() {
        if (this == NULL_BLOCK) return this;

        return ProcBlock.newBlock(body, binding, escapeBlock);
    }

    public Block toThread() {
        if (this == NULL_BLOCK) return this;

        return ThreadBlock.newBlock(body, binding, escapeBlock);
    }

    public Block toNormal() {
        if (this == NULL_BLOCK) return this;

        return NormalBlock.newBlock(body, binding, escapeBlock);
    }

    public Block toType(Type type) {
        switch (type) {
            case PROC: return toProc();
            case LAMBDA: return toLambda();
            case THREAD: return toThread();
            case NORMAL: return toNormal();
        }
        throw new RuntimeException("should not get here");
    }

    public boolean isLambda() {
        return type == Type.LAMBDA;
    }

    public boolean isProc() {
        return type == Type.PROC;
    }

    public boolean isThread() {
        return type == Type.THREAD;
    }

    public boolean isNormal() {
        return type == Type.NORMAL;
    }

    public DynamicScope allocScope(DynamicScope parentScope) {
        return DynamicScope.newDynamicScope(body.getStaticScope(), parentScope, body.getEvalType());
    }

    public EvalType getEvalType() {
        return body.getEvalType();
    }

    public void setEvalType(EvalType evalType) {
        body.setEvalType(evalType);
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        return body.call(context, this, args, NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block blockArg) {
        return body.call(context, this, args, blockArg);
    }

    public IRubyObject call(ThreadContext context) {
        return body.call(context, this, IRubyObject.NULL_ARRAY, NULL_BLOCK);
    }
    public IRubyObject call(ThreadContext context, Block blockArg) {
        return body.call(context, this, IRubyObject.NULL_ARRAY, blockArg);
    }
    public IRubyObject yieldSpecific(ThreadContext context) {
        return body.yieldSpecific(context, this);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0) {
        return body.call(context, this, arg0, NULL_BLOCK);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0, Block blockArg) {
        return body.call(context, this, arg0, blockArg);
    }
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0) {
        return body.yieldSpecific(context, this, arg0);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return body.call(context, this, arg0, arg1, NULL_BLOCK);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block blockArg) {
        return body.call(context, this, arg0, arg1, blockArg);
    }
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return body.yieldSpecific(context, this, arg0, arg1);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return body.call(context, this, arg0, arg1, arg2, NULL_BLOCK);
    }
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block blockArg) {
        return body.call(context, this, arg0, arg1, arg2, blockArg);
    }
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return body.yieldSpecific(context, this, arg0, arg1, arg2);
    }

    public IRubyObject yield(ThreadContext context, IRubyObject value) {
        return body.yield(context, this, value, null, NULL_BLOCK);
    }

    public IRubyObject yieldNonArray(ThreadContext context, IRubyObject value, IRubyObject self) {
        return body.yield(context, this, arrayOf(value), self, NULL_BLOCK);
    }

    public IRubyObject yieldArray(ThreadContext context, IRubyObject value, IRubyObject self) {
        // SSS FIXME: Later on, we can move this code into IR insructions or
        // introduce a specialized entry-point when we know that this block has
        // explicit call protocol IR instructions.
        IRubyObject[] args = IRRuntimeHelpers.singleBlockArgToArray(value);
        return body.yield(context, this, args, self, NULL_BLOCK);
    }

    public IRubyObject yieldValues(ThreadContext context, IRubyObject[] args) {
        return body.yield(context, this, args, null, NULL_BLOCK);
    }

    public Block cloneBlock() {
        return new Block(body, binding, type, escapeBlock);
    }

    public Block cloneBlockAndBinding() {
        return new Block(body, binding.clone(), type, this);
    }

    public Block cloneBlockAndFrame() {
        return new Block(body, binding.cloneAndDupFrame(), type, this);
    }

    /**
     * Clone this block and make it a lambda, as appropriate for define_method.
     */
    public Block cloneForMethod() {
        return LambdaBlock.newBlock(body, binding.cloneAndDupFrame(), escapeBlock);
    }

    public Block cloneBlockForEval(IRubyObject self, EvalType evalType) {
        Block block = cloneBlock();

        block.getBinding().setSelf(self);
        block.getBinding().getFrame().setSelf(self);
        block.setEvalType(evalType);

        return block;
    }

    public Block deepCloneBlockForEval(IRubyObject self, EvalType evalType) {
        Block block = cloneBlockAndBinding();

        block.getBinding().setSelf(self);
        block.getBinding().getFrame().setSelf(self);
        block.setEvalType(evalType);

        return block;
    }

    public Signature getSignature() {
        return body.getSignature();
    }

    /**
     * Retrieve the proc object associated with this block
     *
     * @return the proc or null if this has no proc associated with it
     */
    public RubyProc getProcObject() {
    	return proc;
    }

    /**
     * Set the proc object associated with this block
     *
     * @param procObject
     */
    public void setProcObject(RubyProc procObject) {
    	this.proc = procObject;
    }

    /**
     * Is the current block a real yield'able block instead a null one
     *
     * @return true if this is a valid block or false otherwise
     */
    public final boolean isGiven() {
        return this != NULL_BLOCK;
    }

    public Binding getBinding() {
        return binding;
    }

    public BlockBody getBody() {
        return body;
    }

    /**
     * Gets the frame.
     *
     * @return Returns a RubyFrame
     */
    public Frame getFrame() {
        return binding.getFrame();
    }

    public boolean isEscaped() {
        return escapeBlock.escaped;
    }

    public void escape() {
        escapeBlock.escaped = true;
    }

    @Override
    public boolean equals(Object other) {
        if ( this == other ) return true;
        if ( ! ( other instanceof Block ) ) return false;

        final Block that = (Block) other;

        return this.binding.equals(that.binding) && this.body == that.body;
    }

    @Override
    public int hashCode() {
        int hash = 11;
        hash = 13 * hash + Objects.hashCode(this.binding);
        hash = 17 * hash + Objects.hashCode(this.body);
        return hash;
    }

    @Deprecated
    public Block(BlockBody body, Binding binding) {
        this(body, binding, Type.NORMAL);
    }

    @Deprecated
    public Block(BlockBody body) {
        this(body, Binding.DUMMY, Type.NORMAL);
    }

    @Deprecated
    public Arity arity() {
        return getSignature().arity();
    }

}
