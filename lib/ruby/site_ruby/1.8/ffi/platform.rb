require 'ffi.so'
module JRuby
  module FFI
    module Platform
      #
      # Most of the constants are now defined in org.jruby.ext.ffi.Platform.java
      #
      FFI_DIR = File.dirname(__FILE__)
      CONF_DIR = File.join(FFI_DIR, "platform", NAME)
    end
  end
end
module FFI
  module Platform
    def self.windows?
      JRuby::FFI::Platform::IS_WINDOWS
    end
    def self.mac?
      JRuby::FFI::Platform::IS_MAC
    end
    def self.unix?
      !JRuby::FFI::Platform::IS_WINDOWS
    end
    def self.bsd?
      JRuby::FFI::Platform::IS_BSD
    end
  end
end
