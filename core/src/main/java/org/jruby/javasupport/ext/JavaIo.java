/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2016 The JRuby Team
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

package org.jruby.javasupport.ext;

import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.javasupport.JavaUtil.unwrapIfJavaObject;
import static org.jruby.runtime.Visibility.PUBLIC;

/**
 * <code>java.io</code> package Ruby additions.
 *
 * @author kares
 */
public abstract class JavaIo {

    public static void define(ThreadContext context) {
        JavaExtensions.put(context.runtime, java.io.InputStream.class, (proxyClass) -> {
            proxyClass.addMethodInternal(context, "to_io", new InputStreamToIO(proxyClass));
        });

        JavaExtensions.put(context.runtime, java.io.OutputStream.class, (proxyClass) -> {
            proxyClass.addMethodInternal(context, "to_io", new OutputStreamToIO(proxyClass));
        });

        JavaExtensions.put(context.runtime, java.nio.channels.Channel.class, (proxyClass) -> {
            proxyClass.addMethodInternal(context, "to_io", new ChannelToIO(proxyClass));
        });
    }

    private static final class InputStreamToIO extends JavaMethod.JavaMethodZeroOrOne {

        InputStreamToIO(RubyModule implClass) {
            super(implClass, PUBLIC, "to_io");
        }

        @Override
        public RubyIO call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            return to_io(context, (java.io.InputStream) unwrapIfJavaObject(self), null);
        }

        @Override
        public RubyIO call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject opts) {
            return to_io(context, (java.io.InputStream) unwrapIfJavaObject(self), opts);
        }

    }

    static RubyIO to_io(final ThreadContext context, final java.io.InputStream stream, final IRubyObject opts) {
        final RubyIO io = new RubyIO(context.runtime, stream);
        setAutoclose(context, io, opts);
        return io;
    }

    private static final class OutputStreamToIO extends JavaMethod.JavaMethodZeroOrOne {

        OutputStreamToIO(RubyModule implClass) {
            super(implClass, PUBLIC, "to_io");
        }

        @Override
        public RubyIO call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            return to_io(context, (java.io.OutputStream) unwrapIfJavaObject(self), null);
        }

        @Override
        public RubyIO call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject opts) {
            return to_io(context, (java.io.OutputStream) unwrapIfJavaObject(self), opts);
        }

    }

    static RubyIO to_io(final ThreadContext context, final java.io.OutputStream stream, final IRubyObject opts) {
        final RubyIO io = new RubyIO(context.runtime, stream);
        setAutoclose(context, io, opts);
        return io;
    }

    private static final class ChannelToIO extends JavaMethod.JavaMethodZeroOrOne {

        ChannelToIO(RubyModule implClass) {
            super(implClass, PUBLIC, "to_io");
        }

        @Override
        public RubyIO call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            return to_io(context, self, null);
        }

        @Override
        public RubyIO call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject opts) {
            return to_io(context, self, opts);
        }

        private static RubyIO to_io(final ThreadContext context, final IRubyObject self, final IRubyObject opts) {
            final RubyIO io = new RubyIO(context.runtime, (java.nio.channels.Channel) unwrapIfJavaObject(self));
            setAutoclose(context, io, opts);
            return io;
        }

    }

    private static void setAutoclose(final ThreadContext context, final RubyIO io, final IRubyObject opts) {
        if ( opts != null && opts != context.nil ) {
            IRubyObject autoclose = opts.callMethod(context, "[]", asSymbol(context, "autoclose"));
            if ( autoclose != context.nil ) io.setAutoclose( autoclose.isTrue() );
        }
    }

}