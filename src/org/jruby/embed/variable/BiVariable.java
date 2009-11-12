/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.variable;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents bidirectional, both Java and Ruby, variables. Users don't instantiate
 * BiVariable type objects. Instead, users can get this type object from
 * {@link BiVariableMap} after a variable is set to the map. Users can set variables
 * in Java program explicitly through put() methods in {@link ScriptingContainer}
 * and {@link BiVariableMap} or equivalents. However, variables in Ruby scripts are
 * set in the map implicitly. When varibles and constants
 * are used in the script, thoses are automatically saved in the map converting to this type.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public interface BiVariable {
    /**
     * Defines a type correspond to Ruby's variables and constant types.
     */
    public enum Type {
        Constant, GlobalVariable, ClassVariable, InstanceVariable, LocalVariable
    }

    /**
     * Returns one of the Ruby's variables or constant types defined by Type.
     *
     * @return a type that corresponds to Ruby's variables and constant types.
     */
    public Type getType();

    /**
     * Returns a name of the variable this object holds. The name follows Ruby's
     * naming rule.
     *
     * @return a name of the variable
     */
    public String getName();

    /**
     * Returns a value of the variable this object holds in Java type.
     *
     * @return a value in Java type.
     */
    public Object getJavaObject();

    /**
     * Sets a Java object as a value of this object. At the same time,
     * an equivalent Ruby object is set automatically.
     *
     * @param runtime is used to convert a Java object to Ruby object.
     * @param javaObject is a variable value to be set.
     */
    public void setJavaObject(Ruby runtime, Object javaObject);

    /**
     * Injects a variable value to a parsed Ruby script. This method is invoked
     * during EvalUnit#run() is executed. Users don't use this method.
     *
     * @param runtime is environment where a variable injection occurs
     * @param receiver is the instance that will have variable injection.
     */
    public void inject(Ruby runtime, IRubyObject receiver);

    /**
     * Returns a value of the variable this object holds in
     * a org.jruby.runtime.builtin.IRubyObject type.
     * 
     * @return a value in IRubyObject type.
     */
    public IRubyObject getRubyObject();

    /**
     * Sets a org.jruby.runtime.builtin.IRubyObject type, Ruby object as a value
     * of this object. At the same time, an equivalent Java object is set automatically.
     *
     * @param runtime is environment where a variable injection occurs
     * @param rubyObject is a variable value to be set.
     */
    public void setRubyObject(IRubyObject rubyObject);

    /**
     * Removes this object from {@link BiVariableMap}.
     *
     * @param runtime enviroment where a variabe is removed.
     */
    public void remove(Ruby runtime);
}
