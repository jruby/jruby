# Stub to satisfy `require "timeout"` for MRI tests
# Error stub for rubygems command_manager.rb
module Timeout
  class Error < RuntimeError
  end
end