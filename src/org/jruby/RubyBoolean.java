/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
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
    
    public RubyBoolean(Ruby runtime, boolean value) {
        super(runtime, (value ? runtime.getTrueClass() : runtime.getFalseClass()), // Don't initialize with class
                false, false); // Don't put in object space and don't taint

        if (!value) flags = FALSE_F;
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
        
        falseClass.defineAnnotatedMethods(False.class);
        
        falseClass.getMetaClass().undefineMethod("new");
        
        return falseClass;
    }
    
    public static RubyClass createTrueClass(Ruby runtime) {
        RubyClass trueClass = runtime.defineClass("TrueClass", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setTrueClass(trueClass);
        trueClass.index = ClassIndex.TRUE;
        
        trueClass.defineAnnotatedMethods(True.class);
        
        trueClass.getMetaClass().undefineMethod("new");
        
        return trueClass;
    }
    
    public static RubyBoolean newBoolean(Ruby runtime, boolean value) {
        return value ? runtime.getTrue() : runtime.getFalse();
    }
    
    public static class False {
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
            return f.getRuntime().newString("false");
        }
    }
    
    public static class True {
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
            return t.getRuntime().newString("true");
        }
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

    public Object toJava(Class target) {
        if (isFalse()) return Boolean.FALSE;

        return Boolean.TRUE;
    }
}

