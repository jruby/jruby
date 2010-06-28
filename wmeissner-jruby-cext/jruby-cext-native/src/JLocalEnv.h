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

#ifndef JRUBY_JLOCALENV_H
#define JRUBY_JLOCALENV_H

namespace jruby {
    
class JLocalEnv {
public:
	JLocalEnv(bool popFrame = true, int depth = 100);
	JLocalEnv(JNIEnv* env, bool popFrame = true, int depth = 100);
	~JLocalEnv();
	operator JNIEnv*() { return env; }
	JNIEnv* operator->() { return env; }
private:
        bool detach;
        bool pop;
	JNIEnv* env;
};

}
#endif // JRUBY_JLOCALENV_H
