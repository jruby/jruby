/***** BEGIN LICENSE BLOCK *****
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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;

/**
 *
 * @author  jpetersen
 */
public class RubyBoolean extends RubyObject {
    private final Ruby runtime;
    
    public RubyBoolean(Ruby runtime, boolean value) {
        super(runtime,
                (value ? runtime.getClass("TrueClass") : runtime.getClass("FalseClass")), // Don't initialize with class
                false); // Don't put in object space
        this.isTrue = value;
        this.runtime = runtime;
    }
    
    public int getNativeTypeIndex() {
        return isTrue ? ClassIndex.TRUE : ClassIndex.FALSE;
    }
    
    public Ruby getRuntime() {
        return runtime;
    }
    
    public boolean isImmediate() {
        return true;
    }
    
    public Class getJavaClass() {
        return Boolean.TYPE;
    }
    
//    public RubyClass getMetaClass() {
//        return isTrue
//                ? getRuntime().getClass("TrueClass")
//                : getRuntime().getClass("FalseClass");
//    }
    
    public RubyFixnum id() {
        return getRuntime().newFixnum(isTrue ? 2 : 0);
    }
    
    public static RubyClass createFalseClass(Ruby runtime) {
        RubyClass falseClass = runtime.defineClass("FalseClass", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        falseClass.index = ClassIndex.FALSE;
        
        CallbackFactory fact = runtime.callbackFactory(RubyBoolean.class);
        
        falseClass.defineFastMethod("type", fact.getFastMethod("type"));
        falseClass.defineFastMethod("&", fact.getFastMethod("false_and", RubyKernel.IRUBY_OBJECT));
        falseClass.defineFastMethod("|", fact.getFastMethod("false_or", RubyKernel.IRUBY_OBJECT));
        falseClass.defineFastMethod("^", fact.getFastMethod("false_xor", RubyKernel.IRUBY_OBJECT));
        falseClass.defineFastMethod("id", fact.getFastMethod("false_id"));
        falseClass.defineFastMethod("to_s", fact.getFastMethod("false_to_s"));
        falseClass.getMetaClass().undefineMethod("new");
        
        return falseClass;
    }
    
    public static RubyClass createTrueClass(Ruby runtime) {
        RubyClass trueClass = runtime.defineClass("TrueClass", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        trueClass.index = ClassIndex.TRUE;
        
        CallbackFactory fact = runtime.callbackFactory(RubyBoolean.class);
        
        trueClass.defineFastMethod("type", fact.getFastMethod("type"));
        trueClass.defineFastMethod("&", fact.getFastMethod("true_and", RubyKernel.IRUBY_OBJECT));
        trueClass.defineFastMethod("|", fact.getFastMethod("true_or", RubyKernel.IRUBY_OBJECT));
        trueClass.defineFastMethod("^", fact.getFastMethod("true_xor", RubyKernel.IRUBY_OBJECT));
        trueClass.defineFastMethod("id", fact.getFastMethod("true_id"));
        trueClass.defineFastMethod("to_s", fact.getFastMethod("true_to_s"));
        trueClass.getMetaClass().undefineMethod("new");
        
        return trueClass;
    }
    
    public static RubyBoolean newBoolean(Ruby runtime, boolean value) {
        return value ? runtime.getTrue() : runtime.getFalse();
    }
    
    /** false_type
     *  true_type
     *
     */
    public RubyClass type() {
        return getMetaClass();
    }
    
    public IRubyObject false_and(IRubyObject oth) {
        return this;
    }
    
    public IRubyObject false_or(IRubyObject oth) {
        return oth.isTrue() ? getRuntime().getTrue() : this;
    }
    
    public IRubyObject false_xor(IRubyObject oth) {
        return oth.isTrue() ? getRuntime().getTrue() : this;
    }
    
    public IRubyObject false_id() {
        return RubyFixnum.zero(getRuntime());
    }
    
    public IRubyObject false_to_s() {
        return getRuntime().newString("false");
    }
    
    public IRubyObject true_and(IRubyObject oth) {
        return oth.isTrue() ? this : getRuntime().getFalse();
    }
    
    public IRubyObject true_or(IRubyObject oth) {
        return this;
    }
    
    public IRubyObject true_xor(IRubyObject oth) {
        return oth.isTrue() ? getRuntime().getFalse() : this;
    }
    
    public IRubyObject true_id() {
        return getRuntime().newFixnum(2);
    }
    
    public IRubyObject true_to_s() {
        return getRuntime().newString("true");
    }
    
    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write(isTrue() ? 'T' : 'F');
    }
}

