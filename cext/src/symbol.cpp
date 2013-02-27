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
 * Copyright (C) 2010 Wayne Meissner
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/param.h>
#include <jni.h>

#include <map>
#include <cctype>

#include "JLocalEnv.h"
#include "jruby.h"
#include "Handle.h"
#include "JUtil.h"
#include "ruby.h"



using namespace jruby;

static Symbol* addSymbol(JNIEnv* env, ID id, jobject obj);

struct StringKey {
    StringKey(): str(NULL), len(0) { }
    StringKey(const char* s, long l): str(s), len(l) {}
    const char* str;
    long len;
};

struct StringKeyCompare: public std::binary_function<StringKey, StringKey, bool> {
    inline bool operator()(const StringKey& k1, const StringKey& k2) const {
        return k1.len < k2.len || strncmp(k1.str, k2.str, k2.len) < 0;
    }
};

struct StringCompare: public std::binary_function<const char*, const char*, bool> {
    inline bool operator()(const char* k1, const char* k2) const {
        return strcmp(k1, k2) < 0;
    }
};

static std::map<const char*, ID> constSymbolMap;
static std::map<StringKey, ID, StringKeyCompare> nonConstSymbolMap;

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

#undef rb_intern_const
extern "C" ID
rb_intern_const(const char* name)
{
    std::map<const char*, ID>::iterator it = constSymbolMap.find(name);
    if (it != constSymbolMap.end()) {
        return it->second;
    }

    ID id = rb_intern2(name, strlen(name));

    constSymbolMap.insert(std::map<const char*, ID>::value_type(name, id));

    return id;
}

#undef rb_intern
extern "C" ID
rb_intern(const char* name)
{
    return rb_intern2(name, strlen(name));
}

extern "C" ID
rb_intern2(const char* name, long len)
{
    std::map<StringKey, ID>::iterator it = nonConstSymbolMap.find(StringKey(name, len));
    if (it != nonConstSymbolMap.end()) {
        return it->second;
    }

    JLocalEnv env;
    jobject result = env->CallObjectMethod(getRuntime(), Ruby_newSymbol_method, env->NewStringUTF(name));
    checkExceptions(env);

    Symbol* sym = addSymbol(env, env->GetIntField(result, RubySymbol_id_field), result);
    nonConstSymbolMap.insert(std::map<StringKey, ID>::value_type(StringKey(sym->cstr, (long) len), sym->id));

    return sym->id;
}

extern "C" const char*
rb_id2name(ID sym)
{
    return lookupSymbolById(sym)->cstr;
}

extern "C" ID
rb_to_id(VALUE obj)
{
    return SYM2ID(callMethod(obj, "to_sym", 0));
}

extern "C" int
rb_is_const_id(ID symbol)
{
    return isupper(rb_id2name(symbol)[0]);
}

extern "C" int
rb_is_instance_id(ID symbol)
{
    const char* c_symbol = rb_id2name(symbol);
    return c_symbol[0] == '@' && c_symbol[1] != '@';
}

extern "C" int
rb_is_class_id(ID symbol)
{
    const char* c_symbol = rb_id2name(symbol);
    return c_symbol[0] == '@' && c_symbol[1] == '@';
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
    sym->jstr = env->NewGlobalRef(str);
    checkExceptions(env);

    jint len = env->GetStringLength(str);
    checkExceptions(env);

    sym->cstr = (char *) calloc(len + 1, sizeof(char));
    env->GetStringUTFRegion(str, 0, len, sym->cstr);
    checkExceptions(env);

    return symbols[id] = sym;
}
