package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

import java.util.Iterator;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IGlobalVariables {
    void define(String name, IAccessor variable);
    void defineReadonly(String name, IAccessor variable);
    boolean isDefined(String name);
    void alias(String name, String oldName);

    IRubyObject get(String name);
    IRubyObject set(String name, IRubyObject value);

    Iterator getNames();
}