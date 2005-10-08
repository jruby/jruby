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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport;

import java.lang.reflect.Array;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaArray extends JavaObject {

    public JavaArray(IRuby runtime, Object array) {
        super(runtime, runtime.getModule("Java").getClass("JavaArray"), array);
        assert array.getClass().isArray();
    }

    public static RubyClass createJavaArrayClass(IRuby runtime, RubyModule javaModule) {
        return javaModule.defineClassUnder("JavaArray", javaModule.getClass("JavaObject"));
    }

    public RubyFixnum length() {
        return getRuntime().newFixnum(getLength());
    }

    private int getLength() {
        return Array.getLength(getValue());
    }

    public IRubyObject aref(IRubyObject index) {
        if (! (index instanceof RubyInteger)) {
            throw getRuntime().newTypeError(index, getRuntime().getClass("Integer"));
        }
        int intIndex = (int) ((RubyInteger) index).getLongValue();
        if (intIndex < 0 || intIndex >= getLength()) {
            throw getRuntime().newArgumentError(
                                    "index out of bounds for java array (" + intIndex +
                                    " for length " + getLength() + ")");
        }
        Object result = Array.get(getValue(), intIndex);
        if (result == null) {
            return getRuntime().getNil();
        }
        return JavaObject.wrap(getRuntime(), result);
    }

    public IRubyObject aset(IRubyObject index, IRubyObject value) {
         if (! (index instanceof RubyInteger)) {
            throw getRuntime().newTypeError(index, getRuntime().getClass("Integer"));
        }
        int intIndex = (int) ((RubyInteger) index).getLongValue();
        if (! (value instanceof JavaObject)) {
            throw getRuntime().newTypeError("not a java object:" + value);
        }
        Object javaObject = ((JavaObject) value).getValue();
        try {
            Array.set(getValue(), intIndex, javaObject);
        } catch (IndexOutOfBoundsException e) {
            throw getRuntime().newArgumentError(
                                    "index out of bounds for java array (" + intIndex +
                                    " for length " + getLength() + ")");
        } catch (ArrayStoreException e) {
            throw getRuntime().newArgumentError(
                                    "wrong element type " + javaObject.getClass() + "(array is " +
                                    getValue().getClass() + ")");
        }
        return value;
    }
}
