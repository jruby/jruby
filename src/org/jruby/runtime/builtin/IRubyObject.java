/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime.builtin;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyString;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;

/** Object is the parent class of all classes in Ruby. Its methods are
 * therefore available to all objects unless explicitly overridden.
 */
public interface IRubyObject {
    /**
     *
     */
    public static final IRubyObject[] NULL_ARRAY = new IRubyObject[0];

    @Deprecated
    public IRubyObject callSuper(ThreadContext context, IRubyObject[] args, Block block);

    public IRubyObject callMethod(ThreadContext context, String name);
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject arg);
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args);
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, Block block);
    @Deprecated
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name);
    @Deprecated
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name, IRubyObject arg);

    public IRubyObject checkCallMethod(ThreadContext context, String name);
    
    /**
     * RubyMethod isNil.
     * @return boolean
     */
    boolean isNil();
    
    /**
     *
     * @return
     */
    boolean isTrue();
    
    /**
     * RubyMethod isTaint.
     * @return boolean
     */
    boolean isTaint();
    
    /**
     * RubyMethod setTaint.
     * @param b
     */
    void setTaint(boolean b);
    
    /**
     * Infect this object using the taint of another object
     * @param obj
     * @return
     */
    IRubyObject infectBy(IRubyObject obj);
    
    /**
     * RubyMethod isFrozen.
     * @return boolean
     */
    boolean isFrozen();
    
    /**
     * RubyMethod setFrozen.
     * @param b
     */
    void setFrozen(boolean b);

    /**
     * RubyMethod isUntrusted.
     * @return boolean
     */
    boolean isUntrusted();

    /**
     * RubyMethod setUntrusted.
     * @param b
     */
    void setUntrusted(boolean b);
    
    /**
     *
     * @return
     */
    boolean isImmediate();
    
    /**
     * RubyMethod getRubyClass.
     * @return
     */
    RubyClass getMetaClass();
    
    /**
     * RubyMethod getSingletonClass.
     * @return RubyClass
     */
    RubyClass getSingletonClass();
    
    /**
     * RubyMethod getType.
     * @return RubyClass
     */
    RubyClass getType();
    
    /**
     * RubyMethod respondsTo.
     * @param string
     * @return boolean
     */
    boolean respondsTo(String string);

    /**
     * RubyMethod respondsTo.
     * @param string
     * @return boolean
     */
    boolean respondsToMissing(String string);

    /**
     * RubyMethod respondsTo.
     * @param string
     * @return boolean
     */
    boolean respondsToMissing(String string, boolean priv);
    
    /**
     * RubyMethod getRuntime.
     * @return
     */
    Ruby getRuntime();
    
    /**
     * RubyMethod getJavaClass.
     * @return Class
     */
    Class getJavaClass();
    
    /**
     * Convert the object into a symbol name if possible.
     *
     * @return String the symbol name
     */
    String asJavaString();
    
    /** rb_obj_as_string
     * @return
     */
    RubyString asString();
    
    /**
     * Methods which perform to_xxx if the object has such a method
     * @return
     */
    RubyArray convertToArray();
    /**
     *
     * @return
     */
    RubyHash convertToHash();    
    /**
    *
    * @return
    */    
    RubyFloat convertToFloat();
    /**
     *
     * @return
     */
    RubyInteger convertToInteger();
    /**
     *
     * @return
     */
    @Deprecated
    RubyInteger convertToInteger(int convertMethodIndex, String convertMethod);
    /**
     *
     * @return
     */
    RubyInteger convertToInteger(String convertMethod);
    /**
     *
     * @return
     */
    RubyString convertToString();
    
    /**
     *
     * @return
     */
    IRubyObject anyToString();
    
    /**
     *
     * @return
     */
    IRubyObject checkStringType();

    /**
     *
     * @return
     */
    IRubyObject checkStringType19();
    
    /**
     *
     * @return
     */
    IRubyObject checkArrayType();

    /**
     * Convert the object to the specified Java class, if possible.
     *
     * @param cls The target type to which the object should be converted.
     */
    Object toJava(Class cls);

    /**
     * RubyMethod dup.
     * @return
     */
    IRubyObject dup();
    
    /**
     * RubyMethod inspect.
     * @return String
     */
    IRubyObject inspect();
    
    /**
     * RubyMethod rbClone.
     * @return IRubyObject
     */
    IRubyObject rbClone();

    /**
     * @return true if an object is Ruby Module instance (note that it will return false for Ruby Classes).
     * If is_a? semantics is required, use <code>(someObject instanceof RubyModule)</code> instead.
     */
    boolean isModule();    
    
    /**
     * @return true if an object is Ruby Class instance (note that it will return false for Ruby singleton classes). 
     * If is_a? semantics is required, use <code>(someObject instanceof RubyClass/MetaClass)</code> instead.
     */
    boolean isClass();
    
    /**
     * Our version of Data_Wrap_Struct.
     *
     * This method will just set a private pointer to the object provided. This pointer is transient
     * and will not be accessible from Ruby.
     *
     * @param obj the object to wrap
     */
    void dataWrapStruct(Object obj);
    
    /**
     * Our version of Data_Get_Struct.
     *
     * Returns a wrapped data value if there is one, otherwise returns null.
     *
     * @return the object wrapped.
     */
    Object dataGetStruct();
    Object dataGetStructChecked();
    
    /**
     *
     * @return
     */
    IRubyObject id();
    
    
    public IRubyObject op_equal(ThreadContext context, IRubyObject other);
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other);
    public boolean eql(IRubyObject other);

    public void addFinalizer(IRubyObject finalizer);

    public void removeFinalizers();

    //
    // COMMON VARIABLE METHODS
    //

    /**
     * Returns true if object has any variables, defined as:
     * <ul>
     * <li> instance variables
     * <li> class variables
     * <li> constants
     * <li> internal variables, such as those used when marshalling Ranges and Exceptions
     * </ul>
     * @return true if object has any variables, else false
     */
    boolean hasVariables();

    /**
     * @return the count of all variables (ivar/cvar/constant/internal)
     */
    int getVariableCount();
    
    /**
     * Sets object's variables to those in the supplied list,
     * removing/replacing any previously defined variables.  Applies
     * to all variable types (ivar/cvar/constant/internal).
     * 
     * @param variables the variables to be set for object 
     */
    @Deprecated
    void syncVariables(List<Variable<Object>> variables);

    /**
     * Sets object's variables to those in the supplied object,
     * removing/replacing any previously defined variables of the same name.
     * Applies to all variable types (ivar/cvar/constant/internal).
     *
     * @param source the source object containing the variables to sync
     */
    void syncVariables(IRubyObject source);
    
    /**
     * @return a list of all variables (ivar/cvar/constant/internal)
     */
    List<Variable<Object>> getVariableList();

    //
    // INSTANCE VARIABLE METHODS
    //
    
    InstanceVariables getInstanceVariables();

    //
    // INTERNAL VARIABLE METHODS
    //

    InternalVariables getInternalVariables();

    /**
     * @return a list of all variable names (ivar/cvar/constant/internal)
     */
    List<String> getVariableNameList();

    void copySpecialInstanceVariables(IRubyObject clone);

    public Object getVariable(int index);
    public void setVariable(int index, Object value);
}
