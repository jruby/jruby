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
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.marshal.DataType;

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
    public RubyFixnum arity() {
        return getRuntime().newFixnum(method.getSignature().arityValue());
    }

    @Deprecated
    public final IRubyObject op_eql19(ThreadContext context, IRubyObject other) {
        return op_eql(context, other);
    }

    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject op_eql(ThreadContext context, IRubyObject other) {
        return RubyBoolean.newBoolean(context,  equals(other) );
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    public abstract AbstractRubyMethod rbClone();

    @JRubyMethod(name = "name")
    public IRubyObject name(ThreadContext context) {
        return context.runtime.newSymbol(methodName);
    }

    public String getMethodName() {
        return methodName;
    }

    @JRubyMethod(name = "owner")
    public IRubyObject owner(ThreadContext context) {
        return implementationModule.getOrigin();
    }

    @JRubyMethod(name = "source_location")
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

    @JRubyMethod(name = "parameters")
    public IRubyObject parameters(ThreadContext context) {
        return Helpers.methodToParameters(context.runtime, this);
    }

    protected IRubyObject super_method(ThreadContext context, IRubyObject receiver, RubyModule superClass) {
        if (superClass == null) return context.nil;

        CacheEntry entry = superClass.searchWithCache(methodName);
        if (entry.method == UndefinedMethod.INSTANCE ||
                entry.method.getDefinedClass().getMethods().get(entry.method.getName()) == UndefinedMethod.INSTANCE) {
            return context.nil;
        }

        if (receiver == null) {
            return RubyUnboundMethod.newUnboundMethod(superClass, methodName, superClass, originName, entry);
        } else {
            return RubyMethod.newMethod(superClass, methodName, superClass, originName, entry, receiver);
        }
    }

    @JRubyMethod
    public IRubyObject original_name(ThreadContext context) {
        if (method instanceof AliasMethod) {
            return context.runtime.newSymbol(((AliasMethod) method).getOldName());
        }

        return context.runtime.newSymbol(method.getName());
    }
}

