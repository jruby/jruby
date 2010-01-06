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

#ifndef loadmod_jstring_h
#define loadmod_jstring_h

#include <string>
#include <jni.h>

namespace jruby {
    

class JString {
public:
	JString(JNIEnv *, jstring);
	~JString();
	const char* c_str() const;
	operator bool() { return cstr_ != NULL; }
	bool operator==(void *ptr) { return cstr_ == ptr; }
	operator std::string();
private:
	jstring jstr_;
	JNIEnv* env_;
	const char* cstr_;
};


} // namespace JRuby
#endif // loadmod_jstring_h
