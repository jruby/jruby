/*
 * Created on Jun 20, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.jruby.runtime.builtin.meta;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.runtime.Arity;
import org.jruby.util.collections.SinglyLinkedList;

// Note: This code is not currently live.  It will be hooked up
// some time around 0.8.3 development cycle.
public class ClassMetaClass extends ObjectMetaClass {
    public ClassMetaClass(IRuby runtime, RubyClass superClass) {
    	super(runtime, null, superClass, runtime.getObject().getCRef(), "Class", RubyClass.class);
    }

	public ClassMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
		super(name, RubyClass.class, superClass, parentCRef);
	}
	
	protected class ClassMeta extends Meta {
		public void initializeClass() {
	        defineMethod("new", Arity.optional(), "newInstance");
	        defineMethod("superclass", Arity.noArguments(), "superclass");
	
	        defineSingletonMethod("new", Arity.optional(), "newClass");
	        defineSingletonMethod("inherited", Arity.singleArgument());
	        
	        undefineMethod("module_function");
		}
	};
	
	protected Meta getMeta() {
		return new ClassMeta();
	}
	
	/*
	public RubyClass newSubClass(String name, RubyModule parentModule) {
		return new ClassMetaClass(name, this, parentModule);
	}

	protected IRubyObject allocateObject() {
        RubyClass instance = (RubyClass) newClass(IRubyObject.NULL_ARRAY);
        
		instance.setMetaClass(this);
		
		return instance;
	}

    public IRubyObject newInstance(IRubyObject[] args) {
        return newClass(IRubyObject.NULL_ARRAY);
    }
    
	public IRubyObject newClass(IRubyObject[] args) {
	    Ruby runtime = getRuntime();

        RubyClass superClass;
        if (args.length > 0) {
            if (args[0] instanceof RubyClass) {
                superClass = (RubyClass) args[0];
            } else {
                throw runtime.newTypeError(
                    "wrong argument type " + args[0].getType().getName() + " (expected Class)");
            }
        } else {
        	 superClass = runtime.getClasses().getObjectClass();
        }

        RubyClass newClass = superClass.subclass();

        newClass.makeMetaClass(superClass.getMetaClass(), runtime.getCurrentContext().getLastRubyClass());

        // call "initialize" method
        newClass.callInit(args);

        // call "inherited" method of the superclass
        newClass.inheritedBy(superClass);

        if (runtime.getCurrentContext().isBlockGiven()) {
            runtime.yield(null, newClass, newClass, false);
        }

        return newClass;
    }
    
    public IRubyObject inherited(IRubyObject ignore) {
    	return getRuntime().getNil();
    }*/
}
