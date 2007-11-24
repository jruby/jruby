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
package org.jruby.runtime;

import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.ast.NodeType;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *  Internal live representation of a block ({...} or do ... end).
 */
public class Block {
    // FIXME: Maybe not best place, but move it to a good home
    public static final int ZERO_ARGS = 0;
    public static final int MULTIPLE_ASSIGNMENT = 1;
    public static final int ARRAY = 2;
    public static final int SINGLE_RESTARG = 3;
    
    public enum Type { NORMAL, PROC, LAMBDA }
    
    /**
     * The Proc that this block is associated with.  When we reference blocks via variable
     * reference they are converted to Proc objects.  We store a reference of the associated
     * Proc object for easy conversion.  
     */
    private RubyProc proc = null;
    
    public Type type = Type.NORMAL;
    
    private final Binding binding;
    
    private final BlockBody body;
    
    /**
     * All Block variables should either refer to a real block or this NULL_BLOCK.
     */
    public static final Block NULL_BLOCK = new Block() {
        public boolean isGiven() {
            return false;
        }
        
        public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, 
                RubyModule klass, boolean aValue) {
            throw context.getRuntime().newLocalJumpError("noreason", (IRubyObject)value, "yield called out of block");
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] args) {
            throw context.getRuntime().newLocalJumpError("noreason", context.getRuntime().newArrayNoCopy(args), "yield called out of block");
        }

        public IRubyObject yield(ThreadContext context, IRubyObject value) {
            throw context.getRuntime().newLocalJumpError("noreason", (IRubyObject)value, "yield called out of block");
        }
        
        public Block cloneBlock() {
            return this;
        }
        
        public BlockBody getBody() {
            return BlockBody.NULL_BODY;
        }
    };
    
    protected Block() {
        this(null, null);
    }
    
    public Block(BlockBody body, Binding binding) {
        this.body = body;
        this.binding = binding;
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        return body.call(context, args, binding, type);
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject value) {
        return body.yield(context, value, binding, type);
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, 
            RubyModule klass, boolean aValue) {
        return body.yield(context, value, self, klass, aValue, binding, type);
    }
    
    protected int arrayLength(IRubyObject node) {
        return node instanceof RubyArray ? ((RubyArray)node).getLength() : 0;
    }
    
    public Block cloneBlock() {
        // We clone dynamic scope because this will be a new instance of a block.  Any previously
        // captured instances of this block may still be around and we do not want to start
        // overwriting those values when we create a new one.
        // ENEBO: Once we make self, lastClass, and lastMethod immutable we can remove duplicate
        Block newBlock = body.cloneBlock(binding);
        
        newBlock.type = type;

        return newBlock;
    }

    /**
     * What is the arity of this block?
     * 
     * @return the arity
     */
    public Arity arity() {
        return body.arity();
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
    public boolean isGiven() {
        return true;
    }
    
    public Binding getBinding() {
        return binding;
    }
    
    public BlockBody getBody() {
        return body;
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
}
