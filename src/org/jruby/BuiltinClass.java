/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
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
