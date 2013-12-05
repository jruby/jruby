require 'fiddle.so' unless RUBY_ENGINE == 'jruby'
require 'fiddle/jruby' if RUBY_ENGINE == 'jruby'
require 'fiddle/function'
require 'fiddle/closure'
require 'dl' unless Object.const_defined?(:DL)

module Fiddle

  # A reference to DL::CPtr
  Pointer = DL::CPtr unless RUBY_ENGINE == 'jruby'

  if WINDOWS
    # Returns the last win32 +Error+ of the current executing +Thread+ or nil
    # if none
    def self.win32_last_error
      if RUBY_ENGINE == 'jruby'
        errno = FFI.errno
        errno = nil if errno == 0
      else
        Thread.current[:__FIDDLE_WIN32_LAST_ERROR__]
      end
    end

    # Sets the last win32 +Error+ of the current executing +Thread+ to +error+
    def self.win32_last_error= error
      if RUBY_ENGINE == 'jruby'
        FFI.errno = error || 0
      else
        Thread.current[:__FIDDLE_WIN32_LAST_ERROR__] = error
      end
    end
  end

  # Returns the last +Error+ of the current executing +Thread+ or nil if none
  def self.last_error
    if RUBY_ENGINE == 'jruby'
      errno = FFI.errno
      errno = nil if errno == 0
      errno
    else
      Thread.current[:__FIDDLE_LAST_ERROR__]
    end
  end

  # Sets the last +Error+ of the current executing +Thread+ to +error+
  def self.last_error= error
    if RUBY_ENGINE == 'jruby'
      FFI.errno = error || 0
    else
      Thread.current[:__DL2_LAST_ERROR__] = error
      Thread.current[:__FIDDLE_LAST_ERROR__] = error
    end
  end
end
