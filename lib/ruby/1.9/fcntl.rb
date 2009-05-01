begin
  require 'ffi'
  require "#{File.join(FFI::Platform::CONF_DIR, 'fcntl.rb')}"
rescue LoadError => ex
  raise LoadError, "Fcntl not supported on this platform"
end