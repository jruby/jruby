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
package org.jruby.ext.delegate;

import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class Delegator {

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public static IRubyObject initialize(ThreadContext context, IRubyObject self, IRubyObject obj) {
        return context.getRuntime().getNil();
    }

    @JRubyMethod(rest = true)
    public static IRubyObject method_missing(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        IRubyObject[] newArgs;
        if (args.length == 0) {
            newArgs = IRubyObject.NULL_ARRAY;
        } else {
            newArgs = new IRubyObject[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        }
        String methodName = args[0].asJavaString();
        IRubyObject object = self.callMethod(context, "__getobj__");
        DynamicMethod method = ((RubyObject) object).getMetaClass().searchMethod(methodName);
        if (method.getVisibility().isPrivate()) {
            throw context.getRuntime().newNoMethodError("method `" + methodName + "' is private", methodName, context.getRuntime().getNil());
        }
        return method.call(context, object, object.getMetaClass(), methodName, newArgs, block);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject send(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return ((RubyObject) self.callMethod(context, "__getobj__")).send(context, args, block);
    }

    @JRubyMethod
    public static IRubyObject method(ThreadContext context, IRubyObject self, IRubyObject name) {
        final String methodName = name.asJavaString();
        final IRubyObject object = self.callMethod(context, "__getobj__");
        // try to get method from self's metaclass
        if (!self.getMetaClass().searchMethod(methodName).isUndefined()) {
            return (RubyMethod) ((RubyObject) self).method(name);
        }
        // try to get method from delegated object
        final RubyMethod method = (RubyMethod) ((RubyObject) object).method(name);
        return RubyMethod.newMethod(self.getMetaClass(), methodName, self.getMetaClass(), methodName, new JavaMethodNBlock(self.getMetaClass(), Visibility.PUBLIC) {

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                if (self.callMethod(context, "__getobj__") != object) {
                    throw context.getRuntime().newNameError("object changed", "object changed");
                }
                return method.call(context, args, block);
            }
        }, self);
    }

    @JRubyMethod(name = "respond_to?")
    public static IRubyObject repond_to_p(ThreadContext context, IRubyObject self, IRubyObject name) {
        if (self.getMetaClass().isMethodBound(name.asJavaString(), false)) {
            return context.getRuntime().getTrue();
        }
        return ((RubyObject) self.callMethod(context, "__getobj__")).callMethod(context, "respond_to?", name);
    }

    @JRubyMethod
    public static IRubyObject __getobj__(ThreadContext context, IRubyObject self) {
        throw context.getRuntime().newNotImplementedError("need to define `__getobj__'");
    }

    @JRubyMethod
    public static IRubyObject marshal_dump(ThreadContext context, IRubyObject self) {
        return (RubyObject) self.callMethod(context, "__getobj__");
    }

    @JRubyMethod
    public static IRubyObject marshal_load(ThreadContext context, IRubyObject self, IRubyObject obj) {
        return self.callMethod(context, "__setobj__", obj);
    }
    
}
