/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.runtime.builtin;

import java.util.List;

/**
 *
 * @author headius
 */
public interface InstanceVariables {
    //
    // INSTANCE VARIABLE METHODS
    //

    boolean hasInstanceVariable(String name);
    boolean fastHasInstanceVariable(String internedName);
    
    IRubyObject getInstanceVariable(String name);
    IRubyObject fastGetInstanceVariable(String internedName);
    
    IRubyObject setInstanceVariable(String name, IRubyObject value);
    IRubyObject fastSetInstanceVariable(String internedName, IRubyObject value);

    IRubyObject removeInstanceVariable(String name);

    List<Variable<IRubyObject>> getInstanceVariableList();

    List<String> getInstanceVariableNameList();
}
