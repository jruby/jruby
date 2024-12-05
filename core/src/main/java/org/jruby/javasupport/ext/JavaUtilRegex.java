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
 * Copyright (C) 2016 The JRuby Team
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

package org.jruby.javasupport.ext;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RubyStringBuilder;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;
import static org.jruby.javasupport.JavaUtil.convertJavaToUsableRubyObject;
import static org.jruby.javasupport.JavaUtil.unwrapJavaObject;
import static org.jruby.util.Inspector.*;

/**
 * Java::JavaLangReflect package extensions.
 *
 * @author kares
 */
public abstract class JavaUtilRegex {

    public static void define(ThreadContext context) {
        var runtime = context.runtime;
        JavaExtensions.put(runtime, java.util.regex.Pattern.class, proxy -> proxy.defineMethods(context, Pattern.class));
        JavaExtensions.put(runtime, java.util.regex.Matcher.class, proxy -> proxy.defineMethods(context, Matcher.class));
    }

    @JRubyClass(name = "Java::JavaUtilRegex::Pattern")
    public static class Pattern {
        @JRubyMethod(name = "=~")
        public static IRubyObject op_match(final ThreadContext context, final IRubyObject self, IRubyObject str) {
            final java.util.regex.Matcher matcher = matcher(self, str);
            return matcher.find() ? asFixnum(context, matcher.start()) : context.nil;
        }

        @JRubyMethod(name = "match")
        public static IRubyObject match(final ThreadContext context, final IRubyObject self, IRubyObject str) {
            final java.util.regex.Matcher matcher = matcher(self, str);
            if ( ! matcher.find() ) return context.nil;
            final RubyObject matcherProxy = (RubyObject) convertJavaToUsableRubyObject(context.runtime, matcher);
            matcherProxy.setInternalVariable("str", str); // matcher.str = str
            return matcherProxy;
        }

        @JRubyMethod(name = "===")
        public static IRubyObject eqq(final ThreadContext context, final IRubyObject self, IRubyObject str) {
            return asBoolean(context,  matcher(self, str).find() );
        }

        @JRubyMethod(name = "casefold?")
        public static IRubyObject casefold_p(final ThreadContext context, final IRubyObject self) {
            final java.util.regex.Pattern regex = unwrapJavaObject(self);
            return asBoolean(context, (regex.flags() & java.util.regex.Pattern.CASE_INSENSITIVE) != 0);
        }

        @JRubyMethod(name = "inspect")
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            final java.util.regex.Pattern regex = unwrapJavaObject(self);

            RubyString buf = inspectPrefix(context, self.getMetaClass());
            RubyStringBuilder.cat(context.runtime, buf, SPACE);
            RubyStringBuilder.cat(context.runtime, buf, SLASH);
            RubyStringBuilder.cat(context.runtime, buf, regex.toString());
            RubyStringBuilder.cat(context.runtime, buf, SLASH);
            RubyStringBuilder.cat(context.runtime, buf, GT); // >
            return buf;
        }

        private static java.util.regex.Matcher matcher(final IRubyObject self, final IRubyObject str) {
            final java.util.regex.Pattern regex = unwrapJavaObject(self);
            return regex.matcher(str.toJava(CharSequence.class));
        }

    }

    @JRubyClass(name = "Java::JavaUtilRegex::Matcher")
    public static class Matcher {
        @JRubyMethod
        public static IRubyObject regexp(final ThreadContext context, final IRubyObject self) {
            final java.util.regex.Matcher matcher = unwrapJavaObject(self);
            return convertJavaToUsableRubyObject(context.runtime, matcher.pattern());
        }

        @JRubyMethod
        public static IRubyObject begin(final ThreadContext context, final IRubyObject self, final IRubyObject idx) {
            final java.util.regex.Matcher matcher = unwrapJavaObject(self);
            if (idx instanceof RubySymbol) return asFixnum(context, matcher.start(idx.toString()));

            final int group = idx.convertToInteger().getIntValue();
            return asFixnum(context, matcher.start(group));
        }

        @JRubyMethod
        public static IRubyObject end(final ThreadContext context, final IRubyObject self, final IRubyObject idx) {
            final java.util.regex.Matcher matcher = unwrapJavaObject(self);
            if (idx instanceof RubySymbol) return asFixnum(context, matcher.end(idx.toString()));

            final int group = idx.convertToInteger().getIntValue();
            return asFixnum(context, matcher.end(group));
        }

        @JRubyMethod
        public static IRubyObject offset(final ThreadContext context, final IRubyObject self, final IRubyObject idx) {
            final java.util.regex.Matcher matcher = unwrapJavaObject(self);
            final IRubyObject beg;
            final IRubyObject end;

            if (idx instanceof RubySymbol) {
                beg = asFixnum(context, matcher.start(idx.toString()));
                end = asFixnum(context, matcher.end(idx.toString()));
            } else {
                final int group = idx.convertToInteger().getIntValue();
                beg = asFixnum(context, matcher.start(group));
                end = asFixnum(context, matcher.end(group));
            }
            return newArray(context, beg, end);
        }

        @JRubyMethod(name = { "length", "size" })
        public static RubyFixnum size(final ThreadContext context, final IRubyObject self) {
            final java.util.regex.Matcher matcher = unwrapJavaObject(self);
            return asFixnum(context, matcher.groupCount() + 1); // the Ruby way!
        }

        @JRubyMethod
        public static RubyString string(final ThreadContext context, final IRubyObject self) {
            return str(context, self);
        }

        private static RubyString str(final ThreadContext context, final IRubyObject self) {
            final IRubyObject str = (IRubyObject) self.getInternalVariables().getInternalVariable("str");
            return /* str == null ? string(context, self) : */ str.convertToString();
        }

        @JRubyMethod // str[ 0..start(0) ]
        public static IRubyObject pre_match(final ThreadContext context, final IRubyObject self) {
            final java.util.regex.Matcher matcher = unwrapJavaObject(self);
            return str(context, self).substr(context.runtime, 0, matcher.start(0));
        }

        @JRubyMethod // str[ end(0)..-1 ]
        public static IRubyObject post_match(final ThreadContext context, final IRubyObject self) {
            final java.util.regex.Matcher matcher = unwrapJavaObject(self);
            final RubyString str = str(context, self);
            final int offset = matcher.end(0);
            return str.substr(context.runtime, offset, str.size() - offset);
        }

        @JRubyMethod
        public static RubyArray to_a(final ThreadContext context, final IRubyObject self) {
            return RubyArray.newArrayMayCopy(context.runtime, groups(context, self, 0));
        }

        @JRubyMethod
        public static RubyArray captures(final ThreadContext context, final IRubyObject self) {
            return RubyArray.newArrayMayCopy(context.runtime, groups(context, self, 1));
        }

        private static IRubyObject[] groups(final ThreadContext context, final IRubyObject self, final int off) {
            final java.util.regex.Matcher matcher = unwrapJavaObject(self);
            final IRubyObject[] arr = new IRubyObject[ matcher.groupCount() - off + 1 ];
            for ( int i = 0; i < arr.length; i++ ) {
                if ( matcher.start(i + off) == -1 ) {
                    arr[i] = context.nil;
                } else {
                    arr[i] = newString(context, matcher.group(i + off));
                }
            }
            return arr;
        }

        @JRubyMethod(name = "[]")
        public static IRubyObject aref(final ThreadContext context, final IRubyObject self, final IRubyObject idx) {
            final java.util.regex.Matcher matcher = unwrapJavaObject(self);
            if ( idx instanceof RubySymbol || idx instanceof RubyString ) {
                return newString(context, matcher.group(idx.toString()));
            }
            if ( idx instanceof RubyInteger ) {
                final int group = ((RubyInteger) idx).getIntValue();
                return newString(context, matcher.group(group));
            }
            return to_a(context, self).aref(context, idx); // Range
        }

        @JRubyMethod(name = "[]")
        public static IRubyObject aref(final ThreadContext context, final IRubyObject self,
            final IRubyObject arg0, final IRubyObject arg1) {
            return to_a(context, self).aref(arg0, arg1);
        }

        @JRubyMethod(rest = true)
        public static IRubyObject values_at(final ThreadContext context, final IRubyObject self, final IRubyObject[] args) {
            return to_a(context, self).values_at(context, args);
        }

    }

}
