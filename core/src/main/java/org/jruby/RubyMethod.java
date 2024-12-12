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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.IRMethodArgs;
import org.jruby.internal.runtime.methods.PartialDelegatingMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.MethodBlockBody;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.ir.runtime.IRRuntimeHelpers.dupIfKeywordRestAtCallsite;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

/** 
 * The RubyMethod class represents a RubyMethod object.
 * 
 * You can get such a method by calling the "method" method of an object.
 * 
 * Note: This was renamed from Method.java
 * 
 * @author  jpetersen
 * @since 0.2.3
 */
@JRubyClass(name="Method")
public class RubyMethod extends AbstractRubyMethod {
    protected IRubyObject receiver;

    protected RubyMethod(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public static RubyClass createMethodClass(ThreadContext context, RubyClass Object) {
        return defineClass(context, "Method", Object, NOT_ALLOCATABLE_ALLOCATOR).
                reifiedClass(RubyMethod.class).
                classIndex(ClassIndex.METHOD).
                defineMethods(context, AbstractRubyMethod.class, RubyMethod.class);
    }

    public static RubyMethod newMethod(
        RubyModule implementationModule,
        String methodName,
        RubyModule originModule,
        String originName,
        CacheEntry entry,
        IRubyObject receiver) {
        Ruby runtime = implementationModule.getRuntime();
        RubyMethod newMethod = new RubyMethod(runtime, runtime.getMethod());

        newMethod.implementationModule = implementationModule;
        newMethod.methodName = methodName;
        newMethod.originModule = originModule;
        newMethod.originName = originName;
        newMethod.entry = entry;
        newMethod.method = entry.method;
        newMethod.sourceModule = entry.sourceModule;
        newMethod.receiver = receiver;

        return newMethod;
    }

    /** Call the method.
     * 
     */
    @JRubyMethod(name = {"call", "[]"}, keywords = true)
    public IRubyObject call(ThreadContext context, Block block) {
        return method.call(context, receiver, sourceModule, methodName, block);
    }
    @JRubyMethod(name = {"call", "[]"}, keywords = true)
    public IRubyObject call(ThreadContext context, IRubyObject arg, Block block) {
        arg = dupIfKeywordRestAtCallsite(context, arg);

        return method.call(context, receiver, sourceModule, methodName, arg, block);
    }
    @JRubyMethod(name = {"call", "[]"}, keywords = true)
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        arg1 = dupIfKeywordRestAtCallsite(context, arg1);

        return method.call(context, receiver, sourceModule, methodName, arg0, arg1, block);
    }
    @JRubyMethod(name = {"call", "[]"}, keywords = true)
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        arg2 = dupIfKeywordRestAtCallsite(context, arg2);

        return method.call(context, receiver, sourceModule, methodName, arg0, arg1, arg2, block);
    }
    @JRubyMethod(name = {"call", "[]"}, rest = true, keywords = true)
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
        if (args.length > 0) {
            args[args.length - 1] = dupIfKeywordRestAtCallsite(context, args[args.length - 1]);
        }

        return method.call(context, receiver, sourceModule, methodName, args, block);
    }

    /** Returns the number of arguments a method accepted.
     * 
     * @return the number of arguments of a method.
     */
    @JRubyMethod
    public RubyFixnum arity(ThreadContext context) {
        return asFixnum(context, method.getSignature().arityValue());
    }

    @JRubyMethod(name = "eql?")
    public IRubyObject op_eql(ThreadContext context, IRubyObject other) {
        return op_equal(context, other);
    }

    @Override
    @JRubyMethod(name = "==")
    public RubyBoolean op_equal(ThreadContext context, IRubyObject other) {
        return asBoolean(context,  equals(other) );
    }

    @Override
    @JRubyMethod(name = "===")
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) {
        return method.call(context, receiver, sourceModule, methodName, other, Block.NULL_BLOCK);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RubyMethod)) {
            return false;
        }
        if (method instanceof ProcMethod) {
            return ((ProcMethod) method).isSame(((RubyMethod) other).getMethod());
        }
        if (getMetaClass() != ((RubyBasicObject) other).getMetaClass()) {
            return false;
        }

        RubyMethod otherMethod = (RubyMethod)other;
        return receiver == otherMethod.receiver &&
            ( isSerialMatch(otherMethod.method) || isMethodMissingMatch(otherMethod.getMethod().getRealMethod()) );
    }

    private boolean isMethodMissingMatch(DynamicMethod other) {
        return (method.getRealMethod() instanceof RubyModule.RespondToMissingMethod) &&
                ((RubyModule.RespondToMissingMethod) method.getRealMethod()).equals(other);
    }

    private boolean isSerialMatch(DynamicMethod otherMethod) {
        return method.getRealMethod().getSerialNumber() == otherMethod.getRealMethod().getSerialNumber();
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, hashCodeImpl());
    }

    @Override
    public int hashCode() {
        return (int) hashCodeImpl();
    }

    private long hashCodeImpl() {
        return receiver.hashCode() * method.getRealMethod().getSerialNumber();
    }

    @JRubyMethod(name = "clone")
    @Override
    public RubyMethod rbClone() {
        RubyMethod newMethod = newMethod(implementationModule, methodName, originModule, originName, entry, receiver);
        ThreadContext context = getRuntime().getCurrentContext();

        return (RubyMethod) cloneSetup(context, newMethod, context.nil);
    }

    @JRubyMethod
    public RubyMethod dup(ThreadContext context) {
        RubyMethod newMethod = newMethod(implementationModule, methodName, originModule, originName, entry, receiver);

        return (RubyMethod) dupSetup(context, newMethod);
    }

    /** Create a Proc object.
     * 
     */
    @JRubyMethod
    public IRubyObject to_proc(ThreadContext context) {
        Ruby runtime = context.runtime;

        MethodBlockBody body;
        Signature signature = method.getSignature();
        ArgumentDescriptor[] argsDesc;
        if (method instanceof IRMethodArgs) {
            argsDesc = ((IRMethodArgs) method).getArgumentDescriptors();
        } else {
            argsDesc = Helpers.methodToArgumentDescriptors(method);
        }

        int line = getLine(); // getLine adds 1 to 1-index but we need to reset to 0-index internally
        body = new MethodBlockBody(runtime.getStaticScopeFactory().getDummyScope(), signature, entry, argsDesc,
                receiver, originModule, originName, getFilename(), line == -1 ? -1 : line - 1);
        Block b = MethodBlockBody.createMethodBlock(body);

        RubyProc proc = RubyProc.newProc(runtime, b, Block.Type.LAMBDA);
        proc.setFromMethod();
        return proc;
    }

    @JRubyMethod
    public RubyUnboundMethod unbind() {
        RubyUnboundMethod unboundMethod =
        	RubyUnboundMethod.newUnboundMethod(implementationModule, methodName, originModule, originName, entry);

        return unboundMethod;
    }
    
    @JRubyMethod(name = {"inspect", "to_s"})
    public IRubyObject inspect(ThreadContext context) {
        return inspect(receiver);
    }

    @JRubyMethod
    public IRubyObject receiver(ThreadContext context) {
        return receiver;
    }

    @JRubyMethod
    public IRubyObject source_location(ThreadContext context) {
        String filename = getFilename();
        return filename == null ? context.nil :
                newArray(context, newString(context, filename), asFixnum(context, getLine()));
    }

    @JRubyMethod
    public IRubyObject parameters(ThreadContext context) {
        return Helpers.methodToParameters(context.runtime, this);
    }

    @JRubyMethod
    public IRubyObject curry(ThreadContext context) {
        IRubyObject proc = to_proc(context);
        return sites(context).curry.call(context, proc, proc);
    }

    @JRubyMethod
    public IRubyObject curry(ThreadContext context, IRubyObject arg0) {
        IRubyObject proc = to_proc(context);
        return sites(context).curry.call(context, proc, proc, arg0);
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
        return super_method(context, receiver, superClass);
    }

    @JRubyMethod
    public IRubyObject original_name(ThreadContext context) {
        return method instanceof AliasMethod ? asSymbol(context, ((AliasMethod)method).getOldName()) : name(context);
    }

    public IRubyObject getReceiver() {
        return receiver;
    }

    @Deprecated
    public IRubyObject curry(ThreadContext context, IRubyObject[] args) {
        IRubyObject proc = to_proc(context);
        return sites(context).curry.call(context, proc, proc, args);
    }

    private static JavaSites.MethodSites sites(ThreadContext context) {
        return context.sites.Method;
    }

}

