# RbConfig::SIZEOF, implemented in terms of FFI
require 'ffi'

module RbConfig
  sizeof = {}
  FFI::TypeDefs.each {|k, v| sizeof[k.to_s] = v.size}
  sizeof["void*"] = sizeof["pointer"]
  sizeof.freeze
  SIZEOF = sizeof

  limits = {}
  limits['FIXNUM_MAX'] = 0x7fffffffffffffff
  limits['FIXNUM_MIN'] = -0x8000000000000000
  limits['LONG_MAX'] = limits['FIXNUM_MAX']
  limits['LONG_MIN'] = limits['FIXNUM_MIN']
  limits['INT_MAX'] = 0x7fffffff
  limits['INT_MIN'] = -0x80000000
  limits['INTPTR_MAX'] = 0x7FFFFFFFFFFFFFFF
  limits['INT64_MAX'] = 0x7FFFFFFFFFFFFFFF
  limits['INT64_MIN'] = -0x8000000000000000
  limits['UINT64_MAX'] = 0xFFFFFFFFFFFFFFFF
  limits['LLONG_MAX'] = limits['INT64_MAX']
  limits['LLONG_MIN'] = limits['INT64_MIN']
  limits['ULLONG_MAX'] = limits['UINT64_MAX']
  limits.freeze
  LIMITS = limits
end