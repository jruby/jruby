/*
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License or
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License and GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License and GNU Lesser General Public License along with JRuby;
 * if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby;

import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.ReflectionCallback;

/**
 * <p>
 * This class should be subclassed by the JRuby builtin classes to provide methods
 * for subclass creating and instance allocating.
 * </p><p>
 * It also provides some covenience methods for method definition:
 * <ul>
 * 		<li>{@link BuiltinClass#defineMethod(String, Arity, String)} and</li>
 *      <li>{@link BuiltinClass#defineSingletonMethod(String, Arity, String)}.</li>
 * </ul>
 * </p>
 */
public abstract class BuiltinClass extends RubyClass {
    private final Class builtinClass;
    
    protected BuiltinClass(String name, Class builtinClass, RubyClass superClass) {
        this(name, builtinClass, superClass, superClass.getRuntime().getClasses().getObjectClass(), true);
    }

    protected BuiltinClass(String name, Class builtinClass, RubyClass superClass, RubyModule parentModule) {
        this(name, builtinClass, superClass, parentModule, false);
    }

    protected BuiltinClass(String name, Class builtinClass, RubyClass superClass, RubyModule parentModule, boolean init) {
        super(superClass.getRuntime(), superClass.getRuntime().getClasses().getClassClass(), superClass, parentModule, name);

        assert name != null;
        assert builtinClass != null;
        assert RubyObject.class.isAssignableFrom(builtinClass) : "builtinClass have to be a subclass of RubyObject.";
        assert superClass != null;

        this.builtinClass = builtinClass;

        makeMetaClass(superClass.getMetaClass());
        inheritedBy(superClass);
        getRuntime().getClasses().putClass(name, this);

        if (init) {
            initializeClass();
        }
    }

    protected abstract void initializeClass();

    public abstract RubyClass newSubClass(String name, RubyModule parentModule);
    
    protected abstract IRubyObject allocateObject();

    public void defineMethod(String name, Arity arity, String java_name) {
        assert name != null;
        assert arity != null;
        assert java_name != null;
        assert builtinClass != null;

        defineMethod(name, new ReflectionCallback(builtinClass, java_name, arity));
    }

    public void defineSingletonMethod(String name, Arity arity, String java_name) {
        assert name != null;
        assert arity != null;
        assert java_name != null;

        getSingletonClass().defineMethod(name, new ReflectionCallback(getClass(), java_name, arity));
    }
}