/*
 * Copyright (C) 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Thomas E Enebo <enebo@acm.org>
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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.ast.Node;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.marshal.MarshalStream;

import java.io.IOException;

/** Object is the parent class of all classes in Ruby. Its methods are
 * therefore available to all objects unless explicitly overridden.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public interface IRubyObject {
    public static final IRubyObject[] NULL_ARRAY = new IRubyObject[0];
    
    /**
     * RubyMethod getInstanceVar.
     * @param string
     * @return RubyObject
     */
    IRubyObject getInstanceVariable(String string);

    /**
     * RubyMethod setInstanceVar.
     * @param string
     * @param rubyObject
     * @return RubyObject
     */
    IRubyObject setInstanceVariable(String string, IRubyObject rubyObject);

    /**
     * RubyMethod funcall.
     * @param string
     * @return RubyObject
     */
    IRubyObject callMethod(String string);

    /**
     * RubyMethod isNil.
     * @return boolean
     */
    boolean isNil();

    boolean isTrue();

    /**
     * RubyMethod isTaint.
     * @return boolean
     */
    boolean isTaint();

    /**
     * RubyMethod isFrozen.
     * @return boolean
     */
    boolean isFrozen();

    /**
     * RubyMethod funcall.
     * @param string
     * @param arg
     * @return RubyObject
     */
    IRubyObject callMethod(String string, IRubyObject arg);

    /**
     * RubyMethod getRubyClass.
     */
    RubyClass getMetaClass();

    void setMetaClass(RubyClass metaClass);

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
     * RubyMethod isKindOf.
     * @param rubyClass
     * @return boolean
     */
    boolean isKindOf(RubyModule rubyClass);

    /**
     * RubyMethod respondsTo.
     * @param string
     * @return boolean
     */
    boolean respondsTo(String string);

    /**
     * RubyMethod getRuntime.
     */
    Ruby getRuntime();

    /**
     * RubyMethod getJavaClass.
     * @return Class
     */
    Class getJavaClass();

    /**
     * RubyMethod callMethod.
     * @param method
     * @param rubyArgs
     * @return IRubyObject
     */
    IRubyObject callMethod(String method, IRubyObject[] rubyArgs);

    /**
     * RubyMethod eval.
     * @param iNode
     * @return IRubyObject
     */
    IRubyObject eval(Node iNode);

    /**
     * RubyMethod eval.
     * @param iRubyObject
     * @param rubyObject
     * @param string
     * @param i
     * @return IRubyObject
     */
    IRubyObject eval(IRubyObject iRubyObject, IRubyObject rubyObject, String string, int i);

    /**
     * RubyMethod extendObject.
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
     * RubyMethod convertToType.
     * @param string
     * @param string1
     * @param b
     */
    IRubyObject convertToType(String string, String string1, boolean b);

    IRubyObject convertToString();

    /**
     * RubyMethod setTaint.
     * @param b
     */
    void setTaint(boolean b);

    /**
     * RubyMethod checkSafeString.
     */
    void checkSafeString();

    /**
     * RubyMethod marshalTo.
     * @param marshalStream
     */
    void marshalTo(MarshalStream marshalStream) throws IOException;

    /**
     * RubyMethod convertType.
     * @param type
     * @param string
     * @param string1
     */
    IRubyObject convertType(Class type, String string, String string1);

    /**
     * RubyMethod dup.
     */
    IRubyObject dup();

    /**
     * RubyMethod setupClone.
     * @param rubyString
     */
    void setupClone(IRubyObject rubyString);

    /**
     * RubyMethod setFrozen.
     * @param b
     */
    void setFrozen(boolean b);

    /**
     * RubyMethod inspect.
     * @return String
     */
    RubyString inspect();

    /**
     * RubyMethod argCount.
     * @param args
     * @param i
     * @param i1
     * @return int
     */
    int checkArgumentCount(IRubyObject[] args, int i, int i1);

    /**
     * RubyMethod rbClone.
     * @return IRubyObject
     */
    IRubyObject rbClone();

    /**
     * RubyMethod isInstanceVarDefined.
     * @param string
     * @return boolean
     */
    boolean hasInstanceVariable(String string);

    public void callInit(IRubyObject[] args);

    /**
     * RubyMethod method_missing.
     * @param args
     * @return IRubyObject
     */
    IRubyObject method_missing(IRubyObject[] args);

    /**
     * RubyMethod defineSingletonMethod.
     * @param name
     * @param callback
     */
    void defineSingletonMethod(String name, Callback callback);

    boolean singletonMethodsAllowed();
}
