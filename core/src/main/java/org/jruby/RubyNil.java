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

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.compiler.Constantizable;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.util.ByteList;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newEmptyArray;
import static org.jruby.api.Create.newSmallHash;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="NilClass")
public class RubyNil extends RubyObject implements Constantizable {

    private final int hashCode;
    private final transient Object constant;

    public RubyNil(Ruby runtime) {
        super(runtime, runtime.getNilClass(), false);
        flags |= NIL_F | FALSE_F | FROZEN_F;

        if (RubyInstanceConfig.CONSISTENT_HASHING_ENABLED) {
            // default to a fixed value
            this.hashCode = 34;
        } else {
            // save the object id based hash code;
            this.hashCode = System.identityHashCode(this);
        }

        constant = OptoFactory.newConstantWrapper(IRubyObject.class, this);
    }
    
    public static RubyClass createNilClass(Ruby runtime, RubyClass Object) {
        RubyClass NilClass = runtime.defineClass("NilClass", Object, NOT_ALLOCATABLE_ALLOCATOR).
                reifiedClass(RubyNil.class).
                classIndex(ClassIndex.NIL);

        NilClass.defineAnnotatedMethodsIndividually(RubyNil.class);
        NilClass.getMetaClass().undefineMethod("new");

        return NilClass;
    }
    
    @Override
    public ClassIndex getNativeClassIndex() {
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

    /**
     * @see org.jruby.compiler.Constantizable
     */
    @Override
    public Object constant() {
        return constant;
    }

    // Methods of the Nil Class (nil_*):
    
    /** nil_to_i
     *
     */
    @JRubyMethod
    public static RubyFixnum to_i(ThreadContext context, IRubyObject recv) {
        return RubyFixnum.zero(context.runtime);
    }
    
    /**
     * nil_to_f
     *
     */
    @JRubyMethod
    public static RubyFloat to_f(ThreadContext context, IRubyObject recv) {
        return RubyFloat.newFloat(context.runtime, 0.0D);
    }
    
    /** nil_to_s
     *
     */
    @JRubyMethod
    public static RubyString to_s(ThreadContext context, IRubyObject recv) {
        return context.runtime.getNilString();
    }
    
    /** nil_to_a
     *
     */
    @JRubyMethod
    public static RubyArray to_a(ThreadContext context, IRubyObject recv) {
        return newEmptyArray(context);
    }
    
    @JRubyMethod
    public static RubyHash to_h(ThreadContext context, IRubyObject recv) {
        return newSmallHash(context);
    }

    /** nil_inspect
     *
     */
    @Override
    public IRubyObject inspect(ThreadContext context) {
        return RubyNil.inspect(metaClass.runtime);
    }

    static final byte[] nilBytes = new byte[] { 'n','i','l' }; // RubyString.newUSASCIIString(runtime, "nil")
    private static final ByteList nil = new ByteList(nilBytes, USASCIIEncoding.INSTANCE);

    static RubyString inspect(Ruby runtime) {
        return RubyString.newStringShared(runtime, runtime.getString(), nil);
    }

    @Override
    @JRubyMethod(name = "=~")
    public IRubyObject op_match(ThreadContext context, IRubyObject arg) {
        return this; // nil
    }

    /** nil_and
     *
     */
    @JRubyMethod(name = "&")
    public static RubyBoolean op_and(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        return context.fals;
    }
    
    /** nil_or
     *
     */
    @JRubyMethod(name = "|")
    public static RubyBoolean op_or(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        return asBoolean(context, obj.isTrue());
    }
    
    /** nil_xor
     *
     */
    @JRubyMethod(name = "^")
    public static RubyBoolean op_xor(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        return asBoolean(context, obj.isTrue());
    }

    @Override
    @JRubyMethod(name = "nil?")
    public RubyBoolean nil_p(ThreadContext context) {
        return context.tru;
    }

    @Deprecated
    public IRubyObject nil_p() {
        return getRuntime().getTrue();
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, hashCode());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public RubyFixnum id() {
        return RubyFixnum.newFixnum(metaClass.runtime, 8);
    }

    /** nilclass_to_c
     * 
     */
    @JRubyMethod
    public static IRubyObject to_c(ThreadContext context, IRubyObject recv) {
        return RubyComplex.newComplexCanonicalize(context, RubyFixnum.zero(context.runtime));
    }
    
    /** nilclass_to_r
     * 
     */
    @JRubyMethod
    public static IRubyObject to_r(ThreadContext context, IRubyObject recv) {
        return RubyRational.newRationalCanonicalize(context, 0);
    }

    /** nilclass_rationalize
     *
     */
    @JRubyMethod(optional = 1, checkArity = false)
    public static IRubyObject rationalize(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return to_r(context, recv);
    }

    @Override
    public <T> T toJava(Class<T> target) {
        if (target.isPrimitive()) {
            if (target == boolean.class) {
                return (T) Boolean.FALSE;
            } else if (target == char.class) {
                return (T) (Character) '\0';
            } else {
                switch (target.getSimpleName().charAt(0)) {
                    case 'b': return (T) (Byte) (byte) 0;
                    case 's': return (T) (Short) (short) 0;
                    case 'i': return (T) (Integer) 0;
                    case 'l': return (T) (Long) 0L;
                    case 'f': return (T) (Float) 0.0F;
                    case 'd': return (T) (Double) 0.0;
                }
            }
        }
        return null;
    }

    @Deprecated
    @Override
    public IRubyObject taint(ThreadContext context) {
        return this;
    }

    @Deprecated
    public static final ObjectAllocator NIL_ALLOCATOR = NOT_ALLOCATABLE_ALLOCATOR;
}
