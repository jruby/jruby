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

#include <jni.h>
#include "JavaException.h"
#include "JLocalEnv.h"

jruby::JavaException::JavaException(JNIEnv* env, jthrowable t)
{
    this->jException = (jthrowable) env->NewGlobalRef(t);
}

jruby::JavaException::JavaException(JNIEnv* env, const char* exceptionName, const char* fmt, ...)
{
    va_list ap;
    char buf[1024] = { 0 };
    va_start(ap, fmt);
    vsnprintf(buf, sizeof(buf) - 1, fmt, ap);

    env->PushLocalFrame(10);
    jclass exceptionClass = env->FindClass(exceptionName);

    if (exceptionClass != NULL) {
        jmethodID init = env->GetMethodID(exceptionClass, "<init>", "(Ljava/lang/String;)V");
        jException = (jthrowable) env->NewGlobalRef(env->NewObject(exceptionClass, init, env->NewStringUTF(buf)));
    }

    env->PopLocalFrame(NULL);
    va_end(ap);
}

jruby::JavaException::~JavaException() throw() {
    try {
        JLocalEnv(false)->DeleteGlobalRef(jException);
    } catch(...) {}
}

jthrowable jruby::JavaException::getCause() const {
    return (jthrowable) JLocalEnv(false)->NewLocalRef(this->jException);
}

const char*
jruby::JavaException::what() const throw() 
{
    return "java exception";
}