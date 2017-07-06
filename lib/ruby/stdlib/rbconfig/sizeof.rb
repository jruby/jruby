# RbConfig::SIZEOF, implemented in terms of FFI
require 'ffi'

module RbConfig
  sizeof = {}
  FFI::TypeDefs.each {|k, v| sizeof[k.to_s] = v.size}
  SIZEOF = sizeof
end