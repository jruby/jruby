/*
 * Copyright (C) 2010 Wayne Meissner
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <sys/param.h>
#include <jni.h>

#include <map>

#include "JLocalEnv.h"
#include "jruby.h"
#include "JUtil.h"
#include "ruby.h"

static std::map<const char*, VALUE> constSymbolMap;

using namespace jruby;

extern "C" ID
rb_intern_const(const char* name)
{
    std::map<const char*, VALUE>::iterator it = constSymbolMap.find(name);
    if (it != constSymbolMap.end()) {
        return it->second;
    }

    return constSymbolMap[name] = jruby_intern_nonconst(name);
}

extern "C" const char*
rb_id2name(ID sym) {
    return RSTRING_PTR(callMethod((VALUE)sym, "to_s", 0, NULL));
}

extern "C" ID
jruby_intern_nonconst(const char* name)
{
    JLocalEnv env;
    jobject result = env->CallObjectMethod(getRuntime(), Ruby_newSymbol_method, env->NewStringUTF(name));
    checkExceptions(env);

    VALUE v = objectToValue(env, result);
    Handle* h = (Handle *) v;
    h->type = T_SYMBOL;

    return v;
}