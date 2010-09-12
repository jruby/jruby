/*
 * Copyright (C) 1993-2003 Yukihiro Matsumoto
 * Copyright (C) 2000 Network Applied Communication Laboratory, Inc.
 * Copyright (C) 2000 Information-technology Promotion Agency, Japan
 * Copyright (C) 2010 Tim Felgentreff
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

#include "Handle.h"
#include "JavaException.h"
#include "JLocalEnv.h"
#include "ruby.h"
#include <errno.h>

using namespace jruby;

static int set_non_blocking(int fd);

struct RIO*
RubyIO::toRIO()
{
    JLocalEnv env;

    if (!rio.f && FileDescriptor_object) {
        // open the file with the given descriptor
        int jfd = (int)(env->GetIntField(FileDescriptor_object, FileDescriptor_fd_field));
        rio.fd = jfd;
        rio.f = fdopen(jfd, mode);
        rio.io_obj = (VALUE)this;
    }

    if (rio.fd) {
        /* TODO: Synchronization of stream positions
        long int cpos = ftell(rio.f);
        long long rpos = NUM2LL(callMethod(this, "pos", 0));
        callMethod
        */
    } else {
        throw JavaException(env, "java/lang/NullPointerException",
                "Invalid file descriptor %d\n", rio.fd);
    }

    return &rio;
}

RubyIO::RubyIO(FILE* native_file, int native_fd, const char* native_mode)
{
    JLocalEnv env;
    setType(T_FILE);
    rio.fd = native_fd;
    rio.f = native_file;
    strncpy(mode, native_mode, 4);
    mode[4] = '\0';

    obj = valueToObject(env, callMethod(rb_cIO, "new", 2, INT2FIX(native_fd), rb_str_new_cstr(mode)));
}

RubyIO::RubyIO(JNIEnv* env, jobject obj_, jobject fd_, jstring mode_): Handle(env, obj_, T_FILE) {
    obj = obj_;
    FileDescriptor_object = fd_;
    rio.fd = NULL;
    rio.f = NULL;

    const char* utf = env->GetStringUTFChars(mode_, NULL);
    strncpy(mode, utf, 4);
    mode[4] = '\0';
    env->ReleaseStringUTFChars(mode_, utf);
}

RubyIO::~RubyIO() {
    // For now, just don't close the file.
    // if (rio.f) {
    //     fclose(rio.f);
    // }
}

extern "C" rb_io_t*
jruby_io_struct(VALUE io)
{
    Handle* h = Handle::valueOf(io);
    if (h->getType() != T_FILE) {
        rb_raise(rb_eArgError, "Invalid type. Expected an object of type IO");
    }
    return ((RubyIO*) h)->toRIO();
}

static int
jruby_io_wait(int fd, int read)
{
    bool retry = false;

    if (fd < 0) {
        rb_raise(rb_eIOError, "closed stream");
    }

    switch(errno) {
    case EINTR:
#ifdef ERESTART
    case ERESTART:
#endif
        retry = true;
        break;

    case EAGAIN:
#if defined(EWOULDBLOCK) && EWOULDBLOCK != EAGAIN
    case EWOULDBLOCK:
#endif
        break;

    default:
        return Qfalse;
    }

    fd_set fds;
    FD_ZERO(&fds);
    FD_SET(fd, &fds);

    int ready = 0;

    while (!ready) {
        if (read) {
            ready = rb_thread_select(fd+1, &fds, 0, 0, 0);
        } else {
            ready = rb_thread_select(fd+1, 0, &fds, 0, 0);
        }
        if (!retry) break;
    }

    return Qtrue;
}

extern "C" int
rb_io_wait_readable(int f)
{
    return jruby_io_wait(f, 1);
}

extern "C" int
rb_io_wait_writable(int f)
{
    return jruby_io_wait(f, 0);
}

/** Send #write to io passing str. */
extern "C" VALUE
rb_io_write(VALUE io, VALUE str)
{
    return callMethod(io, "write", 1, str);
}

extern "C" int
rb_io_fd(VALUE io)
{
    return jruby_io_struct(io)->fd;
}

extern "C" void
rb_io_set_nonblock(rb_io_t* io)
{
    set_non_blocking(io->fd);
}

extern "C" void
rb_io_check_readable(rb_io_t* io) {
    callMethod(io->io_obj, "read_nonblock", 1, INT2NUM(0));
}

extern "C" void
rb_io_check_writable(rb_io_t* io) {
    callMethod(io->io_obj, "write_nonblock", 1, INT2NUM(0));
}

static int set_non_blocking(int fd) {
  int flags;
#if defined(O_NONBLOCK)
  if (-1 == (flags = fcntl(fd, F_GETFL, 0)))
    flags = 0;
  return fcntl(fd, F_SETFL, flags | O_NONBLOCK);
#else
  flags = 1;
  return ioctl(fd, FIOBIO, &flags);
#endif
}
