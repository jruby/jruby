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
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.CompiledBlockCallback19;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReflectionCallbackFactory extends CallbackFactory {
    private final Class type;

    public ReflectionCallbackFactory(Class type) {
            this.type = type;
    }
	
    @Deprecated
    public Callback getMethod(String method) {
        return new ReflectionCallback(type, method, NULL_CLASS_ARRAY, false, false, Arity.noArguments(), false);
    }
    
    @Deprecated
    public Callback getFastMethod(String method) {
        return new ReflectionCallback(type, method, NULL_CLASS_ARRAY, false, false, Arity.noArguments(), true);
    }
    
    @Deprecated
    public Callback getFastMethod(String rubyName, String method) {
        return new ReflectionCallback(type, method, NULL_CLASS_ARRAY, false, false, Arity.noArguments(), true);
    }

    @Deprecated
    public Callback getMethod(String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1 }, false, false, Arity.singleArgument(), false);
    }

    @Deprecated
    public Callback getFastMethod(String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1 }, false, false, Arity.singleArgument(), true);
    }

    @Deprecated
    public Callback getFastMethod(String rubyName, String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1 }, false, false, Arity.singleArgument(), true);
    }

    @Deprecated
    public Callback getMethod(String method, Class arg1, Class arg2) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2 }, false, false, Arity.fixed(2), false);
    }

    @Deprecated
    public Callback getFastMethod(String method, Class arg1, Class arg2) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2 }, false, false, Arity.fixed(2), true);
    }

    @Deprecated
    public Callback getFastMethod(String rubyName, String method, Class arg1, Class arg2) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2 }, false, false, Arity.fixed(2), true);
    }

    @Deprecated
    public Callback getMethod(String method, Class arg1, Class arg2, Class arg3) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2, arg3 }, false, false, Arity.fixed(3), false);
    }

    @Deprecated
    public Callback getFastMethod(String method, Class arg1, Class arg2, Class arg3) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2, arg3 }, false, false, Arity.fixed(3), true);
    }

    @Deprecated
    public Callback getFastMethod(String rubyName, String method, Class arg1, Class arg2, Class arg3) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2, arg3 }, false, false, Arity.fixed(3), true);
    }

    @Deprecated
    public Callback getSingletonMethod(String method) {
        return new ReflectionCallback(type, method, NULL_CLASS_ARRAY, false, true, Arity.noArguments(), false);
    }

    @Deprecated
    public Callback getFastSingletonMethod(String method) {
        return new ReflectionCallback(type, method, NULL_CLASS_ARRAY, false, true, Arity.noArguments(), true);
    }

    @Deprecated
    public Callback getSingletonMethod(String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1 }, false, true, Arity.singleArgument(), false);
    }

    @Deprecated
    public Callback getFastSingletonMethod(String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1 }, false, true, Arity.singleArgument(), true);
    }

    @Deprecated
    public Callback getSingletonMethod(String method, Class arg1, Class arg2) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2 }, false, true, Arity.fixed(2), false);
    }

    @Deprecated
    public Callback getFastSingletonMethod(String method, Class arg1, Class arg2) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2 }, false, true, Arity.fixed(2), true);
    }

    @Deprecated
    public Callback getSingletonMethod(String method, Class arg1, Class arg2, Class arg3) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2, arg3 }, false, true, Arity.fixed(3), false);
    }

    @Deprecated
    public Callback getFastSingletonMethod(String method, Class arg1, Class arg2, Class arg3) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2, arg3 }, false, true, Arity.fixed(3), true);
    }

    @Deprecated
    public Callback getBlockMethod(String method) {
        return new ReflectionCallback(
            type,
            method,
            new Class[] { IRubyObject.class, IRubyObject.class },
            false,
            true,
            Arity.fixed(2), false);
    }

    @Deprecated
    public CompiledBlockCallback getBlockCallback(String method, final Object scriptObject) {
        try {
            final Method blockMethod = scriptObject.getClass().getMethod(method, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject.class});
            return new CompiledBlockCallback() {
                public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject args) {
                    try {
                        return (IRubyObject)blockMethod.invoke(scriptObject, new Object[]{context, self, args});
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    } catch (IllegalArgumentException ex) {
                        throw new RuntimeException(ex);
                    } catch (InvocationTargetException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof RuntimeException) {
                            throw (RuntimeException) cause;
                        } else if (cause instanceof Error) {
                            throw (Error) cause;
                        } else {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            };
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }
    }

    @Deprecated
    public CompiledBlockCallback19 getBlockCallback19(String method, final Object scriptObject) {
        try {
            final Method blockMethod = scriptObject.getClass().getMethod(method, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class});
            return new CompiledBlockCallback19() {
                public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
                    try {
                        return (IRubyObject)blockMethod.invoke(scriptObject, new Object[]{context, self, args, block});
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    } catch (IllegalArgumentException ex) {
                        throw new RuntimeException(ex);
                    } catch (InvocationTargetException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof RuntimeException) {
                            throw (RuntimeException) cause;
                        } else if (cause instanceof Error) {
                            throw (Error) cause;
                        } else {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            };
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }
    }

    @Deprecated
    public Callback getOptSingletonMethod(String method) {
        return new ReflectionCallback(type, method, new Class[] { IRubyObject[].class }, true, true, Arity.optional(), false);
    }

    @Deprecated
    public Callback getFastOptSingletonMethod(String method) {
        return new ReflectionCallback(type, method, new Class[] { IRubyObject[].class }, true, true, Arity.optional(), true);
    }

    @Deprecated
    public Callback getOptMethod(String method) {
        return new ReflectionCallback(type, method, new Class[] { IRubyObject[].class }, true, false, Arity.optional(), false);
    }

    @Deprecated
    public Callback getFastOptMethod(String method) {
        return new ReflectionCallback(type, method, new Class[] { IRubyObject[].class }, true, false, Arity.optional(), true);
    }

    @Deprecated
    public Callback getFastOptMethod(String rubyName, String method) {
        return new ReflectionCallback(type, method, new Class[] { IRubyObject[].class }, true, false, Arity.optional(), true);
    }
}
