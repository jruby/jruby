module JRuby
  module FFI
    module Platform
      #
      # Most of the constants are now defined in org.jruby.ext.ffi.Platform.java
      #
      NAME = "#{ARCH}-#{OS}"
      FFI_DIR = File.dirname(__FILE__)
      CONF_DIR = File.join(File.dirname(__FILE__), "platform", NAME)      
    end
  end
end

