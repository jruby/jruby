/*
 * ClassNode.java - No description
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

/**
 *
 * @author  jpetersen
 * @version
 */
public class ClassNode extends Node {
    public ClassNode(String classNameId, Node bodyNode, Node superNode) {
        super(Constants.NODE_CLASS, classNameId, bodyNode, superNode);
    }
    
 	public RubyObject eval(Ruby ruby, RubyObject self) {
        if (ruby.getRubyClass() == null) {
            throw new RubyTypeException(ruby, "no outer class/module");
        }
        
        RubyModule superClass = null;
        
        if (getSuperNode() != null) {
            superClass = getSuperClass(ruby, self, getSuperNode());
        }
        
        RubyClass rubyClass = null;
        // if ((ruby_class == getRuby().getObjectClass()) && rb_autoload_defined(node.nd_cname())) {
        //     rb_autoload_load(node.nd_cname());
        // }
        if (ruby.getRubyClass().isConstantDefined(getClassNameId())) {
            rubyClass = (RubyClass)ruby.getRubyClass().getConstant(getClassNameId());
        }
        if (rubyClass != null) {
            if (!rubyClass.isClass()) {
                throw new RubyTypeException(ruby, getClassNameId() + " is not a class");
            }
            if (superClass != null) {
                RubyModule tmp = rubyClass.getSuperClass();
                if (tmp.isSingleton()) {
                    tmp = tmp.getSuperClass();
                }
                while (tmp.isIncluded()) {
                    tmp = tmp.getSuperClass();
                }
                if (tmp != superClass) {
                    superClass = tmp;
                    //goto override_class;
                    if (superClass == null) {
                        superClass = ruby.getClasses().getObjectClass();
                    }
                    rubyClass = ruby.defineClass(getClassNameId(), (RubyClass)superClass);
                    ruby.getRubyClass().setConstant(getClassNameId(), rubyClass);
                    rubyClass.setClassPath(ruby.getRubyClass(), getClassNameId());
                    // end goto
                }
            }
            if (ruby.getSecurityLevel() >= 4) {
                throw new RubySecurityException(ruby, "extending class prohibited");
            }
            // rb_clear_cache();
        } else {
            //override_class:
            if (superClass == null) {
                superClass = ruby.getClasses().getObjectClass();
            }
            rubyClass = ruby.defineClass(getClassNameId(), (RubyClass)superClass);
            ruby.getRubyClass().setConstant(getClassNameId(), rubyClass);
            rubyClass.setClassPath(ruby.getRubyClass(), getClassNameId());
        }
        if (ruby.getWrapper() != null) {
            rubyClass.getSingletonClass().includeModule(ruby.getWrapper());
            rubyClass.includeModule(ruby.getWrapper());
        }
        
        return ((ScopeNode)getBodyNode()).setupModule(ruby, rubyClass);
    }
    
    public RubyClass getSuperClass(Ruby ruby, RubyObject self, Node node) {
        RubyObject obj;
        // int state = 1; // unreachable
        
        // PUSH_TAG(PROT_NONE);
        // if ((state = EXEC_TAG()) == 0 ) {
        obj = node.eval(ruby, self);
        // }
        // POP_TAG();
        
        /*if (state != 0) {
            switch (node.nd_type()) {
                case NODE_COLON2:
                    throw new RubyTypeException("undefined superclass '" + ((RubyId)node.nd_mid()).toName() + "'");
                case NODE_CONST:
                    throw new RubyTypeException("undefined superclass '" + ((RubyId)node.nd_vid()).toName() + "'");
                default:
                    throw new RubyTypeException("undefined superclass");
            }
        //     JUMP_TAG(state);
        }*/
        if (!(obj instanceof RubyClass)) {
            throw new RuntimeException();
            // goto superclass_error;
        }
        if (((RubyClass)obj).isSingleton()) {
            throw new RubyTypeException(ruby, "can't make subclass of virtual class");
        }
        return (RubyClass)obj;
    }

	
	/**
	 * Accept for the visitor pattern.
	 * @param iVisitor the visitor
	 **/
	public void accept(NodeVisitor iVisitor)	
	{
		iVisitor.visitClassNode(this);
	}
}
