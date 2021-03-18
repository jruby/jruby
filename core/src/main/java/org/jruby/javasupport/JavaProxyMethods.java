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

package org.jruby.javasupport;

import org.jruby.RubyBasicObject;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaProxyMethods {

    private JavaProxyMethods() { /* no instances */ }

    public static RubyModule createJavaProxyMethods(ThreadContext context) {
        RubyModule JavaProxyMethods = context.runtime.defineModule("JavaProxyMethods");
        JavaProxyMethods.defineAnnotatedMethods(JavaProxyMethods.class);
        return JavaProxyMethods;
    }
    
    @JRubyMethod
    public static IRubyObject java_class(ThreadContext context, IRubyObject recv) {
        return JavaProxy.getJavaClass(recv.getMetaClass().getRealClass());
    }

    @JRubyMethod
    public static IRubyObject java_object(ThreadContext context, IRubyObject recv) {
        Object javaObj = recv.dataGetStruct();
        if (javaObj instanceof IRubyObject) {
            return (IRubyObject) javaObj;
        }
        if (javaObj == null) return context.nil;
        return Java.getInstance(context.runtime, javaObj);
    }

    @JRubyMethod
    public static IRubyObject to_java_object(IRubyObject recv) {
        return (JavaObject) recv.dataGetStruct();
    }

    @JRubyMethod(name = "eql?")
    public static IRubyObject op_eql(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        return op_equal(context, recv, obj);
    }

    @JRubyMethod(name = "==")
    public static IRubyObject op_equal(ThreadContext context, IRubyObject recv, IRubyObject other) {
        if (recv instanceof JavaProxy) {
            return JavaObject.equals(context.runtime, ((JavaProxy) recv).getObject(), other);
        }
        // NOTE: only JavaProxy includes JavaProxyMethods
        // these is only here for 'manual' JavaObject wrapping :
        if (recv.dataGetStruct() instanceof RubyBasicObject) {
            return ((RubyBasicObject) recv.dataGetStruct()).op_equal(context, other);
        }
        return context.nil;
    }
    
    @JRubyMethod
    public static IRubyObject to_s(ThreadContext context, IRubyObject recv) {
        if (recv instanceof JavaProxy) {
            return JavaObject.to_s(context.runtime, ((JavaProxy) recv).getObject());
        }
        // NOTE: only JavaProxy includes JavaProxyMethods
        // these is only here for 'manual' JavaObject wrapping :
        if (recv.dataGetStruct() instanceof IRubyObject) {
            return ((RubyBasicObject) recv.dataGetStruct()).to_s();
        }
        return ((RubyBasicObject) recv).to_s();
    }

    @JRubyMethod
    public static IRubyObject inspect(IRubyObject recv) {
        if (recv instanceof RubyBasicObject) {
            return ((RubyBasicObject) recv).hashyInspect();
        }
        return recv.inspect();
    }
    
    @JRubyMethod
    public static IRubyObject hash(ThreadContext context, IRubyObject recv) {
        if (recv instanceof JavaProxy) {
            return RubyFixnum.newFixnum(context.runtime, ((JavaProxy) recv).getObject().hashCode());
        }
        // NOTE: only JavaProxy includes JavaProxyMethods
        // these is only here for 'manual' JavaObject wrapping :
        if (recv.dataGetStruct() instanceof IRubyObject) {
            return ((RubyBasicObject) recv.dataGetStruct()).hash();
        }
        return ((RubyBasicObject) recv).hash();
    }
    
    @JRubyMethod(name = "synchronized")
    public static IRubyObject rbSynchronized(ThreadContext context, IRubyObject recv, Block block) {
        if (recv instanceof JavaProxy) {
            return JavaObject.ruby_synchronized(context, ((JavaProxy) recv).getObject(), block);
        }
        final Object lock;
        // NOTE: only JavaProxy includes JavaProxyMethods
        // these is only here for 'manual' JavaObject wrapping :
        if (recv.dataGetStruct() instanceof IRubyObject) {
            lock = recv.dataGetStruct();
        } else {
            lock = recv;
        }
        synchronized (lock) { return block.yield(context, null); }
    }
}
