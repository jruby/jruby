/*
 * DefnNode.java - No description
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
import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version
 */
public class DefnNode extends Node {
    public DefnNode(int noex, RubyId mId, Node defnNode) {
        super(Constants.NODE_DEFN, noex, mId, defnNode);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        if (getDefnNode() != null) {
            if (ruby.getRubyClass() == null) {
                throw new RubyTypeException(ruby, "no class to add method");
            }
            
            //if (ruby_class == getRuby().getObjectClass() && node.nd_mid() == init) {
            // rom.rb_warn("redefining Object#initialize may cause infinite loop");
            //}
            //if (node.nd_mid() == __id__ || node.nd_mid() == __send__) {
            // rom.rb_warn("redefining `%s' may cause serious problem", ((RubyId)node.nd_mid()).toName());
            //}
            // ruby_class.setFrozen(true);
            
            MethodNode body = ruby.getRubyClass().searchMethod(getMId());
            // RubyObject origin = body.getOrigin();
            
            if (body != null){
                // if (ruby_verbose.isTrue() && ruby_class == origin && body.nd_cnt() == 0) {
                //     rom.rb_warning("discarding old %s", ((RubyId)node.nd_mid()).toName());
                // }
                // if (node.nd_noex() != 0) { /* toplevel */
                            /* should upgrade to rb_warn() if no super was called inside? */
                //     rom.rb_warning("overriding global function `%s'", ((RubyId)node.nd_mid()).toName());
                // }
            }
            
            int noex;
            
            if (ruby.isScope(Constants.SCOPE_PRIVATE) || getMId().equals(ruby.intern("initialize"))) {
                noex = Constants.NOEX_PRIVATE;
            } else if (ruby.isScope(Constants.SCOPE_PROTECTED)) {
                noex = Constants.NOEX_PROTECTED;
            } else if (ruby.getRubyClass() == ruby.getClasses().getObjectClass()) {
                noex =  getNoex();
            } else {
                noex = Constants.NOEX_PUBLIC;
            }

            if (body != null && body.getOrigin() == ruby.getRubyClass() && (body.getNoex() & Constants.NOEX_UNDEF) != 0) {
                noex |= Constants.NOEX_UNDEF;
            }
            
            Node defn = getDefnNode().copyNodeScope(ruby.getCRef());
            ruby.getRubyClass().addMethod(getMId(), defn, noex);
            
            // rb_clear_cache_by_id(node.nd_mid());
            
            if (ruby.getActMethodScope() == Constants.SCOPE_MODFUNC) {
                ruby.getRubyClass().getSingletonClass().addMethod(getMId(), defn, Constants.NOEX_PUBLIC);
                ruby.getRubyClass().funcall(ruby.intern("singleton_method_added"), getMId().toSymbol());
            }

            if (ruby.getRubyClass().isSingleton()) {
                ruby.getRubyClass().getInstanceVar("__attached__").funcall(ruby.intern("singleton_method_added"), getMId().toSymbol());
            } else {
                ruby.getRubyClass().funcall(ruby.intern("method_added"), getMId().toSymbol());
            }
        }
        return ruby.getNil();
    }
}