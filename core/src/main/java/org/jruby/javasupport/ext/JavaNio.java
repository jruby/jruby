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
 * Copyright (C) 2020 The JRuby Team
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
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RubyStringBuilder;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.javasupport.JavaUtil.unwrapIfJavaObject;
import static org.jruby.runtime.Visibility.PUBLIC;
import static org.jruby.util.Inspector.GT;
import static org.jruby.util.Inspector.SPACE;
import static org.jruby.util.Inspector.inspectPrefix;

/**
 * <code>java.nio</code> package additions.
 *
 * @author kares
 */
public abstract class JavaNio {

    public static void define(ThreadContext context) {
        var runtime = context.runtime;
        JavaExtensions.put(runtime, java.nio.Buffer.class, (proxy) -> Buffer.define(context, (RubyClass) proxy));
        // make sure it does not use CharSequence#inspect but rather a unified Buffer#inspect format:
        JavaExtensions.put(runtime, java.nio.CharBuffer.class, (proxy) -> proxy.addMethod(context, "inspect", new InspectBuffer(proxy)));
    }

    @JRubyModule(name = "Java::JavaNio::Buffer")
    public static class Buffer {
        static RubyModule define(ThreadContext context, final RubyClass proxy) {
            proxy.defineMethods(context, Buffer.class).
                    addMethod(context, "inspect", new InspectBuffer(proxy));
            return proxy;
        }

        @JRubyMethod(name = {"length", "size"})
        public static IRubyObject length(final ThreadContext context, final IRubyObject self) {
            java.nio.Buffer obj = self.toJava(java.nio.Buffer.class);
            return asFixnum(context, obj.remaining()); // limit - position
        }

    }

    private static final class InspectBuffer extends JavaMethod.JavaMethodZero {

        InspectBuffer(RubyModule implClass) {
            super(implClass, PUBLIC, "inspect");
        }

        @Override
        public IRubyObject call(final ThreadContext context, final IRubyObject self, final RubyModule clazz, final java.lang.String name) {
            java.nio.Buffer obj = unwrapIfJavaObject(self);

            RubyString buf = inspectPrefix(context, self.getMetaClass(), System.identityHashCode(obj));
            RubyStringBuilder.cat(context.runtime, buf, SPACE);
            buf.catString("position=" + obj.position() + ", limit=" + obj.limit() + ", capacity=" + obj.capacity() + ", readOnly=" + obj.isReadOnly());
            RubyStringBuilder.cat(context.runtime, buf, GT); // >
            return buf;
        }
    }

}
