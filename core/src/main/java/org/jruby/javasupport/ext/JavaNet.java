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
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;
import java.io.InputStream;

import static org.jruby.javasupport.JavaUtil.unwrapIfJavaObject;
import static org.jruby.runtime.Visibility.PUBLIC;

/**
 * <code>java.net</code> package Ruby additions.
 *
 * @author kares
 */
public abstract class JavaNet {

    public static void define(ThreadContext context) {
        JavaExtensions.put(context.runtime, java.net.URL.class, (proxyClass) ->
            proxyClass.addMethodInternal(context, "open", new URLOpenMethod(proxyClass)));
    }

    private static final class URLOpenMethod extends JavaMethod.JavaMethodZeroOrNBlock {

        URLOpenMethod(RubyModule implClass) {
            super(implClass, PUBLIC, "open");
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            java.net.URL url = unwrapIfJavaObject(self);
            final InputStream stream; final RubyIO io;
            try {
                stream = url.openStream();
                io = JavaIo.to_io(context, stream, null);

                if ( block.isGiven() ) {
                    try {
                        return block.yield(context, io);
                    }
                    finally {
                        stream.close();
                    }
                }
            }
            catch (IOException e) {
                Helpers.throwException(e); return null; // throw context.runtime.newIOErrorFromException(e);
            }
            return io; // unless block_given?
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            return call(context, self, clazz, name, Block.NULL_BLOCK);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            return call(context, self, clazz, name, block);
        }

    }

}