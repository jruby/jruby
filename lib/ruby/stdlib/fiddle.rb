require 'fiddle.so' unless RUBY_ENGINE == 'jruby'
require 'fiddle/jruby' if RUBY_ENGINE == 'jruby'
require 'fiddle/function'
require 'fiddle/closure'

module Fiddle
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

  # call-seq: dlopen(library) => Fiddle::Handle
  #
  # Creates a new handler that opens +library+, and returns an instance of
  # Fiddle::Handle.
  #
  # If +nil+ is given for the +library+, Fiddle::Handle::DEFAULT is used, which
  # is the equivalent to RTLD_DEFAULT. See <code>man 3 dlopen</code> for more.
  #
  #   lib = Fiddle.dlopen(nil)
  #
  # The default is dependent on OS, and provide a handle for all libraries
  # already loaded. For example, in most cases you can use this to access
  # +libc+ functions, or ruby functions like +rb_str_new+.
  #
  # See Fiddle::Handle.new for more.
  def dlopen library
    Fiddle::Handle.new library
  end
  module_function :dlopen

  class Handle
    RTLD_GLOBAL = FFI::DynamicLibrary::RTLD_GLOBAL
    RTLD_LAZY = FFI::DynamicLibrary::RTLD_LAZY
    RTLD_NOW = FFI::DynamicLibrary::RTLD_NOW
    
    def initialize(libname = nil, flags = RTLD_LAZY | RTLD_GLOBAL)
      @lib = FFI::DynamicLibrary.open(libname, flags)
      raise RuntimeError, "Could not open #{libname}" unless @lib

      @open = true

      begin
        yield(self)
      ensure
        self.close
      end if block_given?
    end

    def close
      raise DLError.new("closed handle") unless @open
      @open = false
      0
    end

    def self.sym(func)
      DEFAULT.sym(func)
    end

    def sym(func)
      raise TypeError.new("invalid function name") unless func.is_a?(String)
      raise DLError.new("closed handle") unless @open
      address = @lib.find_function(func)
      raise DLError.new("unknown symbol #{func}") if address.nil? || address.null?
      address.to_i
    end

    def self.[](func)
      self.sym(func)
    end

    def [](func)
      sym(func)
    end

    def enable_close
      @enable_close = true
    end

    def close_enabled?
      @enable_close
    end

    def disable_close
      @enable_close = false
    end
  end

  # Add constants for backwards compat

  RTLD_GLOBAL = Handle::RTLD_GLOBAL # :nodoc:
  RTLD_LAZY   = Handle::RTLD_LAZY   # :nodoc:
  RTLD_NOW    = Handle::RTLD_NOW    # :nodoc:
end
