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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.javasupport;

import java.lang.reflect.Array;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.java.util.ArrayUtils;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name="Java::JavaArray", parent="Java::JavaObject")
public class JavaArray extends JavaObject {

    private final JavaUtil.JavaConverter javaConverter;

    public JavaArray(Ruby runtime, Object array) {
        super(runtime, runtime.getJavaSupport().getJavaArrayClass(), array);
        assert array.getClass().isArray();
        javaConverter = JavaUtil.getJavaConverter(array.getClass().getComponentType());
    }

    public static RubyClass createJavaArrayClass(final Ruby runtime, final RubyModule Java) {
        return createJavaArrayClass(runtime, Java, Java.getClass("JavaObject"));
    }

    static RubyClass createJavaArrayClass(final Ruby runtime, RubyModule Java, RubyClass JavaObject) {
        return Java.defineClassUnder("JavaArray", JavaObject, ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
    }

    public Class getComponentType() {
        return getValue().getClass().getComponentType();
    }

    public RubyFixnum length() {
        return getRuntime().newFixnum(getLength());
    }

    public int getLength() {
        return Array.getLength(getValue());
    }

    public boolean equals(Object other) {
        return other instanceof JavaArray &&
            this.getValue() == ((JavaArray)other).getValue();
    }

    public IRubyObject arefDirect(Ruby runtime, int intIndex) {
        return ArrayUtils.arefDirect(runtime, getValue(), javaConverter, intIndex);
    }

    public IRubyObject aset(IRubyObject index, IRubyObject value) {
         if (! (index instanceof RubyInteger)) {
            throw getRuntime().newTypeError(index, getRuntime().getInteger());
        }
        int intIndex = (int) ((RubyInteger) index).getLongValue();
        if (! (value instanceof JavaObject)) {
            throw getRuntime().newTypeError("not a java object:" + value);
        }
        Object javaObject = ((JavaObject) value).getValue();

        ArrayUtils.setWithExceptionHandlingDirect(getRuntime(), javaObject, intIndex, javaObject);

        return value;
    }

    public IRubyObject asetDirect(Ruby runtime, int intIndex, IRubyObject value) {
        return ArrayUtils.asetDirect(runtime, getValue(), javaConverter, intIndex, value);
    }

    public void setWithExceptionHandling(int intIndex, Object javaObject) {
        ArrayUtils.setWithExceptionHandlingDirect(getRuntime(), getValue(), intIndex, javaObject);
    }

    public IRubyObject afill(IRubyObject beginIndex, IRubyObject endIndex, IRubyObject value) {
        if (! (beginIndex instanceof RubyInteger)) {
            throw getRuntime().newTypeError(beginIndex, getRuntime().getInteger());
        }
        int intIndex = (int) ((RubyInteger) beginIndex).getLongValue();
        if (! (endIndex instanceof RubyInteger)) {
            throw getRuntime().newTypeError(endIndex, getRuntime().getInteger());
        }
        int intEndIndex = (int) ((RubyInteger) endIndex).getLongValue();
        if (! (value instanceof JavaObject)) {
            throw getRuntime().newTypeError("not a java object:" + value);
        }
        Object javaObject = ((JavaObject) value).getValue();
        fillWithExceptionHandling(intIndex, intEndIndex, javaObject);
        return value;
    }

    public void fillWithExceptionHandling(int intIndex, int intEndIndex, Object javaObject) {
        try {
          for ( ; intIndex < intEndIndex; intIndex++) {
            Array.set(getValue(), intIndex, javaObject);
          }
        } catch (IndexOutOfBoundsException e) {
            throw getRuntime().newArgumentError(
                                    "index out of bounds for java array (" + intIndex +
                                    " for length " + getLength() + ")");
        } catch (ArrayStoreException e) {
            throw getRuntime().newArgumentError(
                                    "wrong element type " + javaObject.getClass() + "(array is " +
                                    getValue().getClass() + ")");
        }
    }
}
