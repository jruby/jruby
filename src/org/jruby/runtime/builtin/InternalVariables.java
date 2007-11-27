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
public interface InternalVariables {
    /**
     * Returns true if object has the named internal variable.  Use only
     * for internal variables (not ivar/cvar/constant).
     * 
     * @param name the name of an internal variable
     * @return true if object has the named internal variable.
     */
    boolean hasInternalVariable(String name);
    
    /**
     * Returns true if object has the named internal variable.  Use only
     * for internal variables (not ivar/cvar/constant). The supplied
     * name <em>must</em> have been previously interned.
     * 
     * @param internedName the interned name of an internal variable
     * @return true if object has the named internal variable, else false
     */
    boolean fastHasInternalVariable(String internedName);

    /**
     * Returns the named internal variable if present, else null.  Use only
     * for internal variables (not ivar/cvar/constant).
     * 
     * @param name the name of an internal variable
     * @return the named internal variable if present, else null
     */
    IRubyObject getInternalVariable(String name);
    
    /**
     * Returns the named internal variable if present, else null.  Use only
     * for internal variables (not ivar/cvar/constant). The supplied
     * name <em>must</em> have been previously interned.
     * 
     * @param internedName the interned name of an internal variable
     * @return he named internal variable if present, else null
     */
    IRubyObject fastGetInternalVariable(String internedName);

    /**
     * Sets the named internal variable to the specified value.  Use only
     * for internal variables (not ivar/cvar/constant).
     * 
     * @param name the name of an internal variable
     * @param value the value to be set
     */
    void setInternalVariable(String name, IRubyObject value);
    
    /**
     * Sets the named internal variable to the specified value.  Use only
     * for internal variables (not ivar/cvar/constant). The supplied
     * name <em>must</em> have been previously interned.
     * 
     * @param internedName the interned name of an internal variable
     * @param value the value to be set
     */
    void fastSetInternalVariable(String internedName, IRubyObject value);

    /**
     * Removes the named internal variable, if present, returning its
     * value.  Use only for internal variables (not ivar/cvar/constant).
     * 
     * @param name the name of the variable to remove
     * @return the value of the remove variable, if present; else null
     */
    IRubyObject removeInternalVariable(String name);

    /**
     * @return only internal variables (NOT ivar/cvar/constant)
     */
    List<Variable<IRubyObject>> getInternalVariableList();
}
