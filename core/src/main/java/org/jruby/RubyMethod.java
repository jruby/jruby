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
import org.jruby.internal.runtime.methods.DelegatingDynamicMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.IRMethodArgs;
import org.jruby.internal.runtime.methods.PartialDelegatingMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.MethodBlockBody;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

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

    /** Create the RubyMethod class and add it to the Ruby runtime.
     * 
     */
    public static RubyClass createMethodClass(Ruby runtime) {
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here. Confirm. JRUBY-415
        RubyClass methodClass = runtime.defineClass("Method", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        methodClass.setClassIndex(ClassIndex.METHOD);
        methodClass.setReifiedClass(RubyMethod.class);

        methodClass.defineAnnotatedMethods(AbstractRubyMethod.class);
        methodClass.defineAnnotatedMethods(RubyMethod.class);
        
        return methodClass;
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
    @JRubyMethod(name = {"call", "[]"})
    public IRubyObject call(ThreadContext context, Block block) {
        return method.call(context, receiver, sourceModule, methodName, block);
    }
    @JRubyMethod(name = {"call", "[]"})
    public IRubyObject call(ThreadContext context, IRubyObject arg, Block block) {
        return method.call(context, receiver, sourceModule, methodName, arg, block);
    }
    @JRubyMethod(name = {"call", "[]"})
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return method.call(context, receiver, sourceModule, methodName, arg0, arg1, block);
    }
    @JRubyMethod(name = {"call", "[]"})
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return method.call(context, receiver, sourceModule, methodName, arg0, arg1, arg2, block);
    }
    @JRubyMethod(name = {"call", "[]"}, rest = true)
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
        return method.call(context, receiver, sourceModule, methodName, args, block);
    }

    /** Returns the number of arguments a method accepted.
     * 
     * @return the number of arguments of a method.
     */
    @JRubyMethod
    public RubyFixnum arity() {
        return getRuntime().newFixnum(method.getSignature().arityValue());
    }

    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject op_eql(ThreadContext context, IRubyObject other) {
        return op_equal(context, other);
    }

    @Override
    @JRubyMethod(name = "==", required = 1)
    public RubyBoolean op_equal(ThreadContext context, IRubyObject other) {
        return RubyBoolean.newBoolean(context,  equals(other) );
    }

    @Override
    @JRubyMethod(name = "===", required = 1)
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
        return receiver == otherMethod.receiver && originModule == otherMethod.originModule &&
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
        return context.runtime.newFixnum(hashCodeImpl());
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
        newMethod.setMetaClass(getMetaClass());
        return newMethod;
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
        Block b = MethodBlockBody.createMethodBlock(context, body);
        
        return RubyProc.newProc(runtime, b, Block.Type.LAMBDA);
    }

    @JRubyMethod
    public RubyUnboundMethod unbind() {
        RubyUnboundMethod unboundMethod =
        	RubyUnboundMethod.newUnboundMethod(implementationModule, methodName, originModule, originName, entry);
        unboundMethod.infectBy(this);
        
        return unboundMethod;
    }
    
    @JRubyMethod(name = {"inspect", "to_s"})
    @Override
    public IRubyObject inspect() {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        RubyString str = RubyString.newString(runtime, "#<");
        String sharp = "#";
        
        str.catString(getType().getName()).catString(": ");

        RubyModule definedClass;
        RubyModule mklass = originModule;

        if (method instanceof AliasMethod || method instanceof DelegatingDynamicMethod) {
            definedClass = method.getRealMethod().getDefinedClass();
        }
        else {
            definedClass = method.getDefinedClass();
        }

        if (definedClass.isIncluded()) {
            definedClass = definedClass.getMetaClass();
        }

        if (mklass.isSingleton()) {
            IRubyObject attached = ((MetaClass) mklass).getAttached();
            if (receiver == null) {
                str.cat19(inspect(context, mklass).convertToString());
            } else if (receiver == attached) {
                str.cat19(inspect(context, attached).convertToString());
                sharp = ".";
            } else {
                str.cat19(inspect(context, receiver).convertToString());
                str.catString("(");
                str.cat19(inspect(context, attached).convertToString());
                str.catString(")");
                sharp = ".";
            }
        } else {
            str.catString(mklass.getName());
            if (definedClass != mklass) {
                str.catString("(");
                str.catString(definedClass.getName());
                str.catString(")");
            }
        }
        str.catString(sharp);
        str.catString(this.methodName);
        if (!methodName.equals(method.getName())) {
            str.catString("(");
            str.catString(method.getName());
            str.catString(")");
        }
        if (method.isNotImplemented()) {
            str.catString(" (not-implemented)");
        }
        str.catString(">");

        return str;
    }

    @JRubyMethod
    public IRubyObject receiver(ThreadContext context) {
        return receiver;
    }

    @JRubyMethod
    public IRubyObject source_location(ThreadContext context) {
        Ruby runtime = context.runtime;

        String filename = getFilename();
        if (filename != null) {
            return runtime.newArray(runtime.newString(filename), runtime.newFixnum(getLine()));
        }

        return context.nil;
    }

    public String getFilename() {
        DynamicMethod realMethod = method.getRealMethod(); // Follow Aliases
        if (realMethod instanceof PositionAware) {
            PositionAware poser = (PositionAware) realMethod;
            return poser.getFile();
        }
        return null;
    }

    public int getLine() {
        DynamicMethod realMethod = method.getRealMethod(); // Follow Aliases
        if (realMethod instanceof PositionAware) {
            PositionAware poser = (PositionAware) realMethod;
            return poser.getLine() + 1;
        }
        return -1;
    }

    @JRubyMethod
    public IRubyObject parameters(ThreadContext context) {
        return Helpers.methodToParameters(context.runtime, this);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject curry(ThreadContext context, IRubyObject[] args) {
        return to_proc(context).callMethod(context, "curry", args);
    }

    @JRubyMethod
    public IRubyObject super_method(ThreadContext context) {
        RubyModule superClass = null;
        if (method instanceof PartialDelegatingMethod || method instanceof AliasMethod) {
            RubyModule definedClass = method.getRealMethod().getDefinedClass();
            RubyModule module = sourceModule.findImplementer(definedClass);

            if (module != null) {
                superClass = module.getSuperClass();
            }
        } else {
            superClass = sourceModule.getSuperClass();
        }
        return super_method(context, receiver, superClass);
    }

    @JRubyMethod
    public IRubyObject original_name(ThreadContext context) {
        if (method instanceof AliasMethod) {
            return context.runtime.newSymbol(((AliasMethod)method).getOldName());
        }
        return name(context);
    }

}

