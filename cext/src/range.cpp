/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2010 Wayne Meissner
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

#include "jruby.h"
#include "ruby.h"

extern "C" VALUE
rb_range_new(VALUE beg, VALUE end, int exclude_end)
{
    return callMethod(rb_cRange, "new", 3, beg, end, exclude_end ? Qtrue : Qfalse);
}

extern "C" VALUE
rb_range_beg_len(VALUE range, long* begp, long* lenp, long len, int err)
{
    long beg, end, b, e;

    if(!rb_obj_is_kind_of(range, rb_cRange)) return Qfalse;

    beg = b = NUM2LONG(rb_funcall(range, rb_intern("begin"), 0));
    end = e = NUM2LONG(rb_funcall(range, rb_intern("end"), 0));

    if(beg < 0) {
        beg += len;
        if(beg < 0) goto out_of_range;
    }
    if(err == 0 || err == 2) {
        if(beg > len) goto out_of_range;
        if(end > len) end = len;
    }
    if(end < 0) end += len;
    if(!RTEST(rb_funcall(range, rb_intern("exclude_end?"), 0))) end++;    /* include end point */
    len = end - beg;
    if(len < 0) len = 0;

    *begp = beg;
    *lenp = len;
    return Qtrue;

out_of_range:
    if(err) {
        rb_raise(rb_eRangeError, "%ld..%ld out of range",
                 b, e);
    }
    return Qnil;
}
