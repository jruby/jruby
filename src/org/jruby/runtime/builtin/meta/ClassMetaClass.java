/*
 * Created on Jun 20, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.jruby.runtime.builtin.meta;

import org.jruby.Ruby;
import org.jruby.RubyClass;
//import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;

public class ClassMetaClass extends ObjectMetaClass {
    public ClassMetaClass(Ruby runtime, RubyClass superClass) {
    	super(runtime, superClass, null, null, "Class", RubyClass.class);
    }

    /*
	public ClassMetaClass(String name, RubyClass superClass, RubyModule parentModule) {
		super(name, RubyClass.class, superClass, parentModule);
	}*/

	protected void initializeClass() {
        defineMethod("new", Arity.noArguments(), "newInstance");
        defineMethod("superclass", Arity.noArguments(), "superclass");

        defineSingletonMethod("new", Arity.optional(), "newClass");
        defineSingletonMethod("inherited", Arity.singleArgument());
        
        undefineMethod("module_function");
	}
	
	/*
	public RubyClass newSubClass(String name, RubyModule parentModule) {
		return new ClassMetaClass(name, this, parentModule);
	}

	protected IRubyObject allocateObject() {
        RubyClass instance = getRuntime().newClass();
        
		instance.setMetaClass(this);
		
		return instance;
	}

    public IRubyObject newInstance(IRubyObject[] args) {
        RubyClass instance = getRuntime().newClass();
        
        instance.callInit(args);
       
        return instance;
    }*/
    
	/*    public IRubyObject newClass(IRubyObject[] args) {
	    final Ruby runtime = getRuntime();

        RubyClass superClass = runtime.getClasses().getObjectClass();
        if (args.length > 0) {
            if (args[0] instanceof RubyClass) {
                superClass = (RubyClass) args[0];
            } else {
                throw runtime.newTypeError(
                    "wrong argument type " + superClass.getType().getName() + " (expected Class)");
            }
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
    
    public IRubyObject inherited() {
    	return getRuntime().getNil();
    }*/
}
