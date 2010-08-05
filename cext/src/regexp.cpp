/*
 * Copyright (C) 2010 Wayne Meissner, Tim Felgentreff
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
