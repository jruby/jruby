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
 * Copyright (C) 2009 Wayne Meissner
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

#include <stdlib.h>
#include <errno.h>
#include <string.h>

#include "ruby.h"

static size_t checkOverflow(size_t n, size_t size, const char*);

extern "C" void *
xmalloc(size_t size)
{
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
