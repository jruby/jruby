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
package org.jruby.internal.runtime.methods;

import java.lang.reflect.Method;

import java.util.List;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReflectedJavaMultiMethod extends JavaMethod {
    private ReflectedJavaMethod method0;
    private ReflectedJavaMethod method1;
    private ReflectedJavaMethod method2;
    private ReflectedJavaMethod method3;
    private ReflectedJavaMethod methodN;

    public ReflectedJavaMultiMethod(
            RubyModule implementationClass, List<Method> methods, List<JRubyMethod> annotations) {
        super(implementationClass, annotations.get(1).visibility());
        
        // we take the first method found as our "n" method, since for any non-specific
        // arity they'll all error the same. If an actual "n" method is created, we
        // use that.
        ReflectedJavaMethod nMethod = null;
        boolean foundActualNMethod = false;
        
        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            JRubyMethod annotation = annotations.get(i);
            
            ReflectedJavaMethod dynMethod = new ReflectedJavaMethod(implementationClass, method, annotation);
            
            switch (dynMethod.arityValue) {
            case 0:
                method0 = dynMethod;
                if (nMethod == null && !foundActualNMethod) nMethod = dynMethod;
                break;
            case 1:
                method1 = dynMethod;
                if (nMethod == null && !foundActualNMethod) nMethod = dynMethod;
                break;
            case 2:
                method2 = dynMethod;
                if (nMethod == null && !foundActualNMethod) nMethod = dynMethod;
                break;
            case 3:
                method3 = dynMethod;
                if (nMethod == null && !foundActualNMethod) nMethod = dynMethod;
                break;
            default:
                // all other arities use "n" dispatch path
                methodN = dynMethod;
                nMethod = dynMethod;
                foundActualNMethod = true;
                break;
            }
        }
        
        // For all uninitialized specific arities, we defer to the "n" method which
        // should raise appropriate error in all cases.
        if (methodN == null) methodN = nMethod;
        if (method0 == null) method0 = methodN;
        if (method1 == null) method1 = methodN;
        if (method2 == null) method2 = methodN;
        if (method3 == null) method3 = methodN;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject[] args, Block block) {
        switch (args.length) {
        case 0:
            return method0.call(context, self, clazz, name, block);
        case 1:
            return method1.call(context, self, clazz, name, args[0], block);
        case 2:
            return method2.call(context, self, clazz, name, args[0], args[1], block);
        case 3:
            return method3.call(context, self, clazz, name, args[0], args[1], args[2], block);
        default:
            return methodN.call(context, self, clazz, name, args, block);
        }
    }
}
