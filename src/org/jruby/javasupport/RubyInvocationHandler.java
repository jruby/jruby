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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A RubyInvocationHandler intercepts method calls to Proxy objects
 * and forwards them on to the corresponding RubyObject.  Arguments
 * passed to and sent from the method are translated between Java and
 * Ruby objects.  A new RubyInvocationHandler instance is created for
 * each Proxy object created by RubyProxyFactory.
 **/
public class RubyInvocationHandler implements InvocationHandler {
    /**
     * Store a reference to the RubyProxyFactory that created this
     * invocation handler for use in converting arguments that are passed
     * to or returned from Ruby methods.
     **/
    private RubyProxyFactory factory = null;

    /**
     * Store a reference to the RubyObject that we should forward method
     * requests to.
     **/
    private IRubyObject rubyObject = null;

    /**
     * Construct a RubyInvocationHandler instance.  This should only
     * be accessible RubyProxyFactory so it is package-scoped.
     **/
    RubyInvocationHandler(RubyProxyFactory factory, IRubyObject obj) {
        this.factory = factory;
        this.rubyObject = obj;
    }

    /**
     * The invoke method is called by Java's reflection whenever a
     * method is called on a Proxy object.  This handler will forward
     * the method call on to the corresponding RubyObject, converting
     * arguments passed to and returned from the method, and will
     * return the end result.
     **/
    public Object invoke(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();
        IRubyObject[] rubyArgs = factory.convertJavaToRuby(args);

        IRubyObject out;

        // 'get' and 'set' methods (as well as 'is' and 'has') are already
        // handled by JavaSupport.toRubyName.  Should we be using this?

        if (methodName.equals("getRubyObject")) {
            return getRubyObject();
        } else if (methodName.equals("getRubyProxyFactory")) {
            return getRubyProxyFactory();
        } else if (respondsTo(rubyObject, methodName)) {
            out = rubyObject.callMethod(methodName, rubyArgs);
        } else if (methodName.equals("toString")) {
            out = rubyObject.callMethod("to_s");
        } else if (methodName.equals("equals")) {
            out = rubyObject.callMethod("equal", rubyArgs); // ?
        } else if (methodName.indexOf("get") == 0) {
            String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

            out = rubyObject.callMethod(fieldName, rubyArgs);
        } else if (methodName.indexOf("set") == 0) {
            String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4) + "=";

            out = rubyObject.callMethod(fieldName, rubyArgs);
        } else {
            throw new RuntimeException("method " + methodName + " not found.");
        }

        return factory.convertRubyToJava(out, method.getReturnType());
    }

    /** Get the Ruby instance that should be used. */
    protected Ruby getRuby() {
        return factory.getRuntime();
    }

    /** Get the RubyProxyFactory instance that created this handler. */
    protected RubyProxyFactory getRubyProxyFactory() {
        return factory;
    }

    /** Get the RubyObject that we should forward method calls to. */
    protected IRubyObject getRubyObject() {
        return rubyObject;
    }

    /** Simple helper method for RubyObject.respond_to(RubySymbol).  */
    protected static boolean respondsTo(IRubyObject obj, String methodName) {
        return obj.respondsTo(methodName);
    }
}

