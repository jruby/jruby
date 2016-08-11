#include <ruby.h>

#include <libxml/xmlstring.h>

VALUE xml_UTF8Strlen(VALUE self, VALUE str) {
  // We need to copy to a native string as we can't pass Ruby strings into native at the moment

  size_t str_len = RSTRING_LEN(str);
  char *str_ptr = RSTRING_PTR(str);

  unsigned char *native_str = alloca(str_len + 1);

  for (int n = 0; n < str_len; n++) {
    native_str[n] = str_ptr[n];
  }

  native_str[str_len] = '\0';

  return INT2NUM(xmlUTF8Strlen(native_str));
}

void Init_xml() {
  VALUE module = rb_define_module("XML");
  rb_define_module_function(module, "UTF8Strlen", &xml_UTF8Strlen, 1);
}
