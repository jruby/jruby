/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby.runtime.builtin;

import java.io.IOException;
import java.util.Map;

import org.ablaf.ast.INode;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.runtime.Callback;
import org.jruby.runtime.marshal.MarshalStream;

/** Object is the parent class of all classes in Ruby. Its methods are
 * therefore available to all objects unless explicitly overridden.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public interface IRubyObject {
    public static final IRubyObject[] NULL_ARRAY = new IRubyObject[0];
    
    /**
     * Method getInstanceVar.
     * @param string
     * @return RubyObject
     */
    IRubyObject getInstanceVariable(String string);

    /**
     * Method setInstanceVar.
     * @param string
     * @param rubyObject
     * @return RubyObject
     */
    IRubyObject setInstanceVariable(String string, IRubyObject rubyObject);

    /**
     * Method funcall.
     * @param string
     * @return RubyObject
     */
    IRubyObject callMethod(String string);

    /**
     * Method isNil.
     * @return boolean
     */
    boolean isNil();

    boolean isTrue();

    /**
     * Method isTaint.
     * @return boolean
     */
    boolean isTaint();

    /**
     * Method isFrozen.
     * @return boolean
     */
    boolean isFrozen();

    /**
     * Method funcall.
     * @param string
     * @param arg
     * @return RubyObject
     */
    IRubyObject callMethod(String string, IRubyObject arg);

    /**
     * Method getRubyClass.
     */
    RubyClass getMetaClass();

    void setMetaClass(RubyClass metaClass);

    /**
     * Method getSingletonClass.
     * @return RubyClass
     */
    RubyClass getSingletonClass();

    /**
     * Method getType.
     * @return RubyClass
     */
    RubyClass getType();

    /**
     * Method isKindOf.
     * @param rubyClass
     * @return boolean
     */
    boolean isKindOf(RubyModule rubyClass);

    /**
     * Method respondsTo.
     * @param string
     * @return boolean
     */
    boolean respondsTo(String string);

    /**
     * Method getRuntime.
     */
    Ruby getRuntime();

    /**
     * Method getJavaClass.
     * @return Class
     */
    Class getJavaClass();

    /**
     * Method callMethod.
     * @param method
     * @param rubyArgs
     * @return IRubyObject
     */
    IRubyObject callMethod(String method, IRubyObject[] rubyArgs);

    /**
     * Method eval.
     * @param iNode
     * @return IRubyObject
     */
    IRubyObject eval(INode iNode);

    /**
     * Method eval.
     * @param iRubyObject
     * @param rubyObject
     * @param string
     * @param i
     * @return IRubyObject
     */
    IRubyObject eval(IRubyObject iRubyObject, IRubyObject rubyObject, String string, int i);

    /**
     * Method extendObject.
     * @param rubyModule
     */
    void extendObject(RubyModule rubyModule);

    /**
     * Convert the object into a symbol name if possible.
     * 
     * @return String the symbol name
     */
    String asSymbol();

    /**
     * Method convertToType.
     * @param string
     * @param string1
     * @param b
     */
    IRubyObject convertToType(String string, String string1, boolean b);

    IRubyObject convertToString();

    /**
     * Method setTaint.
     * @param b
     */
    void setTaint(boolean b);

    /**
     * Method checkSafeString.
     */
    void checkSafeString();

    /**
     * Method marshalTo.
     * @param marshalStream
     */
    void marshalTo(MarshalStream marshalStream) throws IOException;

    /**
     * Method convertType.
     * @param type
     * @param string
     * @param string1
     */
    IRubyObject convertType(Class type, String string, String string1);

    /**
     * Method dup.
     */
    IRubyObject dup();

    /**
     * Method setupClone.
     * @param rubyString
     */
    void setupClone(IRubyObject rubyString);

    /**
     * Method setFrozen.
     * @param b
     */
    void setFrozen(boolean b);

    /**
     * Method inspect.
     * @return String
     */
    RubyString inspect();

    /**
     * Method argCount.
     * @param args
     * @param i
     * @param i1
     * @return int
     */
    int argCount(IRubyObject[] args, int i, int i1);

    /**
     * Method rbClone.
     * @return IRubyObject
     */
    IRubyObject rbClone();

    /**
     * Method isInstanceVarDefined.
     * @param string
     * @return boolean
     */
    boolean hasInstanceVariable(String string);

    /**
     * Method getInstanceVariables.
     * @return Object
     */
    Map getInstanceVariables();

    public void callInit(IRubyObject[] args);

    /**
     * Method method_missing.
     * @param args
     * @return IRubyObject
     */
    IRubyObject method_missing(IRubyObject[] args);

    /**
     * Method defineSingletonMethod.
     * @param name
     * @param callback
     */
    void defineSingletonMethod(String name, Callback callback);

    void setInstanceVariables(Map map);
}