package org.jruby.runtime.variables;

import java.util.Iterator;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IVariablesService {
    void defineGlobalVariable(String name, IGlobalVariable variable);

    IRubyObject getGlobalVariable(String name);
    IRubyObject setGlobalVariable(String name, IRubyObject value);

    Iterator getGlobalVariables();
}