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

#ifndef HANDLE_H
#define	HANDLE_H

#include <jni.h>

#ifdef	__cplusplus
extern "C" {
#endif


    class Handle {
    public:
        Handle();
        jweak obj;
        int flags;
        int type;
        void (*finalize)(Handle *);
        void (*dmark)(void *);
        void (*dfree)(void *);
        void* data;
    };

// FIXME - no need to match ruby here, unless we fold type into flags
#define FL_MARK      (1<<6)
#define FL_FINALIZE  (1<<7)
#define FL_TAINT     (1<<8)
#define FL_EXIVAR    (1<<9)
#define FL_FREEZE    (1<<10)

#ifdef	__cplusplus
}
#endif

#endif	/* HANDLE_H */

