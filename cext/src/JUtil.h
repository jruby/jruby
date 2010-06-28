/*
 * Copyright (C) 2008, 2009 Wayne Meissner
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

#ifndef JRUBY_JUTIL_H
#define JRUBY_JUTIL_H

#include <jni.h>
#include <stdint.h>
#include <exception>

namespace jruby {

    /**
     * Convert a C pointer into a java long
     */
    static inline jlong p2j(const void *p) {
        return (jlong) (uintptr_t) p;
    }

    /**
     * Convert a java long into a C pointer
     */
    static inline void* j2p(jlong j) {
        return (void *) (uintptr_t) j;
    }

    JNIEnv* getCurrentEnv();
    extern JavaVM* jvm;


#define throwException(env, name, fmt, a...) \
    jffi_throwExceptionByName((env), jffi_##name##Exception, fmt, ##a)

    extern const char* IllegalArgumentException;
    extern const char* NullPointerException;
    extern const char* OutOfBoundsException;
    extern const char* OutOfMemoryException;
    extern const char* RuntimeException;


    extern void throwExceptionByName(JNIEnv* env, const char* exceptionName, const char* fmt, ...);

} //namespace jruby

#endif // JRUBY_JUTIL_H
