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
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

/**
 * An unbound method representation (e.g. when retrieving an instance method from a class - isn't bound to any instance).
 *
 * @note This was renamed from UnboundMethod.java
 * @author jpetersen
 */
@JRubyClass(name="UnboundMethod", parent="Method")
public class RubyUnboundMethod extends AbstractRubyMethod {
    protected RubyUnboundMethod(Ruby runtime) {
        super(runtime, runtime.getUnboundMethod());
    }

    public static RubyUnboundMethod newUnboundMethod(
        RubyModule implementationModule,
        String methodName,
        RubyModule originModule,
        String originName,
        CacheEntry entry) {
        RubyUnboundMethod newMethod = new RubyUnboundMethod(implementationModule.getRuntime());

        newMethod.implementationModule = implementationModule;
        newMethod.methodName = methodName;
        newMethod.originModule = originModule;
        newMethod.originName = originName;
        newMethod.entry = entry;
        newMethod.method = entry.method;
        newMethod.sourceModule = entry.sourceModule;

        return newMethod;
    }

    public static RubyClass defineUnboundMethodClass(Ruby runtime) {
        RubyClass newClass = 
        	runtime.defineClass("UnboundMethod", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        newClass.setClassIndex(ClassIndex.UNBOUNDMETHOD);
        newClass.setReifiedClass(RubyUnboundMethod.class);

        newClass.defineAnnotatedMethods(AbstractRubyMethod.class);
        newClass.defineAnnotatedMethods(RubyUnboundMethod.class);

        newClass.getSingletonClass().undefineMethod("new");

        return newClass;
    }

    @Override
    @JRubyMethod(name = "==", required = 1)
    public RubyBoolean op_equal(ThreadContext context, IRubyObject other) {
        return RubyBoolean.newBoolean(context,  equals(other) );
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AbstractRubyMethod)) return false;
        if (method instanceof ProcMethod) {
            return ((ProcMethod) method).isSame(((AbstractRubyMethod) other).getMethod());
        }
        AbstractRubyMethod otherMethod = (AbstractRubyMethod) other;
        return originModule == otherMethod.originModule &&
               method.getRealMethod().getSerialNumber() == otherMethod.method.getRealMethod().getSerialNumber();
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return context.runtime.newFixnum(hashCode());
    }

    @Override
    public int hashCode() {
        long serial = method.getRealMethod().getSerialNumber();
        return 997 * ((int) (serial >> 32) ^ (int) serial & 0xFF);
    }

    @JRubyMethod
    public RubyMethod bind(ThreadContext context, IRubyObject aReceiver) {
        RubyClass receiverClass = aReceiver.getMetaClass();
        
        receiverClass.checkValidBindTargetFrom(context, (RubyModule) owner(context), true);
        
        return RubyMethod.newMethod(implementationModule, methodName, receiverClass, originName, entry, aReceiver);
    }

    @JRubyMethod(name = "clone")
    @Override
    public RubyUnboundMethod rbClone() {
        return newUnboundMethod(implementationModule, methodName, originModule, originName, entry);
    }

    @JRubyMethod(name = {"inspect", "to_s"})
    @Override
    public IRubyObject inspect() {
        StringBuilder str = new StringBuilder(24).append("#<");
        char sharp = '#';

        str.append(getMetaClass().getRealClass().getName()).append(": ");

        if (implementationModule.isSingleton()) {
            str.append(implementationModule.inspect().toString());
        } else {
            str.append(originModule.getName());

            if (implementationModule != originModule) {
                str.append('(').append(implementationModule.getName()).append(')');
            }
        }

        str.append(sharp).append(methodName); // (real-name) if alias
        final String realName= method.getRealMethod().getName();
        if ( realName != null && ! methodName.equals(realName) ) {
            str.append('(').append(realName).append(')');
        }
        str.append('>');

        RubyString res = RubyString.newString(getRuntime(), str);
        res.setTaint(isTaint());
        return res;
    }

    @JRubyMethod
    public IRubyObject super_method(ThreadContext context ) {
        return super_method(context, null, sourceModule.getSuperClass());
    }
}
