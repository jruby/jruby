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

static Symbol* addSymbol(JNIEnv* env, ID id, jobject obj);

static std::map<const char*, ID> constSymbolMap;
std::vector<Symbol*> jruby::symbols;

Symbol*
jruby::resolveSymbolById(ID id)
{
    JLocalEnv env;

    jobject obj = env->CallStaticObjectMethod(Symbol_class, RubySymbol_getSymbolLong,
            jruby::getRuntime(), (jlong) id);

    if (env->IsSameObject(obj, NULL)) {
        rb_raise(rb_eRuntimeError, "could not resolve symbol ID %lld", (long long) id);
    }

    return addSymbol(env, id, obj);
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

extern "C" const char*
rb_id2name(ID sym)
{
    return lookupSymbolById(sym)->cstr;
}

extern "C" ID
jruby_intern_nonconst(const char* name)
{
    JLocalEnv env;
    jobject result = env->CallObjectMethod(getRuntime(), Ruby_newSymbol_method, env->NewStringUTF(name));
    checkExceptions(env);

    return addSymbol(env, env->GetIntField(result, RubySymbol_id_field), result)->id;
}

static Symbol*
addSymbol(JNIEnv* env, ID id, jobject obj)
{
    if (symbols.size() <= id) {
        symbols.resize(id + 1);
    }
    if (symbols[id] != NULL) {
        return symbols[id];
    }

    Symbol* sym = new Symbol;
    sym->obj = env->NewGlobalRef(obj);
    sym->id = id;

    jstring str = (jstring) env->GetObjectField(obj, RubySymbol_symbol_field);
    checkExceptions(env);

    jint len = env->GetStringLength(str);
    checkExceptions(env);

    sym->cstr = (char *) malloc(len + 1);
    env->GetStringUTFRegion(str, 0, len, sym->cstr);
    checkExceptions(env);

    return symbols[id] = sym;
}