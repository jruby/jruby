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

package org.jruby.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Links a Java declaration to the CRuby C function or macro it implements or was ported from.
 *
 * <pre>
 * &#64;CRuby("match_size")
 * &#64;CRuby(value = "match_begin", file = "re.c")
 * &#64;CRuby(value = "ary_reject_bang", file = "array.c")
 * &#64;CRuby(value = "reject_bang_i", file = "array.c")
 * &#64;CRuby(value = "rb_str_count", note = "first half")
 * </pre>
 *
 * Use it in place of a {@code // MRI: rb_foo} comment or a Javadoc that only names the C function, so tools can
 * audit the mapping and documentation can link to the C source. It is retained at runtime so that tools working from a
 * live JRuby (IRB, RI, language servers) can find it by reflecting on the Java method behind a Ruby method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE, ElementType.FIELD})
@Repeatable(CRuby.List.class)
public @interface CRuby {
    /**
     * The C function or macro, e.g. "match_integer_at".
     */
    String value();

    /**
     * The C source file defining it, relative to the CRuby root, e.g. "re.c". Optional, mainly useful when the
     * name is defined in more than one file.
     */
    String file() default "";

    /**
     * Free text qualifying the link, e.g. "first half" or "loop body".
     */
    String note() default "";

    /**
     * Container for repeated {@link CRuby} annotations; not used directly.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE, ElementType.FIELD})
    @interface List {
        CRuby[] value();
    }
}
