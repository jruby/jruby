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
 * Copyright (C) 2010 Wayne Meissner, Tim Felgentreff
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

using namespace jruby;

extern "C" VALUE
rb_reg_nth_match(long nth, VALUE match_data) {
  if (NIL_P(match_data)) {
    return Qnil;
  }
  return callMethod(match_data, "[]", 1, LONG2NUM(nth));
}

extern "C" VALUE
rb_reg_match(VALUE re, VALUE str)
{
    return callMethodA(re, "=~", 1, &str);
}

extern "C" VALUE
rb_reg_new(const char *str, long len, int options)
{
    return callMethod(rb_cRegexp, "new", 2, rb_str_new(str, len), INT2NUM(options));
}

extern "C" VALUE
rb_reg_source(VALUE regexp)
{
    return callMethod(regexp, "source", 0);
}

extern "C" int
rb_reg_options(VALUE regexp)
{
    VALUE count = callMethod(regexp, "options", 0);
    return FIX2INT(count);
}

extern "C" VALUE
rb_reg_regcomp(VALUE str)
{
    return callMethod(rb_cRegexp, "new", 1, str);
}

extern "C" VALUE
rb_backref_get(void)
{
    return rb_gv_get("$~");
}
