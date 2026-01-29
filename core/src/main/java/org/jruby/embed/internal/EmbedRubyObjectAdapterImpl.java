/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009-2012 Yoko Harada <yokolet@gmail.com>
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
package org.jruby.embed.internal;

import org.jruby.Ruby;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyObjectAdapter;
import org.jruby.RubyString;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.EmbedRubyObjectAdapter;
import org.jruby.embed.InvokeFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.variable.BiVariable;
import org.jruby.embed.variable.InstanceVariable;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.embed.internal.EmbedEvalUnitImpl.isSharingVariables;
import static org.jruby.embed.internal.EmbedRubyRuntimeAdapterImpl.createLocalVarScope;

/**
 * Implementation of {@link EmbedRubyObjectAdapter}. Users get an instance of this
 * class by newObjectAdapter() method of {@link ScriptingContainer}.
 *
 * @author Yoko Harada &lt;<a href="mailto:yokolet@gmail.com">yokolet@gmail.com</a>&gt;
 */
public class EmbedRubyObjectAdapterImpl implements EmbedRubyObjectAdapter {

    private final RubyObjectAdapter adapter = JavaEmbedUtils.newObjectAdapter();
    private final ScriptingContainer container;
    private final boolean wrapExceptions;

    @Deprecated(since = "9.3.0.0") // not used
    public enum MethodType {
        CALLMETHOD,
        CALLSUPER,
        CALLMETHOD_NOARG,
        CALLMETHOD_WITHBLOCK,
        CALLSUPER_WITHBLOCK,
    }

    public EmbedRubyObjectAdapterImpl(ScriptingContainer container) {
        this(container, false);
    }

    public EmbedRubyObjectAdapterImpl(ScriptingContainer container, boolean wrapExceptions) {
        this.container = container;
        this.wrapExceptions = wrapExceptions;
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

    public IRubyObject setInstanceVariable(IRubyObject obj, String variableName, IRubyObject value) {
        BiVariableMap map = container.getVarMap();
        synchronized (map) {
            if (map.containsKey(variableName)) {
                BiVariable bv = map.getVariable((RubyObject) getTopSelf(), variableName);
                bv.setRubyObject(value);
            } else {
                InstanceVariable iv = new InstanceVariable(obj, variableName, value);
                map.update(variableName, iv);
            }
        }
        return obj.getInstanceVariables().setInstanceVariable(variableName, value);
    }

    public IRubyObject getInstanceVariable(IRubyObject obj, String variableName) {
        BiVariableMap map = container.getVarMap();
        synchronized (map) {
            if (map.containsKey(variableName)) {
                BiVariable bv = map.getVariable((RubyObject) getTopSelf(), variableName);
                return bv.getRubyObject();
            }
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

    public <T> T callMethod(Object receiver, String methodName, Class<T> returnType) {
        return doInvokeMethod(returnType, getReceiverObject(receiver), methodName, Block.NULL_BLOCK, null);
    }

    public <T> T callMethod(Object receiver, String methodName, Object singleArg, Class<T> returnType) {
        return doInvokeMethod(returnType, getReceiverObject(receiver), methodName, Block.NULL_BLOCK, null, singleArg);
    }

    public <T> T callMethod(Object receiver, String methodName, Object[] args, Class<T> returnType) {
        return doInvokeMethod(returnType, getReceiverObject(receiver), methodName, Block.NULL_BLOCK, null, args);
    }

    public <T> T callMethod(Object receiver, String methodName, Object[] args, Block block, Class<T> returnType) {
        return doInvokeMethod(returnType, getReceiverObject(receiver), methodName, block, null, args);
    }

    public <T> T callMethod(Object receiver, String methodName, Class<T> returnType, EmbedEvalUnit unit) {
        return doInvokeMethod(returnType, getReceiverObject(receiver), methodName, Block.NULL_BLOCK, unit);
    }

    public <T> T callMethod(Object receiver, String methodName, Object[] args, Class<T> returnType, EmbedEvalUnit unit) {
        return doInvokeMethod(returnType, getReceiverObject(receiver), methodName, Block.NULL_BLOCK, unit, args);
    }

    public <T> T callMethod(Object receiver, String methodName, Object[] args, Block block, Class<T> returnType, EmbedEvalUnit unit) {
        return doInvokeMethod(returnType, getReceiverObject(receiver), methodName, block, unit, args);
    }

    public <T> T callSuper(Object receiver, Object[] args, Class<T> returnType) {
        return doInvokeSuper(returnType, getReceiverObject(receiver), Block.NULL_BLOCK, null, args);
    }

    public <T> T callSuper(Object receiver, Object[] args, Block block, Class<T> returnType) {
        return doInvokeSuper(returnType, getReceiverObject(receiver), block, null, args);
    }

    public Object callMethod(Object receiver, String methodName, Object... args) {
        return doInvokeMethod(Object.class, getReceiverObject(receiver), methodName, Block.NULL_BLOCK, null, args);
    }

    public Object callMethod(Object receiver, String methodName, Block block, Object... args) {
        return doInvokeMethod(Object.class, getReceiverObject(receiver), methodName, block, null, args);
    }

    public <T> T runRubyMethod(Class<T> returnType, Object receiver, String methodName, Block block, Object... args) {
        assert block != null;
        IRubyObject rubyReceiver = JavaEmbedUtils.javaToRuby(container.getProvider().getRuntime(), receiver);
        return doInvokeMethod(returnType, rubyReceiver, methodName, block, null, args);
    }

    private <T> T doInvokeSuper(Class<T> returnType, IRubyObject rubyReceiver, Block block, EmbedEvalUnit unit, Object... args) {
        final Ruby runtime = container.getProvider().getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final boolean sharing_variables = isSharingVariables(container);

        if (sharing_variables) {
            beforeSharingVariablesCall(context, unit);
        }

        try {
            IRubyObject result = Helpers.invokeSuper(context, rubyReceiver, convertArgs(runtime, args), block);
            if (sharing_variables) {
                container.getVarMap().retrieve(rubyReceiver);
            }
            if (returnType != null) {
                Object ret = JavaEmbedUtils.rubyToJava(runtime, result, returnType);
                return returnType.cast(ret);
            }
            return null;
        } catch (Exception e) {
            if (e instanceof InvokeFailedException) throw e;
            if (wrapExceptions) throw new InvokeFailedException(e);
            Helpers.throwException(e); return null; // never returns
        } finally {
            if (sharing_variables) {
                afterSharingVariablesCall(context);
            }
        }
    }

    private <T> T doInvokeMethod(Class<T> returnType, IRubyObject rubyReceiver, String methodName, Block block, EmbedEvalUnit unit, Object... args) {
        final Ruby runtime = container.getProvider().getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final boolean sharing_variables = isSharingVariables(container);

        if (sharing_variables) {
            beforeSharingVariablesCall(context, unit);
        }
        try {
            IRubyObject result = Helpers.invoke(context, rubyReceiver, methodName, convertArgs(runtime, args), block);
            if (sharing_variables) {
                container.getVarMap().retrieve(rubyReceiver);
            }
            if (returnType != null) {
                Object ret = JavaEmbedUtils.rubyToJava(runtime, result, returnType);
                return returnType.cast(ret);
            }
            return null;
        } catch (Exception e) {
            if (e instanceof InvokeFailedException) throw e;
            if (wrapExceptions) throw new InvokeFailedException(e);
            Helpers.throwException(e); return null; // never returns
        } finally {
            if (sharing_variables) {
                afterSharingVariablesCall(context);
            }
        }
    }

    private void beforeSharingVariablesCall(final ThreadContext context, EmbedEvalUnit unit) {
        DynamicScope scope;
        if (unit != null && unit.getLocalVarScope() != null) scope = unit.getLocalVarScope();
        else scope = createLocalVarScope(context.runtime, container.getVarMap().getLocalVarNames());
        container.getVarMap().inject(scope);
        context.pushScope(scope);
    }

    private void afterSharingVariablesCall(final ThreadContext context) {
        context.popScope();
    }

    private IRubyObject getReceiverObject(Object receiver) {
        if (receiver instanceof IRubyObject) return (IRubyObject) receiver;
        return getTopSelf();
    }

    private IRubyObject getTopSelf() {
        return container.getProvider().getRuntime().getTopSelf();
    }

    @SuppressWarnings("deprecation")
    private static IRubyObject[] convertArgs(final Ruby runtime, final Object[] args) {
        IRubyObject[] rubyArgs = JavaUtil.convertJavaArrayToRuby(runtime, args);
        for (int i = 0; i < rubyArgs.length; i++) {
            IRubyObject obj = rubyArgs[i];
            if (obj instanceof org.jruby.javasupport.JavaObject) {
                rubyArgs[i] = Java.wrap(runtime, obj);
            }
        }
        return rubyArgs;
    }

}
