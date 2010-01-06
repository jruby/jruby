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
#include "JUtil.h"
#include "JLocalEnv.h"

namespace jruby {

JLocalEnv::JLocalEnv(bool popFrame, int depth)
{
    detach = jvm->GetEnv((void **) & env, JNI_VERSION_1_4) != JNI_OK &&
        jvm->AttachCurrentThread((void **) & env, NULL) == JNI_OK;
    if (env == NULL) {
        throw std::exception();
    }

    env->PushLocalFrame(depth);
}

JLocalEnv::JLocalEnv(JNIEnv* env, bool popFrame, int depth)
{
    this->env = env;
    detach = false;
    pop = popFrame;
    if (popFrame) {
        env->PushLocalFrame(depth);
    }
}

JLocalEnv::~JLocalEnv()
{
    if (pop && env != NULL) {
        env->PopLocalFrame(NULL);
    }
    if (detach) {
        jvm->DetachCurrentThread();
    }
}

} // namespace JRuby
