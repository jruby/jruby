#include <ruby.h>

#include <openssl/md5.h>

VALUE xopenssl_md5(VALUE self, VALUE str) {
  // We need to copy to a native string as we can't pass Ruby strings into native at the moment

  size_t str_len = RSTRING_LEN(str);
  char *str_ptr = RSTRING_PTR(str);

  unsigned char *native_str = alloca(str_len + 1);

  for (int n = 0; n < str_len; n++) {
    native_str[n] = str_ptr[n];
  }

  native_str[str_len] = '\0';

  unsigned char digest[MD5_DIGEST_LENGTH];

  MD5(native_str, str_len, digest);

  char *hex = alloca(MD5_DIGEST_LENGTH * 2 + 1);

  char *hex_ptr = hex;

  for (int n = 0; n < MD5_DIGEST_LENGTH; n++){
    hex_ptr += sprintf(hex_ptr, "%02x", digest[n]);
  }

  *hex_ptr = '\0';

  return rb_str_new_cstr(hex);
}

void Init_xopenssl() {
  VALUE module = rb_define_module("XOpenSSL");
  rb_define_module_function(module, "md5", &xopenssl_md5, 1);
}
