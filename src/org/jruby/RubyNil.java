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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="NilClass")
public class RubyNil extends RubyObject {

    private final int hashCode;

    public RubyNil(Ruby runtime) {
        super(runtime, runtime.getNilClass(), false);
        flags |= NIL_F | FALSE_F;

        if (RubyInstanceConfig.CONSISTENT_HASHING_ENABLED) {
            // default to a fixed value
            this.hashCode = 34;
        } else {
            // save the object id based hash code;
            this.hashCode = System.identityHashCode(this);
        }
    }
    
    public static final ObjectAllocator NIL_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return runtime.getNil();
        }
    };
    
    public static RubyClass createNilClass(Ruby runtime) {
        RubyClass nilClass = runtime.defineClass("NilClass", runtime.getObject(), NIL_ALLOCATOR);
        runtime.setNilClass(nilClass);
        nilClass.index = ClassIndex.NIL;
        nilClass.setReifiedClass(RubyNil.class);
        
        nilClass.defineAnnotatedMethods(RubyNil.class);
        
        nilClass.getMetaClass().undefineMethod("new");
        
        // FIXME: This is causing a verification error for some reason
        //nilClass.dispatcher = callbackFactory.createDispatcher(nilClass);
        
        return nilClass;
    }
    
    @Override
    public int getNativeTypeIndex() {
        return ClassIndex.NIL;
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
        return void.class;
    }
    
    // Methods of the Nil Class (nil_*):
    
    /** nil_to_i
     *
     */
    @JRubyMethod(name = "to_i")
    public static RubyFixnum to_i(ThreadContext context, IRubyObject recv) {
        return RubyFixnum.zero(recv.getRuntime());
    }
    
    /**
     * nil_to_f
     *
     */
    @JRubyMethod(name = "to_f")
    public static RubyFloat to_f(ThreadContext context, IRubyObject recv) {
        return RubyFloat.newFloat(context.runtime, 0.0D);
    }
    
    /** nil_to_s
     *
     */
    @JRubyMethod(name = "to_s")
    public static RubyString to_s(ThreadContext context, IRubyObject recv) {
        return RubyString.newEmptyString(context.runtime);
    }
    
    /** nil_to_a
     *
     */
    @JRubyMethod(name = "to_a")
    public static RubyArray to_a(ThreadContext context, IRubyObject recv) {
        return context.runtime.newEmptyArray();
    }
    
    @JRubyMethod(name = "to_h")
    public static RubyHash to_h(ThreadContext context, IRubyObject recv) {
        return RubyHash.newSmallHash(context.runtime);
    }
    
    /** nil_inspect
     *
     */
    @JRubyMethod(name = "inspect")
    public static RubyString inspect(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        if (runtime.is1_9()) {
            return RubyString.newUSASCIIString(runtime, "nil");
        } else {
            return RubyString.newString(runtime, "nil");
        }
    }
    
    /** nil_type
     *
     */
    @JRubyMethod(name = "type", compat = CompatVersion.RUBY1_8)
    public static RubyClass type(IRubyObject recv) {
        return recv.getRuntime().getNilClass();
    }
    
    /** nil_and
     *
     */
    @JRubyMethod(name = "&", required = 1)
    public static RubyBoolean op_and(IRubyObject recv, IRubyObject obj) {
        return recv.getRuntime().getFalse();
    }
    
    /** nil_or
     *
     */
    @JRubyMethod(name = "|", required = 1)
    public static RubyBoolean op_or(IRubyObject recv, IRubyObject obj) {
        return recv.getRuntime().newBoolean(obj.isTrue());
    }
    
    /** nil_xor
     *
     */
    @JRubyMethod(name = "^", required = 1)
    public static RubyBoolean op_xor(IRubyObject recv, IRubyObject obj) {
        return recv.getRuntime().newBoolean(obj.isTrue());
    }

    @JRubyMethod(name = "nil?")
    public IRubyObject nil_p() {
        return getRuntime().getTrue();
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
        return getRuntime().newFixnum(4);
    }
    
    @Override
    public IRubyObject taint(ThreadContext context) {
        return this;
    }

    /** nilclass_to_c
     * 
     */
    @JRubyMethod(name = "to_c", compat = CompatVersion.RUBY1_9)
    public static IRubyObject to_c(ThreadContext context, IRubyObject recv) {
        return RubyComplex.newComplexCanonicalize(context, RubyFixnum.zero(context.runtime));
    }
    
    /** nilclass_to_r
     * 
     */
    @JRubyMethod(name = "to_r", compat = CompatVersion.RUBY1_9)
    public static IRubyObject to_r(ThreadContext context, IRubyObject recv) {
        return RubyRational.newRationalCanonicalize(context, RubyFixnum.zero(context.runtime));
    }

    /** nilclass_rationalize
     *
     */
    @JRubyMethod(name = "rationalize", optional = 1, compat = CompatVersion.RUBY1_9)
    public static IRubyObject rationalize(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return to_r(context, recv);
    }

    @Override
    public Object toJava(Class target) {
        if (target.isPrimitive()) {
            if (target == Boolean.TYPE) {
                return Boolean.FALSE;
            } else if (target == Byte.TYPE) {
                return (byte)0;
            } else if (target == Short.TYPE) {
                return (short)0;
            } else if (target == Character.TYPE) {
                return (char)0;
            } else if (target == Integer.TYPE) {
                return 0;
            } else if (target == Long.TYPE) {
                return (long)0;
            } else if (target == Float.TYPE) {
                return (float)0;
            } else if (target == Double.TYPE) {
                return (double)0;
            }
        }
        return null;
    }
}
