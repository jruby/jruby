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
 * Copyright (C) 2010 Tim Felgentreff
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

    if (!rio.f) {
        char mode[4] = "\0";
        // open the file with the given descriptor in a compatible mode
        // switch (rio.mode & FMODE_READWRITE) {
        // case FMODE_READABLE:
        //     strcpy(mode, "rb");
        //     break;
        // case FMODE_WRITABLE:
        //     strcpy(mode, "rb");
        //     break;
        // case FMODE_READWRITE:
        //     strcpy(mode, "rb+");
        //     break;
        // default:
        //     throw JavaException(env, "java/lang/NullPointerException", "Invalid RubyIO mode %d", rio.mode);
        // }

        /* FIXME: XXX: This is using a hammer to squash a fly. I'm not sure how the JVM open's
           it's files and how that relates to the Ruby file modes, but the above case did not work */
        rio.f = fdopen(rio.fd, "ab+");
        if (!rio.f) rio.f = fdopen(rio.fd, "ab");
        if (!rio.f) rio.f = fdopen(rio.fd, "rb");
        if (!rio.f)
            throw JavaException(env, "java/lang/NullPointerException",
                "Invalid RubyIO mode for descriptor %d (opened with %d)", rio.fd, rio.mode);
    }

    // If the file is not closed, sync
    if (rio.fd > -1) {
        /* TODO: Synchronization of stream positions
        long int cpos = ftell(rio.f);
        long long rpos = NUM2LL(callMethod(this, "pos", 0));
        callMethod
        */
    }

    return &rio;
}

RubyIO::RubyIO(FILE* native_file, int native_fd, int mode_)
{
    JLocalEnv env;
    setType(T_FILE);
    memset(&rio, 0, sizeof(rio));
    rio.fd = native_fd;
    rio.f = native_file;
    rio.mode = mode_;
    rio.obj = this->asValue();

    obj = valueToObject(env, callMethod(rb_cIO, "new", 2, INT2FIX(native_fd), INT2FIX(mode_)));
}

RubyIO::RubyIO(JNIEnv* env, jobject obj_, jint fileno, jint mode_): Handle(env, obj_, T_FILE) 
{
    memset(&rio, 0, sizeof(rio));
    rio.fd = (int)fileno;
    rio.f = NULL;
    rio.mode = (int)mode_;
    rio.obj = this->asValue();
}

RubyIO::~RubyIO() {
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
    if (!(io->mode & FMODE_READABLE)) {
        rb_raise(rb_eIOError, "IO is not opened for reading");
    }
}

extern "C" void
rb_io_check_writable(rb_io_t* io) {
    if (!(io->mode & FMODE_WRITABLE)) {
        rb_raise(rb_eIOError, "IO is not opened for writing");
    }
}

extern "C" void
rb_io_check_closed(rb_io_t* io) {
    if ((io->fd < 0) || callMethod(io->obj, "closed?", 0)) {
        rb_raise(rb_eIOError, "IO is closed");
    }
}

extern "C" VALUE
rb_io_close(VALUE io)
{
    return callMethod(io, "close", 0);
}

extern "C" VALUE
rb_file_open(char* filename, char* mode)
{
    return callMethod(rb_cFile, "open", 2, rb_str_new2(filename), rb_str_new2(mode));
}

static int set_non_blocking(int fd) {
#ifndef JRUBY_WIN32
    int flags;
#if defined(O_NONBLOCK)
    if (-1 == (flags = fcntl(fd, F_GETFL, 0)))
        flags = 0;
    return fcntl(fd, F_SETFL, flags | O_NONBLOCK);
#else
    flags = 1;
    return ioctl(fd, FIOBIO, &flags);
#endif
#endif
    return 0; // Fake out on Win32 for now
}
