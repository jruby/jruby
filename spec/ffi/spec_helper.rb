#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require_relative 'fixtures/compile'
require 'timeout'

RSpec.configure do |c|
  c.filter_run_excluding :broken => true
end

module TestLibrary
  def self.force_gc
    if RUBY_ENGINE == 'jruby'
      java.lang.System.gc
    elsif RUBY_ENGINE == 'rbx'
      GC.run(true)
    else
      GC.start
    end
  end
end

module LibTest
  extend FFI::Library
  ffi_lib TestLibrary::PATH
end

def external_run(cmd, rb_file, options: [], timeout: 10)
  path = File.join(File.dirname(__FILE__), rb_file)
  log = "#{path}.log"
  pid = spawn(cmd, "-Ilib", path, { [:out, :err] => log })
  begin
    Timeout.timeout(timeout){ Process.wait(pid) }
  rescue Timeout::Error
    Process.kill(9, pid)
    raise
  else
    if $?.exitstatus != 0
      raise "external process failed:\n#{ File.read(log) }"
    end
  end
  File.read(log)
end

module OrderHelper
  case FFI::Platform::BYTE_ORDER
  when FFI::Platform::LITTLE_ENDIAN
    ORDER = :little
    OTHER_ORDER = :big
  when FFI::Platform::BIG_ENDIAN
    ORDER = :big
    OTHER_ORDER = :little
  else
    raise
  end
end
