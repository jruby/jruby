/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002 Don Schwartz <schwardo@users.sourceforge.net>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport;

import java.lang.reflect.Proxy;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * <p> A RubyProxyFactory dispenses RubyProxy objects, which provide a
 * convenient, dynamic wrapper around Ruby objects.  A
 * RubyProxyFactory must be initialized with a Ruby instance,
 * presumably one which has been loaded with classes which you want to
 * instantiate from Java, and an object implementing the
 * RubyToJavaClassMap interface, which provides a way to map between
 * Ruby and Java classes while converting arguments to and from method
 * calls. </p>
 *
 * <p> The API is composed of three primary method calls:
 * newProxyObject, which creates a new RubyObject and returns a
 * wrapper around it, getProxyForObject, which takes an existing
 * RubyObject and returns a wrapper around it, and getProxyForGlobal,
 * which finds the RubyObject stored in a Ruby global variable
 * ($variable) and returns a wrapper around it.  </p>
 *
 * @version $Revision$ 
 * @author schwardo
 */
public class RubyProxyFactory {
    private Ruby runtime = null;
    private RubyToJavaClassMap classMap = null;
    private RubyConversion conversion = null;

    public RubyProxyFactory(Ruby runtime, RubyToJavaClassMap classMap) {
        this.runtime = runtime;
        this.classMap = classMap;
        this.conversion = new RubyConversion(this);
    }

    public Ruby getRuntime() {
        return runtime;
    }

    /**
     * Create a new instance of the RubyClass corresponding to the
     * specified Java interface.  Return a Java object that conforms
     * to this Java interface, but implemented by the RubyClass.
     **/
    public RubyProxy newProxyObject(Class javaInterface) {
        return newProxyObject(javaInterface, new Object[0]);
    }

    public RubyProxy newProxyObject(Class javaInterface, Object[] args) {
        RubyClass rubyClass = getRubyClassForJavaClass(javaInterface);
        IRubyObject[] rubyArgs = convertJavaToRuby(args);
        IRubyObject obj = rubyClass.newInstance(rubyArgs);

        if (obj != null) {
            return getProxyForObject(obj, javaInterface);
        }
		return null;
    }

    public RubyProxy getProxyForGlobal(String globalVar) {
        return getProxyForGlobal(globalVar, null);
    }

    public RubyProxy getProxyForGlobal(String globalVar, Class javaInterface) {
        IRubyObject obj = getRuntime().getGlobalVariables().get(globalVar);

        if (obj != null && !obj.isNil()) {
			return getProxyForObject(obj, javaInterface);
		}
        return null;
    }

    /**
     * Create a wrapper around the specified RubyObject.  Any method
     * calls will be forwarded to the implementation of the RubyObject
     * and any returned objects will be converted to their
     * corresponding Java objects.
     **/
    public RubyProxy getProxyForObject(IRubyObject obj) {
        return getProxyForObject(obj, null);
    }

    public RubyProxy getProxyForObject(IRubyObject obj, Class javaInterface) {
        if (javaInterface == null) {
			javaInterface = getJavaClassForRubyClass(obj.getMetaClass());
		}
        return (RubyProxy) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] { javaInterface, RubyProxy.class },
            new RubyInvocationHandler(this, obj));
    }

    /*****************************************************
            Helper methods for RubyToJavaClassMap
     *****************************************************/

    protected RubyClass getRubyClassForJavaClass(Class javaClass) {
        String className = classMap.getRubyClassNameForJavaClass(javaClass);
        return getRuntime().getClass(className);
    }

    protected Class getJavaClassForRubyClass(RubyClass rubyClass) {
        return classMap.getJavaClassForRubyClass(rubyClass);
    }

    /*****************************************************
              Helper methods for RubyConversion
               (used by RubyInvocationHandler)
     *****************************************************/

    public IRubyObject[] convertJavaToRuby(Object[] obj) {
        return conversion.convertJavaToRuby(obj);
    }

    public Object convertRubyToJava(IRubyObject obj, Class type) {
        return conversion.convertRubyToJava(obj, type);
    }
}

