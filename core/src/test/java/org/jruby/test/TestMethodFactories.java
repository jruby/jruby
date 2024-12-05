/*
 ***** BEGIN LICENSE BLOCK *****
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

package org.jruby.test;


import org.jruby.Ruby;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.ArgumentError;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Define.defineModule;


public class TestMethodFactories extends Base {
    private ThreadContext context;

    public void setUp() {
        runtime = Ruby.newInstance();
        context.runtime.getCurrentContext();
    }
    
    public void testInvocationMethodFactory() {
        var mod = defineModule(context, "Wombat" + hashCode()).defineMethods(context, MyBoundClass.class);
        confirmCheckArity(mod);
    }

    public void testReflectionMethodFactory() {
        RubyModule mod = defineModule(context, "Wombat" + hashCode()).defineMethods(context, MyBoundClass.class);

        confirmMethods(mod);
    }

    private void confirmMethods(RubyModule mod) {
        assertTrue("module should have method defined", !mod.searchMethod("void_returning_method").isUndefined());
        IRubyObject nil = context.nil;
        assertTrue("four-arg method should be callable",
                mod.searchMethod("four_arg_method").call(context, mod, mod.getMetaClass(), "four_arg_method", new IRubyObject[] {nil, nil, nil, nil}).isTrue());
    }

    // jruby/jruby#7851: Restore automatic arity checking with an opt-out
    private void confirmCheckArity(RubyModule mod) {
        verifyCheckArityError(mod, context, "optWithCheckArityTrue", true);
        verifyCheckArityError(mod, context, "optWithCheckArityFalse", false);
        verifyCheckArityError(mod, context, "optWithCheckArityDefault", true);
    }

    private static void verifyCheckArityError(RubyModule mod, ThreadContext context, String method, boolean error) {
        if (error) {
            try {
                mod.searchMethod(method).call(context, mod, mod.getMetaClass(), method);
                if (error) fail("optCheckArityTrue should error with zero args");
            } catch (ArgumentError ae) {
                // pass
                if (!error) fail(method + " should not error with zero args");
                return;
            }
        }
    }

    public static class MyBoundClass {
        // void methods should work
        @JRubyMethod
        public static void void_returning_method(IRubyObject obj) {}

        // methods with required = 4 or higher should bind and be callable using reflection
        // JRUBY-3649
        @JRubyMethod(required = 4)
        public static IRubyObject four_arg_method(ThreadContext context, IRubyObject self, IRubyObject[] obj) {
            return context.tru;
        }

        // jruby/jruby#7851: Restore automatic arity checking with an opt-out
        @JRubyMethod(required = 1, optional = 1, checkArity = true)
        public static IRubyObject optWithCheckArityTrue(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return context.tru;
        }

        // jruby/jruby#7851: Restore automatic arity checking with an opt-out
        @JRubyMethod(required = 1, optional = 1, checkArity = false)
        public static IRubyObject optWithCheckArityFalse(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return context.tru;
        }

        // jruby/jruby#7851: Restore automatic arity checking with an opt-out
        @JRubyMethod(required = 1, optional = 1)
        public static IRubyObject optWithCheckArityDefault(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return context.tru;
        }
    }

    public static class GH3463Module {
        @JRubyMethod(module = true)
        public static IRubyObject a_module_method(IRubyObject self) {
            return self;
        }
    }

    // Module methods define a second copy on singleton with proper implClass
    // jruby/jruby#3463
    public void testModuleMethodOwner() {
        RubyModule mod = defineModule(context, "GH3463Module").defineMethods(context, GH3463Module.class);

        DynamicMethod method = mod.getSingletonClass().searchMethod("a_module_method");

        assertEquals(mod.getSingletonClass(), method.getImplementationClass());

        RubyMethod rubyMethod = (RubyMethod)mod.method(asSymbol(context, "a_module_method"));

        assertEquals(mod.getSingletonClass(), rubyMethod.owner(context));
    }
}
