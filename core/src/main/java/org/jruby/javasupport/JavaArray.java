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
import org.jruby.RubyBasicObject;
import org.jruby.RubyFixnum;
import org.jruby.api.Convert;
import org.jruby.java.util.ArrayUtils;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.javasupport.Java.castToJavaObject;

/**
 * Java::JavaArray wrapping is no longer used with JRuby.
 * The (automatic) Java proxy wrapping has been the preferred method for a while and
 * works with arrays, use <code>java.lang.Object[2].new</code> as usual.
 *
 * @deprecated since 9.4
 */
@Deprecated(since = "9.4.0.0") // @JRubyClass(name="Java::JavaArray", parent="Java::JavaObject")
public class JavaArray extends JavaObject {

    private final JavaUtil.JavaConverter javaConverter;

    public JavaArray(Ruby runtime, Object array) {
        super(runtime, runtime.getJavaSupport().getJavaArrayClass(), array);
        assert array.getClass().isArray();
        javaConverter = JavaUtil.getJavaConverter(array.getClass().getComponentType());
    }

    public Class getComponentType() {
        return getValue().getClass().getComponentType();
    }

    @Deprecated(since = "10.0.0.0")
    public RubyFixnum length() {
        return asFixnum(getCurrentContext(), getLength());
    }

    public int getLength() {
        return Array.getLength(getValue());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JavaArray && this.getValue() == ((JavaArray) other).getValue();
    }

    @Override
    public int hashCode() {
        return 17 * getValue().hashCode();
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject arefDirect(Ruby runtime, int intIndex) {
        return ArrayUtils.arefDirect(runtime, getValue(), javaConverter, intIndex);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject aset(IRubyObject indexArg, IRubyObject value) {
        var context = getCurrentContext();
        var index = Convert.castAsInteger(context, indexArg).asInt(context);
        var javaObject = castToJavaObject(context, value).getValue();

        ArrayUtils.setWithExceptionHandlingDirect(context.runtime, javaObject, index, javaObject);

        return value;
    }

    public IRubyObject asetDirect(Ruby runtime, int intIndex, IRubyObject value) {
        return ArrayUtils.asetDirect(runtime, getValue(), javaConverter, intIndex, value);
    }

    @Deprecated(since = "10.0.0.0")
    public void setWithExceptionHandling(ThreadContext context, int intIndex, Object javaObject) {
        ArrayUtils.setWithExceptionHandlingDirect(context.runtime, getValue(), intIndex, javaObject);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject afill(IRubyObject beginArg, IRubyObject endArg, IRubyObject value) {
        var context = ((RubyBasicObject) beginArg).getCurrentContext();
        var beginIndex = Convert.castAsInteger(context, beginArg).asInt(context);
        var endIndex = Convert.castAsInteger(context, endArg).asInt(context);
        var javaValue = castToJavaObject(context, value).getValue();

        fillWithExceptionHandling(context, beginIndex, endIndex, javaValue);

        return value;
    }

    @Deprecated(since = "10.0.0.0")
    public final void fillWithExceptionHandling(ThreadContext context, int start, int end, Object javaValue) {
        final Object array = getValue();
        for (int i = start; i < end; i++) {
            ArrayUtils.setWithExceptionHandlingDirect(context.runtime, array, i, javaValue);
        }
    }
}
