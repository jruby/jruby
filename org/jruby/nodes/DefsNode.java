/*
 * DefsNode.java - No description
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
import org.jruby.nodes.visitor.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DefsNode extends Node {
    public DefsNode(Node recvNode, String mId, Node defnNode) {
        super(Constants.NODE_DEFS, recvNode, mId, defnNode);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        if (getDefnNode() != null) {
            RubyObject recv = getRecvNode().eval(ruby, self);
            
            if (ruby.getSafeLevel() >= 4 && !recv.isTaint()) {
                throw new RubySecurityException(ruby, "Insecure; can't define singleton method");
            }
                /*if (FIXNUM_P(recv) || SYMBOL_P(recv)) {
                    rb_raise(rb_eTypeError, "can't define singleton method \"%s\" for %s",
                    rb_id2name(node.nd_mid()), rb_class2name(CLASS_OF(recv)));
                }*/ // not needed in jruby
            
            if (recv.isFrozen()) {
                throw new RubyFrozenException(ruby, "object");
            }
            RubyClass rubyClass = recv.getSingletonClass();
            
            Node body = (Node)rubyClass.getMethods().get(getMId());
            if (body != null) {
                if (ruby.getSafeLevel() >= 4) {
                    throw new RubySecurityException(ruby, "redefining method prohibited");
                }
                /*if (RTEST(ruby_verbose)) {
                    rb_warning("redefine %s", rb_id2name(node.nd_mid()));
                }*/
            }
            Node defn = getDefnNode().copyNodeScope(ruby.getCRef());
            defn.setRefValue(ruby.getCRef());
            ruby.getMethodCache().clearByName(getMId());
            rubyClass.addMethod(getMId(), defn, Constants.NOEX_PUBLIC | 
                    (body != null ? body.getNoex() & Constants.NOEX_UNDEF : 0));
            recv.funcall("singleton_method_added", RubySymbol.newSymbol(ruby, getMId()));
        }
        return ruby.getNil();
    }
	/**
	 * Accept for the visitor pattern.
	 * @param iVisitor the visitor
	 **/
	public void accept(NodeVisitor iVisitor)	
	{
		iVisitor.visitDefsNode(this);
	}
}
