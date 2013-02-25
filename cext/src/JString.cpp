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

JString::JString(JNIEnv* env, VALUE str) {
    env_ = env;
    jstr_ = (jstring)env->CallObjectMethod(valueToObject(env, str), IRubyObject_asJavaString_method);
    cstr_ = env->GetStringUTFChars(jstr_, NULL);
}

JString::~JString() {
    env_->ReleaseStringUTFChars(jstr_, cstr_);
}

const char*
JString::c_str() const {
    return cstr_;
}

jstring
JString::j_str() const {
    return jstr_;
}

JString::operator std::string() {
    return std::string(cstr_);
}


} // namespace JRuby
