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
 * method definition node.
 * <ul>
 * <li>
 * u1 ==&gt; noex (int) NOTE: noex flag, private, public or protected?
 * </li>
 * <li>
 * u2 ==&gt;  mId (String) NOTE: method id
 * </li>
 * <li>
 * u3 ==&gt; defnNode (ScopeNode) NOTE: the body of the method
 * it is actually a RFunc which means a ScopeNode with a body containing 
 * the concatenation of the nodes corresponding to the arguments and to the body
 * </li>
 * </ul>
 * @author  jpetersen
 * @version $Revision$
 */
public class DefnNode extends Node {
    public DefnNode(int noex, String mId, Node defnNode) {
        super(Constants.NODE_DEFN, noex, mId, defnNode);
    }

	/**
	 * eval the defnNode.
	 * creates a method with name mid in the current context
	 * (the current rubyClass as returned by ruby.getRubyClass) 
	 * then returns QNil
	 **/
    public RubyObject eval(Ruby ruby, RubyObject self) {
        if (getDefnNode() != null) {
			RubyModule rubyClass = ruby.getRubyClass()	;
            if (rubyClass == null) {
                throw new TypeError(ruby, "no class to add method");
            }
            
            //if (ruby_class == getRuby().getObjectClass() && node.nd_mid() == init) {
            // rom.rb_warn("redefining Object#initialize may cause infinite loop");
            //}
            //if (node.nd_mid() == __id__ || node.nd_mid() == __send__) {
            // rom.rb_warn("redefining `%s' may cause serious problem", ((RubyId)node.nd_mid()).toName());
            //}
            // ruby_class.setFrozen(true);
            
            MethodNode body = rubyClass.searchMethod(getMId());
            // RubyObject origin = body.getOrigin();
            
//            if (body != null){
                // if (ruby_verbose.isTrue() && ruby_class == origin && body.nd_cnt() == 0) {
                //     rom.rb_warning("discarding old %s", ((RubyId)node.nd_mid()).toName());
                // }
                // if (node.nd_noex() != 0) { /* toplevel */
                            /* should upgrade to rb_warn() if no super was called inside? */
                //     rom.rb_warning("overriding global function `%s'", ((RubyId)node.nd_mid()).toName());
                // }
  //          }
            
            int noex;
            
            if (ruby.isScope(Constants.SCOPE_PRIVATE) || getMId().equals("initialize")) {
                noex = Constants.NOEX_PRIVATE;
            } else if (ruby.isScope(Constants.SCOPE_PROTECTED)) {
                noex = Constants.NOEX_PROTECTED;
            } else if (rubyClass == ruby.getClasses().getObjectClass()) {
                noex =  getNoex();
            } else {
                noex = Constants.NOEX_PUBLIC;
            }

            if (body != null && body.getOrigin() == rubyClass && (body.getNoex() & Constants.NOEX_UNDEF) != 0) {
                noex |= Constants.NOEX_UNDEF;
            }
            
            Node defn = getDefnNode().copyNodeScope(ruby.getCRef());
            ruby.getMethodCache().clearByName(getMId());
            rubyClass.addMethod(getMId(), defn, noex);
            
            if (ruby.getActMethodScope() == Constants.SCOPE_MODFUNC) {
                rubyClass.getSingletonClass().addMethod(getMId(), defn, Constants.NOEX_PUBLIC);
                rubyClass.funcall("singleton_method_added", RubySymbol.newSymbol(ruby, getMId()));
            }

            if (rubyClass.isSingleton()) {
                rubyClass.getInstanceVar("__attached__").funcall("singleton_method_added", RubySymbol.newSymbol(ruby, getMId()));
            } else {
                rubyClass.funcall("method_added", RubySymbol.newSymbol(ruby, getMId()));
            }
        }
        return ruby.getNil();
    }
	public void accept(NodeVisitor iVisitor)
	{
		iVisitor.visitDefnNode(this);
	}
}
