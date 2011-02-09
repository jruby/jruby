require 'fiddle/jruby'
require 'fiddle/function'
require 'fiddle/closure'
require 'dl' unless Object.const_defined?(:DL)

module Fiddle
  #Pointer = DL::CPtr
  
  if WINDOWS
    def self.win32_last_error
      errno = FFI.errno
      errno = nil if errno == 0
    end

    def self.win32_last_error= error
      FFI.errno = error || 0
    end
  end

  def self.last_error
    errno = FFI.errno
    errno = nil if errno == 0
    errno
  end

  def self.last_error= error
    FFI.errno = error || 0
  end
end
