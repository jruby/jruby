package org.jruby.javasupport.db4o;

import java.util.*;

import org.jruby.*;

public class RubyObjectWrapper {
	public Map instanceVariables = new HashMap();
	public String typeName;
	
	public RubyObjectWrapper() {
	}
	
	public RubyObjectWrapper(RubyObject rubyObject) {
	    this.typeName = rubyObject.type().toName();
	    
	    Iterator iter = rubyObject.getInstanceVariables().values().iterator();
	    while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            
            this.instanceVariables.put(((RubyId)entry.getKey()).toName(), entry.getValue());
        }
	}
	
	public RubyObject createRubyObject(Ruby ruby) {
	    RubyClass type = ruby.getRubyClass(typeName);
	    
	    RubyObject newObject = new RubyObject(ruby, type);
	    
	    Iterator iter = instanceVariables.values().iterator();
	    while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            
	    	newObject.setInstanceVar((String)entry.getKey(), (RubyObject)entry.getValue());
        }
	    
	    return newObject;
	}
}