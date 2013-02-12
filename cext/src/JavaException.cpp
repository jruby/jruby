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
 * Copyright (C) 2008, 2009 Wayne Meissner
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

jruby::JavaException::~JavaException() throw()
{
    try {
        JLocalEnv env(false);
        env->DeleteGlobalRef(jException);
    } catch(...) {}
}

jthrowable
jruby::JavaException::getCause() const
{
    JLocalEnv env(false);
    return (jthrowable) env->NewLocalRef(this->jException);
}

jthrowable
jruby::JavaException::getCause(JNIEnv* env) const
{
    return (jthrowable) env->NewLocalRef(this->jException);
}

const char*
jruby::JavaException::what() const throw()
{
    return "java exception";
}
