/*
 * Copyright (C) 2008-2010 Wayne Meissner
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

#ifndef JRUBY_HANDLE_H
#define	JRUBY_HANDLE_H

#include <jni.h>
#include <vector>
#include "jruby.h"
#include "util.h"
#include "queue.h"
#include "ruby.h"

#ifdef	__cplusplus
extern "C" {
#endif

namespace jruby {

    class Handle;
    class RubyData;
    struct Symbol;
    extern Handle* constHandles[3];
    extern std::vector<Symbol*> symbols;
    TAILQ_HEAD(HandleList, Handle);
    TAILQ_HEAD(DataHandleList, RubyData);
    SIMPLEQ_HEAD(SyncQueue, Handle);
    extern HandleList liveHandles, deadHandles;
    extern DataHandleList dataHandles;


    class Handle {
    private:
        void Init();
        void makeStrong_(JNIEnv* env);


    public:
        Handle();
        Handle(JNIEnv* env, jobject obj, int type_ = T_NONE);
        virtual ~Handle();

        static inline Handle* valueOf(VALUE v) {
            return likely(!SPECIAL_CONST_P(v)) ? (Handle *) v : specialHandle(v);
        }

        inline void makeStrong(JNIEnv* env) {
            if (unlikely((flags & FL_WEAK) != 0)) {
                makeStrong_(env);
            }
        }

        inline int getType() {
            return flags & T_MASK;
        }

        inline void setType(int type_) {
            this->flags |= (type_ & T_MASK);
        }

        static Handle* specialHandle(VALUE v);

        jobject obj;
        int flags;
        TAILQ_ENTRY(Handle) all;
    };

    class RubyFixnum : public Handle {
    private:
        jlong value;

    public:
        RubyFixnum(JNIEnv* env, jobject obj_, jlong value_);

        inline jlong longValue() {
            return value;
        }
    };

    class RubyFloat : public Handle {
    private:
        struct RFloat rfloat;

    public:
        RubyFloat(jdouble value_);
        RubyFloat(JNIEnv* env, jobject obj_, jdouble value_);

        inline jdouble doubleValue() {
            return rfloat.value;
        }

        inline struct RFloat* toRFloat() {
            return &rfloat;
        }

    };

    class RubyData : public Handle {
    public:
        virtual ~RubyData();
        TAILQ_ENTRY(RubyData) dataList;
        void (*dmark)(void *);
        void (*dfree)(void *);
        void* data;
    };

    class RubyString : public Handle {
    public:
        RubyString(JNIEnv* env, jobject obj);
        virtual ~RubyString();

        RString* toRString(bool readonly);
        bool jsync(JNIEnv* env);
        bool nsync(JNIEnv* env);
        bool clean(JNIEnv* env);
        int length();
    private:
        struct RWData {
            bool readonly;
            RString* rstring;
            DataSync jsync;
            DataSync nsync;
            DataSync clean;
            DataSync rosync;
        };
        RWData rwdata;
    };

    class RubyArray : public Handle {
    private:
        struct RArray rarray;
    public:
        RubyArray(JNIEnv* env, jobject obj);
        virtual ~RubyArray();

        struct RArray* toRArray();
    };

    extern void runSyncQueue(JNIEnv* env, DataSyncQueue* q);

    inline void jsync(JNIEnv* env) {
        if (unlikely(!TAILQ_EMPTY(&jsyncq))) {
            runSyncQueue(env, &jsyncq);
        }
    }

    inline void nsync(JNIEnv* env) {
        if (unlikely(!TAILQ_EMPTY(&nsyncq))) {
            runSyncQueue(env, &nsyncq);
        }
    }

    inline VALUE makeStrongRef(JNIEnv* env, VALUE v) {
        if (!SPECIAL_CONST_P(v)) {
            Handle::valueOf(v)->makeStrong(env);
        }

        return v;
    }

    struct Symbol {
        ID id;
        char* cstr;
        jobject obj;
    };

    extern Symbol* resolveSymbolById(ID id);
    
    inline Symbol* lookupSymbolById(ID id) {
        Symbol* sym;
        if (likely(id < symbols.size() && (sym  = symbols[id]) != NULL)) {
            return sym;
        }
        return resolveSymbolById(id);
    }
    
    inline jobject idToObject(JNIEnv* env, ID id) {
        return lookupSymbolById(id)->obj;
    }

    extern jobject fixnumToObject(JNIEnv* env, VALUE v);
}

#ifdef	__cplusplus
}
#endif

#endif	/* JRUBY_HANDLE_H */

