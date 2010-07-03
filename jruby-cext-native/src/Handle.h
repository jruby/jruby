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
#include "queue.h"
#include "ruby.h"

#ifdef	__cplusplus
extern "C" {
#endif

#define CONST_MASK (0x7UL)
#define IS_CONST(x) (((x) & ~CONST_MASK) == 0L)
// FIXME - no need to match ruby here, unless we fold type into flags
#define FL_MARK      (1<<6)
#define FL_FINALIZE  (1<<7)
#define FL_TAINT     (1<<8)
#define FL_EXIVAR    (1<<9)
#define FL_FREEZE    (1<<10)
#define FL_CONST     (1<<11)
#define FL_NSYNC     (1 << 12)
#define FL_JSYNC     (1 << 13)
#define FL_SYNC      (FL_NSYNC | FL_JSYNC)

namespace jruby {

    class Handle;
    extern Handle* constHandles[3];

    class Handle {
    private:
        void Init();
    public:
        Handle();
        Handle(JNIEnv* env, jobject obj, int type_ = T_NONE);
        virtual ~Handle();
        
        static inline Handle* valueOf(VALUE v) {
            return !IS_CONST(v) ? (Handle *) v : jruby::constHandles[(v & CONST_MASK) >> 1];
        }

        /**
         * Sync up the java data with the native data
         *
         * @param env A pointer to a JNIEnv instance.
         */
        virtual void jsync(JNIEnv* env);

        /**
         * Sync up the native data with the java data
         *
         * @param env A pointer to a JNIEnv instance.
         */
        virtual void nsync(JNIEnv* env);

        jobject obj;
        int flags;
        int type;
        TAILQ_ENTRY(Handle) all;
        SIMPLEQ_ENTRY(Handle) syncq;
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
    };

    class RubyArray : public Handle {
    public:
        RubyArray(JNIEnv* env, jobject obj);
        virtual ~RubyArray();
    };

    TAILQ_HEAD(HandleList, Handle);
    TAILQ_HEAD(DataHandleList, RubyData);
    SIMPLEQ_HEAD(SyncQueue, Handle);
    extern HandleList liveHandles, deadHandles;
    extern DataHandleList dataHandles;
    extern SyncQueue syncQueue;

    extern void jsync_(JNIEnv *env);
    extern void nsync_(JNIEnv *env);
    
    inline void jsync(JNIEnv* env) {
        if (unlikely(!SIMPLEQ_EMPTY(&syncQueue))) {
            jsync_(env);
        }
    }

    inline void nsync(JNIEnv* env) {
        if (unlikely(!SIMPLEQ_EMPTY(&syncQueue))) {
            nsync_(env);
        }
    }
}

#ifdef	__cplusplus
}
#endif

#endif	/* JRUBY_HANDLE_H */

