/*
 * Copyright (C) 2008, 2009 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "jruby.h"
#include "ruby.h"

using namespace jruby;

/* Hash */
extern "C" VALUE 
rb_hash_new(void)
{
    return callMethod(getClass("Hash"), "new", 0);
}
extern "C" VALUE 
rb_hash_aref(VALUE hash, VALUE key) 
{
    return callMethod(hash, "[]", 1, key);
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