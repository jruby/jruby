/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.runtime.builtin;

import java.util.List;

/**
 * Interface that represents the instance variable aspect of Ruby
 * objects.
 *
 * @author headius
 */
public interface InstanceVariables {
    //
    // INSTANCE VARIABLE METHODS
    //

    /**
     * Returns true if object has the named instance variable.
     * 
     * @param name the name of an instance variable
     * @return true if object has the named instance variable.
     */
    boolean hasInstanceVariable(String name);

    @Deprecated
    boolean fastHasInstanceVariable(String internedName);
    
    /**
     * Returns the named instance variable if present, else null. 
     * 
     * @param name the name of an instance variable
     * @return the named instance variable if present, else null
     */
    IRubyObject getInstanceVariable(String name);

    @Deprecated
    IRubyObject fastGetInstanceVariable(String internedName);

    /**
     * Sets the named instance variable to the specified value.
     * 
     * @param name the name of an instance variable
     * @param value the value to be set
     */    
    IRubyObject setInstanceVariable(String name, IRubyObject value);

    @Deprecated
    IRubyObject fastSetInstanceVariable(String internedName, IRubyObject value);

    /**
     * Removes the named instance variable, if present, returning its
     * value.
     * 
     * @param name the name of the variable to remove
     * @return the value of the remove variable, if present; else null
     */
    IRubyObject removeInstanceVariable(String name);

    /**
     * @return instance variables
     */
    List<Variable<IRubyObject>> getInstanceVariableList();

    /**
     * @return instance variable names
     */
    List<String> getInstanceVariableNameList();

    /**
     * Copies all instance variables from the given object into the receiver
     */
    void copyInstanceVariablesInto(InstanceVariables other);
}
