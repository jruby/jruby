/*
 * BlockPassNode.java - No description
 * Created on 05. November 2001, 21:44
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
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
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class BlockPassNode extends Node {
    public BlockPassNode(Node bodyNode) {
        super(Constants.NODE_BLOCK_PASS, null, bodyNode, null);
    }

 	public String toString()   
	{
		return super.toString() + getBodyNode().toString() + ")";
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
            return ruby.getNil(); // +++ rb_eval(self, node->nd_iter);
        }
        if (block.kind_of(ruby.getClasses().getMethodClass()).isTrue()) {
            block = null; // method_proc(block);
        // } else if (!(block instanceof RubyProc)) {
        }
        
        return result;
    /*if (NIL_P(block)) {
	return rb_eval(self, node->nd_iter);
    }
    if (rb_obj_is_kind_of(block, rb_cMethod)) {
	block = method_proc(block);
    }
    else if (!rb_obj_is_proc(block)) {
	rb_raise(rb_eTypeError, "wrong argument type %s (expected Proc)",
		 rb_class2name(CLASS_OF(block)));
    }

    Data_Get_Struct(block, struct BLOCK, data);
    orphan = blk_orphan(data);

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
}
