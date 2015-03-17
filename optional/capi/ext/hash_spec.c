#include "ruby.h"
#include "rubyspec.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifdef HAVE_RB_HASH
VALUE hash_spec_rb_hash(VALUE self, VALUE hash) {
  return rb_hash(hash);
}
#endif

#ifdef HAVE_RB_HASH_AREF
VALUE hash_spec_rb_hash_aref(VALUE self, VALUE hash, VALUE key) {
  return rb_hash_aref(hash, key);
}

VALUE hash_spec_rb_hash_aref_nil(VALUE self, VALUE hash, VALUE key) {
  VALUE ret = rb_hash_aref(hash, key);
  return ret == Qnil ? Qtrue : Qfalse;
}
#endif

#ifdef HAVE_RB_HASH_ASET
VALUE hash_spec_rb_hash_aset(VALUE self, VALUE hash, VALUE key, VALUE val) {
  return rb_hash_aset(hash, key, val);
}
#endif

#ifdef HAVE_RB_HASH_DELETE
VALUE hash_spec_rb_hash_delete(VALUE self, VALUE hash, VALUE key) {
  return rb_hash_delete(hash, key);
}
#endif

#ifdef HAVE_RB_HASH_DELETE_IF
VALUE hash_spec_rb_hash_delete_if(VALUE self, VALUE hash) {
  return rb_hash_delete_if(hash);
}
#endif

#ifdef HAVE_RB_HASH_FOREACH
static int foreach_i(VALUE key, VALUE val, VALUE other) {
  rb_hash_aset(other, key, val);
  return 0; /* ST_CONTINUE; */
}

static int foreach_stop_i(VALUE key, VALUE val, VALUE other) {
  rb_hash_aset(other, key, val);
  return 1; /* ST_STOP; */
}

static int foreach_delete_i(VALUE key, VALUE val, VALUE other) {
  rb_hash_aset(other, key, val);
  return 2; /* ST_DELETE; */
}

VALUE hash_spec_rb_hash_foreach(VALUE self, VALUE hsh) {
  VALUE other = rb_hash_new();
  rb_hash_foreach(hsh, foreach_i, other);
  return other;
}

VALUE hash_spec_rb_hash_foreach_stop(VALUE self, VALUE hsh) {
  VALUE other = rb_hash_new();
  rb_hash_foreach(hsh, foreach_stop_i, other);
  return other;
}

VALUE hash_spec_rb_hash_foreach_delete(VALUE self, VALUE hsh) {
  VALUE other = rb_hash_new();
  rb_hash_foreach(hsh, foreach_delete_i, other);
  return other;
}
#endif

#ifdef HAVE_RB_HASH_LOOKUP
VALUE hash_spec_rb_hash_lookup(VALUE self, VALUE hash, VALUE key) {
  return rb_hash_lookup(hash, key);
}

VALUE hash_spec_rb_hash_lookup_nil(VALUE self, VALUE hash, VALUE key) {
  VALUE ret = rb_hash_lookup(hash, key);
  return ret == Qnil ? Qtrue : Qfalse;
}
#endif

#ifdef HAVE_RB_HASH_NEW
VALUE hash_spec_rb_hash_new(VALUE self) {
  return rb_hash_new();
}
#endif

#ifdef HAVE_RB_HASH_SIZE
/* rb_hash_size is a static symbol in MRI */
VALUE hash_spec_rb_hash_size(VALUE self, VALUE hash) {
  return rb_hash_size(hash);
}
#endif

void Init_hash_spec() {
  VALUE cls;
  cls = rb_define_class("CApiHashSpecs", rb_cObject);

#ifdef HAVE_RB_HASH
  rb_define_method(cls, "rb_hash", hash_spec_rb_hash, 1);
#endif

#ifdef HAVE_RB_HASH_AREF
  rb_define_method(cls, "rb_hash_aref", hash_spec_rb_hash_aref, 2);
  rb_define_method(cls, "rb_hash_aref_nil", hash_spec_rb_hash_aref_nil, 2);
#endif

#ifdef HAVE_RB_HASH_ASET
  rb_define_method(cls, "rb_hash_aset", hash_spec_rb_hash_aset, 3);
#endif

#ifdef HAVE_RB_HASH_DELETE
  rb_define_method(cls, "rb_hash_delete", hash_spec_rb_hash_delete, 2);
#endif

#ifdef HAVE_RB_HASH_DELETE_IF
  rb_define_method(cls, "rb_hash_delete_if", hash_spec_rb_hash_delete_if, 1);
#endif

#ifdef HAVE_RB_HASH_FOREACH
  rb_define_method(cls, "rb_hash_foreach", hash_spec_rb_hash_foreach, 1);
  rb_define_method(cls, "rb_hash_foreach_stop", hash_spec_rb_hash_foreach_stop, 1);
  rb_define_method(cls, "rb_hash_foreach_delete", hash_spec_rb_hash_foreach_delete, 1);
#endif

#ifdef HAVE_RB_HASH_LOOKUP
  rb_define_method(cls, "rb_hash_lookup_nil", hash_spec_rb_hash_lookup_nil, 2);
  rb_define_method(cls, "rb_hash_lookup", hash_spec_rb_hash_lookup, 2);
#endif

#ifdef HAVE_RB_HASH_NEW
  rb_define_method(cls, "rb_hash_new", hash_spec_rb_hash_new, 0);
#endif

#ifdef HAVE_RB_HASH_SIZE
  rb_define_method(cls, "rb_hash_size", hash_spec_rb_hash_size, 1);
#endif
}

#ifdef __cplusplus
}
#endif
