/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.runtime.callback.Callback;

/**
 * Helper class to build Callback method.
 * This impements method corresponding to the signature of method most often found in
 * the Ruby library, for methods with other signature the appropriate Callback object
 * will need to be explicitly created.
 **/
public abstract class CallbackFactory {
    @Deprecated
    public static final Class[] NULL_CLASS_ARRAY = new Class[0];

    /**
     * gets an instance method with no arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getMethod(String method);

    /**
     * gets a fast instance method with no arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getFastMethod(String method);

    /**
     * gets an instance method with 1 argument.
     * @param method name of the method
     * @param arg1 the class of the only argument for this method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getMethod(String method, Class arg1);

    /**
     * gets a fast instance method with 1 argument.
     * @param method name of the method
     * @param arg1 the class of the only argument for this method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getFastMethod(String method, Class arg1);

    /**
     * gets an instance method with two arguments.
     * @param method name of the method
     * @param arg1 the java class of the first argument for this method
     * @param arg2 the java class of the second argument for this method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getMethod(String method, Class arg1, Class arg2);

    /**
     * gets a fast instance method with two arguments.
     * @param method name of the method
     * @param arg1 the java class of the first argument for this method
     * @param arg2 the java class of the second argument for this method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getFastMethod(String method, Class arg1, Class arg2);

    /**
     * gets an instance method with two arguments.
     * @param method name of the method
     * @param arg1 the java class of the first argument for this method
     * @param arg2 the java class of the second argument for this method
     * @param arg3 the java class of the second argument for this method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getMethod(String method, Class arg1, Class arg2, Class arg3);

    /**
     * gets a fast instance method with two arguments.
     * @param method name of the method
     * @param arg1 the java class of the first argument for this method
     * @param arg2 the java class of the second argument for this method
     * @param arg3 the java class of the second argument for this method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getFastMethod(String method, Class arg1, Class arg2, Class arg3);

    /**
     * gets a singleton (class) method without arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getSingletonMethod(String method);

    /**
     * gets a fast singleton (class) method without arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     */
    public abstract Callback getFastSingletonMethod(String method);

    /**
     * gets a singleton (class) method with 1 argument.
     * @param method name of the method
     * @param arg1 the class of the only argument for this method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getSingletonMethod(String method, Class arg1);

    /**
     * gets a fast singleton (class) method with 1 argument.
     * @param method name of the method
     * @param arg1 the class of the only argument for this method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getFastSingletonMethod(String method, Class arg1);

    /**
     * gets a singleton (class) method with 2 arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getSingletonMethod(String method, Class arg1, Class arg2);

    /**
     * gets a fast singleton (class) method with 2 arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getFastSingletonMethod(String method, Class arg1, Class arg2);

    /**
     * gets a singleton (class) method with 3 arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getSingletonMethod(String method, Class arg1, Class arg2, Class arg3);

    /**
     * gets a fast singleton (class) method with 3 arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getFastSingletonMethod(String method, Class arg1, Class arg2, Class arg3);

    @Deprecated
    public abstract Callback getBlockMethod(String method);

    @Deprecated
    public abstract CompiledBlockCallback getBlockCallback(String method, Object scriptObject);

    @Deprecated
    public abstract CompiledBlockCallback19 getBlockCallback19(String method, Object scriptObject);

    /**
     * gets a singleton (class) method with no mandatory argument and some optional arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getOptSingletonMethod(String method);


    /**
     * gets a fast singleton (class) method with no mandatory argument and some optional arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getFastOptSingletonMethod(String method);

    /**
     * gets an instance method with no mandatory argument and some optional arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getOptMethod(String method);

    /**
     * gets a fast instance method with no mandatory argument and some optional arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     * @deprecated Callbacks are inefficient; use MethodFactory.
     **/
    public abstract Callback getFastOptMethod(String method);

    @Deprecated
    public static CallbackFactory createFactory(Ruby runtime, Class type) {
        throw new RuntimeException("callback-style handles are no longer supported in JRuby");
    }

    // used by compiler
    @Deprecated
    public static CallbackFactory createFactory(Ruby runtime, Class type, ClassLoader classLoader) {
        throw new RuntimeException("callback-style handles are no longer supported in JRuby");
    }
}
