/*
 ***** BEGIN LICENSE BLOCK *****
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

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name={"TrueClass", "FalseClass"})
public class RubyBoolean extends RubyObject {

    private final int hashCode;

    RubyBoolean(Ruby runtime, boolean value) {
        super(runtime,
                (value ? runtime.getTrueClass() : runtime.getFalseClass()),
                false); // Don't put in object space

        if (!value) flags = FALSE_F;

        if (RubyInstanceConfig.CONSISTENT_HASHING_ENABLED) {
            // default to a fixed value
            this.hashCode = value ? 155 : -48;
        } else {
            // save the object id based hash code;
            this.hashCode = System.identityHashCode(this);
        }
    }
    
    @Override
    public int getNativeTypeIndex() {
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

    public static RubyClass createFalseClass(Ruby runtime) {
        RubyClass falseClass = runtime.defineClass("FalseClass", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setFalseClass(falseClass);
        falseClass.index = ClassIndex.FALSE;
        falseClass.setReifiedClass(RubyBoolean.class);
        
        falseClass.defineAnnotatedMethods(False.class);
        falseClass.defineAnnotatedMethods(RubyBoolean.class);
        
        falseClass.getMetaClass().undefineMethod("new");
        
        return falseClass;
    }
    
    public static RubyClass createTrueClass(Ruby runtime) {
        RubyClass trueClass = runtime.defineClass("TrueClass", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setTrueClass(trueClass);
        trueClass.index = ClassIndex.TRUE;
        trueClass.setReifiedClass(RubyBoolean.class);
        
        trueClass.defineAnnotatedMethods(True.class);
        trueClass.defineAnnotatedMethods(RubyBoolean.class);
        
        trueClass.getMetaClass().undefineMethod("new");
        
        return trueClass;
    }
    
    public static RubyBoolean newBoolean(Ruby runtime, boolean value) {
        return value ? runtime.getTrue() : runtime.getFalse();
    }
    
    public static class False extends RubyBoolean {
        False(Ruby runtime) {
            super(runtime,
                    false); // Don't put in object space

            flags = FALSE_F;
        }
        
        @JRubyMethod(name = "&")
        public static IRubyObject false_and(IRubyObject f, IRubyObject oth) {
            return f;
        }

        @JRubyMethod(name = "|")
        public static IRubyObject false_or(IRubyObject f, IRubyObject oth) {
            return oth.isTrue() ? f.getRuntime().getTrue() : f;
        }

        @JRubyMethod(name = "^")
        public static IRubyObject false_xor(IRubyObject f, IRubyObject oth) {
            return oth.isTrue() ? f.getRuntime().getTrue() : f;
        }

        @JRubyMethod(name = "to_s")
        public static IRubyObject false_to_s(IRubyObject f) {
            return RubyString.newUSASCIIString(f.getRuntime(), "false");
        }
    }
    
    public static class True extends RubyBoolean {
        True(Ruby runtime) {
            super(runtime,
                    true); // Don't put in object space
        }
        
        @JRubyMethod(name = "&")
        public static IRubyObject true_and(IRubyObject t, IRubyObject oth) {
            return oth.isTrue() ? t : t.getRuntime().getFalse();
        }

        @JRubyMethod(name = "|")
        public static IRubyObject true_or(IRubyObject t, IRubyObject oth) {
            return t;
        }

        @JRubyMethod(name = "^")
        public static IRubyObject true_xor(IRubyObject t, IRubyObject oth) {
            return oth.isTrue() ? t.getRuntime().getFalse() : t;
        }

        @JRubyMethod(name = "to_s")
        public static IRubyObject true_to_s(IRubyObject t) {
            return RubyString.newUSASCIIString(t.getRuntime(), "true");
        }
    }
    
    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        return context.runtime.newFixnum(hashCode());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public RubyFixnum id() {
        if ((flags & FALSE_F) == 0) {
            return RubyFixnum.newFixnum(getRuntime(), 2);
        } else {
            return RubyFixnum.zero(getRuntime());
        }
    }

    @Override
    public IRubyObject taint(ThreadContext context) {
        return this;
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write(isTrue() ? 'T' : 'F');
    }

    @Override
    public Object toJava(Class target) {
        if (target.isAssignableFrom(Boolean.class) || target.equals(boolean.class)) {
            if (isFalse()) return Boolean.FALSE;

            return Boolean.TRUE;
        } else {
            return super.toJava(target);
        }
    }
}

