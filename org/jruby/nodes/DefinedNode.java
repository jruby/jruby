/*
 * DefinedNode.java - No description
 * Created on 19.01.2002, 22:18:23
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
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
import org.jruby.nodes.types.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DefinedNode extends Node {

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

        /* 
         * This is going to be ugly for now until I figure it out 
         * more completely -- CEF 
         */
        String def = getDefined(ruby, self);
        if (def == null) {
            return ruby.getNil();
        }
        return new RubyString(ruby, def);
    }

    public String getDefined(Ruby ruby, RubyObject self) {
        Node head = getHeadNode();

        switch (head.getType()) {
            case Constants.NODE_ZSUPER :
            case Constants.NODE_SUPER :
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
            case Constants.NODE_VCALL :
            case Constants.NODE_FCALL :
				//FIXME
            case Constants.NODE_CALL :
				//FIXME
            case Constants.NODE_MATCH2 :
            case Constants.NODE_MATCH3 :
                return "method";
            case Constants.NODE_YIELD :
                return ruby.isBlockGiven() ? "yield" : null;
            case Constants.NODE_SELF :
                return "self";
            case Constants.NODE_NIL :
                return "nil";
            case Constants.NODE_TRUE :
                return "true";
            case Constants.NODE_FALSE :
                return "false";
            case Constants.NODE_ATTRSET :
            case Constants.NODE_OP_ASGN1 :
            case Constants.NODE_OP_ASGN2 :
            case Constants.NODE_MASGN :
            case Constants.NODE_LASGN :
            case Constants.NODE_DASGN :
            case Constants.NODE_DASGN_CURR :
            case Constants.NODE_GASGN :
            case Constants.NODE_CDECL :
            case Constants.NODE_CVDECL :
            case Constants.NODE_CVASGN :
                return "assignment";
            case Constants.NODE_LVAR :
                return "local-variable";
            case Constants.NODE_DVAR :
                return "local-variable(in-block)";
            case Constants.NODE_GVAR :
                //FIXME...need to first check that it's defined!!!
                // 	if (rb_gvar_defined(node->nd_entry)) {
                return "global-variable";
            case Constants.NODE_IVAR :
                return self.isInstanceVarDefined(head.getVId()) ? "instance-variable" : null;
            case Constants.NODE_CONST :
                // FIXME if(constant is defined)
                return "constant";
            case Constants.NODE_CVAR :
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
            case Constants.NODE_CVAR2 :
                // FIXME!!!!
                //if (rb_cvar_defined(rb_cvar_singleton(self), node->nd_vid)) {
                //return "class variable";
                //}
            case Constants.NODE_COLON2 :
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
            case Constants.NODE_NTH_REF :
                //FIXME!!
                //if (RTEST(rb_reg_nth_defined(node->nd_nth, MATCH_DATA))) {
                //sprintf(buf, "$%d", node->nd_nth);
                //return buf;
                //}
                //break;
                return null;
            case Constants.NODE_BACK_REF :
                //FIXME!!
                //if (RTEST(rb_reg_nth_defined(0, MATCH_DATA))) {
                //sprintf(buf, "$%c", node->nd_nth);
                //return buf;
                //}
                //break;
                return null;
            default :
                try {
                    self.eval(head);
                    return "expression";
                } catch (JumpException jumpExcptn) {
                    return null;
                }
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
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitDefinedNode(this);
    }
}