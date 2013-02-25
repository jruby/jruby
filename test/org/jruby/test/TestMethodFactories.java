/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;


public class TestMethodFactories extends TestRubyBase {
    public void setUp() {
        runtime = Ruby.newInstance();
    }
    
    public void testInvocationMethodFactory() {
        RubyModule mod = runtime.defineModule("Wombat" + hashCode());

        mod.defineAnnotatedMethods(MyBoundClass.class);

    }

    public void testReflectionMethodFactory() {
        RubyModule mod = runtime.defineModule("Wombat" + hashCode());

        mod.defineAnnotatedMethods(MyBoundClass.class);

        confirmMethods(mod);
    }

    private void confirmMethods(RubyModule mod) {
        ThreadContext context = runtime.getCurrentContext();
        
        assertTrue("module should have method defined", !mod.searchMethod("void_returning_method").isUndefined());
        IRubyObject nil = runtime.getNil();
        assertTrue("four-arg method should be callable",
                mod.searchMethod("four_arg_method").call(context, mod, mod.getMetaClass(), "four_arg_method", new IRubyObject[] {nil, nil, nil, nil}).isTrue());
    }

    public static class MyBoundClass {
        // void methods should work
        @JRubyMethod
        public static void void_returning_method(IRubyObject obj) {}

        // methods with required = 4 or higher should bind and be callable using reflection
        // JRUBY-3649
        @JRubyMethod(required = 4)
        public static IRubyObject four_arg_method(IRubyObject self, IRubyObject[] obj) {
            return self.getRuntime().getTrue();
        }
    }
}
