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
package org.jruby.embed.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import org.jruby.Ruby;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyObjectAdapter;
import org.jruby.RubyString;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.EmbedRubyObjectAdapter;
import org.jruby.embed.InvokeFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.variable.BiVariable;
import org.jruby.embed.variable.InstanceVariable;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 * Implementation of {@link EmbedRubyObjectAdapter}. Users get an instance of this
 * class by newObjectAdapter() method of {@link ScriptingContainer}.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class EmbedRubyObjectAdapterImpl implements EmbedRubyObjectAdapter {
    private RubyObjectAdapter adapter = JavaEmbedUtils.newObjectAdapter();
    private ScriptingContainer container;

    public EmbedRubyObjectAdapterImpl(ScriptingContainer container) {
        this.container = container;
    }

    public boolean isKindOf(IRubyObject value, RubyModule rubyModule) {
        return adapter.isKindOf(value, rubyModule);
    }
    
    public IRubyObject[] convertToJavaArray(IRubyObject array) {
        return adapter.convertToJavaArray(array);
    }

    public RubyInteger convertToRubyInteger(IRubyObject obj) {
        return adapter.convertToRubyInteger(obj);
    }
    
    public RubyString convertToRubyString(IRubyObject obj) {
        return adapter.convertToRubyString(obj);
    }

    public synchronized IRubyObject setInstanceVariable(IRubyObject obj, String variableName, IRubyObject value) {
        BiVariableMap map = container.getVarMap();
        if (map.containsKey(variableName)) {
            BiVariable bv = map.getVariable(variableName);
            bv.setRubyObject(obj);
        } else {
            InstanceVariable iv = new InstanceVariable(variableName, value);
            map.update(variableName, iv);
        }
        return obj.getInstanceVariables().setInstanceVariable(variableName, value);
    }

    public IRubyObject getInstanceVariable(IRubyObject obj, String variableName) {
        BiVariableMap map = container.getVarMap();
        if (map.containsKey(variableName)) {
            BiVariable bv = map.getVariable(variableName);
            return bv.getRubyObject();
        }
        return null;
    }

    public IRubyObject callMethod(IRubyObject receiver, String methodName) {
        return adapter.callMethod(receiver, methodName);
    }

    public IRubyObject callMethod(IRubyObject receiver, String methodName, IRubyObject singleArg) {
        return adapter.callMethod(receiver, methodName, singleArg);
    }

    public IRubyObject callMethod(IRubyObject receiver, String methodName, IRubyObject[] args) {
        return adapter.callMethod(receiver, methodName, args);
    }

    public IRubyObject callMethod(IRubyObject receiver, String methodName, IRubyObject[] args, Block block) {
        return adapter.callMethod(receiver, methodName, args, block);
    }

    public IRubyObject callSuper(IRubyObject receiver, IRubyObject[] args) {
        return adapter.callSuper(receiver, args);
    }

    public IRubyObject callSuper(IRubyObject receiver, IRubyObject[] args, Block block) {
        return adapter.callSuper(receiver, args, block);
    }

    @CallMethodType(methodType=0)
    public <T> T callMethod(Object receiver, String methodName, Class<T> returnType) {
        Class[] params = {Object.class, String.class, Class.class};
        Method method;
        try {
            method = getClass().getMethod("callMethod", params);
            return call(returnType, method, receiver, methodName, null, null, new Object[]{});
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new InvokeFailedException(ex);
                }
            }
            throw new InvokeFailedException(e);
        }
    }

    @CallMethodType(methodType=1)
    public <T> T callMethod(Object receiver, String methodName, Object singleArg, Class<T> returnType) {
        Class[] params = {Object.class, String.class, Object.class, Class.class};
        Method method;
        try {
            method = getClass().getMethod("callMethod", params);
            return call(returnType, method, receiver, methodName, null, null, singleArg);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new InvokeFailedException(ex);
                }
            }
            throw new InvokeFailedException(e);
        }
    }

    @CallMethodType(methodType=2)
    public <T> T callMethod(Object receiver, String methodName, Object[] args, Class<T> returnType) {
        Class[] params = {Object.class, String.class, Object[].class, Class.class};
        Method method;
        try {
            method = getClass().getMethod("callMethod", params);
            return call(returnType, method, receiver, methodName, null, null, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new InvokeFailedException(ex);
                }
            }
            throw new InvokeFailedException(e);
        }
    }

    @CallMethodType(methodType=3)
    public <T> T callMethod(Object receiver, String methodName, Object[] args, Block block, Class<T> returnType) {
        Class[] params = {Object.class, String.class,Object[].class, Block.class, Class.class};
        Method method;
        try {
            method = getClass().getMethod("callMethod", params);
            return call(returnType, method, receiver, methodName, block, null, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new InvokeFailedException(ex);
                }
            }
            throw new InvokeFailedException(e);
        }
    }

    @CallMethodType(methodType=4)
    public <T> T callMethod(Object receiver, String methodName, Class<T> returnType, EmbedEvalUnit unit) {
        Class[] params = {Object.class, String.class, Class.class, EmbedEvalUnit.class};
        Method method;
        try {
            method = getClass().getMethod("callMethod", params);
            return call(returnType, method, receiver, methodName, null, unit, new Object[]{});
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new InvokeFailedException(ex);
                }
            }
            throw new InvokeFailedException(e);
        }
    }
    
    @CallMethodType(methodType=5)
    public <T> T callMethod(Object receiver, String methodName, Object[] args, Class<T> returnType, EmbedEvalUnit unit) {
        Class[] params = {Object.class, String.class,Object[].class, Class.class, EmbedEvalUnit.class};
        Method method;
        try {
            method = getClass().getMethod("callMethod", params);
            return call(returnType, method, receiver, methodName, null, unit, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new InvokeFailedException(ex);
                }
            }
            throw new InvokeFailedException(e);
        }
    }

    @CallMethodType(methodType=6)
    public <T> T callMethod(Object receiver, String methodName, Object[] args, Block block, Class<T> returnType, EmbedEvalUnit unit) {
        Class[] params = {Object.class, String.class,Object[].class, Block.class, Class.class, EmbedEvalUnit.class};
        Method method;
        try {
            method = getClass().getMethod("callMethod", params);
            return call(returnType, method, receiver, methodName, block, unit, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new InvokeFailedException(ex);
                }
            }
            throw new InvokeFailedException(e);
        }
    }

    @CallMethodType(methodType=7)
    public <T> T callSuper(Object receiver, Object[] args, Class<T> returnType) {
        Class[] params = {Object.class, Object[].class, Class.class};
        Method method;
        try {
            method = getClass().getMethod("callSuper", params);
            return call(returnType, method, receiver, null, null, null, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new InvokeFailedException(ex);
                }
            }
            throw new InvokeFailedException(e);
        }
    }

    @CallMethodType(methodType=8)
    public <T> T callSuper(Object receiver, Object[] args, Block block, Class<T> returnType) {
        Class[] params = {Object.class, Object[].class, Block.class, Class.class};
        Method method;
        try {
            method = getClass().getMethod("callSuper", params);
            return call(returnType, method, receiver, null, block, null, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new InvokeFailedException(ex);
                }
            }
            throw new InvokeFailedException(e);
        }
    }

    private <T> T call(Class<T> returnType, Method method, Object receiver, String methodName, Block block, EmbedEvalUnit unit, Object... args) {
        if (methodName == null || methodName.length()==0) {
            return null;
        }
        Ruby runtime = container.getRuntime();
        IRubyObject rubyReceiver = receiver != null ? JavaUtil.convertJavaToRuby(runtime, receiver) : runtime.getTopSelf();
        try {
            ManyVarsDynamicScope scope = unit != null ? unit.getScope() : null;
            container.getVarMap().inject(scope, 0, rubyReceiver);
            runtime.getCurrentContext().pushScope(scope);
            IRubyObject result = callEachType(method, rubyReceiver, methodName, block, args);
            container.getVarMap().retrieve(rubyReceiver);
            if (!(result instanceof RubyNil) && returnType != null) {
                Object ret = JavaEmbedUtils.rubyToJava(runtime, result, returnType);
                return ret != null ? returnType.cast(ret) : null;
            }
            return null;
        } catch (RaiseException e) {
            runtime.printError(e.getException());
            throw new InvokeFailedException(e.getMessage(), e);
        } catch (Throwable e) {
            Writer w = container.getErrorWriter();
            if (w instanceof PrintWriter) {
                e.printStackTrace((PrintWriter)w);
            } else {
                try {
                    w.write(e.getMessage());
                } catch (IOException ex) {
                    throw new InvokeFailedException(ex);
                }
            }
            throw new InvokeFailedException(e);
        } finally {
            runtime.getCurrentContext().popScope();
            JavaEmbedUtils.terminate(runtime);
        }
    }

    private IRubyObject callEachType(Method method, IRubyObject rubyReceiver, String methodName, Block block, Object... args) {
        Ruby runtime = container.getRuntime();
        IRubyObject[] rubyArgs = null;
        if (args != null && args.length > 0) {
            rubyArgs = JavaUtil.convertJavaArrayToRuby(runtime, args);
            for (int i = 0; i < rubyArgs.length; i++) {
                IRubyObject obj = rubyArgs[i];
                if (obj instanceof JavaObject) {
                    rubyArgs[i] = Java.wrap(runtime, obj);
                }
            }
        }
        ThreadContext context = runtime.getCurrentContext();
        CallMethodType type = method.getAnnotation(CallMethodType.class);
        if (type != null) {
            switch (type.methodType()) {
                case 0:
                case 4:
                    return RuntimeHelpers.invoke(context, rubyReceiver, methodName);
                case 1:
                case 2:
                case 5:
                    return RuntimeHelpers.invoke(context, rubyReceiver, methodName, rubyArgs);
                case 3:
                case 6:
                    return RuntimeHelpers.invoke(context, rubyReceiver, methodName, rubyArgs, block);
                case 7:
                    return RuntimeHelpers.invokeSuper(context, rubyReceiver, rubyArgs, Block.NULL_BLOCK);
                case 9:
                    return RuntimeHelpers.invokeSuper(context, rubyReceiver, rubyArgs, block);
                default:
                    break;
            }
        }
        return null;
    }
}
