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
package org.jruby.ext.delegate;

import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyKernel;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

/**
 * This library extends the delegate stdlib with a native method_missing, in
 * order to reduce the cost of delegating through to the target object. It only
 * supports 1.9 and higher, since 1.8 actively defined all methods from the
 * target object rather than simply delegating all calls through method_missing.
 * 
 * Note that this does no call site caching and interferes with advanced call
 * site binding logic as found in our invokedynamic support. Future improvement
 * would be to cache recent methods or wire up indy logic to see through
 * delegation.
 */
public class NativeDelegateLibrary implements Library{
    public void load(Ruby runtime, boolean wrap) {
        assert !runtime.is1_8() : "the native delegator extension is not compatible with 1.8";
        
        RubyClass delegateClass = runtime.getClass("Delegator");
        delegateClass.defineAnnotatedMethods(NativeDelegateLibrary.class);
    }
    
    @JRubyMethod(omit = true)
    public static IRubyObject method_missing(ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) {
        String methodName = arg0.asJavaString();
        RubyBasicObject object = (RubyBasicObject)self.callMethod(context, "__getobj__");
        
        DynamicMethod method = object.getMetaClass().searchMethod(methodName);
        
        if (method.isUndefined()) {
            // catch respond_to? and respond_to_missing? cases
            if (object.callMethod(context, "respond_to?", arg0).isTrue()) {
                return object.callMethod(context, methodName, IRubyObject.NULL_ARRAY, block);
            }
            RubyKernel.methodMissing(context, self, methodName, Visibility.PUBLIC, CallType.FUNCTIONAL, IRubyObject.NULL_ARRAY, block);
        } else if (method.getVisibility().isPrivate()) {
            RubyKernel.methodMissing(context, self, methodName, Visibility.PRIVATE, CallType.FUNCTIONAL, IRubyObject.NULL_ARRAY, block);
        }
        
        return method.call(context, object, object.getMetaClass(), methodName, block);
    }

    @JRubyMethod(omit = true)
    public static IRubyObject method_missing(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        String methodName = arg0.asJavaString();
        RubyBasicObject object = (RubyBasicObject)self.callMethod(context, "__getobj__");
        
        DynamicMethod method = object.getMetaClass().searchMethod(methodName);
        
        if (method.isUndefined()) {
            // catch respond_to? and respond_to_missing? cases
            if (object.callMethod(context, "respond_to?", arg0).isTrue()) {
                return object.callMethod(context, methodName, new IRubyObject[] {arg1}, block);
            }
            RubyKernel.methodMissing(context, self, methodName, Visibility.PUBLIC, CallType.FUNCTIONAL, new IRubyObject[] {arg1}, block);
        } else if (method.getVisibility().isPrivate()) {
            RubyKernel.methodMissing(context, self, methodName, Visibility.PRIVATE, CallType.FUNCTIONAL, new IRubyObject[] {arg1}, block);
        }
        
        return method.call(context, object, object.getMetaClass(), methodName, arg1, block);
    }

    @JRubyMethod(omit = true)
    public static IRubyObject method_missing(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        String methodName = arg0.asJavaString();
        RubyBasicObject object = (RubyBasicObject)self.callMethod(context, "__getobj__");
        
        DynamicMethod method = object.getMetaClass().searchMethod(methodName);
        
        if (method.isUndefined()) {
            // catch respond_to? and respond_to_missing? cases
            if (object.callMethod(context, "respond_to?", arg0).isTrue()) {
                return object.callMethod(context, methodName, new IRubyObject[] {arg1, arg2}, block);
            }
            RubyKernel.methodMissing(context, self, methodName, Visibility.PUBLIC, CallType.FUNCTIONAL, new IRubyObject[] {arg1, arg2}, block);
        } else if (method.getVisibility().isPrivate()) {
            RubyKernel.methodMissing(context, self, methodName, Visibility.PRIVATE, CallType.FUNCTIONAL, new IRubyObject[] {arg1, arg2}, block);
        }
        
        return method.call(context, object, object.getMetaClass(), methodName, arg1, arg2, block);
    }

    @JRubyMethod(required = 1, rest = true, omit = true)
    public static IRubyObject method_missing(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        IRubyObject[] newArgs = new IRubyObject[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        String methodName = args[0].asJavaString();
        RubyBasicObject object = (RubyBasicObject)self.callMethod(context, "__getobj__");
        
        DynamicMethod method = object.getMetaClass().searchMethod(methodName);
        
        if (method.isUndefined()) {
            // catch respond_to? and respond_to_missing? cases
            if (object.callMethod(context, "respond_to?", args[0]).isTrue()) {
                return object.callMethod(context, methodName, newArgs, block);
            }
            RubyKernel.methodMissing(context, self, methodName, Visibility.PUBLIC, CallType.FUNCTIONAL, newArgs, block);
        } else if (method.getVisibility().isPrivate()) {
            RubyKernel.methodMissing(context, self, methodName, Visibility.PRIVATE, CallType.FUNCTIONAL, newArgs, block);
        }
        
        return method.call(context, object, object.getMetaClass(), methodName, newArgs, block);
    }
}
