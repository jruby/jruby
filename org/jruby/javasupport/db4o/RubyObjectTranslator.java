package org.jruby.javasupport.db4o;

import java.util.Iterator;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;

import com.db4o.ObjectContainer;
import com.db4o.config.ObjectConstructor;

public class RubyObjectTranslator implements ObjectConstructor {
    private Ruby ruby;
    
    public RubyObjectTranslator(Ruby ruby) {
        this.ruby = ruby;
    }

    /*
     * @see ObjectConstructor#onInstantiate(ObjectContainer, Object)
     */
    public Object onInstantiate(ObjectContainer container, Object storedObject) {
      	return new RubyObject(ruby, null);
    }

    /*
     * @see ObjectTranslator#onStore(ObjectContainer, Object)
     */
    public Object onStore(ObjectContainer container, Object applicationObject) {
      	return new RubyObjectWrapper((RubyObject)applicationObject);
    }

    /*
     * @see ObjectTranslator#onActivate(ObjectContainer, Object, Object)
     */
    public void onActivate(ObjectContainer container, Object applicationObject, Object storedObject) {
        try {
        ((RubyObjectWrapper)storedObject).createRubyObject(ruby, (RubyObject)applicationObject);
        } catch (Exception excptn) {
            excptn.printStackTrace(); 
        }
    }

    /*
     * @see ObjectTranslator#storedClass()
     */
    public Class storedClass() {
        return RubyObjectWrapper.class;
    }
}