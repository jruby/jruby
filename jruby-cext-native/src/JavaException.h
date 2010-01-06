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

#ifndef JRuby_JavaException_h
#define JRuby_JavaException_h

#include <stdexcept>
#include <jni.h>
namespace jruby {
    

class JavaException: public std::exception {
private:
    jthrowable jException;
public:
    JavaException(JNIEnv* env, jthrowable t);
    JavaException(JNIEnv* env, const char* exceptionName, const char* fmt, ...);
    ~JavaException() throw();
    
    jthrowable getCause() const;
    const char* what() const throw();
};

} // namespace JRuby 

#endif // JRuby_JavaException_h
