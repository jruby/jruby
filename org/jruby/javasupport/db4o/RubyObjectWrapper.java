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
	    
	    Iterator iter = rubyObject.getInstanceVariables().entrySet().iterator();
	    while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            
            this.instanceVariables.put(entry.getKey(), entry.getValue());
        }
	}
	
	public void createRubyObject(Ruby ruby, RubyObject newObject) {
	    RubyClass type = ruby.getRubyClass(typeName);
	    
	    newObject.setRubyClass(type);
	    
	    Iterator iter = instanceVariables.entrySet().iterator();
	    while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            
	    	newObject.setInstanceVar((String)entry.getKey(), (RubyObject)entry.getValue());
        }
	}
}