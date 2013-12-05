/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009-2011 Yoko Harada <yokolet@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.variable;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents bidirectional, both Java and Ruby, variables. Users don't instantiate
 * BiVariable type objects. Instead, users can get this type object from
 * {@link BiVariableMap} after a variable is set to the map. Users can set variables
 * in Java program explicitly through put() methods in {@link ScriptingContainer}
 * and {@link BiVariableMap} or equivalents. However, variables in Ruby scripts are
 * set in the map implicitly. When variables and constants
 * are used in the script, those are automatically saved in the map converting to this type.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public interface BiVariable {
    /**
     * Defines a type correspond to Ruby's variables and constant types.
     */
    public enum Type {
        Argv, Constant, GlobalVariable, LocalGlobalVariable, ClassVariable, InstanceVariable, LocalVariable
    }

    /**
     * Returns one of the Ruby's variables or constant types defined by Type.
     *
     * @return a type that corresponds to Ruby's variables and constant types.
     */
    public Type getType();

    /**
     * Returns the original receiver where this variable has been retrieved.
     *
     * @return an original receiver.
     */
    public IRubyObject getReceiver();

   /**
     * Returns true if a given receiver is identical to the receiver this object has.
     *
     * @return true if identical otherwise false.
     */
    public boolean isReceiverIdentical(RubyObject receiver);

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
     */
    public void inject();

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
     * Attempts to remove this variable/constant from top self or receiver.
     *
     */
    public void remove();
}
