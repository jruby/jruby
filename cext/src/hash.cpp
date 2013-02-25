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
 * Copyright (C) 2008,2009 Wayne Meissner
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
#include "st.h"

using namespace jruby;

/* Hash */
extern "C" VALUE
rb_hash_new(void)
{
    return callMethod(rb_cHash, "new", 0);
}

extern "C" VALUE
rb_hash_aref(VALUE hash, VALUE key)
{
    return callMethod(hash, "[]", 1, key);
}

extern "C" VALUE
rb_hash_lookup(VALUE hash, VALUE key)
{
    if (RTEST(callMethod(hash, "has_key?", 1, key))) {
        return rb_hash_aref(hash, key);
    }
    return Qnil; // without Hash#default
}

extern "C" VALUE
rb_hash_aset(VALUE hash, VALUE key, VALUE val)
{
    return callMethod(hash, "[]=", 2, key, val);
}

extern "C" VALUE
rb_hash_delete(VALUE hash, VALUE key)
{
    return callMethod(hash, "delete", 1, key);
}

extern "C" VALUE
rb_hash_size(VALUE hash)
{
    return callMethod(hash, "size", 0);
}

extern "C" void
rb_hash_foreach(VALUE hash, int (*func)(ANYARGS), VALUE arg)
{
    long size = NUM2LONG(rb_hash_size(hash));
    if (size == 0) return;

    VALUE hash_array = callMethod(hash, "to_a", 0);
    for (long i = 0; i < size; i++) {
        VALUE key_value_ary = rb_ary_entry(hash_array, i);
        VALUE key = rb_ary_entry(key_value_ary, 0);
        VALUE value = rb_ary_entry(key_value_ary, 1);

        int ret = (*func)(key, value, arg);
        switch (ret) {

        case ST_CONTINUE:
            continue;

        case ST_STOP:
            return;

        case ST_DELETE:
            callMethod(hash, "delete", 1, key);
            continue;

        default:
            rb_raise(rb_eArgError, "unsupported hash_foreach value");
        }
    }
}

extern "C" VALUE
rb_hash_delete_if(VALUE obj)
{
    if (rb_block_given_p()) {
        return jruby_funcall2b(obj, rb_intern("delete_if"), 0, NULL, rb_block_proc());
    } else {
        return callMethod(obj, "delete_if", 0, NULL);
    }
}
