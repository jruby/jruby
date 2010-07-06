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
#include "Handle.h"
#include "JUtil.h"
#include "ruby.h"



using namespace jruby;

static std::map<const char*, ID> constSymbolMap;
std::vector<jobject> jruby::symbols;

jobject
jruby::resolveSymbolById(JNIEnv* env, ID id)
{
    jobject obj = env->CallStaticObjectMethod(Symbol_class, RubySymbol_getSymbolLong,
            jruby::getRuntime(), (jlong) id);

    if (env->IsSameObject(obj, NULL)) {
        rb_raise(rb_eRuntimeError, "could not resolve symbol ID %lld", (long long) id);
    }

    if (symbols.size() <= id) {
        symbols.resize(id + 1);
    }
    
    return symbols[id] = env->NewGlobalRef(obj);
}

extern "C" ID
rb_intern_const(const char* name)
{
    std::map<const char*, ID>::iterator it = constSymbolMap.find(name);
    if (it != constSymbolMap.end()) {
        return it->second;
    }

    return constSymbolMap[name] = jruby_intern_nonconst(name);
}

extern "C" ID
jruby_intern_nonconst(const char* name)
{
    JLocalEnv env;
    jobject result = env->CallObjectMethod(getRuntime(), Ruby_newSymbol_method, env->NewStringUTF(name));
    checkExceptions(env);

    ID id = env->GetIntField(result, RubySymbol_id_field);

    if (symbols.size() <= id) {
        symbols.resize(id + 1);
    }

    symbols[id] = env->NewGlobalRef(result);
    
    return id;
}
