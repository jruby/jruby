require 'ffi.so'

module FFI
  module Platform
    #
    # Most of the constants are now defined in org.jruby.ext.ffi.Platform.java
    #
    FFI_DIR = File.dirname(__FILE__)
    CONF_DIR = File.join(FFI_DIR, "platform", NAME)
    def self.windows?
      FFI::Platform::IS_WINDOWS
    end
    def self.mac?
      FFI::Platform::IS_MAC
    end
    def self.unix?
      !FFI::Platform::IS_WINDOWS
    end
    def self.bsd?
      FFI::Platform::IS_BSD
    end
    def self.linux?
      FFI::Platform::IS_LINUX
    end
  end
end
