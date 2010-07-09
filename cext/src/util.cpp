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

#include "jruby.h"
#include "ruby.h"
#include "JLocalEnv.h"

using namespace jruby;

int rb_scan_args(int argc, const VALUE* argv, const char* spec, ...) {
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
    }
    else {
      goto error;
    }

    if (ISDIGIT(*p)) {
      n = i + *p - '0';
      for (; i<n; i++) {
        var = va_arg(vargs, VALUE*);
        if (argc > i) {
          if (var) *var = argv[i];
        }
        else {
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
      }
      else {
        if (var) *var = rb_ary_new();
      }
      p++;
    }

    if (*p == '&') {
      var = va_arg(vargs, VALUE*);
      // *var = TODO: Get the passed block and put it into the var
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
