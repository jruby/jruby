/*
 * ModuleNode.java - No description
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
public class ModuleNode extends Node {
    
    public ModuleNode(RubyId classNameId, Node bodyNode) {
        super(Constants.NODE_MODULE, classNameId, bodyNode, null);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        if (ruby.getRubyClass() == null) {
            throw new RubyTypeException(ruby, "no outer class/module");
        }
        
        RubyModule module = null;
        
        if ((ruby.getRubyClass() == ruby.getClasses().getObjectClass()) && 
                                    ruby.isAutoloadDefined(getClassNameId())) {
            // getRuby().rb_autoload_load(node.nd_cname());
        }
        if (ruby.getRubyClass().isConstantDefined(getClassNameId())) {
            module = (RubyModule)ruby.getRubyClass().getConstant(getClassNameId());
        }
        if (module != null) {
            /*if (!(module instanceof RubyModule)) {
                throw new RubyTypeException(moduleName.toName() + " is not a module");
                
            }*/
            if (ruby.getSecurityLevel() >= 4) {
                throw new RubySecurityException(ruby, "extending module prohibited");
            }
        } else {
            module = ruby.defineModuleId(getClassNameId());
            ruby.getRubyClass().setConstant(getClassNameId(), module);
            module.setClassPath(ruby.getRubyClass(), getClassNameId().toName());
        }
        if (ruby.getWrapper() != null) {
            module.getSingletonClass().includeModule(ruby.getWrapper());
            module.includeModule(ruby.getWrapper());
        }
        
        return ((ScopeNode)getBodyNode()).setupModule(ruby, module);
    }
}