/*
 * Copyright (C) 2009 Wayne Meissner
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

#include <stdlib.h>
#include <errno.h>
#include <string.h>

#include "ruby.h"

static size_t checkOverflow(size_t n, size_t size, const char*);

extern "C" void *
xmalloc(size_t size)
{
    if (size < 0) {
        rb_raise(rb_eArgError, "malloc: negative size allocation");
        return NULL;
    }

    void* ptr = malloc(size);
    if (ptr == NULL) {
        rb_raise(rb_eNoMemError, "malloc(3) failed: %s", strerror(errno));
        return NULL;
    }

    return ptr;
}

extern "C" void *
xmalloc2(size_t n, size_t size)
{
    return xmalloc(checkOverflow(n, size, "malloc"));
}

extern "C" void *
xcalloc(size_t n, size_t size)
{
    void* ptr = xmalloc2(n, size);

    memset(ptr, 0, n * size);

    return ptr;
}

extern "C" void *
xrealloc(void* old, size_t size)
{
    void* ptr = realloc(old, size);
    if (ptr == NULL) {
        rb_raise(rb_eNoMemError, "realloc(3) failed: %s", strerror(errno));
        return NULL;
    }

    return ptr;
}

extern "C" void *
xrealloc2(void* old, size_t n, size_t size)
{
    return xrealloc(old, checkOverflow(n, size, "realloc"));
}

extern "C" void
xfree(void* ptr)
{
    free(ptr);
}



static size_t
checkOverflow(size_t n, size_t size, const char* fn)
{
    size_t len = size * n;
    if (n != 0 && size != len / n) {
        rb_raise(rb_eArgError, "%s: possible integer overflow", fn);
    }

    return len;
}

