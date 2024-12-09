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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.compiler.Constantizable;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.util.ByteList;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;
import static org.jruby.util.StringSupport.CR_7BIT;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name={"TrueClass", "FalseClass"})
public class RubyBoolean extends RubyObject implements Constantizable, Appendable {

    private final int hashCode;
    private final transient Object constant;
    private final ByteList toS;

    RubyBoolean(Ruby runtime, RubyClass metaClass, boolean value) {
        super(runtime, metaClass, false);

        if (!value) flags = FALSE_F;

        if (RubyInstanceConfig.CONSISTENT_HASHING_ENABLED) {
            // default to a fixed value
            this.hashCode = value ? 155 : -48;
        } else {
            // save the object id based hash code;
            this.hashCode = System.identityHashCode(this);
        }

        constant = OptoFactory.newConstantWrapper(IRubyObject.class, this);
        toS = value ? TRUE_BYTES : FALSE_BYTES;
    }
    
    @Override
    public ClassIndex getNativeClassIndex() {
        return (flags & FALSE_F) == 0 ? ClassIndex.TRUE : ClassIndex.FALSE;
    }
    
    @Override
    public boolean isImmediate() {
        return true;
    }

    @Override
    public RubyClass getSingletonClass() {
        return metaClass;
    }

    @Override
    public Class<?> getJavaClass() {
        return boolean.class;
    }

    /**
     * @see org.jruby.compiler.Constantizable
     */
    @Override
    public Object constant() {
        return constant;
    }

    public static void createFalseClass(ThreadContext context, RubyClass False) {
        False.reifiedClass(RubyBoolean.class).
                classIndex(ClassIndex.FALSE).
                defineMethods(context, False.class).
                defineMethods(context, RubyBoolean.class).
                tap(c -> c.getMetaClass().undefMethods(context, "new"));
    }
    
    public static void createTrueClass(ThreadContext context, RubyClass True) {
        True.reifiedClass(RubyBoolean.class).
                classIndex(ClassIndex.TRUE).
                defineMethods(context, True.class).
                defineMethods(context, RubyBoolean.class).
                tap(c -> c.getMetaClass().undefMethods(context, "new"));
    }

    @Deprecated
    public static RubyBoolean newBoolean(Ruby runtime, boolean value) {
        return value ? runtime.getTrue() : runtime.getFalse();
    }

    @Deprecated
    public static RubyBoolean newBoolean(ThreadContext context, boolean value) {
        return value ? context.tru : context.fals;
    }

    public static final ByteList FALSE_BYTES = new ByteList(new byte[] { 'f','a','l','s','e' }, USASCIIEncoding.INSTANCE);

    @Override
    public void appendIntoString(RubyString target) {
        target.catWithCodeRange(toS, CR_7BIT);
    }

    public static class False extends RubyBoolean {
        False(Ruby runtime, RubyClass metaClass) {
            super(runtime, metaClass, false);

            flags = FALSE_F | FROZEN_F;
        }
        
        @JRubyMethod(name = "&")
        public static IRubyObject false_and(IRubyObject fals, IRubyObject oth) {
            return fals;
        }

        @JRubyMethod(name = "|")
        public static IRubyObject false_or(ThreadContext context, IRubyObject fals, IRubyObject oth) {
            return oth.isTrue() ? context.tru : fals;
        }

        @JRubyMethod(name = "^")
        public static IRubyObject false_xor(ThreadContext context, IRubyObject fals, IRubyObject oth) {
            return oth.isTrue() ? context.tru : fals;
        }

        @JRubyMethod(name = "to_s", alias = "inspect")
        public static RubyString false_to_s(ThreadContext context, IRubyObject fals) {
            return context.runtime.getFalseString();
        }

        @Override
        public RubyString inspect() {
            return getRuntime().getFalseString();
        }

        @Override
        public <T> T toJava(Class<T> target) {
            if (target.isAssignableFrom(Boolean.class) || target == boolean.class) {
                return (T) Boolean.FALSE;
            }
            return super.toJava(target);
        }
    }

    public static final ByteList TRUE_BYTES = new ByteList(new byte[] { 't','r','u','e' }, USASCIIEncoding.INSTANCE);

    public static class True extends RubyBoolean {
        True(Ruby runtime, RubyClass metaClass) {
            super(runtime, metaClass, true);

            flags |= FROZEN_F;
        }
        
        @JRubyMethod(name = "&")
        public static IRubyObject true_and(ThreadContext context, IRubyObject tru, IRubyObject oth) {
            return oth.isTrue() ? tru : context.fals;
        }

        @JRubyMethod(name = "|")
        public static IRubyObject true_or(IRubyObject tru, IRubyObject oth) {
            return tru;
        }

        @JRubyMethod(name = "^")
        public static IRubyObject true_xor(ThreadContext context, IRubyObject tru, IRubyObject oth) {
            return oth.isTrue() ? context.fals : tru;
        }

        @JRubyMethod(name = "to_s", alias = "inspect")
        public static RubyString true_to_s(ThreadContext context, IRubyObject tru) {
            return context.runtime.getTrueString();
        }

        @Override
        public RubyString inspect() {
            return getRuntime().getTrueString();
        }

        @Override
        public <T> T toJava(Class<T> target) {
            if (target.isAssignableFrom(Boolean.class) || target == boolean.class) {
                return (T) Boolean.TRUE;
            }
            return super.toJava(target);
        }
    }
    
    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, hashCode());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public RubyFixnum id() {
        if ((flags & FALSE_F) == 0) {
            return RubyFixnum.newFixnum(metaClass.runtime, 20);
        } else {
            return RubyFixnum.zero(metaClass.runtime);
        }
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write(isTrue() ? 'T' : 'F');
    }

    @Deprecated
    @Override
    public IRubyObject taint(ThreadContext context) {
        return this;
    }
}

