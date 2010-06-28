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

#include <stdio.h>
#include <string>
#include <stdint.h>

#include <jni.h>
#include "JString.h"

namespace jruby {

JString::JString(JNIEnv* env, jstring jstr) {
    env_ = env;
    jstr_ = jstr;
    cstr_ = env->GetStringUTFChars(jstr, NULL);
}

JString::~JString() {
    env_->ReleaseStringUTFChars(jstr_, cstr_);
}

const char*
JString::c_str() const {
    return cstr_;
}

JString::operator std::string() {
    return std::string(cstr_);
}


} // namespace JRuby
