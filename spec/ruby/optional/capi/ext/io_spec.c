#include "ruby.h"
#include "rubyspec.h"
#ifdef RUBY_VERSION_IS_1_8_EX_1_9
#include "rubyio.h"
#else
#include "ruby/io.h"
#endif
#include <fcntl.h>
#include <unistd.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifdef RUBY_VERSION_IS_1_8_EX_1_8_7
#define rb_io_t OpenFile
#endif

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

#ifdef HAVE_GET_OPEN_FILE
static int io_spec_get_fd(VALUE io) {
  rb_io_t* fp;
  GetOpenFile(io, fp);
#ifdef RUBY_VERSION_IS_1_9
  return fp->fd;
#else
  return fileno(fp->f);
#endif
}

VALUE io_spec_GetOpenFile_fd(VALUE self, VALUE io) {
  return INT2NUM(io_spec_get_fd(io));
}
#endif

#ifdef HAVE_RB_IO_ADDSTR
VALUE io_spec_rb_io_addstr(VALUE self, VALUE io, VALUE str) {
  return rb_io_addstr(io, str);
}
#endif

#ifdef HAVE_RB_IO_PRINTF
VALUE io_spec_rb_io_printf(VALUE self, VALUE io, VALUE ary) {
  long argc = RARRAY_LEN(ary);
  VALUE *argv = alloca(sizeof(VALUE) * argc);
  int i;

  for (i = 0; i < argc; i++) {
    argv[i] = rb_ary_entry(ary, i);
  }

  return rb_io_printf((int)argc, argv, io);
}
#endif

#ifdef HAVE_RB_IO_PRINT
VALUE io_spec_rb_io_print(VALUE self, VALUE io, VALUE ary) {
  long argc = RARRAY_LEN(ary);
  VALUE *argv = alloca(sizeof(VALUE) * argc);
  int i;

  for (i = 0; i < argc; i++) {
    argv[i] = rb_ary_entry(ary, i);
  }

  return rb_io_print((int)argc, argv, io);
}
#endif

#ifdef HAVE_RB_IO_PUTS
VALUE io_spec_rb_io_puts(VALUE self, VALUE io, VALUE ary) {
  long argc = RARRAY_LEN(ary);
  VALUE *argv = alloca(sizeof(VALUE) * argc);
  int i;

  for (i = 0; i < argc; i++) {
    argv[i] = rb_ary_entry(ary, i);
  }

  return rb_io_puts((int)argc, argv, io);
}
#endif

#ifdef HAVE_RB_IO_WRITE
VALUE io_spec_rb_io_write(VALUE self, VALUE io, VALUE str) {
  return rb_io_write(io, str);
}
#endif


#ifdef HAVE_RB_IO_CHECK_READABLE
VALUE io_spec_rb_io_check_readable(VALUE self, VALUE io) {
  rb_io_t* fp;
  GetOpenFile(io, fp);
  rb_io_check_readable(fp);
  return Qnil;
}
#endif

#ifdef HAVE_RB_IO_CHECK_WRITABLE
VALUE io_spec_rb_io_check_writable(VALUE self, VALUE io) {
  rb_io_t* fp;
  GetOpenFile(io, fp);
  rb_io_check_writable(fp);
  return Qnil;
}
#endif

#ifdef HAVE_RB_IO_CHECK_CLOSED
VALUE io_spec_rb_io_check_closed(VALUE self, VALUE io) {
  rb_io_t* fp;
  GetOpenFile(io, fp);
  rb_io_check_closed(fp);
  return Qnil;
}
#endif

#ifdef RUBY_VERSION_IS_1_9
typedef int wait_bool;
#define wait_bool_to_ruby_bool(x) (x ? Qtrue : Qfalse)
#else
typedef VALUE wait_bool;
#define wait_bool_to_ruby_bool(x) (x)
#endif

#ifdef HAVE_RB_IO_WAIT_READABLE
#define RB_IO_WAIT_READABLE_BUF 13

VALUE io_spec_rb_io_wait_readable(VALUE self, VALUE io, VALUE read_p) {
  int fd = io_spec_get_fd(io);
  char buf[RB_IO_WAIT_READABLE_BUF];
  wait_bool ret;

  set_non_blocking(fd);

  if(RTEST(read_p)) {
    rb_ivar_set(self, rb_intern("@write_data"), Qtrue);
    if(read(fd, buf, RB_IO_WAIT_READABLE_BUF) != -1) {
      return Qnil;
    }
  }

  ret = rb_io_wait_readable(fd);

  if(RTEST(read_p)) {
    if(read(fd, buf, RB_IO_WAIT_READABLE_BUF) != 13) {
      return Qnil;
    }
    rb_ivar_set(self, rb_intern("@read_data"),
        rb_str_new(buf, RB_IO_WAIT_READABLE_BUF));
  }

  return wait_bool_to_ruby_bool(ret);
}
#endif

#ifdef HAVE_RB_IO_WAIT_WRITABLE
VALUE io_spec_rb_io_wait_writable(VALUE self, VALUE io) {
  wait_bool ret;
  ret = rb_io_wait_writable(io_spec_get_fd(io));
  return wait_bool_to_ruby_bool(ret);
}
#endif

#ifdef HAVE_RB_THREAD_WAIT_FD
VALUE io_spec_rb_thread_wait_fd(VALUE self, VALUE io) {
  rb_thread_wait_fd(io_spec_get_fd(io));
  return Qnil;
}
#endif

#ifdef HAVE_RB_THREAD_FD_WRITABLE
VALUE io_spec_rb_thread_fd_writable(VALUE self, VALUE io) {
  rb_thread_fd_writable(io_spec_get_fd(io));
  return Qnil;
}
#endif

#ifdef HAVE_RB_IO_BINMODE
VALUE io_spec_rb_io_binmode(VALUE self, VALUE io) {
  return rb_io_binmode(io);
}
#endif

#ifdef HAVE_RB_IO_CLOSE
VALUE io_spec_rb_io_close(VALUE self, VALUE io) {
  return rb_io_close(io);
}
#endif

void Init_io_spec() {
  VALUE cls = rb_define_class("CApiIOSpecs", rb_cObject);

#ifdef HAVE_GET_OPEN_FILE
  rb_define_method(cls, "GetOpenFile_fd", io_spec_GetOpenFile_fd, 1);
#endif

#ifdef HAVE_RB_IO_ADDSTR
  rb_define_method(cls, "rb_io_addstr", io_spec_rb_io_addstr, 2);
#endif

#ifdef HAVE_RB_IO_PRINTF
  rb_define_method(cls, "rb_io_printf", io_spec_rb_io_printf, 2);
#endif

#ifdef HAVE_RB_IO_PRINT
  rb_define_method(cls, "rb_io_print", io_spec_rb_io_print, 2);
#endif

#ifdef HAVE_RB_IO_PUTS
  rb_define_method(cls, "rb_io_puts", io_spec_rb_io_puts, 2);
#endif

#ifdef HAVE_RB_IO_WRITE
  rb_define_method(cls, "rb_io_write", io_spec_rb_io_write, 2);
#endif

#ifdef HAVE_RB_IO_CLOSE
  rb_define_method(cls, "rb_io_close", io_spec_rb_io_close, 1);
#endif

#ifdef HAVE_RB_IO_CHECK_READABLE
  rb_define_method(cls, "rb_io_check_readable", io_spec_rb_io_check_readable, 1);
#endif

#ifdef HAVE_RB_IO_CHECK_WRITABLE
  rb_define_method(cls, "rb_io_check_writable", io_spec_rb_io_check_writable, 1);
#endif

#ifdef HAVE_RB_IO_CHECK_CLOSED
  rb_define_method(cls, "rb_io_check_closed", io_spec_rb_io_check_closed, 1);
#endif

#ifdef HAVE_RB_IO_WAIT_READABLE
  rb_define_method(cls, "rb_io_wait_readable", io_spec_rb_io_wait_readable, 2);
#endif

#ifdef HAVE_RB_IO_WAIT_WRITABLE
  rb_define_method(cls, "rb_io_wait_writable", io_spec_rb_io_wait_writable, 1);
#endif

#ifdef HAVE_RB_THREAD_WAIT_FD
  rb_define_method(cls, "rb_thread_wait_fd", io_spec_rb_thread_wait_fd, 1);
#endif

#ifdef HAVE_RB_THREAD_FD_WRITABLE
  rb_define_method(cls, "rb_thread_fd_writable", io_spec_rb_thread_fd_writable, 1);
#endif

#ifdef HAVE_RB_IO_BINMODE
  rb_define_method(cls, "rb_io_binmode", io_spec_rb_io_binmode, 1);
#endif

}

#ifdef __cplusplus
}
#endif
