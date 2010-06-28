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

#include <string.h>
#include <stdlib.h>
#include <jni.h>
#include "JUtil.h"

JavaVM* jruby::jvm;

JNIEnv*
jruby::getCurrentEnv()
{
    JNIEnv* env;
    jvm->GetEnv((void **) & env, JNI_VERSION_1_4);
    if (env == NULL) {
        jvm->AttachCurrentThread((void **) & env, NULL);
    }
    return env;
}


void
jruby::throwExceptionByName(JNIEnv* env, const char* exceptionName, const char* fmt, ...)
{
    va_list ap;
    char buf[1024] = { 0 };
    va_start(ap, fmt);
    vsnprintf(buf, sizeof(buf) - 1, fmt, ap);

    env->PushLocalFrame(10);
    jclass exceptionClass = env->FindClass(exceptionName);
    if (exceptionClass != NULL) {
        env->ThrowNew(exceptionClass, buf);
    }
    env->PopLocalFrame(NULL);
    va_end(ap);
}

const char* jruby::IllegalArgumentException = "java/lang/IllegalArgumentException";
const char* jruby::NullPointerException = "java/lang/NullPointerException";
const char* jruby::OutOfBoundsException = "java/lang/IndexOutOfBoundsException";
const char* jruby::OutOfMemoryException = "java/lang/OutOfMemoryError";
const char* jruby::RuntimeException = "java/lang/RuntimeException";

