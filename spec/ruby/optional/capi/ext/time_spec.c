#include "ruby.h"
#include "rubyspec.h"

#include <time.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifdef HAVE_RB_TIME_NEW
#ifdef RUBY_VERSION_IS_1_8_EX_1_9
static VALUE time_spec_rb_time_new(VALUE self, VALUE sec, VALUE usec) {
  return rb_time_new(NUM2LONG(sec), NUM2LONG(usec));
}
#endif

#ifdef RUBY_VERSION_IS_1_9
static VALUE time_spec_rb_time_new(VALUE self, VALUE sec, VALUE usec) {
  return rb_time_new(NUM2TIMET(sec), NUM2LONG(usec));
}
#endif
#endif

#ifdef HAVE_RB_TIME_NANO_NEW
static VALUE time_spec_rb_time_nano_new(VALUE self, VALUE sec, VALUE nsec) {
  return rb_time_nano_new(NUM2TIMET(sec), NUM2LONG(nsec));
}
#endif

#ifdef HAVE_RB_TIME_NUM_NEW
static VALUE time_spec_rb_time_num_new(VALUE self, VALUE ts, VALUE offset) {
  return rb_time_num_new(ts, offset);
}
#endif

#ifdef HAVE_RB_TIME_INTERVAL
static VALUE time_spec_rb_time_interval(VALUE self, VALUE ts) {
  struct timeval interval = rb_time_interval(ts);
  VALUE ary = rb_ary_new();
  rb_ary_push(ary, TIMET2NUM(interval.tv_sec));
  rb_ary_push(ary, TIMET2NUM(interval.tv_usec));
  return ary;
}
#endif

#ifdef HAVE_RB_TIME_TIMEVAL
static VALUE time_spec_rb_time_timeval(VALUE self, VALUE ts) {
  struct timeval tv = rb_time_timeval(ts);
  VALUE ary = rb_ary_new();
  rb_ary_push(ary, TIMET2NUM(tv.tv_sec));
  rb_ary_push(ary, TIMET2NUM(tv.tv_usec));
  return ary;
}
#endif

#ifdef HAVE_RB_TIME_TIMESPEC
static VALUE time_spec_rb_time_timespec(VALUE self, VALUE time) {
  struct timespec ts = rb_time_timespec(time);
  VALUE ary = rb_ary_new();
  rb_ary_push(ary, TIMET2NUM(ts.tv_sec));
  rb_ary_push(ary, TIMET2NUM(ts.tv_nsec));
  return ary;
}
#endif

#ifdef HAVE_TIMET2NUM
static VALUE time_spec_TIMET2NUM(VALUE self) {
  time_t t = 10;
  return TIMET2NUM(t);
}
#endif

void Init_time_spec() {
  VALUE cls;
  cls = rb_define_class("CApiTimeSpecs", rb_cObject);

#ifdef HAVE_RB_TIME_NEW
  rb_define_method(cls, "rb_time_new", time_spec_rb_time_new, 2);
#endif

#ifdef HAVE_TIMET2NUM
  rb_define_method(cls, "TIMET2NUM", time_spec_TIMET2NUM, 0);
#endif

#ifdef HAVE_RB_TIME_NANO_NEW
  rb_define_method(cls, "rb_time_nano_new", time_spec_rb_time_nano_new, 2);
#endif

#ifdef HAVE_RB_TIME_NUM_NEW
  rb_define_method(cls, "rb_time_num_new", time_spec_rb_time_num_new, 2);
#endif

#ifdef HAVE_RB_TIME_INTERVAL
  rb_define_method(cls, "rb_time_interval", time_spec_rb_time_interval, 1);
#endif

#ifdef HAVE_RB_TIME_TIMEVAL
  rb_define_method(cls, "rb_time_timeval", time_spec_rb_time_timeval, 1);
#endif

#ifdef HAVE_RB_TIME_TIMESPEC
  rb_define_method(cls, "rb_time_timespec", time_spec_rb_time_timespec, 1);
#endif
}

#ifdef __cplusplus
}
#endif
