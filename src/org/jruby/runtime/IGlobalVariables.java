package org.jruby.runtime;

import java.util.Iterator;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IGlobalVariables {
    void defineGlobalVariable(String name, IGlobalVariable variable);

    IRubyObject getGlobalVariable(String name);
    IRubyObject setGlobalVariable(String name, IRubyObject value);
    
    boolean isGlobalVariableDefined(String name);
    void undefineGlobalVariable(String name);

    Iterator getGlobalVariables();
}