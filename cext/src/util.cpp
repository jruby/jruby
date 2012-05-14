/*
 * Copyright (C) 2010 Tim Felgentreff
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

/* Parts of this file are copyright (c) 2007, Evan Phoenix
 * The following license applies.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  * Neither the name of the Evan Phoenix nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "jruby.h"
#include "ruby.h"
#include "JLocalEnv.h"

using namespace jruby;

extern "C" int
rb_scan_args(int argc, const VALUE* argv, const char* spec, ...)
{
    int n, i = 0;
    const char *p = spec;
    VALUE *var;
    va_list vargs;

    va_start(vargs, spec);

    if (*p == '*') goto rest_arg;

    if (ISDIGIT(*p)) {
        n = *p - '0';
        if (n > argc)
            rb_raise(rb_eArgError, "wrong number of arguments (%d for %d)", argc, n);
        for (i=0; i<n; i++) {
            var = va_arg(vargs, VALUE*);
            if (var) *var = argv[i];
        }
        p++;
    } else {
        goto error;
    }

    if (ISDIGIT(*p)) {
        n = i + *p - '0';
        for (; i<n; i++) {
            var = va_arg(vargs, VALUE*);
            if (argc > i) {
                if (var) *var = argv[i];
            } else {
                if (var) *var = Qnil;
            }
        }
        p++;
    }

    if(*p == '*') {
        rest_arg:
        var = va_arg(vargs, VALUE*);
        if (argc > i) {
            if (var) *var = rb_ary_new4(argc-i, argv+i);
            i = argc;
        } else {
            if (var) *var = rb_ary_new();
        }
        p++;
    }

    if (*p == '&') {
        var = va_arg(vargs, VALUE*);
        if (var) {
            if (rb_block_given_p()) {
                *var = rb_block_proc();
            } else {
                *var = Qnil;
            }
        }
        p++;
    }
    va_end(vargs);

    if (*p != '\0') {
        goto error;
    }

    if (argc > i) {
        rb_raise(rb_eArgError, "wrong number of arguments (%d for %d)", argc, i);
    }

    return argc;

    error:
    rb_raise(rb_eFatal, "bad scan arg format: %s", spec);
    return 0;
}

// Imported from MRI
extern "C" char *
ruby_strdup(const char *str) 
{
    char *tmp;
    size_t len = strlen(str) + 1;

    tmp = (char *) xmalloc(len);
    memcpy(tmp, str, len);

    return tmp;
}

#undef setenv
extern "C" void 
ruby_setenv(const char *name, const char *value)
{
    setenv(name, value, 1);
}
