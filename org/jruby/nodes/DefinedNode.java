/*
 * DefinedNode.java - No description
 * Created on 05. November 2001, 21:45
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
public class DefinedNode extends Node{
    
    public DefinedNode(Node headNode) {
        super(Constants.NODE_DEFINED, headNode, null, null);
    }
    public RubyObject eval(Ruby ruby, RubyObject self) {
        // String buf;
        // String desc = is_defined(self, node.nd_head(), buf);
        //
        // if (desc) {
        //     result = rb_str_new2(desc);
        // } else {
        //     result = Qnil;
        /* This is going to be ugly for now until I figure it out 
           more completely -- CEF 
	*/
	String def = getDefined();
	if(def == null) 
		return ruby.getNil();
	return new RubyString(ruby, getDefined());
     }
     
     public String getDefined() {
        Node head = getHeadNode();
        if(head instanceof ZSuperNode || head instanceof SuperNode) {
		//if (ruby_frame->last_func == 0) return 0;
		//	else if (ruby_frame->last_class == 0) return 0;
		//	else if (rb_method_boundp(RCLASS(ruby_frame->last_class)->super,
		//				  ruby_frame->last_func, 0)) {
		//	    if (nd_type(node) == NODE_SUPER) {
		//		return arg_defined(self, node->nd_args, buf, "super");
		//	    }
		//	    return "super";
		//	}
		//	break;
	} else if (head instanceof VCallNode || head instanceof FCallNode) {
	} else if (head instanceof CallNode) {
	} else if (head instanceof Match2Node || head instanceof Match3Node) {
		return "method";
	} else if (head instanceof YieldNode) {
		//if block given, TODO----FIXME
		return "yield";
	} else if (head instanceof SelfNode) {
		return "self";
	} else if (head instanceof NilNode) {
		return "nil";
	} else if (head instanceof TrueNode) {
		return "true";
	} else if (head instanceof FalseNode) {
		return "false";
	} else if (head instanceof org.jruby.nodes.types.AssignableNode || head instanceof AttrSetNode || head instanceof OpAsgn1Node || head instanceof OpAsgn2Node) {
		return "assignment";
	} else if (head instanceof LVarNode) {
		return "local-variable";
	} else if (head instanceof DVarNode) {
		return "local-variable(in-block)";
	} else if (head instanceof GVarNode) {
		//FIXME...need to first check that it's defined!!!
		// 	if (rb_gvar_defined(node->nd_entry)) {
		return "global-variable";
	} else if (head instanceof IVarNode) {
		//FIXME...need to first check that it's defined!!!
		// 	if (rb_ivar_defined(self, node->nd_vid)) {
		return "instance-variable";
	} else if (head instanceof ConstNode) {
		// FIXME if(constant is defined)
		return "constant";
	} else if (head instanceof CVarNode) {
		// FIXME!!!!
		//if (NIL_P(ruby_cbase)) {
	    	//if (rb_cvar_defined(CLASS_OF(self), node->nd_vid)) {
			//return "class variable";
	    	//}
	    	//break;
		//}
		//if (!FL_TEST(ruby_cbase, FL_SINGLETON)) {
	    	//if (rb_cvar_defined(ruby_cbase, node->nd_vid)) {
			//return "class variable";
	    	//}
	    	//break;
		//}
		//self = rb_iv_get(ruby_cbase, "__attached__");
		return "class variable";
	} else if (head instanceof CVar2Node) {
		// FIXME!!!!
		//if (rb_cvar_defined(rb_cvar_singleton(self), node->nd_vid)) {
	    	//return "class variable";
		//}
	} else if (head instanceof Colon2Node) {
		//FIXME!!!!!
	//PUSH_TAG(PROT_NONE);
		//if ((state = EXEC_TAG()) == 0) {
	    	//val = rb_eval(self, node->nd_head);
		//}
		//POP_TAG();
		//if (state) {
	    	//ruby_errinfo = Qnil;
	    	//return 0;
		//}
		//else {
	    	//switch (TYPE(val)) {
	      	//case T_CLASS:
	      	//case T_MODULE:
			//if (rb_const_defined_at(val, node->nd_mid))
		    	//return "constant";
	      	//default:
			//if (rb_method_boundp(val, node->nd_mid, 1)) {
		    	//return "method";
			//}
	    	//}
		//}
		//break;

		return null;
	} else if (head instanceof NthRefNode) {
		//FIXME!!
		//if (RTEST(rb_reg_nth_defined(node->nd_nth, MATCH_DATA))) {
	    	//sprintf(buf, "$%d", node->nd_nth);
	    	//return buf;
		//}
		//break;
		return null;
	} else if (head instanceof BackRefNode) {
		//FIXME!!
		//if (RTEST(rb_reg_nth_defined(0, MATCH_DATA))) {
	    	//sprintf(buf, "$%c", node->nd_nth);
	    	//return buf;
		//}
		//break;
		return null;
	} else {
		//FIXME!!
		 	//default:
		//PUSH_TAG(PROT_NONE);
		//if ((state = EXEC_TAG()) == 0) {
	    	//rb_eval(self, node);
		//}
		//POP_TAG();
		//if (!state) {
	    	//return "expression";
		//}
		//ruby_errinfo = Qnil;
		//break;

	}
	
	return null;
    }
}
