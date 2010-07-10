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


struct StringCompare: public std::binary_function<const char*, const char*, bool> {
    inline bool operator()(const char* k1, const char* k2) const {
        return strcmp(k1, k2) < 0;
    }
};

static std::map<const char*, ID> constSymbolMap;
static std::map<const char*, ID, StringCompare> nonConstSymbolMap;

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

    ID id = jruby_intern_nonconst(name);

    constSymbolMap.insert(std::map<const char*, ID>::value_type(name, id));

    return id;
}

extern "C" const char*
rb_id2name(ID sym)
{
    return lookupSymbolById(sym)->cstr;
}

extern "C" ID
jruby_intern_nonconst(const char* name)
{
    std::map<const char*, ID>::iterator it = nonConstSymbolMap.find(name);
    if (it != nonConstSymbolMap.end()) {
        return it->second;
    }

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

    sym->cstr = (char *) calloc(len + 1, sizeof(char));
    env->GetStringUTFRegion(str, 0, len, sym->cstr);
    checkExceptions(env);

    nonConstSymbolMap.insert(std::map<const char*, ID>::value_type(sym->cstr, sym->id));

    return symbols[id] = sym;
}