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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.runtime.callback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jruby.Ruby;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;

public class CompiledReflectionCallback implements Callback {
    private Ruby runtime;
    private String methodName;
    private String className;
    private int arity;
    private ClassLoader classLoader;
    private Method method = null;

    public CompiledReflectionCallback(Ruby runtime, String className, String methodName, int arity, ClassLoader classLoader) {
        this.runtime = runtime;
        this.className = className;
        this.methodName = methodName;
        this.arity = arity;
        this.classLoader = classLoader;
    }

    public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
        assert arity == args.length;
        Object[] arguments = new Object[2 + args.length];
        arguments[0] = runtime;
        arguments[1] = recv;
        System.arraycopy(args, 0, arguments, 2, args.length);
        try {
            return (IRubyObject) getMethod().invoke(null, arguments);
        } catch (IllegalAccessException e) {
            assert false : e;
            return null;
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
			assert false : e.getCause();
			return null;
        }
    }

    public Arity getArity() {
        return Arity.fixed(arity);
    }

    private Method getMethod() {
        if (method != null) {
            return method;
        }
        try {
            Class javaClass = getJavaClass();
            Class[] args = new Class[2 + arity];
            args[0] = Ruby.class;
            args[1] = IRubyObject.class;
            for (int i = 2; i < args.length; i++) {
                args[i] = IRubyObject.class;
            }
            method = javaClass.getMethod(methodName, args);
            return method;

        } catch (NoSuchMethodException e) {
            assert false : "method not found: " + methodName;
            return null;
        }
    }

    private Class getJavaClass() {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            assert false : "class not found: " + className;
            return null;
        }
    }
}
