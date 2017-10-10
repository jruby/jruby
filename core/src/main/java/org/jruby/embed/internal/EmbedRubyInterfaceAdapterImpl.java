/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.internal;

import org.jruby.Ruby;
import org.jruby.RubyNil;
import org.jruby.embed.EmbedRubyInterfaceAdapter;
import org.jruby.embed.InvokeFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The implementation of {@link EmbedRubyInterfaceAdapter} and implements the
 * method that gets a instance of requested interface, which is implemented in Ruby.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class EmbedRubyInterfaceAdapterImpl implements EmbedRubyInterfaceAdapter {

    private final ScriptingContainer container;

    public EmbedRubyInterfaceAdapterImpl(ScriptingContainer container) {
        this.container = container;
    }

    /**
     * Returns a instance of a requested interface type from a previously evaluated script.
     *
     * @param receiver a receiver of the previously evaluated script.
     * @param clazz an interface type of the returning instance.
     * @return an instance of requested interface type.
     */
    public <T> T getInstance(Object receiver, Class<T> clazz) {
        if (clazz == null || !clazz.isInterface()) {
            return null;
        }
        final Ruby runtime = container.getProvider().getRuntime();
        final Object obj;
        if (receiver == null || receiver instanceof RubyNil) {
            obj = JavaEmbedUtils.rubyToJava(runtime, runtime.getTopSelf(), clazz);
        } else if (receiver instanceof IRubyObject) {
            obj = JavaEmbedUtils.rubyToJava(runtime, (IRubyObject) receiver, clazz);
        } else {
            IRubyObject rubyReceiver = JavaUtil.convertJavaToRuby(runtime, receiver);
            obj = JavaEmbedUtils.rubyToJava(runtime, rubyReceiver, clazz);
        }
        final String name = clazz.getName();
        final ClassLoader loader = obj.getClass().getClassLoader();
        try {
            @SuppressWarnings("unchecked")
            Class<T> klass = (Class<T>) Class.forName(name, true, loader);
            return klass.cast(obj);
        }
        catch (ClassNotFoundException e) {
            throw new InvokeFailedException(e);
        }
    }

}
