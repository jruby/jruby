package org.jruby.javasupport.db4o;

import com.db4o.*;
import com.db4o.config.*;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyString;

public class RubyStringTranslator implements ObjectConstructor {
    private Ruby ruby;
    
    public RubyStringTranslator(Ruby ruby) {
        this.ruby = ruby;
    }

    /*
     * @see ObjectConstructor#onInstantiate(ObjectContainer, Object)
     */
    public Object onInstantiate(ObjectContainer container, Object storedObject) {
        return ((RubyStringWrapper)storedObject).createRubyString(ruby);
    }

    /*
     * @see ObjectTranslator#onStore(ObjectContainer, Object)
     */
    public Object onStore(ObjectContainer container, Object applicationObject) {
        return new RubyStringWrapper((RubyString)applicationObject);
    }

    /*
     * @see ObjectTranslator#onActivate(ObjectContainer, Object, Object)
     */
    public void onActivate(ObjectContainer container, Object applicationObject, Object storedObject) {
    }

    /*
     * @see ObjectTranslator#storedClass()
     */
    public Class storedClass() {
        return RubyStringWrapper.class;
    }
}