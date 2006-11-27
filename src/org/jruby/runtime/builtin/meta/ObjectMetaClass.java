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
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.runtime.builtin.meta;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

/**
 * <p>
 * The meta class for Object
 * </p>
 */
public class ObjectMetaClass extends AbstractMetaClass {
    // Only for creating ObjectMetaClass directly
    public ObjectMetaClass(IRuby runtime) {
    	super(runtime, null /*Would be Class if it existed yet */, null, null, "Object");
    	
    	this.builtinClass = RubyObject.class;
    }
    
    // Only for other core modules/classes
    protected ObjectMetaClass(IRuby runtime, RubyClass metaClass, RubyClass superClass, 
            SinglyLinkedList parentCRef, String name, Class builtinClass) {
    	super(runtime, metaClass, superClass, parentCRef, name);
    	
    	this.builtinClass = builtinClass;
    }
    
    protected ObjectMetaClass(String name, Class builtinClass, RubyClass superClass) {
        this(name, builtinClass, superClass, superClass.getRuntime().getClass("Object").getCRef());
    }

    protected ObjectMetaClass(String name, Class builtinClass, RubyClass superClass, SinglyLinkedList parentCRef) {
        super(superClass.getRuntime(), superClass.getRuntime().getClass("Class"), superClass, parentCRef, name);

        assert builtinClass != null;
        //assert RubyObject.class.isAssignableFrom(builtinClass) || RubyObject.class == builtinClass: "builtinClass have to be a subclass of RubyObject.";
        assert superClass != null;

        this.builtinClass = builtinClass;

        makeMetaClass(superClass.getMetaClass(), getCRef());
        inheritedBy(superClass);

        if(name != null) {
            ((RubyModule)parentCRef.getValue()).setConstant(name, this);
        }
    }
    
    protected class ObjectMeta extends Meta {
	    protected void initializeClass() {
	        definePrivateMethod("initialize", Arity.optional());
	        definePrivateMethod("inherited", Arity.singleArgument());
		}
    };
    
    protected Meta getMeta() {
    	return new ObjectMeta();
    }

	protected IRubyObject allocateObject() {
        RubyObject instance = new RubyObject(getRuntime(), this);
        
		instance.setMetaClass(this);
		
		return instance;
	}
    
    public void initializeClass() {
        getMeta().initializeClass();
    }
    
}
