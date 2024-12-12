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
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.PartialDelegatingMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Define.defineClass;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;
import static org.jruby.util.RubyStringBuilder.str;

/**
 * An unbound method representation (e.g. when retrieving an instance method from a class - isn't bound to any instance).
 *
 * <p>Note: This was renamed from UnboundMethod.java</p>
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

    public static RubyClass defineUnboundMethodClass(ThreadContext context, RubyClass Object) {
        return defineClass(context, "UnboundMethod", Object, NOT_ALLOCATABLE_ALLOCATOR).
                reifiedClass(RubyUnboundMethod.class).
                classIndex(ClassIndex.UNBOUNDMETHOD).
                defineMethods(context, AbstractRubyMethod.class, RubyUnboundMethod.class).
                tap(c -> c.getSingletonClass().undefMethods(context, "new"));
    }

    @Override
    @JRubyMethod(name = "==")
    public RubyBoolean op_equal(ThreadContext context, IRubyObject other) {
        return asBoolean(context,  equals(other) );
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AbstractRubyMethod)) return false;
        if (method instanceof ProcMethod) {
            return ((ProcMethod) method).isSame(((AbstractRubyMethod) other).getMethod());
        }

        AbstractRubyMethod otherMethod = (AbstractRubyMethod) other;
        return method.getRealMethod().getSerialNumber() == otherMethod.method.getRealMethod().getSerialNumber();
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, hashCode());
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

        CacheEntry methodEntry = convertUnboundMethodToCallableEntry(context, receiverClass);

        return RubyMethod.newMethod(implementationModule, methodName, receiverClass, originName, methodEntry, aReceiver);
    }

    private CacheEntry convertUnboundMethodToCallableEntry(ThreadContext context, RubyClass receiverClass) {
        CacheEntry methodEntry = entry;

        if (implementationModule.isModule()) {
            IncludedModuleWrapper alreadyIncluded = receiverClass.findModuleInAncestors(implementationModule);

            if (alreadyIncluded != null) {
                methodEntry = new CacheEntry(method, alreadyIncluded, entry.token);
            } else {
                RubyModule boundModule = new IncludedModuleWrapper(context.runtime, receiverClass, implementationModule);
                methodEntry = new CacheEntry(method, boundModule, entry.token);
            }
        }

        return methodEntry;
    }

    @JRubyMethod(name = "clone")
    public RubyUnboundMethod rbClone() {
        RubyUnboundMethod unboundMethod = newUnboundMethod(implementationModule, methodName, originModule, originName, entry);
        ThreadContext context = getRuntime().getCurrentContext();

        return (RubyUnboundMethod) cloneSetup(context, unboundMethod, context.nil);
    }

    @JRubyMethod
    public RubyUnboundMethod dup(ThreadContext context) {
        RubyUnboundMethod unboundMethod = newUnboundMethod(implementationModule, methodName, originModule, originName, entry);

        return (RubyUnboundMethod) dupSetup(context, unboundMethod);
    }

    @JRubyMethod(required =  1, rest = true, checkArity = false, keywords = true)
    public IRubyObject bind_call(ThreadContext context, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);

        IRubyObject receiver = args[0];
        IRubyObject[] newArgs = new IRubyObject[argc - 1];
        System.arraycopy(args, 1, newArgs, 0, argc - 1);

        RubyClass receiverClass = receiver.getMetaClass();

        receiverClass.checkValidBindTargetFrom(context, (RubyModule) owner(context), true);

        CacheEntry methodEntry = convertUnboundMethodToCallableEntry(context, receiverClass);

        return method.call(context, receiver, methodEntry.sourceModule, methodName, newArgs, block);
    }

    @JRubyMethod(name = {"inspect", "to_s"})
    @Override
    public IRubyObject inspect(ThreadContext context) {
        return inspect((IRubyObject) null);
    }

    @JRubyMethod
    public IRubyObject super_method(ThreadContext context) {
        RubyModule superClass = null;
        if (method instanceof PartialDelegatingMethod || method instanceof AliasMethod) {
            RubyModule definedClass = method.getRealMethod().getDefinedClass();
            RubyModule module = sourceModule.findImplementer(definedClass);

            if (module != null) superClass = module.getSuperClass();
        } else {
            superClass = sourceModule.getSuperClass();
        }
        return super_method(context, null, superClass);
    }
}
