# RbConfig::SIZEOF, implemented in terms of FFI
require 'ffi'

module RbConfig
  sizeof = {}
  FFI::TypeDefs.each {|k, v| sizeof[k.to_s] = v.size}
  sizeof["void*"] = sizeof["pointer"]
  sizeof.freeze
  SIZEOF = sizeof

  limits = {}
  limits['FIXNUM_MAX'] = 0x8000000000000000
  limits['FIXNUM_MIN'] = 0x7fffffffffffffff
  limits['LONG_MAX'] = limits['FIXNUM_MAX']
  limits['LONG_MIN'] = limits['FIXNUM_MIN']
  limits.freeze
  LIMITS = limits
end