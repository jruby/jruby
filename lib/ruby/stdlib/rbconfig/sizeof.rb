# RbConfig::SIZEOF, implemented in terms of FFI
require 'ffi'

module RbConfig
  sizeof = {}
  FFI::TypeDefs.each {|k, v| sizeof[k.to_s] = v.size}
  sizeof["void*"] = sizeof["pointer"]
  SIZEOF = sizeof
end