/*
 * BlockPassNode.java - No description
 * Created on 20.01.2002, 20:06:22
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.nodes;

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.nodes.visitor.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class BlockPassNode extends Node {
    public BlockPassNode(Node bodyNode) {
        super(Constants.NODE_BLOCK_PASS, null, bodyNode, null);
    }

    public RubyObject eval(Ruby ruby, RubyObject self) {
        RubyObject block = getBodyNode().eval(ruby, self);

        // RubyBlock oldBlock;
        // RubyBlock _block;
        // RubyBlock data;

        RubyObject result = ruby.getNil();
        // int orphan;
        // int safe = ruby.getSecurityLevel();

        if (block.isNil()) {
            return self.eval(getIterNode());
        }
        if (block instanceof RubyMethod/*.kind_of(ruby.getClasses().getMethodClass()).isTrue()*/) {
            // +++
            block = methodProc(ruby, (RubyMethod)block);
            // throw new TypeError(ruby, "wrong argument type Method (not supported yet)");
            // ---
        } else if (!(block instanceof RubyProc)) {
            throw new TypeError(ruby, "wrong argument type " + block.getRubyClass().toName() + " (expected Proc)");
        }

        RubyBlock oldBlock = ruby.getBlock();
        ruby.setBlock(((RubyProc) block).getBlock());

        ruby.getIter().push(RubyIter.ITER_PRE);
        ruby.getRubyFrame().setIter(RubyIter.ITER_PRE);

        try {
            return self.eval(getIterNode());
        } finally {
            ruby.getIter().pop();
            ruby.setBlock(oldBlock);
        }

        // return result;

        // Data_Get_Struct(block, struct BLOCK, data);
        // orphan = blk_orphan(data);

        /* PUSH BLOCK from data */
        /*old_block = ruby_block;
        _block = *data;
        ruby_block = &_block;
        PUSH_ITER(ITER_PRE);
        ruby_frame->iter = ITER_PRE;
        
        PUSH_TAG(PROT_NONE);
        state = EXEC_TAG();
        if (state == 0) {
        proc_set_safe_level(block);
        if (safe > ruby_safe_level)
            ruby_safe_level = safe;
        result = rb_eval(self, node->nd_iter);
        }
        POP_TAG();
        POP_ITER();
        if (_block.tag->dst == state) {
        if (orphan) {
            state &= TAG_MASK;
        }
        else {
            struct BLOCK *ptr = old_block;
        
            while (ptr) {
        	if (ptr->scope == _block.scope) {
        	    ptr->tag->dst = state;
        	    break;
        	}
        	ptr = ptr->prev;
            }
            if (!ptr) {
        	state &= TAG_MASK;
            }
        }
        }
        ruby_block = old_block;
        ruby_safe_level = safe;
        
        switch (state) {/* escape from orphan procedure */
        /*case 0:
        break;
        case TAG_BREAK:
        if (orphan) {
          rb_raise(rb_eLocalJumpError, "break from proc-closure");
        }
        break;
        case TAG_RETRY:
        rb_raise(rb_eLocalJumpError, "retry from proc-closure");
        break;
        case TAG_RETURN:
        if (orphan) {
          rb_raise(rb_eLocalJumpError, "return from proc-closure");
        }
        default:
        JUMP_TAG(state);
        }
        
        return result;
        */
    }

    public static RubyObject mproc(Ruby ruby, RubyObject recv) {
        try {
            ruby.getIter().push(RubyIter.ITER_CUR);
            ruby.getRubyFrame().push();
            return RubyGlobal.lambda(ruby, null);
        } finally {
            ruby.getRubyFrame().pop();
            ruby.getIter().pop();
        }
    }

    private static RubyObject methodProc(Ruby ruby, RubyMethod method) {
        return ruby.iterate(
            CallbackFactory.getSingletonMethod(BlockPassNode.class, "mproc"),
            null,
            CallbackFactory.getBlockMethod(BlockPassNode.class, "bmcall"),
            method);
    }

    // Block method

    public static RubyObject bmcall(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        if (blockArg instanceof RubyArray) {
        	return ((RubyMethod) arg1).call(((RubyArray)blockArg).toJavaArray());
        } else {
           	return ((RubyMethod) arg1).call(new RubyObject[]{blockArg});
        }
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitBlockPassNode(this);
    }

}