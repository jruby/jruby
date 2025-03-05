/*
 **** BEGIN LICENSE BLOCK *****
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

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.DelegatingDynamicMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.PartialDelegatingMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.marshal.DataType;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;

/**
 * @see RubyMethod
 * @see RubyUnboundMethod
 */
@JRubyClass(name = {"Method", "UnboundMethod"}, overrides = {RubyMethod.class, RubyUnboundMethod.class})
public abstract class AbstractRubyMethod extends RubyObject implements DataType {
    protected RubyModule implementationModule;
    protected String methodName;
    protected RubyModule originModule;
    protected String originName;
    protected CacheEntry entry;
    protected DynamicMethod method;
    protected RubyModule sourceModule;

    protected AbstractRubyMethod(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public DynamicMethod getMethod() {
        return method;
    }

    /** Returns the number of arguments a method accepted.
     * 
     * @return the number of arguments of a method.
     */
    @JRubyMethod(name = "arity")
    public RubyFixnum arity(ThreadContext context) {
        return asFixnum(context, method.getSignature().arityValue());
    }

    @Deprecated
    public RubyFixnum arity() {
        return arity(getCurrentContext());
    }

    @Deprecated(since = "9.4-") // since 2017
    public final IRubyObject op_eql19(ThreadContext context, IRubyObject other) {
        return op_eql(context, other);
    }

    @JRubyMethod(name = "eql?")
    public IRubyObject op_eql(ThreadContext context, IRubyObject other) {
        return asBoolean(context,  equals(other) );
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    public abstract AbstractRubyMethod rbClone();

    @JRubyMethod(name = "name")
    public IRubyObject name(ThreadContext context) {
        return asSymbol(context, methodName);
    }

    public String getMethodName() {
        return methodName;
    }

    @JRubyMethod(name = "owner")
    public IRubyObject owner(ThreadContext context) {
        // If original method has changed visibility in a higher module/class then we return that location
        // and not where it was originally defined.
        if (method instanceof PartialDelegatingMethod) {
            return method.getImplementationClass();
        } else {
            return implementationModule.getOrigin();
        }
    }

    @JRubyMethod(name = "source_location")
    public IRubyObject source_location(ThreadContext context) {
        String filename = getFilename();

        return filename != null ?
                newArray(context, newString(context, filename), asFixnum(context, getLine())) :
                context.nil;
    }

    @Deprecated(since = "10.0")
    public RubyBoolean public_p(ThreadContext context) {
        return context.runtime.newBoolean(method.getVisibility().isPublic());
    }

    @Deprecated(since = "10.0")
    public RubyBoolean protected_p(ThreadContext context) {
        return context.runtime.newBoolean(method.getVisibility().isProtected());
    }

    @Deprecated(since = "10.0")
    public RubyBoolean private_p(ThreadContext context) {
        return context.runtime.newBoolean(method.getVisibility().isPrivate());
    }

    public String getFilename() {
        DynamicMethod realMethod = method.getRealMethod(); // Follow Aliases
        if (realMethod instanceof PositionAware) {
            PositionAware poser = (PositionAware) realMethod;
            return TraceType.maskInternalFiles(poser.getFile());
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

    @JRubyMethod(name = "parameters")
    public IRubyObject parameters(ThreadContext context) {
        return Helpers.methodToParameters(context, this);
    }

    protected IRubyObject super_method(ThreadContext context, IRubyObject receiver, RubyModule superClass) {
        if (superClass == null) return context.nil;

        String searchName = method.getRealMethod().getName();
        CacheEntry entry = superClass.searchWithCache(searchName);
        if (entry.method == UndefinedMethod.INSTANCE ||
                entry.method.getDefinedClass().getMethods().get(entry.method.getName()) == UndefinedMethod.INSTANCE) {
            return context.nil;
        }

        if (receiver == null) {
            return RubyUnboundMethod.newUnboundMethod(entry.sourceModule, methodName, superClass, originName, entry);
        } else {
            return RubyMethod.newMethod(entry.sourceModule, methodName, superClass, originName, entry, receiver);
        }
    }

    @JRubyMethod
    public IRubyObject original_name(ThreadContext context) {
        return asSymbol(context, method instanceof AliasMethod alias ? alias.getOldName() : method.getName());
    }

    public IRubyObject inspect(IRubyObject receiver) {
        ThreadContext context = getRuntime().getCurrentContext();

        RubyString str = newString(context, "#<");
        String sharp = "#";

        str.catString(getType().getName(context)).catString(": ");

        RubyModule definedClass;
        RubyModule mklass = originModule;

        if (method instanceof AliasMethod || method instanceof DelegatingDynamicMethod) {
            definedClass = method.getRealMethod().getDefinedClass();
        } else {
            definedClass = method.getDefinedClass();
        }

        if (definedClass.isIncluded()) {
            definedClass = definedClass.getMetaClass();
        }

        if (receiver == null) {
            str.catWithCodeRange(inspect(context, definedClass).convertToString());
        } else if (mklass.isSingleton()) {
            IRubyObject attached = ((MetaClass) mklass).getAttached();
            if (receiver == null) {
                str.catWithCodeRange(inspect(context, mklass).convertToString());
            } else if (receiver == attached) {
                str.catWithCodeRange(inspect(context, attached).convertToString());
                sharp = ".";
            } else {
                str.catWithCodeRange(inspect(context, receiver).convertToString());
                str.catString("(");
                str.catWithCodeRange(inspect(context, attached).convertToString());
                str.catString(")");
                sharp = ".";
            }
        } else {
            if (receiver instanceof RubyClass) {
                str.catString("#<");
                str.cat(mklass.rubyName(context));
                str.catString(":");
                str.cat(((RubyClass) receiver).rubyName(context));
                str.catString(">");
            } else {
                str.cat(mklass.rubyName(context));
            }
            if (definedClass != mklass) {
                str.catString("(");
                str.cat(definedClass.rubyName(context));
                str.catString(")");
            }
        }
        str.catString(sharp);
        str.cat(asSymbol(context, methodName).asString());
        if (!methodName.equals(method.getName())) {
            str.catString("(");
            str.cat(asSymbol(context, method.getRealMethod().getName()).asString());
            str.catString(")");
        }
        if (method.isNotImplemented()) {
            str.catString(" (not-implemented)");
        }

        str.catString("(");
        ArgumentDescriptor[] descriptors = Helpers.methodToArgumentDescriptors(context, method);
        if (descriptors.length > 0) {
            RubyString desc = descriptors[0].asParameterName(context);

            str.cat(desc);
            for (int i = 1; i < descriptors.length; i++) {
                desc = descriptors[i].asParameterName(context);

                str.catString(", ");
                str.cat(desc);
            }
        }
        str.catString(")");
        String fileName = getFilename();
        if (fileName != null) { // Only Ruby Methods will have this info.
            str.catString(" ");
            str.catString(fileName).cat(':').catString("" + getLine());
        }
        str.catString(">");

        return str;
    }
}

